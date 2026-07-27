package policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteTrainingActionSpace {
    public static List<String> keys() {
        return Arrays.asList(
            "choose_route_A",
            "choose_route_C",
            "choose_route_D",
            "request_replanning",
            "switch_safety",
            "switch_time",
            "wait_dds"
        );
    }

    public static AgentAction action(String key) {
        if ("choose_route_A".equals(key)) {
            return AgentAction.chooseRoute("route_A_main_road", 0.62, "Q-learning selected route A from state-action value.");
        }
        if ("choose_route_C".equals(key)) {
            return AgentAction.chooseRoute("route_C_elevated_detour", 0.72, "Q-learning selected safer elevated detour from DDS evidence.");
        }
        if ("choose_route_D".equals(key)) {
            return AgentAction.chooseRoute("route_D_emergency_green_wave", 0.78, "Q-learning selected emergency green-wave route from urgency and risk.");
        }
        if ("request_replanning".equals(key)) {
            return AgentAction.requestReplanning("Q-learning requests dynamic replanning under conflict or high risk.");
        }
        if ("switch_safety".equals(key)) {
            return AgentAction.switchToSafetyPriority("Q-learning switches to safety priority under trusted risk evidence.");
        }
        if ("switch_time".equals(key)) {
            return AgentAction.switchToTimePriority("Q-learning switches to time priority when urgency dominates safely.");
        }
        return AgentAction.waitForMoreDDSMessages("Q-learning waits for more DDS messages when evidence quality is weak.");
    }

    public static String key(AgentAction action) {
        if (action == null) return "wait_dds";
        switch (action.type) {
            case REQUEST_REPLANNING:
                return "request_replanning";
            case SWITCH_TO_SAFETY_PRIORITY:
                return "switch_safety";
            case SWITCH_TO_TIME_PRIORITY:
                return "switch_time";
            case WAIT_FOR_MORE_DDS_MESSAGES:
            case REQUEST_MORE_INFORMATION:
                return "wait_dds";
            case CHOOSE_ROUTE:
            default:
                if ("route_D_emergency_green_wave".equals(action.route)) return "choose_route_D";
                if ("route_C_elevated_detour".equals(action.route)) return "choose_route_C";
                return "choose_route_A";
        }
    }

    public static List<String> documentation() {
        List<String> docs = new ArrayList<String>();
        for (String key : keys()) {
            docs.add(key + " -> " + action(key).toMap());
        }
        return docs;
    }
}
