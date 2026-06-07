package dds;

import chongqing.TrafficFlow;
import urban.EnvironmentPerception;
import urban.EmergencyAlert;
import urban.VehicleData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 新协同主题的DDS消费者。
 */
public class CoordinationTopicConsumer {

    private final DDSManager dds;
    private final CoordinationTopicMapper mapper;
    private final Map<String, BiConsumer<String, Map<String, Object>>> handlers = new ConcurrentHashMap<>();

    public CoordinationTopicConsumer(DDSManager dds) {
        this.dds = dds;
        this.mapper = new CoordinationTopicMapper();
    }

    public void onSemanticTopic(String semanticTopic, BiConsumer<String, Map<String, Object>> handler) {
        handlers.put(semanticTopic, handler);
    }

    public void subscribeAll() {
        dds.subscribeTrafficFlow(this::onTrafficFlow);
        dds.subscribeEnvironmentPerception(this::onEnvironmentPerception);
        dds.subscribeVehicleData(this::onGlobalPerceptionVehicleData);
        dds.subscribeEmergencyStatus(status -> {
            // 保留原有EmergencyStatus订阅能力，不映射到新语义topic。
        });
        dds.subscribeMissionCommand(cmd -> {
            // 保留原有MissionCommand订阅能力，不映射到新语义topic。
        });
    }

    public void subscribeReplanTrigger() {
        // 使用EmergencyAlert作为重规划触发映射。
        dds.subscribeEmergencyAlert(this::onReplanTriggerAlert);
    }

    private void onTrafficFlow(TrafficFlow flow) {
        dispatch(CoordinationTopicMapper.TOPIC_FORWARD_TRAFFIC, mapper.fromTrafficFlow(flow));
    }

    private void onEnvironmentPerception(EnvironmentPerception perception) {
        dispatch(CoordinationTopicMapper.TOPIC_FORWARD_CORRIDOR, mapper.fromEnvironmentPerception(perception));
    }

    private void onGlobalPerceptionVehicleData(VehicleData data) {
        if ("GLOBAL_PERCEPTION".equals(data.drive_mode)) {
            dispatch(CoordinationTopicMapper.TOPIC_GLOBAL_PERCEPTION, mapper.fromVehicleData(data));
        }
    }

    private void onReplanTriggerAlert(EmergencyAlert alert) {
        dispatch(CoordinationTopicMapper.TOPIC_REPLAN_TRIGGER, mapper.fromEmergencyAlert(alert));
    }

    private void dispatch(String semanticTopic, Map<String, Object> payload) {
        BiConsumer<String, Map<String, Object>> handler = handlers.get(semanticTopic);
        if (handler != null) {
            handler.accept(semanticTopic, payload);
        }
    }
}
