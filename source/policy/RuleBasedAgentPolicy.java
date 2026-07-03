package policy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rule baseline policy. It is intentionally simple and should only be used as a baseline.
 */
public class RuleBasedAgentPolicy implements AgentPolicy {
    private double highRiskThreshold = 0.70;
    private double lowConfidenceThreshold = 0.45;
    private String lastExplanation = "";

    public AgentAction decide(AgentState state) {
        if (state.messageConfidence < lowConfidenceThreshold || state.messageFreshness < 0.25) {
            lastExplanation = "Rule baseline waits because evidence confidence or freshness is too low.";
            return AgentAction.waitForMoreDDSMessages(lastExplanation);
        }
        if (state.safetyRisk >= highRiskThreshold || state.replanRequired) {
            lastExplanation = "Rule baseline requests replanning because risk or replan flag crossed threshold.";
            return AgentAction.requestReplanning(lastExplanation);
        }
        if (state.conflictStatus) {
            lastExplanation = "Rule baseline switches to safety priority when route advice conflicts.";
            return AgentAction.switchToSafetyPriority(lastExplanation);
        }
        String route = state.currentRoute == null || state.currentRoute.isBlank()
            ? firstCandidate(state) : state.currentRoute;
        lastExplanation = "Rule baseline chooses the current or first candidate route.";
        return AgentAction.chooseRoute(route, 0.55, lastExplanation);
    }

    public void configure(Map<String, Object> config) {
        if (config == null) return;
        highRiskThreshold = number(config, "highRiskThreshold", highRiskThreshold);
        lowConfidenceThreshold = number(config, "lowConfidenceThreshold", lowConfidenceThreshold);
    }

    public String explain(AgentState state, AgentAction action) {
        return lastExplanation;
    }

    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = AgentPolicy.super.evaluationResult(state, action, outcome);
        result.put("policyName", "RuleBasedAgentPolicy");
        result.put("policyRole", "baseline_only");
        result.put("configuration", configuration());
        return result;
    }

    public Map<String, Object> configuration() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("highRiskThreshold", highRiskThreshold);
        config.put("lowConfidenceThreshold", lowConfidenceThreshold);
        config.put("baselineOnly", true);
        return config;
    }

    private String firstCandidate(AgentState state) {
        return state.candidateRoutes.isEmpty() ? "" : state.candidateRoutes.get(0);
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }
}
