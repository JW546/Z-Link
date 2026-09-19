package collaboration.ros2;

import collaboration.runtime.LocalDDSBroker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter seam between the project's unified runtime messages and ROS 2/DDS.
 * It can dry-run without ROS 2 installed and can later execute ros2 CLI commands.
 */
public class Ros2DdsAdapter {
    private final String ros2Executable;
    private final boolean customMessages;

    public Ros2DdsAdapter() {
        this(findRos2Executable(), customMessagesFromEnv());
    }

    public Ros2DdsAdapter(String ros2Executable) {
        this(ros2Executable, false);
    }

    public Ros2DdsAdapter(String ros2Executable, boolean customMessages) {
        this.ros2Executable = ros2Executable == null ? "" : ros2Executable;
        this.customMessages = customMessages;
    }

    public boolean isAvailable() {
        return !ros2Executable.isBlank();
    }

    public String executable() {
        return ros2Executable;
    }

    public boolean usesCustomMessages() {
        return customMessages;
    }

    public List<Ros2TopicBinding> topicBindings() {
        return Arrays.asList(
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_INTENT_CONTRACT, "/intent/contract",
                messageType("RuntimeIntentContract"), "urban::RuntimeIntentContract", "RELIABLE_TRANSIENT_LOCAL", "pub",
                "Natural-language intent and DSL task contract"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_AGENT_STATUS, "/agent/status",
                messageType("RuntimeAgentStatus"), "urban::RuntimeAgentStatus", "BEST_EFFORT_SENSOR_STREAM", "pub/sub",
                "Periodic UAV, ego-car, and peer-car state"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_PROTOCOL_FRAME, "/protocol/native_frame",
                messageType("ProtocolNativeFrame"), "urban::ProtocolNativeFrame", "RELIABLE_KEEP_LAST", "pub/sub",
                "Native vendor protocol frame emitted by Adapter"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_CONFLICT_EVENT, "/coordination/conflict_event",
                messageType("ConflictEvent"), "urban::ConflictEvent", "RELIABLE_LOW_LATENCY", "pub/sub",
                "Event-triggered conflict message"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_REPLAN_REQUEST, "/coordination/replan_request",
                messageType("ReplanningRequest"), "urban::ReplanningRequest", "RELIABLE_LOW_LATENCY", "pub/sub",
                "Dynamic replanning request"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_FINAL_DECISION, "/coordination/final_decision",
                messageType("RouteDecision"), "urban::RouteDecision", "RELIABLE_TRANSIENT_LOCAL", "pub/sub",
                "Explainable final route decision"),
            new Ros2TopicBinding(LocalDDSBroker.TOPIC_POLICY_EVALUATION, "/coordination/policy_evaluation",
                messageType("PolicyEvaluationReport"), "urban::PolicyEvaluationReport", "RELIABLE_KEEP_LAST", "pub/sub",
                "Baseline and cooperative policy comparison")
        );
    }

    public Ros2BridgePlan planBridge(List<Map<String, Object>> runtimeHistory) {
        List<Map<String, Object>> bindings = new ArrayList<Map<String, Object>>();
        for (Ros2TopicBinding binding : topicBindings()) {
            bindings.add(binding.toMap());
        }
        List<Map<String, Object>> commands = new ArrayList<Map<String, Object>>();
        if (runtimeHistory != null) {
            for (Map<String, Object> message : runtimeHistory) {
                commands.add(toPublishCommand(message));
            }
        }
        String mode = isAvailable() ? "ros2_cli_ready" : "dry_run_no_ros2";
        return new Ros2BridgePlan(isAvailable(), ros2Executable, mode, bindings, commands,
            "local_tcp_dds_compatible_runtime");
    }

    public Map<String, Object> toPublishCommand(Map<String, Object> runtimeMessage) {
        Map<String, Object> command = new LinkedHashMap<String, Object>();
        String semanticTopic = String.valueOf(runtimeMessage.get("topic"));
        Ros2TopicBinding binding = bindingFor(semanticTopic);
        String payload = escapeForRos2String(ros2SafePayload(runtimeMessage));
        command.put("semanticTopic", semanticTopic);
        command.put("rosTopic", binding.rosTopic);
        command.put("messageType", binding.messageType);
        command.put("idlType", binding.idlType);
        command.put("qosProfile", binding.qosProfile);
        command.put("dryRun", !isAvailable());
        command.put("messageMode", customMessages ? "custom_ros2_messages" : "std_msgs_string");
        command.put("command", Arrays.asList(
            ros2Executable.isBlank() ? "ros2" : ros2Executable,
            "topic",
            "pub",
            "--once",
            "--wait-matching-subscriptions",
            "0",
            "--keep-alive",
            "2.0",
            binding.rosTopic,
            binding.messageType,
            customMessages ? customPayload(binding, runtimeMessage) : "{data: \"" + payload + "\"}"
        ));
        return command;
    }

    private String messageType(String name) {
        return customMessages ? "urban_runtime_msgs/msg/" + name : "std_msgs/msg/String";
    }

    private Ros2TopicBinding bindingFor(String semanticTopic) {
        for (Ros2TopicBinding binding : topicBindings()) {
            if (binding.semanticTopic.equals(semanticTopic)) {
                return binding;
            }
        }
        return new Ros2TopicBinding(semanticTopic, "/runtime/unknown",
            customMessages ? "urban_runtime_msgs/msg/RuntimeAgentStatus" : "std_msgs/msg/String",
            "urban::UnknownRuntimeMessage", "RELIABLE_KEEP_LAST", "pub", "Unknown runtime message");
    }

    @SuppressWarnings("unchecked")
    private String customPayload(Ros2TopicBinding binding, Map<String, Object> runtimeMessage) {
        Map<String, Object> payload = runtimeMessage.get("payload") instanceof Map<?, ?>
            ? (Map<String, Object>) runtimeMessage.get("payload") : new LinkedHashMap<String, Object>();
        String sourceId = value(runtimeMessage, "sourceId", "ROS2-BRIDGE");
        long timestamp = longValue(runtimeMessage.get("timestamp"), System.currentTimeMillis());
        String topic = String.valueOf(runtimeMessage.get("topic"));
        String common = "message_id: '" + quote("MSG-" + timestamp) + "', source_id: '" + quote(sourceId)
            + "', target_id: 'ALL', timestamp_ms: " + timestamp;
        if (LocalDDSBroker.TOPIC_AGENT_STATUS.equals(topic)) {
            return "{" + common + ", agent_id: '" + quote(value(payload, "agentId", sourceId))
                + "', role: '" + quote(value(payload, "role", "UNKNOWN"))
                + "', protocol: '" + quote(value(payload, "protocol", "UNKNOWN"))
                + "', route: '" + quote(value(payload, "route", "unknown"))
                + "', position_x_m: " + doubleValue(payload.get("x"), 0.0)
                + ", position_y_m: " + doubleValue(payload.get("y"), 0.0)
                + ", position_z_m: " + doubleValue(payload.get("z"), 0.0)
                + ", speed_mps: " + doubleValue(payload.get("speed"), 0.0)
                + ", battery_pct: " + doubleValue(payload.get("battery"), 0.0)
                + ", publish_rate_hz: " + doubleValue(payload.get("rateHz"), 0.0)
                + ", mission_status: '" + quote(value(payload, "missionStatus", "UNKNOWN"))
                + "', link_id: '" + quote(value(payload, "linkId", ""))
                + "', message_type: '" + quote(value(payload, "messageType", ""))
                + "', message_instance_id: " + longValue(payload.get("messageInstanceId"), 0L)
                + ", stream_id: " + longValue(payload.get("streamId"), 0L)
                + ", ack_required: " + booleanValue(payload.get("ackRequired"), false)
                + ", valid_time_ms: " + longValue(payload.get("validTimeMs"), 0L)
                + ", latency_ms: " + doubleValue(payload.get("latencyMs"), 0.0)
                + ", checksum: '" + quote(value(payload, "checksum", "")) + "'}";
        }
        if (LocalDDSBroker.TOPIC_FINAL_DECISION.equals(topic)) {
            return "{" + common + ", mission_id: '" + quote(value(payload, "missionId", "MSN-RUNTIME"))
                + "', selected_strategy: '" + quote(value(payload, "selectedStrategy", "unknown"))
                + "', selected_route: '" + quote(value(payload, "selectedRoute", "unknown"))
                + "', baseline_comparison: '" + quote(value(payload, "baselineComparison", ""))
                + "', explanation: '" + quote(value(payload, "explanation", ""))
                + "', ros2_dds_ready: " + booleanValue(payload.get("ros2DdsReady"), true) + "}";
        }
        if (LocalDDSBroker.TOPIC_CONFLICT_EVENT.equals(topic)) {
            return "{" + common + ", conflict_id: '" + quote(value(payload, "conflictId", "CFL-RUNTIME"))
                + "', conflict_type: '" + quote(value(payload, "conflictType", "UNKNOWN"))
                + "', involved_agents: " + stringArray(value(payload, "involvedAgents", ""))
                + ", route: '" + quote(value(payload, "route", "unknown"))
                + "', severity: " + doubleValue(payload.get("severity"), 0.0)
                + ", explanation: '" + quote(value(payload, "explanation", "")) + "'}";
        }
        if (LocalDDSBroker.TOPIC_REPLAN_REQUEST.equals(topic)) {
            return "{" + common + ", mission_id: '" + quote(value(payload, "missionId", "MSN-RUNTIME"))
                + "', reason: '" + quote(value(payload, "reason", ""))
                + "', request: '" + quote(value(payload, "request", ""))
                + "', requested_by: '" + quote(sourceId) + "'}";
        }
        if (LocalDDSBroker.TOPIC_POLICY_EVALUATION.equals(topic)) {
            return "{" + common + ", mission_id: '" + quote(value(payload, "missionId", "MSN-RUNTIME"))
                + "', winner: '" + quote(value(payload, "winner", "unknown"))
                + "', dijkstra_risk: " + doubleValue(payload.get("dijkstraRisk"), 0.0)
                + ", greedy_risk: " + doubleValue(payload.get("greedyRisk"), 0.0)
                + ", cooperative_risk: " + doubleValue(payload.get("cooperativeRisk"), 0.0)
                + ", conflict_count: " + longValue(payload.get("conflictCount"), 0L)
                + ", explanation: '" + quote(value(payload, "explanation", "")) + "'}";
        }
        if (LocalDDSBroker.TOPIC_PROTOCOL_FRAME.equals(topic)) {
            return "{" + common + ", agent_id: '" + quote(value(payload, "agentId", sourceId))
                + "', role: '" + quote(value(payload, "role", "UNKNOWN"))
                + "', protocol: '" + quote(value(payload, "protocol", "UNKNOWN"))
                + "', unified_topic: '" + quote(value(payload, "unifiedTopic", ""))
                + "', native_payload_json: '" + quote(value(payload, "native", ""))
                + "', compatibility_note: 'adapter-normalized-native-frame'"
                + ", message_type: '" + quote(value(payload, "messageType", ""))
                + "', message_instance_id: " + longValue(payload.get("messageInstanceId"), 0L)
                + ", stream_id: " + longValue(payload.get("streamId"), 0L)
                + ", ack_required: " + booleanValue(payload.get("ackRequired"), false)
                + ", valid_time_ms: " + longValue(payload.get("validTimeMs"), 0L)
                + ", latency_ms: " + doubleValue(payload.get("latencyMs"), 0.0)
                + ", checksum: '" + quote(value(payload, "checksum", "")) + "'}";
        }
        return "{" + common + ", mission_id: '" + quote(value(payload, "missionId", "MSN-RUNTIME"))
            + "', natural_language: '" + quote(value(payload, "naturalLanguage", ""))
            + "', dsl_contract: '" + quote(value(payload, "dslContract", ""))
            + "', required_agents: " + stringArray(value(payload, "requiredAgents", ""))
            + ", default_status_interval_ms: " + longValue(payload.get("defaultStatusIntervalMs"), 500L) + "}";
    }

    private String ros2SafePayload(Map<String, Object> map) {
        String topic = String.valueOf(map.get("topic"));
        String sourceId = String.valueOf(map.get("sourceId"));
        Object payloadValue = map.get("payload");
        String payload = String.valueOf(payloadValue);
        String summary = "topic=" + topic + "|source=" + sourceId;
        if (topic.equals(LocalDDSBroker.TOPIC_FINAL_DECISION)) {
            summary += "|strategy=" + extract(payload, "selectedStrategy=", ",");
            summary += "|route=" + extract(payload, "selectedRoute=", ",");
        } else if (topic.equals(LocalDDSBroker.TOPIC_REPLAN_REQUEST)) {
            summary += "|event=replanning_request";
        } else if (topic.equals(LocalDDSBroker.TOPIC_CONFLICT_EVENT)) {
            summary += "|event=route_conflict";
        } else if (topic.equals(LocalDDSBroker.TOPIC_POLICY_EVALUATION)) {
            summary += "|winner=" + extract(payload, "winner=", "}");
        } else if (topic.equals(LocalDDSBroker.TOPIC_AGENT_STATUS)) {
            summary += "|agent=" + extract(payload, "agentId=", ",");
            summary += "|role=" + extract(payload, "role=", ",");
            summary += "|route=" + extract(payload, "route=", ",");
        }
        return summary.replace(" ", "_").replace("\"", "'");
    }

    private String extract(String text, String prefix, String delimiter) {
        int start = text.indexOf(prefix);
        if (start < 0) return "unknown";
        int valueStart = start + prefix.length();
        int end = text.indexOf(delimiter, valueStart);
        if (end < 0) end = text.length();
        return text.substring(valueStart, end).trim();
    }

    private String escapeForRos2String(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private String quote(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "''").replace("\r", " ").replace("\n", " ");
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value == null) return fallback;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringArray(String csv) {
        if (csv == null || csv.trim().isEmpty()) return "[]";
        StringBuilder builder = new StringBuilder("[");
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append("'").append(quote(parts[i].trim())).append("'");
        }
        return builder.append("]").toString();
    }

    private static String findRos2Executable() {
        String env = System.getenv("ROS2_CLI");
        if (isExecutable(env)) return env;
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) return "";
        String executableName = isWindows() ? "ros2.exe" : "ros2";
        for (String entry : path.split(File.pathSeparator)) {
            File candidate = new File(entry, executableName);
            if (candidate.exists()) {
                return candidate.getAbsolutePath();
            }
        }
        return "";
    }

    private static boolean isExecutable(String path) {
        return path != null && !path.isBlank() && new File(path).exists();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean customMessagesFromEnv() {
        String mode = System.getProperty("ROS2_MESSAGE_MODE");
        if (mode == null || mode.isBlank()) {
            mode = System.getenv("ROS2_MESSAGE_MODE");
        }
        return mode != null && mode.equalsIgnoreCase("custom");
    }
}
