package agent;

import decision.ConflictResolver;
import planner.CandidateRoute;
import planner.RouteFeatureExtractor;
import planner.RouteScoringContext;
import planner.RouteSelectionResult;
import policy.AgentState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Passenger-intent collaboration engine used by the frontend demo.
 * It keeps route IDs as scenario data, but decisions are made from route
 * features, DDS evidence quality, and conflict resolution scores.
 */
public class CollaborationDecisionEngine {
    private final RouteFeatureExtractor routeSelector = new RouteFeatureExtractor();
    private final ConflictResolver conflictResolver = new ConflictResolver();
    private final RouteDecisionAssistant routeDecisionAssistant;

    public CollaborationDecisionEngine() {
        this(new QwenRouteDecisionAssistant());
    }

    public CollaborationDecisionEngine(RouteDecisionAssistant routeDecisionAssistant) {
        this.routeDecisionAssistant = routeDecisionAssistant;
    }

    public Map<String, Object> evaluate(String passengerUtterance, String missionId) {
        String text = passengerUtterance == null ? "" : passengerUtterance;
        String destination = inferDestination(text);
        boolean urgent = containsAny(text, "尽快", "最快", "紧急", "urgent", "emergency");
        boolean safetyFirst = containsAny(text, "安全", "避开", "稳定", "safe", "safety");
        boolean congestion = containsAny(text, "拥堵", "堵", "事故", "congestion", "accident");

        AgentState baseState = AgentState.builder("CAR-CQ-001")
            .passengerIntent(text)
            .passengerUrgent(urgent)
            .passengerSafetyFirst(safetyFirst)
            .congestionLevel(congestion ? 0.82 : 0.32)
            .safetyRisk(congestion ? 0.58 : 0.28)
            .messageConfidence(0.86)
            .messageFreshness(0.96)
            .sourceTrust(0.86)
            .conflictStatus(congestion)
            .replanRequired(congestion)
            .candidateRoutes(Arrays.asList("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave"))
            .build();

        List<CandidateRoute> candidates = candidateRoutes(congestion, urgent, safetyFirst);
        List<Map<String, Object>> deviceAdvice = buildDeviceAdvice(baseState, candidates);
        List<AgentAdvice> adviceObjects = toAdvice(deviceAdvice);
        Map<String, Object> resolution = conflictResolver.resolve(adviceObjects, urgent, safetyFirst);
        RouteSelectionResult egoSelection = routeSelector.select(candidates, RouteScoringContext.fromState(baseState, AgentRole.EGO_VEHICLE));
        Map<String, Object> finalDecision = finalDecision(text, destination, resolution, egoSelection, deviceAdvice);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("autonomyMode", "vehicle_self_decision");
        result.put("passengerIntent", text);
        result.put("destination", destination);
        result.put("decisionOwner", "CAR-CQ-001");
        result.put("publishTopics", publishTopics());
        result.put("subscribeTopics", subscribeTopics());
        result.put("deviceAdvice", deviceAdvice);
        result.put("finalDecision", finalDecision);
        result.put("routePlan", routePlan(egoSelection));
        result.put("taskSchedule", taskSchedule(urgent, congestion));
        result.put("algorithmSummary", algorithmSummary());
        result.put("experimentMetrics", experimentMetrics(deviceAdvice, finalDecision));
        result.put("idlMappings", idlMappings());
        result.put("timeline", timeline(missionId, finalDecision));
        return result;
    }

    private List<CandidateRoute> candidateRoutes(boolean congestion, boolean urgent, boolean safetyFirst) {
        double mainRisk = congestion ? 0.74 : 0.30;
        double mainCongestion = congestion ? 0.86 : 0.28;
        return Arrays.asList(
            CandidateRoute.builder("route_A_main_road")
                .travelTime(congestion ? 18.0 : 10.0)
                .distance(5.2)
                .safetyRisk(mainRisk)
                .congestionLevel(mainCongestion)
                .routeAvailability(congestion ? 0.58 : 0.96)
                .passengerPreference(urgent && !safetyFirst ? 0.72 : 0.42)
                .messageConfidence(0.82)
                .sourceTrust(0.82)
                .messageFreshness(0.92)
                .conflictPenalty(congestion ? 0.28 : 0.0)
                .build(),
            CandidateRoute.builder("route_C_elevated_detour")
                .travelTime(14.8)
                .distance(7.4)
                .safetyRisk(0.34)
                .congestionLevel(0.30)
                .routeAvailability(0.90)
                .passengerPreference(safetyFirst ? 0.78 : 0.62)
                .messageConfidence(0.86)
                .sourceTrust(0.86)
                .messageFreshness(0.94)
                .build(),
            CandidateRoute.builder("route_D_emergency_green_wave")
                .travelTime(11.6)
                .distance(6.1)
                .safetyRisk(0.28)
                .congestionLevel(0.24)
                .routeAvailability(urgent ? 0.92 : 0.70)
                .emergencyPriority(urgent ? 0.88 : 0.20)
                .passengerPreference(urgent || safetyFirst ? 0.90 : 0.55)
                .messageConfidence(0.90)
                .sourceTrust(0.92)
                .messageFreshness(0.96)
                .build()
        );
    }

    private List<Map<String, Object>> buildDeviceAdvice(AgentState state, List<CandidateRoute> candidates) {
        List<Map<String, Object>> advice = new ArrayList<Map<String, Object>>();
        advice.add(advice("CAR-CQ-002", AgentRole.PEER_VEHICLE, "邻近无人车", "urban/vehicle/peer_advice.dsl",
            "urban.VehicleTelemetry", select(candidates, state, AgentRole.PEER_VEHICLE, 0.78), 24,
            "邻车根据跟驰风险、拥堵和消息置信度发布建议。"));
        advice.add(advice("UAV-CQ-007", AgentRole.UAV, "低空无人机", "urban/uav/aerial_perception.dsl",
            "urban.EnvironmentPerception", select(candidates, state, AgentRole.UAV, 0.88), 32,
            "无人机根据前视感知、新鲜度和安全风险发布建议。"));
        advice.add(advice("RSU-SCHOOL-009", AgentRole.RSU, "路侧单元 RSU", "urban/rsu/road_event.dsl",
            "urban.RsuRoadEvent", select(candidates, state, AgentRole.RSU, 0.94), 18,
            "RSU 根据道路事件、信号状态和高可信风险证据发布建议。"));
        advice.add(advice("CN-BACKBONE-CQ", AgentRole.COMPLIANT_BACKBONE, "合规广域态势链路", "backbone/compliant/context.dsl",
            "urban.CompliantNetworkContext", select(candidates, state, AgentRole.COMPLIANT_BACKBONE, 0.78), 41,
            "通信适配节点提供广域态势和链路健康，不执行远程派单。"));
        return advice;
    }

    private CandidateRoute select(List<CandidateRoute> candidates, AgentState state, AgentRole role, double sourceTrust) {
        AgentState adjusted = AgentState.builder(state.agentId)
            .passengerIntent(state.passengerIntent)
            .passengerUrgent(state.passengerUrgent)
            .passengerSafetyFirst(state.passengerSafetyFirst)
            .congestionLevel(state.congestionLevel)
            .safetyRisk(state.safetyRisk)
            .messageConfidence(state.messageConfidence)
            .messageFreshness(state.messageFreshness)
            .sourceTrust(sourceTrust)
            .conflictStatus(state.conflictStatus)
            .replanRequired(state.replanRequired)
            .candidateRoutes(state.candidateRoutes)
            .build();
        return routeSelector.select(candidates, RouteScoringContext.fromState(adjusted, role)).selectedRoute;
    }

    private Map<String, Object> advice(String nodeId, AgentRole role, String label, String topic, String idlType,
                                       CandidateRoute route, int latencyMs, String text) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("nodeId", nodeId);
        item.put("role", label);
        item.put("topic", topic);
        item.put("route", route.routeId);
        item.put("advice", text);
        item.put("riskScore", round(route.safetyRisk));
        item.put("confidence", round(route.messageConfidence));
        item.put("sourceTrust", round(route.sourceTrust));
        item.put("freshness", round(route.messageFreshness));
        item.put("travelTime", round(route.travelTime));
        item.put("congestionLevel", round(route.congestionLevel));
        item.put("latencyMs", latencyMs);
        item.put("idlType", idlType);
        item.put("agentRole", role.name());
        return item;
    }

    private List<AgentAdvice> toAdvice(List<Map<String, Object>> items) {
        List<AgentAdvice> advice = new ArrayList<AgentAdvice>();
        for (Map<String, Object> item : items) {
            advice.add(new AgentAdvice(
                String.valueOf(item.get("nodeId")),
                AgentRole.valueOf(String.valueOf(item.get("agentRole"))),
                String.valueOf(item.get("topic")),
                String.valueOf(item.get("route")),
                number(item, "riskScore", 0.40),
                number(item, "confidence", 0.70),
                String.valueOf(item.get("advice"))));
        }
        return advice;
    }

    private Map<String, Object> finalDecision(String passengerIntent, String destination, Map<String, Object> resolution,
                                              RouteSelectionResult egoSelection,
                                              List<Map<String, Object>> deviceAdvice) {
        String resolvedRoute = String.valueOf(resolution.getOrDefault("finalRoute", ""));
        String route = resolvedRoute.isBlank() ? egoSelection.routeId() : resolvedRoute;
        Map<String, Object> decision = new LinkedHashMap<String, Object>();
        decision.put("route", route);
        decision.put("destination", destination);
        decision.put("reason", resolution.get("decisionReason"));
        decision.put("riskScore", resolution.get("riskScore"));
        decision.put("confidence", resolution.get("confidence"));
        decision.put("consensusPolicy", resolution.get("policy"));
        decision.put("routePath", Arrays.asList("START", "FEATURE_SELECTED_CORRIDOR", "DESTINATION"));
        decision.put("routeCost", egoSelection.routeScores.isEmpty() ? 0.0 : egoSelection.routeScores.get(0).score);
        decision.put("routeAlgorithm", "feature_score_route_selection + conflict_resolver");
        decision.put("nextAction", "publish urban/route/final_decision.dsl and continue DDS advice subscription");
        decision.put("routeSelection", egoSelection.toMap());
        decision.put("conflictResolution", resolution);
        applyRouteDecisionSupport(decision, passengerIntent, destination, route, deviceAdvice, resolution, egoSelection);
        return decision;
    }

    private void applyRouteDecisionSupport(Map<String, Object> decision, String passengerIntent, String destination,
                                           String route, List<Map<String, Object>> deviceAdvice,
                                           Map<String, Object> resolution, RouteSelectionResult egoSelection) {
        String ruleReason = String.valueOf(decision.getOrDefault("reason", ""));
        decision.put("decisionAuthority", "vehicle_agent_rule_score_safety_constraint");
        decision.put("llmRole", "explanation_intent_completion_auxiliary_analysis_only");
        decision.put("ruleBasedReason", ruleReason);

        RouteDecisionAssistant.RouteDecisionSupport support = routeDecisionAssistant == null
            ? RouteDecisionAssistant.RouteDecisionSupport.unavailable("No route decision assistant configured.")
            : routeDecisionAssistant.analyze(new RouteDecisionAssistant.RouteDecisionRequest(
                passengerIntent,
                destination,
                route,
                ruleReason,
                deviceAdvice,
                resolution,
                egoSelection == null ? null : egoSelection.toMap()
            ));

        decision.put("llmAssisted", support.available);
        decision.put("llmProvider", support.provider);
        decision.put("llmModel", support.model);
        decision.put("llmConfidence", round(support.confidence));
        decision.put("llmLatencyMs", support.latencyMs);
        decision.put("llmAnalysis", support.summary);
        decision.put("passengerExplanation", support.available ? support.passengerExplanation : ruleReason);
    }

    private Map<String, Object> routePlan(RouteSelectionResult selection) {
        Map<String, Object> plan = new LinkedHashMap<String, Object>();
        plan.put("routeId", selection.routeId());
        plan.put("algorithm", "RouteFeatureExtractor");
        plan.put("candidateRoutes", selection.toMap().get("routeScores"));
        plan.put("reason", selection.explanation);
        plan.put("estimatedMinutes", selection.selectedRoute == null ? 0.0 : selection.selectedRoute.travelTime);
        plan.put("riskScore", selection.selectedRoute == null ? 0.0 : selection.selectedRoute.safetyRisk);
        return plan;
    }

    private Map<String, Object> taskSchedule(boolean urgent, boolean congestion) {
        Map<String, Object> schedule = new LinkedHashMap<String, Object>();
        schedule.put("policy", "dynamic_weighted_task_scheduling");
        schedule.put("urgent", urgent);
        schedule.put("congestionObserved", congestion);
        schedule.put("assignments", Arrays.asList("vehicle-route-evaluation", "uav-forward-perception", "rsu-road-event", "backbone-context-sync"));
        return schedule;
    }

    private List<String> publishTopics() {
        return Arrays.asList("cabin/passenger/intent.dsl", "urban/vehicle/telemetry.dsl",
            "urban/route/advice.dsl", "urban/route/final_decision.dsl");
    }

    private List<String> subscribeTopics() {
        return Arrays.asList("urban/vehicle/peer_advice.dsl", "urban/uav/aerial_perception.dsl",
            "urban/rsu/road_event.dsl", "backbone/compliant/context.dsl", "urban/route/advice.dsl");
    }

    private List<Map<String, Object>> algorithmSummary() {
        return Arrays.asList(
            metric("routeSelection", "feature_score", "route IDs are data labels, not decision rules"),
            metric("conflictResolution", "weighted_evidence", "risk, confidence, trust, freshness, delay and packet loss"),
            metric("baseline", "Dijkstra/Greedy", "baseline only")
        );
    }

    private List<Map<String, Object>> experimentMetrics(List<Map<String, Object>> advice, Map<String, Object> finalDecision) {
        double maxLatency = 0.0;
        for (Map<String, Object> item : advice) maxLatency = Math.max(maxLatency, number(item, "latencyMs", 0.0));
        return Arrays.asList(
            metric("协同闭环延迟", String.format(Locale.ROOT, "%.0fms", maxLatency + 38), "可用于演示"),
            metric("建议融合置信度", String.format(Locale.ROOT, "%.0f%%", number(finalDecision, "confidence", 0.0) * 100), "论文指标"),
            metric("车端自主决策覆盖率", "100%", "无控制中心派单"),
            metric("虚拟 DDS Topic 覆盖", "8/8", "与 DDS topic 映射一致")
        );
    }

    private List<Map<String, Object>> idlMappings() {
        return Arrays.asList(
            idl("cabin/passenger/intent.dsl", "urban", "MissionCommand", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/vehicle/peer_advice.dsl", "urban", "RouteAdvice", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/uav/aerial_perception.dsl", "urban", "EnvironmentPerception", "urban_intent_collaboration.idl", "BEST_EFFORT sensor stream"),
            idl("urban/rsu/road_event.dsl", "urban", "RsuRoadEvent", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/route/final_decision.dsl", "urban", "RouteDecision", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST")
        );
    }

    private List<Map<String, Object>> timeline(String missionId, Map<String, Object> decision) {
        return Arrays.asList(
            event("T0", "乘客意图", "发布 cabin/passenger/intent.dsl"),
            event("T1", "DDS 订阅", "车辆订阅邻车、无人机、RSU 和广域态势 Topic"),
            event("T2", "多 Agent 建议", "各设备基于路线特征和 DDS 证据质量发布建议"),
            event("T3", "冲突消解", "ConflictResolver 输出最终路线 " + decision.get("route")),
            event("T4", "最终决策", "任务 " + missionId + " 发布 urban/route/final_decision.dsl")
        );
    }

    private Map<String, Object> idl(String topic, String module, String type, String file, String qos) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("topic", topic);
        item.put("idlModule", module);
        item.put("idlType", type);
        item.put("idlFile", file);
        item.put("qos", qos);
        return item;
    }

    private Map<String, Object> event(String t, String title, String detail) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("time", t);
        item.put("title", title);
        item.put("detail", detail);
        return item;
    }

    private Map<String, Object> metric(String label, Object value, String trend) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("label", label);
        item.put("value", value);
        item.put("trend", trend);
        return item;
    }

    private String inferDestination(String text) {
        if (containsAny(text, "医院", "hospital")) return "西南大学附属医院";
        if (containsAny(text, "机场", "airport")) return "机场";
        return "乘客指定目的地";
    }

    private boolean containsAny(String text, String... terms) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (lower.contains(term.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
