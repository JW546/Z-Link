package agent;

import java.util.*;

/**
 * 发布/订阅智能决策器
 * 
 * 根据用户意图、设备角色、任务阶段自动决定：
 * - 何时发布数据
 * - 何时订阅数据
 * 
 * @paper_ref 第4章 4.2 DDS发布订阅 -> 智能主题决策
 */
public class PubSubDecisionMaker {
    
    // 决策规则库
    private final Map<String, DecisionRule> rules = new LinkedHashMap<>();
    
    public PubSubDecisionMaker() {
        initializeRules();
    }
    
    /**
     * 初始化决策规则库
     */
    private void initializeRules() {
        // ========== 导航场景 ==========
        rules.put("NAVIGATION:VEHICLE", new DecisionRule()
            .addPublishTopic("VehicleData")           // 发布自身位置和状态
            .addPublishTopic("MissionRequest")        // 发布任务请求
            .addSubscribeTopic("IntersectionPerception")  // 订阅路口感知
            .addSubscribeTopic("TrafficFlow")         // 订阅交通流
            .addSubscribeTopic("DroneStatus")         // 订阅周边无人机
            .setDescription("车辆导航: 发布位置，订阅路况和感知数据"));
        
        rules.put("NAVIGATION:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")           // 发布无人机状态
            .addPublishTopic("AerialView")           // 发布航拍视图
            .addSubscribeTopic("TrafficFlow")         // 订阅地面交通
            .addSubscribeTopic("VehicleData")         // 订阅车辆数据
            .setDescription("无人机导航: 发布空中视角，订阅地面态势"));
        
        // ========== 查询场景 ==========
        rules.put("QUERY_BATTERY:VEHICLE", new DecisionRule()
            .addPublishTopic("BatteryQuery")          // 发布电池查询请求
            .addSubscribeTopic("BatteryStatus")       // 订阅电池状态
            .setDescription("车辆查询电量: 只订阅，不发布数据"));
        
        rules.put("QUERY_BATTERY:DRONE", new DecisionRule()
            .addPublishTopic("BatteryQuery")
            .addSubscribeTopic("BatteryStatus")
            .setDescription("无人机查询电量: 只订阅，不发布数据"));
        
        rules.put("QUERY_STATUS:VEHICLE", new DecisionRule()
            .addSubscribeTopic("VehicleStatus")
            .addSubscribeTopic("MissionStatus")
            .setDescription("车辆查询状态: 订阅自身和任务状态"));

        rules.put("QUERY_STATUS:DRONE", new DecisionRule()
            .addSubscribeTopic("DroneStatus")
            .addSubscribeTopic("MissionStatus")
            .setDescription("无人机查询状态: 订阅飞行与任务状态"));

        rules.put("QUERY_STATUS:CONTROL_CENTER", new DecisionRule()
            .addSubscribeTopic("VehicleStatus")
            .addSubscribeTopic("DroneStatus")
            .addSubscribeTopic("MissionStatus")
            .setDescription("控制中心查询状态: 汇总全局任务状态"));
        
        // ========== 应急场景 ==========
        rules.put("EMERGENCY:VEHICLE", new DecisionRule()
            .addPublishTopic("EmergencyAlert")       // 发布紧急告警
            .addPublishTopic("VehicleData")           // 发布位置（便于救援）
            .addSubscribeTopic("EmergencyAlert")     // 订阅他人告警
            .addSubscribeTopic("IntersectionPerception")  // 订阅路口（快速通过）
            .setPriority(100)
            .setDescription("车辆应急: 发布告警，订阅应急通道"));
        
        rules.put("EMERGENCY:DRONE", new DecisionRule()
            .addPublishTopic("EmergencyAlert")
            .addPublishTopic("DroneStatus")
            .addSubscribeTopic("EmergencyAlert")
            .setPriority(100)
            .setDescription("无人机应急: 发布告警，协助救援"));
        
        // ========== 充电场景 ==========
        rules.put("CHARGE:VEHICLE", new DecisionRule()
            .addPublishTopic("ChargeRequest")         // 发布充电请求
            .addSubscribeTopic("ChargingStationStatus")  // 订阅充电站状态
            .setDescription("车辆充电: 发布充电请求，订阅站点状态"));
        
        rules.put("CHARGE:DRONE", new DecisionRule()
            .addPublishTopic("ChargeRequest")
            .addSubscribeTopic("ChargingPadStatus")
            .setDescription("无人机充电: 发布充电请求，订阅停机坪状态"));
        
        // ========== 停止场景 ==========
        rules.put("STOP:VEHICLE", new DecisionRule()
            .addPublishTopic("VehicleData")          // 发布停止状态
            .addSubscribeTopic("VehicleStatus")
            .setDescription("车辆停止: 发布停止状态"));
        
        // ========== 配送场景 ==========
        rules.put("DELIVERY:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")
            .addPublishTopic("DeliveryStatus")
            .addSubscribeTopic("WeatherData")         // 订阅气象数据
            .addSubscribeTopic("NoFlyZone")           // 订阅禁飞区
            .setDescription("无人机配送: 发布状态，订阅气象和空域"));

        rules.put("DELIVERY:VEHICLE", new DecisionRule()
            .addPublishTopic("VehicleData")
            .addPublishTopic("DeliveryStatus")
            .addSubscribeTopic("TrafficFlow")
            .addSubscribeTopic("MissionAssignment")
            .setDescription("车辆配送: 发布配送状态，订阅路况与任务分配"));

        // ========== 乘客意图驱动的自治协同场景 ==========
        rules.put("COORDINATION:CONTROL_CENTER", new DecisionRule()
            .addPublishTopic("CompliantBackboneSync")
            .addPublishTopic("GlobalPerception")
            .addSubscribeTopic("VehicleStatus")
            .addSubscribeTopic("DroneStatus")
            .addSubscribeTopic("urban/route/advice.dsl")
            .addSubscribeTopic("TrafficFlow")
            .addSubscribeTopic("UAV/Perception/ForwardCorridor")
            .setPriority(95)
            .setDescription("全域协同: 合规广域中继汇总态势，不直接分派任务"));

        rules.put("COORDINATION:VEHICLE", new DecisionRule()
            .addPublishTopic("VehicleData")
            .addPublishTopic("cabin/passenger/intent.dsl")
            .addPublishTopic("urban/route/advice.dsl")
            .addPublishTopic("Vehicle/Peer/Advice")
            .addSubscribeTopic("Vehicle/Peer/Advice")
            .addSubscribeTopic("UAV/Perception/ForwardCorridor")
            .addSubscribeTopic("Traffic/Intersection/Forward")
            .addSubscribeTopic("CompliantBackbone/GlobalPerception")
            .setPriority(90)
            .setDescription("全域协同: 车辆理解乘客意图并自主协商路线"));

        rules.put("COORDINATION:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")
            .addPublishTopic("UAV/Perception/ForwardCorridor")
            .addPublishTopic("urban/route/advice.dsl")
            .addSubscribeTopic("cabin/passenger/intent.dsl")
            .addSubscribeTopic("Vehicle/Peer/Advice")
            .addSubscribeTopic("NoFlyZone")
            .setPriority(90)
            .setDescription("全域协同: 无人机自主订阅乘客意图并发布空中路线建议"));
        
        // ========== 巡逻场景 ==========
        rules.put("PATROL:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")
            .addPublishTopic("PerceptionData")
            .addSubscribeTopic("TrafficFlow")
            .addSubscribeTopic("WeatherData")
            .setDescription("无人机巡逻: 发布感知，订阅气象和交通"));
        
        // ========== 侦察场景 ==========
        rules.put("SURVEILLANCE:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")
            .addPublishTopic("SurveillanceFeed")
            .addSubscribeTopic("NoFlyZone")
            .addSubscribeTopic("WeatherData")
            .setDescription("无人机侦察: 发布侦察画面，订阅空域和气象"));
        
        // ========== 默认规则（未知意图） ==========
        rules.put("UNKNOWN:VEHICLE", new DecisionRule()
            .addPublishTopic("VehicleData")
            .addSubscribeTopic("TrafficFlow")
            .setDescription("默认车辆: 只发布状态"));
        
        rules.put("UNKNOWN:DRONE", new DecisionRule()
            .addPublishTopic("DroneStatus")
            .setDescription("默认无人机: 只发布状态"));
        
        rules.put("UNKNOWN:CONTROL_CENTER", new DecisionRule()
            .addPublishTopic("CompliantBackboneSync")
            .addSubscribeTopic("VehicleStatus")
            .addSubscribeTopic("DroneStatus")
            .addSubscribeTopic("TrafficFlow")
            .setDescription("合规广域中继: 汇总设备状态并提供全局态势"));
    }
    
    /**
     * 做出发布/订阅决策
     * 
     * @param intent 用户意图
     * @param role 设备角色
     * @param phase 任务阶段
     * @return 决策结果
     */
    public PubSubDecision makeDecision(String intent, CPSEnums.DeviceRole role, CPSEnums.TaskPhase phase) {
        String key = intent + ":" + role.name();
        DecisionRule rule = rules.getOrDefault(key, getDefaultRule(role));
        
        PubSubDecision decision = new PubSubDecision();
        decision.intent = intent;
        decision.deviceRole = role;
        decision.taskPhase = phase;
        decision.publishTopics = new ArrayList<>(rule.publishTopics);
        decision.subscribeTopics = new ArrayList<>(rule.subscribeTopics);
        decision.priority = rule.priority;
        decision.description = rule.description;
        
        // 根据任务阶段调整决策
        adjustByPhase(decision, phase);
        
        // 根据优先级调整
        if (rule.priority > 80) {
            decision.description += " [高优先级]";
        }
        
        return decision;
    }

    /**
     * 场景增强：根据目的地和导航意图补充前方路口协同订阅主题。
     */
    public void applyScenarioHints(PubSubDecision decision, String destination) {
        if (decision == null) {
            return;
        }

        if ("NAVIGATION".equals(decision.intent) && decision.deviceRole == CPSEnums.DeviceRole.VEHICLE) {
            addUnique(decision.publishTopics, "SubscriptionIntent");
            addUnique(decision.subscribeTopics, "Traffic/Intersection/Forward");
            addUnique(decision.subscribeTopics, "UAV/Perception/ForwardCorridor");
            addUnique(decision.subscribeTopics, "CompliantBackbone/GlobalPerception");
            addUnique(decision.subscribeTopics, "Route/Replan/Trigger");

            if (destination != null && !destination.trim().isEmpty()) {
                decision.description += " [场景增强: 前方路口协同订阅 -> " + destination + "]";
            } else {
                decision.description += " [场景增强: 前方路口协同订阅]";
            }
        }
    }

    private void addUnique(List<String> topics, String topic) {
        if (!topics.contains(topic)) {
            topics.add(topic);
        }
    }
    
    /**
     * 根据任务阶段调整决策
     */
    private void adjustByPhase(PubSubDecision decision, CPSEnums.TaskPhase phase) {
        switch (phase) {
            case PLANNING:
                // 规划阶段：多订阅，少发布
                decision.description += " (规划中)";
                break;
            case EXECUTING:
                // 执行阶段：按规则正常执行
                decision.description += " (执行中)";
                break;
            case MONITORING:
                // 监控阶段：发布监控数据
                decision.publishTopics.add("MonitorData");
                decision.description += " (监控中)";
                break;
            default:
                break;
        }
    }
    
    /**
     * 获取默认规则
     */
    private DecisionRule getDefaultRule(CPSEnums.DeviceRole role) {
        return rules.getOrDefault("UNKNOWN:" + role.name(), 
            new DecisionRule().setDescription("无匹配规则"));
    }
    
    /**
     * 添加自定义规则
     */
    public void addRule(String key, DecisionRule rule) {
        rules.put(key, rule);
    }
    
    /**
     * 打印所有规则（调试用）
     */
    public void printAllRules() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║              发布/订阅决策规则库                       ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        for (Map.Entry<String, DecisionRule> entry : rules.entrySet()) {
            System.out.printf("║ %-25s ║ %n", entry.getKey());
            DecisionRule rule = entry.getValue();
            System.out.printf("║   发布: %-48s ║%n", rule.publishTopics);
            System.out.printf("║   订阅: %-48s ║%n", rule.subscribeTopics);
            System.out.println("╠═══════════════════════════════════════════════════════╣");
        }
        System.out.println("╚═══════════════════════════════════════════════════════╝");
    }
    
    // ========== 内部类 ==========
    
    /**
     * 决策规则
     */
    private static class DecisionRule {
        List<String> publishTopics = new ArrayList<>();
        List<String> subscribeTopics = new ArrayList<>();
        int priority = 50;  // 默认优先级
        String description = "";
        
        DecisionRule addPublishTopic(String topic) {
            publishTopics.add(topic);
            return this;
        }
        
        DecisionRule addSubscribeTopic(String topic) {
            subscribeTopics.add(topic);
            return this;
        }
        
        DecisionRule setPriority(int priority) {
            this.priority = priority;
            return this;
        }
        
        DecisionRule setDescription(String description) {
            this.description = description;
            return this;
        }
    }
    
    /**
     * 发布订阅决策结果
     */
    public static class PubSubDecision {
        public String intent;
        public CPSEnums.DeviceRole deviceRole;
        public CPSEnums.TaskPhase taskPhase;
        public List<String> publishTopics = new ArrayList<>();
        public List<String> subscribeTopics = new ArrayList<>();
        public int priority;
        public String description;
        
        public List<String> getPublishTopics() { return publishTopics; }
        public List<String> getSubscribeTopics() { return subscribeTopics; }
        
        @Override
        public String toString() {
            return String.format("PubSubDecision[意图=%s, 角色=%s, 发布=%s, 订阅=%s, 优先级=%d]",
                intent, deviceRole, publishTopics, subscribeTopics, priority);
        }
    }
    
    // ========== 测试 ==========
    public static void main(String[] args) {
        PubSubDecisionMaker maker = new PubSubDecisionMaker();
        
        System.out.println("发布/订阅决策器测试\n");
        
        // 测试各种场景
        String[] testCases = {
            "NAVIGATION:VEHICLE",
            "QUERY_BATTERY:DRONE",
            "EMERGENCY:VEHICLE",
            "CHARGE:VEHICLE",
            "DELIVERY:DRONE"
        };
        
        for (String key : testCases) {
            String[] parts = key.split(":");
            PubSubDecision decision = maker.makeDecision(
                parts[0], 
                CPSEnums.DeviceRole.valueOf(parts[1]),
                CPSEnums.TaskPhase.EXECUTING
            );
            System.out.println(decision);
            System.out.println();
        }
        
        // 打印所有规则
        System.out.println("\n--- 所有规则 ---");
        maker.printAllRules();
    }
}
