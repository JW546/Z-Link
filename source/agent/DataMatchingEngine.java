package agent;

import java.util.*;

/**
 * 数据匹配引擎
 * 
 * 功能：
 * 1. 判断自己能否满足某个主题的数据需求
 * 2. 从已有数据中提取可发布的内容
 * 3. 匹配数据提供方和需求方
 * 
 * @paper_ref 第4章 4.2 数据匹配 -> 供需对齐
 */
public class DataMatchingEngine {
    
    // 设备能力库
    private final Map<CPSEnums.DeviceRole, DeviceCapabilities> capabilities = new HashMap<>();
    
    // 主题到能力的映射
    private final Map<String, Set<String>> topicCapabilities = new HashMap<>();
    
    public DataMatchingEngine() {
        initializeCapabilities();
    }
    
    /**
     * 初始化设备能力库
     */
    private void initializeCapabilities() {
        // ========== 车辆能力 ==========
        DeviceCapabilities vehicleCap = new DeviceCapabilities();
        vehicleCap.canProvide.add("VehicleData");      // 车辆数据
        vehicleCap.canProvide.add("Position");          // 位置
        vehicleCap.canProvide.add("Speed");             // 速度
        vehicleCap.canProvide.add("BatteryStatus");     // 电量
        vehicleCap.canProvide.add("RoadCondition");     // 路面状况（如果有摄像头）
        vehicleCap.canProvide.add("TrafficObservation"); // 交通观察
        vehicleCap.canRequire.add("TrafficFlow");        // 需要交通流
        vehicleCap.canRequire.add("IntersectionPerception"); // 需要路口感知
        vehicleCap.canRequire.add("WeatherData");       // 需要气象
        capabilities.put(CPSEnums.DeviceRole.VEHICLE, vehicleCap);
        
        // ========== 无人机能力 ==========
        DeviceCapabilities droneCap = new DeviceCapabilities();
        droneCap.canProvide.add("DroneStatus");         // 无人机状态
        droneCap.canProvide.add("AerialView");           // 航拍视角
        droneCap.canProvide.add("Position");             // 位置
        droneCap.canProvide.add("BatteryStatus");        // 电量
        droneCap.canProvide.add("PerceptionData");      // 感知数据
        droneCap.canProvide.add("WeatherObservation");   // 空中气象观察
        droneCap.canRequire.add("WeatherData");          // 需要气象
        droneCap.canRequire.add("NoFlyZone");            // 需要禁飞区
        droneCap.canRequire.add("TrafficFlow");          // 需要交通
        capabilities.put(CPSEnums.DeviceRole.DRONE, droneCap);
        
        // ========== 控制中心能力 ==========
        DeviceCapabilities centerCap = new DeviceCapabilities();
        centerCap.canProvide.add("ControlCommand");     // 控制命令
        centerCap.canProvide.add("MissionAssignment");  // 任务分配
        centerCap.canProvide.add("WeatherData");         // 气象数据
        centerCap.canProvide.add("NoFlyZone");          // 禁飞区
        centerCap.canRequire.add("VehicleStatus");       // 需要车辆状态
        centerCap.canRequire.add("DroneStatus");        // 需要无人机状态
        capabilities.put(CPSEnums.DeviceRole.CONTROL_CENTER, centerCap);
        
        // ========== 主题能力映射 ==========
        topicCapabilities.put("VehicleData", Set.of("position", "speed", "heading", "battery"));
        topicCapabilities.put("DroneStatus", Set.of("position", "speed", "altitude", "battery", "mission"));
        topicCapabilities.put("IntersectionPerception", Set.of("image", "lidar", "radar", "congestion"));
        topicCapabilities.put("TrafficFlow", Set.of("vehicle_count", "average_speed", "congestion_level"));
        topicCapabilities.put("WeatherData", Set.of("temperature", "wind", "visibility", "precipitation"));
        topicCapabilities.put("Traffic/Intersection/Forward", Set.of("intersectionId", "congestionLevel", "queueLength", "timestamp"));
        topicCapabilities.put("UAV/Perception/ForwardCorridor", Set.of("corridorId", "clearPath", "riskScore", "timestamp"));
        topicCapabilities.put("CompliantBackbone/GlobalPerception", Set.of("globalRisk", "cityCongestion", "timestamp", "sourceType"));
        topicCapabilities.put("Route/Replan/Trigger", Set.of("triggerReason", "severity", "timestamp"));

        vehicleCap.canRequire.add("Traffic/Intersection/Forward");
        vehicleCap.canRequire.add("UAV/Perception/ForwardCorridor");
        vehicleCap.canRequire.add("CompliantBackbone/GlobalPerception");

        droneCap.canProvide.add("UAV/Perception/ForwardCorridor");
        centerCap.canProvide.add("CompliantBackbone/GlobalPerception");
        centerCap.canProvide.add("Route/Replan/Trigger");
    }
    
    /**
     * 判断能否满足某个主题的需求
     * 
     * @param topic 订阅的主题
     * @param myRole 我的角色
     * @param myData 我的已知数据
     * @return 能否满足
     */
    public boolean canSatisfyTopic(String topic, CPSEnums.DeviceRole myRole, Map<String, Object> myData) {
        DeviceCapabilities myCap = capabilities.get(myRole);
        if (myCap == null) return false;
        
        // 检查我的能力是否包含该主题
        if (!myCap.canProvide.contains(topic)) {
            return false;
        }
        
        // 检查我是否有足够的数据来满足这个主题
        return hasDataForTopic(topic, myData);
    }

    /**
     * 结合时空上下文判断是否可满足主题。
     */
    public boolean canSatisfyTopicWithContext(String topic, CPSEnums.DeviceRole myRole,
            Map<String, Object> myData, String scope, long nowTimestamp) {
        if (!canSatisfyTopic(topic, myRole, myData)) {
            return false;
        }
        if (myData == null) {
            return true;
        }

        Object dataScope = myData.get("scope");
        if (scope != null && dataScope != null && !scope.equals(String.valueOf(dataScope))) {
            return false;
        }

        Object ts = myData.get("timestamp");
        if (ts instanceof Number) {
            long age = nowTimestamp - ((Number) ts).longValue();
            return age < 30000;
        }
        return true;
    }
    
    /**
     * 检查是否有足够的数据满足主题
     */
    private boolean hasDataForTopic(String topic, Map<String, Object> myData) {
        if (myData == null || myData.isEmpty()) {
            // 没有数据但有能力，可以实时采集
            return true;
        }
        
        Set<String> requiredFields = topicCapabilities.get(topic);
        if (requiredFields == null) {
            return true; // 未知主题，保守返回true
        }
        
        // 检查数据中是否有所需字段
        for (String field : requiredFields) {
            if (!hasField(myData, field)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查数据对象是否有某个字段
     */
    private boolean hasField(Map<String, Object> data, String field) {
        // 直接匹配
        if (data.containsKey(field)) return true;
        
        // 模糊匹配
        String lowerField = field.toLowerCase();
        for (String key : data.keySet()) {
            if (key.toLowerCase().contains(lowerField)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 查找可以满足需求的主题列表
     * 
     * @param myRole 我的角色
     * @param myPublishTopics 我计划发布的主题
     * @return 可满足需求的主题列表
     */
    public List<String> findSatisfiableTopics(CPSEnums.DeviceRole myRole, List<String> myPublishTopics) {
        List<String> satisfiable = new ArrayList<>();
        
        DeviceCapabilities myCap = capabilities.get(myRole);
        if (myCap == null) return satisfiable;
        
        // 检查我发布的主题中有哪些能被别人利用
        for (String topic : myPublishTopics) {
            if (myCap.canProvide.contains(topic)) {
                satisfiable.add(topic + " (可供他人订阅)");
            }
        }
        
        return satisfiable;
    }
    
    /**
     * 提取可用于特定主题的数据
     * 
     * @param topic 目标主题
     * @param myData 我的原始数据
     * @return 提取后的数据
     */
    public Map<String, Object> extractDataForTopic(String topic, Map<String, Object> myData) {
        Map<String, Object> extracted = new HashMap<>();
        
        Set<String> fields = topicCapabilities.get(topic);
        if (fields == null) {
            // 未知主题，返回原始数据
            return new HashMap<>(myData);
        }
        
        // 提取匹配的字段
        for (String key : myData.keySet()) {
            String lowerKey = key.toLowerCase();
            for (String field : fields) {
                if (lowerKey.contains(field.toLowerCase()) || field.toLowerCase().contains(lowerKey)) {
                    extracted.put(key, myData.get(key));
                    break;
                }
            }
        }
        
        return extracted;
    }
    
    /**
     * 匹配数据提供方和需求方
     * 
     * @param provider 供方角色
     * @param consumer 需方角色
     * @return 匹配的主题列表
     */
    public List<String> matchProviderConsumer(CPSEnums.DeviceRole provider, CPSEnums.DeviceRole consumer) {
        List<String> matches = new ArrayList<>();
        
        DeviceCapabilities providerCap = capabilities.get(provider);
        DeviceCapabilities consumerCap = capabilities.get(consumer);
        
        if (providerCap == null || consumerCap == null) {
            return matches;
        }
        
        // 供方提供的 = 需方需要的
        for (String topic : providerCap.canProvide) {
            if (consumerCap.canRequire.contains(topic)) {
                matches.add(topic);
            }
        }
        
        return matches;
    }
    
    /**
     * 获取设备能力描述
     */
    public DeviceCapabilities getCapabilities(CPSEnums.DeviceRole role) {
        return capabilities.get(role);
    }
    
    /**
     * 打印匹配分析报告
     */
    public void printMatchingReport(CPSEnums.DeviceRole provider, CPSEnums.DeviceRole consumer) {
        List<String> matches = matchProviderConsumer(provider, consumer);
        
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.printf("║           数据匹配报告: %s -> %s          ║%n", 
            provider, consumer);
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        
        if (matches.isEmpty()) {
            System.out.println("║  无匹配主题                                            ║");
        } else {
            System.out.println("║  可匹配主题:                                           ║");
            for (String topic : matches) {
                System.out.printf("║    ✓ %-50s ║%n", topic);
            }
        }
        
        System.out.println("╚═══════════════════════════════════════════════════════╝");
    }
    
    // ========== 内部类 ==========
    
    /**
     * 设备能力
     */
    public static class DeviceCapabilities {
        public Set<String> canProvide = new HashSet<>();  // 可提供的数据
        public Set<String> canRequire = new HashSet<>();   // 需要的数据
        
        @Override
        public String toString() {
            return String.format("Capabilities[可提供=%s, 需要=%s]", canProvide, canRequire);
        }
    }
    
    // ========== 测试 ==========
    public static void main(String[] args) {
        DataMatchingEngine engine = new DataMatchingEngine();
        
        System.out.println("数据匹配引擎测试\n");
        
        // 测试1: 车辆能否满足交通流需求
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("speed", 30.0);
        vehicleData.put("position", "29.5,106.5");
        vehicleData.put("vehicle_id", "VEH_001");
        
        System.out.println("测试1: 车辆能否满足TrafficFlow? " + 
            engine.canSatisfyTopic("TrafficFlow", CPSEnums.DeviceRole.VEHICLE, vehicleData));
        
        System.out.println("测试2: 车辆能否满足VehicleData? " + 
            engine.canSatisfyTopic("VehicleData", CPSEnums.DeviceRole.VEHICLE, vehicleData));
        
        // 测试2: 匹配报告
        System.out.println();
        engine.printMatchingReport(CPSEnums.DeviceRole.VEHICLE, CPSEnums.DeviceRole.CONTROL_CENTER);
        engine.printMatchingReport(CPSEnums.DeviceRole.DRONE, CPSEnums.DeviceRole.VEHICLE);
        
        // 测试3: 查找可满足的主题
        System.out.println("\n测试3: 无人机可满足的主题:");
        List<String> satisfiable = engine.findSatisfiableTopics(
            CPSEnums.DeviceRole.DRONE,
            Arrays.asList("DroneStatus", "AerialView")
        );
        System.out.println("  " + satisfiable);
    }
}
