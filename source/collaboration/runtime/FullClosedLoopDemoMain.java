package collaboration.runtime;

import com.google.gson.Gson;

import java.util.Map;

public class FullClosedLoopDemoMain {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Exception {
        long timeoutMs = args.length > 0 ? Long.parseLong(args[0]) : 5_000L;
        RuntimeScenarioResult result = new FullClosedLoopScenario().run(timeoutMs);
        System.out.println("[CLOSED-LOOP] messages=" + result.history.size());
        for (Map<String, Object> item : result.history) {
            String topic = String.valueOf(item.get("topic"));
            if (LocalDDSBroker.TOPIC_PROTOCOL_FRAME.equals(topic)
                || LocalDDSBroker.TOPIC_CONFLICT_EVENT.equals(topic)
                || LocalDDSBroker.TOPIC_REPLAN_REQUEST.equals(topic)
                || LocalDDSBroker.TOPIC_FINAL_DECISION.equals(topic)
                || LocalDDSBroker.TOPIC_POLICY_EVALUATION.equals(topic)) {
                System.out.println("[CLOSED-LOOP] " + topic + " source=" + item.get("sourceId")
                    + " payload=" + GSON.toJson(item.get("payload")));
            }
        }
        if (result.finalDecision == null) {
            throw new IllegalStateException("Closed-loop scenario did not produce final decision.");
        }
        System.out.println("[CLOSED-LOOP] finalDecision=" + GSON.toJson(result.finalDecision.payload));
    }
}
