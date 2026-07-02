package policy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomPolicy implements AgentPolicy {
    private final Random random;
    private String lastExplanation = "";

    public RandomPolicy() {
        this(20260630L);
    }

    public RandomPolicy(long seed) {
        this.random = new Random(seed);
    }

    public AgentAction decide(AgentState state) {
        List<AgentAction> actions = Arrays.asList(
            AgentAction.chooseRoute(firstRoute(state), 0.30, "Random baseline chose a route without learned value estimates."),
            AgentAction.requestReplanning("Random baseline sampled replanning action."),
            AgentAction.waitForMoreDDSMessages("Random baseline sampled waiting action."),
            AgentAction.switchToSafetyPriority("Random baseline sampled safety-priority action."),
            AgentAction.switchToTimePriority("Random baseline sampled time-priority action.")
        );
        AgentAction action = actions.get(random.nextInt(actions.size()));
        lastExplanation = action.reason;
        return action;
    }

    public String explain(AgentState state, AgentAction action) {
        return lastExplanation;
    }

    public Map<String, Object> evaluationResult(AgentState state, AgentAction action, Map<String, Object> outcome) {
        Map<String, Object> result = AgentPolicy.super.evaluationResult(state, action, outcome);
        result.put("policyName", "RandomPolicy");
        result.put("policyRole", "baseline_only");
        result.put("trainingStatus", "random_baseline_no_training");
        return result;
    }

    private String firstRoute(AgentState state) {
        if (state == null || state.candidateRoutes.isEmpty()) return "route_A_main_road";
        return state.candidateRoutes.get(0);
    }
}
