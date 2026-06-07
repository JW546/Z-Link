package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic passenger-intent collaboration engine used by the frontend.
 *
 * The data model mirrors integration.VirtualDDSScenarioDemo: the passenger
 * publishes intent, devices publish their own observations/advice, and the ego
 * vehicle makes the final route decision locally.
 */
public class CollaborationDecisionEngine {
    private final AdaptiveRoutePlanner routePlanner = new AdaptiveRoutePlanner();
    private final DynamicTaskScheduler taskScheduler = new DynamicTaskScheduler();

    public Map<String, Object> evaluate(String passengerUtterance, String missionId) {
        String destination = inferDestination(passengerUtterance);
        boolean urgent = containsAny(passengerUtterance, "尽快", "最快", "赶时间", "急", "urgently", "urgent");
        boolean stable = containsAny(passengerUtterance, "安全", "稳定", "更稳", "保守", "stable", "safety");
        boolean congestion = containsAny(passengerUtterance, "拥堵", "堵", "前方", "红灯", "事故", "crowded", "congested");
        AdaptiveRoutePlanner.RoutePlan routePlan = planRoute(destination, urgent, stable, congestion);
        DynamicTaskScheduler.ScheduleResult schedule = scheduleCollaborationTasks(urgent, congestion);

        List<Map<String, Object>> deviceAdvice = new ArrayList<Map<String, Object>>();
        deviceAdvice.add(advice(
            "CAR-CQ-002",
            "邻近无人车",
            "urban/vehicle/peer_advice.dsl",
            congestion ? "route_C_elevated_detour" : "route_A_main_road",
            congestion
                ? "邻车检测到主路减速和车队排队，建议切换高架绕行并保持车间态势共享。"
                : "邻车跟驰状态稳定，建议继续主路并保持低延迟车车协同。",
            congestion ? 0.50 : 0.28,
            congestion ? 0.84 : 0.78,
            24,
            "urban.VehicleTelemetry"
        ));
        deviceAdvice.add(advice(
            "UAV-CQ-007",
            "低空无人机",
            "urban/uav/aerial_perception.dsl",
            congestion ? "route_C_elevated_detour" : "route_A_main_road",
            congestion
                ? "无人机前视发现主路事故拥堵，建议绕行并持续发布前方走廊感知。"
                : "无人机确认主路走廊可通行，建议保持主路并低频更新空中态势。",
            congestion ? 0.28 : 0.22,
            0.88,
            32,
            "urban.EnvironmentPerception"
        ));
        deviceAdvice.add(advice(
            "RSU-SCHOOL-009",
            "路侧单元 RSU",
            "urban/rsu/road_event.dsl",
            congestion ? "route_D_emergency_green_wave" : "route_A_main_road",
            congestion
                ? "RSU 发布主路事故、红灯和排队事件，并建议车辆申请应急绿波路线。"
                : "RSU 发布路口正常通行事件，建议继续主路并监测学校区域行人。",
            congestion ? 0.36 : 0.20,
            congestion ? 0.86 : 0.80,
            18,
            "urban.RsuRoadEvent"
        ));
        deviceAdvice.add(advice(
            "CN-BACKBONE-CQ",
            "合规广域态势链路",
            "backbone/compliant/context.dsl",
            congestion ? "route_D_emergency_green_wave" : "route_A_main_road",
            "融合区域交通流、RSU 状态和链路健康，只提供态势，不进行远程派单或控制。",
            congestion ? 0.52 : 0.31,
            0.76,
            41,
            "urban.CompliantNetworkContext"
        ));

        Map<String, Object> finalDecision = chooseFinalDecision(deviceAdvice, urgent, stable, congestion, destination, routePlan);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("autonomyMode", "vehicle_self_decision");
        result.put("passengerIntent", passengerUtterance);
        result.put("destination", destination);
        result.put("decisionOwner", "CAR-CQ-001");
        result.put("publishTopics", Arrays.asList(
            "cabin/passenger/intent.dsl",
            "urban/vehicle/telemetry.dsl",
            "urban/route/advice.dsl",
            "urban/route/final_decision.dsl"
        ));
        result.put("subscribeTopics", Arrays.asList(
            "urban/vehicle/peer_advice.dsl",
            "urban/uav/aerial_perception.dsl",
            "urban/rsu/road_event.dsl",
            "backbone/compliant/context.dsl",
            "urban/route/advice.dsl"
        ));
        result.put("deviceAdvice", deviceAdvice);
        result.put("finalDecision", finalDecision);
        result.put("routePlan", routePlan.toMap());
        result.put("taskSchedule", schedule.toMap());
        result.put("algorithmSummary", algorithmSummary());
        result.put("experimentMetrics", experimentMetrics(deviceAdvice, finalDecision));
        result.put("idlMappings", idlMappings());
        result.put("timeline", timeline(missionId, finalDecision));
        return result;
    }

    private Map<String, Object> chooseFinalDecision(List<Map<String, Object>> advice, boolean urgent, boolean stable,
                                                    boolean congestion, String destination,
                                                    AdaptiveRoutePlanner.RoutePlan routePlan) {
        String route = routePlan != null && routePlan.routeId != null ? routePlan.routeId : "route_A_main_road";

        double risk = 0.0;
        double confidence = 0.0;
        double totalWeight = 0.0;
        for (Map<String, Object> item : advice) {
            double itemRisk = ((Number) item.get("riskScore")).doubleValue();
            double itemConfidence = ((Number) item.get("confidence")).doubleValue();
            double latencyPenalty = Math.min(0.18, ((Number) item.get("latencyMs")).doubleValue() / 500.0);
            double routeBonus = route.equals(item.get("route")) ? 0.16 : 0.0;
            double weight = Math.max(0.2, itemConfidence + routeBonus - latencyPenalty);
            risk += itemRisk * weight;
            confidence += itemConfidence * weight;
            totalWeight += weight;
        }
        risk = totalWeight == 0 ? 0.5 : risk / totalWeight;
        confidence = totalWeight == 0 ? 0.75 : confidence / totalWeight;
        if (routePlan != null) {
            risk = risk * 0.65 + routePlan.riskScore * 0.35;
            confidence = Math.min(0.98, confidence + 0.04);
        }

        Map<String, Object> decision = new LinkedHashMap<String, Object>();
        decision.put("route", route);
        decision.put("destination", destination);
        decision.put("reason", buildReason(route, urgent, stable, congestion));
        decision.put("riskScore", round(risk));
        decision.put("confidence", round(confidence));
        decision.put("consensusPolicy", stable ? "safety_weighted_consensus + A*_route_cost" : "time_risk_balanced_consensus + A*_route_cost");
        decision.put("routePath", routePlan != null ? routePlan.path : Arrays.asList("START", "HOSPITAL"));
        decision.put("routeCost", routePlan != null ? round(routePlan.cost) : 0.0);
        decision.put("routeAlgorithm", routePlan != null ? routePlan.algorithm : "fallback_route_policy");
        decision.put("nextAction", "publish urban/route/final_decision.dsl and continue DDS advice subscription");
        return decision;
    }

    private AdaptiveRoutePlanner.RoutePlan planRoute(String destination, boolean urgent, boolean stable, boolean congestion) {
        AdaptiveRoutePlanner.RouteContext context = new AdaptiveRoutePlanner.RouteContext();
        context.destination = destination;
        context.urgent = urgent;
        context.safetyFirst = stable;
        context.congestionLevel = congestion ? 0.82 : 0.32;
        context.roadFriction = stable ? 0.78 : 0.83;
        context.ambulanceLaneClearance = urgent && congestion ? 0.88 : 0.62;
        try {
            return routePlanner.plan(context);
        } catch (RuntimeException ex) {
            return fallbackRoutePlan("路径规划异常，已回退主路候选: " + ex.getMessage());
        }
    }

    private DynamicTaskScheduler.ScheduleResult scheduleCollaborationTasks(boolean urgent, boolean congestion) {
        List<DynamicTaskScheduler.TaskRequest> tasks = Arrays.asList(
            DynamicTaskScheduler.task("forward-perception", "无人机前视感知", congestion ? 9 : 7, 80, 35, "aerial_perception", "route_advice"),
            DynamicTaskScheduler.task("road-event-query", "路侧事件查询", urgent ? 10 : 8, 60, 0, "rsu_event", "traffic_signal"),
            DynamicTaskScheduler.task("peer-route-advice", "邻车路线建议", 8, 70, 30, "vehicle_advice", "low_latency"),
            DynamicTaskScheduler.task("wide-area-context", "合规广域态势同步", 6, 120, 0, "global_context", "context_only")
        );
        List<DynamicTaskScheduler.DeviceState> devices = Arrays.asList(
            DynamicTaskScheduler.device("CAR-CQ-002", "邻近无人车", true, 24, 79, 0.38, 0.88, false, "vehicle_advice", "low_latency", "route_advice"),
            DynamicTaskScheduler.device("UAV-CQ-007", "低空无人机", true, 32, 76, 0.44, 0.82, false, "aerial_perception", "route_advice"),
            DynamicTaskScheduler.device("RSU-SCHOOL-009", "路侧单元", true, 18, 100, 0.30, 0.92, true, "rsu_event", "traffic_signal"),
            DynamicTaskScheduler.device("CN-BACKBONE-CQ", "合规广域态势链路", true, 41, 100, 0.26, 0.84, true, "global_context", "context_only")
        );
        try {
            return taskScheduler.schedule(tasks, devices);
        } catch (RuntimeException ex) {
            DynamicTaskScheduler.ScheduleResult fallback = new DynamicTaskScheduler.ScheduleResult();
            fallback.note = "任务调度异常，已回退为空调度方案: " + ex.getMessage();
            return fallback;
        }
    }

    private AdaptiveRoutePlanner.RoutePlan fallbackRoutePlan(String reason) {
        AdaptiveRoutePlanner.RoutePlan plan = new AdaptiveRoutePlanner.RoutePlan();
        plan.routeId = "route_A_main_road";
        plan.algorithm = "fallback_route_policy";
        plan.path = Arrays.asList("START", "HOSPITAL");
        plan.estimatedMinutes = 30.0;
        plan.riskScore = 0.30;
        plan.cost = 0.0;
        plan.reason = reason;
        return plan;
    }

    private List<Map<String, Object>> algorithmSummary() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        items.add(metric("已有缓存机制", "LRU", "NLUCacheManager 使用 LinkedHashMap access-order，查询/写入均摊 O(1)"));
        items.add(metric("已有并发通信", "ScheduledExecutor + ConcurrentHashMap", "VirtualDDSScenarioDemo 支持并发发布、订阅、QoS 重试"));
        items.add(metric("新增路径规划", "A*/Dijkstra", "按动态边权选择主路、高架、学校慢行或应急绿波"));
        items.add(metric("新增任务调度", "动态权重贪心任务调度", "按能力、优先级、延迟、电量、负载和链路健康分配协同任务"));
        return items;
    }

    private List<Map<String, Object>> experimentMetrics(List<Map<String, Object>> advice, Map<String, Object> finalDecision) {
        double maxLatency = 0;
        for (Map<String, Object> item : advice) {
            maxLatency = Math.max(maxLatency, ((Number) item.get("latencyMs")).doubleValue());
        }
        return Arrays.asList(
            metric("协同闭环延迟", String.format(Locale.ROOT, "%.0fms", maxLatency + 38), "可用于比赛演示"),
            metric("建议融合置信度", String.format(Locale.ROOT, "%.0f%%", ((Number) finalDecision.get("confidence")).doubleValue() * 100), "论文指标"),
            metric("车端自主决策覆盖率", "100%", "无控制中心派单"),
            metric("虚拟 DDS Topic 覆盖", "8/8", "与 VirtualDDSScenarioDemo 一致")
        );
    }

    private List<Map<String, Object>> idlMappings() {
        return Arrays.asList(
            idl("cabin/passenger/intent.dsl", "urban", "MissionCommand", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/vehicle/telemetry.dsl", "urban", "VehicleTelemetry", "urban_intent_collaboration.idl", "BEST_EFFORT sensor stream"),
            idl("urban/vehicle/peer_advice.dsl", "urban", "RouteAdvice", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/uav/aerial_perception.dsl", "urban", "EnvironmentPerception", "urban_intent_collaboration.idl", "BEST_EFFORT sensor stream"),
            idl("urban/rsu/road_event.dsl", "urban", "RsuRoadEvent", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("backbone/compliant/context.dsl", "urban", "CompliantNetworkContext", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/route/advice.dsl", "urban", "RouteAdvice", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST"),
            idl("urban/route/final_decision.dsl", "urban", "RouteDecision", "urban_intent_collaboration.idl", "RELIABLE + KEEP_LAST")
        );
    }

    private List<Map<String, Object>> timeline(String missionId, Map<String, Object> decision) {
        return Arrays.asList(
            event("T0", "乘客意图", "座舱将自然语言转换为 DSL，并发布 cabin/passenger/intent.dsl"),
            event("T1", "车辆自主订阅", "CAR-CQ-001 自主订阅邻车、无人机、RSU 和合规广域态势 Topic"),
            event("T2", "设备发布建议", "邻车、无人机、RSU 和合规链路分别发布感知与路线建议"),
            event("T3", "多源建议融合", "CAR-CQ-001 采用 " + decision.get("consensusPolicy") + " 选择 " + decision.get("route")),
            event("T4", "最终决策发布", "任务 " + missionId + " 发布 urban/route/final_decision.dsl")
        );
    }

    private Map<String, Object> advice(String nodeId, String role, String topic, String route, String text,
                                       double risk, double confidence, int latencyMs, String idlType) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("nodeId", nodeId);
        item.put("role", role);
        item.put("topic", topic);
        item.put("route", route);
        item.put("advice", text);
        item.put("riskScore", round(risk));
        item.put("confidence", round(confidence));
        item.put("latencyMs", latencyMs);
        item.put("idlType", idlType);
        return item;
    }

    private Map<String, Object> metric(String label, String value, String trend) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("label", label);
        item.put("value", value);
        item.put("trend", trend);
        return item;
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

    private String buildReason(String route, boolean urgent, boolean stable, boolean congestion) {
        if ("route_D_emergency_green_wave".equals(route)) {
            return "乘客强调尽快到达且主路发生事故拥堵，车辆融合无人机、邻车、RSU 和合规态势后选择应急绿波路线。";
        }
        if ("route_C_elevated_detour".equals(route)) {
            return stable
                ? "乘客强调安全稳定，车辆提高邻车和 RSU 建议权重，选择高架绕行。"
                : "检测到拥堵风险，车辆选择高架绕行以降低主路排队影响。";
        }
        if ("route_B_school_zone_slow_safe".equals(route)) {
            return "A* 路网搜索判断学校慢行路线在当前拥堵与风险权重下总代价更低，车辆选择低速安全通过。";
        }
        if (urgent) {
            return "乘客强调尽快到达，当前风险可控，车辆保持主路优先。";
        }
        return "车辆在风险、置信度、链路延迟之间取得平衡后保持主路。";
    }

    private String inferDestination(String text) {
        if (text == null || text.isBlank()) {
            return "西南大学附属医院";
        }
        String[] hints = {"西南大学附属医院", "医院", "机场", "解放碑", "西南大学", "观音桥", "火车站"};
        for (String hint : hints) {
            if (text.contains(hint)) {
                return hint.equals("医院") ? "西南大学附属医院" : hint;
            }
        }
        return "乘客指定目的地";
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (text.contains(term) || lower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
