package policy;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentAction {
    public enum Type {
        CHOOSE_ROUTE,
        REJECT_ROUTE,
        ACCEPT_SUGGESTION,
        REJECT_SUGGESTION,
        REQUEST_REPLANNING,
        REQUEST_MORE_INFORMATION,
        PUBLISH_WARNING,
        WAIT_FOR_MORE_DDS_MESSAGES,
        SWITCH_TO_SAFETY_PRIORITY,
        SWITCH_TO_TIME_PRIORITY
    }

    public final String actionType;
    public final String route;
    public final Type type;
    public final double priority;
    public final String reason;
    public final String suggestionSource;
    public final boolean safetyPriority;
    public final boolean timePriority;

    public AgentAction(String actionType, String route) {
        this(actionType, route, 0.5, "");
    }

    public AgentAction(String actionType, String route, double priority, String reason) {
        this(parseType(actionType), route, "", priority, reason, false, false);
    }

    public AgentAction(Type type, String route, String suggestionSource, double priority, String reason,
                       boolean safetyPriority, boolean timePriority) {
        this.type = type == null ? Type.CHOOSE_ROUTE : type;
        this.actionType = this.type.name();
        this.route = route;
        this.suggestionSource = suggestionSource == null ? "" : suggestionSource;
        this.priority = priority;
        this.reason = reason == null ? "" : reason;
        this.safetyPriority = safetyPriority;
        this.timePriority = timePriority;
    }

    public static AgentAction chooseRoute(String route, double priority, String reason) {
        return new AgentAction(Type.CHOOSE_ROUTE, route, "", priority, reason, false, false);
    }

    public static AgentAction rejectRoute(String route, String reason) {
        return new AgentAction(Type.REJECT_ROUTE, route, "", 0.8, reason, false, false);
    }

    public static AgentAction acceptSuggestion(String source, String route, String reason) {
        return new AgentAction(Type.ACCEPT_SUGGESTION, route, source, 0.7, reason, false, false);
    }

    public static AgentAction rejectSuggestion(String source, String route, String reason) {
        return new AgentAction(Type.REJECT_SUGGESTION, route, source, 0.7, reason, false, false);
    }

    public static AgentAction requestReplanning(String reason) {
        return new AgentAction(Type.REQUEST_REPLANNING, "", "", 0.9, reason, false, false);
    }

    public static AgentAction requestMoreInformation(String reason) {
        return new AgentAction(Type.REQUEST_MORE_INFORMATION, "", "", 0.55, reason, false, false);
    }

    public static AgentAction publishWarning(String reason) {
        return new AgentAction(Type.PUBLISH_WARNING, "", "", 0.85, reason, false, false);
    }

    public static AgentAction waitForMoreDDSMessages(String reason) {
        return new AgentAction(Type.WAIT_FOR_MORE_DDS_MESSAGES, "", "", 0.45, reason, false, false);
    }

    public static AgentAction switchToSafetyPriority(String reason) {
        return new AgentAction(Type.SWITCH_TO_SAFETY_PRIORITY, "", "", 0.88, reason, true, false);
    }

    public static AgentAction switchToTimePriority(String reason) {
        return new AgentAction(Type.SWITCH_TO_TIME_PRIORITY, "", "", 0.72, reason, false, true);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("actionType", actionType);
        map.put("type", type.name());
        map.put("route", route);
        map.put("suggestionSource", suggestionSource);
        map.put("priority", priority);
        map.put("reason", reason);
        map.put("safetyPriority", safetyPriority);
        map.put("timePriority", timePriority);
        return map;
    }

    private static Type parseType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return Type.CHOOSE_ROUTE;
        }
        String normalized = actionType.trim().toUpperCase();
        if ("REPLAN".equals(normalized) || "REQUEST_REPLAN".equals(normalized)) return Type.REQUEST_REPLANNING;
        if ("PUBLISH_ADVICE".equals(normalized)) return Type.CHOOSE_ROUTE;
        if ("HOLD".equals(normalized)) return Type.WAIT_FOR_MORE_DDS_MESSAGES;
        try {
            return Type.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Type.CHOOSE_ROUTE;
        }
    }
}
