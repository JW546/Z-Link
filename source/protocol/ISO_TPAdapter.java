package protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ISO-TP 协议适配器
 * 
 * ISO 15765-2 传输层协议
 * 用于在CAN总线上传输大于8字节的数据
 * 
 * 特点：
 * - 单帧传输（SF）：数据 <= 7字节（标准CAN）
 * - 首帧（FF）：数据 > 7字节时的第一帧
 * - 连续帧（CF）：首帧后的后续帧
 * - 流控帧（FC）：接收方发送的流控信息
 * 
 * 应用场景：
 * - 汽车OBD诊断
 * - ECU远程刷写
 * - 车载网络诊断
 * 
 * @author protocol-agent
 */
public class ISO_TPAdapter implements ProtocolRegistry.ProtocolAdapter {
    
    // 适配器信息
    private static final String NAME = "ISO-TP";
    private static final String VERSION = "ISO 15765-2";
    private static final String DESCRIPTION = "ISO Transport Protocol - 汽车诊断传输协议";
    
    // ISO-TP N_PCI (Network Protocol Control Information)
    public static final byte PCI_SINGLE_FRAME = 0x00;   // 单帧
    public static final byte PCI_FIRST_FRAME = 0x10;     // 首帧 (高4位)
    public static final byte PCI_CONSECUTIVE_FRAME = 0x20;  // 连续帧
    public static final byte PCI_FLOW_CONTROL = 0x30;     // 流控帧
    
    // 流控状态
    public static final byte FC_CTS = 0x00;   // Continue to Send
    public static final byte FC_WAIT = 0x01;  // Wait
    public static final byte FC_OVERFLOW = 0x02;  // Overflow/Abort
    
    // CAN数据域大小（标准帧）
    public static final int CAN_MAX_DLEN = 8;
    
    // 单帧数据长度限制（8字节CAN帧，第一字节用于PCI）
    public static final int SF_MAX_DLEN = 7;
    
    // 多帧传输参数
    private int blockSize = 0;        // 块大小（0表示无限制）
    private int stMin = 0;            // 连续帧最小间隔时间(ms)
    
    // 接收缓冲
    private final Map<Integer, ReceiveSession> activeSessions;
    
    /**
     * 接收会话（用于多帧重组）
     */
    private static class ReceiveSession {
        int canId;
        int totalLength;
        int receivedLength;
        byte[] data;
        int expectedSequence;
        long timestamp;
        boolean isExtendedAddressing;
        byte addressingFormat;
        
        ReceiveSession(int canId, int totalLength, byte addressingFormat) {
            this.canId = canId;
            this.totalLength = totalLength;
            this.receivedLength = 0;
            this.data = new byte[4095];  // ISO-TP最大数据长度
            this.expectedSequence = 1;
            this.timestamp = System.currentTimeMillis();
            this.addressingFormat = addressingFormat;
            this.isExtendedAddressing = (addressingFormat == 0x01);
        }
        
        void addData(byte[] frameData, int offset, int length) {
            System.arraycopy(frameData, offset, data, receivedLength, length);
            receivedLength += length;
        }
        
        boolean isComplete() {
            return receivedLength >= totalLength;
        }
        
        byte[] getData() {
            return Arrays.copyOf(data, receivedLength);
        }
    }
    
    /**
     * ISO-TP消息
     */
    public static class TPMessage {
        private int canId;
        private byte[] data;
        private int totalLength;
        private boolean isMultiFrame;
        
        public TPMessage() {
        }
        
        public TPMessage(int canId, byte[] data) {
            this.canId = canId;
            this.data = data;
            this.totalLength = data != null ? data.length : 0;
            this.isMultiFrame = totalLength > SF_MAX_DLEN;
        }
        
        public int getCanId() { return canId; }
        public void setCanId(int canId) { this.canId = canId; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { 
            this.data = data; 
            this.totalLength = data != null ? data.length : 0;
            this.isMultiFrame = totalLength > SF_MAX_DLEN;
        }
        public int getTotalLength() { return totalLength; }
        public boolean isMultiFrame() { return isMultiFrame; }
        
        @Override
        public String toString() {
            return String.format("TPMessage[canId=0x%03X, len=%d, multi=%s]",
                canId, totalLength, isMultiFrame);
        }
    }
    
    /**
     * ISO-TP帧
     */
    public static class TPFrame {
        byte pciType;
        byte[] data;
        
        public TPFrame(byte pciType, byte[] data) {
            this.pciType = pciType;
            this.data = data;
        }
        
        public byte getPciType() { return pciType; }
        public byte[] getData() { return data; }
    }
    
    /**
     * 流控参数
     */
    public static class FlowControl {
        byte fs;     // Flow Status
        byte bs;     // Block Size
        byte stmin;  // Separation Time Minimum
        
        public FlowControl(byte fs, byte bs, byte stmin) {
            this.fs = fs;
            this.bs = bs;
            this.stmin = stmin;
        }
        
        @Override
        public String toString() {
            return String.format("FlowControl[fs=%d, bs=%d, stmin=%d]", fs, bs, stmin);
        }
    }
    
    /**
     * 构造函数
     */
    public ISO_TPAdapter() {
        this.activeSessions = new ConcurrentHashMap<>();
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
        if (data instanceof TPMessage) {
            TPMessage msg = (TPMessage) data;
            return encodeMessage(msg);
        }
        
        if (data instanceof byte[]) {
            // 默认使用标准ID 0x7E0（诊断会话）
            TPMessage msg = new TPMessage(0x7E0, (byte[]) data);
            return encodeMessage(msg);
        }
        
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            return encodeFromMap(map);
        }
        
        return data.toString().getBytes();
    }
    
    /**
     * 编码ISO-TP消息
     */
    private byte[] encodeMessage(TPMessage msg) {
        if (!msg.isMultiFrame()) {
            // 单帧传输
            return encodeSingleFrame(msg);
        } else {
            // 多帧传输 - 返回首帧
            return encodeFirstFrame(msg);
        }
    }
    
    /**
     * 编码单帧
     */
    private byte[] encodeSingleFrame(TPMessage msg) {
        byte[] canFrame = new byte[CAN_MAX_DLEN];
        byte[] data = msg.getData();
        
        // N_PCI + 数据
        int dlen = Math.min(data.length, SF_MAX_DLEN);
        canFrame[0] = (byte) ((PCI_SINGLE_FRAME) | dlen);
        System.arraycopy(data, 0, canFrame, 1, dlen);
        
        return canFrame;
    }
    
    /**
     * 编码首帧
     */
    private byte[] encodeFirstFrame(TPMessage msg) {
        byte[] canFrame = new byte[CAN_MAX_DLEN];
        byte[] data = msg.getData();
        int totalLen = data.length;
        
        // 首帧格式: FF_DL高字节在N_PCI低字节, FF_DL低字节在数据第一个字节
        canFrame[0] = (byte) ((PCI_FIRST_FRAME) | ((totalLen >> 8) & 0x0F));
        canFrame[1] = (byte) (totalLen & 0xFF);
        
        // 前6字节数据
        int copyLen = Math.min(6, data.length);
        System.arraycopy(data, 0, canFrame, 2, copyLen);
        
        System.out.println("[ISO-TP] 首帧编码: " + formatHex(canFrame) + 
                          " (总长度=" + totalLen + ")");
        return canFrame;
    }
    
    /**
     * 编码连续帧
     */
    public byte[] encodeConsecutiveFrame(TPMessage msg, int sequenceNumber) {
        byte[] canFrame = new byte[CAN_MAX_DLEN];
        byte[] data = msg.getData();
        int offset = 6 * sequenceNumber;  // 每帧7字节数据
        
        // N_PCI + 序列号
        canFrame[0] = (byte) (PCI_CONSECUTIVE_FRAME | (sequenceNumber & 0x0F));
        
        // 7字节数据
        int copyLen = Math.min(7, data.length - offset);
        if (copyLen > 0) {
            System.arraycopy(data, offset, canFrame, 1, copyLen);
        }
        
        System.out.println("[ISO-TP] 连续帧[" + sequenceNumber + "]编码: " + formatHex(canFrame));
        return canFrame;
    }
    
    /**
     * 编码流控帧
     */
    public byte[] encodeFlowControl(FlowControl fc) {
        byte[] canFrame = new byte[CAN_MAX_DLEN];
        
        canFrame[0] = (byte) (PCI_FLOW_CONTROL | (fc.fs & 0x0F));
        canFrame[1] = fc.bs;  // Block Size
        canFrame[2] = fc.stmin;  // STmin
        
        System.out.println("[ISO-TP] 流控帧编码: " + formatHex(canFrame));
        return canFrame;
    }
    
    /**
     * 从Map编码
     */
    private byte[] encodeFromMap(Map<String, Object> map) {
        int canId = 0x7E0;  // 默认诊断ID
        byte[] data = new byte[0];
        
        if (map.containsKey("can_id")) {
            Object id = map.get("can_id");
            canId = id instanceof Number ? ((Number) id).intValue() : Integer.parseInt(id.toString());
        }
        
        if (map.containsKey("data")) {
            Object d = map.get("data");
            if (d instanceof byte[]) {
                data = (byte[]) d;
            } else if (d instanceof Number) {
                data = new byte[]{(byte) ((Number) d).intValue()};
            }
        }
        
        TPMessage msg = new TPMessage(canId, data);
        return encodeMessage(msg);
    }
    
    @Override
    public Object decode(byte[] rawData) {
        if (rawData == null || rawData.length < 1) {
            return null;
        }
        
        byte pciNibble = (byte) (rawData[0] & 0xF0);
        
        switch (pciNibble) {
            case PCI_SINGLE_FRAME:
                return decodeSingleFrame(rawData);
            case PCI_FIRST_FRAME:
                return decodeFirstFrame(rawData);
            case PCI_CONSECUTIVE_FRAME:
                return decodeConsecutiveFrame(rawData);
            case PCI_FLOW_CONTROL:
                return decodeFlowControl(rawData);
            default:
                System.out.println("[ISO-TP] 未知PCI类型: 0x" + String.format("%02X", pciNibble));
                return null;
        }
    }
    
    /**
     * 解码单帧
     */
    private TPMessage decodeSingleFrame(byte[] data) {
        int dlen = data[0] & 0x0F;
        byte[] payload = new byte[dlen];
        
        if (dlen > 0 && data.length > 1) {
            System.arraycopy(data, 1, payload, 0, Math.min(dlen, data.length - 1));
        }
        
        System.out.println("[ISO-TP] 解码单帧: 长度=" + dlen + ", 数据=" + formatHex(payload));
        
        TPMessage msg = new TPMessage(0, payload);
        return msg;
    }
    
    /**
     * 解码首帧
     */
    private TPMessage decodeFirstFrame(byte[] data) {
        int totalLenHigh = data[0] & 0x0F;
        int totalLenLow = data[1] & 0xFF;
        int totalLen = (totalLenHigh << 8) | totalLenLow;
        
        // 获取前6字节数据
        byte[] partialData = new byte[Math.min(6, data.length - 2)];
        System.arraycopy(data, 2, partialData, 0, partialData.length);
        
        System.out.println("[ISO-TP] 解码首帧: 总长度=" + totalLen + ", 前导数据=" + formatHex(partialData));
        
        // 创建接收会话
        ReceiveSession session = new ReceiveSession(0, totalLen, (byte) 0);
        session.addData(partialData, 0, partialData.length);
        activeSessions.put(0, session);
        
        // 返回部分消息
        TPMessage msg = new TPMessage(0, partialData);
        return msg;
    }
    
    /**
     * 解码连续帧
     */
    private TPMessage decodeConsecutiveFrame(byte[] data) {
        int sequenceNum = data[0] & 0x0F;
        
        // 获取7字节数据
        byte[] payload = new byte[Math.min(7, data.length - 1)];
        System.arraycopy(data, 1, payload, 0, payload.length);
        
        System.out.println("[ISO-TP] 解码连续帧[" + sequenceNum + "]: " + formatHex(payload));
        
        // 更新会话
        ReceiveSession session = activeSessions.get(0);
        if (session != null) {
            session.addData(payload, 0, payload.length);
            
            if (session.isComplete()) {
                activeSessions.remove(0);
                System.out.println("[ISO-TP] 多帧重组完成: " + session.receivedLength + " bytes");
                return new TPMessage(session.canId, session.getData());
            }
        }
        
        return new TPMessage(0, payload);
    }
    
    /**
     * 解码流控帧
     */
    private FlowControl decodeFlowControl(byte[] data) {
        byte fs = (byte) (data[0] & 0x0F);
        byte bs = data.length > 1 ? data[1] : 0;
        byte stmin = data.length > 2 ? data[2] : 0;
        
        FlowControl fc = new FlowControl(fs, bs, stmin);
        System.out.println("[ISO-TP] 解码流控帧: " + fc);
        
        // 更新发送参数
        this.blockSize = bs & 0xFF;
        this.stMin = stmin & 0xFF;
        
        return fc;
    }
    
    @Override
    public boolean validate(byte[] data) {
        if (data == null || data.length < 1 || data.length > CAN_MAX_DLEN) {
            return false;
        }
        
        byte pciNibble = (byte) (data[0] & 0xF0);
        return pciNibble == PCI_SINGLE_FRAME || 
               pciNibble == PCI_FIRST_FRAME ||
               pciNibble == PCI_CONSECUTIVE_FRAME ||
               pciNibble == PCI_FLOW_CONTROL;
    }
    
    /**
     * 发送ISO-TP消息（完整流程）
     */
    public byte[][] sendMessage(int canId, byte[] data) {
        System.out.println("\n[ISO-TP] 发送消息: CAN ID=0x" + Integer.toHexString(canId) + 
                          ", 长度=" + data.length);
        
        TPMessage msg = new TPMessage(canId, data);
        byte[][] frames = fragmentMessage(msg);
        
        System.out.println("[ISO-TP] 分割为 " + frames.length + " 帧");
        for (int i = 0; i < frames.length; i++) {
            System.out.println("  帧[" + i + "]: " + formatHex(frames[i]));
        }
        
        return frames;
    }
    
    /**
     * 分割消息为多个CAN帧
     */
    private byte[][] fragmentMessage(TPMessage msg) {
        byte[] data = msg.getData();
        
        if (data.length <= SF_MAX_DLEN) {
            // 单帧
            return new byte[][]{encodeSingleFrame(msg)};
        }
        
        // 多帧计算
        int frames = 1 + (data.length - 6 + 6) / 7;  // 首帧 + ceil((len-6)/7)
        byte[][] result = new byte[frames][];
        
        // 首帧
        result[0] = encodeFirstFrame(msg);
        
        // 连续帧
        int offset = 6;
        for (int i = 1; i < frames; i++) {
            byte[] cfData = new byte[Math.min(7, data.length - offset)];
            System.arraycopy(data, offset, cfData, 0, cfData.length);
            result[i] = new byte[CAN_MAX_DLEN];
            result[i][0] = (byte) (PCI_CONSECUTIVE_FRAME | (i & 0x0F));
            System.arraycopy(cfData, 0, result[i], 1, cfData.length);
            offset += 7;
        }
        
        return result;
    }
    
    /**
     * 接收ISO-TP消息（完整流程）
     */
    public TPMessage receiveMessage(byte[] canFrames) {
        System.out.println("\n[ISO-TP] 接收CAN帧序列...");
        
        // 简化实现：直接解析第一个帧
        Object result = decode(canFrames);
        
        if (result instanceof TPMessage) {
            TPMessage msg = (TPMessage) result;
            if (!msg.isMultiFrame()) {
                return msg;
            }
        }
        
        return null;
    }
    
    /**
     * 格式化十六进制输出
     */
    private String formatHex(byte[] data) {
        if (data == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i < data.length - 1) sb.append(" ");
        }
        return sb.toString();
    }
    
    /**
     * 创建诊断请求
     */
    public byte[] createDiagnosticRequest(int serviceId, byte[] data) {
        byte[] payload = new byte[1 + (data != null ? data.length : 0)];
        payload[0] = (byte) serviceId;
        if (data != null) {
            System.arraycopy(data, 0, payload, 1, data.length);
        }
        
        TPMessage msg = new TPMessage(0x7E0, payload);
        return encodeSingleFrame(msg);
    }
    
    /**
     * 创建诊断响应解析
     */
    public byte[] createDiagnosticResponse(int serviceId, byte[] data) {
        byte[] payload = new byte[1 + (data != null ? data.length : 0)];
        payload[0] = (byte) (serviceId | 0x40);  // 响应ID = 请求ID + 0x40
        if (data != null) {
            System.arraycopy(data, 0, payload, 1, data.length);
        }
        
        TPMessage msg = new TPMessage(0x7E8, payload);
        return encodeSingleFrame(msg);
    }
    
    /**
     * 演示ISO-TP功能
     */
    public static void demo() {
        System.out.println("\n========== ISO-TP 协议演示 ==========");
        
        ISO_TPAdapter adapter = new ISO_TPAdapter();
        
        // 单帧传输
        System.out.println("\n--- 单帧传输 (诊断请求) ---");
        byte[] singleFrame = adapter.createDiagnosticRequest(0x10, null);  // 诊断会话
        System.out.println("输出: " + adapter.formatHex(singleFrame));
        
        // 多帧传输
        System.out.println("\n--- 多帧传输 (ECU信息) ---");
        byte[] multiData = new byte[20];
        for (int i = 0; i < 20; i++) multiData[i] = (byte) (0x01 + i);
        byte[][] frames = adapter.sendMessage(0x7E0, multiData);
        
        // 模拟接收
        System.out.println("\n--- 模拟接收 ---");
        byte[] receivedFF = new byte[]{0x10, 0x14, 0x49, 0x02, 0x01, 0x00, 0x00, 0x00};  // 首帧
        adapter.decode(receivedFF);
        
        byte[] receivedCF = new byte[]{0x21, 0x06, 0x07, 0x08, 0x09, (byte)0x0A, (byte)0x0B, (byte)0x0C};  // 连续帧1
        adapter.decode(receivedCF);
        
        byte[] receivedFC = new byte[]{0x30, 0x00, 0x0A};  // 流控帧
        FlowControl fc = adapter.decodeFlowControl(receivedFC);
        System.out.println("收到流控: " + fc);
        
        System.out.println("\n========== 演示完成 ==========\n");
    }
}
