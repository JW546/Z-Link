package collaboration.ros2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ros2BridgePlan {
    public final boolean ros2Available;
    public final String ros2Executable;
    public final String runtimeMode;
    public final List<Map<String, Object>> topicBindings;
    public final List<Map<String, Object>> publishCommands;
    public final String fallbackMode;

    public Ros2BridgePlan(boolean ros2Available, String ros2Executable, String runtimeMode,
                          List<Map<String, Object>> topicBindings,
                          List<Map<String, Object>> publishCommands,
                          String fallbackMode) {
        this.ros2Available = ros2Available;
        this.ros2Executable = ros2Executable;
        this.runtimeMode = runtimeMode;
        this.topicBindings = topicBindings == null ? new ArrayList<Map<String, Object>>() : topicBindings;
        this.publishCommands = publishCommands == null ? new ArrayList<Map<String, Object>>() : publishCommands;
        this.fallbackMode = fallbackMode;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("ros2Available", ros2Available);
        map.put("ros2Executable", ros2Executable);
        map.put("runtimeMode", runtimeMode);
        map.put("topicBindings", topicBindings);
        map.put("publishCommands", publishCommands);
        map.put("fallbackMode", fallbackMode);
        return map;
    }
}
