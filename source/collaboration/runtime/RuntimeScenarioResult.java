package collaboration.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuntimeScenarioResult {
    public final List<Map<String, Object>> history;
    public final RuntimeMessage conflictEvent;
    public final RuntimeMessage finalDecision;

    public RuntimeScenarioResult(List<Map<String, Object>> history, RuntimeMessage conflictEvent, RuntimeMessage finalDecision) {
        this.history = history == null ? new ArrayList<Map<String, Object>>() : history;
        this.conflictEvent = conflictEvent;
        this.finalDecision = finalDecision;
    }
}
