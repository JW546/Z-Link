package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;

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
        boolean congestion = text.contains("拥堵") || text.contains("堵") || text.toLowerCase().contains("congestion");
        List<AgentAdvice> advice = new ArrayList<AgentAdvice>();
        advice.add(new AgentAdvice("CAR-CQ-002", AgentRole.PEER_VEHICLE, "urban/vehicle/peer_advice.dsl",
            congestion ? "route_C_elevated_detour" : "route_A_main_road",
            congestion ? 0.50 : 0.28, 0.84,
            "邻车基于跟驰和队列状态发布路线建议。"));
        advice.add(new AgentAdvice("UAV-CQ-007", AgentRole.UAV, "urban/uav/aerial_perception.dsl",
            congestion ? "route_C_elevated_detour" : "route_A_main_road",
            congestion ? 0.28 : 0.22, 0.88,
            "无人机基于前视感知发布空中视角建议。"));
        advice.add(new AgentAdvice("RSU-SCHOOL-009", AgentRole.RSU, "urban/rsu/road_event.dsl",
            congestion ? "route_D_emergency_green_wave" : "route_A_main_road",
            congestion ? 0.36 : 0.20, 0.86,
            "路侧节点基于信号灯和路侧事件发布建议。"));
        advice.add(new AgentAdvice("CN-BACKBONE-CQ", AgentRole.COMPLIANT_BACKBONE, "backbone/compliant/context.dsl",
            congestion ? "route_D_emergency_green_wave" : "route_A_main_road",
            congestion ? 0.52 : 0.31, 0.76,
            "通信适配节点只提供合规态势，不执行远程派单。"));
        return advice;
    }
}
