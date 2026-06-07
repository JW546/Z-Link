package protocol;

import java.util.Map;
import java.util.HashMap;

/**
 * 合规广域链路协议转换器
 * 
 * 实现合规广域链路卫星通信协议与地面网络协议之间的转换：
 * - COMPLIANT_BACKBONE ↔ DDS (数据分发服务)
 * - COMPLIANT_BACKBONE ↔ MAVLink (无人机通信)
 * - COMPLIANT_BACKBONE ↔ V2X (车联网)
 * - COMPLIANT_BACKBONE ↔ MQTT (物联网消息队列)
 * 
 * @since 论文第3章 - 协议设计
 */
public class CompliantBackboneProtocolConverter {
    
    private CompliantBackboneAdapter CompliantBackboneAdapter;
    
    /**
     * 转换结果包装类
     */
    public static class ConversionResult {
        private boolean success;
        private String protocolType;
        private Object data;
        private String errorMessage;
        private long processingTimeNs;
        
        public ConversionResult(boolean success, String protocolType, Object data) {
            this.success = success;
            this.protocolType = protocolType;
            this.data = data;
            this.processingTimeNs = System.nanoTime();
        }
        
        public static ConversionResult success(String protocolType, Object data) {
            return new ConversionResult(true, protocolType, data);
        }
        
        public static ConversionResult failure(String errorMessage) {
            ConversionResult result = new ConversionResult(false, null, null);
            result.errorMessage = errorMessage;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getProtocolType() { return protocolType; }
        public Object getData() { return data; }
        public String getErrorMessage() { return errorMessage; }
        public long getProcessingTimeNs() { return processingTimeNs; }
    }
    
    /**
     * 合规广域链路协议消息结构
     */
    public static class CompliantBackboneMessage {
        private String messageId;
        private String sourceTerminal;
        private String destTerminal;
        private String protocol;
        private long timestamp;
        private long nanosecondTime;    // 纳秒级时间戳
        private byte[] payload;
        private int priority;           // 优先级 0-9
        private boolean encrypted;
        
        public CompliantBackboneMessage() {
            this.timestamp = System.currentTimeMillis();
            this.nanosecondTime = System.nanoTime();
        }
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getSourceTerminal() { return sourceTerminal; }
        public void setSourceTerminal(String sourceTerminal) { this.sourceTerminal = sourceTerminal; }
        public String getDestTerminal() { return destTerminal; }
        public void setDestTerminal(String destTerminal) { this.destTerminal = destTerminal; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public long getTimestamp() { return timestamp; }
        public long getNanosecondTime() { return nanosecondTime; }
        public byte[] getPayload() { return payload; }
        public void setPayload(byte[] payload) { this.payload = payload; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    }
    
    public CompliantBackboneProtocolConverter(CompliantBackboneAdapter adapter) {
        this.CompliantBackboneAdapter = adapter;
    }
    
    /**
     * 合规广域链路 → DDS 转换
     */
    public ConversionResult compliantBackboneToDDS(CompliantBackboneAdapter.GlobalPerceptionData data) {
        System.out.println("[COMPLIANT_BACKBONE-DDS] 转换全球感知数据为DDS格式...");
        
        try {
            String ddsFormat = CompliantBackboneAdapter.toDDSFormat(data);
            
            System.out.println("  输入: " + data.getPerceptionType());
            System.out.println("  输出: " + ddsFormat.substring(0, Math.min(80, ddsFormat.length())) + "...");
            
            return ConversionResult.success("DDS", ddsFormat);
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * DDS → 合规广域链路 转换
     */
    public ConversionResult ddsToCompliantBackbone(String ddsData) {
        System.out.println("[DDS-COMPLIANT_BACKBONE] 转换DDS数据为合规广域链路传输格式...");
        
        try {
            CompliantBackboneAdapter.GlobalPerceptionData perceptionData = CompliantBackboneAdapter.fromDDSFormat(ddsData);
            
            // 通过合规广域链路发送
            boolean sent = CompliantBackboneAdapter.sendGlobalPerceptionData(perceptionData);
            
            if (sent) {
                return ConversionResult.success("COMPLIANT_BACKBONE", "数据已通过合规广域链路发送");
            } else {
                return ConversionResult.failure("合规广域链路发送失败");
            }
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 合规广域链路 → MAVLink 转换
     */
    public ConversionResult compliantBackboneToMAVLink(Map<String, Object> satelliteData) {
        System.out.println("[COMPLIANT_BACKBONE-MAVLink] 转换卫星数据为MAVLink v2.0格式...");
        
        try {
            StringBuilder mavlink = new StringBuilder();
            mavlink.append("MAV-2.0:");
            mavlink.append("timestamp=").append(System.currentTimeMillis()).append(",");
            mavlink.append("msg_id=33,");
            mavlink.append("sys_id=255,");
            mavlink.append("comp_id=1,");
            mavlink.append("payload={");
            
            if (satelliteData.containsKey("latitude")) {
                int lat = (int) (toDouble(satelliteData.get("latitude"), 0.0) * 1E7);
                mavlink.append("lat:").append(lat).append(",");
            }
            if (satelliteData.containsKey("longitude")) {
                int lon = (int) (toDouble(satelliteData.get("longitude"), 0.0) * 1E7);
                mavlink.append("lon:").append(lon).append(",");
            }
            if (satelliteData.containsKey("altitude")) {
                int alt = (int) (toDouble(satelliteData.get("altitude"), 0.0) * 1000);
                mavlink.append("alt:").append(alt).append(",");
            }
            if (satelliteData.containsKey("source")) {
                mavlink.append("source:").append(satelliteData.get("source")).append(",");
            }
            if (satelliteData.containsKey("perception_type")) {
                mavlink.append("perception_type:").append(satelliteData.get("perception_type")).append(",");
            }
            mavlink.append("}");
            
            System.out.println("  输出: " + mavlink.toString());
            return ConversionResult.success("MAVLink", mavlink.toString());
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * MAVLink → 合规广域链路 转换
     */
    public ConversionResult mavlinkToCompliantBackbone(String mavlinkData) {
        System.out.println("[MAVLink-COMPLIANT_BACKBONE] 将MAVLink数据转换为合规广域链路传输格式...");
        
        try {
            Map<String, Object> extractedData = new HashMap<>();
            
            if (mavlinkData.contains("lat:")) {
                String latStr = extractValue(mavlinkData, "lat");
                extractedData.put("latitude", parseNumber(latStr, 0.0) / 1E7);
            }
            if (mavlinkData.contains("lon:")) {
                String lonStr = extractValue(mavlinkData, "lon");
                extractedData.put("longitude", parseNumber(lonStr, 0.0) / 1E7);
            }
            if (mavlinkData.contains("alt:")) {
                String altStr = extractValue(mavlinkData, "alt");
                extractedData.put("altitude", parseNumber(altStr, 0.0) / 1000.0);
            }
            
            CompliantBackboneAdapter.GlobalPerceptionData perceptionData = new CompliantBackboneAdapter.GlobalPerceptionData();
            if (extractedData.containsKey("latitude")) {
                perceptionData.setLatitude((Double) extractedData.get("latitude"));
            }
            if (extractedData.containsKey("longitude")) {
                perceptionData.setLongitude((Double) extractedData.get("longitude"));
            }
            if (extractedData.containsKey("altitude")) {
                perceptionData.setAltitude((Double) extractedData.get("altitude"));
            }
            perceptionData.setDataSource("mavlink_relay");
            perceptionData.setPerceptionType("drone_position");
            perceptionData.setConfidence(95);
            
            boolean sent = CompliantBackboneAdapter.sendGlobalPerceptionData(perceptionData);
            return sent ? ConversionResult.success("COMPLIANT_BACKBONE", "MAVLink数据已通过合规广域链路发送") 
                        : ConversionResult.failure("合规广域链路发送失败");
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 合规广域链路 → V2X 转换
     */
    public ConversionResult compliantBackboneToV2X(CompliantBackboneAdapter.GlobalPerceptionData data) {
        System.out.println("[COMPLIANT_BACKBONE-V2X] 转换卫星数据为V2X BSM格式...");
        
        try {
            String bsm = String.format(
                "BSM:msg_id=0x2B1,sec_mark=%d,temp_id=%s,lat=%.6f,lon=%.6f,speed=%.1f,heading=%.1f",
                System.currentTimeMillis() % 65536,
                Math.abs(CompliantBackboneAdapter.getTerminalId().hashCode()) & 0xFFFF,
                data.getLatitude(), data.getLongitude(), 0.0, 0.0
            );
            return ConversionResult.success("V2X", bsm);
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * V2X → 合规广域链路 转换
     */
    public ConversionResult v2xToCompliantBackbone(String v2xData) {
        System.out.println("[V2X-COMPLIANT_BACKBONE] 转换V2X BSM数据为合规广域链路格式...");
        
        try {
            CompliantBackboneAdapter.GlobalPerceptionData data = new CompliantBackboneAdapter.GlobalPerceptionData();
            if (v2xData.contains("lat=")) {
                data.setLatitude(parseNumber(extractValue(v2xData, "lat"), 0.0));
            }
            if (v2xData.contains("lon=")) {
                data.setLongitude(parseNumber(extractValue(v2xData, "lon"), 0.0));
            }
            data.setDataSource("v2x_relay");
            data.setPerceptionType("vehicle_bsm");
            data.setConfidence(90);
            
            boolean sent = CompliantBackboneAdapter.sendGlobalPerceptionData(data);
            return sent ? ConversionResult.success("COMPLIANT_BACKBONE", "V2X数据已通过合规广域链路发送") 
                        : ConversionResult.failure("合规广域链路发送失败");
        } catch (Exception e) {
            return ConversionResult.failure("转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 通用协议转换入口
     */
    public ConversionResult convert(String sourceProtocol, String targetProtocol, Object data) {
        System.out.println("\n[COMPLIANT_BACKBONEConverter] 协议转换: " + sourceProtocol + " -> " + targetProtocol);

        if (sourceProtocol == null || targetProtocol == null) {
            return ConversionResult.failure("协议名称不能为空");
        }

        if (data == null) {
            return ConversionResult.failure("转换数据不能为空");
        }
        
        if ("COMPLIANT_BACKBONE".equalsIgnoreCase(sourceProtocol) && "DDS".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof CompliantBackboneAdapter.GlobalPerceptionData) {
                return compliantBackboneToDDS((CompliantBackboneAdapter.GlobalPerceptionData) data);
            }
            if (data instanceof String) {
                return compliantBackboneToDDS(CompliantBackboneAdapter.fromDDSFormat((String) data));
            }
        }
        if ("DDS".equalsIgnoreCase(sourceProtocol) && "COMPLIANT_BACKBONE".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof String) {
                return ddsToCompliantBackbone((String) data);
            }
        }
        if ("COMPLIANT_BACKBONE".equalsIgnoreCase(sourceProtocol) && "MAVLink".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof Map) {
                return compliantBackboneToMAVLink((Map<String, Object>) data);
            }
        }
        if ("MAVLink".equalsIgnoreCase(sourceProtocol) && "COMPLIANT_BACKBONE".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof String) {
                return mavlinkToCompliantBackbone((String) data);
            }
        }
        if ("COMPLIANT_BACKBONE".equalsIgnoreCase(sourceProtocol) && "V2X".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof CompliantBackboneAdapter.GlobalPerceptionData) {
                return compliantBackboneToV2X((CompliantBackboneAdapter.GlobalPerceptionData) data);
            }
            if (data instanceof String) {
                return compliantBackboneToV2X(CompliantBackboneAdapter.fromDDSFormat((String) data));
            }
        }
        if ("V2X".equalsIgnoreCase(sourceProtocol) && "COMPLIANT_BACKBONE".equalsIgnoreCase(targetProtocol)) {
            if (data instanceof String) {
                return v2xToCompliantBackbone((String) data);
            }
        }
        return ConversionResult.failure("不支持的转换路径: " + sourceProtocol + " -> " + targetProtocol);
    }
    
    private String extractValue(String data, String key) {
        int idx = data.indexOf(key + "=");
        if (idx < 0) {
            idx = data.indexOf(key + ":");
        }
        if (idx < 0) return "";
        int start = idx + key.length() + 1;
        int end = data.indexOf(",", start);
        if (end < 0) end = data.indexOf("}", start);
        if (end < 0) end = data.length();
        return data.substring(start, end).trim();
    }

    private double parseNumber(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private double toDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
