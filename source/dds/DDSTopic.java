package dds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Semantic DDS topic plus IDL binding.
 */
public class DDSTopic {
    public final String name;
    public final String ddsTopic;
    public final String idlType;
    public final DDSQoS qos;

    public DDSTopic(String name, String ddsTopic, String idlType, DDSQoS qos) {
        this.name = name;
        this.ddsTopic = ddsTopic;
        this.idlType = idlType;
        this.qos = qos == null ? DDSQoS.reliable() : qos;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", name);
        map.put("ddsTopic", ddsTopic);
        map.put("idlType", idlType);
        map.put("qos", qos.toMap());
        return map;
    }
}
