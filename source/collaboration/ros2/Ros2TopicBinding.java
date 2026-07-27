package collaboration.ros2;

import java.util.LinkedHashMap;
import java.util.Map;

public class Ros2TopicBinding {
    public final String semanticTopic;
    public final String rosTopic;
    public final String messageType;
    public final String idlType;
    public final String qosProfile;
    public final String direction;
    public final String purpose;

    public Ros2TopicBinding(String semanticTopic, String rosTopic, String messageType,
                            String idlType, String qosProfile, String direction, String purpose) {
        this.semanticTopic = semanticTopic;
        this.rosTopic = rosTopic;
        this.messageType = messageType;
        this.idlType = idlType;
        this.qosProfile = qosProfile;
        this.direction = direction;
        this.purpose = purpose;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("semanticTopic", semanticTopic);
        map.put("rosTopic", rosTopic);
        map.put("messageType", messageType);
        map.put("idlType", idlType);
        map.put("qosProfile", qosProfile);
        map.put("direction", direction);
        map.put("purpose", purpose);
        return map;
    }
}
