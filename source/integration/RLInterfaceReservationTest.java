package integration;

import policy.AgentAction;
import policy.AgentPolicy;
import policy.AgentState;
import policy.Episode;
import policy.HeuristicPolicy;
import policy.MARLTrainingConfig;
import policy.PolicyEnvironment;
import policy.PolicyTrainer;
import policy.RandomPolicy;
import policy.ReplayRecord;
import policy.RLTrainingConfig;
import policy.StepResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RLInterfaceReservationTest {
    public static void main(String[] args) {
        AgentState state = AgentState.builder("CAR-CQ-001")
            .passengerIntent("尽快去医院，优先安全，避开拥堵")
            .candidateRoutes(Arrays.asList("route_A_fast", "route_B_safe"))
            .currentRoute("route_A_fast")
            .safetyRisk(0.72)
            .congestionLevel(0.58)
            .messageConfidence(0.86)
            .messageFreshness(0.91)
            .sourceTrust(0.88)
            .conflictStatus(true)
            .emergencyLevel(0.84)
            .build();

        StepResult step = new StepResult(state, 7.5, false, mapOf("source", "interface-test"));
        require(step.toMap().containsKey("nextState"), "StepResult exposes nextState");
        require(number(step.toMap(), "reward") == 7.5, "StepResult exposes reward");

        AgentAction action = AgentAction.chooseRoute("route_B_safe", 0.91, "Safety route selected for replay record.");
        ReplayRecord record = new ReplayRecord("CAR-CQ-001", state, action, 7.5, state, false, mapOf("algorithm", "DQN"));
        Episode episode = new Episode("EP-INTERFACE-001", "hospital_emergency");
        episode.add(record);
        require(episode.records.size() == 1, "Episode stores replay records");
        require(Math.abs(episode.totalReward() - 7.5) < 0.0001, "Episode calculates total reward");
        require(String.valueOf(episode.metadata.get("trainingStatus")).contains("no_rl_training_data"),
            "Episode metadata does not claim training data exists");

        RLTrainingConfig rlConfig = new RLTrainingConfig();
        require(rlConfig.supportedAlgorithms.contains("DQN"), "RL config reserves DQN");
        require(rlConfig.supportedAlgorithms.contains("PPO"), "RL config reserves PPO");
        require(rlConfig.supportedAlgorithms.contains("SAC"), "RL config reserves SAC");
        require(String.valueOf(rlConfig.toMap().get("trainingStatus")).contains("no_rl_training"),
            "RL config is marked config-only");

        MARLTrainingConfig marlConfig = new MARLTrainingConfig();
        require(marlConfig.supportedAlgorithms.contains("MARL"), "MARL config reserves multi-agent training");
        require(marlConfig.agentIds.size() >= 2, "MARL config exposes multiple agent ids");
        require(String.valueOf(marlConfig.toMap().get("trainingStatus")).contains("no_marl_training"),
            "MARL config is marked config-only");

        RandomPolicy randomPolicy = new RandomPolicy(42L);
        AgentAction randomAction = randomPolicy.decide(state);
        Map<String, Object> randomEval = randomPolicy.evaluationResult(state, randomAction, mapOf("success", Boolean.TRUE));
        require(randomAction != null, "RandomPolicy baseline can select an action");
        require(String.valueOf(randomEval.get("trainingStatus")).contains("no_training"),
            "RandomPolicy baseline does not claim training");

        HeuristicPolicy heuristicPolicy = new HeuristicPolicy();
        AgentAction heuristicAction = heuristicPolicy.decide(state);
        Map<String, Object> heuristicEval = heuristicPolicy.evaluationResult(state, heuristicAction, mapOf("success", Boolean.TRUE));
        require(heuristicAction != null, "HeuristicPolicy baseline can select an action");
        require(String.valueOf(heuristicEval.get("trainingStatus")).contains("no_rl_training"),
            "HeuristicPolicy baseline does not claim RL training");

        StubEnvironment environment = new StubEnvironment(state);
        StubTrainer trainer = new StubTrainer();
        Map<String, Object> train = trainer.train(environment, rlConfig);
        Map<String, Object> trainMultiAgent = trainer.trainMultiAgent(environment, marlConfig);
        require(trainer.supportedAlgorithms().containsAll(Arrays.asList("DQN", "PPO", "SAC", "MARL")),
            "PolicyTrainer interface can advertise future algorithms");
        require(String.valueOf(train.get("trainingStatus")).contains("interface_only"),
            "PolicyTrainer train stub remains interface-only");
        require(String.valueOf(trainMultiAgent.get("trainingStatus")).contains("not_implemented"),
            "PolicyTrainer multi-agent default remains not implemented");
        require(trainer.evaluate(heuristicPolicy, environment).containsKey("reward"),
            "PolicyTrainer interface can evaluate a policy without training");

        System.out.println("[PASS] RL interface reservation test passed.");
    }

    private static class StubEnvironment implements PolicyEnvironment {
        private final AgentState state;
        private boolean terminal;

        StubEnvironment(AgentState state) {
            this.state = state;
        }

        public AgentState reset() {
            terminal = false;
            return state;
        }

        public StepResult step(AgentAction action) {
            terminal = true;
            return new StepResult(state, 8.0, true, mapOf("actionType", action.actionType));
        }

        public List<String> agentIds() {
            return Arrays.asList("CAR-CQ-001", "UAV-CQ-007", "RSU-SCHOOL-009");
        }

        public boolean isTerminal() {
            return terminal;
        }

        public Map<String, Object> metadata() {
            return mapOf("environmentStatus", "interface_only_no_real_training_environment");
        }
    }

    private static class StubTrainer implements PolicyTrainer {
        public String algorithmName() {
            return "InterfaceOnlyTrainer";
        }

        public List<String> supportedAlgorithms() {
            return Arrays.asList("DQN", "PPO", "SAC", "MARL");
        }

        public Map<String, Object> train(PolicyEnvironment environment, RLTrainingConfig config) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("algorithm", config.algorithm);
            result.put("trainingStatus", "interface_only_no_rl_training");
            result.put("environment", environment.metadata());
            result.put("config", config.toMap());
            return result;
        }

        public Map<String, Object> evaluate(AgentPolicy policy, PolicyEnvironment environment) {
            AgentState state = environment.reset();
            AgentAction action = policy.decide(state);
            return policy.evaluationResult(state, action, mapOf("success", Boolean.TRUE));
        }
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    private static double number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
