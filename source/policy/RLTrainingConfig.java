package policy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RLTrainingConfig {
    public String algorithm = "DQN";
    public int maxEpisodes = 0;
    public int maxStepsPerEpisode = 0;
    public double learningRate = 0.0;
    public double discountFactor = 0.99;
    public double explorationRate = 0.0;
    public int replayBufferSize = 0;
    public int batchSize = 0;
    public List<String> supportedAlgorithms = Arrays.asList("DQN", "PPO", "SAC");
    public Map<String, Object> hyperParameters = new LinkedHashMap<String, Object>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("algorithm", algorithm);
        map.put("maxEpisodes", maxEpisodes);
        map.put("maxStepsPerEpisode", maxStepsPerEpisode);
        map.put("learningRate", learningRate);
        map.put("discountFactor", discountFactor);
        map.put("explorationRate", explorationRate);
        map.put("replayBufferSize", replayBufferSize);
        map.put("batchSize", batchSize);
        map.put("supportedAlgorithms", supportedAlgorithms);
        map.put("hyperParameters", hyperParameters);
        map.put("trainingStatus", "config_only_no_rl_training");
        return map;
    }
}
