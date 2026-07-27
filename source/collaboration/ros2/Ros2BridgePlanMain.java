package collaboration.ros2;

import com.google.gson.Gson;
import collaboration.runtime.LocalRuntimeScenario;
import collaboration.runtime.RuntimeScenarioResult;

import java.util.List;
import java.util.Map;

public class Ros2BridgePlanMain {
    public static void main(String[] args) throws Exception {
        long intervalMs = args.length > 0 ? Long.parseLong(args[0]) : 500L;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int maxCommands = args.length > 2 ? Integer.parseInt(args[2]) : 12;
        RuntimeScenarioResult runtime = new LocalRuntimeScenario().run(intervalMs, ticks);
        Ros2BridgePlan plan = new Ros2DdsAdapter().planBridge(runtime.history);
        Map<String, Object> map = plan.toMap();
        List<Map<String, Object>> commands = plan.publishCommands;
        if (commands.size() > maxCommands) {
            map.put("publishCommands", commands.subList(0, maxCommands));
            map.put("truncated", true);
            map.put("totalCommands", commands.size());
        }
        System.out.println(new Gson().toJson(map));
    }
}
