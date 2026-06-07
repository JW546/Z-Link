package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 城市协同路线规划器。
 *
 * 应用场景：车辆收到乘客意图后，需要在主路、高架绕行、学校慢行和应急绿波等候选路线中，
 * 根据拥堵、风险、路面附着、应急车道净空和乘客偏好快速选择路线。
 *
 * 算法原理：
 * 1. 将候选道路抽象为有向图，节点是关键路段/事件点，边是可通行路线段。
 * 2. 使用 A* 搜索；当启发式权重为 0 时退化为 Dijkstra，适合无可靠空间启发信息的场景。
 * 3. 边权由预计时间、风险、拥堵敏感度和业务偏好动态组合。
 *
 * 复杂度：使用优先队列时，时间复杂度 O((V + E) log V)，空间复杂度 O(V + E)。
 */
public class AdaptiveRoutePlanner {
    private final Map<String, Node> nodes = new HashMap<String, Node>();
    private final Map<String, List<Edge>> graph = new HashMap<String, List<Edge>>();

    public AdaptiveRoutePlanner() {
        addNode("START", 0.0, 0.0);
        addNode("MAIN_ROAD", 1.0, 0.1);
        addNode("SCHOOL_ZONE", 1.8, -0.4);
        addNode("ELEVATED", 1.2, 0.9);
        addNode("GREEN_WAVE", 2.0, 0.6);
        addNode("HOSPITAL", 3.0, 0.0);
        addNode("AIRPORT", 3.4, 0.7);

        addEdge("START", "MAIN_ROAD", "route_A_main_road", 8.0, 0.18, 0.95, false);
        addEdge("MAIN_ROAD", "HOSPITAL", "route_A_main_road", 14.0, 0.26, 1.10, false);
        addEdge("START", "SCHOOL_ZONE", "route_B_school_zone_slow_safe", 11.0, 0.16, 0.45, false);
        addEdge("SCHOOL_ZONE", "HOSPITAL", "route_B_school_zone_slow_safe", 18.0, 0.18, 0.50, false);
        addEdge("START", "ELEVATED", "route_C_elevated_detour", 12.0, 0.20, 0.40, false);
        addEdge("ELEVATED", "HOSPITAL", "route_C_elevated_detour", 18.0, 0.19, 0.35, false);
        addEdge("ELEVATED", "AIRPORT", "route_C_elevated_detour", 22.0, 0.22, 0.35, false);
        addEdge("START", "GREEN_WAVE", "route_D_emergency_green_wave", 9.0, 0.24, 0.25, true);
        addEdge("GREEN_WAVE", "HOSPITAL", "route_D_emergency_green_wave", 12.0, 0.20, 0.20, true);
        addEdge("GREEN_WAVE", "AIRPORT", "route_D_emergency_green_wave", 19.0, 0.23, 0.25, true);
    }

    public RoutePlan plan(RouteContext context) {
        RouteContext ctx = context == null ? new RouteContext() : context;
        String goal = ctx.destination != null && ctx.destination.toLowerCase(Locale.ROOT).contains("airport")
            ? "AIRPORT" : "HOSPITAL";

        PriorityQueue<SearchState> open = new PriorityQueue<SearchState>(Comparator.comparingDouble(s -> s.fScore));
        Map<String, Double> best = new HashMap<String, Double>();
        Map<String, String> previous = new HashMap<String, String>();
        Map<String, Edge> previousEdge = new HashMap<String, Edge>();

        best.put("START", 0.0);
        open.add(new SearchState("START", 0.0, heuristic("START", goal)));

        while (!open.isEmpty()) {
            SearchState current = open.poll();
            if (current.gScore > best.getOrDefault(current.nodeId, Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (goal.equals(current.nodeId)) {
                return buildPlan(goal, previous, previousEdge, best.get(goal), ctx);
            }

            for (Edge edge : graph.getOrDefault(current.nodeId, Collections.<Edge>emptyList())) {
                double edgeCost = dynamicCost(edge, ctx);
                if (Double.isInfinite(edgeCost)) {
                    continue;
                }
                double nextCost = current.gScore + edgeCost;
                if (nextCost < best.getOrDefault(edge.to, Double.POSITIVE_INFINITY)) {
                    best.put(edge.to, nextCost);
                    previous.put(edge.to, current.nodeId);
                    previousEdge.put(edge.to, edge);
                    open.add(new SearchState(edge.to, nextCost, nextCost + heuristic(edge.to, goal)));
                }
            }
        }

        RoutePlan failed = new RoutePlan();
        failed.routeId = "route_A_main_road";
        failed.algorithm = "A* search";
        failed.reason = "路网不可达，回退主路候选；建议接入真实地图路网后重试。";
        return failed;
    }

    private double dynamicCost(Edge edge, RouteContext ctx) {
        if (edge.requiresEmergencyClearance && ctx.ambulanceLaneClearance < 0.75) {
            return Double.POSITIVE_INFINITY;
        }

        double congestion = clamp(ctx.congestionLevel, 0.0, 1.0);
        double frictionPenalty = ctx.roadFriction < 0.70 ? 1.0 + (0.70 - ctx.roadFriction) : 1.0;
        double time = edge.baseMinutes * (1.0 + edge.congestionSensitivity * congestion) * frictionPenalty;
        double risk = edge.baseRisk + congestion * 0.22 + Math.max(0.0, 0.75 - ctx.roadFriction) * 0.35;

        double timeWeight = ctx.urgent ? 0.66 : 0.48;
        double riskWeight = ctx.safetyFirst ? 0.52 : 0.34;
        if (edge.requiresEmergencyClearance && ctx.urgent) {
            time *= 0.82;
        }
        return time * timeWeight + risk * 60.0 * riskWeight;
    }

    private RoutePlan buildPlan(String goal, Map<String, String> previous, Map<String, Edge> previousEdge,
                                double cost, RouteContext ctx) {
        List<String> nodePath = new ArrayList<String>();
        List<Edge> edgePath = new ArrayList<Edge>();
        String cursor = goal;
        while (cursor != null) {
            nodePath.add(cursor);
            Edge edge = previousEdge.get(cursor);
            if (edge != null) {
                edgePath.add(edge);
            }
            cursor = previous.get(cursor);
        }
        Collections.reverse(nodePath);
        Collections.reverse(edgePath);

        Map<String, Integer> routeVotes = new LinkedHashMap<String, Integer>();
        double minutes = 0.0;
        double risk = 0.0;
        for (Edge edge : edgePath) {
            routeVotes.put(edge.routeId, routeVotes.getOrDefault(edge.routeId, 0) + 1);
            minutes += edge.baseMinutes * (1.0 + edge.congestionSensitivity * clamp(ctx.congestionLevel, 0.0, 1.0));
            risk += edge.baseRisk;
        }

        String routeId = "route_A_main_road";
        int bestVotes = -1;
        for (Map.Entry<String, Integer> entry : routeVotes.entrySet()) {
            if (entry.getValue() > bestVotes) {
                routeId = entry.getKey();
                bestVotes = entry.getValue();
            }
        }

        RoutePlan plan = new RoutePlan();
        plan.routeId = routeId;
        plan.algorithm = "A* search with dynamic edge weights";
        plan.path = nodePath;
        plan.estimatedMinutes = Math.max(1.0, minutes);
        plan.riskScore = edgePath.isEmpty() ? 0.30 : clamp(risk / edgePath.size(), 0.0, 1.0);
        plan.cost = cost;
        plan.reason = "综合拥堵、风险、路面附着和乘客偏好后的最小代价路径";
        return plan;
    }

    private double heuristic(String from, String to) {
        Node a = nodes.get(from);
        Node b = nodes.get(to);
        if (a == null || b == null) {
            return 0.0;
        }
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy) * 2.0;
    }

    private void addNode(String id, double x, double y) {
        nodes.put(id, new Node(id, x, y));
    }

    private void addEdge(String from, String to, String routeId, double baseMinutes, double baseRisk,
                         double congestionSensitivity, boolean requiresEmergencyClearance) {
        graph.computeIfAbsent(from, ignored -> new ArrayList<Edge>())
            .add(new Edge(from, to, routeId, baseMinutes, baseRisk, congestionSensitivity, requiresEmergencyClearance));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class RouteContext {
        public String destination;
        public boolean urgent;
        public boolean safetyFirst;
        public double congestionLevel = 0.30;
        public double roadFriction = 0.83;
        public double ambulanceLaneClearance = 0.62;
    }

    public static class RoutePlan {
        public String routeId;
        public String algorithm;
        public List<String> path = new ArrayList<String>();
        public double estimatedMinutes;
        public double riskScore;
        public double cost;
        public String reason;
        public List<Map<String, Object>> candidateRoutes = new ArrayList<Map<String, Object>>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("routeId", routeId);
            map.put("algorithm", algorithm);
            map.put("path", path);
            map.put("estimatedMinutes", round(estimatedMinutes));
            map.put("riskScore", round(riskScore));
            map.put("cost", round(cost));
            map.put("reason", reason);
            map.put("candidateRoutes", candidateRoutes.isEmpty() ? defaultCandidateRoutes(routeId) : candidateRoutes);
            map.put("timeComplexity", "O((V + E) log V)");
            map.put("spaceComplexity", "O(V + E)");
            return map;
        }

        private double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }

        private List<Map<String, Object>> defaultCandidateRoutes(String selectedRoute) {
            List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
            candidates.add(candidate("route_A_main_road", Arrays.asList("START", "MAIN_ROAD", "HOSPITAL"), 12.4, 0.72, 0.38, "NORMAL", 0.86, selectedRoute));
            candidates.add(candidate("route_B_school_zone_slow_safe", Arrays.asList("START", "SCHOOL_ZONE", "HOSPITAL"), 14.1, 0.36, 0.28, "SCHOOL_ZONE_SLOW", 0.78, selectedRoute));
            candidates.add(candidate("route_C_elevated_detour", Arrays.asList("START", "ELEVATED", "HOSPITAL"), 16.8, 0.34, 0.30, "ELEVATED_CLEAR", 0.82, selectedRoute));
            candidates.add(candidate("route_D_emergency_green_wave", Arrays.asList("START", "GREEN_WAVE", "HOSPITAL"), 11.6, 0.25, 0.32, "GREEN_WAVE", 0.90, selectedRoute));
            return candidates;
        }

        private Map<String, Object> candidate(String routeId, List<String> path, double distanceKm,
                                              double congestion, double risk, String roadCondition,
                                              double communicationCoverage, String selectedRoute) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            double routeCost = distanceKm * 0.35 + congestion * 25.0 + risk * 30.0 + (1.0 - communicationCoverage) * 12.0;
            item.put("routeId", routeId);
            item.put("path", path);
            item.put("distanceKm", round(distanceKm));
            item.put("congestion", round(congestion));
            item.put("risk", round(risk));
            item.put("roadCondition", roadCondition);
            item.put("emergencyLaneAvailable", "route_D_emergency_green_wave".equals(routeId));
            item.put("communicationCoverage", round(communicationCoverage));
            item.put("cost", round(routeCost));
            item.put("recommended", routeId != null && routeId.equals(selectedRoute));
            item.put("reason", routeId != null && routeId.equals(selectedRoute)
                ? "动态边权总代价最低或满足当前任务优先级"
                : "相对推荐路径存在拥堵、距离、风险或通信覆盖劣势");
            return item;
        }
    }

    private static class Node {
        final String id;
        final double x;
        final double y;

        Node(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static class Edge {
        final String from;
        final String to;
        final String routeId;
        final double baseMinutes;
        final double baseRisk;
        final double congestionSensitivity;
        final boolean requiresEmergencyClearance;

        Edge(String from, String to, String routeId, double baseMinutes, double baseRisk,
             double congestionSensitivity, boolean requiresEmergencyClearance) {
            this.from = from;
            this.to = to;
            this.routeId = routeId;
            this.baseMinutes = baseMinutes;
            this.baseRisk = baseRisk;
            this.congestionSensitivity = congestionSensitivity;
            this.requiresEmergencyClearance = requiresEmergencyClearance;
        }
    }

    private static class SearchState {
        final String nodeId;
        final double gScore;
        final double fScore;

        SearchState(String nodeId, double gScore, double fScore) {
            this.nodeId = nodeId;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }
}
