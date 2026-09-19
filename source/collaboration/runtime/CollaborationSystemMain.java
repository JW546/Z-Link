package collaboration.runtime;

import java.util.Map;

public class CollaborationSystemMain {
    public static void main(String[] args) throws Exception {
        long intervalMs = args.length > 0 ? Long.parseLong(args[0]) : 500L;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        RuntimeScenarioResult result = new LocalRuntimeScenario().run(intervalMs, ticks);
        System.out.println("[CollaborationSystem] messages=" + result.history.size());
        for (Map<String, Object> item : result.history) {
            System.out.println(item);
        }
        if (result.finalDecision == null) {
            throw new IllegalStateException("No final decision was produced by coordinator.");
        }
    }
}
