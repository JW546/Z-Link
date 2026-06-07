package agent;

import nlu.IntentRecognizer;
import nlu.EntityExtractor;
import nlu.NLUProcessor;
import nlu.NLUResult;
import utils.AmapRealAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * CPS智能协同Agent - 核心协调器
 * 
 * 功能：
 * 1. 接收用户语音/键盘输入
 * 2. 调用NLU理解用户意图
 * 3. 协调PubSubDecisionMaker决策发布/订阅
 * 4. 协调DataMatchingEngine判断能否满足他人需求
 * 5. 协调RouteOptimizer进行路线优化
 * 
 * @paper_ref 第3章 3.2 意图理解 -> 语义对齐
 * @paper_ref 第4章 4.2 DDS发布订阅 -> 智能决策
 */
public class CPSAgent {
    
    // 使用公共枚举
    public static final CPSEnums.DeviceRole VehicleRole = CPSEnums.DeviceRole.VEHICLE;
    public static final CPSEnums.DeviceRole DroneRole = CPSEnums.DeviceRole.DRONE;
    public static final CPSEnums.DeviceRole ControlCenterRole = CPSEnums.DeviceRole.CONTROL_CENTER;
    
    // 核心组件
    private final IntentRecognizer intentRecognizer;
    private final EntityExtractor entityExtractor;
    private final PubSubDecisionMaker decisionMaker;
    private final DataMatchingEngine dataMatcher;
    private final RouteOptimizer routeOptimizer;
    private final CollaborativeDataFusionEngine fusionEngine;
    private final AmapRealAPI amap;
    private final CoordinationPolicy policy;
    private final SemanticDDSBridge ddsBridge;
    private final DeepSeekClient deepSeekClient;
    
    // Agent状态
    private CPSEnums.DeviceRole currentRole = CPSEnums.DeviceRole.VEHICLE;  // 默认车辆
    private CPSEnums.TaskPhase currentPhase = CPSEnums.TaskPhase.IDLE;
    private final Map<String, Object> knownData = new HashMap<>();
    
    // 订阅的数据缓存
    private final Map<String, List<Object>> subscribedDataCache = new HashMap<>();
    private String currentScenarioScope = "GLOBAL";
    
    public CPSAgent() {
        this.intentRecognizer = new IntentRecognizer();
        this.entityExtractor = new EntityExtractor();
        this.decisionMaker = new PubSubDecisionMaker();
        this.dataMatcher = new DataMatchingEngine();
        this.routeOptimizer = new RouteOptimizer();
        this.fusionEngine = new CollaborativeDataFusionEngine();
        this.amap = new AmapRealAPI();
        this.policy = new CoordinationPolicy();
        this.ddsBridge = new SemanticDDSBridge();
        this.deepSeekClient = new DeepSeekClient();
    }
    
    /**
     * 处理用户输入（语音/键盘）
     * @param input 用户输入的自然语言
     * @return 处理结果
     */
    public AgentResult processInput(String input) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║            CPS Agent 智能协同引擎                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("[Agent] 收到输入: " + input);
        
        AgentResult result = new AgentResult();
        result.originalInput = input;
        
        // ========== Step 1: NLU意图理解 ==========
        System.out.println("\n[Step 1] NLU意图理解");
        IntentParseResult parseResult = parseIntent(input);
        result.intent = parseResult.intent;
        result.nluConfidence = parseResult.nluConfidence;
        currentScenarioScope = parseResult.destination != null && !parseResult.destination.isBlank()
            ? parseResult.destination : "GLOBAL";
        System.out.println("  意图: " + result.intent);
        System.out.println("  NLU置信度: " + String.format(Locale.ROOT, "%.2f", result.nluConfidence));
        if (parseResult.currentLocation != null) {
            System.out.println("  当前位置: " + parseResult.currentLocation);
        }
        if (parseResult.destination != null) {
            System.out.println("  目的地: " + parseResult.destination);
        }
        if (parseResult.urgency != null) {
            System.out.println("  紧急程度: " + parseResult.urgency);
        }
        
        // ========== Step 2: 设备角色识别 ==========
        System.out.println("\n[Step 2] 设备角色识别");
        currentRole = identifyDeviceRole(input);
        System.out.println("  当前角色: " + currentRole);
        result.deviceRole = currentRole;

        DeepSeekClient.DeepSeekSuggestion deepSeekSuggestion = deepSeekClient.suggest(
            input, result.intent, currentRole, currentScenarioScope);
        if (deepSeekSuggestion != null) {
            result.llmUsed = true;
            result.llmProvider = deepSeekSuggestion.provider;
            result.llmModel = deepSeekSuggestion.model;
            result.llmConfidence = deepSeekSuggestion.confidence;
            result.llmSummary = deepSeekSuggestion.summary;
            result.llmLatencyMs = deepSeekSuggestion.latencyMs;
            result.llmRawContent = deepSeekSuggestion.rawContent;
            result.llmRawResponse = deepSeekSuggestion.rawResponse;
            if (shouldAdoptIntent(result.intent, result.nluConfidence, deepSeekSuggestion.intent, deepSeekSuggestion.confidence)) {
                result.intent = deepSeekSuggestion.intent;
                System.out.println("  DeepSeek接管意图: " + result.intent +
                    " (confidence=" + String.format(Locale.ROOT, "%.2f", deepSeekSuggestion.confidence) +
                    ", latency=" + deepSeekSuggestion.latencyMs + "ms)");
            } else {
                System.out.println("  DeepSeek辅助建议: " + deepSeekSuggestion.intent +
                    " (confidence=" + String.format(Locale.ROOT, "%.2f", deepSeekSuggestion.confidence) + ")");
            }
            if (deepSeekSuggestion.summary != null && !deepSeekSuggestion.summary.isBlank()) {
                System.out.println("  DeepSeek摘要: " + deepSeekSuggestion.summary);
            }
        }
        
        // ========== Step 3: 发布/订阅决策 ==========
        System.out.println("\n[Step 3] 发布/订阅智能决策");
        PubSubDecisionMaker.PubSubDecision decision = decisionMaker.makeDecision(result.intent, currentRole, currentPhase);
        decisionMaker.applyScenarioHints(decision, parseResult.destination);
        if (deepSeekSuggestion != null) {
            mergeDeepSeekTopics(decision, deepSeekSuggestion);
        }
        applyPolicyTopics(decision, result.intent);
        result.publishTopics = decision.getPublishTopics();
        result.subscribeTopics = decision.getSubscribeTopics();
        System.out.println("  发布主题: " + result.publishTopics);
        System.out.println("  订阅主题: " + result.subscribeTopics);
        
        // ========== Step 4: 执行发布/订阅 ==========
        System.out.println("\n[Step 4] DDS协同执行");
        executePublishSubscribe(decision);
        
        // ========== Step 5: 数据匹配分析 ==========
        System.out.println("\n[Step 5] 数据匹配分析（判断能否满足他人需求）");
        List<String> satisfiableTopics = dataMatcher.findSatisfiableTopics(currentRole, result.publishTopics);
        System.out.println("  可满足的需求: " + satisfiableTopics);
        result.satisfiableTopics = satisfiableTopics;

        List<CollaborativeDataPacket> packets = collectCollaborativePackets(result.subscribeTopics);
        String targetScope = parseResult.destination != null ? parseResult.destination : "GLOBAL";
        CollaborativeDataFusionEngine.FusionResult fusionResult = fusionEngine.fuse(packets, targetScope);
        result.fusionScore = fusionResult.fusionScore;
        result.replanTriggered = fusionResult.replanRecommended;
        System.out.printf("  融合评分: %.2f, 数据源: %d, 建议重规划: %s%n",
            fusionResult.fusionScore, fusionResult.sourceCount, fusionResult.replanRecommended);
        
        // ========== Step 6: 路线优化 ==========
        if (parseResult.destination != null && !parseResult.destination.isEmpty()) {
            System.out.println("\n[Step 6] 路线优化");

            String origin = parseResult.currentLocation != null ? parseResult.currentLocation : "当前位置";
            if (isSameLocation(origin, parseResult.destination)) {
                OptimizedRoute route = new OptimizedRoute("已在目的地，无需规划路线", 0, 0.0, false);
                result.optimizedRoute = route;
                System.out.println("  优化后路线: " + route.description);
                System.out.println("  预计时间: 0分钟");
            } else {
                OptimizedRoute route = optimizeRoute(origin, parseResult.destination, subscribedDataCache, fusionResult);
                result.optimizedRoute = route;
                System.out.println("  优化后路线: " + route.description);
                System.out.println("  预计时间: " + route.estimatedTime + "分钟");
            }
        }
        
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║                  Agent 处理完成                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        
        return result;
    }
    
    /**
     * NLU意图解析
     */
    private IntentParseResult parseIntent(String input) {
        IntentParseResult result = new IntentParseResult();
        
        // 使用现有的NLU组件
        NLUResult nluResult = new NLUProcessor().processToResult(input);
        result.nluConfidence = nluResult != null ? nluResult.getConfidence() : 0.0;

        // 语义优先规则：项目简介中的关键能力
        if (containsAny(input, policy.getCoordinationKeywords().toArray(new String[0]))) {
            result.intent = "COORDINATION";
        } else if (containsAny(input, "配送", "送达", "送货", "医疗物资", "急救包", "药品")) {
            result.intent = "DELIVERY";
        }
        
        // 意图识别
        if (result.intent == null && nluResult.getPrimaryIntent() != null) {
            result.intent = nluResult.getPrimaryIntent().name();
        } else if (result.intent == null) {
            result.intent = detectIntentFallback(input);
        }
        
        // 目的地
        if (nluResult.getDestination() != null) {
            result.destination = sanitizePlaceText(nluResult.getDestination());
        }

        // 起点（当前位置）
        if (nluResult.getStartLocation() != null) {
            result.currentLocation = sanitizePlaceText(nluResult.getStartLocation());
        }

        // 回退：直接从原句提取“我在/我就在”当前位置
        if (result.currentLocation == null) {
            result.currentLocation = extractCurrentLocationFromInput(input);
        }
        
        // 从文本中提取目的地（备用）
        if (result.destination == null && (input.contains("去") || input.contains("到"))) {
            String[] parts = input.split("(去|到|往)");
            if (parts.length > 1) {
                result.destination = sanitizePlaceText(parts[1]);
            }
        }
        
        // 紧急程度识别
        if (input.contains("非常赶") || input.contains("十万火急") || input.contains("马上")) {
            result.urgency = "CRITICAL";
        } else if (input.contains("赶") || input.contains("急")) {
            result.urgency = "HIGH";
        }
        
        return result;
    }
    
    /**
     * 意图解析结果
     */
    private static class IntentParseResult {
        String intent;
        String destination;
        String currentLocation;
        String urgency;
        double nluConfidence;
    }

    private String sanitizePlaceText(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim();
        cleaned = cleaned.replaceAll("(你告诉我|请问|我想知道).*$", "");
        cleaned = cleaned.replaceAll("(要|还要)?\\d+\\s*分钟吗?$", "");
        cleaned = cleaned.replaceAll("(要|还要).*$", "");
        cleaned = cleaned.replaceAll("(还要多久|多久到|要多久|吗)$", "");
        cleaned = cleaned.replaceAll("[，。！？,.!?].*$", "");
        cleaned = cleaned.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String extractCurrentLocationFromInput(String input) {
        if (input == null) {
            return null;
        }
        String raw = input;
        String[] markers = {"我就在", "我在", "当前位置在", "现在在"};
        for (String marker : markers) {
            int idx = raw.indexOf(marker);
            if (idx >= 0) {
                String tail = raw.substring(idx + marker.length());
                return sanitizePlaceText(tail);
            }
        }
        return null;
    }

    private boolean isSameLocation(String origin, String destination) {
        if (origin == null || destination == null) {
            return false;
        }
        String normOrigin = origin.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        String normDest = destination.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return normOrigin.equals(normDest) || normOrigin.contains(normDest) || normDest.contains(normOrigin);
    }
    
    /**
     * 回退意图检测
     */
    private String detectIntentFallback(String input) {
        if (containsAny(input, policy.getCoordinationKeywords().toArray(new String[0]))) return "COORDINATION";
        if (containsAny(input, "配送", "送达", "送货", "运送", "医疗物资", "药品")) return "DELIVERY";
        if (input.contains("去") || input.contains("导航") || input.contains("到")) return "NAVIGATION";
        if (input.contains("电量") || input.contains("电池")) return "QUERY_BATTERY";
        if (containsAny(input, "多久", "要多久", "还要", "到哪里", "到了吗", "状态")) return "QUERY_STATUS";
        if (input.contains("停车") || input.contains("停下")) return "STOP";
        if (input.contains("充电")) return "CHARGE";
        if (input.contains("状态") || input.contains("情况")) return "QUERY_STATUS";
        if (input.contains("应急") || input.contains("紧急")) return "EMERGENCY";
        return "UNKNOWN";
    }

    private boolean containsAny(String input, String... keywords) {
        if (input == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void applyPolicyTopics(PubSubDecisionMaker.PubSubDecision decision, String intent) {
        if ("COORDINATION".equals(intent) && policy.isHcpModeEnabled()) {
            if (!decision.subscribeTopics.contains("MissionStatus")) {
                decision.subscribeTopics.add("MissionStatus");
            }
        }
        if ("COORDINATION".equals(intent) && policy.isGlobalBackboneEnabled()) {
            if (!decision.publishTopics.contains("CompliantBackboneSync")) {
                decision.publishTopics.add("CompliantBackboneSync");
            }
            if (!decision.subscribeTopics.contains("GlobalPerception")) {
                decision.subscribeTopics.add("GlobalPerception");
            }
        }
        if (policy.isTclSemanticEnabled() && ("DELIVERY".equals(intent) || "COORDINATION".equals(intent))) {
            if (!decision.publishTopics.contains("SemanticTaskDSL")) {
                decision.publishTopics.add("SemanticTaskDSL");
            }
        }
    }
    
    /**
     * 设备角色识别
     */
    private CPSEnums.DeviceRole identifyDeviceRole(String input) {
        if (input.contains("无人机") || input.contains("大疆") || input.contains("飞")) {
            return CPSEnums.DeviceRole.DRONE;
        }
        if (input.contains("控制中心") || input.contains("后台")) {
            return CPSEnums.DeviceRole.CONTROL_CENTER;
        }
        return CPSEnums.DeviceRole.VEHICLE;  // 默认车辆
    }
    
    /**
     * 执行发布/订阅
     */
    private void executePublishSubscribe(PubSubDecisionMaker.PubSubDecision decision) {
        // 发布数据
        for (String topic : decision.getPublishTopics()) {
            publishData(topic);
        }
        
        // 订阅数据
        for (String topic : decision.getSubscribeTopics()) {
            subscribeData(topic);
        }
    }
    
    /**
     * 发布数据
     */
    private void publishData(String topic) {
        System.out.println("  [发布] " + topic);
        
        // 根据主题发布对应数据（模拟）
        switch (topic) {
            case "VehicleData":
                System.out.println("    -> 发布车辆数据: 位置、速度、电量");
                knownData.put("VehicleData", createMockVehicleData());
                break;
                
            case "DroneStatus":
                System.out.println("    -> 发布无人机状态: 位置、高度、电量");
                knownData.put("DroneStatus", createMockDroneData());
                break;
                
            case "MissionRequest":
                System.out.println("    -> 发布任务请求");
                break;

            case "SubscriptionIntent":
                System.out.println("    -> 发布订阅意图: 请求前方路口/空域协同数据");
                if (ddsBridge.isEnabled()) {
                    ddsBridge.publishSeedForScope(currentScenarioScope);
                }
                break;
                
            case "EmergencyAlert":
                System.out.println("    -> 发布紧急告警");
                break;
                
            case "ChargeRequest":
                System.out.println("    -> 发布充电请求");
                break;
                
            default:
                System.out.println("    -> 发布数据: " + topic);
        }
    }
    
    private Map<String, Object> createMockVehicleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("vehicle_id", "VEH_" + System.currentTimeMillis() % 1000);
        data.put("speed", 30.0);
        data.put("battery_level", 85.0);
        data.put("position", "29.5,106.5");
        return data;
    }
    
    private Map<String, Object> createMockDroneData() {
        Map<String, Object> data = new HashMap<>();
        data.put("drone_id", "DRONE_" + System.currentTimeMillis() % 1000);
        data.put("altitude", 120.0);
        data.put("battery_level", 92.0);
        data.put("position", "29.55,106.52");
        return data;
    }
    
    /**
     * 订阅数据
     */
    private void subscribeData(String topic) {
        System.out.println("  [订阅] " + topic);
        
        switch (topic) {
            case "IntersectionPerception":
                System.out.println("    <- 等待路口感知数据...");
                // 模拟接收到数据
                subscribedDataCache.put("IntersectionPerception", 
                    Arrays.asList(createMockPerceptionData()));
                break;
                
            case "TrafficFlow":
                System.out.println("    <- 等待交通流数据...");
                subscribedDataCache.put("TrafficFlow", 
                    Arrays.asList(createMockTrafficData()));
                break;
                
            case "DroneStatus":
                System.out.println("    <- 等待无人机状态...");
                subscribedDataCache.put("DroneStatus", 
                    Arrays.asList(createMockDroneStatusData()));
                break;

            case "Traffic/Intersection/Forward":
                System.out.println("    <- 等待前方路口交通数据...");
                subscribeSemanticTopic(
                    "Traffic/Intersection/Forward",
                    createMockForwardIntersectionTraffic()
                );
                break;

            case "UAV/Perception/ForwardCorridor":
                System.out.println("    <- 等待无人机前方走廊感知...");
                subscribeSemanticTopic(
                    "UAV/Perception/ForwardCorridor",
                    createMockForwardCorridorPerception()
                );
                break;

            case "CompliantBackbone/GlobalPerception":
            case "GlobalPerception":
                System.out.println("    <- 等待合规广域全局感知...");
                Object globalData = subscribeSemanticTopic(
                    "CompliantBackbone/GlobalPerception",
                    createMockCompliantBackbonePerception()
                );
                subscribedDataCache.put("GlobalPerception", Arrays.asList(globalData));
                break;

            case "Route/Replan/Trigger":
                System.out.println("    <- 等待重规划触发事件...");
                subscribeSemanticTopic(
                    "Route/Replan/Trigger",
                    createMockReplanTrigger()
                );
                break;
                
            default:
                System.out.println("    <- 等待数据: " + topic);
                subscribedDataCache.put(topic, new ArrayList<>());
        }
    }

    private Object subscribeSemanticTopic(String topic, Object fallbackData) {
        if (ddsBridge.isEnabled()) {
            ddsBridge.publishSeed(topic, currentScenarioScope);
            Map<String, Object> ddsData = ddsBridge.awaitLatest(topic, 200);
            if (ddsData != null && !ddsData.isEmpty()) {
                subscribedDataCache.put(topic, Arrays.asList(ddsData));
                System.out.println("    <- 已接收真实DDS数据: " + topic);
                return ddsData;
            }
            System.out.println("    <- DDS暂无数据，回退模拟数据");
        }

        subscribedDataCache.put(topic, Arrays.asList(fallbackData));
        return fallbackData;
    }
    
    private Object createMockPerceptionData() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "IntersectionPerception");
        data.put("congestion", "MEDIUM");
        data.put("clearPath", true);
        return data;
    }
    
    private Object createMockTrafficData() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "TrafficFlow");
        data.put("congestionLevel", 0.6);
        data.put("vehicleCount", 45);
        return data;
    }
    
    private Object createMockDroneStatusData() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "DroneStatus");
        data.put("available", true);
        data.put("batteryLevel", 92.0);
        return data;
    }

    private Object createMockForwardIntersectionTraffic() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "Traffic/Intersection/Forward");
        data.put("sourceType", "VEHICLE");
        data.put("sourceId", "RSU_N10");
        data.put("scope", currentScenarioScope);
        data.put("intersectionId", "INT-N10");
        data.put("queueLength", 12);
        data.put("congestionLevel", 0.72);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    private Object createMockForwardCorridorPerception() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "UAV/Perception/ForwardCorridor");
        data.put("sourceType", "DRONE");
        data.put("sourceId", "UAV_A19");
        data.put("scope", currentScenarioScope);
        data.put("corridorId", "COR-N10");
        data.put("clearPath", false);
        data.put("riskScore", 0.65);
        data.put("congestionLevel", 0.68);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    private Object createMockCompliantBackbonePerception() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "CompliantBackbone/GlobalPerception");
        data.put("sourceType", "CN_LEO_SATCOM_OR_5GA_EDGE");
        data.put("sourceId", "CN_BACKBONE_CHONGQING_01");
        data.put("scope", currentScenarioScope);
        data.put("globalRisk", 0.58);
        data.put("cityCongestion", 0.66);
        data.put("congestionLevel", 0.66);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    private Object createMockReplanTrigger() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "Route/Replan/Trigger");
        data.put("sourceType", "AUTONOMOUS_SWARM");
        data.put("sourceId", "CAR_UAV_COLLAB_01");
        data.put("scope", currentScenarioScope);
        data.put("triggerReason", "ahead_congestion_spike");
        data.put("severity", "HIGH");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
    
    /**
     * 路线优化
     */
        private OptimizedRoute optimizeRoute(String origin, String destination,
            Map<String, List<Object>> subscribedData,
            CollaborativeDataFusionEngine.FusionResult fusionResult) {
        System.out.println("  分析订阅数据用于路线优化...");
        
        // 收集路况信息
        double congestionFactor = 1.0;
        boolean hasClearPath = false;
        
        // 分析交通流数据
        List<Object> trafficData = subscribedData.get("TrafficFlow");
        if (trafficData != null && !trafficData.isEmpty()) {
            for (Object data : trafficData) {
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    if (dataMap.containsKey("congestionLevel")) {
                        congestionFactor = ((Number) dataMap.get("congestionLevel")).doubleValue();
                        System.out.println("    拥堵指数: " + congestionFactor);
                    }
                }
            }
        }

        List<Object> forwardTraffic = subscribedData.get("Traffic/Intersection/Forward");
        if (forwardTraffic != null && !forwardTraffic.isEmpty()) {
            for (Object data : forwardTraffic) {
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    if (dataMap.containsKey("congestionLevel")) {
                        congestionFactor = Math.max(congestionFactor,
                            ((Number) dataMap.get("congestionLevel")).doubleValue());
                    }
                }
            }
        }
        
        // 分析感知数据
        List<Object> perceptionData = subscribedData.get("IntersectionPerception");
        if (perceptionData != null && !perceptionData.isEmpty()) {
            for (Object data : perceptionData) {
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    if (Boolean.TRUE.equals(dataMap.get("clearPath"))) {
                        hasClearPath = true;
                    }
                }
            }
        }
        
        // 调用高德API获取基础路线
        String originalRoute = "默认路线";
        int baseTime = 30;
        
        // 计算优化后的路线
        if (fusionResult != null) {
            congestionFactor = Math.max(congestionFactor, fusionResult.fusedCongestion);
            if (fusionResult.replanRecommended) {
                hasClearPath = true;
                System.out.println("    触发重规划: 多源融合建议切换路径");
            }
        }

        OptimizedRoute route = routeOptimizer.optimize(
            originalRoute, baseTime, congestionFactor, hasClearPath);
        
        return route;
    }
    
    /**
     * 接收外部订阅请求，判断能否满足
     */
    public boolean canSatisfyExternalRequest(String topic, CPSEnums.DeviceRole requester) {
        return dataMatcher.canSatisfyTopic(topic, currentRole, knownData);
    }
    
    /**
     * 获取订阅缓存的数据
     */
    public Map<String, List<Object>> getSubscribedData() {
        return new HashMap<>(subscribedDataCache);
    }

    private List<CollaborativeDataPacket> collectCollaborativePackets(List<String> subscribedTopics) {
        List<CollaborativeDataPacket> packets = new ArrayList<>();
        if (subscribedTopics == null || subscribedTopics.isEmpty()) {
            return packets;
        }
        for (String topic : subscribedTopics) {
            List<Object> dataList = subscribedDataCache.get(topic);
            if (dataList == null || dataList.isEmpty()) {
                continue;
            }
            for (Object obj : dataList) {
                if (!(obj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                String sourceType = String.valueOf(map.getOrDefault("sourceType", "UNKNOWN"));
                String sourceId = String.valueOf(map.getOrDefault("sourceId", "UNKNOWN"));
                String scope = String.valueOf(map.getOrDefault("scope", currentScenarioScope));
                if (scope == null || scope.isBlank() || "null".equalsIgnoreCase(scope) || "GLOBAL".equalsIgnoreCase(scope)) {
                    scope = currentScenarioScope;
                }
                long timestamp = ((Number) map.getOrDefault("timestamp", System.currentTimeMillis())).longValue();
                double confidence = 0.8;
                packets.add(new CollaborativeDataPacket(topic, sourceType, sourceId, scope, timestamp, confidence, map));
            }
        }
        return packets;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 设备角色
     */
    public enum DeviceRole {
        VEHICLE,      // 车辆
        DRONE,        // 无人机
        CONTROL_CENTER // 控制中心
    }
    
    /**
     * 任务阶段
     */
    public enum TaskPhase {
        IDLE,         // 空闲
        PLANNING,     // 规划中
        EXECUTING,    // 执行中
        MONITORING    // 监控中
    }
    
    /**
     * Agent处理结果
     */
    public static class AgentResult {
        public String originalInput;
        public String intent;
        public CPSEnums.DeviceRole deviceRole;
        public List<String> publishTopics = new ArrayList<>();
        public List<String> subscribeTopics = new ArrayList<>();
        public List<String> satisfiableTopics = new ArrayList<>();
        public double fusionScore;
        public boolean replanTriggered;
        public OptimizedRoute optimizedRoute;
        public boolean llmUsed;
        public double llmConfidence;
        public String llmSummary;
        public String llmProvider;
        public String llmModel;
        public long llmLatencyMs;
        public String llmRawContent;
        public String llmRawResponse;
        public double nluConfidence;
    }

    private boolean shouldAdoptIntent(String currentIntent, double nluConfidence, String suggestedIntent, double llmConfidence) {
        if (suggestedIntent == null || suggestedIntent.isBlank()) {
            return false;
        }
        if (currentIntent == null || currentIntent.isBlank() || "UNKNOWN".equalsIgnoreCase(currentIntent)) {
            return llmConfidence >= deepSeekClient.getAdoptConfidenceThreshold();
        }
        if (nluConfidence < 0.55 && llmConfidence >= deepSeekClient.getAdoptConfidenceThreshold()) {
            return true;
        }
        if (nluConfidence < 0.35 && llmConfidence >= 0.45) {
            return true;
        }
        return false;
    }

    private void mergeDeepSeekTopics(PubSubDecisionMaker.PubSubDecision decision, DeepSeekClient.DeepSeekSuggestion suggestion) {
        if (decision == null || suggestion == null) {
            return;
        }
        for (String topic : suggestion.publishTopics) {
            if (topic != null && !topic.isBlank() && !decision.publishTopics.contains(topic)) {
                decision.publishTopics.add(topic);
            }
        }
        for (String topic : suggestion.subscribeTopics) {
            if (topic != null && !topic.isBlank() && !decision.subscribeTopics.contains(topic)) {
                decision.subscribeTopics.add(topic);
            }
        }
        if (suggestion.summary != null && !suggestion.summary.isBlank()) {
            decision.description = (decision.description == null ? "" : decision.description + " ") + "[DeepSeek: " + suggestion.summary + "]";
        }
    }

    /**
     * 通过反射接入DDS语义主题桥，避免无DDS环境时影响主流程。
     */
    private static class SemanticDDSBridge {
        private static final String TOPIC_FORWARD_TRAFFIC = "Traffic/Intersection/Forward";
        private static final String TOPIC_FORWARD_CORRIDOR = "UAV/Perception/ForwardCorridor";
        private static final String TOPIC_GLOBAL_PERCEPTION = "CompliantBackbone/GlobalPerception";
        private static final String TOPIC_REPLAN_TRIGGER = "Route/Replan/Trigger";

        private final Map<String, Map<String, Object>> latestPayloads = new ConcurrentHashMap<>();
        private boolean enabled;

        private Object producer;
        private java.lang.reflect.Method publishMethod;

        SemanticDDSBridge() {
            this.enabled = init();
        }

        boolean isEnabled() {
            return enabled;
        }

        private boolean init() {
            try {
                Class<?> managerClass = Class.forName("dds.DDSManager");
                Object manager = managerClass.getMethod("getInstance").invoke(null);

                boolean connected = (Boolean) managerClass.getMethod("connect").invoke(manager);
                if (!connected) {
                    System.out.println("[DDS-Bridge] DDS连接失败，使用模拟模式");
                    return false;
                }

                Class<?> producerClass = Class.forName("dds.CoordinationTopicProducer");
                this.producer = producerClass.getConstructor(managerClass).newInstance(manager);
                this.publishMethod = producerClass.getMethod("publish", String.class, Map.class);

                Class<?> consumerClass = Class.forName("dds.CoordinationTopicConsumer");
                Object consumer = consumerClass.getConstructor(managerClass).newInstance(manager);
                java.lang.reflect.Method onSemanticTopic = consumerClass.getMethod("onSemanticTopic", String.class, BiConsumer.class);

                registerTopicHandler(onSemanticTopic, consumer, TOPIC_FORWARD_TRAFFIC);
                registerTopicHandler(onSemanticTopic, consumer, TOPIC_FORWARD_CORRIDOR);
                registerTopicHandler(onSemanticTopic, consumer, TOPIC_GLOBAL_PERCEPTION);
                registerTopicHandler(onSemanticTopic, consumer, TOPIC_REPLAN_TRIGGER);

                consumerClass.getMethod("subscribeAll").invoke(consumer);
                consumerClass.getMethod("subscribeReplanTrigger").invoke(consumer);

                System.out.println("[DDS-Bridge] 已启用真实DDS语义主题桥");
                return true;
            } catch (Throwable t) {
                System.out.println("[DDS-Bridge] 初始化失败，回退模拟模式: " + t.getMessage());
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        private void registerTopicHandler(java.lang.reflect.Method onSemanticTopic, Object consumer, String topic) throws Exception {
            BiConsumer<String, Map<String, Object>> handler = (t, payload) -> {
                if (payload != null) {
                    latestPayloads.put(normalizeTopic(t), new HashMap<>(payload));
                }
            };
            onSemanticTopic.invoke(consumer, topic, handler);
        }

        void publishSeedForScope(String scope) {
            publishSeed(TOPIC_FORWARD_TRAFFIC, scope);
            publishSeed(TOPIC_FORWARD_CORRIDOR, scope);
            publishSeed(TOPIC_GLOBAL_PERCEPTION, scope);
            publishSeed(TOPIC_REPLAN_TRIGGER, scope);
        }

        void publishSeed(String topic, String scope) {
            if (!enabled) {
                return;
            }
            try {
                Map<String, Object> payload = createSeedPayload(topic, scope);
                publishMethod.invoke(producer, normalizeTopic(topic), payload);
            } catch (Throwable ignored) {
                // 保持主流程稳定，DDS失败时由上层回退模拟数据
            }
        }

        Map<String, Object> awaitLatest(String topic, long timeoutMillis) {
            String normalized = normalizeTopic(topic);
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < timeoutMillis) {
                Map<String, Object> payload = latestPayloads.get(normalized);
                if (payload != null && !payload.isEmpty()) {
                    return new HashMap<>(payload);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return null;
        }

        private Map<String, Object> createSeedPayload(String topic, String scope) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("scope", scope != null ? scope : "GLOBAL");
            payload.put("timestamp", System.currentTimeMillis());

            switch (normalizeTopic(topic)) {
                case TOPIC_FORWARD_TRAFFIC:
                    payload.put("intersectionId", "INT-SCOPE-01");
                    payload.put("queueLength", 12);
                    payload.put("averageSpeed", 11.0);
                    payload.put("congestionLevel", 0.72);
                    payload.put("latitude", 29.5652);
                    payload.put("longitude", 106.4571);
                    break;
                case TOPIC_FORWARD_CORRIDOR:
                    payload.put("sourceId", "UAV-EDGE-01");
                    payload.put("temperature", 22.0);
                    payload.put("windSpeed", 8.5);
                    payload.put("visibility", 1500.0);
                    payload.put("riskScore", 0.63);
                    payload.put("illumination", 68);
                    break;
                case TOPIC_GLOBAL_PERCEPTION:
                    payload.put("sourceId", "CN-BACKBONE-NODE-01");
                    payload.put("sourceType", "CN_LEO_SATCOM_OR_5GA_EDGE");
                    payload.put("globalFlowSpeed", 17.5);
                    payload.put("heading", 40.0);
                    payload.put("healthScore", 97);
                    payload.put("latitude", 29.5600);
                    payload.put("longitude", 106.5400);
                    payload.put("altitude", 500.0);
                    break;
                case TOPIC_REPLAN_TRIGGER:
                    payload.put("alertId", 9001);
                    payload.put("triggerReason", "forward_congestion_spike");
                    payload.put("recommendation", "切换备选路线");
                    payload.put("latitude", 29.5652);
                    payload.put("longitude", 106.4571);
                    payload.put("radius", 600.0);
                    break;
                default:
                    break;
            }
            return payload;
        }

        private String normalizeTopic(String topic) {
            if ("GlobalPerception".equals(topic)) {
                return TOPIC_GLOBAL_PERCEPTION;
            }
            return topic;
        }
    }
    
    /**
     * 优化后的路线
     */
    public static class OptimizedRoute {
        public String description;
        public int estimatedTime;
        public double distanceKm;
        public boolean rerouted;
        
        public OptimizedRoute(String desc, int time, double dist, boolean reroute) {
            this.description = desc;
            this.estimatedTime = time;
            this.distanceKm = dist;
            this.rerouted = reroute;
        }
    }
    
    // ========== 测试入口 ==========
    public static void main(String[] args) {
        CPSAgent agent = new CPSAgent();
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("       CPS Agent 智能协同系统 - 演示");
        System.out.println("═══════════════════════════════════════════════════════");
        
        // 场景1: 车辆导航
        System.out.println("\n\n【场景1】车辆导航去西南大学");
        AgentResult result1 = agent.processInput("我想去西南大学，非常赶时间");
        System.out.println("\n决策摘要:");
        System.out.println("  - 发布: " + result1.publishTopics);
        System.out.println("  - 订阅: " + result1.subscribeTopics);
        System.out.println("  - 可满足需求: " + result1.satisfiableTopics);
        
        // 场景2: 无人机任务
        System.out.println("\n\n═══════════════════════════════════════════════════════");
        System.out.println("\n【场景2】无人机执行配送任务");
        AgentResult result2 = agent.processInput("启动无人机配送任务到观音桥");
        System.out.println("\n决策摘要:");
        System.out.println("  - 发布: " + result2.publishTopics);
        System.out.println("  - 订阅: " + result2.subscribeTopics);
        
        // 场景3: 查询电量
        System.out.println("\n\n═══════════════════════════════════════════════════════");
        System.out.println("\n【场景3】查询电池状态");
        AgentResult result3 = agent.processInput("电量还够吗");
        System.out.println("\n决策摘要:");
        System.out.println("  - 发布: " + result3.publishTopics);
        System.out.println("  - 订阅: " + result3.subscribeTopics);
        
        System.out.println("\n\n═══════════════════════════════════════════════════════");
        System.out.println("       演示完成！");
        System.out.println("═══════════════════════════════════════════════════════");
    }
}
