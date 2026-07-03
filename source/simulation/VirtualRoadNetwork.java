package simulation;

import planner.RouteCandidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured 2D road topology with route-level congestion and risk state.
 */
public class VirtualRoadNetwork {
    public final List<RoadNode> nodes = new ArrayList<RoadNode>();
    public final List<RoadEdge> edges = new ArrayList<RoadEdge>();
    public final List<VirtualRoute> routes = new ArrayList<VirtualRoute>();

    public static VirtualRoadNetwork urbanEmergencyNetwork(double initialCongestion, double initialRisk) {
        VirtualRoadNetwork network = new VirtualRoadNetwork();
        network.addNode("origin", "Passenger origin", 10, 68);
        network.addNode("main_junction", "Main road junction", 34, 58);
        network.addNode("school_zone", "School zone", 54, 45);
        network.addNode("elevated", "Elevated detour", 48, 24);
        network.addNode("green_wave", "Emergency green wave", 69, 54);
        network.addNode("hospital", "Hospital destination", 88, 35);

        network.addEdge("edge_origin_main", "origin", "main_junction", 1.8, initialCongestion * 0.65, initialRisk * 0.45);
        network.addEdge("edge_main_school", "main_junction", "school_zone", 2.4, initialCongestion, initialRisk);
        network.addEdge("edge_school_hospital", "school_zone", "hospital", 2.8, Math.max(0.25, initialCongestion - 0.22), Math.max(0.18, initialRisk - 0.25));
        network.addEdge("edge_main_elevated", "main_junction", "elevated", 3.0, Math.max(0.18, initialCongestion - 0.48), Math.max(0.16, initialRisk - 0.32));
        network.addEdge("edge_elevated_hospital", "elevated", "hospital", 3.8, Math.max(0.20, initialCongestion - 0.45), Math.max(0.18, initialRisk - 0.30));
        network.addEdge("edge_main_green", "main_junction", "green_wave", 2.2, Math.max(0.20, initialCongestion - 0.36), Math.max(0.16, initialRisk - 0.35));
        network.addEdge("edge_green_hospital", "green_wave", "hospital", 2.0, Math.max(0.16, initialCongestion - 0.52), Math.max(0.14, initialRisk - 0.38));

        network.addRoute("route_A_main_road", "Main road short path",
            Arrays.asList("origin", "main_junction", "school_zone", "hospital"),
            Arrays.asList("edge_origin_main", "edge_main_school", "edge_school_hospital"));
        network.addRoute("route_C_elevated_detour", "Elevated detour",
            Arrays.asList("origin", "main_junction", "elevated", "hospital"),
            Arrays.asList("edge_origin_main", "edge_main_elevated", "edge_elevated_hospital"));
        network.addRoute("route_D_emergency_green_wave", "Emergency green-wave route",
            Arrays.asList("origin", "main_junction", "green_wave", "hospital"),
            Arrays.asList("edge_origin_main", "edge_main_green", "edge_green_hospital"));
        return network;
    }

    public void advance(int tick) {
        for (RoadEdge edge : edges) {
            double wave = Math.sin((tick + edge.edgeId.length()) / 3.0) * 0.035;
            edge.congestionLevel = clamp(edge.congestionLevel + wave);
            if (edge.edgeId.contains("main_school") && tick >= 2) {
                edge.congestionLevel = clamp(edge.congestionLevel + 0.08);
                edge.riskScore = clamp(edge.riskScore + 0.06);
                edge.event = "ACCIDENT_QUEUE";
            }
            if (edge.edgeId.contains("green")) {
                edge.congestionLevel = clamp(edge.congestionLevel - 0.03);
                edge.riskScore = clamp(edge.riskScore - 0.02);
            }
        }
        refreshRoutes();
    }

    public List<RouteCandidate> routeCandidates() {
        refreshRoutes();
        List<RouteCandidate> candidates = new ArrayList<RouteCandidate>();
        for (VirtualRoute route : routes) {
            candidates.add(new RouteCandidate(
                route.routeId,
                route.estimatedMinutes,
                route.riskScore,
                route.congestionLevel,
                route.routeId.contains("emergency") ? 0.86 : 0.48,
                0.86,
                route.routeId.contains("green") ? 0.92 : 0.82,
                0.95,
                route.congestionLevel > 0.70 ? 0.16 : 0.04,
                0.0,
                0.0
            ));
        }
        return candidates;
    }

    public VirtualRoute bestByRiskAwareScore() {
        refreshRoutes();
        VirtualRoute best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (VirtualRoute route : routes) {
            double score = 1.0 - route.riskScore - route.congestionLevel * 0.45 - route.estimatedMinutes / 60.0;
            if (best == null || score > bestScore) {
                best = route;
                bestScore = score;
            }
        }
        return best;
    }

    public RoadNode node(String nodeId) {
        for (RoadNode node : nodes) {
            if (node.nodeId.equals(nodeId)) return node;
        }
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    public VirtualRoute route(String routeId) {
        for (VirtualRoute route : routes) {
            if (route.routeId.equals(routeId)) return route;
        }
        return routes.isEmpty() ? null : routes.get(0);
    }

    public Map<String, Object> toMap() {
        refreshRoutes();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> nodeMaps = new ArrayList<Map<String, Object>>();
        for (RoadNode node : nodes) nodeMaps.add(node.toMap());
        List<Map<String, Object>> edgeMaps = new ArrayList<Map<String, Object>>();
        for (RoadEdge edge : edges) edgeMaps.add(edge.toMap());
        List<Map<String, Object>> routeMaps = new ArrayList<Map<String, Object>>();
        for (VirtualRoute route : routes) routeMaps.add(route.toMap());
        map.put("nodes", nodeMaps);
        map.put("edges", edgeMaps);
        map.put("routes", routeMaps);
        return map;
    }

    private void addNode(String id, String label, double x, double y) {
        nodes.add(new RoadNode(id, label, x, y));
    }

    private void addEdge(String id, String from, String to, double lengthKm, double congestion, double risk) {
        edges.add(new RoadEdge(id, from, to, lengthKm, clamp(congestion), clamp(risk)));
    }

    private void addRoute(String id, String label, List<String> nodeIds, List<String> edgeIds) {
        routes.add(new VirtualRoute(id, label, nodeIds, edgeIds));
        refreshRoutes();
    }

    private void refreshRoutes() {
        for (VirtualRoute route : routes) {
            double distance = 0.0;
            double congestion = 0.0;
            double risk = 0.0;
            int count = 0;
            for (String edgeId : route.edgeIds) {
                RoadEdge edge = edge(edgeId);
                if (edge == null) continue;
                distance += edge.lengthKm;
                congestion += edge.congestionLevel;
                risk = Math.max(risk, edge.riskScore);
                count++;
            }
            route.distanceKm = distance;
            route.congestionLevel = count == 0 ? 0.0 : congestion / count;
            route.riskScore = risk;
            route.estimatedMinutes = Math.max(4.0, distance / Math.max(18.0, 46.0 - route.congestionLevel * 24.0) * 60.0);
        }
    }

    private RoadEdge edge(String edgeId) {
        for (RoadEdge edge : edges) {
            if (edge.edgeId.equals(edgeId)) return edge;
        }
        return null;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static class RoadNode {
        public final String nodeId;
        public final String label;
        public final double x;
        public final double y;

        public RoadNode(String nodeId, String label, double x, double y) {
            this.nodeId = nodeId;
            this.label = label;
            this.x = x;
            this.y = y;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("nodeId", nodeId);
            map.put("label", label);
            map.put("x", x);
            map.put("y", y);
            return map;
        }
    }

    public static class RoadEdge {
        public final String edgeId;
        public final String fromNodeId;
        public final String toNodeId;
        public final double lengthKm;
        public double congestionLevel;
        public double riskScore;
        public String event = "";

        public RoadEdge(String edgeId, String fromNodeId, String toNodeId, double lengthKm,
                        double congestionLevel, double riskScore) {
            this.edgeId = edgeId;
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.lengthKm = lengthKm;
            this.congestionLevel = congestionLevel;
            this.riskScore = riskScore;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("edgeId", edgeId);
            map.put("fromNodeId", fromNodeId);
            map.put("toNodeId", toNodeId);
            map.put("lengthKm", lengthKm);
            map.put("congestionLevel", round(congestionLevel));
            map.put("riskScore", round(riskScore));
            map.put("event", event);
            return map;
        }
    }

    public static class VirtualRoute {
        public final String routeId;
        public final String label;
        public final List<String> nodeIds;
        public final List<String> edgeIds;
        public double distanceKm;
        public double estimatedMinutes;
        public double congestionLevel;
        public double riskScore;

        public VirtualRoute(String routeId, String label, List<String> nodeIds, List<String> edgeIds) {
            this.routeId = routeId;
            this.label = label;
            this.nodeIds = new ArrayList<String>(nodeIds);
            this.edgeIds = new ArrayList<String>(edgeIds);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("routeId", routeId);
            map.put("label", label);
            map.put("nodeIds", new ArrayList<String>(nodeIds));
            map.put("edgeIds", new ArrayList<String>(edgeIds));
            map.put("distanceKm", round(distanceKm));
            map.put("estimatedMinutes", round(estimatedMinutes));
            map.put("congestionLevel", round(congestionLevel));
            map.put("riskScore", round(riskScore));
            return map;
        }
    }
}
