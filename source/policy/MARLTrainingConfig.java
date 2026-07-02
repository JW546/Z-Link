package policy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MARLTrainingConfig extends RLTrainingConfig {
    public String coordinationMode = "centralized_training_decentralized_execution";
    public List<String> agentIds = Arrays.asList("CAR-CQ-001", "UAV-CQ-007", "RSU-SCHOOL-009", "PEER-CAR-031");
    public boolean sharedPolicy;
    public boolean centralizedCritic;
    public Map<String, Object> communicationConfig = new LinkedHashMap<String, Object>();

    public MARLTrainingConfig() {
        algorithm = "MARL";
        supportedAlgorithms = Arrays.asList("DQN", "PPO", "SAC", "MARL");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("coordinationMode", coordinationMode);
        map.put("agentIds", agentIds);
        map.put("sharedPolicy", sharedPolicy);
        map.put("centralizedCritic", centralizedCritic);
        map.put("communicationConfig", communicationConfig);
        map.put("trainingStatus", "config_only_no_marl_training");
        return map;
    }
}
