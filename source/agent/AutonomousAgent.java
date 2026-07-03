package agent;

import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import planner.RouteFeatureExtractor;
import planner.RouteScoringContext;
import planner.RouteSelectionResult;
import policy.AgentAction;
import policy.AgentState;
import policy.WeightedAgentPolicy;
import worldmodel.SystemWorldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AutonomousAgent {
    protected final String agentId;
    protected final AgentRole role;
    protected final SystemWorldModel worldModel;
    protected final WeightedAgentPolicy policy;
    protected final RouteFeatureExtractor routeSelector = new RouteFeatureExtractor();
    protected final List<DDSMessage> inbox = new ArrayList<DDSMessage>();
    protected final List<AgentAdvice> proposals = new ArrayList<AgentAdvice>();
    protected String lastExplanation = "";
    protected RouteSelectionResult lastRouteSelection;

    protected AutonomousAgent(String agentId, AgentRole role, WeightedAgentPolicy policy) {
        this.agentId = agentId;
        this.role = role;
        this.policy = policy;
        this.worldModel = new SystemWorldModel();
    }

    public void observe(VirtualDDSMessageBus bus) {
        for (String topic : subscribedTopics()) {
            bus.subscribe(topic, this::updateState);
        }
    }

    public void updateState(DDSMessage message) {
        inbox.add(message);
        worldModel.update(message);
    }

    public MessageAssessment evaluate(DDSMessage message) {
        double sourceTrust = worldModel.trustFor(message.sourceId);
        double confidence = message.confidence;
        double freshness = message.messageFreshness();
        double risk = readNumber(message.payload, "riskScore", readNumber(message.payload, "risk_score", worldModel.roadRisk));
        double alignment = intentAlignment(message);
        double conflict = worldModel.hasConflict ? 0.35 : 0.0;
        return new MessageAssessment(message, sourceTrust, confidence, freshness, risk, alignment, conflict);
    }

    public AgentAdvice propose() {
        AgentState state = worldModel.toAgentState(agentId, role.name());
        AgentAction action = policy.decide(state);
        String route = chooseRoute(action.route, state);
        double selectedRisk = lastRouteSelection != null && lastRouteSelection.selectedRoute != null
            ? lastRouteSelection.selectedRoute.safetyRisk : state.localRisk;
        double selectedConfidence = lastRouteSelection != null && lastRouteSelection.selectedRoute != null
            ? lastRouteSelection.selectedRoute.messageConfidence : state.messageConfidence;
        double risk = clamp(selectedRisk + roleRiskBias());
        double confidence = clamp(selectedConfidence * 0.55 + state.sourceTrust * 0.30 + state.messageFreshness * 0.15);
        lastExplanation = explain(action, state, route);
        AgentAdvice advice = new AgentAdvice(agentId, role, publishTopic(), route, risk, confidence, lastExplanation);
        proposals.add(advice);
        return advice;
    }

    public double arbitrate(List<AgentAdvice> advice) {
        AgentState state = worldModel.toAgentState(agentId, role.name());
        double routeAgreement = 0.0;
        AgentAdvice own = proposals.isEmpty() ? propose() : proposals.get(proposals.size() - 1);
        if (advice != null && !advice.isEmpty()) {
            int same = 0;
            for (AgentAdvice item : advice) {
                if (own.route.equals(item.route)) same++;
            }
            routeAgreement = same / (double) advice.size();
        }
        return clamp(state.sourceTrust * 0.35 + own.confidence * 0.30 + (1.0 - own.riskScore) * 0.20 + routeAgreement * 0.15);
    }

    public DDSMessage act(VirtualDDSMessageBus bus) {
        AgentAdvice advice = proposals.isEmpty() ? propose() : proposals.get(proposals.size() - 1);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("agentId", advice.agentId);
        payload.put("role", advice.role.name());
        payload.put("route", advice.route);
        payload.put("riskScore", advice.riskScore);
        payload.put("confidence", advice.confidence);
        payload.put("explanation", advice.explanation);
        return bus.publish(advice.topic, agentId, payload, DDSQoS.reliable().withTimeToLiveMs(2_500L));
    }

    public String explain() {
        return lastExplanation;
    }

    public int observedCount() {
        return inbox.size();
    }

    public SystemWorldModel worldModel() {
        return worldModel;
    }

    public String agentId() {
        return agentId;
    }

    public AgentRole role() {
        return role;
    }

    protected abstract List<String> subscribedTopics();

    protected abstract String publishTopic();

    protected String chooseRoute(String policyRoute, AgentState state) {
        RouteScoringContext context = RouteScoringContext.fromState(state, role);
        lastRouteSelection = routeSelector.select(state, context);
        String selected = lastRouteSelection.routeId();
        return selected.isBlank() ? (policyRoute == null ? "" : policyRoute) : selected;
    }

    protected abstract double roleRiskBias();

    protected String explain(AgentAction action, AgentState state, String route) {
        String routeReason = lastRouteSelection == null ? "" : " " + lastRouteSelection.explanation
            + " DDS influence=" + lastRouteSelection.influencingMessages + ".";
        return role.name() + " agent " + agentId + " chose " + route
            + " because " + action.reason
            + ", risk=" + round(state.localRisk)
            + ", congestion=" + round(state.congestionLevel)
            + ", freshness=" + round(state.messageFreshness)
            + ", trust=" + round(state.sourceTrust) + "."
            + routeReason;
    }

    protected double intentAlignment(DDSMessage message) {
        String text = worldModel.passengerIntent == null ? "" : worldModel.passengerIntent;
        double score = 0.45;
        double risk = readNumber(message.payload, "safetyRisk", readNumber(message.payload, "riskScore", worldModel.roadRisk));
        double congestion = readNumber(message.payload, "congestionLevel", worldModel.congestionLevel);
        double emergency = readNumber(message.payload, "emergencyPriority", readNumber(message.payload, "emergencyLevel", 0.0));
        if ((text.contains("安全") || text.toLowerCase().contains("safe")) && risk <= 0.35) score += 0.35;
        if ((text.contains("尽快") || text.toLowerCase().contains("urgent")) && emergency >= 0.50) score += 0.35;
        if ((text.contains("拥堵") || text.toLowerCase().contains("congestion")) && congestion <= 0.45) score += 0.20;
        return clamp(score);
    }

    protected double readNumber(Map<String, Object> payload, String key, double fallback) {
        Object value = payload.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    protected double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    protected double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
