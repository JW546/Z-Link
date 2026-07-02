package simulation;

import agent.FaultInjectionService;
import agent.ScenarioLibrary;
import dds.DDSMessage;
import dds.DDSQoS;
import decision.ConflictResolver;
import evaluation.EvaluationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario and disturbance seam for simulation runs.
 */
public class SimulationScenarioService {
    private final ScenarioLibrary scenarios = ScenarioLibrary.getInstance();
    private final FaultInjectionService faults = FaultInjectionService.getInstance();
    private final ConflictResolver conflictResolver = new ConflictResolver();
    private final EvaluationService evaluationService = new EvaluationService();

    public List<Map<String, Object>> listScenarios() {
        return scenarios.list();
    }

    public Map<String, Object> getScenario(String id) {
        return scenarios.get(id);
    }

    public Map<String, Object> runScenario(String id) {
        Map<String, Object> scenario = getScenario(id);
        String passengerIntent = String.valueOf(scenario.getOrDefault("passengerIntent", ""));
        List<DDSMessage> messages = toDDSMessages(scenario);
        List<Map<String, Object>> ddsTimeline = new ArrayList<Map<String, Object>>();
        for (DDSMessage message : messages) {
            Map<String, Object> event = new LinkedHashMap<String, Object>();
            event.put("topic", message.topic);
            event.put("sourceId", message.sourceId);
            event.put("timestamp", message.timestamp);
            event.put("confidence", message.confidence);
            event.put("freshness", message.messageFreshness());
            event.put("payload", message.payload);
            ddsTimeline.add(event);
        }

        Map<String, Object> finalDecision = conflictResolver.resolveMessages(messages, passengerIntent);
        Map<String, Object> evaluation = evaluationService.evaluateScenario(scenario, finalDecision);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scenarioId", scenario.get("scenarioId"));
        result.put("passengerIntent", passengerIntent);
        result.put("ddsTimeline", ddsTimeline);
        result.put("finalDecision", finalDecision);
        result.put("evaluation", evaluation);
        result.put("expectedConflicts", scenario.get("expectedConflicts"));
        result.put("expectedDecisionPrinciple", scenario.get("expectedDecisionPrinciple"));
        return result;
    }

    public void injectMessageConflict(String target) {
        faults.inject("MESSAGE_CONFLICT", target == null ? "RouteAdviceTopic" : target, "simulation conflict trigger");
    }

    public boolean shouldTriggerReplan(double congestionLevel, boolean messageConflict) {
        return congestionLevel > 0.70 || messageConflict;
    }

    @SuppressWarnings("unchecked")
    private List<DDSMessage> toDDSMessages(Map<String, Object> scenario) {
        List<DDSMessage> messages = new ArrayList<DDSMessage>();
        Object rawMessages = scenario.get("ddsMessages");
        if (!(rawMessages instanceof List<?>)) {
            return messages;
        }
        for (Object raw : (List<?>) rawMessages) {
            if (!(raw instanceof Map<?, ?>)) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>((Map<String, Object>) raw);
            String topic = String.valueOf(item.remove("topic"));
            String sourceId = String.valueOf(item.remove("sourceId"));
            double confidence = number(item, "confidence", 0.80);
            long delayMs = (long) number(item, "ddsDelayMs", 0.0);
            double packetLoss = number(item, "packetLossRate", 0.0);
            DDSQoS qos = DDSQoS.reliable()
                .withDeliveryDelayMs(delayMs)
                .withDropProbability(packetLoss)
                .withTimeToLiveMs(delayMs > 1_000L ? 800L : 5_000L);
            long timestamp = System.currentTimeMillis() - delayMs;
            messages.add(new DDSMessage(topic, sourceId, "ALL", item, qos, confidence, timestamp));
        }
        return messages;
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }
}
