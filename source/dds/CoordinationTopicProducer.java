package dds;

import java.util.Map;

/**
 * 新协同主题的DDS生产者。
 */
public class CoordinationTopicProducer {

    private final DDSManager dds;
    private final CoordinationTopicMapper mapper;

    public CoordinationTopicProducer(DDSManager dds) {
        this.dds = dds;
        this.mapper = new CoordinationTopicMapper();
    }

    public long publish(String semanticTopic, Map<String, Object> payload) {
        if (CoordinationTopicMapper.TOPIC_FORWARD_TRAFFIC.equals(semanticTopic)) {
            return dds.publishTrafficFlow(mapper.toTrafficFlow(payload));
        }
        if (CoordinationTopicMapper.TOPIC_FORWARD_CORRIDOR.equals(semanticTopic)) {
            return dds.publishEnvironmentPerception(mapper.toEnvironmentPerception(payload));
        }
        if (CoordinationTopicMapper.TOPIC_GLOBAL_PERCEPTION.equals(semanticTopic)) {
            return dds.publishVehicleData(mapper.toGlobalPerceptionVehicleData(payload));
        }
        if (CoordinationTopicMapper.TOPIC_REPLAN_TRIGGER.equals(semanticTopic)) {
            return dds.publishEmergencyAlert(mapper.toReplanTriggerAlert(payload));
        }
        throw new IllegalArgumentException("Unsupported semantic topic: " + semanticTopic);
    }
}
