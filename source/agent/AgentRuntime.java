package agent;

import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import planner.CandidateRoute;
import planner.RouteFeatureExtractor;
import planner.RouteScoringContext;
import policy.AgentState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime seam for autonomous device Agents.
 */
public class AgentRuntime {
    private final List<AutonomousAgent> agents;

    public AgentRuntime() {
        this.agents = Arrays.asList(
            new VehicleAgent("CAR-CQ-001"),
            new DroneAgent("UAV-CQ-007"),
            new RoadsideAgent("RSU-SCHOOL-009"),
            new NeighborVehicleAgent("PEER-CAR-031"),
            new CommunicationAdapterAgent("CN-BACKBONE-CQ")
        );
    }

    public List<AutonomousAgent> agents() {
        return agents;
    }

    public List<AgentAdvice> runScenario(VirtualDDSMessageBus bus, String intentText) {
        for (AutonomousAgent agent : agents) {
            agent.observe(bus);
        }
        Map<String, Object> intentPayload = new LinkedHashMap<String, Object>();
        intentPayload.put("intentText", intentText == null ? "" : intentText);
        intentPayload.put("confidence", 0.92);
        bus.publish(VirtualDDSMessageBus.TOPIC_INTENT_DSL, "CABIN-NLU", intentPayload, DDSQoS.reliable());

        Map<String, Object> riskPayload = new LinkedHashMap<String, Object>();
        riskPayload.put("road_event", "main_road_accident_and_congestion");
        riskPayload.put("riskScore", 0.68);
        riskPayload.put("congestionLevel", 0.84);
        riskPayload.put("route", "route_A_main_road");
        riskPayload.put("confidence", 0.88);
        bus.publish(VirtualDDSMessageBus.TOPIC_ROADSIDE_RISK_EVENT, "RSU-SCHOOL-009", riskPayload, DDSQoS.reliable());

        List<AgentAdvice> advice = new ArrayList<AgentAdvice>();
        for (AutonomousAgent agent : agents) {
            advice.add(agent.propose());
        }
        for (AutonomousAgent agent : agents) {
            agent.arbitrate(advice);
            agent.act(bus);
        }
        return advice;
    }

    public List<AgentAdvice> collectAdvice(String intentText) {
        String text = intentText == null ? "" : intentText;
        boolean congestion = containsAny(text, "拥堵", "堵", "congestion", "accident");
        boolean urgent = containsAny(text, "尽快", "紧急", "urgent", "emergency");
        boolean safetyFirst = containsAny(text, "安全", "避开", "safe");
        AgentState state = AgentState.builder("COLLECT-ADVICE")
            .passengerIntent(text)
            .passengerUrgent(urgent)
            .passengerSafetyFirst(safetyFirst)
            .congestionLevel(congestion ? 0.82 : 0.32)
            .safetyRisk(congestion ? 0.58 : 0.28)
            .messageConfidence(0.86)
            .sourceTrust(0.82)
            .messageFreshness(1.0)
            .candidateRoutes(Arrays.asList("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave"))
            .build();

        List<AgentAdvice> advice = new ArrayList<AgentAdvice>();
        advice.add(new AgentAdvice("CAR-CQ-002", AgentRole.PEER_VEHICLE, "urban/vehicle/peer_advice.dsl",
            selectRoute(state, AgentRole.PEER_VEHICLE, 0.78),
            congestion ? 0.50 : 0.28, 0.84,
            "邻车基于跟驰、排队和链路证据发布路线建议。"));
        advice.add(new AgentAdvice("UAV-CQ-007", AgentRole.UAV, "urban/uav/aerial_perception.dsl",
            selectRoute(state, AgentRole.UAV, 0.88),
            congestion ? 0.28 : 0.22, 0.88,
            "无人机基于前视感知和 DDS 证据质量发布路线建议。"));
        advice.add(new AgentAdvice("RSU-SCHOOL-009", AgentRole.RSU, "urban/rsu/road_event.dsl",
            selectRoute(state, AgentRole.RSU, 0.94),
            congestion ? 0.36 : 0.20, 0.86,
            "路侧节点基于信号灯、道路事件和风险阈值发布建议。"));
        advice.add(new AgentAdvice("CN-BACKBONE-CQ", AgentRole.COMPLIANT_BACKBONE, "backbone/compliant/context.dsl",
            selectRoute(state, AgentRole.COMPLIANT_BACKBONE, 0.78),
            congestion ? 0.52 : 0.31, 0.76,
            "通信适配节点提供合规态势，不执行远程派单。"));
        return advice;
    }

    private String selectRoute(AgentState state, AgentRole role, double sourceTrust) {
        double congestion = state.congestionLevel;
        List<CandidateRoute> candidates = Arrays.asList(
            CandidateRoute.builder("route_A_main_road")
                .travelTime(congestion > 0.65 ? 18.0 : 10.0)
                .distance(5.0)
                .safetyRisk(congestion > 0.65 ? 0.72 : 0.30)
                .congestionLevel(congestion)
                .messageConfidence(0.82)
                .sourceTrust(sourceTrust)
                .messageFreshness(0.95)
                .build(),
            CandidateRoute.builder("route_C_elevated_detour")
                .travelTime(14.0)
                .distance(7.0)
                .safetyRisk(0.34)
                .congestionLevel(0.30)
                .messageConfidence(0.86)
                .sourceTrust(sourceTrust)
                .messageFreshness(0.94)
                .build(),
            CandidateRoute.builder("route_D_emergency_green_wave")
                .travelTime(11.5)
                .distance(6.2)
                .safetyRisk(0.28)
                .congestionLevel(0.24)
                .emergencyPriority(0.88)
                .passengerPreference(state.passengerUrgent ? 0.92 : 0.60)
                .messageConfidence(0.88)
                .sourceTrust(sourceTrust)
                .messageFreshness(0.96)
                .build()
        );
        return new RouteFeatureExtractor()
            .select(candidates, RouteScoringContext.fromState(state, role))
            .routeId();
    }

    private boolean containsAny(String text, String... tokens) {
        String value = text == null ? "" : text.toLowerCase();
        for (String token : tokens) {
            if (value.contains(token.toLowerCase())) return true;
        }
        return false;
    }
}
