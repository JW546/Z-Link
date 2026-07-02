package policy;

import java.util.LinkedHashMap;
import java.util.Map;

public class StepResult {
    public final AgentState nextState;
    public final double reward;
    public final boolean done;
    public final Map<String, Object> info;

    public StepResult(AgentState nextState, double reward, boolean done, Map<String, Object> info) {
        this.nextState = nextState;
        this.reward = reward;
        this.done = done;
        this.info = info == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(info);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("nextState", nextState == null ? new LinkedHashMap<String, Object>() : nextState.toMap());
        map.put("reward", reward);
        map.put("done", done);
        map.put("info", info);
        return map;
    }
}
