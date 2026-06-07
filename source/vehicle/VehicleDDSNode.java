package vehicle;

import dds.DDSManager;
import integration.DSLIDLMapper;
import urban.*;
import chongqing.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 车辆边缘 DDS 节点
 * 
 * 功能：
 * 1. 将本地DSL数据发布到DDS网络
 * 2. 订阅其他车辆/无人机的数据
 * 3. V2X协同决策
 * 
 * @paper_ref 第4章 4.2 IDL层 -> DDS边缘节点
 */
public class VehicleDDSNode {
    
    private static final String TAG = "[VehicleDDSNode]";
    
    // DDS管理器
    private final DDSManager dds;
    
    // 本地DSL引擎
    private final VehicleDSLEngine engine;
    
    // 配置
    private final String vehicleId;
    private final int domainId;
    
    // 发布频率配置 (Hz)
    private static final int STATUS_FREQ = 10;      // 状态：10Hz
    
    // 运行控制
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread publisherThread;
    private Thread localExecutorThread;
    
    // 协同感知缓存
    private final ConcurrentHashMap<String, VehicleData> nearbyVehicles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EmergencyAlert> receivedAlerts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TrafficFlow> trafficZones = new ConcurrentHashMap<>();
    
    // IDL映射器
    private final DSLIDLMapper mapper = DSLIDLMapper.getInstance();
    
    // 构造函数
    public VehicleDDSNode(String vehicleId, int domainId) {
        this.vehicleId = vehicleId;
        this.domainId = domainId;
        this.dds = DDSManager.getInstance(domainId);
        this.engine = new VehicleDSLEngine();
        
        System.out.println(TAG + " 初始化车辆DDS节点: " + vehicleId);
    }
    
    // ========== 生命周期管理 ==========
    
    /**
     * 启动边缘节点
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            System.out.println(TAG + " 启动车辆DDS节点: " + vehicleId);
            
            // 1. 连接DDS域
            if (!dds.connect()) {
                System.err.println(TAG + " DDS连接失败！车辆将仅在本地模式运行");
                startLocalOnlyMode();
                return;
            }
            
            System.out.println(TAG + " DDS连接成功");
            
            // 2. 订阅数据
            subscribeToTopics();
            
            // 3. 启动发布线程
            startPublisherThread();
            
            // 4. 启动本地执行循环
            startLocalExecutorThread();
            
            System.out.println(TAG + " 节点启动完成");
        } else {
            System.out.println(TAG + " 节点已在运行");
        }
    }
    
    /**
     * 停止边缘节点
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println(TAG + " 停止车辆DDS节点");
            
            // 等待线程结束
            try {
                if (publisherThread != null) {
                    publisherThread.join(1000);
                }
                if (localExecutorThread != null) {
                    localExecutorThread.join(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 断开DDS
            dds.disconnect();
            
            System.out.println(TAG + " 节点已停止");
        }
    }
    
    /**
     * 仅本地模式启动（无网络）
     */
    private void startLocalOnlyMode() {
        System.out.println(TAG + " 启动仅本地模式");
        startLocalExecutorThread();
    }
    
    // ========== 订阅管理 ==========
    
    /**
     * 订阅DDS主题
     */
    private void subscribeToTopics() {
        System.out.println(TAG + " 订阅DDS主题");
        
        try {
            // 订阅其他车辆状态
            dds.subscribeVehicleData(data -> {
                if (!data.vehicle_id.equals(vehicleId)) {
                    handleVehicleData(data);
                }
            });
            System.out.println(TAG + " - 已订阅: urban/vehicle/data");
            
        } catch (Exception e) {
            System.err.println(TAG + " 订阅失败: " + e.getMessage());
        }
    }
    
    // ========== 数据处理 ==========
    
    /**
     * 处理收到的车辆数据
     */
    private void handleVehicleData(VehicleData data) {
        // 更新缓存
        nearbyVehicles.put(data.vehicle_id, data);
        
        // 计算相对位置
        double distance = calculateDistance(data);
        
        System.out.println(TAG + " 收到车辆 " + data.vehicle_id + 
            " 状态: 速度=" + String.format("%.1f", data.speed) + "m/s, " +
            "距离=" + String.format("%.0f", distance) + "m");
    }
    
    // ========== 发布线程 ==========
    
    /**
     * 启动数据发布线程
     */
    private void startPublisherThread() {
        publisherThread = new Thread(() -> {
            System.out.println(TAG + " 发布线程启动");
            
            while (running.get()) {
                try {
                    // 发布车辆状态 (10Hz)
                    publishVehicleStatus();
                    
                    Thread.sleep(1000 / STATUS_FREQ);
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println(TAG + " 发布异常: " + e.getMessage());
                }
            }
            
            System.out.println(TAG + " 发布线程结束");
        }, "DDS-VehiclePublisher");
        
        publisherThread.start();
    }
    
    /**
     * 发布车辆状态
     */
    private void publishVehicleStatus() {
        if (!dds.isConnected()) return;
        
        try {
            // 获取本地DSL引擎的状态
            VehicleData data = engine.toIDLData();
            
            if (data != null) {
                dds.publishVehicleData(data);
                
                if (System.currentTimeMillis() % 1000 < 200) {  // 每秒打印一次
                    System.out.println(TAG + " 发布状态: " + data.vehicle_id + 
                        " 速度=" + String.format("%.1f", data.speed) + "m/s" +
                        " 电量=" + data.battery_level + "%");
                }
            }
        } catch (Exception e) {
            System.err.println(TAG + " 发布状态失败: " + e.getMessage());
        }
    }
    
    // ========== 本地执行线程 ==========
    
    /**
     * 启动本地执行循环
     */
    private void startLocalExecutorThread() {
        localExecutorThread = new Thread(() -> {
            System.out.println(TAG + " 本地ECU执行线程启动");
            
            while (running.get()) {
                try {
                    // 本地DSL执行（不依赖网络）
                    VehicleDSLEngine.ExecutionResult result = engine.executeLocal();
                    
                    // 如果是紧急操作，直接执行
                    if ("EMERGENCY_STOP".equals(result.action)) {
                        executeEmergencyControl(result);
                    }
                    
                    Thread.sleep(50);  // 20Hz本地控制
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println(TAG + " 本地执行异常: " + e.getMessage());
                }
            }
            
            System.out.println(TAG + " 本地ECU执行线程结束");
        }, "Local-ECU-Executor");
        
        localExecutorThread.start();
    }
    
    /**
     * 执行紧急控制
     */
    private void executeEmergencyControl(VehicleDSLEngine.ExecutionResult result) {
        System.out.println(TAG + " 执行本地紧急控制: " + result.reason);
        
        // 直接通过CAN总线发送到底盘ECU
        engine.sendEmergencyBrake();
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 计算与其他车辆的距离
     */
    private double calculateDistance(VehicleData other) {
        VehicleDSLEngine.Position myPos = engine.getCurrentPosition();
        
        // 使用简化的欧几里得距离（实际应使用Haversine公式）
        double dlat = other.latitude - myPos.lat;
        double dlon = other.longitude - myPos.lon;
        
        // 近似计算：1度 ≈ 111km
        return Math.sqrt(dlat * dlat + dlon * dlon) * 111000;
    }
    
    // ========== 公共访问方法 ==========
    
    /**
     * 获取本地引擎
     */
    public VehicleDSLEngine getEngine() {
        return engine;
    }
    
    /**
     * 获取附近车辆列表
     */
    public Map<String, VehicleData> getNearbyVehicles() {
        return new HashMap<>(nearbyVehicles);
    }
    
    /**
     * 获取活跃预警列表
     */
    public Map<String, EmergencyAlert> getActiveAlerts() {
        return new HashMap<>(receivedAlerts);
    }
    
    /**
     * 获取DDS连接状态
     */
    public boolean isConnected() {
        return dds.isConnected();
    }
    
    /**
     * 获取车辆ID
     */
    public String getVehicleId() {
        return vehicleId;
    }
}
