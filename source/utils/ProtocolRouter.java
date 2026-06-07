package utils;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议路由管理器
 * 
 * 基于设备类型自动选择最佳协议适配器
 * 
 * 功能：
 * - 设备类型识别
 * - 协议自动选择
 * - 协议转换路由
 * - 多协议并发通信
 * 
 * 支持的设备类型：
 * - DRONE: 无人机 -> MAVLink, DroneCAN
 * - VEHICLE: 车辆 -> V2X, CAN-Bus, DDS
 * - RSU: 路侧单元 -> V2X, HTTP
 * - GCS: 地面站 -> MQTT, DDS
 * - ECU: 车载ECU -> ISO-TP, CAN-Bus
 * 
 * @author main-agent
 */
public class ProtocolRouter {
    
    // 设备类型枚举
    public enum DeviceType {
        DRONE("无人机"),
        VEHICLE("车辆"),
        RSU("路侧单元"),
        GCS("地面站"),
        ECU("车载ECU"),
        SENSOR("传感器"),
        CLOUD("云平台"),
        UNKNOWN("未知设备");
        
        private final String description;
        DeviceType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
    
    // 协议类型枚举
    public enum ProtocolType {
        MAVLINK("MAVLink v2.0", DeviceType.DRONE),
        DRONECAN("DroneCAN", DeviceType.DRONE),
        V2X("V2X IEEE 802.11p", DeviceType.VEHICLE),
        CAN_BUS("CAN-Bus", DeviceType.VEHICLE),
        ISO_TP("ISO-TP", DeviceType.ECU),
        DDS("DDS", DeviceType.GCS),
        MQTT("MQTT 5.0", DeviceType.GCS),
        HTTP("HTTP/1.1", DeviceType.RSU);
        
        private final String version;
        private final DeviceType primaryDevice;
        
        ProtocolType(String version, DeviceType primaryDevice) {
            this.version = version;
            this.primaryDevice = primaryDevice;
        }
        
        public String getVersion() { return version; }
        public DeviceType getPrimaryDevice() { return primaryDevice; }
    }
    
    // 单例实例
    private static volatile ProtocolRouter instance;
    
    // 设备注册表
    private final Map<String, DeviceInfo> registeredDevices;
    
    // 协议优先级配置
    private final Map<DeviceType, ProtocolPreference> protocolPreferences;
    
    /**
     * 设备信息
     */
    public static class DeviceInfo {
        private String deviceId;
        private DeviceType deviceType;
        private Set<ProtocolType> supportedProtocols;
        private ProtocolType preferredProtocol;
        private long lastHeartbeat;
        private Map<String, Object> metadata;
        
        public DeviceInfo(String deviceId, DeviceType deviceType) {
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.supportedProtocols = ConcurrentHashMap.newKeySet();
            this.metadata = new ConcurrentHashMap<>();
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        public String getDeviceId() { return deviceId; }
        public DeviceType getDeviceType() { return deviceType; }
        public Set<ProtocolType> getSupportedProtocols() { return supportedProtocols; }
        public ProtocolType getPreferredProtocol() { return preferredProtocol; }
        public void setPreferredProtocol(ProtocolType p) { this.preferredProtocol = p; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public void updateHeartbeat() { this.lastHeartbeat = System.currentTimeMillis(); }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * 协议优先级配置
     */
    public static class ProtocolPreference {
        private DeviceType deviceType;
        private ProtocolType[] priorityOrder;
        private boolean autoFailover;
        
        public ProtocolPreference(DeviceType deviceType, ProtocolType[] priorityOrder, boolean autoFailover) {
            this.deviceType = deviceType;
            this.priorityOrder = priorityOrder;
            this.autoFailover = autoFailover;
        }
        
        public DeviceType getDeviceType() { return deviceType; }
        public ProtocolType[] getPriorityOrder() { return priorityOrder; }
        public boolean isAutoFailover() { return autoFailover; }
    }
    
    /**
     * 路由决策结果
     */
    public static class RoutingDecision {
        private String sourceDeviceId;
        private String targetDeviceId;
        private DeviceType sourceType;
        private DeviceType targetType;
        private ProtocolType selectedProtocol;
        private String conversionPath;
        private double confidence;
        
        public RoutingDecision() {}
        
        public String getSourceDeviceId() { return sourceDeviceId; }
        public void setSourceDeviceId(String s) { this.sourceDeviceId = s; }
        public String getTargetDeviceId() { return targetDeviceId; }
        public void setTargetDeviceId(String t) { this.targetDeviceId = t; }
        public DeviceType getSourceType() { return sourceType; }
        public void setSourceType(DeviceType s) { this.sourceType = s; }
        public DeviceType getTargetType() { return targetType; }
        public void setTargetType(DeviceType t) { this.targetType = t; }
        public ProtocolType getSelectedProtocol() { return selectedProtocol; }
        public void setSelectedProtocol(ProtocolType p) { this.selectedProtocol = p; }
        public String getConversionPath() { return conversionPath; }
        public void setConversionPath(String p) { this.conversionPath = p; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double c) { this.confidence = c; }
        
        @Override
        public String toString() {
            return String.format("Routing[%s:%s -> %s:%s via %s, conf=%.2f]",
                sourceType, sourceDeviceId, targetType, targetDeviceId, 
                selectedProtocol, confidence);
        }
    }
    
    /**
     * 私有构造函数
     */
    private ProtocolRouter() {
        this.registeredDevices = new ConcurrentHashMap<>();
        this.protocolPreferences = new ConcurrentHashMap<>();
        initializeDefaultPreferences();
    }
    
    /**
     * 获取单例实例
     */
    public static ProtocolRouter getInstance() {
        if (instance == null) {
            synchronized (ProtocolRouter.class) {
                if (instance == null) {
                    instance = new ProtocolRouter();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化默认协议优先级
     */
    private void initializeDefaultPreferences() {
        // 无人机协议优先级
        protocolPreferences.put(DeviceType.DRONE, new ProtocolPreference(
            DeviceType.DRONE,
            new ProtocolType[]{ProtocolType.MAVLINK, ProtocolType.DRONECAN, ProtocolType.DDS},
            true
        ));
        
        // 车辆协议优先级
        protocolPreferences.put(DeviceType.VEHICLE, new ProtocolPreference(
            DeviceType.VEHICLE,
            new ProtocolType[]{ProtocolType.V2X, ProtocolType.CAN_BUS, ProtocolType.DDS},
            true
        ));
        
        // ECU协议优先级
        protocolPreferences.put(DeviceType.ECU, new ProtocolPreference(
            DeviceType.ECU,
            new ProtocolType[]{ProtocolType.ISO_TP, ProtocolType.CAN_BUS},
            true
        ));
        
        // 地面站协议优先级
        protocolPreferences.put(DeviceType.GCS, new ProtocolPreference(
            DeviceType.GCS,
            new ProtocolType[]{ProtocolType.MQTT, ProtocolType.DDS, ProtocolType.HTTP},
            true
        ));
        
        // 路侧单元协议优先级
        protocolPreferences.put(DeviceType.RSU, new ProtocolPreference(
            DeviceType.RSU,
            new ProtocolType[]{ProtocolType.V2X, ProtocolType.HTTP, ProtocolType.DDS},
            true
        ));
    }
    
    /**
     * 注册设备
     */
    public void registerDevice(String deviceId, DeviceType deviceType, ProtocolType... protocols) {
        DeviceInfo info = new DeviceInfo(deviceId, deviceType);
        if (protocols != null && protocols.length > 0) {
            for (ProtocolType p : protocols) {
                info.getSupportedProtocols().add(p);
            }
            info.setPreferredProtocol(protocols[0]);
        }
        registeredDevices.put(deviceId, info);
        System.out.println("[ProtocolRouter] 注册设备: " + deviceId + " (" + deviceType.getDescription() + ")");
    }
    
    /**
     * 获取设备信息
     */
    public DeviceInfo getDevice(String deviceId) {
        return registeredDevices.get(deviceId);
    }
    
    /**
     * 从数据自动识别设备类型
     */
    public DeviceType inferDeviceType(byte[] data) {
        if (data == null || data.length < 1) {
            return DeviceType.UNKNOWN;
        }
        
        byte firstByte = data[0];
        
        // MAVLink v2.0
        if (firstByte == (byte) 0xFD) {
            return DeviceType.DRONE;
        }
        
        // DroneCAN magic
        if (data.length >= 2 && (data[0] & 0xFF) == 0x0D && (data[1] & 0xFF) == 0x27) {
            return DeviceType.DRONE;
        }
        
        // V2X BSM
        if (data.length >= 2 && data[0] == 0x12 && data[1] == 0x34) {
            return DeviceType.VEHICLE;
        }
        
        // ISO-TP 帧
        int pci = firstByte & 0xF0;
        if (pci == 0x00 || pci == 0x10 || pci == 0x20 || pci == 0x30) {
            return DeviceType.ECU;
        }
        
        // CAN-Bus
        if (isCANBusFrame(data)) {
            return DeviceType.VEHICLE;
        }
        
        return DeviceType.UNKNOWN;
    }
    
    private boolean isCANBusFrame(byte[] data) {
        if (data.length < 2) return false;
        int dlc = data[0] & 0x0F;
        return dlc <= 0x0F;
    }
    
    /**
     * 选择最佳协议
     */
    public ProtocolType selectBestProtocol(DeviceType sourceType, DeviceType targetType) {
        // 获取源设备偏好
        ProtocolPreference sourcePref = protocolPreferences.get(sourceType);
        ProtocolPreference targetPref = protocolPreferences.get(targetType);
        
        if (sourcePref == null || targetPref == null) {
            // 降级为 DDS
            return ProtocolType.DDS;
        }
        
        // 查找共同支持的协议
        Set<ProtocolType> sourceProtocols = getProtocolsForDevice(sourceType);
        Set<ProtocolType> targetProtocols = getProtocolsForDevice(targetType);
        
        for (ProtocolType p : sourcePref.getPriorityOrder()) {
            if (targetProtocols.contains(p)) {
                return p;
            }
        }
        
        // 没有共同协议，使用 DDS 作为中转
        return ProtocolType.DDS;
    }
    
    /**
     * 获取设备支持的协议集合
     */
    private Set<ProtocolType> getProtocolsForDevice(DeviceType deviceType) {
        Set<ProtocolType> protocols = ConcurrentHashMap.newKeySet();
        
        for (ProtocolType p : ProtocolType.values()) {
            if (p.getPrimaryDevice() == deviceType) {
                protocols.add(p);
            }
        }
        
        // 所有设备都支持 DDS
        protocols.add(ProtocolType.DDS);
        
        return protocols;
    }
    
    /**
     * 路由决策
     */
    public RoutingDecision route(String sourceDeviceId, String targetDeviceId) {
        RoutingDecision decision = new RoutingDecision();
        decision.setSourceDeviceId(sourceDeviceId);
        decision.setTargetDeviceId(targetDeviceId);
        
        DeviceInfo source = registeredDevices.get(sourceDeviceId);
        DeviceInfo target = registeredDevices.get(targetDeviceId);
        
        if (source == null) {
            System.out.println("[ProtocolRouter] 警告: 源设备未注册: " + sourceDeviceId);
            decision.setSourceType(DeviceType.UNKNOWN);
        } else {
            decision.setSourceType(source.getDeviceType());
        }
        
        if (target == null) {
            System.out.println("[ProtocolRouter] 警告: 目标设备未注册: " + targetDeviceId);
            decision.setTargetType(DeviceType.UNKNOWN);
        } else {
            decision.setTargetType(target.getDeviceType());
        }
        
        // 选择协议
        decision.setSelectedProtocol(selectBestProtocol(
            decision.getSourceType(), 
            decision.getTargetType()
        ));
        
        // 构建转换路径
        decision.setConversionPath(buildConversionPath(
            decision.getSourceType(),
            decision.getTargetType(),
            decision.getSelectedProtocol()
        ));
        
        decision.setConfidence(1.0);
        
        return decision;
    }
    
    /**
     * 从原始数据进行路由决策
     */
    public RoutingDecision routeFromData(String sourceDeviceId, byte[] data) {
        RoutingDecision decision = new RoutingDecision();
        decision.setSourceDeviceId(sourceDeviceId);
        decision.setSourceType(inferDeviceType(data));
        
        // 如果能识别源设备类型，获取更多信息
        DeviceInfo source = registeredDevices.get(sourceDeviceId);
        if (source != null) {
            decision.setSourceType(source.getDeviceType());
        }
        
        System.out.println("[ProtocolRouter] 从数据推断设备类型: " + decision.getSourceType().getDescription());
        
        return decision;
    }
    
    /**
     * 构建协议转换路径
     */
    private String buildConversionPath(DeviceType source, DeviceType target, ProtocolType protocol) {
        StringBuilder path = new StringBuilder();
        
        // 源协议
        path.append(getProtocolForDeviceType(source));
        
        // 转换步骤
        if (source != DeviceType.UNKNOWN && protocol != null) {
            String sourceProtocol = getProtocolForDeviceType(source);
            if (!sourceProtocol.equals(protocol.name())) {
                path.append(" -> ").append(protocol.name());
            }
        }
        
        return path.toString();
    }
    
    /**
     * 获取设备类型对应的默认协议
     */
    private String getProtocolForDeviceType(DeviceType type) {
        switch (type) {
            case DRONE: return "MAVLINK";
            case VEHICLE: return "V2X";
            case ECU: return "ISO_TP";
            case GCS: return "MQTT";
            case RSU: return "V2X";
            default: return "DDS";
        }
    }
    
    /**
     * 获取所有已注册设备
     */
    public Map<String, DeviceInfo> getAllDevices() {
        return new HashMap<>(registeredDevices);
    }
    
    /**
     * 列出路由表
     */
    public void listRoutes() {
        System.out.println("\n========================================");
        System.out.println("         协议路由表          ");
        System.out.println("========================================");
        
        System.out.println("\n【已注册设备】");
        for (Map.Entry<String, DeviceInfo> entry : registeredDevices.entrySet()) {
            DeviceInfo info = entry.getValue();
            System.out.printf("  %-15s [%s] 支持: ", entry.getKey(), info.getDeviceType().getDescription());
            for (ProtocolType p : info.getSupportedProtocols()) {
                System.out.print(p.name() + " ");
            }
            System.out.println();
        }
        
        System.out.println("\n【协议优先级配置】");
        for (Map.Entry<DeviceType, ProtocolPreference> entry : protocolPreferences.entrySet()) {
            System.out.printf("  %s: ", entry.getKey().getDescription());
            for (ProtocolType p : entry.getValue().getPriorityOrder()) {
                System.out.print(p.name() + " ");
            }
            System.out.println();
        }
        
        System.out.println("\n========================================");
    }
    
    /**
     * 演示路由功能
     */
    public static void demo() {
        System.out.println("\n========== 协议路由演示 ==========");
        
        ProtocolRouter router = ProtocolRouter.getInstance();
        
        // 注册设备
        System.out.println("\n--- 注册设备 ---");
        router.registerDevice("DRONE-001", DeviceType.DRONE, ProtocolType.MAVLINK, ProtocolType.DRONECAN);
        router.registerDevice("DRONE-002", DeviceType.DRONE, ProtocolType.MAVLINK, ProtocolType.DDS);
        router.registerDevice("VEHICLE-001", DeviceType.VEHICLE, ProtocolType.V2X, ProtocolType.CAN_BUS);
        router.registerDevice("ECU-001", DeviceType.ECU, ProtocolType.ISO_TP);
        router.registerDevice("GCS-001", DeviceType.GCS, ProtocolType.MQTT, ProtocolType.DDS);
        router.registerDevice("RSU-001", DeviceType.RSU, ProtocolType.V2X, ProtocolType.HTTP);
        
        // 列出路由表
        router.listRoutes();
        
        // 路由测试
        System.out.println("\n--- 路由决策测试 ---");
        
        RoutingDecision r1 = router.route("DRONE-001", "GCS-001");
        System.out.println("  决策: " + r1);
        
        RoutingDecision r2 = router.route("VEHICLE-001", "RSU-001");
        System.out.println("  决策: " + r2);
        
        RoutingDecision r3 = router.route("ECU-001", "VEHICLE-001");
        System.out.println("  决策: " + r3);
        
        // 数据类型推断测试
        System.out.println("\n--- 数据类型推断测试 ---");
        
        byte[] mavlinkData = new byte[]{(byte) 0xFD, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00};
        DeviceType inferred1 = router.inferDeviceType(mavlinkData);
        System.out.println("  MAVLink帧 -> " + inferred1.getDescription());
        
        byte[] v2xData = new byte[]{0x12, 0x34, 0x00, 0x00, 0x00};
        DeviceType inferred2 = router.inferDeviceType(v2xData);
        System.out.println("  V2X BSM -> " + inferred2.getDescription());
        
        byte[] isotpData = new byte[]{0x10, 0x14, 0x49, 0x02, 0x01};
        DeviceType inferred3 = router.inferDeviceType(isotpData);
        System.out.println("  ISO-TP帧 -> " + inferred3.getDescription());
        
        System.out.println("\n========== 演示完成 ==========\n");
    }
}
