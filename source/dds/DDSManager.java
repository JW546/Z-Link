package dds;

import chongqing.*;
import urban.*;

import com.rti.dds.domain.*;
import com.rti.dds.infrastructure.*;
import com.rti.dds.publication.*;
import com.rti.dds.subscription.*;
import com.rti.dds.topic.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RTI Connext DDS 真实发布-订阅管理器
 * @paper_ref 第4章 4.2 三层无缝集成 -> IDL层实现
 */
public class DDSManager {
    
    private static DDSManager instance;
    private DomainParticipant participant = null;
    private final int domainId;
    
    private Publisher publisher = null;
    private Subscriber subscriber = null;
    
    private final Map<String, DataWriter> writers = new ConcurrentHashMap<>();
    private final Map<String, DataReader> readers = new ConcurrentHashMap<>();
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final Map<String, List<DataCallback<?>>> readerCallbacks = new ConcurrentHashMap<>();
    
    private final AtomicLong sampleSequence = new AtomicLong(0);
    private volatile boolean connected = false;
    
    // 日志和监控集成
    private DDSLogger logger;
    private MetricsCollector metrics;
    private volatile boolean loggingEnabled = true;
    
    public interface DataCallback<T> { void onData(T data); }
    
    private DDSManager(int domainId) { 
        this.domainId = domainId;
        initLogging();
    }
    
    /**
     * 初始化日志和指标收集系统
     * @paper_ref 第5章 5.3 数据流监控
     */
    private void initLogging() {
        try {
            this.logger = DDSLogger.getInstance("logs/dds");
            this.metrics = MetricsCollector.getInstance();
            logger.info("DDSManager", "日志和监控模块初始化完成");
        } catch (Exception e) {
            System.err.println("[DDSManager] 日志初始化失败: " + e.getMessage());
        }
    }
    
    public static DDSManager getInstance() { return getInstance(0); }
    
    public static synchronized DDSManager getInstance(int domainId) {
        if (instance == null) instance = new DDSManager(domainId);
        return instance;
    }
    
    public boolean connect() {
        if (connected) { System.out.println("[DDS] 已连接到域: " + domainId); return true; }
        System.out.println("[DDS] 连接到域: " + domainId);
        if (logger != null) logger.info("DDSManager", "正在连接到域: " + domainId);
        
        try {
            // 创建 QoS 对象
            DomainParticipantQos dpQos = new DomainParticipantQos();
            PublisherQos pubQos = new PublisherQos();
            SubscriberQos subQos = new SubscriberQos();
            
            // 获取默认 QoS
            DomainParticipantFactory.get_instance().get_default_participant_qos(dpQos);
            participant = DomainParticipantFactory.get_instance()
                .create_participant(domainId, dpQos, null, StatusKind.STATUS_MASK_NONE);
            if (participant == null) { 
                System.err.println("[DDS] 创建 DomainParticipant 失败!"); 
                if (logger != null) logger.error("DDSManager", "创建 DomainParticipant 失败");
                return false; 
            }
            
            // 注册所有类型
            EmergencyStatusTypeSupport.register_type(participant, EmergencyStatusTypeSupport.get_type_name());
            MissionCommandTypeSupport.register_type(participant, MissionCommandTypeSupport.get_type_name());
            DronePositionTypeSupport.register_type(participant, DronePositionTypeSupport.get_type_name());
            TrafficFlowTypeSupport.register_type(participant, TrafficFlowTypeSupport.get_type_name());
            VehicleDataTypeSupport.register_type(participant, VehicleDataTypeSupport.get_type_name());
            EmergencyAlertTypeSupport.register_type(participant, EmergencyAlertTypeSupport.get_type_name());
            SwarmMissionTypeSupport.register_type(participant, SwarmMissionTypeSupport.get_type_name());
            EnvironmentPerceptionTypeSupport.register_type(participant, EnvironmentPerceptionTypeSupport.get_type_name());
            
            participant.get_default_publisher_qos(pubQos);
            participant.get_default_subscriber_qos(subQos);
            publisher = participant.create_publisher(pubQos, null, StatusKind.STATUS_MASK_NONE);
            subscriber = participant.create_subscriber(subQos, null, StatusKind.STATUS_MASK_NONE);
            
            connected = true;
            System.out.println("[DDS] 连接成功 (Domain=" + domainId + ") ✓");
            if (logger != null) {
                logger.info("DDSManager", "DDS连接成功，域=" + domainId);
                if (metrics != null) metrics.start();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[DDS] 连接失败: " + e.getMessage());
            if (logger != null) logger.error("DDSManager", "连接失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public synchronized void disconnect() {
        if (!connected || participant == null) return;
        
        // 记录断开前的统计
        if (logger != null) {
            logger.info("DDSManager", "正在断开DDS连接...");
        }
        
        // 1. 删除 DataWriter
        writers.values().forEach(this::cleanupWriter);
        writers.clear();
        // 2. 删除 DataReader
        readers.values().forEach(this::cleanupReader);
        readers.clear();
        // 3. 删除 Topics（必须在 delete_participant 之前）
        topics.values().forEach(this::cleanupTopic);
        topics.clear();
        // 4. 删除 Publisher 和 Subscriber
        if (publisher != null) participant.delete_publisher(publisher);
        if (subscriber != null) participant.delete_subscriber(subscriber);
        // 5. 删除 DomainParticipant
        DomainParticipantFactory.get_instance().delete_participant(participant);
        participant = null; connected = false;
        
        // 停止指标收集
        if (metrics != null) {
            metrics.stop();
        }
        
        System.out.println("[DDS] 已断开连接");
        if (logger != null) {
            logger.info("DDSManager", "DDS连接已断开");
            printMonitoringReport();
        }
    }
    
    // ==================== EmergencyStatus ====================
    public long publishEmergencyStatus(EmergencyStatus status) {
        if (!connected) return -1;
        try {
            EmergencyStatusDataWriter writer = (EmergencyStatusDataWriter) writers.computeIfAbsent("chongqing/emergency/status", k -> createWriter("chongqing/emergency/status", EmergencyStatusTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long now = System.currentTimeMillis();
            status.timestamp_sec = (int)(now / 1000);
            status.timestamp_usec = (int)((now % 1000) * 1000);
            long seq = sampleSequence.incrementAndGet();
            
            // 记录发送时间戳用于延迟计算
            long sendTimeNs = System.nanoTime();
            writer.write(status, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] EmergencyStatus[" + status.drone_id + "] seq=" + seq + " status=" + status.status);
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("drone=%s status=%s battery=%.1f", 
                    status.drone_id, status.status, status.battery_level);
                logger.logPublish("chongqing/emergency/status", summary, estimateSize(status), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("chongqing/emergency/status", estimateSize(status));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "EmergencyStatus发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("chongqing/emergency/status");
            return -1; 
        }
    }
    
    public void subscribeEmergencyStatus(DataCallback<EmergencyStatus> callback) {
        if (!connected) connect();
        subscribeTopic("chongqing/emergency/status", EmergencyStatus.class, callback);
    }
    
    // ==================== MissionCommand ====================
    public long publishMissionCommand(MissionCommand command) {
        if (!connected) return -1;
        try {
            MissionCommandDataWriter writer = (MissionCommandDataWriter) writers.computeIfAbsent("chongqing/mission/command", k -> createWriter("chongqing/mission/command", MissionCommandTypeSupport.get_type_name()));
            if (writer == null) return -1;
            if (command.valid_until == 0) command.valid_until = (int)(System.currentTimeMillis() / 1000 + 3600);
            long seq = sampleSequence.incrementAndGet();
            
            long sendTimeNs = System.nanoTime();
            writer.write(command, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] MissionCommand[" + command.command_id + "] drone=" + command.drone_id);
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("cmd=%s drone=%s type=%s priority=%d", 
                    command.command_id, command.drone_id, command.mission_type, command.priority);
                logger.logPublish("chongqing/mission/command", summary, estimateSize(command), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("chongqing/mission/command", estimateSize(command));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "MissionCommand发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("chongqing/mission/command");
            return -1; 
        }
    }
    
    public void subscribeMissionCommand(DataCallback<MissionCommand> callback) {
        if (!connected) connect();
        subscribeTopic("chongqing/mission/command", MissionCommand.class, callback);
    }
    
    // ==================== SwarmMission ====================
    public long publishSwarmMission(SwarmMission swarm) {
        if (!connected) return -1;
        try {
            SwarmMissionDataWriter writer = (SwarmMissionDataWriter) writers.computeIfAbsent("chongqing/swarm/mission", k -> createWriter("chongqing/swarm/mission", SwarmMissionTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long seq = sampleSequence.incrementAndGet();
            
            long sendTimeNs = System.nanoTime();
            writer.write(swarm, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] SwarmMission[" + swarm.mission_id + "] team=" + swarm.team_size);
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("mission=%s team=%d type=%s", 
                    swarm.mission_id, swarm.team_size, swarm.mission_type);
                logger.logPublish("chongqing/swarm/mission", summary, estimateSize(swarm), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("chongqing/swarm/mission", estimateSize(swarm));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "SwarmMission发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("chongqing/swarm/mission");
            return -1; 
        }
    }
    
    // ==================== EmergencyAlert ====================
    public long publishEmergencyAlert(EmergencyAlert alert) {
        if (!connected) return -1;
        try {
            EmergencyAlertDataWriter writer = (EmergencyAlertDataWriter) writers.computeIfAbsent("chongqing/emergency/alert", k -> createWriter("chongqing/emergency/alert", EmergencyAlertTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long seq = sampleSequence.incrementAndGet();
            
            long sendTimeNs = System.nanoTime();
            writer.write(alert, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] EmergencyAlert[id=" + alert.alert_id + "] level=" + alert.level);
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("alert=%d level=%s type=%s radius=%.1fm", 
                    alert.alert_id, alert.level, alert.alert_type, alert.affected_radius);
                logger.logPublish("chongqing/emergency/alert", summary, estimateSize(alert), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("chongqing/emergency/alert", estimateSize(alert));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "EmergencyAlert发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("chongqing/emergency/alert");
            return -1; 
        }
    }

    public void subscribeEmergencyAlert(DataCallback<EmergencyAlert> callback) {
        if (!connected) connect();
        subscribeTopic("chongqing/emergency/alert", EmergencyAlert.class, callback);
    }
    
    // ==================== VehicleData ====================
    public long publishVehicleData(VehicleData vehicle) {
        if (!connected) return -1;
        try {
            VehicleDataDataWriter writer = (VehicleDataDataWriter) writers.computeIfAbsent("urban/vehicle/data", k -> createWriter("urban/vehicle/data", VehicleDataTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long seq = sampleSequence.incrementAndGet();
            
            long sendTimeNs = System.nanoTime();
            writer.write(vehicle, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] VehicleData[" + vehicle.vehicle_id + "] speed=" + vehicle.speed + "m/s");
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("vehicle=%s speed=%.1f heading=%.1f battery=%d", 
                    vehicle.vehicle_id, vehicle.speed, vehicle.heading, vehicle.battery_level);
                logger.logPublish("urban/vehicle/data", summary, estimateSize(vehicle), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("urban/vehicle/data", estimateSize(vehicle));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "VehicleData发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("urban/vehicle/data");
            return -1; 
        }
    }
    
    public void subscribeVehicleData(DataCallback<VehicleData> callback) {
        if (!connected) connect();
        subscribeTopic("urban/vehicle/data", VehicleData.class, callback);
    }
    
    // ==================== TrafficFlow ====================
    public long publishTrafficFlow(TrafficFlow flow) {
        if (!connected) return -1;
        try {
            TrafficFlowDataWriter writer = (TrafficFlowDataWriter) writers.computeIfAbsent("chongqing/traffic/flow", k -> createWriter("chongqing/traffic/flow", TrafficFlowTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long seq = sampleSequence.incrementAndGet();
            
            long sendTimeNs = System.nanoTime();
            writer.write(flow, InstanceHandle_t.HANDLE_NIL);
            
            System.out.println("[DDS→] TrafficFlow[" + flow.zone_id + "] vehicles=" + flow.vehicle_count);
            
            // 日志记录
            if (loggingEnabled && logger != null) {
                String summary = String.format("zone=%s vehicles=%d congestion=%.1f", 
                    flow.zone_id, flow.vehicle_count, flow.congestion_level);
                logger.logPublish("chongqing/traffic/flow", summary, estimateSize(flow), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("chongqing/traffic/flow", estimateSize(flow));
            }
            
            return seq;
        } catch (Exception e) { 
            System.err.println("[DDS] 发布失败: " + e.getMessage()); 
            if (logger != null) logger.error("DDSManager", "TrafficFlow发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("chongqing/traffic/flow");
            return -1; 
        }
    }

    public void subscribeTrafficFlow(DataCallback<TrafficFlow> callback) {
        if (!connected) connect();
        subscribeTopic("chongqing/traffic/flow", TrafficFlow.class, callback);
    }

    // ==================== EnvironmentPerception ====================
    public long publishEnvironmentPerception(EnvironmentPerception perception) {
        if (!connected) return -1;
        try {
            EnvironmentPerceptionDataWriter writer = (EnvironmentPerceptionDataWriter) writers.computeIfAbsent(
                "urban/environment/perception", k -> createWriter("urban/environment/perception", EnvironmentPerceptionTypeSupport.get_type_name()));
            if (writer == null) return -1;
            long seq = sampleSequence.incrementAndGet();

            long sendTimeNs = System.nanoTime();
            writer.write(perception, InstanceHandle_t.HANDLE_NIL);

            System.out.println("[DDS→] EnvironmentPerception[" + perception.drone_id + "] visibility="
                + String.format("%.0f", perception.visibility));

            if (loggingEnabled && logger != null) {
                String summary = String.format("source=%s temp=%.1f wind=%.1f vis=%.0f turb=%.2f",
                    perception.drone_id, perception.temperature, perception.wind_speed,
                    perception.visibility, perception.turbulence_level);
                logger.logPublish("urban/environment/perception", summary, estimateSize(perception), sendTimeNs);
            }
            if (metrics != null) {
                metrics.recordPublish("urban/environment/perception", estimateSize(perception));
            }

            return seq;
        } catch (Exception e) {
            System.err.println("[DDS] 发布失败: " + e.getMessage());
            if (logger != null) logger.error("DDSManager", "EnvironmentPerception发布失败: " + e.getMessage());
            if (metrics != null) metrics.recordError("urban/environment/perception");
            return -1;
        }
    }

    public void subscribeEnvironmentPerception(DataCallback<EnvironmentPerception> callback) {
        if (!connected) connect();
        subscribeTopic("urban/environment/perception", EnvironmentPerception.class, callback);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 估算数据大小（用于日志统计）
     * @paper_ref 第5章 5.3 数据流监控 -> 吞吐量统计
     */
    private int estimateSize(Object obj) {
        if (obj == null) return 0;
        try {
            // 使用估算值，实际大小取决于序列化方式
            if (obj instanceof EmergencyStatus) return 256;
            if (obj instanceof MissionCommand) return 512;
            if (obj instanceof SwarmMission) return 1024;
            if (obj instanceof EmergencyAlert) return 384;
            if (obj instanceof VehicleData) return 320;
            if (obj instanceof TrafficFlow) return 280;
            if (obj instanceof EnvironmentPerception) return 260;
            return 256;
        } catch (Exception e) {
            return 256;
        }
    }
    
    // ==================== 日志监控方法 ====================
    
    /**
     * 获取日志记录器实例
     */
    public DDSLogger getLogger() {
        return logger;
    }
    
    /**
     * 获取指标收集器实例
     */
    public MetricsCollector getMetrics() {
        return metrics;
    }
    
    /**
     * 启用/禁用日志记录
     */
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
        if (logger != null) {
            logger.info("DDSManager", "日志记录" + (enabled ? "已启用" : "已禁用"));
        }
    }
    
    /**
     * 打印监控报告
     */
    public void printMonitoringReport() {
        if (metrics != null) {
            metrics.printReport();
        }
        if (logger != null) {
            System.out.println(logger.generateReport());
        }
    }
    
    /**
     * 关闭日志监控系统
     */
    public void shutdownLogging() {
        if (logger != null) {
            logger.shutdown();
        }
        if (metrics != null) {
            metrics.shutdown();
        }
    }
    
    private DataWriter createWriter(String topicNameStr, String typeName) {
        try {
            Topic topic = getOrCreateTopic(topicNameStr, typeName);
            if (topic == null) return null;
            
            DataWriterQos dwQos = new DataWriterQos();
            publisher.get_default_datawriter_qos(dwQos);
            System.out.println("[DDS] 创建 DataWriter");
            return publisher.create_datawriter(topic, dwQos, null, StatusKind.STATUS_MASK_NONE);
        } catch (Exception e) { System.err.println("[DDS] 创建 DataWriter 失败: " + e.getMessage()); return null; }
    }
    
    @SuppressWarnings("unchecked")
    private <T> void subscribeTopic(String topicName, Class<T> dataClass, DataCallback<T> callback) {
        readers.computeIfAbsent(topicName, k -> {
            try {
                String typeName = getTypeName(dataClass);
                Topic topic = getOrCreateTopic(topicName, typeName);
                if (topic == null) return null;
                
                DataReaderQos drQos = new DataReaderQos();
                subscriber.get_default_datareader_qos(drQos);
                DataReader reader = subscriber.create_datareader(topic, drQos, null, StatusKind.STATUS_MASK_NONE);
                readerCallbacks.put(topicName, new CopyOnWriteArrayList<>());
                startReaderThread(topicName, reader, dataClass);
                return reader;
            } catch (Exception e) { System.err.println("[DDS] 订阅失败: " + e.getMessage()); return null; }
        });
        if (readerCallbacks.containsKey(topicName)) {
            ((List<DataCallback<T>>)(Object)readerCallbacks.get(topicName)).add(callback);
        }
    }

    private Topic getOrCreateTopic(String topicName, String typeName) {
        Topic existing = topics.get(topicName);
        if (existing != null) {
            return existing;
        }

        synchronized (topics) {
            existing = topics.get(topicName);
            if (existing != null) {
                return existing;
            }

            try {
                System.out.println("[DDS] 创建 Topic: " + topicName);
                TopicQos tQos = new TopicQos();
                participant.get_default_topic_qos(tQos);
                Topic topic = participant.create_topic(topicName, typeName, tQos, null, StatusKind.STATUS_MASK_NONE);
                topics.put(topicName, topic);
                return topic;
            } catch (Exception e) {
                System.err.println("[DDS] 创建 Topic 失败: " + e.getMessage());
                return null;
            }
        }
    }
    
    private String getTypeName(Class<?> dataClass) {
        if (dataClass == EmergencyStatus.class) return EmergencyStatusTypeSupport.get_type_name();
        if (dataClass == MissionCommand.class) return MissionCommandTypeSupport.get_type_name();
        if (dataClass == EmergencyAlert.class) return EmergencyAlertTypeSupport.get_type_name();
        if (dataClass == VehicleData.class) return VehicleDataTypeSupport.get_type_name();
        if (dataClass == TrafficFlow.class) return TrafficFlowTypeSupport.get_type_name();
        if (dataClass == EnvironmentPerception.class) return EnvironmentPerceptionTypeSupport.get_type_name();
        return "";
    }
    
    private <T> void startReaderThread(String topicName, DataReader reader, Class<T> dataClass) {
        new Thread(() -> {
            try {
                while (readers.containsKey(topicName) && connected) {
                    if (dataClass == EmergencyStatus.class) {
                        pollEmergencyStatus(topicName, (EmergencyStatusDataReader) reader);
                    } else if (dataClass == MissionCommand.class) {
                        pollMissionCommand(topicName, (MissionCommandDataReader) reader);
                    } else if (dataClass == VehicleData.class) {
                        pollVehicleData(topicName, (VehicleDataDataReader) reader);
                    } else if (dataClass == TrafficFlow.class) {
                        pollTrafficFlow(topicName, (TrafficFlowDataReader) reader);
                    } else if (dataClass == EmergencyAlert.class) {
                        pollEmergencyAlert(topicName, (EmergencyAlertDataReader) reader);
                    } else if (dataClass == EnvironmentPerception.class) {
                        pollEnvironmentPerception(topicName, (EnvironmentPerceptionDataReader) reader);
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "DDS-Reader-" + topicName).start();
    }

    private void pollEmergencyStatus(String topicName, EmergencyStatusDataReader reader) {
        EmergencyStatusSeq dataSeq = new EmergencyStatusSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    private void pollMissionCommand(String topicName, MissionCommandDataReader reader) {
        MissionCommandSeq dataSeq = new MissionCommandSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    private void pollVehicleData(String topicName, VehicleDataDataReader reader) {
        VehicleDataSeq dataSeq = new VehicleDataSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    private void pollTrafficFlow(String topicName, TrafficFlowDataReader reader) {
        TrafficFlowSeq dataSeq = new TrafficFlowSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    private void pollEmergencyAlert(String topicName, EmergencyAlertDataReader reader) {
        EmergencyAlertSeq dataSeq = new EmergencyAlertSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    private void pollEnvironmentPerception(String topicName, EnvironmentPerceptionDataReader reader) {
        EnvironmentPerceptionSeq dataSeq = new EnvironmentPerceptionSeq();
        SampleInfoSeq infoSeq = new SampleInfoSeq();
        try {
            reader.take(dataSeq, infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                SampleStateKind.ANY_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < dataSeq.size(); i++) {
                if (infoSeq.get(i).valid_data) {
                    dispatchCallback(topicName, dataSeq.get(i));
                }
            }
        } catch (RETCODE_NO_DATA ignored) {
        } finally {
            reader.return_loan(dataSeq, infoSeq);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatchCallback(String topicName, T data) {
        List<DataCallback<?>> callbacks = readerCallbacks.get(topicName);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        for (DataCallback<?> callback : callbacks) {
            try {
                ((DataCallback<T>) callback).onData(data);
                if (metrics != null) {
                    metrics.recordSubscribe(topicName, estimateSize(data));
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("DDSManager", "回调失败 topic=" + topicName + ", error=" + e.getMessage());
                }
            }
        }
    }
    
    private void cleanupWriter(DataWriter w) { try { if (w != null) publisher.delete_datawriter(w); } catch (Exception e) {} }
    private void cleanupReader(DataReader r) { try { if (r != null) subscriber.delete_datareader(r); } catch (Exception e) {} }
    private void cleanupTopic(Topic t) { try { if (t != null && participant != null) participant.delete_topic(t); } catch (Exception e) {} }
    
    // ==================== 状态方法 ====================
    public long getPublishedSampleCount() { return sampleSequence.get(); }
    public int getActiveTopicCount() { return writers.size(); }
    public boolean isConnected() { return connected; }
    
    public void printStatus() {
        System.out.println("\n┌─────────────────────────────────────────────┐");
        System.out.println("│           DDS 发布-订阅状态摘要              │");
        System.out.println("├─────────────────────────────────────────────┤");
        System.out.println("│ 连接状态:     " + (connected ? "已连接 ✓" : "未连接 ✗") + "                    │");
        System.out.println("│ 域 ID:        " + domainId + "                        │");
        System.out.println("│ 发布主题:     " + writers.size() + " 个                      │");
        System.out.println("│ 订阅主题:     " + readers.size() + " 个                      │");
        System.out.println("│ 发布样本:     " + sampleSequence.get() + " 个                     │");
        System.out.println("├─────────────────────────────────────────────┤");
        System.out.println("│ 日志记录:     " + (loggingEnabled ? "已启用 ✓" : "已禁用 ✗") + "                   │");
        if (metrics != null) {
            System.out.println("│ 指标收集:     已启用 ✓                     │");
            System.out.println("│ 发布统计:     " + String.format("%-24d 条│", metrics.getGlobalMetrics().totalPublishCount.get()));
            System.out.println("│ 订阅统计:     " + String.format("%-24d 条│", metrics.getGlobalMetrics().totalSubscribeCount.get()));
        } else {
            System.out.println("│ 指标收集:     未初始化 ✗                   │");
        }
        System.out.println("└─────────────────────────────────────────────┘");
        if (!writers.isEmpty()) {
            System.out.println("\n活跃发布主题:");
            writers.forEach((k, v) -> System.out.println("  • " + k));
        }
    }
    
    // ==================== 测试入口 ====================
    /**
     * DDS 日志监控功能演示
     * @paper_ref 第5章 5.3 数据流监控 -> 系统测试
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  RTI Connext DDS 真实数据发布测试");
        System.out.println("  (含日志与监控功能)");
        System.out.println("===========================================\n");
        
        DDSManager dds = DDSManager.getInstance();
        
        // 演示日志级别设置
        if (dds.getLogger() != null) {
            dds.getLogger().setThreshold(DDSLogger.LogLevel.DEBUG);
            dds.getLogger().info("DDSManager", "===== 开始 DDS 数据流日志监控演示 =====");
        }
        
        if (!dds.connect()) {
            System.err.println("DDS 连接失败!");
            return;
        }
        
        System.out.println("\n--- 发布 EmergencyStatus ---");
        EmergencyStatus status = new EmergencyStatus();
        status.drone_id = "DRONE_001";
        status.device_type = DeviceType.DRONE_MULTIROTOR;
        status.mission_type = MissionType.MEDICAL_SUPPLY_DELIVERY;
        status.status = StatusCode.IN_TRANSIT;
        status.current_position = new Position3D();
        status.current_position.latitude = 29.5647;
        status.current_position.longitude = 106.5507;
        status.current_position.altitude = 120.0;
        status.speed = 15.5;
        status.battery_level = 78.5;
        status.priority = 8;
        dds.publishEmergencyStatus(status);
        
        System.out.println("\n--- 发布 MissionCommand ---");
        MissionCommand cmd = new MissionCommand();
        cmd.command_id = "CMD_001";
        cmd.drone_id = "DRONE_001";
        cmd.mission_type = MissionType.MEDICAL_SUPPLY_DELIVERY;
        cmd.parameters = "{\"target\":\"hospital_a\",\"priority\":8}";
        cmd.priority = 8;
        cmd.source_system = "DSS";
        cmd.operator_id = "OP_001";
        dds.publishMissionCommand(cmd);
        
        System.out.println("\n--- 发布 EmergencyAlert ---");
        EmergencyAlert alert = new EmergencyAlert();
        alert.alert_id = 1001;
        alert.level = AlertLevel.HIGH;
        alert.alert_type = "weather_disaster";
        alert.latitude = 29.55;
        alert.longitude = 106.55;
        alert.affected_radius = 5000;
        alert.recommendation = "建议绕飞或延迟任务";
        dds.publishEmergencyAlert(alert);
        
        System.out.println("\n--- 发布 VehicleData (V2X) ---");
        VehicleData vehicle = new VehicleData();
        vehicle.vehicle_id = "VEH_001";
        vehicle.latitude = 29.57;
        vehicle.longitude = 106.56;
        vehicle.altitude = 0;
        vehicle.speed = 30.0;
        vehicle.heading = 90.0;
        vehicle.battery_level = 100;
        vehicle.drive_mode = "AUTONOMOUS";
        dds.publishVehicleData(vehicle);
        
        System.out.println("\n--- 发布 TrafficFlow ---");
        TrafficFlow flow = new TrafficFlow();
        flow.zone_id = "ZONE_A1";
        flow.vehicle_count = 150;
        flow.congestion_level = 0.75f;
        flow.average_speed = 25.0;
        dds.publishTrafficFlow(flow);
        
        System.out.println("\n--- 发布 SwarmMission ---");
        SwarmMission swarm = new SwarmMission();
        swarm.mission_id = "SWARM_001";
        swarm.team_size = 5;
        swarm.mission_type = "SEARCH_AND_RESCUE";
        dds.publishSwarmMission(swarm);
        
        // 打印状态（包含日志监控统计）
        dds.printStatus();
        
        // 打印详细监控报告
        System.out.println("\n--- 监控报告 ---");
        dds.printMonitoringReport();
        
        System.out.println("\n断开 DDS 连接...");
        try { dds.disconnect(); } catch (Exception e) { System.out.println("[DDS] 断开时出现警告: " + e.getMessage()); }
        
        System.out.println("\n===========================================");
        System.out.println("  测试完成!");
        System.out.println("  日志文件已保存到 logsource/dds/ 目录");
        System.out.println("===========================================");
    }
}
