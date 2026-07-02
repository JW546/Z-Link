package policy;

import java.util.List;
import java.util.Map;

public interface PolicyEnvironment {
    AgentState reset();

    StepResult step(AgentAction action);

    default Map<String, AgentState> resetMultiAgent() {
        return java.util.Collections.emptyMap();
    }

    default Map<String, StepResult> stepMultiAgent(Map<String, AgentAction> actions) {
        return java.util.Collections.emptyMap();
    }

    List<String> agentIds();

    boolean isTerminal();

    Map<String, Object> metadata();
}
