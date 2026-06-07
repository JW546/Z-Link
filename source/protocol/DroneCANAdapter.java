package protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DroneCAN 协议适配器
 * DroneCAN (formerly UAVCAN) 是一种用于航空航天系统的轻量级CAN总线协议
 */
public class DroneCANAdapter implements protocol.ProtocolRegistry.ProtocolAdapter {
    
    private static final String NAME = "DroneCAN";
    private static final String VERSION = "1.0";
    private static final String DESCRIPTION = "DroneCAN Protocol - 无人机CAN总线协议";
    public static final int DUC_MAGIC = 0x0D27;
    
    public enum Priority {
        CRITICAL(0), HIGH(8), ELEVATED(16), NORMAL(24), LOW(31);
        final int value;
        Priority(int value) { this.value = value; }
    }
    
    public static final byte SERVICE_ID_MASK = 0x3F;
    public static final int MSG_ID_HEARTBEAT = 0x1000;
    public static final int MSG_ID_GET_NODE_INFO = 0x1000;
    
    private int nodeId = 1;
    private String nodeName = "org.dsdl.DSLCode.DroneNode";
    private int nodeHealth = 0;
    private int nodeMode = 1;
    private final Map<Integer, NodeInfo> discoveredNodes = new ConcurrentHashMap<>();
    private final AtomicInteger messageId = new AtomicInteger(0);
    private long messagesSent = 0;
    private long messagesReceived = 0;
    private long heartbeatsSent = 0;
    
    public static class NodeInfo {
        private int nodeId;
        private String name;
        private int health;
        private int mode;
        private long lastHeartbeat;
        
        public NodeInfo(int nodeId, String name) {
            this.nodeId = nodeId;
            this.name = name;
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        public int getNodeId() { return nodeId; }
        public String getName() { return name; }
        public int getHealth() { return health; }
        public void setHealth(int h) { this.health = h; }
        public int getMode() { return mode; }
        public void setMode(int m) { this.mode = m; }
        public boolean isOnline() { return System.currentTimeMillis() - lastHeartbeat < 5000; }
        
        @Override
        public String toString() {
            return String.format("Node[%d:%s, online=%s]", nodeId, name, isOnline());
        }
    }
    
    public static class DroneCANMessage {
        private int priority = Priority.NORMAL.value;
        private boolean isService = false;
        private byte serviceId;
        private int sourceNodeId = 127;
        private int dataTypeId;
        private byte[] payload;
        
        public int getPriority() { return priority; }
        public void setPriority(int p) { this.priority = p; }
        public boolean isService() { return isService; }
        public void setService(boolean s) { this.isService = s; }
        public int getSourceNodeId() { return sourceNodeId; }
        public void setSourceNodeId(int id) { this.sourceNodeId = id; }
        public int getDataTypeId() { return dataTypeId; }
        public void setDataTypeId(int id) { this.dataTypeId = id; }
        public byte[] getPayload() { return payload; }
        public void setPayload(byte[] p) { this.payload = p; }
        
        @Override
        public String toString() {
            return String.format("DroneCAN[pri=%d, %s, dtid=0x%04X, src=%d]",
                priority, isService ? "SVC" : "MSG", dataTypeId, sourceNodeId);
        }
    }
    
    public static class Heartbeat {
        private int uptime;
        private int health;
        private int mode;
        private int vendorId;
        
        public Heartbeat() { this.uptime = 0; this.health = 0; this.mode = 1; }
        public Heartbeat(int uptime, int health, int mode) {
            this(); this.uptime = uptime; this.health = health; this.mode = mode;
        }
        
        public byte[] toBytes() {
            return new byte[]{
                (byte)(uptime & 0xFF), (byte)((uptime >> 8) & 0xFF),
                (byte)((uptime >> 16) & 0xFF), (byte)((uptime >> 24) & 0xFF),
                (byte)((health & 0x03) | ((mode & 0x07) << 2)),
                (byte)(vendorId & 0xFF), (byte)((vendorId >> 8) & 0xFF)
            };
        }
        
        @Override
        public String toString() {
            String[] hStr = {"OK", "WARNING", "ERROR", "CRITICAL"};
            String[] mStr = {"INIT", "MAIN", "MAINT", "UPDATE"};
            return String.format("Heartbeat[uptime=%ds, health=%s, mode=%s]",
                uptime, hStr[health], mStr[Math.min(mode, 3)]);
        }
    }
    
    public DroneCANAdapter() {}
    
    public DroneCANAdapter(int nodeId, String nodeName) {
        this(); this.nodeId = nodeId; this.nodeName = nodeName;
    }
    
    @Override public String getName() { return NAME; }
    @Override public String getVersion() { return VERSION; }
    @Override public String getDescription() { return DESCRIPTION; }
    
    @Override
    public byte[] encode(Object data) {
        if (data instanceof DroneCANMessage) {
            DroneCANMessage msg = (DroneCANMessage) data;
            byte[] frame = new byte[7 + (msg.payload != null ? msg.payload.length : 0)];
            frame[0] = (byte)(DUC_MAGIC & 0xFF);
            frame[1] = (byte)((DUC_MAGIC >> 8) & 0xFF);
            frame[6] = (byte)(messageId.incrementAndGet() & 0x1F);
            if (msg.payload != null && msg.payload.length > 0) {
                System.arraycopy(msg.payload, 0, frame, 7, msg.payload.length);
            }
            messagesSent++;
            return frame;
        }
        return data.toString().getBytes();
    }
    
    @Override
    public Object decode(byte[] rawData) {
        messagesReceived++;
        if (rawData == null || rawData.length < 7) return null;
        int magic = (rawData[0] & 0xFF) | ((rawData[1] & 0xFF) << 8);
        if (magic != DUC_MAGIC) {
            System.out.println("[DroneCAN] Invalid magic: 0x" + Integer.toHexString(magic));
            return null;
        }
        DroneCANMessage msg = new DroneCANMessage();
        msg.setSourceNodeId((rawData[5] >> 1) & 0x7F);
        System.out.println("[DroneCAN] Decode from Node " + msg.getSourceNodeId());
        return msg;
    }
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 7) return false;
        int magic = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        return magic == DUC_MAGIC;
    }
    
    public DroneCANMessage createHeartbeatMessage() {
        DroneCANMessage msg = new DroneCANMessage();
        msg.setPriority(Priority.HIGH.value);
        msg.setDataTypeId(MSG_ID_HEARTBEAT & 0xFF);
        msg.setSourceNodeId(this.nodeId);
        msg.setPayload(new Heartbeat((int)(System.currentTimeMillis() / 1000), nodeHealth, nodeMode).toBytes());
        heartbeatsSent++;
        return msg;
    }
    
    public DroneCANMessage createDataMessage(int dataTypeId, byte[] data) {
        DroneCANMessage msg = new DroneCANMessage();
        msg.setPriority(Priority.NORMAL.value);
        msg.setDataTypeId(dataTypeId);
        msg.setSourceNodeId(this.nodeId);
        msg.setPayload(data);
        return msg;
    }
    
    public void sendHeartbeat() {
        DroneCANMessage hb = createHeartbeatMessage();
        encode(hb);
    }
    
    public void discoverNodes() {
        System.out.println("[DroneCAN] Node discovery...");
        DroneCANMessage msg = new DroneCANMessage();
        msg.setPriority(Priority.ELEVATED.value);
        msg.setDataTypeId(MSG_ID_GET_NODE_INFO & 0xFF);
        msg.setSourceNodeId(127);
        encode(msg);
    }
    
    public Map<Integer, NodeInfo> getDiscoveredNodes() { return new HashMap<>(discoveredNodes); }
    public Map<Integer, NodeInfo> getOnlineNodes() {
        Map<Integer, NodeInfo> online = new HashMap<>();
        for (Map.Entry<Integer, NodeInfo> e : discoveredNodes.entrySet()) {
            if (e.getValue().isOnline()) online.put(e.getKey(), e.getValue());
        }
        return online;
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("node_id", nodeId);
        stats.put("messages_sent", messagesSent);
        stats.put("messages_received", messagesReceived);
        stats.put("discovered_nodes", discoveredNodes.size());
        return stats;
    }
    
    public static void demo() {
        System.out.println("\n========== DroneCAN Demo ==========");
        DroneCANAdapter adapter = new DroneCANAdapter(10, "test-node");
        
        System.out.println("\n--- Heartbeat ---");
        adapter.sendHeartbeat();
        
        System.out.println("\n--- Node Discovery ---");
        adapter.discoverNodes();
        
        System.out.println("\n--- Data Message ---");
        adapter.encode(adapter.createDataMessage(0x2001, new byte[]{(byte)0x01, (byte)0x02}));
        
        System.out.println("\n--- Statistics ---");
        adapter.getStatistics().forEach((k, v) -> System.out.println("  " + k + ": " + v));
        System.out.println("\n========== Demo Complete ==========\n");
    }
}
