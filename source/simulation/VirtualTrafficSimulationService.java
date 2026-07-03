package simulation;

import agent.AgentAdvice;
import agent.AgentRole;
import agent.AgentRuntime;
import agent.ScenarioLibrary;
import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import decision.ConflictResolver;
import planner.PlanningContext;
import planner.PlanningResult;
import planner.RouteCandidate;
import planner.RoutePlanningService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep simulation module for virtual traffic collaboration.
 *
 * Public callers only start, tick, stop, and read state. The implementation owns
 * virtual devices, DDS publication, planner comparison, and conflict resolution.
 */
public class VirtualTrafficSimulationService {
    private final ScenarioLibrary scenarios = ScenarioLibrary.getInstance();
    private final AgentRuntime agentRuntime = new AgentRuntime();
    private final RoutePlanningService planner = new RoutePlanningService();
    private final ConflictResolver conflictResolver = new ConflictResolver();
    private VirtualTrafficWorld world;
    private VirtualDDSMessageBus bus;
    private SimulationTickResult lastTickResult;

    public List<Map<String, Object>> listScenarios() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : scenarios.list()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("scenarioId", scenario.get("scenarioId"));
            item.put("name", scenario.get("name"));
            item.put("description", scenario.get("description"));
            item.put("naturalLanguageTask", scenario.get("passengerIntent"));
            item.put("simulationMode", "structured_2d_virtual_traffic");
            item.put("devices", list("CAR-CQ-001", "UAV-CQ-007", "RSU-SCHOOL-009", "PEER-CAR-031"));
            item.put("topics", list(
                VirtualDDSMessageBus.TOPIC_INTENT_DSL,
                VirtualDDSMessageBus.TOPIC_VEHICLE_STATUS,
                VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION,
                VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT,
                VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS,
                VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION,
                VirtualDDSMessageBus.TOPIC_FINAL_DECISION,
                VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> start(String scenarioId) {
        stop();
        Map<String, Object> scenario = scenarios.get(scenarioId == null || scenarioId.isBlank()
            ? "hospital-emergency" : scenarioId);
        double congestion = number(castMap(scenario.get("initialWorldState")), "congestionLevel", 0.72);
        double risk = number(castMap(scenario.get("initialWorldState")), "roadRisk", 0.52);
        VirtualRoadNetwork roadNetwork = VirtualRoadNetwork.urbanEmergencyNetwork(congestion, risk);
        this.world = new VirtualTrafficWorld(
            String.valueOf(scenario.get("scenarioId")),
            String.valueOf(scenario.get("name")),
            String.valueOf(scenario.getOrDefault("passengerIntent", "")),
            roadNetwork,
            new VirtualVehicleState("CAR-CQ-001", "Ego autonomous vehicle", "origin", 10, 68),
            new VirtualDroneState("UAV-CQ-007", "Forward perception UAV", "main_junction", 36, 28),
            new VirtualRsuState("RSU-SCHOOL-009", "Roadside unit", "school_zone", 54, 45),
            new VirtualNeighborVehicleState("PEER-CAR-031", "Neighbor vehicle", "main_junction", 30, 62)
        );
        this.bus = new VirtualDDSMessageBus(20260703L);
        publishIntent();
        this.lastTickResult = null;
        return state();
    }

    public SimulationTickResult tick() {
        ensureRunning();
        world.advanceKinematics();
        List<DDSMessage> messages = publishDeviceMessages();
        List<AgentAdvice> advice = adviceFromMessages(messages);
        if (advice.isEmpty()) {
            advice.addAll(agentRuntime.collectAdvice(world.naturalLanguageTask));
        }
        Map<String, Object> plannerResult = plannerComparison();
        Map<String, Object> conflict = conflictResolver.resolveMessages(messages, world.naturalLanguageTask);
        Map<String, Object> accepted = castMap(conflict.get("acceptedSuggestion"));
        String plannerRoute = String.valueOf(castMap(plannerResult.get("riskAware")).getOrDefault("routeId", ""));
        String finalRoute = String.valueOf(accepted.getOrDefault("route", plannerRoute));
        if (finalRoute == null || finalRoute.isBlank()) {
            finalRoute = plannerRoute;
        }
        String explanation = String.valueOf(conflict.getOrDefault("decisionReason",
            "Planner selected the route with the best risk-aware score."));
        world.finalRouteId = finalRoute;
        world.finalExplanation = explanation;
        world.vehicle.currentRouteId = finalRoute;

        DDSMessage finalDecisionMessage = publishFinalDecision(finalRoute, explanation, conflict);
        messages.add(finalDecisionMessage);
        if (Boolean.TRUE.equals(conflict.get("whetherReplanningTriggered"))) {
            messages.add(publishReplanningRequest(finalRoute, conflict));
        }

        List<Map<String, Object>> ddsMaps = messagesToMaps(messages);
        List<Map<String, Object>> adviceMaps = adviceToMaps(advice);
        Map<String, Object> finalDecision = new LinkedHashMap<String, Object>();
        finalDecision.put("route", finalRoute);
        finalDecision.put("explanation", explanation);
        finalDecision.put("owner", "CAR-CQ-001");
        finalDecision.put("tick", world.tick);
        finalDecision.put("ddsTopic", VirtualDDSMessageBus.TOPIC_FINAL_DECISION);
        finalDecision.put("replanningTriggered", conflict.get("whetherReplanningTriggered"));

        lastTickResult = new SimulationTickResult(world.tick, world.toMap(), ddsMaps,
            adviceMaps, plannerResult, conflict, finalDecision);
        return lastTickResult;
    }

    public Map<String, Object> stop() {
        if (world != null) {
            world.running = false;
        }
        if (bus != null) {
            bus.shutdown();
        }
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("running", false);
        response.put("tick", world == null ? 0 : world.tick);
        return response;
    }

    public Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("ok", true);
        state.put("running", world != null && world.running);
        state.put("currentTick", world == null ? 0 : world.tick);
        state.put("world", world == null ? new LinkedHashMap<String, Object>() : world.toMap());
        state.put("recentDdsMessages", ddsEvents());
        state.put("lastTickResult", lastTickResult == null ? new LinkedHashMap<String, Object>() : lastTickResult.toMap());
        if (lastTickResult != null) {
            state.put("agentAdvice", lastTickResult.agentAdvice);
            state.put("plannerResult", lastTickResult.plannerResult);
            state.put("conflictResolution", lastTickResult.conflictResolution);
            state.put("finalDecision", lastTickResult.finalDecision);
        }
        return state;
    }

    public List<Map<String, Object>> ddsEvents() {
        if (bus == null) return new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> history = bus.history();
        int from = Math.max(0, history.size() - 60);
        return new ArrayList<Map<String, Object>>(history.subList(from, history.size()));
    }

    private void publishIntent() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("intentText", world.naturalLanguageTask);
        payload.put("contractType", "DSL_TASK_CONTRACT");
        payload.put("destination", "hospital");
        payload.put("confidence", 0.93);
        bus.publish(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU", payload, DDSQoS.reliable());
    }

    private List<DDSMessage> publishDeviceMessages() {
        List<DDSMessage> messages = new ArrayList<DDSMessage>();
        VirtualRoadNetwork.VirtualRoute mainRoute = world.roadNetwork.route("route_A_main_road");
        VirtualRoadNetwork.VirtualRoute bestRoute = world.roadNetwork.bestByRiskAwareScore();
        String bestRouteId = bestRoute == null ? "route_D_emergency_green_wave" : bestRoute.routeId;
        double mainRisk = mainRoute == null ? 0.55 : mainRoute.riskScore;
        double mainCongestion = mainRoute == null ? 0.70 : mainRoute.congestionLevel;

        messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_VEHICLE_STATUS, world.vehicle.deviceId,
            payload("tick", world.tick, "missionStatus", world.vehicle.missionStatus,
                "route", world.vehicle.currentRouteId, "positionNode", world.vehicle.roadNodeId,
                "x", world.vehicle.x, "y", world.vehicle.y, "routeProgress", world.vehicle.routeProgress,
                "confidence", 0.90), DDSQoS.bestEffort()));
        messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_DRONE_OBSERVATION, world.drone.deviceId,
            payload("tick", world.tick, "role", "UAV", "route", bestRouteId,
                "observedRoute", world.drone.observedRouteId, "congestionLevel", mainCongestion,
                "riskScore", bestRoute == null ? 0.34 : bestRoute.riskScore,
                "affectedRoute", "route_A_main_road", "affectedRiskScore", mainRisk,
                "timeAdvantage", bestRouteId.contains("green") ? 0.82 : 0.62,
                "intentAlignment", 0.82, "confidence", 0.88,
                "explanation", "UAV reports forward congestion and recommends the clearer route."),
            DDSQoS.bestEffort()));
        messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, world.rsu.deviceId,
            payload("tick", world.tick, "role", "RSU", "route", "route_D_emergency_green_wave",
                "signalPhase", world.rsu.signalPhase, "road_event", world.rsu.activeEvents,
                "riskScore", 0.28, "affectedRoute", "route_A_main_road",
                "affectedRiskScore", Math.max(0.62, mainRisk), "timeAdvantage", 0.76,
                "intentAlignment", 0.88, "confidence", 0.94,
                "explanation", "RSU publishes accident, signal, and green-wave risk evidence."),
            DDSQoS.reliable()));
        messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_NEIGHBOR_STATUS, world.neighborVehicle.deviceId,
            payload("tick", world.tick, "role", "PEER_VEHICLE", "route", "route_C_elevated_detour",
                "queueLengthM", world.neighborVehicle.queueLengthM + world.tick * 18.0,
                "followingRisk", world.neighborVehicle.followingRisk + world.tick * 0.025,
                "riskScore", 0.34, "affectedRoute", "route_A_main_road",
                "affectedRiskScore", Math.max(0.56, mainRisk - 0.04), "timeAdvantage", 0.58,
                "intentAlignment", 0.74, "confidence", 0.78,
                "explanation", "Neighbor vehicle reports queue growth and following risk."),
            DDSQoS.bestEffort()));
        messages.add(bus.publish(VirtualDDSMessageBus.TOPIC_ROUTE_SUGGESTION, world.vehicle.deviceId,
            payload("tick", world.tick, "role", "EGO_VEHICLE", "route", bestRouteId,
                "riskScore", bestRoute == null ? 0.30 : bestRoute.riskScore,
                "timeAdvantage", bestRouteId.contains("green") ? 0.84 : 0.60,
                "intentAlignment", 0.86, "confidence", 0.87,
                "explanation", "Vehicle publishes its local route suggestion after reading virtual DDS traffic evidence."),
            DDSQoS.reliable()));
        return messages;
    }

    private List<AgentAdvice> adviceFromMessages(List<DDSMessage> messages) {
        List<AgentAdvice> advice = new ArrayList<AgentAdvice>();
        for (DDSMessage message : messages) {
            String route = string(message.payload, "route", "");
            if (route.isBlank()) continue;
            String explanation = string(message.payload, "explanation", "DDS evidence converted to Agent advice.");
            advice.add(new AgentAdvice(message.sourceId, role(message.sourceId), message.topic, route,
                number(message.payload, "riskScore", 0.35), message.confidence, explanation));
        }
        return advice;
    }

    private Map<String, Object> plannerComparison() {
        List<RouteCandidate> candidates = world.roadNetwork.routeCandidates();
        PlanningContext context = PlanningContext.safetyFirst();
        PlanningResult dijkstra = planner.planDijkstraBaseline(candidates);
        PlanningResult greedy = planner.planGreedyBaseline(candidates);
        PlanningResult riskAware = planner.planRiskAware(candidates, context);
        PlanningResult multiObjective = planner.planMultiObjective(candidates, context);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("candidateRoutes", candidatesToMaps(candidates));
        result.put("dijkstraBaseline", dijkstra.toMap());
        result.put("greedyBaseline", greedy.toMap());
        result.put("riskAware", riskAware.toMap());
        result.put("multiObjective", multiObjective.toMap());
        result.put("explanation", "Dijkstra and Greedy are baseline references; final selection uses DDS evidence, risk-aware planning, and conflict resolution.");
        return result;
    }

    private DDSMessage publishFinalDecision(String route, String explanation, Map<String, Object> conflict) {
        return bus.publish(VirtualDDSMessageBus.TOPIC_FINAL_DECISION, "CAR-CQ-001",
            payload("tick", world.tick, "route", route, "riskScore", conflict.get("riskScore"),
                "confidence", conflict.get("confidence"), "explanation", explanation,
                "decisionOwner", "CAR-CQ-001"), DDSQoS.reliable());
    }

    private DDSMessage publishReplanningRequest(String route, Map<String, Object> conflict) {
        return bus.publish(VirtualDDSMessageBus.TOPIC_REPLANNING_REQUEST, "CAR-CQ-001",
            payload("tick", world.tick, "route", route, "reason", conflict.get("conflictType"),
                "confidence", 0.90, "explanation", "Risk or route divergence triggered dynamic replanning."),
            DDSQoS.reliable());
    }

    private void ensureRunning() {
        if (world == null || bus == null || !world.running) {
            start("hospital-emergency");
        }
    }

    private List<Map<String, Object>> messagesToMaps(List<DDSMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DDSMessage message : messages) {
            result.add(new LinkedHashMap<String, Object>(message.toMap()));
        }
        return result;
    }

    private List<Map<String, Object>> adviceToMaps(List<AgentAdvice> advice) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (AgentAdvice item : advice) {
            result.add(item.toMap());
        }
        return result;
    }

    private List<Map<String, Object>> candidatesToMaps(List<RouteCandidate> candidates) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RouteCandidate candidate : candidates) {
            result.add(candidate.toMap());
        }
        return result;
    }

    private AgentRole role(String sourceId) {
        if (sourceId == null) return AgentRole.EGO_VEHICLE;
        if (sourceId.contains("UAV")) return AgentRole.UAV;
        if (sourceId.contains("RSU")) return AgentRole.RSU;
        if (sourceId.contains("PEER")) return AgentRole.PEER_VEHICLE;
        return AgentRole.EGO_VEHICLE;
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private List<String> list(String... values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) result.add(value);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private double number(Map<String, Object> payload, String key, double fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }
}
