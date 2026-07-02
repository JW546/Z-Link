package policy;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReplayRecord {
    public final String agentId;
    public final AgentState state;
    public final AgentAction action;
    public final double reward;
    public final AgentState nextState;
    public final boolean done;
    public final Map<String, Object> info;

    public ReplayRecord(String agentId, AgentState state, AgentAction action, double reward,
                        AgentState nextState, boolean done, Map<String, Object> info) {
        this.agentId = agentId == null || agentId.isBlank() ? "AGENT-UNKNOWN" : agentId;
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
        this.info = info == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(info);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("agentId", agentId);
        map.put("state", state == null ? new LinkedHashMap<String, Object>() : state.toMap());
        map.put("action", action == null ? new LinkedHashMap<String, Object>() : action.toMap());
        map.put("reward", reward);
        map.put("nextState", nextState == null ? new LinkedHashMap<String, Object>() : nextState.toMap());
        map.put("done", done);
        map.put("info", info);
        return map;
    }
}
