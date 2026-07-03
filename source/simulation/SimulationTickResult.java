package simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Observable result produced after one virtual simulation tick.
 */
public class SimulationTickResult {
    public final int tick;
    public final Map<String, Object> worldState;
    public final List<Map<String, Object>> ddsMessages;
    public final List<Map<String, Object>> agentAdvice;
    public final Map<String, Object> plannerResult;
    public final Map<String, Object> conflictResolution;
    public final Map<String, Object> finalDecision;

    public SimulationTickResult(int tick,
                                Map<String, Object> worldState,
                                List<Map<String, Object>> ddsMessages,
                                List<Map<String, Object>> agentAdvice,
                                Map<String, Object> plannerResult,
                                Map<String, Object> conflictResolution,
                                Map<String, Object> finalDecision) {
        this.tick = tick;
        this.worldState = worldState;
        this.ddsMessages = ddsMessages;
        this.agentAdvice = agentAdvice;
        this.plannerResult = plannerResult;
        this.conflictResolution = conflictResolution;
        this.finalDecision = finalDecision;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("tick", tick);
        map.put("worldState", worldState);
        map.put("ddsMessages", new ArrayList<Map<String, Object>>(ddsMessages));
        map.put("agentAdvice", new ArrayList<Map<String, Object>>(agentAdvice));
        map.put("plannerResult", plannerResult);
        map.put("conflictResolution", conflictResolution);
        map.put("finalDecision", finalDecision);
        return map;
    }
}
