package model;

import java.util.HashMap;
import java.util.Map;
// import annotations.PathAnnotation;
import utils.UnitConverter;

public class PathPlan extends AbstractDSLEntity {
    private String name;
    private Map<String, String> fields = new HashMap<>();
    private AnnotationInfo annotation;
    
    // ID 字段列表 - 这些字段不应该进行单位转换
    private static final String[] ID_FIELDS = {
        "fleet_id", "drone_id", "vehicle_id", "network_id", "cargo_id",
        "command_id", "drone_types", "bridge_list", "monitoring_areas",
        "waypoints", "destination", "alert_type", "recommendation"
    };

    public PathPlan(String name) {
        this.name = name;
    }

    public void addField(String key, String value) {
        // 对 ID 字段直接存储原始值，不进行单位转换
        if (isIdField(key)) {
            fields.put(key, value);
        } else {
            String converted = UnitConverter.smartConvert(key, value);
            fields.put(key, converted);
        }
    }
    
    /**
     * 检查是否是 ID 字段（不应该进行单位转换）
     */
    private boolean isIdField(String fieldName) {
        for (String idField : ID_FIELDS) {
            if (fieldName.equalsIgnoreCase(idField)) {
                return true;
            }
        }
        // 包含 _id 的字段也视为 ID 字段
        if (fieldName.toLowerCase().contains("_id")) {
            return true;
        }
        return false;
    }

    public void setAnnotation(AnnotationInfo annotation) {
        this.annotation = annotation;
    }

    public AnnotationInfo getAnnotation() {
        return annotation;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}