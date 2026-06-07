package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TopicTopologyService {
    public Map<String, Object> build(List<Map<String, Object>> topics, List<Map<String, Object>> devices,
                                     List<Map<String, Object>> logs, String runtimeMode) {
        Map<String, Object> topology = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> edges = new ArrayList<Map<String, Object>>();
        nodes.add(node("PlatformOrchestrator", "service", "任务编排"));
        nodes.add(node("DdsCommunicationService", "service", "DDS 网关"));
        if (devices != null) {
            for (Map<String, Object> device : devices) nodes.add(node(String.valueOf(device.get("deviceId")), "device", String.valueOf(device.get("deviceType"))));
        }
        if (topics != null) {
            for (Map<String, Object> topic : topics) {
                String topicName = String.valueOf(topic.get("name"));
                nodes.add(node(topicName, "topic", String.valueOf(topic.get("idlType"))));
                Object pubs = topic.get("publishers");
                if (pubs instanceof List<?>) {
                    for (Object pub : (List<?>) pubs) edges.add(edge(String.valueOf(pub), topicName, "publish", topic.get("defaultQos")));
                }
                Object subs = topic.get("subscribers");
                if (subs instanceof List<?>) {
                    for (Object sub : (List<?>) subs) edges.add(edge(topicName, String.valueOf(sub), "subscribe", topic.get("defaultQos")));
                }
            }
        }
        if (logs != null) {
            for (Map<String, Object> log : logs) {
                String phase = String.valueOf(log.get("phase"));
                if (phase.contains("RETRY") || phase.contains("DEGRADE")) {
                    edges.add(edge("DdsCommunicationService", String.valueOf(log.get("topic")), phase.toLowerCase(), log.get("qos")));
                }
            }
        }
        topology.put("nodes", nodes);
        topology.put("edges", edges);
        topology.put("runtimeMode", runtimeMode);
        topology.put("recentStatus", logs == null || logs.isEmpty() ? "NO_RECENT_TRAFFIC" : String.valueOf(logs.get(logs.size() - 1).get("status")));
        topology.put("qosProfile", "dynamic mission QoS with retry/fallback policy");
        return topology;
    }

    private Map<String, Object> node(String id, String type, String label) {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("id", id);
        node.put("type", type);
        node.put("label", label);
        return node;
    }

    private Map<String, Object> edge(String from, String to, String relation, Object qos) {
        Map<String, Object> edge = new LinkedHashMap<String, Object>();
        edge.put("from", from);
        edge.put("to", to);
        edge.put("relation", relation);
        edge.put("qosProfile", qos);
        return edge;
    }
}
