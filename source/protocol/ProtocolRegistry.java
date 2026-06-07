package protocol;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议注册中心 - 统一管理所有协议适配器
 * 
 * 功能：
 * - 注册/注销协议适配器
 * - 协议自动识别
 * - 协议查找和获取
 * - 协议元数据管理
 * 
 * 支持的协议：
 * - CAN-Bus: 车规级控制器局域网
 * - V2X: 车联万物通信
 * - ISO-TP: ISO 15765-2 传输层协议
 * - DroneCAN: 无人机CAN总线协议
 * - MAVLink: 已有协议
 * - DDS: 已有协议
 * - COMPLIANT_BACKBONE: 合规广域链路卫星通信协议（全球感知与纳秒授时）
 * 
 * @author protocol-agent
 */
public class ProtocolRegistry {
    
    // 单例实例
    private static volatile ProtocolRegistry instance;
    
    // 协议适配器注册表
    private final Map<String, ProtocolAdapter> adapters;
    
    // 协议元数据
    private final Map<String, ProtocolMetadata> protocolMetadata;
    
    // 协议标识符到名称的映射
    private final Map<Integer, String> protocolIdMap;
    
    /**
     * 协议适配器接口
     */
    public interface ProtocolAdapter {
        /**
         * 获取协议名称
         */
        String getName();
        
        /**
         * 获取协议版本
         */
        String getVersion();
        
        /**
         * 获取协议描述
         */
        String getDescription();
        
        /**
         * 编码数据
         * @param data 要编码的数据
         * @return 编码后的字节数组
         */
        byte[] encode(Object data);
        
        /**
         * 解码数据
         * @param rawData 原始字节数据
         * @return 解码后的数据对象
         */
        Object decode(byte[] rawData);
        
        /**
         * 验证数据格式
         * @param data 要验证的数据
         * @return 是否有效
         */
        boolean validate(byte[] data);
    }
    
    /**
     * 协议元数据
     */
    public static class ProtocolMetadata {
        private final String name;
        private final String version;
        private final String description;
        private final Set<String> capabilities;
        private final int protocolId;
        private final boolean isAutomotiveGrade;
        
        public ProtocolMetadata(String name, String version, String description, 
                               Set<String> capabilities, int protocolId, boolean isAutomotiveGrade) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.capabilities = capabilities;
            this.protocolId = protocolId;
            this.isAutomotiveGrade = isAutomotiveGrade;
        }
        
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public Set<String> getCapabilities() { return capabilities; }
        public int getProtocolId() { return protocolId; }
        public boolean isAutomotiveGrade() { return isAutomotiveGrade; }
    }
    
    /**
     * 私有构造函数（单例）
     */
    private ProtocolRegistry() {
        this.adapters = new ConcurrentHashMap<>();
        this.protocolMetadata = new ConcurrentHashMap<>();
        this.protocolIdMap = new ConcurrentHashMap<>();
        
        // 初始化默认协议元数据
        initializeDefaultMetadata();
    }
    
    /**
     * 获取单例实例
     */
    public static ProtocolRegistry getInstance() {
        if (instance == null) {
            synchronized (ProtocolRegistry.class) {
                if (instance == null) {
                    instance = new ProtocolRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化默认协议元数据
     */
    private void initializeDefaultMetadata() {
        // CAN-Bus 元数据
        registerMetadata("CAN-Bus", "2.0", "Controller Area Network - 车规级总线协议",
            Set.of("standard_frame", "extended_frame", "id_filtering"), 1, true);
        
        // V2X 元数据
        registerMetadata("V2X", "IEEE 802.11p", "Vehicle-to-Everything - 车联万物通信",
            Set.of("V2V", "V2I", "V2P", "V2N", "BSM"), 2, true);
        
        // ISO-TP 元数据
        registerMetadata("ISO-TP", "ISO 15765-2", "ISO Transport Protocol - 汽车诊断传输协议",
            Set.of("single_frame", "multi_frame", "flow_control"), 3, true);
        
        // DroneCAN 元数据
        registerMetadata("DroneCAN", "1.0", "DroneCAN Protocol - 无人机CAN总线协议",
            Set.of("node_discovery", "heartbeat", "data_transfer", "parameters"), 4, false);
        
        // MAVLink 元数据
        registerMetadata("MAVLink", "v2.0", "Micro Air Vehicle Link - 无人机通信协议",
            Set.of("binary_protocol", "telemetry", "command"), 5, false);
        
        // DDS 元数据
        registerMetadata("DDS", "Standard/v2.0", "Data Distribution Service - 数据分发服务",
            Set.of("pub_sub", "topic_based", "real_time"), 6, false);
        
        // COMPLIANT_BACKBONE 合规广域链路元数据
        registerMetadata("COMPLIANT_BACKBONE", "Approved-WAN/v1", "Compliant Backbone - 5G-A/DDS Router/国内批准卫星移动通信融合广域链路",
            Set.of("global_coverage", "nanosecond_sync", "low_latency", "satellite_mesh"), 7, false);
    }
    
    /**
     * 注册协议元数据
     */
    private void registerMetadata(String name, String version, String description,
                                 Set<String> capabilities, int protocolId, boolean isAutomotiveGrade) {
        ProtocolMetadata metadata = new ProtocolMetadata(
            name, version, description, capabilities, protocolId, isAutomotiveGrade);
        protocolMetadata.put(name.toUpperCase(), metadata);
        protocolIdMap.put(protocolId, name.toUpperCase());
    }
    
    /**
     * 注册协议适配器
     * @param adapter 协议适配器实例
     */
    public void registerAdapter(ProtocolAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("适配器不能为空");
        }
        String name = adapter.getName().toUpperCase();
        adapters.put(name, adapter);
        System.out.println("[ProtocolRegistry] 注册协议适配器: " + adapter.getName() + 
                          " (v" + adapter.getVersion() + ")");
    }
    
    /**
     * 注销协议适配器
     * @param protocolName 协议名称
     * @return 是否成功注销
     */
    public boolean unregisterAdapter(String protocolName) {
        String name = protocolName.toUpperCase();
        if (adapters.remove(name) != null) {
            System.out.println("[ProtocolRegistry] 注销协议适配器: " + protocolName);
            return true;
        }
        return false;
    }
    
    /**
     * 获取协议适配器
     * @param protocolName 协议名称
     * @return 协议适配器实例（未找到返回null）
     */
    public ProtocolAdapter getAdapter(String protocolName) {
        return adapters.get(protocolName.toUpperCase());
    }
    
    /**
     * 获取协议元数据
     * @param protocolName 协议名称
     * @return 协议元数据（未找到返回null）
     */
    public ProtocolMetadata getMetadata(String protocolName) {
        return protocolMetadata.get(protocolName.toUpperCase());
    }
    
    /**
     * 自动识别协议类型
     * @param data 原始数据
     * @return 识别的协议名称（未识别返回null）
     */
    public String autoDetectProtocol(byte[] data) {
        if (data == null || data.length < 1) {
            return null;
        }
        
        // 尝试通过magic byte识别
        byte firstByte = data[0];
        
        // CAN-Bus: 标准帧以0x00或0x80开头，扩展帧以0x00或0x80+IDE位开头
        if (isCANBusFrame(data)) {
            return "CAN-BUS";
        }
        
        // DroneCAN: 以DUC报文魔术数开头
        if (data.length >= 4 && (data[0] & 0xFF) == 0x0D && (data[1] & 0xFF) == 0x27) {
            return "DRONECAN";
        }
        
        // MAVLink v2.0: 以0xFD开头
        if (firstByte == (byte) 0xFD) {
            return "MAVLINK";
        }
        
        // MAVLink v1.0: 以0xFE开头
        if (firstByte == (byte) 0xFE) {
            return "MAVLINK";
        }
        
        // ISO-TP: 检查是否为多帧传输
        if (isISOTPFrame(data)) {
            return "ISO-TP";
        }
        
        // V2X BSM: 以J2735消息ID开头
        if (isV2XBSMFrame(data)) {
            return "V2X";
        }
        
        return null;
    }
    
    /**
     * 检查是否为CAN-Bus帧
     */
    private boolean isCANBusFrame(byte[] data) {
        if (data.length < 2) return false;
        // CAN标准帧: 第一个字节的低3位是DLC
        int dlc = data[0] & 0x0F;
        if (dlc > 0x0F) return false;
        return true;
    }
    
    /**
     * 检查是否为ISO-TP帧
     */
    private boolean isISOTPFrame(byte[] data) {
        if (data.length < 1) return false;
        // ISO-TP帧类型: N_PCI
        int nPci = data[0] & 0xF0;
        // 单帧: 0x0X
        // 首帧: 0x1X
        // 连续帧: 0x2X
        // 流控帧: 0x3X
        return (nPci == 0x00 || nPci == 0x10 || nPci == 0x20 || nPci == 0x30);
    }
    
    /**
     * 检查是否为V2X BSM帧
     */
    private boolean isV2XBSMFrame(byte[] data) {
        if (data.length < 3) return false;
        // J2735 BSM消息通常以消息ID 0x1234 (DE_DSRCMessageID) 开头
        // 这里简化检测
        return (data[0] == 0x12 && data[1] == 0x34) || 
               (data[0] == 0x80 && (data[1] & 0x10) != 0);
    }
    
    /**
     * 获取所有已注册的协议名称
     */
    public Set<String> getRegisteredProtocols() {
        return Set.copyOf(adapters.keySet());
    }
    
    /**
     * 获取所有协议元数据
     */
    public Map<String, ProtocolMetadata> getAllMetadata() {
        return Map.copyOf(protocolMetadata);
    }
    
    /**
     * 获取车规级协议
     */
    public Map<String, ProtocolMetadata> getAutomotiveGradeProtocols() {
        Map<String, ProtocolMetadata> automotive = new HashMap<>();
        for (Map.Entry<String, ProtocolMetadata> entry : protocolMetadata.entrySet()) {
            if (entry.getValue().isAutomotiveGrade()) {
                automotive.put(entry.getKey(), entry.getValue());
            }
        }
        return automotive;
    }
    
    /**
     * 列出所有可用协议
     */
    public void listProtocols() {
        System.out.println("\n========================================");
        System.out.println("         协议注册中心 - 协议列表          ");
        System.out.println("========================================");
        
        System.out.println("\n【车规级协议】");
        for (Map.Entry<String, ProtocolMetadata> entry : protocolMetadata.entrySet()) {
            ProtocolMetadata meta = entry.getValue();
            if (meta.isAutomotiveGrade()) {
                System.out.printf("  [%2d] %-12s v%-10s %s%n", 
                    meta.getProtocolId(), meta.getName(), meta.getVersion(), 
                    meta.isAutomotiveGrade() ? "★" : "");
            }
        }
        
        System.out.println("\n【其他协议】");
        for (Map.Entry<String, ProtocolMetadata> entry : protocolMetadata.entrySet()) {
            ProtocolMetadata meta = entry.getValue();
            if (!meta.isAutomotiveGrade()) {
                System.out.printf("  [%2d] %-12s v%-10s %s%n", 
                    meta.getProtocolId(), meta.getName(), meta.getVersion(), "");
            }
        }
        
        System.out.println("\n【已注册适配器】: " + adapters.size());
        for (String name : adapters.keySet()) {
            ProtocolAdapter adapter = adapters.get(name);
            System.out.println("  ✓ " + adapter.getName() + " v" + adapter.getVersion());
        }
        
        System.out.println("\n========================================");
    }
    
    /**
     * 协议转换
     * @param sourceProtocol 源协议
     * @param targetProtocol 目标协议
     * @param data 数据
     * @return 转换结果
     */
    public Object convert(String sourceProtocol, String targetProtocol, Object data) {
        ProtocolAdapter source = getAdapter(sourceProtocol);
        ProtocolAdapter target = getAdapter(targetProtocol);
        
        if (source == null) {
            throw new IllegalArgumentException("未找到源协议适配器: " + sourceProtocol);
        }
        if (target == null) {
            throw new IllegalArgumentException("未找到目标协议适配器: " + targetProtocol);
        }
        
        System.out.println("[ProtocolRegistry] 协议转换: " + sourceProtocol + " -> " + targetProtocol);
        
        // 编码源数据
        byte[] encoded = source.encode(data);
        
        // 解码为中间表示
        Object intermediate = target.decode(encoded);
        
        return intermediate;
    }
    
    /**
     * 批量注册默认适配器
     */
    public void registerDefaultAdapters() {
        // 注册CAN-Bus适配器
        try {
            Class<?> canClass = Class.forName("protocol.CANBusAdapter");
            ProtocolAdapter canAdapter = (ProtocolAdapter) canClass.getDeclaredConstructor().newInstance();
            registerAdapter(canAdapter);
        } catch (Exception e) {
            System.out.println("[ProtocolRegistry] CAN-Bus适配器加载失败: " + e.getMessage());
        }
        
        // 注册V2X适配器
        try {
            Class<?> v2xClass = Class.forName("protocol.V2XAdapter");
            ProtocolAdapter v2xAdapter = (ProtocolAdapter) v2xClass.getDeclaredConstructor().newInstance();
            registerAdapter(v2xAdapter);
        } catch (Exception e) {
            System.out.println("[ProtocolRegistry] V2X适配器加载失败: " + e.getMessage());
        }
        
        // 注册ISO-TP适配器
        try {
            Class<?> isoClass = Class.forName("protocol.ISO_TPAdapter");
            ProtocolAdapter isoAdapter = (ProtocolAdapter) isoClass.getDeclaredConstructor().newInstance();
            registerAdapter(isoAdapter);
        } catch (Exception e) {
            System.out.println("[ProtocolRegistry] ISO-TP适配器加载失败: " + e.getMessage());
        }
        
        // 注册DroneCAN适配器
        try {
            Class<?> droneClass = Class.forName("protocol.DroneCANAdapter");
            ProtocolAdapter droneAdapter = (ProtocolAdapter) droneClass.getDeclaredConstructor().newInstance();
            registerAdapter(droneAdapter);
        } catch (Exception e) {
            System.out.println("[ProtocolRegistry] DroneCAN适配器加载失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理并重置注册表
     */
    public void reset() {
        adapters.clear();
        System.out.println("[ProtocolRegistry] 注册表已重置");
    }
}
