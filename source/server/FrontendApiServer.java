package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;

import api.PlatformApiFacade;
import agent.DeepSeekConfig;
import agent.DeepSeekClient;
import agent.DeviceRegistry;
import agent.DashboardService;
import agent.FaultInjectionService;
import agent.MissionHistoryService;
import agent.MissionExecutionService;
import agent.PlatformOrchestrator;
import agent.ReportService;
import agent.ScenarioLibrary;
import evaluation.BaselineComparison;
import nlu.NLToDSLToJavaPipeline;
import policy.PolicyEvaluator;
import policy.PolicyOptimizationService;
import policy.RouteTrainingService;
import collaboration.ros2.Ros2DdsAdapter;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalRuntimeScenario;
import collaboration.runtime.RuntimeScenarioResult;
import simulation.SimulationScenarioService;
import simulation.SimulationTickResult;
import simulation.VirtualTrafficSimulationService;
import utils.AmapRealAPI;

public class FrontendApiServer {
    private static final Gson GSON = new Gson();
    private static final List<Map<String, Object>> MISSIONS = new ArrayList<Map<String, Object>>();
    private static final List<Map<String, Object>> DEVICES = new ArrayList<Map<String, Object>>();
    private static final PlatformOrchestrator PLATFORM = new PlatformOrchestrator();
    private static final PlatformApiFacade API_FACADE = new PlatformApiFacade();
    private static final Map<String, Map<String, Object>> PLATFORM_PLANS = new HashMap<String, Map<String, Object>>();
    private static final MissionExecutionService EXECUTION = MissionExecutionService.getInstance();
    private static final DeviceRegistry DEVICE_REGISTRY = DeviceRegistry.getInstance();
    private static final ScenarioLibrary SCENARIOS = ScenarioLibrary.getInstance();
    private static final FaultInjectionService FAULTS = FaultInjectionService.getInstance();
    private static final MissionHistoryService HISTORY = MissionHistoryService.getInstance();
    private static final DashboardService DASHBOARD = new DashboardService();
    private static final ReportService REPORTS = new ReportService();
    private static final SimulationScenarioService SIMULATION = new SimulationScenarioService();
    private static final VirtualTrafficSimulationService TRAFFIC_SIMULATION = new VirtualTrafficSimulationService();
    private static final BaselineComparison BASELINE_COMPARISON = new BaselineComparison();
    private static final PolicyEvaluator POLICY_EVALUATOR = new PolicyEvaluator();
    private static final PolicyOptimizationService POLICY_OPTIMIZER = new PolicyOptimizationService();
    private static final RouteTrainingService ROUTE_TRAINING = new RouteTrainingService();

    static {
        MISSIONS.add(mission("MSN-VDDS-001", "乘客希望尽快到达西南大学附属医院，前方拥堵时由车辆自主询问邻车、无人机和 RSU", "running", "乘客意图驱动路线协同"));
        MISSIONS.add(mission("MSN-VDDS-000", "虚拟 DDS 场景验证：自然语言 -> DSL -> DDS/IDL -> 多设备建议融合", "success", "虚拟 DDS 闭环验证"));

        DEVICES.add(device("CABIN-CAR-CQ-001", "PASSENGER_CABIN_NODE", true, 8, "座舱自然语言意图节点", 100, "In-Vehicle Bus"));
        DEVICES.add(device("CAR-CQ-001", "EGO_AUTONOMOUS_VEHICLE", true, 14, "车端自主决策节点", 82, "DDS / C-V2X"));
        DEVICES.add(device("CAR-CQ-002", "PEER_AUTONOMOUS_VEHICLE", true, 24, "邻近无人车建议节点", 79, "DDS / Ad Hoc Mesh"));
        DEVICES.add(device("UAV-CQ-007", "AERIAL_FORWARD_PERCEPTION", true, 32, "低空前视感知节点", 76, "DDS / Private 5G"));
        DEVICES.add(device("UAV-AEP84-001", "AERIAL_INTEROP_UAV", true, 27, "AEP84 标准互操作无人机", 71, "STANAG 4586 / AEP-84"));
        DEVICES.add(device("RSU-SCHOOL-009", "ROADSIDE_UNIT_SCHOOL_ZONE", true, 18, "路侧事件发布节点", 100, "Roadside Ethernet / DDS"));
        DEVICES.add(device("CN-BACKBONE-CQ", "COMPLIANT_CONTEXT_BACKBONE", true, 41, "合规广域态势节点", 100, "5G-A / DDS Router"));
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", withCors(FrontendApiServer::health));
        server.createContext("/api/auth/login", withCors(FrontendApiServer::login));
        server.createContext("/api/auth/logout", withCors(FrontendApiServer::logout));
        server.createContext("/api/capabilities", withCors(FrontendApiServer::listCapabilities));
        server.createContext("/api/capability/action", withCors(FrontendApiServer::capabilityAction));
        server.createContext("/api/nlu/parse", withCors(FrontendApiServer::parseIntent));
        server.createContext("/api/agent/plan", withCors(FrontendApiServer::generatePlan));
        server.createContext("/api/agent/execute", withCors(FrontendApiServer::executeMission));
        server.createContext("/api/agent/devices", withCors(FrontendApiServer::agentDevices));
        server.createContext("/api/agent/topics", withCors(FrontendApiServer::agentTopics));
        server.createContext("/api/agent/logs", withCors(FrontendApiServer::agentLogs));
        server.createContext("/api/agent/simulate", withCors(FrontendApiServer::agentSimulate));
        server.createContext("/api/agent/health", withCors(FrontendApiServer::agentHealth));
        server.createContext("/api/agent/scenarios", withCors(FrontendApiServer::agentScenarios));
        server.createContext("/api/agent/scenario", withCors(FrontendApiServer::agentScenario));
        server.createContext("/api/agent/scenario/plan", withCors(FrontendApiServer::agentScenarioPlan));
        server.createContext("/api/agent/faults", withCors(FrontendApiServer::agentFaults));
        server.createContext("/api/agent/fault/inject", withCors(FrontendApiServer::agentFaultInject));
        server.createContext("/api/agent/fault/clear", withCors(FrontendApiServer::agentFaultClear));
        server.createContext("/api/agent/topic-topology", withCors(FrontendApiServer::agentTopicTopology));
        server.createContext("/api/agent/history", withCors(FrontendApiServer::agentHistory));
        server.createContext("/api/agent/history/clear", withCors(FrontendApiServer::agentHistoryClear));
        server.createContext("/api/agent/dashboard", withCors(FrontendApiServer::agentDashboard));
        server.createContext("/api/agent/report", withCors(FrontendApiServer::agentReport));
        server.createContext("/api/simulation/scenarios", withCors(FrontendApiServer::simulationScenarios));
        server.createContext("/api/simulation/start", withCors(FrontendApiServer::simulationStart));
        server.createContext("/api/simulation/tick", withCors(FrontendApiServer::simulationTick));
        server.createContext("/api/simulation/stop", withCors(FrontendApiServer::simulationStop));
        server.createContext("/api/simulation/state", withCors(FrontendApiServer::simulationState));
        server.createContext("/api/simulation/dds-events", withCors(FrontendApiServer::simulationDdsEvents));
        server.createContext("/api/runtime/profile", withCors(FrontendApiServer::runtimeProfile));
        server.createContext("/api/runtime/topics", withCors(FrontendApiServer::runtimeTopics));
        server.createContext("/api/runtime/run", withCors(FrontendApiServer::runtimeRun));
        server.createContext("/api/runtime/ros2/profile", withCors(FrontendApiServer::runtimeRos2Profile));
        server.createContext("/api/runtime/ros2/bridge-plan", withCors(FrontendApiServer::runtimeRos2BridgePlan));
        server.createContext("/api/mission/action", withCors(FrontendApiServer::missionAction));
        server.createContext("/api/device/action", withCors(FrontendApiServer::deviceAction));
        server.createContext("/api/topic/action", withCors(FrontendApiServer::topicAction));
        server.createContext("/api/missions", withCors(FrontendApiServer::listMissions));
        server.createContext("/api/devices", withCors(FrontendApiServer::listDevices));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[FrontendApiServer] listening on http://localhost:" + port + "/api");
    }

    private static HttpHandler withCors(ApiHandler handler) {
        return exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept-Language, X-Z-Link-Locale");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                handler.handle(exchange);
            } catch (IllegalArgumentException ex) {
                sendJson(exchange, 400, error(ex.getMessage()));
            } catch (Exception ex) {
                sendJson(exchange, 500, error(ex.getMessage()));
            }
        };
    }

    private static void health(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("service", "drone-intent-java-api");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "前后端 API 已融合，支持自然语言识别、Agent 规划、DDS 主题和设备动作。");
        DeepSeekConfig llm = new DeepSeekConfig();
        Map<String, Object> qwen = new LinkedHashMap<String, Object>();
        qwen.put("enabled", llm.isEnabled());
        qwen.put("provider", llm.getBaseUrl() != null && llm.getBaseUrl().toLowerCase().contains("dashscope") ? "qwen-compatible" : "deepseek-compatible");
        qwen.put("model", llm.getModel());
        qwen.put("baseUrl", llm.getBaseUrl());
        response.put("qwen", qwen);
        sendJson(exchange, 200, response);
    }

    private static void login(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String username = requiredString(body, "username");
        String password = requiredString(body, "password");
        if (!isValidLogin(username, password)) {
            sendJson(exchange, 401, actionResult(false, "用户名或密码错误"));
            return;
        }
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("id", "u-" + username.toLowerCase());
        user.put("username", username);
        user.put("displayName", "指挥员 " + username);
        user.put("role", "mission-commander");
        user.put("permissions", Arrays.asList("mission:write", "dds:publish", "device:dispatch", "capability:run"));

        Map<String, Object> response = actionResult(true, "登录成功，后端会话已建立");
        response.put("token", "local-session-" + username + "-" + System.currentTimeMillis());
        response.put("user", user);
        sendJson(exchange, 200, response);
    }

    private static void logout(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        sendJson(exchange, 200, actionResult(true, "已退出登录"));
    }

    private static void listCapabilities(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("updatedAt", System.currentTimeMillis());
        response.put("summary", "后端能力已按 NLU/DSL/Java、Agent/LLM、DDS/IDL、地图、协议车辆、Benchmark/Demo 统一暴露；硬件或长任务依赖先走模拟动作。");
        response.put("groups", backendCapabilityGroups());
        sendJson(exchange, 200, response);
    }

    private static void capabilityAction(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String capabilityId = requiredString(body, "capabilityId");
        String action = body.has("action") ? body.get("action").getAsString() : "run";
        String input = body.has("input") ? body.get("input").getAsString() : "从重庆市观音桥步行街出发，派无人机去解放碑巡检并保持 DDS 同步";

        Map<String, Object> response = actionResult(true, "能力 " + capabilityId + " 已执行动作：" + action);
        response.put("capabilityId", capabilityId);
        response.put("action", action);
        response.put("timestamp", System.currentTimeMillis());

        if ("nlu.pipeline".equals(capabilityId)) {
            PipelineArtifacts artifacts = runOriginalPipeline(input, "cap_" + System.currentTimeMillis());
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("dslPreview", artifacts.dslCode);
            data.put("dslFilePath", artifacts.dslFilePath);
            data.put("javaFilePath", artifacts.javaFilePath);
            data.put("javaClassName", artifacts.javaClassName);
            data.put("compileStatus", artifacts.compileStatus);
            data.put("javaCode", artifacts.javaCode);
            response.put("mode", artifacts.compileStatus.startsWith("ORIGINAL") ? "real" : "fallback");
            response.put("data", data);
        } else if ("agent.qwenHealth".equals(capabilityId)) {
            response.put("mode", "real");
            response.put("data", qwenHealth());
        } else if ("policy.rlTraining".equals(capabilityId)) {
            int episodes = body.has("episodes") ? body.get("episodes").getAsInt() : 120;
            response.put("mode", "simulation-training");
            response.put("data", ROUTE_TRAINING.runExperiment(episodes));
        } else if ("map.route".equals(capabilityId)) {
            response.put("mode", "real-with-fallback");
            response.put("data", mapOverlay(input));
        } else if ("dds.topicCatalog".equals(capabilityId)) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("subscribeTopics", capabilitySubscribeTopics());
            data.put("publishTopics", capabilityPublishTopics("MSN-CAPABILITY"));
            response.put("mode", "real-schema");
            response.put("data", data);
        } else if ("mission.list".equals(capabilityId)) {
            response.put("mode", "real");
            response.put("data", MISSIONS);
        } else if ("device.list".equals(capabilityId)) {
            response.put("mode", "real");
            response.put("data", DEVICES);
        } else {
            response.put("mode", "simulated");
            response.put("simulated", true);
            response.put("data", simulatedCapabilityData(capabilityId, action, input));
        }
        sendJson(exchange, 200, response);
    }

    private static void parseIntent(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        String text = requestText(exchange);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("intentSummary", summarizeIntent(text));
        response.put("confidence", text.length() > 15 ? 0.93 : 0.82);
        response.put("agentNarrative", agentNarrative(text));
        response.put("entities", Arrays.asList(
            pair("乘客意图", containsIgnoreCase(text, "安全") || containsIgnoreCase(text, "稳") ? "安全稳定优先" : "时效与风险平衡"),
            pair("目标地点", extractDestinationLabel(text)),
            pair("协同设备", "无人车 + 后车 + 无人机 + 合规广域中继")
        ));
        response.put("constraints", Arrays.asList("车辆必须自主融合建议并决策", "合规广域链路只提供态势，不进行远程派单", "DDS 订阅/发布主题需保持 QoS 与 IDL 类型一致"));
        response.put("risks", Arrays.asList("前方拥堵状态可能突变", "无人机前视感知存在短时延迟", "邻车建议与全局态势可能冲突，需要置信度融合"));
        response.put("naturalLanguagePlanHint", "Agent 将先发布乘客意图 DSL，再订阅后车、无人机、路口与合规广域态势，最后由车辆发布路线建议和重规划触发。");
        sendJson(exchange, 200, response);
    }

    private static void generatePlan(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        String text = requestText(exchange);
        String missionId = "MSN-" + System.currentTimeMillis();

        Map<String, Object> apiPlan = API_FACADE.generatePlan(text, missionId);
        @SuppressWarnings("unchecked")
        Map<String, Object> collaboration = (Map<String, Object>) apiPlan.get("collaboration");
        @SuppressWarnings("unchecked")
        Map<String, Object> platformPlan = (Map<String, Object>) apiPlan.get("platformPlan");
        @SuppressWarnings("unchecked")
        Map<String, Object> finalDecision = (Map<String, Object>) collaboration.get("finalDecision");

        List<Map<String, Object>> subscribeTopics = passengerSubscribeTopics();
        List<Map<String, Object>> publishTopics = passengerPublishTopics(missionId, finalDecision);

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        PipelineArtifacts artifacts = runOriginalPipeline(text, missionId);
        response.put("missionId", missionId);
        response.put("planNarrative", "Agent 已将乘客自然语言意图转换为车端自治协同任务，并完成 DDS/IDL 订阅发布映射。");
        response.put("agentNarrative", agentNarrative(text) + " 车辆作为决策主体，订阅后车、无人机、路口与合规广域态势建议；合规广域链路只做态势融合，不下发任务。");
        response.put("dslPreview", artifacts.dslCode);
        response.put("dslFilePath", artifacts.dslFilePath);
        response.put("javaFilePath", artifacts.javaFilePath);
        response.put("javaClassName", artifacts.javaClassName);
        response.put("compileStatus", artifacts.compileStatus);
        response.put("javaCode", artifacts.javaCode);
        response.put("steps", Arrays.asList(
            step("1", "识别乘客意图", "抽取目的地、时效偏好、安全偏好和协同提示", "done"),
            step("2", "生成 DSL/IDL 映射", "乘客意图、路线建议、环境感知和重规划触发映射到 DDS Topic", "done"),
            step("3", "自主订阅设备建议", "车辆订阅后车、无人机、路口感知和合规广域态势", "running"),
            step("4", "融合路线建议", "车辆按风险、置信度和链路延迟选择 " + finalDecision.get("route"), "pending")
        ));
        response.put("subscribeTopics", subscribeTopics);
        response.put("publishTopics", publishTopics);
        response.put("kpis", collaboration.get("experimentMetrics"));
        response.put("linkStrategy", linkStrategy("DDS over C-V2X / Ad Hoc Mesh", "5G-A 专网 / DDS Router / 合规低轨卫星互联网预留接口", "本地自治优先，合规广域链路只提供态势同步，不进行远程派单"));
        response.put("mapOverlay", mapOverlay(text));
        response.put("collaboration", collaboration);
        response.put("platformPlan", platformPlan);
        response.put("structuredDsl", platformPlan.get("structuredDsl"));
        response.put("ddsSimulation", platformPlan.get("ddsSimulation"));
        response.put("communicationLogs", platformPlan.get("communicationLogs"));
        response.put("riskWarnings", platformPlan.get("riskWarnings"));
        response.put("riskAssessment", platformPlan.get("riskAssessment"));
        response.put("topicTopology", platformPlan.get("topicTopology"));
        response.put("activeFaults", platformPlan.get("activeFaults"));
        response.put("fallbackStrategies", platformPlan.get("fallbackStrategies"));
        response.put("ddsRuntimeMode", platformPlan.get("ddsRuntimeMode"));
        response.put("executionTimeline", platformPlan.get("executionTimeline"));
        response.put("dashboardTrace", dashboardTrace(text, collaboration, platformPlan, artifacts));

        MISSIONS.add(0, mission(missionId, text, "pending", summarizeIntent(text)));
        PLATFORM_PLANS.put(missionId, platformPlan);
        sendJson(exchange, 200, response);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dashboardTrace(String text, Map<String, Object> collaboration,
                                                       Map<String, Object> platformPlan, PipelineArtifacts artifacts) {
        Map<String, Object> scenario = SCENARIOS.get("hospital-emergency");
        Map<String, Object> scenarioRun = SIMULATION.runScenario("hospital-emergency");
        Map<String, Object> finalDecision = castMap(collaboration.get("finalDecision"));
        Map<String, Object> scenarioDecision = castMap(scenarioRun.get("finalDecision"));
        Map<String, Object> policyOptimization = POLICY_OPTIMIZER.optimize();

        Map<String, Object> trace = new LinkedHashMap<String, Object>();
        trace.put("passengerInput", text);
        trace.put("parsedIntent", parsedIntentSummary(text));
        trace.put("dslTaskContract", platformPlan.get("structuredDsl"));
        trace.put("dslPreview", artifacts.dslCode);
        trace.put("idlDdsTopicMappings", collaboration.get("idlMappings"));
        trace.put("ddsTimeline", scenarioRun.get("ddsTimeline"));
        trace.put("agentSuggestions", categorizedAdvice((List<Map<String, Object>>) collaboration.get("deviceAdvice")));
        trace.put("allAgentSuggestions", collaboration.get("deviceAdvice"));
        trace.put("conflictDetection", conflictDetection(scenario, scenarioDecision));
        trace.put("conflictResolution", scenarioDecision);
        trace.put("finalRoute", finalDecision.get("route"));
        trace.put("routeSelectionReason", finalDecision.get("reason"));
        trace.put("rejectedRoutes", scenarioDecision.get("rejectedSuggestions"));
        trace.put("replanningTriggered", scenarioDecision.get("whetherReplanningTriggered"));
        trace.put("plannerComparison", BASELINE_COMPARISON.compareScenario(scenario));
        trace.put("policyComparison", POLICY_EVALUATOR.evaluateDefaultPolicies());
        trace.put("policyOptimization", policyOptimization);
        trace.put("experimentMetricCharts", metricCharts(collaboration, platformPlan, policyOptimization, scenarioRun));
        trace.put("replanningTimeline", replanningTimeline(collaboration, platformPlan, scenarioDecision));
        trace.put("displayCompleteness", Arrays.asList(
            "passengerInput", "parsedIntent", "dslTaskContract", "idlDdsTopicMappings", "ddsTimeline",
            "agentSuggestions", "conflictDetection", "conflictResolution", "finalRoute", "routeSelectionReason",
            "rejectedRoutes", "replanningTriggered", "plannerComparison", "policyComparison",
            "policyOptimization", "experimentMetricCharts", "replanningTimeline"
        ));
        trace.put("trainingStatus", "dashboard_uses_heuristic_evaluation_and_optimization_no_rl_training");
        return trace;
    }

    private static Map<String, Object> parsedIntentSummary(String text) {
        Map<String, Object> parsed = new LinkedHashMap<String, Object>();
        parsed.put("intentSummary", summarizeIntent(text));
        parsed.put("destination", extractDestinationLabel(text));
        parsed.put("urgent", containsIgnoreCase(text, "尽快") || containsIgnoreCase(text, "快"));
        parsed.put("safetyFirst", containsIgnoreCase(text, "安全") || containsIgnoreCase(text, "避开"));
        parsed.put("avoidCongestion", containsIgnoreCase(text, "拥堵") || containsIgnoreCase(text, "堵"));
        return parsed;
    }

    private static Map<String, Object> conflictDetection(Map<String, Object> scenario, Map<String, Object> decision) {
        Map<String, Object> detection = new LinkedHashMap<String, Object>();
        detection.put("hasConflict", Boolean.TRUE.equals(decision.get("hasConflict"))
            || decision.get("conflictType") != null
            || !((List<?>) scenario.getOrDefault("expectedConflicts", Arrays.asList())).isEmpty());
        detection.put("expectedConflicts", scenario.get("expectedConflicts"));
        detection.put("conflictType", decision.get("conflictType"));
        detection.put("timelineEvent", decision.get("timelineEvent"));
        detection.put("evidenceQuality", Arrays.asList("sourceTrust", "messageConfidence", "messageFreshness", "ddsDelayMs", "packetLossRate"));
        return detection;
    }

    private static Map<String, Object> categorizedAdvice(List<Map<String, Object>> advice) {
        Map<String, Object> categorized = new LinkedHashMap<String, Object>();
        categorized.put("vehicle", filterAdvice(advice, "CAR-CQ-001", "无人车"));
        categorized.put("drone", filterAdvice(advice, "UAV", "无人机"));
        categorized.put("roadside", filterAdvice(advice, "RSU", "路侧"));
        categorized.put("neighborVehicle", filterAdvice(advice, "CAR-CQ-002", "邻"));
        return categorized;
    }

    private static List<Map<String, Object>> filterAdvice(List<Map<String, Object>> advice, String idToken, String roleToken) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (advice == null) return result;
        for (Map<String, Object> item : advice) {
            String nodeId = String.valueOf(item.getOrDefault("nodeId", ""));
            String role = String.valueOf(item.getOrDefault("role", ""));
            if (nodeId.contains(idToken) || role.contains(roleToken)) {
                result.add(item);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> metricCharts(Map<String, Object> collaboration,
                                                           Map<String, Object> platformPlan,
                                                           Map<String, Object> optimization,
                                                           Map<String, Object> scenarioRun) {
        List<Map<String, Object>> charts = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : (List<Map<String, Object>>) collaboration.getOrDefault("experimentMetrics", Arrays.asList())) {
            charts.add(chartBar(String.valueOf(item.get("label")), parseMetricNumber(String.valueOf(item.get("value"))),
                String.valueOf(item.get("trend"))));
        }
        Map<String, Object> evaluation = castMap(scenarioRun.get("evaluation"));
        Map<String, Object> metrics = castMap(evaluation.get("metrics"));
        charts.add(chartBar("successRate", number(metrics, "successRate", 0.0) * 100.0, "scenario"));
        charts.add(chartBar("averageSafetyRisk", number(metrics, "averageSafetyRisk", 0.0) * 100.0, "scenario"));
        charts.add(chartBar("conflictResolutionAccuracy", number(metrics, "conflictResolutionAccuracy", 0.0) * 100.0, "scenario"));

        Map<String, Object> improvement = castMap(optimization.get("improvement"));
        charts.add(chartBar("averageRewardDelta", number(improvement, "averageRewardDelta", 0.0) * 10.0, "policy optimization"));
        charts.add(chartBar("conflictAccuracyDelta", number(improvement, "conflictResolutionAccuracyDelta", 0.0) * 100.0, "policy optimization"));
        charts.add(chartBar("DDS messages", ((List<?>) platformPlan.getOrDefault("ddsSimulation", Arrays.asList())).size() * 10.0, "timeline"));
        return charts;
    }

    private static Map<String, Object> chartBar(String label, double value, String note) {
        Map<String, Object> chart = new LinkedHashMap<String, Object>();
        chart.put("label", label);
        chart.put("value", Math.max(0.0, Math.min(100.0, Math.round(value * 10.0) / 10.0)));
        chart.put("note", note);
        return chart;
    }

    private static double parseMetricNumber(String value) {
        try {
            String normalized = value.replace("%", "").replace("ms", "").replace("/", ".").trim();
            return Double.parseDouble(normalized);
        } catch (Exception ignored) {
            return 50.0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> replanningTimeline(Map<String, Object> collaboration,
                                                                 Map<String, Object> platformPlan,
                                                                 Map<String, Object> decision) {
        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : (List<Map<String, Object>>) collaboration.getOrDefault("timeline", Arrays.asList())) {
            Map<String, Object> event = new LinkedHashMap<String, Object>(item);
            event.put("kind", String.valueOf(item.getOrDefault("title", "")).contains("重规划") ? "replanning" : "collaboration");
            timeline.add(event);
        }
        Object platformTimeline = platformPlan.get("executionTimeline");
        if (platformTimeline instanceof List<?>) {
            for (Object raw : (List<?>) platformTimeline) {
                if (raw instanceof Map<?, ?>) {
                    Map<String, Object> event = new LinkedHashMap<String, Object>((Map<String, Object>) raw);
                    event.put("kind", String.valueOf(event.getOrDefault("topic", "")).contains("replan") ? "replanning" : "execution");
                    timeline.add(event);
                }
            }
        }
        Map<String, Object> conflictEvent = new LinkedHashMap<String, Object>();
        conflictEvent.put("time", "T-conflict");
        conflictEvent.put("title", "冲突消解后重规划判断");
        conflictEvent.put("detail", "whetherReplanningTriggered=" + decision.get("whetherReplanningTriggered")
            + ", accepted=" + decision.get("acceptedSuggestion"));
        conflictEvent.put("kind", "replanning");
        timeline.add(conflictEvent);
        return timeline;
    }

    private static void agentDevices(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("devices", DEVICE_REGISTRY.listDevices());
        response.put("summary", DEVICE_REGISTRY.summary());
        response.put("message", "返回平台级设备模型，包括能力、电量、负载、链路质量、位置、状态和支持 Topic。");
        sendJson(exchange, 200, response);
    }

    private static void agentTopics(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("topics", PLATFORM.topicCatalog());
        response.put("runtimeMode", PLATFORM.ddsRuntimeMode());
        response.put("recentLogs", PLATFORM.latestLogs(30));
        response.put("message", "返回 DDS Topic、IDL 类型、默认 QoS、发布者/订阅者和最近通信日志。");
        sendJson(exchange, 200, response);
    }

    private static void agentLogs(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, String> query = queryParams(exchange);
        String missionId = query.getOrDefault("missionId", "");
        String runId = query.getOrDefault("runId", "");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("logs", PLATFORM.latestLogs(missionId, runId, 100));
        response.put("executionLogs", EXECUTION.logs(missionId, runId));
        response.put("filters", query);
        response.put("message", "返回最近的 DDS 发布、订阅、重试、失败、降级日志及执行时间线日志。");
        sendJson(exchange, 200, response);
    }

    private static void agentSimulate(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        String text;
        try {
            text = requestText(exchange);
        } catch (IllegalArgumentException ex) {
            text = "紧急调度无人车将患者送往医院，并请求无人机、RSU 和广域链路协同";
        }
        String missionId = "SIM-" + System.currentTimeMillis();
        Map<String, Object> simulation = PLATFORM.simulate(text, missionId);
        if (!PLATFORM_PLANS.containsKey(missionId)) PLATFORM_PLANS.put(missionId, PLATFORM.buildFullPlan(text, missionId));
        sendJson(exchange, 200, simulation);
    }

    private static void agentHealth(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        sendJson(exchange, 200, PLATFORM.health());
    }

    private static void agentScenarios(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("scenarios", SCENARIOS.list());
        response.put("message", "返回典型无人系统协同任务场景库。");
        sendJson(exchange, 200, response);
    }

    private static void agentScenario(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        String id = queryParams(exchange).getOrDefault("id", "medical-transfer");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("scenario", SCENARIOS.get(id));
        sendJson(exchange, 200, response);
    }

    private static void agentScenarioPlan(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String id = body.has("scenarioId") ? body.get("scenarioId").getAsString() : "medical-transfer";
        Map<String, Object> scenario = SCENARIOS.get(id);
        String text = String.valueOf(scenario.get("defaultIntentText"));
        String missionId = "SCN-" + id + "-" + System.currentTimeMillis();
        Map<String, Object> platformPlan = PLATFORM.buildFullPlan(text, missionId);
        platformPlan.put("scenario", scenario);
        PLATFORM_PLANS.put(missionId, platformPlan);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("missionId", missionId);
        response.put("scenario", scenario);
        response.put("platformPlan", platformPlan);
        response.put("structuredDsl", platformPlan.get("structuredDsl"));
        response.put("riskAssessment", platformPlan.get("riskAssessment"));
        response.put("ddsSimulation", platformPlan.get("ddsSimulation"));
        response.put("fallbackStrategies", platformPlan.get("fallbackStrategies"));
        sendJson(exchange, 200, response);
    }

    private static void agentFaults(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("faults", FAULTS.list());
        response.put("deviceSummary", DEVICE_REGISTRY.summary());
        sendJson(exchange, 200, response);
    }

    private static void agentFaultInject(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String type = body.has("faultType") ? body.get("faultType").getAsString() : body.has("type") ? body.get("type").getAsString() : "DEVICE_OFFLINE";
        String target = body.has("target") ? body.get("target").getAsString() : body.has("deviceId") ? body.get("deviceId").getAsString() : "UAV-CQ-007";
        String reason = body.has("reason") ? body.get("reason").getAsString() : "";
        Map<String, Object> response = actionResult(true, "故障已注入: " + type + " -> " + target);
        response.put("fault", FAULTS.inject(type, target, reason));
        response.put("faults", FAULTS.list());
        response.put("devices", DEVICE_REGISTRY.listDevices());
        sendJson(exchange, 200, response);
    }

    private static void agentFaultClear(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        Map<String, Object> response = FAULTS.clear();
        response.put("faults", FAULTS.list());
        response.put("devices", DEVICE_REGISTRY.listDevices());
        sendJson(exchange, 200, response);
    }

    private static void agentTopicTopology(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("topology", PLATFORM.topicTopology());
        sendJson(exchange, 200, response);
    }

    private static void agentHistory(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        String missionId = queryParams(exchange).getOrDefault("missionId", "");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("history", HISTORY.list(missionId));
        response.put("stats", HISTORY.stats());
        sendJson(exchange, 200, response);
    }

    private static void agentHistoryClear(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        HISTORY.clear();
        sendJson(exchange, 200, actionResult(true, "任务历史已清空"));
    }

    private static void agentDashboard(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("dashboard", DASHBOARD.build(DEVICE_REGISTRY, HISTORY, PLATFORM.dds(), FAULTS, PLATFORM.topicCatalog()));
        sendJson(exchange, 200, response);
    }

    private static void agentReport(HttpExchange exchange) throws IOException {
        String missionId = queryParams(exchange).getOrDefault("missionId", "");
        if (missionId.isBlank() && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonObject body = requestJson(exchange);
            if (body.has("missionId")) missionId = body.get("missionId").getAsString();
        } else {
            requireMethod(exchange, "GET");
        }
        Map<String, Object> plan = missionId.isBlank() ? latestPlan() : PLATFORM_PLANS.get(missionId);
        Map<String, Object> execution = EXECUTION.latestRun(missionId);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("missionId", missionId);
        response.put("markdown", REPORTS.buildMarkdown(plan, execution));
        sendJson(exchange, 200, response);
    }

    private static void simulationScenarios(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("scenarios", TRAFFIC_SIMULATION.listScenarios());
        response.put("simulationBoundary", "structured_2d_virtual_traffic_not_physical_3d_simulation");
        sendJson(exchange, 200, response);
    }

    private static void simulationStart(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String scenarioId = body.has("scenarioId") ? body.get("scenarioId").getAsString() : "hospital-emergency";
        Map<String, Object> response = TRAFFIC_SIMULATION.start(scenarioId);
        response.put("ok", true);
        response.put("message", "Virtual traffic collaboration simulation started.");
        sendJson(exchange, 200, response);
    }

    private static void simulationTick(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        SimulationTickResult tick = TRAFFIC_SIMULATION.tick();
        Map<String, Object> response = tick.toMap();
        response.put("ok", true);
        sendJson(exchange, 200, response);
    }

    private static void simulationStop(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        sendJson(exchange, 200, TRAFFIC_SIMULATION.stop());
    }

    private static void simulationState(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        sendJson(exchange, 200, TRAFFIC_SIMULATION.state());
    }

    private static void simulationDdsEvents(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("events", TRAFFIC_SIMULATION.ddsEvents());
        sendJson(exchange, 200, response);
    }

    private static void runtimeProfile(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("runtimeMode", "local_tcp_dds_compatible");
        response.put("coreSystemWithoutFrontend", true);
        response.put("defaultStatusIntervalMs", 500);
        response.put("defaultStatusRateHz", 2.0);
        response.put("supportedIntervalsMs", Arrays.asList(1000, 500, 200));
        response.put("communicationPattern", Arrays.asList("initial_intent_contract", "periodic_status_sync", "event_triggered_conflict_and_replan"));
        response.put("agents", Arrays.asList(
            runtimeAgent("UAV-CQ-007", "UAV", "DJI-STYLE", "runtime/agent/status", "runtime/protocol/native_frame"),
            runtimeAgent("UAV-AEP84-001", "UAV", "AEP84-STANAG4586", "runtime/agent/status", "runtime/protocol/native_frame"),
            runtimeAgent("CAR-CQ-001", "EGO_CAR", "ROS2-DDS", "runtime/agent/status", "runtime/protocol/native_frame"),
            runtimeAgent("PEER-CAR-031", "PEER_CAR", "GENERIC-PRIVATE-JSON", "runtime/agent/status", "runtime/protocol/native_frame"),
            runtimeAgent("COORDINATOR-CQ-001", "COORDINATOR", "DDS-COMPATIBLE", "runtime/conflict/event,runtime/replanning/request,runtime/decision/final", "runtime/policy/evaluation")
        ));
        response.put("ros2Boundary", "Local TCP broker can be replaced by a ROS 2/DDS Adapter without moving decision logic into the frontend.");
        sendJson(exchange, 200, response);
    }

    private static void runtimeTopics(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("idlFile", "idl/urban_intent_collaboration.idl");
        response.put("topics", Arrays.asList(
            runtimeTopic(LocalDDSBroker.TOPIC_INTENT_CONTRACT, "urban.RuntimeIntentContract", "RELIABLE_KEEP_LAST", "initial task contract"),
            runtimeTopic(LocalDDSBroker.TOPIC_AGENT_STATUS, "urban.RuntimeAgentStatus", "BEST_EFFORT_SENSOR_STREAM", "periodic status sync"),
            runtimeTopic(LocalDDSBroker.TOPIC_PROTOCOL_FRAME, "urban.ProtocolNativeFrame", "RELIABLE_KEEP_LAST", "adapter output for heterogeneous protocols"),
            runtimeTopic(LocalDDSBroker.TOPIC_CONFLICT_EVENT, "urban.ConflictEvent", "RELIABLE_LOW_LATENCY", "event triggered conflict detection"),
            runtimeTopic(LocalDDSBroker.TOPIC_REPLAN_REQUEST, "urban.ReplanningRequest", "RELIABLE_LOW_LATENCY", "dynamic replanning request"),
            runtimeTopic(LocalDDSBroker.TOPIC_FINAL_DECISION, "urban.RouteDecision", "RELIABLE_TRANSIENT_LOCAL", "explainable final decision"),
            runtimeTopic(LocalDDSBroker.TOPIC_POLICY_EVALUATION, "urban.PolicyEvaluationReport", "RELIABLE_KEEP_LAST", "baseline and cooperative policy comparison")
        ));
        sendJson(exchange, 200, response);
    }

    private static void runtimeRun(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        long intervalMs = body.has("intervalMs") ? body.get("intervalMs").getAsLong() : 500L;
        int ticks = body.has("ticks") ? body.get("ticks").getAsInt() : 6;
        if (intervalMs < 50L || intervalMs > 5_000L) {
            throw new IllegalArgumentException("intervalMs must be between 50 and 5000");
        }
        if (ticks < 1 || ticks > 40) {
            throw new IllegalArgumentException("ticks must be between 1 and 40 for API runtime runs");
        }

        RuntimeScenarioResult result;
        try {
            result = new LocalRuntimeScenario().run(intervalMs, ticks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("runtime run interrupted", e);
        }

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("runtimeMode", "local_tcp_dds_compatible");
        response.put("intervalMs", intervalMs);
        response.put("rateHz", 1000.0 / intervalMs);
        response.put("ticks", ticks);
        response.put("messageCount", result.history.size());
        response.put("history", result.history);
        response.put("agentStatuses", filterTopic(result, LocalDDSBroker.TOPIC_AGENT_STATUS));
        response.put("nativeProtocolFrames", filterTopic(result, LocalDDSBroker.TOPIC_PROTOCOL_FRAME));
        response.put("conflictEvents", filterTopic(result, LocalDDSBroker.TOPIC_CONFLICT_EVENT));
        response.put("replanningRequests", filterTopic(result, LocalDDSBroker.TOPIC_REPLAN_REQUEST));
        response.put("policyEvaluations", filterTopic(result, LocalDDSBroker.TOPIC_POLICY_EVALUATION));
        response.put("finalDecision", result.finalDecision == null ? new LinkedHashMap<String, Object>() : result.finalDecision.toMap());
        response.put("runtimeBoundary", "Core Agent communication runs in backend socket processes; frontend is observer only.");
        sendJson(exchange, 200, response);
    }

    private static void runtimeRos2Profile(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        Ros2DdsAdapter adapter = new Ros2DdsAdapter();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("ros2Available", adapter.isAvailable());
        response.put("ros2Executable", adapter.executable());
        response.put("fallbackMode", "local_tcp_dds_compatible_runtime");
        response.put("messageEncoding", "std_msgs/msg/String for quick interop; urban_runtime_msgs custom messages are available for structured ROS2 I/O");
        response.put("topicBindings", adapter.planBridge(null).topicBindings);
        response.put("installState", adapter.isAvailable() ? "ROS2 CLI detected" : "ROS2 CLI not detected; adapter remains in dry-run mode");
        sendJson(exchange, 200, response);
    }

    private static void runtimeRos2BridgePlan(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        long intervalMs = body.has("intervalMs") ? body.get("intervalMs").getAsLong() : 500L;
        int ticks = body.has("ticks") ? body.get("ticks").getAsInt() : 4;
        RuntimeScenarioResult result;
        try {
            result = new LocalRuntimeScenario().run(intervalMs, ticks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("runtime ROS2 bridge plan interrupted", e);
        }
        Ros2DdsAdapter adapter = new Ros2DdsAdapter();
        Map<String, Object> response = adapter.planBridge(result.history).toMap();
        response.put("ok", true);
        response.put("sourceRuntimeMessageCount", result.history.size());
        response.put("finalDecision", result.finalDecision == null ? new LinkedHashMap<String, Object>() : result.finalDecision.toMap());
        response.put("note", "Commands are dry-run unless ros2 CLI is detected; custom ROS2 interfaces are supported when ROS2_MESSAGE_MODE=custom.");
        sendJson(exchange, 200, response);
    }

    private static void executeMission(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String missionId = requiredString(body, "missionId");
        updateMissionStatus(missionId, "running");
        Map<String, Object> plan = PLATFORM_PLANS.get(missionId);
        if (plan == null) {
            String text = body.has("text") ? body.get("text").getAsString() : "紧急调度无人车将患者送往医院，并请求无人机、RSU 和广域链路协同";
            plan = PLATFORM.buildFullPlan(text, missionId);
            PLATFORM_PLANS.put(missionId, plan);
        }
        Map<String, Object> response = PLATFORM.executePlan(plan);
        response.put("message", "任务 " + missionId + " 已进入可回放执行仿真，runId=" + response.get("runId"));
        sendJson(exchange, 200, response);
    }

    private static void missionAction(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String missionId = requiredString(body, "missionId");
        String action = requiredString(body, "action");
        String status = "pending";
        if ("pause".equals(action)) status = "paused";
        if ("resume".equals(action) || "rerun".equals(action)) status = "running";
        if ("stop".equals(action)) status = "failed";
        updateMissionStatus(missionId, status);
        sendJson(exchange, 200, actionResult(true, "任务 " + missionId + " 已执行动作：" + action));
    }

    private static void deviceAction(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String deviceId = requiredString(body, "deviceId");
        String action = requiredString(body, "action");
        DeviceRegistry.DeviceRecord updated = DEVICE_REGISTRY.applyAction(deviceId, action);
        if (updated == null) {
            sendJson(exchange, 404, actionResult(false, "设备 " + deviceId + " 不存在"));
            return;
        }
        Map<String, Object> response = actionResult(true, "设备 " + deviceId + " 已执行动作：" + action);
        response.put("device", updated.toMap());
        response.put("summary", DEVICE_REGISTRY.summary());
        sendJson(exchange, 200, response);
    }

    private static void topicAction(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "POST");
        JsonObject body = requestJson(exchange);
        String topicName = requiredString(body, "topicName");
        String action = requiredString(body, "action");
        String payload = body.has("payload") ? body.get("payload").getAsString() : "";
        String detail = payload.trim().isEmpty() ? "" : "，payload=" + payload;
        sendJson(exchange, 200, actionResult(true, "主题 " + topicName + " 已执行动作：" + action + detail));
    }

    private static void listMissions(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        sendJson(exchange, 200, MISSIONS);
    }

    private static void listDevices(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET");
        sendJson(exchange, 200, DEVICE_REGISTRY.listDevices());
    }

    private static Map<String, Object> runtimeAgent(String id, String role, String protocol, String subscribes, String publishes) {
        Map<String, Object> agent = new LinkedHashMap<String, Object>();
        agent.put("id", id);
        agent.put("role", role);
        agent.put("protocolAdapter", protocol);
        agent.put("subscribes", Arrays.asList(subscribes.split(",")));
        agent.put("publishes", Arrays.asList(publishes.split(",")));
        return agent;
    }

    private static Map<String, Object> runtimeTopic(String topic, String idlType, String qos, String purpose) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("topic", topic);
        item.put("idlType", idlType);
        item.put("qos", qos);
        item.put("purpose", purpose);
        item.put("transport", "local_tcp_now_ros2_dds_adapter_later");
        return item;
    }

    private static List<Map<String, Object>> filterTopic(RuntimeScenarioResult result, String topic) {
        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : result.history) {
            if (topic.equals(item.get("topic"))) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private static List<Map<String, Object>> backendCapabilityGroups() {
        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        groups.add(capabilityGroup("NLU / DSL / Java", Arrays.asList(
            capability("nlu.pipeline", "自然语言 -> DSL -> Java", "nlu.NLToDSLToJavaPipeline.execute", "/api/agent/plan, /api/capability/action", "real", "已接入", "调用原项目流水线生成 DSL 文件和 Java 文件。", "run", "preview"),
            capability("nlu.dslGenerator", "DSL 语义生成器", "nlu.DSLGenerator / nlu.NLUProcessor", "/api/nlu/parse", "real+simulated", "已接入", "用于识别任务意图、实体和约束，页面动作复用解析接口。", "parse", "validate"),
            capability("integration.dslGateway", "DSL 外部交换网关", "integration.DSLExchangeGateway", "/api/capability/action", "simulated", "已暴露", "外部信息转 DSL、DSL 转自然语言、DDS payload 转换。", "convert", "export")
        )));
        groups.add(capabilityGroup("Agent / LLM", Arrays.asList(
            capability("agent.cps", "CPS Agent 决策", "agent.CPSAgent.processInput", "/api/agent/plan", "real+simulated", "已接入", "前端工作台的意图、步骤、设备、发布订阅策略由此类能力组合生成。", "run", "explain"),
            capability("agent.qwenHealth", "千问兼容 API 健康检查", "agent.DeepSeekClient.healthCheck", "/api/health, /api/capability/action", "real", "已接入", "读取 QWEN/DashScope 配置并可发起兼容模式健康检查。", "health", "config"),
            capability("agent.pubsubDecision", "发布订阅决策器", "agent.PubSubDecisionMaker.makeDecision", "/api/capability/action", "simulated", "已暴露", "根据角色、任务阶段和意图选择 DDS 发布/订阅主题。", "decide", "rules"),
            capability("agent.dataMatching", "协同数据匹配", "agent.DataMatchingEngine.matchProviderConsumer", "/api/capability/action", "simulated", "已暴露", "匹配无人机、无人车、邻车和合规广域中继之间可提供和需要的数据。", "match", "inspect"),
            capability("policy.rlTraining", "路线策略 Q-learning 训练原型", "policy.RouteTrainingService.runExperiment", "/api/capability/action", "simulation-training", "已接入", "在虚拟 DDS 多源证据环境中训练小规模表格 Q-learning 策略，并和随机/规则 baseline 对比；不声称 DQN/PPO 或真实车端训练。", "train", "compare")
        )));
        groups.add(capabilityGroup("DDS / IDL", Arrays.asList(
            capability("dds.topicCatalog", "DDS/IDL 主题目录", "dds.DDSManager + generated IDL TypeSupport", "/api/agent/plan, /api/capability/action", "real-schema", "已接入", "严格返回 DDS Topic、IDL module/type、TypeSupport、QoS、Key。", "list", "publish"),
            capability("dds.manager", "DDS 发布订阅管理", "dds.DDSManager.publish*/subscribe*", "/api/topic/action", "simulated", "已暴露", "真实 DDS writer/reader 需要运行时域和设备，此处先用 API 模拟发布订阅动作。", "publish", "subscribe"),
            capability("dds.idlMapper", "DSL 到 IDL 映射", "integration.DSLIDLMapper", "/api/capability/action", "simulated", "已暴露", "把任务 DSL 映射到 MissionCommand、SwarmMission、EmergencyAlert 等 IDL 数据。", "map", "sample"),
            capability("dds.logger", "DDS 日志与指标", "dds.DDSLogger / dds.MetricsCollector", "/api/capability/action", "simulated", "已暴露", "生成吞吐、延迟、发布订阅统计。", "report", "reset")
        )));
        groups.add(capabilityGroup("Map / Route", Arrays.asList(
            capability("map.route", "真实地图与路线", "utils.AmapRealAPI.geocode/driving + OpenStreetMap", "/api/agent/plan, /api/capability/action", "real-with-fallback", "已接入", "默认起点为重庆市观音桥步行街，识别目的地并返回路线坐标。", "route", "geocode"),
            capability("map.coordinate", "坐标转换", "utils.CoordinateTransformer", "/api/capability/action", "simulated", "已暴露", "用于高德、WGS84、设备坐标之间转换。", "transform", "check"),
            capability("map.service", "地图服务封装", "utils.MapService", "/api/capability/action", "simulated", "已暴露", "管理地理编码、路径规划、区域服务。", "query", "route")
        )));
        groups.add(capabilityGroup("Protocol / Vehicle", Arrays.asList(
            capability("protocol.router", "协议路由", "utils.ProtocolRouter", "/api/capability/action", "simulated", "已暴露", "在 DDS、HTTP、设备私有协议之间选择路由。", "route", "dryRun"),
            capability("protocol.adapter", "真实协议适配", "utils.ProtocolAdapter / RealProtocolAdapter", "/api/capability/action", "simulated", "已暴露", "把后端任务动作适配为设备侧协议帧。", "adapt", "send"),
            capability("vehicle.gateway", "无人设备网关", "vehicle.VehicleProtocolGateway", "/api/capability/action", "simulated", "已暴露", "为无人车、无人机和传感器提供任务指令桥接。", "dispatch", "inspect"),
            capability("vehicle.sensor", "传感器转 DSL 状态", "vehicle.SensorAdapter.toDSLState", "/api/capability/action", "simulated", "已暴露", "把传感器遥测转换为 DSL state。", "convert", "sample")
        )));
        groups.add(capabilityGroup("Benchmark / Demo", Arrays.asList(
            capability("benchmark.latency", "延迟基准", "benchmark.LatencyBenchmark.runAllTests", "/api/capability/action", "simulated", "已暴露", "长时间测试不在 UI 直接阻塞运行，返回模拟摘要。", "run", "report"),
            capability("benchmark.throughput", "吞吐基准", "benchmark.ThroughputBenchmark.runAllTests", "/api/capability/action", "simulated", "已暴露", "模拟发布/订阅吞吐和峰值字节率。", "run", "report"),
            capability("benchmark.stress", "协同压力测试", "benchmark.CoordinationStressBenchmark.run", "/api/capability/action", "simulated", "已暴露", "模拟多设备协同压力测试结果。", "run", "report"),
            capability("demo.e2e", "端到端 DDS Demo", "dds / integration demo scripts", "/api/capability/action", "simulated", "已暴露", "对应原有端到端脚本输出，用自然语言和结构化结果展示。", "play", "explain")
        )));
        return groups;
    }

    private static Map<String, Object> capabilityGroup(String name, List<Map<String, Object>> capabilities) {
        Map<String, Object> group = new LinkedHashMap<String, Object>();
        group.put("name", name);
        group.put("capabilities", capabilities);
        return group;
    }

    private static Map<String, Object> capability(String id, String name, String backendClass, String endpoint,
                                                  String mode, String status, String description, String actionA, String actionB) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", id);
        item.put("name", name);
        item.put("backendClass", backendClass);
        item.put("endpoint", endpoint);
        item.put("mode", mode);
        item.put("status", status);
        item.put("description", description);
        item.put("actions", Arrays.asList(actionA, actionB));
        return item;
    }

    private static Map<String, Object> qwenHealth() {
        try {
            DeepSeekClient.HealthCheckResult health = new DeepSeekClient().healthCheck();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("enabled", health.enabled);
            data.put("success", health.success);
            data.put("provider", health.provider);
            data.put("model", health.model);
            data.put("httpStatus", health.httpStatus);
            data.put("latencyMs", health.latencyMs);
            data.put("responseId", health.responseId);
            data.put("message", health.message);
            return data;
        } catch (Exception ex) {
            DeepSeekConfig llm = new DeepSeekConfig();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("enabled", llm.isEnabled());
            data.put("success", false);
            data.put("provider", llm.getBaseUrl() != null && llm.getBaseUrl().toLowerCase().contains("dashscope") ? "qwen-compatible" : "deepseek-compatible");
            data.put("model", llm.getModel());
            data.put("baseUrl", llm.getBaseUrl());
            data.put("message", ex.getMessage());
            return data;
        }
    }

    private static Map<String, Object> simulatedCapabilityData(String capabilityId, String action, String input) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("input", input);
        data.put("summary", "该能力已在前端接入后端动作入口；当前因为依赖真实 DDS 域、设备链路或长时间 benchmark，返回模拟结果。");
        data.put("events", Arrays.asList(
            "加载后端类: " + capabilityId,
            "执行动作: " + action,
            "生成结构化结果并写入前端运行日志"
        ));
        data.put("metrics", Arrays.asList(
            metric("模拟延迟", "18ms", "稳定"),
            metric("动作完成率", "100%", "通过"),
            metric("硬件依赖", "mock", "待接入真实设备")
        ));
        return data;
    }

    private static List<Map<String, Object>> capabilitySubscribeTopics() {
        return Arrays.asList(
            ddsTopic("PeerAdvice", "urban/vehicle/peer_advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "sub", "CAR-CQ-002", null, "ReliableKeepLast", "RELIABLE", "VOLATILE", "vehicle_id", 10, true, "route=route_C_elevated_detour risk=0.50"),
            ddsTopic("UavAerialPerception", "urban/uav/aerial_perception.dsl", "urban", "EnvironmentPerception", "urban.EnvironmentPerceptionTypeSupport", "sub", "UAV-CQ-007", null, "BestEffortSensorStream", "BEST_EFFORT", "VOLATILE", "uav_id", 15, true, "visibility=1700m route=route_C_elevated_detour"),
            ddsTopic("RsuRoadEvent", "urban/rsu/road_event.dsl", "urban", "RsuRoadEvent", "urban.RsuRoadEventTypeSupport", "sub", "RSU-SCHOOL-009", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "event_id", 5, true, "event=main_road_accident_and_congestion signal=RED"),
            ddsTopic("CompliantBackboneContext", "backbone/compliant/context.dsl", "urban", "CompliantNetworkContext", "urban.CompliantNetworkContextTypeSupport", "sub", "CN-BACKBONE-CQ", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "region_id", 2, true, "link=5G-A/DDS-Router policy=context_only_no_dispatch"),
            ddsTopic("RouteAdvice", "urban/route/advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "sub", "VEHICLE_SWARM", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "mission_id", 8, true, "route_candidate=route_D_emergency_green_wave confidence=0.86")
        );
    }

    private static List<Map<String, Object>> capabilityPublishTopics(String missionId) {
        return Arrays.asList(
            ddsTopic("PassengerIntent", "cabin/passenger/intent.dsl", "urban", "MissionCommand", "urban.MissionCommandTypeSupport", "pub", null, "CAR-CQ-001", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "intent_id", 5, true, "{\"mission_id\":\"" + missionId + "\",\"intent\":\"PASSENGER_NATURAL_LANGUAGE\"}"),
            ddsTopic("VehicleTelemetry", "urban/vehicle/telemetry.dsl", "urban", "VehicleTelemetry", "urban.VehicleTelemetryTypeSupport", "pub", null, "ALL", "BestEffortSensorStream", "BEST_EFFORT", "VOLATILE", "vehicle_id", 20, true, "egoX=28.0 roadEvent=main_road_accident_and_congestion"),
            ddsTopic("RouteAdvice", "urban/route/advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "pub", null, "ALL", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "mission_id,source_id", 8, true, "{\"route\":\"route_D_emergency_green_wave\",\"risk\":0.26}"),
            ddsTopic("FinalRouteDecision", "urban/route/final_decision.dsl", "urban", "RouteDecision", "urban.RouteDecisionTypeSupport", "pub", null, "CABIN-CAR-CQ-001", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "decision_id", 4, true, "{\"route\":\"route_D_emergency_green_wave\",\"owner\":\"CAR-CQ-001\"}")
        );
    }

    private static String requestText(HttpExchange exchange) throws IOException {
        JsonObject body = requestJson(exchange);
        return requiredString(body, "text");
    }

    private static JsonObject requestJson(HttpExchange exchange) throws IOException {
        String raw = readAll(exchange.getRequestBody());
        if (raw.trim().isEmpty()) {
            return new JsonObject();
        }
        JsonObject body = GSON.fromJson(raw, JsonObject.class);
        return body == null ? new JsonObject() : body;
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    private static Map<String, Object> latestPlan() {
        Map<String, Object> latest = null;
        for (Map<String, Object> plan : PLATFORM_PLANS.values()) {
            latest = plan;
        }
        return latest;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private static String requiredString(JsonObject body, String key) {
        if (!body.has(key) || body.get(key).getAsString().trim().isEmpty()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).getAsString();
    }

    private static String readAll(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        StringBuilder builder = new StringBuilder();
        int len;
        while ((len = input.read(buffer)) >= 0) {
            builder.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static void requireMethod(HttpExchange exchange, String expected) {
        if (!expected.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new IllegalArgumentException("Expected " + expected + " " + exchange.getRequestURI().getPath());
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static Map<String, Object> error(String message) {
        return actionResult(false, message == null ? "unknown error" : message);
    }

    private static boolean isValidLogin(String username, String password) {
        String normalized = username == null ? "" : username.trim();
        String configuredUsers = System.getenv("PLATFORM_USERS");
        if (configuredUsers != null && !configuredUsers.isBlank()) {
            for (String pair : configuredUsers.split(",")) {
                String[] parts = pair.split(":", 2);
                if (parts.length == 2 && normalized.equalsIgnoreCase(parts[0].trim())
                    && String.valueOf(password).equals(parts[1].trim())) {
                    return true;
                }
            }
            return false;
        }
        if ("admin".equalsIgnoreCase(normalized) && "admin123".equals(password)) {
            return true;
        }
        if ("operator".equalsIgnoreCase(normalized) && "operator123".equals(password)) {
            return true;
        }
        return "demo".equalsIgnoreCase(normalized) && "demo123".equals(password);
    }

    private static Map<String, Object> actionResult(boolean ok, String message) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", ok);
        response.put("message", message);
        return response;
    }

    private static void updateMissionStatus(String missionId, String status) {
        for (Map<String, Object> mission : MISSIONS) {
            if (missionId.equals(mission.get("id"))) {
                mission.put("status", status);
                return;
            }
        }
    }

    private static boolean containsIgnoreCase(String text, String target) {
        return text.toLowerCase().contains(target.toLowerCase());
    }

    private static String summarizeIntent(String text) {
        if (containsIgnoreCase(text, "安全") || containsIgnoreCase(text, "稳")) {
            return "乘客意图：安全稳定优先的无人设备自治路线协同";
        }
        if (containsIgnoreCase(text, "尽快") || containsIgnoreCase(text, "最快") || containsIgnoreCase(text, "快")) {
            return "乘客意图：尽快抵达的无人车-后车-无人机路线协同";
        }
        return "乘客意图驱动的多无人设备协同路线规划";
    }

    private static String agentNarrative(String text) {
        return "Agent 分析乘客输入：“" + text + "”。该输入表达了车内乘客的路线意图，需要把自然语言转换为任务契约，"
            + "再映射为 DDS 订阅/发布主题：发布乘客意图与车辆路线建议，订阅后车状态、无人机前视、路口交通流和合规广域态势。";
    }

    private static Map<String, Object> mission(String id, String sourceText, String status, String intent) {
        Map<String, Object> mission = new LinkedHashMap<String, Object>();
        mission.put("id", id);
        mission.put("sourceText", sourceText);
        mission.put("status", status);
        mission.put("intent", intent);
        mission.put("createdAt", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        return mission;
    }

    private static Map<String, Object> device(String id, String name, boolean online, int latencyMs, String role, int battery, String link) {
        Map<String, Object> device = new LinkedHashMap<String, Object>();
        device.put("id", id);
        device.put("name", name);
        device.put("online", online);
        device.put("latencyMs", latencyMs);
        device.put("role", role);
        device.put("battery", battery);
        device.put("link", link);
        return device;
    }

    private static Map<String, Object> pair(String name, String value) {
        Map<String, Object> pair = new LinkedHashMap<String, Object>();
        pair.put("name", name);
        pair.put("value", value);
        return pair;
    }

    private static Map<String, Object> step(String id, String title, String description, String status) {
        Map<String, Object> step = new LinkedHashMap<String, Object>();
        step.put("id", id);
        step.put("title", title);
        step.put("description", description);
        step.put("status", status);
        return step;
    }

    private static Map<String, Object> ddsTopic(String name, String ddsTopic, String idlModule, String idlType,
                                                String typeSupport, String direction, String fromDevice, String toDevice,
                                                String qosProfile, String reliability, String durability, String keyFields,
                                                int rateHz, boolean enabled, String lastMessage) {
        Map<String, Object> topic = new LinkedHashMap<String, Object>();
        topic.put("name", name);
        topic.put("ddsTopic", ddsTopic);
        topic.put("type", "IDL");
        topic.put("idlModule", idlModule);
        topic.put("idlType", idlType);
        topic.put("typeSupport", typeSupport);
        topic.put("direction", direction);
        if (fromDevice != null) topic.put("fromDevice", fromDevice);
        if (toDevice != null) topic.put("toDevice", toDevice);
        topic.put("qos", 1);
        topic.put("qosProfile", qosProfile);
        topic.put("reliability", reliability);
        topic.put("durability", durability);
        topic.put("partition", "DomainParticipant/default");
        topic.put("keyFields", Arrays.asList(keyFields.split(",")));
        topic.put("rateHz", rateHz);
        topic.put("enabled", enabled);
        topic.put("lastMessage", lastMessage);
        topic.put("samplePayload", lastMessage);
        return topic;
    }

    private static List<Map<String, Object>> passengerSubscribeTopics() {
        return Arrays.asList(
            ddsTopic("PeerAdvice", "urban/vehicle/peer_advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "sub", "CAR-CQ-002", null, "ReliableKeepLast", "RELIABLE", "VOLATILE", "vehicle_id", 10, true, "peer route=route_C_elevated_detour risk=0.50"),
            ddsTopic("UavAerialPerception", "urban/uav/aerial_perception.dsl", "urban", "EnvironmentPerception", "urban.EnvironmentPerceptionTypeSupport", "sub", "UAV-CQ-007", null, "BestEffortSensorStream", "BEST_EFFORT", "VOLATILE", "uav_id", 15, true, "visibility=1700m route=route_C_elevated_detour"),
            ddsTopic("RsuRoadEvent", "urban/rsu/road_event.dsl", "urban", "RsuRoadEvent", "urban.RsuRoadEventTypeSupport", "sub", "RSU-SCHOOL-009", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "event_id", 5, true, "main_road_accident_and_congestion signal=RED"),
            ddsTopic("CompliantBackboneContext", "backbone/compliant/context.dsl", "urban", "CompliantNetworkContext", "urban.CompliantNetworkContextTypeSupport", "sub", "CN-BACKBONE-CQ", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "region_id", 2, true, "context_only link=5G-A/DDS-Router"),
            ddsTopic("RouteAdvice", "urban/route/advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "sub", "VEHICLE_SWARM", null, "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "mission_id,source_id", 8, true, "route_candidate=route_D_emergency_green_wave confidence=0.86")
        );
    }

    private static List<Map<String, Object>> passengerPublishTopics(String missionId, Map<String, Object> finalDecision) {
        String route = String.valueOf(finalDecision.get("route"));
        String risk = String.valueOf(finalDecision.get("riskScore"));
        return Arrays.asList(
            ddsTopic("PassengerIntent", "cabin/passenger/intent.dsl", "urban", "MissionCommand", "urban.MissionCommandTypeSupport", "pub", null, "CAR-CQ-001", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "intent_id", 5, true, "{\"mission_id\":\"" + missionId + "\",\"intent\":\"PASSENGER_NATURAL_LANGUAGE\"}"),
            ddsTopic("VehicleTelemetry", "urban/vehicle/telemetry.dsl", "urban", "VehicleTelemetry", "urban.VehicleTelemetryTypeSupport", "pub", null, "ALL", "BestEffortSensorStream", "BEST_EFFORT", "VOLATILE", "vehicle_id", 20, true, "egoX=28.0 route=" + route),
            ddsTopic("VehicleRouteAdvice", "urban/route/advice.dsl", "urban", "RouteAdvice", "urban.RouteAdviceTypeSupport", "pub", null, "ALL", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "mission_id,source_id", 8, true, "{\"route\":\"" + route + "\",\"risk\":" + risk + "}"),
            ddsTopic("FinalRouteDecision", "urban/route/final_decision.dsl", "urban", "RouteDecision", "urban.RouteDecisionTypeSupport", "pub", null, "CABIN-CAR-CQ-001", "ReliableKeepLast", "RELIABLE", "TRANSIENT_LOCAL", "decision_id", 4, true, "selected_route=" + route + " risk=" + risk)
        );
    }

    private static Map<String, Object> topic(String name, String type, String direction, String fromDevice, String toDevice, int qos, int rateHz, boolean enabled, String lastMessage) {
        Map<String, Object> topic = new LinkedHashMap<String, Object>();
        topic.put("name", name);
        topic.put("type", type);
        topic.put("direction", direction);
        if (fromDevice != null) topic.put("fromDevice", fromDevice);
        if (toDevice != null) topic.put("toDevice", toDevice);
        topic.put("qos", qos);
        topic.put("rateHz", rateHz);
        topic.put("enabled", enabled);
        topic.put("lastMessage", lastMessage);
        return topic;
    }

    private static Map<String, Object> metric(String label, String value, String trend) {
        Map<String, Object> metric = new LinkedHashMap<String, Object>();
        metric.put("label", label);
        metric.put("value", value);
        metric.put("trend", trend);
        return metric;
    }

    private static PipelineArtifacts runOriginalPipeline(String text, String missionId) {
        PipelineArtifacts artifacts = new PipelineArtifacts();
        try {
            NLToDSLToJavaPipeline pipeline = new NLToDSLToJavaPipeline();
            String outputName = ("web_" + missionId).replaceAll("[^A-Za-z0-9_]", "_");
            NLToDSLToJavaPipeline.Result result = pipeline.execute(text, outputName);
            artifacts.dslCode = result.dslCode == null ? "" : result.dslCode;
            artifacts.dslFilePath = result.dslFilePath;
            artifacts.javaFilePath = result.javaFilePath;
            artifacts.javaCode = result.javaFilePath == null ? "" : Files.readString(Paths.get(result.javaFilePath), StandardCharsets.UTF_8);
            artifacts.javaClassName = extractJavaClassName(artifacts.javaCode, outputName);
            artifacts.compileStatus = result.success ? "ORIGINAL_PIPELINE_READY" : "PIPELINE_FAILED: " + result.error;
        } catch (Exception e) {
            artifacts.dslCode = fallbackDsl(text);
            artifacts.javaCode = fallbackJava(missionId);
            artifacts.javaClassName = "GeneratedDroneMission";
            artifacts.compileStatus = "FALLBACK_READY: " + e.getMessage();
        }
        return artifacts;
    }

    private static String extractJavaClassName(String javaCode, String fallback) {
        if (javaCode != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("public\\s+classource\\s+([A-Za-z_][A-Za-z0-9_]*)").matcher(javaCode);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return fallback;
    }

    private static String fallbackDsl(String text) {
        return "topic fallback_mission {\n"
            + "    task intentContract {\n"
            + "        string source_text = \"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\";\n"
            + "        string intent = \"COORDINATION\";\n"
            + "    }\n"
            + "    state collaborationFlow {\n"
            + "        string publish_topics = \"cabin/passenger/intent.dsl,urban/vehicle/telemetry.dsl,urban/route/advice.dsl,urban/route/final_decision.dsl\";\n"
            + "        string subscribe_topics = \"urban/vehicle/peer_advice.dsl,urban/uav/aerial_perception.dsl,urban/rsu/road_event.dsl,backbone/compliant/context.dsl,urban/route/advice.dsl\";\n"
            + "    }\n"
            + "    state dataExchangeContract {\n"
            + "        string data_1 = \"topic=urban/vehicle/telemetry.dsl; idl=urban.VehicleTelemetry; publisher=CAR-CQ-001|CAR-CQ-002; subscriber=UAV-CQ-007; fields=vehicle_id,position_x_m,position_y_m,speed_mps,heading_deg,road_event,traffic_signal; qos=BEST_EFFORT_sensor_stream\";\n"
            + "        string data_2 = \"topic=urban/uav/aerial_perception.dsl; idl=urban.EnvironmentPerception; publisher=UAV-CQ-007; subscriber=CAR-CQ-001|CAR-CQ-002; fields=event_type,confidence,risk_score,visibility_m,evidence_summary; qos=BEST_EFFORT_sensor_stream\";\n"
            + "        string data_3 = \"topic=urban/rsu/road_event.dsl; idl=urban.RsuRoadEvent; publisher=RSU-SCHOOL-009; subscriber=CAR-CQ-001|CAR-CQ-002; fields=road_event,traffic_signal,pedestrian_count,rsu_load,recommended_route,risk_score; qos=RELIABLE_keep_last_10\";\n"
            + "        string data_4 = \"topic=backbone/compliant/context.dsl; idl=urban.CompliantNetworkContext; publisher=CN-BACKBONE-CQ; subscriber=CAR-CQ-001|CAR-CQ-002; fields=link_mode,link_health,private_5g_signal,expected_latency_ms,recommended_route; qos=RELIABLE_keep_last_10; rule=context_only_no_remote_dispatch\";\n"
            + "    }\n"
            + "    state commandExchangeContract {\n"
            + "        string command_1 = \"topic=cabin/passenger/intent.dsl; idl=urban.MissionCommand; publisher=CABIN-CAR-CQ-001; subscriber=CAR-CQ-001|CAR-CQ-002|UAV-CQ-007|RSU-SCHOOL-009; kind=PASSENGER_INTENT; fields=natural_language,destination,urgency,preference,requested_capabilities\";\n"
            + "        string command_2 = \"topic=urban/vehicle/peer_advice.dsl; idl=urban.RouteAdvice; publisher=CAR-CQ-002; subscriber=CAR-CQ-001; kind=PEER_ADVICE; fields=route_candidate,risk_score,confidence,advice_reason\";\n"
            + "        string command_3 = \"topic=urban/route/advice.dsl; idl=urban.RouteAdvice; publisher=CAR-CQ-001|CAR-CQ-002|UAV-CQ-007; subscriber=CABIN-CAR-CQ-001|CAR-CQ-001|CN-BACKBONE-CQ; kind=ROUTE_ADVICE; fields=route_candidate,risk_score,confidence,required_topics\";\n"
            + "        string command_4 = \"topic=urban/route/final_decision.dsl; idl=urban.RouteDecision; publisher=CAR-CQ-001; subscriber=CABIN-CAR-CQ-001; kind=FINAL_ROUTE_DECISION; fields=decision_owner,autonomy_mode,final_route,final_risk_score,confidence,advice_sources,passenger_explanation\";\n"
            + "    }\n"
            + "    state ddsIdlBinding {\n"
            + "        string idl_file = \"idl/urban_intent_collaboration.idl\";\n"
            + "        string binding = \"DSL topics are semantic names; IDL defines the actual message structure\";\n"
            + "    }\n"
            + "}\n";
    }

    private static String fallbackJava(String missionId) {
        return "public class GeneratedDroneMission {\n"
            + "    public void execute() {\n"
            + "        System.out.println(\"Execute mission " + missionId + "\");\n"
            + "    }\n"
            + "}\n";
    }

    private static Map<String, Object> mapOverlay(String text) {
        double originLng = 106.5328;
        double originLat = 29.5794;
        String destLabel = extractDestinationLabel(text);
        double destLng = 106.5516;
        double destLat = 29.5630;
        if (destLabel.contains("西南大学") || destLabel.contains("医院")) {
            destLng = 106.4204;
            destLat = 29.8255;
            destLabel = "西南大学附属医院";
        } else if (destLabel.contains("解放碑")) {
            destLng = 106.5763;
            destLat = 29.5588;
        } else {
            try {
                AmapRealAPI.AmapGeoResult geo = new AmapRealAPI().geocode(destLabel);
                if (geo != null && geo.isSuccess()) {
                    destLng = geo.getLongitude();
                    destLat = geo.getLatitude();
                    if (geo.getFormattedAddress() != null && !geo.getFormattedAddress().isBlank()) {
                        destLabel = geo.getFormattedAddress();
                    }
                }
            } catch (Exception ignore) {
                // keep fallback coordinate
            }
        }
        double distanceKm = haversineKm(originLat, originLng, destLat, destLng);
        double durationMin = Math.max(5, distanceKm / 25.0 * 60.0);

        Map<String, Object> overlay = new LinkedHashMap<String, Object>();
        overlay.put("centerLabel", "重庆智能交通虚拟 DDS 场景");
        overlay.put("provider", "Amap JS API + VirtualDDSScenarioDemo");
        overlay.put("tileProvider", "https://webapi.amap.com/maps?v=2.0");
        overlay.put("zoom", 13);
        overlay.put("areaName", "观音桥步行街 -> " + destLabel);
        overlay.put("origin", mapPoint("origin", "乘客上车点：观音桥步行街", "origin", 20, 58, "start", originLat, originLng));
        overlay.put("destination", mapPoint("destination", "目的地：" + destLabel, "target", 78, 38, "target", destLat, destLng));
        overlay.put("routeCoordinates", Arrays.asList(
            coordinate(originLat, originLng),
            coordinate(originLat + (destLat - originLat) * 0.30, originLng + (destLng - originLng) * 0.28),
            coordinate(originLat + (destLat - originLat) * 0.58, originLng + (destLng - originLng) * 0.62),
            coordinate(destLat, destLng)
        ));
        overlay.put("distanceKm", distanceKm);
        overlay.put("durationMin", durationMin);
        overlay.put("layers", Arrays.asList("真实地图底图", "虚拟 DDS 设备", "RSU 路侧事件", "无人机前视感知", "车端最终路线"));
        overlay.put("points", Arrays.asList(
            mapPoint("origin", "CABIN-CAR-CQ-001 乘客座舱", "origin", 20, 58, "intent", originLat, originLng),
            mapPoint("CAR-CQ-001", "CAR-CQ-001 自主决策车", "device", 36, 55, "deciding", originLat + 0.006, originLng + 0.012),
            mapPoint("CAR-CQ-002", "CAR-CQ-002 邻近无人车", "device", 28, 68, "advising", originLat + 0.001, originLng + 0.010),
            mapPoint("UAV-CQ-007", "UAV-CQ-007 低空前视", "device", 52, 31, "perceiving", originLat + 0.020, originLng + 0.022),
            mapPoint("RSU-SCHOOL-009", "RSU-SCHOOL-009 路侧单元", "gateway", 61, 49, "road_event", originLat + 0.010, originLng + 0.032),
            mapPoint("CN-BACKBONE-CQ", "CN-BACKBONE-CQ 合规态势链路", "gateway", 42, 80, "context_only", originLat - 0.004, originLng + 0.018),
            mapPoint("destination", destLabel, "target", 78, 38, "tracking", destLat, destLng)
        ));
        overlay.put("routes", Arrays.asList(
            mapRoute("route-A", "route_A_main_road 主路候选", Arrays.asList("origin", "CAR-CQ-001", "RSU-SCHOOL-009", "destination"), "planned"),
            mapRoute("route-C", "route_C_elevated_detour 邻车/无人机建议", Arrays.asList("CAR-CQ-002", "UAV-CQ-007", "destination"), "planned"),
            mapRoute("route-D", "route_D_emergency_green_wave 车端最终决策", Arrays.asList("origin", "CAR-CQ-001", "RSU-SCHOOL-009", "destination"), "active")
        ));
        return overlay;
    }

    private static Map<String, Object> mapPoint(String id, String label, String type, int x, int y, String status) {
        return mapPoint(id, label, type, x, y, status, 0, 0);
    }

    private static Map<String, Object> mapPoint(String id, String label, String type, int x, int y, String status, double lat, double lng) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("id", id);
        point.put("label", label);
        point.put("type", type);
        point.put("x", x);
        point.put("y", y);
        point.put("status", status);
        if (lat != 0 || lng != 0) {
            point.put("lat", lat);
            point.put("lng", lng);
        }
        return point;
    }

    private static Map<String, Object> coordinate(double lat, double lng) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("lat", lat);
        point.put("lng", lng);
        return point;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double radius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String extractDestinationLabel(String text) {
        if (text == null || text.isBlank()) {
            return "目的地";
        }
        if (containsIgnoreCase(text, "zone-b")) return "Zone-B";
        String[] known = {"解放碑", "江北机场", "西南大学", "观音桥", "洪崖洞", "重庆北站", "沙坪坝", "朝天门"};
        for (String item : known) {
            if (text.contains(item)) return item;
        }
        String[] markers = {"前往", "到", "去", "抵达"};
        for (String marker : markers) {
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                String tail = text.substring(idx + marker.length()).trim();
                String[] stops = {"，", "。", ",", ".", "并", "后", "进行", "巡检", "侦察"};
                int end = tail.length();
                for (String stop : stops) {
                    int stopIdx = tail.indexOf(stop);
                    if (stopIdx >= 0) end = Math.min(end, stopIdx);
                }
                String candidate = tail.substring(0, end).trim();
                if (!candidate.isBlank()) return candidate;
            }
        }
        return "目的地";
    }

    private static Map<String, Object> mapRoute(String id, String label, List<String> points, String status) {
        Map<String, Object> route = new LinkedHashMap<String, Object>();
        route.put("id", id);
        route.put("label", label);
        route.put("points", points);
        route.put("status", status);
        return route;
    }

    private static class PipelineArtifacts {
        String dslCode = "";
        String dslFilePath = "";
        String javaFilePath = "";
        String javaCode = "";
        String javaClassName = "GeneratedDroneMission";
        String compileStatus = "UNKNOWN";
    }

    private static Map<String, Object> linkStrategy(String primary, String backup, String policy) {
        Map<String, Object> strategy = new LinkedHashMap<String, Object>();
        strategy.put("primary", primary);
        strategy.put("backup", backup);
        strategy.put("policy", policy);
        return strategy;
    }

    private interface ApiHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
