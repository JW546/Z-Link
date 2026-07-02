package agent;

import dds.VirtualDDSMessageBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured scenario library for autonomous DDS collaboration experiments.
 */
public class ScenarioLibrary {
    private static final ScenarioLibrary INSTANCE = new ScenarioLibrary();
    private final List<Map<String, Object>> scenarios = new ArrayList<Map<String, Object>>();

    private ScenarioLibrary() {
        addScenario("hospital-emergency", "医院急救场景", "乘客急需去医院，系统需要在安全硬约束下压缩到达时间。",
            "尽快去医院，优先安全，避开拥堵",
            state(0.78, 0.58, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_A_main_road", 0.46, 0.88, "无人机观测到主路更快", "route_A_main_road", 0.46, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_D_emergency_green_wave", 0.28, 0.92, "RSU 建议绿波路线并提示主路拥堵", "route_A_main_road", 0.74, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "route_C_elevated_detour", 0.36, 0.78, "邻车报告主路队列变长", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave"),
            conflicts("ROUTE_DIVERGENCE", "INTENT_TENSION", "SAFETY_RISK_CONFLICT"),
            "安全优先，同时尽量保持应急到达效率",
            targets(0.90, 18.0, 0.36, 1, 0.90));

        addScenario("congestion-detour", "拥堵绕行场景", "主干路拥堵，车辆需要融合 UAV 与邻车队列信息选择绕行。",
            "去医院，避开拥堵，尽量快",
            state(0.88, 0.40, "hospital", false),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_C_elevated_detour", 0.25, 0.90, "高架绕行车流稳定", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "route_A_main_road", 0.42, 0.74, "邻车仍建议主路但队列较长", "route_A_main_road", 0.55, 0, 0)
            ),
            routes("route_A_main_road", "route_C_elevated_detour"),
            conflicts("ROUTE_DIVERGENCE"),
            "优先降低拥堵暴露并保持可接受时间",
            targets(0.86, 22.0, 0.32, 1, 0.85));

        addScenario("accident-avoidance", "事故避让场景", "RSU 发布事故风险，系统需要拒绝穿越事故路段的短路线。",
            "去医院，前方事故请绕开，优先安全",
            state(0.62, 0.82, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_B_school_zone_slow_safe", 0.24, 0.94, "A 路线事故占道，建议学校慢行安全路线", "route_A_main_road", 0.88, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_A_main_road", 0.68, 0.86, "空中看到 A 路线距离短但事故未完全清理", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_B_school_zone_slow_safe"),
            conflicts("SAFETY_RISK_CONFLICT"),
            "事故风险高于时间收益",
            targets(0.88, 25.0, 0.25, 1, 0.95));

        addScenario("uav-rsu-route-conflict", "无人机建议与路侧建议冲突场景", "无人机认为 A 更快，路侧节点认为 A 施工并建议 B。",
            "尽快去医院，优先安全，避开拥堵",
            state(0.70, 0.52, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_A_main_road", 0.34, 0.89, "无人机观测到路线 A 更快", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_B_school_zone_slow_safe", 0.23, 0.93, "路线 A 前方施工，建议 B", "route_A_main_road", 0.82, 0, 0)
            ),
            routes("route_A_main_road", "route_B_school_zone_slow_safe"),
            conflicts("ROUTE_DIVERGENCE", "SAFETY_RISK_CONFLICT", "INTENT_TENSION"),
            "安全风险反证覆盖最快路线建议",
            targets(0.90, 24.0, 0.24, 1, 0.95));

        addScenario("neighbor-following-risk-conflict", "邻车风险冲突场景", "邻车报告 B 路线跟驰风险，系统需要在 B/C 中重新排序。",
            "去医院，优先安全",
            state(0.54, 0.48, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_B_school_zone_slow_safe", 0.26, 0.88, "RSU 建议 B 路线", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "route_C_elevated_detour", 0.30, 0.80, "B 路线前车急刹频繁，建议 C", "route_B_school_zone_slow_safe", 0.66, 0, 0)
            ),
            routes("route_B_school_zone_slow_safe", "route_C_elevated_detour"),
            conflicts("ROUTE_DIVERGENCE", "SAFETY_RISK_CONFLICT"),
            "邻车近场风险可触发局部避让",
            targets(0.84, 23.0, 0.34, 1, 0.85));

        addScenario("dds-delay", "DDS 延迟场景", "关键路线建议存在网络延迟，决策需要降低陈旧证据权重。",
            "尽快去医院，优先安全",
            state(0.64, 0.44, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_A_main_road", 0.28, 0.86, "延迟到达的 UAV 快速路线建议", "", 0.0, 1600, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_C_elevated_detour", 0.31, 0.91, "RSU 实时建议高架绕行", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_C_elevated_detour"),
            conflicts("ROUTE_DIVERGENCE", "EVIDENCE_QUALITY_CONFLICT"),
            "延迟证据降权，优先实时可信消息",
            targets(0.82, 24.0, 0.36, 1, 0.80));

        addScenario("dds-packet-loss", "DDS 丢包场景", "邻车消息存在高丢包率，系统需要记录证据质量冲突。",
            "去医院，避开拥堵",
            state(0.76, 0.38, "hospital", false),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "route_A_main_road", 0.30, 0.70, "邻车主路建议但链路丢包严重", "", 0.0, 0, 0.75),
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_C_elevated_detour", 0.28, 0.88, "无人机建议绕行", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_C_elevated_detour"),
            conflicts("ROUTE_DIVERGENCE", "EVIDENCE_QUALITY_CONFLICT"),
            "高丢包来源降权，保持可解释回退",
            targets(0.80, 25.0, 0.35, 1, 0.78));

        addScenario("low-confidence-message", "低置信度消息场景", "某条路线建议置信度低，不能覆盖高可信安全建议。",
            "优先安全去医院",
            state(0.50, 0.34, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, "PEER-CAR-031", "route_A_main_road", 0.22, 0.32, "低置信度主路建议", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_B_school_zone_slow_safe", 0.24, 0.92, "高可信安全路线建议", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_B_school_zone_slow_safe"),
            conflicts("ROUTE_DIVERGENCE", "EVIDENCE_QUALITY_CONFLICT"),
            "低置信度消息不能主导最终路线",
            targets(0.88, 26.0, 0.26, 0, 0.90));

        addScenario("dynamic-replanning", "动态重规划场景", "初始路线可行，但新 DDS 风险事件触发重规划。",
            "尽快去医院，避开拥堵",
            state(0.45, 0.30, "hospital", false),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, "CAR-CQ-001", "route_A_main_road", 0.30, 0.85, "初始主路可行", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_C_elevated_detour", 0.32, 0.94, "新发施工事件触发重规划", "route_A_main_road", 0.76, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST, "CN-BACKBONE-CQ", "route_C_elevated_detour", 0.34, 0.80, "请求重规划", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_C_elevated_detour"),
            conflicts("SAFETY_RISK_CONFLICT"),
            "新风险事件触发动态重规划",
            targets(0.84, 23.0, 0.35, 2, 0.85));

        addScenario("passenger-intent-tension", "乘客意图冲突场景", "乘客同时要求尽快与优先安全，系统需要说明安全优先。",
            "尽快去医院，优先安全",
            state(0.58, 0.46, "hospital", true),
            messages(
                msg(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, "UAV-CQ-007", "route_A_main_road", 0.58, 0.87, "最快路线但风险偏高", "", 0.0, 0, 0),
                msg(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", "route_B_school_zone_slow_safe", 0.22, 0.91, "安全路线更稳", "", 0.0, 0, 0)
            ),
            routes("route_A_main_road", "route_B_school_zone_slow_safe"),
            conflicts("ROUTE_DIVERGENCE", "INTENT_TENSION"),
            "意图冲突时安全约束优先于最快到达",
            targets(0.88, 25.0, 0.24, 1, 0.90));
    }

    public static ScenarioLibrary getInstance() {
        return INSTANCE;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : scenarios) copy.add(deepCopy(scenario));
        return copy;
    }

    public Map<String, Object> get(String id) {
        for (Map<String, Object> scenario : scenarios) {
            if (String.valueOf(scenario.get("scenarioId")).equals(id)) return deepCopy(scenario);
        }
        return scenarios.isEmpty() ? new LinkedHashMap<String, Object>() : deepCopy(scenarios.get(0));
    }

    private void addScenario(String id, String name, String description, String passengerIntent,
                             Map<String, Object> initialWorldState,
                             List<Map<String, Object>> ddsMessages,
                             List<Map<String, Object>> candidateRoutes,
                             List<String> expectedConflicts,
                             String expectedDecisionPrinciple,
                             Map<String, Object> evaluationTargets) {
        Map<String, Object> scenario = new LinkedHashMap<String, Object>();
        scenario.put("scenarioId", id);
        scenario.put("name", name);
        scenario.put("description", description);
        scenario.put("passengerIntent", passengerIntent);
        scenario.put("defaultIntentText", passengerIntent);
        scenario.put("initialWorldState", initialWorldState);
        scenario.put("ddsMessages", ddsMessages);
        scenario.put("candidateRoutes", candidateRoutes);
        scenario.put("expectedConflicts", expectedConflicts);
        scenario.put("expectedDecisionPrinciple", expectedDecisionPrinciple);
        scenario.put("evaluationTargets", evaluationTargets);
        scenario.put("taskType", "无人设备意图协同");
        scenario.put("recommendedDevices", Arrays.asList("无人车", "无人机", "RSU", "邻车", "通信适配节点"));
        scenario.put("requiredTopics", Arrays.asList(
            VirtualDDSMessageBus.TOPIC_INTENT_DSL,
            VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION,
            VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION,
            VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT,
            VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS,
            VirtualDDSMessageBus.TOPIC_FINAL_DECISION,
            VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST));
        scenario.put("qosLevel", "SCENARIO_DEFINED");
        scenario.put("riskFactors", expectedConflicts);
        scenario.put("expectedOutput", Arrays.asList("ddsTimeline", "finalDecision", "evaluationMetrics"));
        scenarios.add(scenario);
    }

    private Map<String, Object> state(double congestion, double risk, String destination, boolean safetyFirst) {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("congestionLevel", congestion);
        state.put("roadRisk", risk);
        state.put("destination", destination);
        state.put("safetyFirst", safetyFirst);
        return state;
    }

    private List<Map<String, Object>> messages(Map<String, Object>... messages) {
        return Arrays.asList(messages);
    }

    private Map<String, Object> msg(String topic, String sourceId, String route, double risk, double confidence,
                                    String explanation, String affectedRoute, double affectedRisk,
                                    long delayMs, double packetLossRate) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("topic", topic);
        message.put("sourceId", sourceId);
        message.put("route", route);
        message.put("riskScore", risk);
        message.put("confidence", confidence);
        message.put("explanation", explanation);
        message.put("affectedRoute", affectedRoute);
        message.put("affectedRiskScore", affectedRisk);
        message.put("ddsDelayMs", delayMs);
        message.put("packetLossRate", packetLossRate);
        message.put("timeAdvantage", route.contains("A") ? 0.92 : route.contains("D") ? 0.82 : route.contains("C") ? 0.58 : 0.42);
        return message;
    }

    private List<Map<String, Object>> routes(String... routeIds) {
        List<Map<String, Object>> routes = new ArrayList<Map<String, Object>>();
        for (String routeId : routeIds) {
            Map<String, Object> route = new LinkedHashMap<String, Object>();
            route.put("routeId", routeId);
            route.put("enabled", true);
            routes.add(route);
        }
        return routes;
    }

    private List<String> conflicts(String... conflicts) {
        return Arrays.asList(conflicts);
    }

    private Map<String, Object> targets(double successRate, double arrivalMinutes, double riskExposure,
                                        int replanCount, double conflictAccuracy) {
        Map<String, Object> targets = new LinkedHashMap<String, Object>();
        targets.put("successRate", successRate);
        targets.put("arrivalTimeMinutes", arrivalMinutes);
        targets.put("riskExposure", riskExposure);
        targets.put("replanCount", replanCount);
        targets.put("conflictResolutionAccuracy", conflictAccuracy);
        return targets;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) value));
            } else if (value instanceof List<?>) {
                copy.put(entry.getKey(), new ArrayList<Object>((List<?>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
