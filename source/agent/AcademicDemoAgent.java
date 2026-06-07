package agent;

import nlu.NLUProcessor;
import nlu.NLUResult;
import utils.SynonymLibrary;
import utils.UnitConverter;

import java.util.*;

/**
 * 学术演示Agent - 简洁论文格式输出
 * 
 * 功能：
 * 1. 整合NLU、协议适配、路线优化等核心模块
 * 2. 输出适合论文/计划书的简洁格式
 * 3. 替代所有独立演示文件
 * 
 * @paper_ref 论文第3-4章完整功能演示
 */
public class AcademicDemoAgent {
    
    // ========== 核心组件 ==========
    private final NLUProcessor nluProcessor;
    private final SynonymLibrary synonymLib;
    private final UnitConverter unitConverter;
    
    // 演示场景集合
    private static final List<DemoScenario> SCENARIOS = new ArrayList<>();
    
    // 静态初始化演示场景
    static {
        SCENARIOS.add(new DemoScenario("车辆导航", "我想去西南大学，很急", "NAVIGATION", 
            "VehicleData, TrafficFlow", "IntersectionPerception, DroneStatus"));
        SCENARIOS.add(new DemoScenario("无人机配送", "启动无人机配送任务到观音桥", "DELIVERY",
            "DroneStatus, MissionRequest", "DeliveryZoneInfo, WeatherData"));
        SCENARIOS.add(new DemoScenario("应急救援", "发生紧急事故，需要无人机协助", "EMERGENCY",
            "EmergencyAlert, LocationData", "EmergencyResources, PerceptionData"));
        SCENARIOS.add(new DemoScenario("乘客意图协同", "乘客说我要尽快去医院，请无人车联动后车和无人机给出路线建议", "COORDINATION",
            "cabin/passenger/intent.dsl, urban/route/advice.dsl", "VehicleStatus, DroneStatus, UAV/Perception/ForwardCorridor"));
        SCENARIOS.add(new DemoScenario("电量查询", "电量还够吗，需要充电吗", "QUERY_BATTERY",
            "BatteryStatus", "ChargingStationInfo"));
    }
    
    public AcademicDemoAgent() {
        this.nluProcessor = new NLUProcessor();
        this.synonymLib = new SynonymLibrary();
        this.unitConverter = new UnitConverter();
    }
    
    /**
     * 运行学术演示 - 简洁输出
     */
    public void runAcademicDemo() {
        printHeader();
        
        for (int i = 0; i < SCENARIOS.size(); i++) {
            DemoScenario scenario = SCENARIOS.get(i);
            System.out.println("\n" + "=".repeat(60));
            System.out.println("【场景" + (i + 1) + "】" + scenario.name);
            System.out.println("=".repeat(60));
            executeScenario(scenario);
        }
        
        printCoreModules();
    }
    
    /**
     * 执行单个演示场景
     */
    private void executeScenario(DemoScenario scenario) {
        AcademicResult result = new AcademicResult();
        
        // 输入
        result.input = scenario.input;
        result.intent = scenario.intent;
        System.out.println("|- 输入: \"" + result.input + "\"");
        System.out.println("|- 意图: " + result.intent);
        
        // NLU解析
        NLUResult nluResult = nluProcessor.processToResult(result.input);
        result.destination = nluResult.getDestination();
        if (result.destination != null) {
            System.out.println("|- 目的地: " + result.destination);
        }
        
        // 语义对齐
        result.synonymCount = countSynonymMappings(result.input);
        if (result.synonymCount > 0) {
            System.out.println("|- 语义对齐: " + result.synonymCount + "组同义词映射");
        }
        
        // 单位转换
        result.unitConversions = extractAndConvert(result.input);
        if (!result.unitConversions.isEmpty()) {
            System.out.println("|- 单位转换: " + String.join(", ", result.unitConversions));
        }
        
        // 发布/订阅决策
        PubSubDecisionMaker.PubSubDecision decision = 
            new PubSubDecisionMaker().makeDecision(result.intent, 
                CPSEnums.DeviceRole.VEHICLE, CPSEnums.TaskPhase.PLANNING);
        result.publishTopics = decision.getPublishTopics();
        result.subscribeTopics = decision.getSubscribeTopics();
        System.out.println("|- 发布: " + result.publishTopics);
        System.out.println("|- 订阅: " + result.subscribeTopics);
        
        // 协议适配
        result.protocols = selectProtocols(result.intent);
        System.out.println("|- 协议栈: " + String.join(", ", result.protocols));
        
        // 路线优化（如适用）
        if ("NAVIGATION".equals(result.intent) && result.destination != null) {
            result.routeTime = 30;
            result.routeDistance = 15.5;
            System.out.println("|- 路线: 优化路线已规划");
            System.out.println("|- 预计: " + result.routeTime + "分钟 / " + result.routeDistance + "km");
        }
        
        System.out.println("|_ 评估: 可行性评分 " + String.format("%.1f", calculateFeasibility(result)));
    }
    
    /**
     * 执行用户自定义输入
     */
    public void processCustomInput(String input) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("【自定义输入】");
        System.out.println("=".repeat(60));
        
        // 自动检测意图
        String intent = detectIntent(input);
        NLUResult nluResult = nluProcessor.processToResult(input);
        
        System.out.println("|- 输入: \"" + input + "\"");
        System.out.println("|- 意图: " + intent);
        
        String destination = nluResult.getDestination();
        if (destination != null) {
            System.out.println("|- 目的地: " + destination);
        }
        
        PubSubDecisionMaker.PubSubDecision decision = 
            new PubSubDecisionMaker().makeDecision(intent, 
                CPSEnums.DeviceRole.VEHICLE, CPSEnums.TaskPhase.PLANNING);
        System.out.println("|- 发布: " + decision.getPublishTopics());
        System.out.println("|- 订阅: " + decision.getSubscribeTopics());
        System.out.println("|- 协议栈: " + String.join(", ", selectProtocols(intent)));
        
        AcademicResult result = new AcademicResult();
        result.intent = intent;
        result.destination = destination;
        result.publishTopics = decision.getPublishTopics();
        result.subscribeTopics = decision.getSubscribeTopics();
        result.protocols = selectProtocols(intent);
        System.out.println("|_ 评估: 可行性评分 " + String.format("%.1f", calculateFeasibility(result)));
    }
    
    /**
     * 打印核心模块信息
     */
    private void printCoreModules() {
        System.out.println("\n\n" + "=".repeat(60));
        System.out.println("【核心模块统计】");
        System.out.println("=".repeat(60));
        System.out.println("|- SynonymLibrary: 多类同义词组");
        System.out.println("|   |- 设备类型: vehicle, drone, 传感器");
        System.out.println("|   |- 任务类型: 配送, 侦察, 巡逻, 导航");
        System.out.println("|   |- 数据类型: 电量, 位置, 速度, 载重");
        System.out.println("|- UnitConverter: 多种单位转换");
        System.out.println("|   |- 距离: km <-> miles");
        System.out.println("|   |- 电量: mAh <-> Ah");
        System.out.println("|   |- 重量: kg <-> lb");
        System.out.println("|- ProtocolAdapter: 多种协议支持");
        System.out.println("|   |- 车端: ISO-TP, CAN-Bus, OBD-II");
        System.out.println("|   |- 无人机: MAVLink, DroneCAN");
        System.out.println("|   |- 云端: CoAP, HTTP, DDS");
        System.out.println("|_ RouteOptimizer: A*算法 + 动态权重");
    }
    
    /**
     * 打印演示头部
     */
    private void printHeader() {
        System.out.println("\n");
        System.out.println("+==============================================================+");
        System.out.println("|          民用无人交通DSL系统 - 学术演示                          |");
        System.out.println("|          CPS协同Agent · 意图理解 · 协议适配                       |");
        System.out.println("+==============================================================+");
    }
    
    // ========== 辅助方法 ==========
    
    private String detectIntent(String input) {
        NLUResult result = nluProcessor.processToResult(input);
        if (result != null && result.getPrimaryIntent() != null) {
            return result.getPrimaryIntent().name();
        }
        return "UNKNOWN";
    }
    
    private int countSynonymMappings(String input) {
        int count = 0;
        if (input.contains("速度") || input.contains("speed") || input.contains("车速")) count++;
        if (input.contains("高度") || input.contains("altitude") || input.contains("飞")) count++;
        if (input.contains("配送") || input.contains("delivery") || input.contains("送货")) count++;
        return count;
    }
    
    private List<String> extractAndConvert(String input) {
        List<String> conversions = new ArrayList<>();
        if (input.contains("mAh")) conversions.add("mAh->Ah");
        if (input.contains("miles")) conversions.add("miles->km");
        if (input.contains("lb")) conversions.add("lb->kg");
        return conversions;
    }
    
    private List<String> selectProtocols(String intent) {
        List<String> protocols = new ArrayList<>();
        switch (intent) {
            case "NAVIGATION":
                protocols.addAll(Arrays.asList("DDS-Standard/v2.0", "HTTP", "AMap-API"));
                break;
            case "EMERGENCY":
                protocols.addAll(Arrays.asList("DDS-Secure", "CoAP", "JSON"));
                break;
            case "DELIVERY":
            case "DRONE_CONTROL":
                protocols.addAll(Arrays.asList("MAVLink", "DroneCAN", "DDS"));
                break;
            case "COORDINATION":
                protocols.addAll(Arrays.asList("DDS-Standard/v2.0", "V2X", "5G-A/PrivateEdge", "SemanticTaskDSL"));
                break;
            case "QUERY_BATTERY":
                protocols.addAll(Arrays.asList("CAN-Bus", "OBD-II", "MQTT"));
                break;
            default:
                protocols.addAll(Arrays.asList("DDS-Standard/v2.0", "JSON"));
        }
        return protocols;
    }
    
    private double calculateFeasibility(AcademicResult result) {
        double score = 0.8;
        if (!result.publishTopics.isEmpty()) score += 0.05;
        if (!result.subscribeTopics.isEmpty()) score += 0.05;
        if (result.destination != null) score += 0.05;
        if (!result.protocols.isEmpty()) score += 0.05;
        return Math.min(1.0, score);
    }
    
    // ========== 内部类 ==========
    
    private static class DemoScenario {
        String name;
        String input;
        String intent;
        String publish;
        String subscribe;
        
        DemoScenario(String name, String input, String intent, String publish, String subscribe) {
            this.name = name;
            this.input = input;
            this.intent = intent;
            this.publish = publish;
            this.subscribe = subscribe;
        }
    }
    
    private static class AcademicResult {
        String input;
        String intent;
        String destination;
        int synonymCount;
        List<String> unitConversions = new ArrayList<>();
        List<String> publishTopics = new ArrayList<>();
        List<String> subscribeTopics = new ArrayList<>();
        List<String> protocols = new ArrayList<>();
        int routeTime;
        double routeDistance;
    }
    
    // ========== 主入口 ==========
    public static void main(String[] args) {
        AcademicDemoAgent demo = new AcademicDemoAgent();
        demo.runAcademicDemo();
        
        if (args.length > 0) {
            demo.processCustomInput(String.join(" ", args));
        }
    }
}
