package policy;

public interface RewardFunction {
    double reward(AgentState state, AgentAction action, double outcomeRisk, double arrivalMinutes);
}
