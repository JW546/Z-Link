package protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * V2X 协议适配器
 * 
 * Vehicle-to-Everything (V2X) 车联万物通信协议
 * 实现车与车(V2V)、车与基础设施(V2I)、车与行人(V2P)、车与网络(V2N)的通信
 * 
 * 主要应用场景：
 * - 智能网联汽车
 * - 低空经济（无人机与车辆协同）
 * - 车路协同系统
 * - 比亚迪"灵鸢"系统
 * 
 * 基础安全消息(BSM)遵循J2735标准
 * 
 * @author protocol-agent
 */
public class V2XAdapter implements ProtocolRegistry.ProtocolAdapter {
    
    // 适配器信息
    private static final String NAME = "V2X";
    private static final String VERSION = "IEEE 802.11p / C-V2X";
    private static final String DESCRIPTION = "Vehicle-to-Everything - 车联万物通信协议";
    
    // V2X消息类型
    public static final int MSG_TYPE_BSM = 0x1234;      // 基础安全消息
    public static final int MSG_TYPE_MAP = 0x8002;     // 地图消息
    public static final int MSG_TYPE_RSI = 0x8003;     // 道路安全消息
    public static final int MSG_TYPE_RSM = 0x8001;     // 路侧单元消息
    public static final int MSG_TYPE_SPAT = 0x8004;    // 信号灯相位消息
    
    // V2X通信类型
    public enum V2XType {
        V2V,  // 车对车
        V2I,  // 车对基础设施
        V2P,  // 车对行人
        V2N   // 车对网络
    }
    
    // 车辆状态
    private String vehicleId;
    private double latitude;
    private double longitude;
    private double elevation;
    private double speed;
    private double heading;
    private long timestamp;
    
    // 消息计数器
    private long messagesSent;
    private long messagesReceived;
    
    /**
     * V2X消息基类
     */
    public static class V2XMessage {
        protected int messageType;
        protected long messageId;
        protected long timestamp;
        protected String sourceId;
        protected int priority;
        
        public V2XMessage() {
            this.messageId = System.currentTimeMillis();
            this.timestamp = System.currentTimeMillis();
            this.priority = 6;  // 默认优先级
        }
        
        // Getters and Setters
        public int getMessageType() { return messageType; }
        public void setMessageType(int messageType) { this.messageType = messageType; }
        public long getMessageId() { return messageId; }
        public void setMessageId(long messageId) { this.messageId = messageId; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }
    
    /**
     * 基础安全消息 (Basic Safety Message)
     * J2735标准定义，用于V2V通信
     */
    public static class BSMMessage extends V2XMessage {
        // 临时ID (每帧变化)
        private int tempId;
        
        // 位置数据
        private double latitude;      // WGS84纬度 (1/10微度)
        private double longitude;      // WGS84经度 (1/10微度)
        private double elevation;     // 椭圆高程 (0.1米)
        
        // 运动数据
        private double speed;         // 速度 (0.02 m/s)
        private double heading;       // 航向角 (0.0125度)
        private double acceleration;  // 纵向加速度
        private double steering;      // 方向盘转角
        
        // 车辆数据
        private int vehicleLength;     // 车辆长度 (1cm)
        private int vehicleWidth;     // 车辆宽度 (1cm)
        private int vehicleClass;     // 车辆类型
        
        // 时间精度
        private int secMark;          // 毫秒精度
        
        public BSMMessage() {
            super();
            this.messageType = MSG_TYPE_BSM;
            this.tempId = (int) (Math.random() * 0xFFFF);
        }
        
        // 简化构造
        public BSMMessage(double lat, double lon, double speed, double heading) {
            this();
            this.latitude = lat;
            this.longitude = lon;
            this.speed = speed;
            this.heading = heading;
        }
        
        public int getTempId() { return tempId; }
        public void setTempId(int tempId) { this.tempId = tempId; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getElevation() { return elevation; }
        public void setElevation(double elevation) { this.elevation = elevation; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public double getHeading() { return heading; }
        public void setHeading(double heading) { this.heading = heading; }
        public double getAcceleration() { return acceleration; }
        public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
        public double getSteering() { return steering; }
        public void setSteering(double steering) { this.steering = steering; }
        public int getVehicleLength() { return vehicleLength; }
        public void setVehicleLength(int vehicleLength) { this.vehicleLength = vehicleLength; }
        public int getVehicleWidth() { return vehicleWidth; }
        public void setVehicleWidth(int vehicleWidth) { this.vehicleWidth = vehicleWidth; }
        public int getVehicleClass() { return vehicleClass; }
        public void setVehicleClass(int vehicleClass) { this.vehicleClass = vehicleClass; }
        public int getSecMark() { return secMark; }
        public void setSecMark(int secMark) { this.secMark = secMark; }
        
        @Override
        public String toString() {
            return String.format(
                "BSM[id=%04X, lat=%.6f, lon=%.6f, speed=%.1fkm/h, heading=%.1fdeg]",
                tempId, latitude, longitude, speed * 3.6, heading
            );
        }
    }
    
    /**
     * V2I消息 - 车对基础设施
     */
    public static class V2IMessage extends V2XMessage {
        private String infrastructureId;
        private double latitude;
        private double longitude;
        private String messageContent;
        
        public V2IMessage() {
            super();
            this.messageType = MSG_TYPE_RSI;
        }
        
        public String getInfrastructureId() { return infrastructureId; }
        public void setInfrastructureId(String infrastructureId) { this.infrastructureId = infrastructureId; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getMessageContent() { return messageContent; }
        public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    }
    
    /**
     * V2V消息 - 车对车通信包装
     */
    public static class V2VMessage extends V2XMessage {
        private BSMMessage bsm;
        private String targetVehicleId;
        private double distance;
        
        public V2VMessage() {
            super();
        }
        
        public V2VMessage(BSMMessage bsm) {
            this();
            this.bsm = bsm;
        }
        
        public BSMMessage getBsm() { return bsm; }
        public void setBsm(BSMMessage bsm) { this.bsm = bsm; }
        public String getTargetVehicleId() { return targetVehicleId; }
        public void setTargetVehicleId(String targetVehicleId) { this.targetVehicleId = targetVehicleId; }
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
    }
    
    /**
     * 构造函数
     */
    public V2XAdapter() {
        this.vehicleId = "VEH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.latitude = 29.5647;   // 重庆默认位置
        this.longitude = 106.5507;
        this.elevation = 0;
        this.speed = 0;
        this.heading = 0;
        this.timestamp = System.currentTimeMillis();
        this.messagesSent = 0;
        this.messagesReceived = 0;
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public byte[] encode(Object data) {
        if (data instanceof V2XMessage) {
            return encodeV2XMessage((V2XMessage) data);
        }
        
        if (data instanceof BSMMessage) {
            return encodeBSM((BSMMessage) data);
        }
        
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            return encodeFromMap(map);
        }
        
        return data.toString().getBytes();
    }
    
    /**
     * 编码V2X消息
     */
    private byte[] encodeV2XMessage(V2XMessage message) {
        if (message instanceof BSMMessage) {
            return encodeBSM((BSMMessage) message);
        }
        
        // 通用编码
        String json = messageToJson(message);
        return json.getBytes();
    }
    
    /**
     * 编码BSM消息为字节数组
     */
    private byte[] encodeBSM(BSMMessage bsm) {
        // 简化实现：使用字节数组模拟J2735编码
        int size = 50;  // BSM典型大小
        byte[] frame = new byte[size];
        int offset = 0;
        
        // 消息类型 (2字节)
        frame[offset++] = (byte) (MSG_TYPE_BSM & 0xFF);
        frame[offset++] = (byte) ((MSG_TYPE_BSM >> 8) & 0xFF);
        
        // 临时ID (2字节)
        frame[offset++] = (byte) (bsm.getTempId() & 0xFF);
        frame[offset++] = (byte) ((bsm.getTempId() >> 8) & 0xFF);
        
        // 时间戳 (8字节)
        long ts = bsm.getTimestamp();
        for (int i = 0; i < 8; i++) {
            frame[offset++] = (byte) ((ts >> (i * 8)) & 0xFF);
        }
        
        // 纬度 (8字节 double)
        long lat = (long) (bsm.getLatitude() * 1e7);
        for (int i = 0; i < 8; i++) {
            frame[offset++] = (byte) ((lat >> (i * 8)) & 0xFF);
        }
        
        // 经度 (8字节 double)
        long lon = (long) (bsm.getLongitude() * 1e7);
        for (int i = 0; i < 8; i++) {
            frame[offset++] = (byte) ((lon >> (i * 8)) & 0xFF);
        }
        
        // 速度 (2字节)
        int spd = (int) (bsm.getSpeed() / 0.02);
        frame[offset++] = (byte) (spd & 0xFF);
        frame[offset++] = (byte) ((spd >> 8) & 0xFF);
        
        // 航向 (2字节)
        int hdg = (int) (bsm.getHeading() / 0.0125);
        frame[offset++] = (byte) (hdg & 0xFF);
        frame[offset++] = (byte) ((hdg >> 8) & 0xFF);
        
        return Arrays.copyOf(frame, offset);
    }
    
    /**
     * 从Map编码
     */
    private byte[] encodeFromMap(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "BSM");
        
        if ("BSM".equalsIgnoreCase(type)) {
            BSMMessage bsm = new BSMMessage();
            
            if (map.containsKey("latitude")) {
                bsm.setLatitude(((Number) map.get("latitude")).doubleValue());
            }
            if (map.containsKey("longitude")) {
                bsm.setLongitude(((Number) map.get("longitude")).doubleValue());
            }
            if (map.containsKey("speed")) {
                bsm.setSpeed(((Number) map.get("speed")).doubleValue());
            }
            if (map.containsKey("heading")) {
                bsm.setHeading(((Number) map.get("heading")).doubleValue());
            }
            
            return encodeBSM(bsm);
        }
        
        return messageToJson(map).getBytes();
    }
    
    @Override
    public Object decode(byte[] rawData) {
        messagesReceived++;
        
        if (rawData == null || rawData.length < 4) {
            return null;
        }
        
        // 解析消息类型
        int msgType = (rawData[0] & 0xFF) | ((rawData[1] & 0xFF) << 8);
        
        switch (msgType) {
            case MSG_TYPE_BSM:
                return decodeBSM(rawData);
            default:
                // 通用解析
                Map<String, Object> msg = new HashMap<>();
                msg.put("message_type", msgType);
                msg.put("raw_data", rawData.length + " bytes");
                return msg;
        }
    }
    
    /**
     * 解码BSM消息
     */
    private BSMMessage decodeBSM(byte[] data) {
        BSMMessage bsm = new BSMMessage();
        int offset = 2;  // 跳过消息类型
        
        // 临时ID
        int tempId = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        bsm.setTempId(tempId);
        offset += 2;
        
        // 时间戳
        long ts = 0;
        for (int i = 0; i < 8 && offset + i < data.length; i++) {
            ts |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
        }
        bsm.setTimestamp(ts);
        offset += 8;
        
        // 纬度
        if (offset + 8 <= data.length) {
            long lat = 0;
            for (int i = 0; i < 8; i++) {
                lat |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
            }
            bsm.setLatitude(lat / 1e7);
            offset += 8;
        }
        
        // 经度
        if (offset + 8 <= data.length) {
            long lon = 0;
            for (int i = 0; i < 8; i++) {
                lon |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
            }
            bsm.setLongitude(lon / 1e7);
            offset += 8;
        }
        
        // 速度
        if (offset + 2 <= data.length) {
            int spd = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            bsm.setSpeed(spd * 0.02);
            offset += 2;
        }
        
        // 航向
        if (offset + 2 <= data.length) {
            int hdg = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            bsm.setHeading(hdg * 0.0125);
        }
        
        System.out.println("[V2X] 解码BSM: " + bsm);
        return bsm;
    }
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        
        // 检查消息类型
        int msgType = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        return msgType == MSG_TYPE_BSM || msgType == MSG_TYPE_MAP || 
               msgType == MSG_TYPE_RSI || msgType == MSG_TYPE_RSM ||
               msgType == MSG_TYPE_SPAT;
    }
    
    /**
     * 创建BSM消息
     */
    public BSMMessage createBSM() {
        BSMMessage bsm = new BSMMessage(latitude, longitude, speed, heading);
        bsm.setSourceId(vehicleId);
        bsm.setElevation(elevation);
        return bsm;
    }
    
    /**
     * 创建V2V消息
     */
    public V2VMessage createV2VMessage() {
        V2VMessage v2v = new V2VMessage(createBSM());
        v2v.setSourceId(vehicleId);
        return v2v;
    }
    
    /**
     * 创建V2I消息
     */
    public V2IMessage createV2IMessage(String infrastructureId, String content) {
        V2IMessage v2i = new V2IMessage();
        v2i.setSourceId(vehicleId);
        v2i.setInfrastructureId(infrastructureId);
        v2i.setMessageContent(content);
        v2i.setLatitude(latitude);
        v2i.setLongitude(longitude);
        return v2i;
    }
    
    /**
     * 发送V2V消息
     */
    public void sendV2V(V2VMessage message) {
        byte[] encoded = encode(message);
        System.out.println("[V2X-V2V] 发送车辆安全消息: " + message.getBsm());
        System.out.println("  编码后: " + encoded.length + " bytes");
        messagesSent++;
    }
    
    /**
     * 发送V2I消息
     */
    public void sendV2I(V2IMessage message) {
        byte[] encoded = encode(message);
        System.out.println("[V2X-V2I] 发送到基础设施: " + message.getInfrastructureId());
        System.out.println("  内容: " + message.getMessageContent());
        messagesSent++;
    }
    
    /**
     * 广播BSM消息
     */
    public void broadcastBSM() {
        BSMMessage bsm = createBSM();
        byte[] encoded = encodeBSM(bsm);
        System.out.println("[V2X] 广播BSM: " + bsm);
        System.out.println("  编码后: " + encoded.length + " bytes");
        messagesSent++;
    }
    
    /**
     * 更新车辆状态
     */
    public void updateVehicleState(double lat, double lon, double spd, double hdg) {
        this.latitude = lat;
        this.longitude = lon;
        this.speed = spd;
        this.heading = hdg;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("vehicle_id", vehicleId);
        stats.put("messages_sent", messagesSent);
        stats.put("messages_received", messagesReceived);
        stats.put("latitude", latitude);
        stats.put("longitude", longitude);
        return stats;
    }
    
    private String messageToJson(Object msg) {
        return msg.toString();
    }
    
    private String messageToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 演示V2X功能
     */
    public static void demo() {
        System.out.println("\n========== V2X 协议演示 ==========");
        
        V2XAdapter adapter = new V2XAdapter();
        
        // 更新车辆状态
        System.out.println("\n--- 模拟车辆状态更新 ---");
        adapter.updateVehicleState(29.564709, 106.550781, 36.0, 45.0);  // 36km/h, 45度
        
        // 广播BSM
        System.out.println("\n--- 广播基础安全消息 ---");
        adapter.broadcastBSM();
        
        // V2V通信
        System.out.println("\n--- V2V 车对车通信 ---");
        V2VMessage v2vMsg = adapter.createV2VMessage();
        adapter.sendV2V(v2vMsg);
        
        // V2I通信
        System.out.println("\n--- V2I 车对基础设施通信 ---");
        V2IMessage v2iMsg = adapter.createV2IMessage("RSU-001", "路况预警:前方500米有事故");
        adapter.sendV2I(v2iMsg);
        
        // 模拟接收
        System.out.println("\n--- 接收测试 ---");
        byte[] testBSM = new byte[]{
            0x34, 0x12,  // 消息类型 BSM
            (byte) 0xAB, (byte) 0xCD,  // 临时ID
            0x00, (byte) 0xE8, 0x76, 0x4B, (byte) 0x9C, (byte) 0xF5, (byte) 0xD8, 0x01,  // 时间戳
            0x00, (byte) 0xBC, 0x61, (byte) 0xEA, 0x00, 0x00, 0x00, 0x00,  // 纬度
            0x00, (byte) 0x8E, (byte) 0xF5, 0x24, 0x00, 0x00, 0x00, 0x00,  // 经度
            0x00, 0x64,  // 速度
            0x00, (byte) 0x90  // 航向
        };
        adapter.decode(testBSM);
        
        // 统计
        System.out.println("\n--- 统计信息 ---");
        Map<String, Object> stats = adapter.getStatistics();
        stats.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        
        System.out.println("\n========== 演示完成 ==========\n");
    }
}
