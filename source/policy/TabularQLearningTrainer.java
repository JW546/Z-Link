package policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TabularQLearningTrainer implements PolicyTrainer {
    private final Random random;
    private final Map<String, Map<String, Double>> qTable = new LinkedHashMap<String, Map<String, Double>>();
    private final List<ReplayRecord> replay = new ArrayList<ReplayRecord>();

    public TabularQLearningTrainer() {
        this(20260712L);
    }

    public TabularQLearningTrainer(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public String algorithmName() {
        return "TabularQLearning";
    }

    @Override
    public List<String> supportedAlgorithms() {
        return Arrays.asList("TABULAR_Q_LEARNING");
    }

    @Override
    public Map<String, Object> train(PolicyEnvironment environment, RLTrainingConfig config) {
        RLTrainingConfig cfg = defaults(config);
        double totalReward = 0.0;
        int steps = 0;
        replay.clear();
        qTable.clear();

        for (int episode = 0; episode < cfg.maxEpisodes; episode++) {
            AgentState state = environment.reset();
            for (int step = 0; step < cfg.maxStepsPerEpisode; step++) {
                String stateKey = RouteTrainingStateEncoder.encode(state);
                String actionKey = chooseAction(stateKey, cfg.explorationRate);
                AgentAction action = RouteTrainingActionSpace.action(actionKey);
                StepResult result = environment.step(action);
                String nextKey = RouteTrainingStateEncoder.encode(result.nextState);
                update(stateKey, actionKey, result.reward, nextKey, cfg.learningRate, cfg.discountFactor);
                replay.add(new ReplayRecord(state.agentId, state, action, result.reward, result.nextState, result.done, result.info));
                totalReward += result.reward;
                steps++;
                state = result.nextState;
                if (result.done) {
                    break;
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("algorithm", algorithmName());
        report.put("trainingStatus", "tabular_q_learning_simulation_training_complete");
        report.put("episodes", cfg.maxEpisodes);
        report.put("steps", steps);
        report.put("averageTrainingReward", round(steps == 0 ? 0.0 : totalReward / steps));
        report.put("qTableSize", qTable.size());
        report.put("replaySize", replay.size());
        report.put("config", cfg.toMap());
        report.put("learnedPolicy", exportPolicy());
        report.put("limitations", "small tabular simulation prototype; not DQN/PPO, not real-vehicle or large-scale MARL training");
        return report;
    }

    @Override
    public Map<String, Object> evaluate(AgentPolicy policy, PolicyEnvironment environment) {
        AgentPolicy safePolicy = policy == null ? new LearnedRoutePolicy(qTable) : policy;
        double totalReward = 0.0;
        int episodes = 30;
        int steps = 0;
        for (int episode = 0; episode < episodes; episode++) {
            AgentState state = environment.reset();
            for (int i = 0; i < 4; i++) {
                AgentAction action = safePolicy.decide(state);
                StepResult result = environment.step(action);
                totalReward += result.reward;
                steps++;
                state = result.nextState;
                if (result.done) {
                    break;
                }
            }
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("episodes", episodes);
        map.put("steps", steps);
        map.put("averageReward", round(steps == 0 ? 0.0 : totalReward / steps));
        return map;
    }

    public AgentPolicy learnedPolicy() {
        return new LearnedRoutePolicy(qTable);
    }

    public List<Map<String, Object>> exportPolicy() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Map<String, Double>> entry : qTable.entrySet()) {
            String action = bestAction(entry.getKey());
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("state", entry.getKey());
            row.put("action", action);
            row.put("qValue", round(value(entry.getKey(), action)));
            rows.add(row);
        }
        return rows;
    }

    private void update(String stateKey, String actionKey, double reward, String nextKey,
                        double learningRate, double discountFactor) {
        double current = value(stateKey, actionKey);
        double target = reward + discountFactor * value(nextKey, bestAction(nextKey));
        putValue(stateKey, actionKey, current + learningRate * (target - current));
    }

    private String chooseAction(String stateKey, double explorationRate) {
        ensureState(stateKey);
        if (random.nextDouble() < explorationRate) {
            List<String> actions = RouteTrainingActionSpace.keys();
            return actions.get(random.nextInt(actions.size()));
        }
        return bestAction(stateKey);
    }

    private String bestAction(String stateKey) {
        ensureState(stateKey);
        String best = RouteTrainingActionSpace.keys().get(0);
        double bestValue = Double.NEGATIVE_INFINITY;
        for (String action : RouteTrainingActionSpace.keys()) {
            double value = value(stateKey, action);
            if (value > bestValue) {
                bestValue = value;
                best = action;
            }
        }
        return best;
    }

    private void ensureState(String stateKey) {
        if (qTable.containsKey(stateKey)) {
            return;
        }
        Map<String, Double> values = new LinkedHashMap<String, Double>();
        for (String action : RouteTrainingActionSpace.keys()) {
            values.put(action, 0.0);
        }
        qTable.put(stateKey, values);
    }

    private double value(String stateKey, String actionKey) {
        ensureState(stateKey);
        Double value = qTable.get(stateKey).get(actionKey);
        return value == null ? 0.0 : value;
    }

    private void putValue(String stateKey, String actionKey, double value) {
        ensureState(stateKey);
        qTable.get(stateKey).put(actionKey, value);
    }

    private RLTrainingConfig defaults(RLTrainingConfig config) {
        RLTrainingConfig cfg = config == null ? new RLTrainingConfig() : config;
        cfg.algorithm = "TABULAR_Q_LEARNING";
        if (cfg.maxEpisodes <= 0) cfg.maxEpisodes = 120;
        if (cfg.maxStepsPerEpisode <= 0) cfg.maxStepsPerEpisode = 4;
        if (cfg.learningRate <= 0.0) cfg.learningRate = 0.28;
        if (cfg.discountFactor <= 0.0) cfg.discountFactor = 0.90;
        if (cfg.explorationRate <= 0.0) cfg.explorationRate = 0.22;
        cfg.supportedAlgorithms = supportedAlgorithms();
        return cfg;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
