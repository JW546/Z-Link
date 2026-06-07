package model;

import java.util.HashMap;
import java.util.Map;

public class AnnotationInfo {
    private String name;
    private Map<String, String> fields = new HashMap<>();

    public AnnotationInfo(String name) {
        this.name = name;
    }

    public void addField(String key, String value) {
        fields.put(key, value);
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("@").append(name);
        if (!fields.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(entry.getKey()).append(" = ").append(entry.getValue());
            }
            sb.append(")");
        }
        return sb.toString();
    }
}