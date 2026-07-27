package policy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteTrainingService {
    public Map<String, Object> runExperiment(int episodes) {
        int safeEpisodes = episodes <= 0 ? 120 : episodes;
        RouteTrainingEnvironment environment = new RouteTrainingEnvironment();
        TabularQLearningTrainer trainer = new TabularQLearningTrainer();
        RLTrainingConfig config = new RLTrainingConfig();
        config.algorithm = "TABULAR_Q_LEARNING";
        config.maxEpisodes = safeEpisodes;
        config.maxStepsPerEpisode = 4;
        config.learningRate = 0.28;
        config.discountFactor = 0.90;
        config.explorationRate = 0.22;

        Map<String, Object> training = trainer.train(environment, config);
        Map<String, Object> comparison = compare(trainer);

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("trainingStatus", training.get("trainingStatus"));
        report.put("algorithm", training.get("algorithm"));
        report.put("episodes", safeEpisodes);
        report.put("environment", environment.metadata());
        report.put("stateEncoding", RouteTrainingStateEncoder.documentation());
        report.put("actionSpace", RouteTrainingActionSpace.documentation());
        report.put("rewardDesign", rewardDesign());
        report.put("trainingMetrics", training);
        report.put("learnedPolicy", training.get("learnedPolicy"));
        report.put("baselineComparison", comparison);
        report.put("howToUse", "Use LearnedRoutePolicy as a candidate policy in simulation only; keep safety constraints and rule baseline as guards before any real DDS/vehicle use.");
        report.put("limitations", "This is small tabular simulation training, not DQN/PPO, not large-scale data training, and not validated on real vehicles.");
        return report;
    }

    private Map<String, Object> compare(TabularQLearningTrainer trainer) {
        Map<String, Object> learned = trainer.evaluate(trainer.learnedPolicy(), new RouteTrainingEnvironment(20260713L, 4));
        Map<String, Object> random = trainer.evaluate(new RandomPolicy(20260713L), new RouteTrainingEnvironment(20260713L, 4));
        Map<String, Object> rule = trainer.evaluate(new RuleBasedAgentPolicy(), new RouteTrainingEnvironment(20260713L, 4));

        Map<String, Object> comparison = new LinkedHashMap<String, Object>();
        comparison.put("learnedAverageReward", learned.get("averageReward"));
        comparison.put("randomAverageReward", random.get("averageReward"));
        comparison.put("ruleBaselineAverageReward", rule.get("averageReward"));
        comparison.put("learnedPolicyStatus", "candidate_policy_simulation_only");
        comparison.put("baselineRole", "RandomPolicy and RuleBasedAgentPolicy remain baselines");
        return comparison;
    }

    private Map<String, Object> rewardDesign() {
        Map<String, Object> reward = new LinkedHashMap<String, Object>();
        reward.put("positive", Arrays.asList("arrived", "conflict handled", "complete explanation", "safe replanning"));
        reward.put("negative", Arrays.asList("high safety risk", "congestion exposure", "trusted low-confidence DDS blindly", "ignored high-trust risk", "excess replanning"));
        reward.put("hardSafetyPenalty", "safetyRisk>=0.80 without safety/replan/reject action is strongly penalized");
        return reward;
    }
}
