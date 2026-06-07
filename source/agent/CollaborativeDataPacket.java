package agent;

/**
 * 协同数据包：统一封装不同设备发布的数据元信息。
 */
public class CollaborativeDataPacket {
    public String topic;
    public String sourceType;
    public String sourceId;
    public String scope;
    public long timestamp;
    public double confidence;
    public Object payload;

    public CollaborativeDataPacket(String topic, String sourceType, String sourceId,
            String scope, long timestamp, double confidence, Object payload) {
        this.topic = topic;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.scope = scope;
        this.timestamp = timestamp;
        this.confidence = confidence;
        this.payload = payload;
    }
}
