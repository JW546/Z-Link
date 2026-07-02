package policy;

import java.util.List;
import java.util.Map;

public interface PolicyTrainer {
    String algorithmName();

    List<String> supportedAlgorithms();

    Map<String, Object> train(PolicyEnvironment environment, RLTrainingConfig config);

    default Map<String, Object> trainMultiAgent(PolicyEnvironment environment, MARLTrainingConfig config) {
        Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("algorithm", algorithmName());
        result.put("supportedAlgorithms", supportedAlgorithms());
        result.put("trainingStatus", "interface_only_marl_training_not_implemented");
        result.put("config", config == null ? new java.util.LinkedHashMap<String, Object>() : config.toMap());
        return result;
    }

    Map<String, Object> evaluate(AgentPolicy policy, PolicyEnvironment environment);
}
