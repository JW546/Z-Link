package protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CAN-Bus 协议适配器
 * 
 * 控制器局域网 (Controller Area Network) 是车规级总线协议
 * 广泛应用于汽车电子、无人机和工业控制系统
 * 
 * 功能特性：
 * - 标准帧 (11位CAN ID) 和扩展帧 (29位CAN ID) 解析
 * - CAN ID 过滤和掩码匹配
 * - 数据帧和远程帧支持
 * - 简单模拟实现（不依赖真实硬件）
 * 
 * @author protocol-agent
 */
public class CANBusAdapter implements protocol.ProtocolRegistry.ProtocolAdapter {
    
    private static final String NAME = "CAN-Bus";
    private static final String VERSION = "2.0";
    private static final String DESCRIPTION = "Controller Area Network - 车规级总线协议";
    
    public static final int STANDARD_FRAME_ID_BITS = 11;
    public static final int EXTENDED_FRAME_ID_BITS = 29;
    public static final int MAX_DLC = 8;
    public static final int MAX_STANDARD_ID = 0x7FF;
    public static final int MAX_EXTENDED_ID = 0x1FFFFFFF;
    
    private static final byte IDE_BIT = 0x04;
    private static final byte RTR_BIT = 0x40;
    private static final byte DLC_MASK = 0x0F;
    
    private final Set<Integer> acceptedIds;
    private final Map<Integer, Integer> idMasks;
    private long framesSent;
    private long framesReceived;
    private long framesFiltered;
    
    public static class CANFrame {
        private int canId;
        private boolean extendedFrame;
        private boolean remoteFrame;
        private int dlc;
        private byte[] data;
        
        public CANFrame() {
            this.data = new byte[MAX_DLC];
            this.dlc = 0;
        }
        
        public CANFrame(int canId, byte[] data) {
            this.canId = canId;
            this.extendedFrame = canId > MAX_STANDARD_ID;
            this.data = data != null ? Arrays.copyOf(data, Math.min(data.length, MAX_DLC)) : new byte[0];
            this.dlc = this.data.length;
            this.remoteFrame = false;
        }
        
        public int getCanId() { return canId; }
        public void setCanId(int canId) { this.canId = canId; }
        public boolean isExtendedFrame() { return extendedFrame; }
        public void setExtendedFrame(boolean extended) { this.extendedFrame = extended; }
        public boolean isRemoteFrame() { return remoteFrame; }
        public void setRemoteFrame(boolean remote) { this.remoteFrame = remote; }
        public int getDlc() { return dlc; }
        public void setDlc(int dlc) { this.dlc = Math.min(dlc, MAX_DLC); }
        public byte[] getData() { return Arrays.copyOf(data, dlc); }
        public void setData(byte[] data) { 
            this.data = Arrays.copyOf(data, MAX_DLC); 
            this.dlc = Math.min(data.length, MAX_DLC);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CAN Frame [");
            sb.append("ID=0x").append(Integer.toHexString(canId).toUpperCase());
            sb.append(extendedFrame ? "(Ext)" : "(Std)");
            sb.append(", DLC=").append(dlc);
            sb.append(", RTR=").append(remoteFrame);
            sb.append(", Data=");
            for (int i = 0; i < dlc; i++) {
                sb.append(String.format("%02X", data[i] & 0xFF));
                if (i < dlc - 1) sb.append(" ");
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    public CANBusAdapter() {
        this.acceptedIds = ConcurrentHashMap.newKeySet();
        this.idMasks = new ConcurrentHashMap<>();
        this.framesSent = 0;
        this.framesReceived = 0;
        this.framesFiltered = 0;
        this.acceptedIds.add(0);
    }
    
    public void addAcceptableId(int canId) { acceptedIds.add(canId); }
    
    public void addIdMask(int canId, int mask) {
        acceptedIds.add(canId);
        idMasks.put(canId, mask);
    }
    
    private boolean isIdAccepted(int canId) {
        if (acceptedIds.contains(0)) return true;
        if (acceptedIds.contains(canId)) return true;
        for (Map.Entry<Integer, Integer> entry : idMasks.entrySet()) {
            int filterId = entry.getKey();
            int mask = entry.getValue();
            if ((canId & mask) == (filterId & mask)) return true;
        }
        return false;
    }
    
    @Override
    public String getName() { return NAME; }
    
    @Override
    public String getVersion() { return VERSION; }
    
    @Override
    public String getDescription() { return DESCRIPTION; }
    
    @Override
    public byte[] encode(Object data) {
        if (data instanceof CANFrame) {
            CANFrame frame = (CANFrame) data;
            byte[] frameBytes = new byte[14];
            int offset = 0;
            frameBytes[offset++] = (byte) (frame.canId & 0xFF);
            frameBytes[offset++] = (byte) ((frame.canId >> 8) & 0x07);
            byte ctrl = (byte) (frame.dlc & DLC_MASK);
            if (frame.extendedFrame) ctrl |= IDE_BIT;
            if (frame.remoteFrame) ctrl |= RTR_BIT;
            frameBytes[offset++] = ctrl;
            System.arraycopy(frame.data, 0, frameBytes, offset, frame.dlc);
            return Arrays.copyOf(frameBytes, offset + frame.dlc);
        }
        if (data instanceof byte[]) return (byte[]) data;
        return data.toString().getBytes();
    }
    
    @Override
    public Object decode(byte[] rawData) {
        framesReceived++;
        if (rawData == null || rawData.length < 2) return null;
        
        CANFrame frame = new CANFrame();
        frame.canId = (rawData[0] & 0xFF) | ((rawData[1] & 0xE0) << 8);
        frame.extendedFrame = frame.canId > MAX_STANDARD_ID;
        if (rawData.length > 2) {
            byte ctrl = rawData[2];
            frame.remoteFrame = (ctrl & RTR_BIT) != 0;
            frame.dlc = Math.min(ctrl & DLC_MASK, rawData.length - 3);
            for (int i = 0; i < frame.dlc; i++) {
                frame.data[i] = rawData[3 + i];
            }
        }
        
        if (!isIdAccepted(frame.canId)) {
            framesFiltered++;
            System.out.println("[CAN-Bus] Frame filtered: ID=0x" + Integer.toHexString(frame.canId).toUpperCase());
            return null;
        }
        System.out.println("[CAN-Bus] Received: " + frame);
        return frame;
    }
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 2) return false;
        int dlc = data[2] & DLC_MASK;
        return dlc <= MAX_DLC;
    }
    
    public CANFrame createStandardFrame(int canId, byte[] data) {
        if (canId < 0 || canId > MAX_STANDARD_ID) throw new IllegalArgumentException("Invalid CAN ID");
        return new CANFrame(canId, data);
    }
    
    public CANFrame createExtendedFrame(int canId, byte[] data) {
        if (canId < 0 || canId > MAX_EXTENDED_ID) throw new IllegalArgumentException("Invalid extended CAN ID");
        CANFrame frame = new CANFrame(canId, data);
        frame.setExtendedFrame(true);
        return frame;
    }
    
    public CANFrame createRemoteFrame(int canId) {
        CANFrame frame = new CANFrame(canId, null);
        frame.setRemoteFrame(true);
        return frame;
    }
    
    public void sendFrame(CANFrame frame) {
        byte[] raw = encode(frame);
        System.out.println("[CAN-Bus] Send: " + frame);
        framesSent++;
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("frames_sent", framesSent);
        stats.put("frames_received", framesReceived);
        stats.put("frames_filtered", framesFiltered);
        return stats;
    }
    
    public static void demo() {
        System.out.println("\n========== CAN-Bus Protocol Demo ==========");
        CANBusAdapter adapter = new CANBusAdapter();
        adapter.addAcceptableId(0x100);
        adapter.addAcceptableId(0x200);
        
        System.out.println("\n--- Standard Frame ---");
        CANFrame stdFrame = adapter.createStandardFrame(0x123, new byte[]{0x11, 0x22, 0x33, 0x44});
        adapter.sendFrame(stdFrame);
        
        System.out.println("\n--- Extended Frame ---");
        CANFrame extFrame = adapter.createExtendedFrame(0x12345678, new byte[]{(byte)0xAA, (byte)0xBB, (byte)0xCC});
        adapter.sendFrame(extFrame);
        
        System.out.println("\n--- Remote Frame ---");
        adapter.sendFrame(adapter.createRemoteFrame(0x200));
        
        System.out.println("\n--- Statistics ---");
        Map<String, Object> stats = adapter.getStatistics();
        stats.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        System.out.println("\n========== Demo Complete ==========\n");
    }
}
