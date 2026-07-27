package collaboration.ros2;

import collaboration.runtime.RuntimeScenarioResult;

import java.util.List;

public class Ros2BidirectionalClosedLoopDemoMain {
    public static void main(String[] args) throws Exception {
        long timeoutMs = args.length > 0 ? Long.parseLong(args[0]) : 5_000L;
        boolean dryRun = args.length > 1 && "dry-run".equalsIgnoreCase(args[1]);
        CapturingExecutor capture = new CapturingExecutor();
        Ros2CommandExecutor executor = dryRun ? capture : new ProcessRos2CommandExecutor();
        RuntimeScenarioResult result = new Ros2BidirectionalClosedLoopScenario(executor).run(timeoutMs);

        System.out.println("[ROS2-BIDIR] localMessages=" + result.history.size());
        System.out.println("[ROS2-BIDIR] finalDecision=" + (result.finalDecision == null ? "{}" : result.finalDecision.payload));
        if (dryRun) {
            System.out.println("[ROS2-BIDIR] dryRunForwarded=" + capture.commands.size());
            for (List<String> command : capture.commands) {
                if (command.contains("/coordination/final_decision")) {
                    System.out.println("[ROS2-BIDIR] outboundFinalDecisionCommand=" + command);
                }
            }
        }
        if (result.finalDecision == null) {
            throw new IllegalStateException("ROS2 bidirectional closed-loop did not produce final decision.");
        }
    }

    private static class CapturingExecutor implements Ros2CommandExecutor {
        private final java.util.List<java.util.List<String>> commands = new java.util.ArrayList<java.util.List<String>>();

        public int execute(List<String> command) {
            commands.add(new java.util.ArrayList<String>(command));
            return 0;
        }
    }
}
