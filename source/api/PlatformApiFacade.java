package api;

import agent.CollaborationDecisionEngine;
import agent.PlatformOrchestrator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin API-facing facade. Keeps HTTP handlers away from platform orchestration details.
 */
public class PlatformApiFacade {
    private final CollaborationDecisionEngine collaborationDecisionEngine;
    private final PlatformOrchestrator platformOrchestrator;

    public PlatformApiFacade() {
        this(new CollaborationDecisionEngine(), new PlatformOrchestrator());
    }

    public PlatformApiFacade(CollaborationDecisionEngine collaborationDecisionEngine,
                             PlatformOrchestrator platformOrchestrator) {
        this.collaborationDecisionEngine = collaborationDecisionEngine;
        this.platformOrchestrator = platformOrchestrator;
    }

    public Map<String, Object> generatePlan(String text, String missionId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("collaboration", collaborationDecisionEngine.evaluate(text, missionId));
        result.put("platformPlan", platformOrchestrator.buildFullPlan(text, missionId));
        return result;
    }

    public Map<String, Object> simulate(String text, String missionId) {
        return platformOrchestrator.simulate(text, missionId);
    }
}
