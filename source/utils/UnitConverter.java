package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

/**
 * 单位转换器 - 实现跨系统的单位自动转换
 */
public class UnitConverter {
    // 字段名称与转换规则的映射
    private static final Map<String, ConversionRule> conversionRules = new HashMap<>();
    
    // 单位类型映射
    private static final Map<String, String> unitTypes = new HashMap<>();
    
    static {
        // 初始化单位类型映射
        initUnitTypes();

        // ==================== 速度转换规则 ====================
        // km/h -> m/s (民用交通常用)
        conversionRules.put("speed_kmh", new ConversionRule(3.6f, "km/h", "m/s"));
        // knots -> m/s (航空常用)
        conversionRules.put("speed_knots", new ConversionRule(1.94384f, "knots", "m/s"));
        // mph -> m/s (英制单位)
        conversionRules.put("speed_mph", new ConversionRule(2.23694f, "mph", "m/s"));

        // ==================== 高度/距离转换规则 ====================
        // m -> km (低空无人机高度)
        conversionRules.put("altitude_m", new ConversionRule(1000, "m", "km"));
        // ft -> m (英制高度)
        conversionRules.put("altitude_ft", new ConversionRule(0.3048f, "ft", "m"));
        // 地面距离 m -> km
        conversionRules.put("distance_m", new ConversionRule(1000, "m", "km"));
        // miles -> km (英制距离)
        conversionRules.put("distance_miles", new ConversionRule(1609.34f, "miles", "km"));

        // ==================== 温度转换规则 ====================
        // C -> F (特殊处理公式)
        conversionRules.put("temperature_c", new ConversionRule(1, "°C", "°F"));
        conversionRules.put("temperature_f", new ConversionRule(1, "°F", "°C"));

        // ==================== 重量/负载转换规则 ====================
        // kg -> lb (载重常用)
        conversionRules.put("weight_kg", new ConversionRule(2.20462f, "kg", "lb"));
        // g -> kg (小件快递)
        conversionRules.put("weight_g", new ConversionRule(1000, "generated-src", "kg"));

        // ==================== 电量/功率转换规则 ====================
        // mAh -> Ah (无人机电池)
        conversionRules.put("battery_mah", new ConversionRule(1000, "mAh", "Ah"));
        // W -> kW (功率)
        conversionRules.put("power_w", new ConversionRule(1000, "W", "kW"));
    }
    
    /**
     * 初始化单位类型映射
     */
    private static void initUnitTypes() {
        // 速度单位
        unitTypes.put("km/h", "speed");
        unitTypes.put("kmh", "speed");
        unitTypes.put("m/s", "speed");
        unitTypes.put("ms", "speed");
        unitTypes.put("knots", "speed");
        unitTypes.put("mph", "speed");
        
        // 高度/距离单位
        unitTypes.put("m", "distance");
        unitTypes.put("km", "distance");
        unitTypes.put("ft", "distance");
        unitTypes.put("miles", "distance");
        
        // 温度单位
        unitTypes.put("°C", "temperature");
        unitTypes.put("C", "temperature");
        unitTypes.put("°F", "temperature");
        unitTypes.put("F", "temperature");
        
        // 重量单位
        unitTypes.put("kg", "weight");
        unitTypes.put("generated-src", "weight");
        unitTypes.put("lb", "weight");
        
        // 电量单位
        unitTypes.put("mAh", "battery");
        unitTypes.put("Ah", "battery");
        
        // 功率单位
        unitTypes.put("W", "power");
        unitTypes.put("kW", "power");
    }
    
    /**
     * 智能单位转换 - 根据字段名和值自动选择转换规则
     */
    public static String smartConvert(String fieldName, String value) {
        // 首先使用同义词库标准化字段名
        String normalizedField = fieldName.toLowerCase();

        // 尝试从字符串中提取数值和单位
        Pattern pattern = Pattern.compile("([\\d.]+)\\s*([^\\d^\\source\"]*)\"?");
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            try {
                double numValue = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2);

                if (unit != null) {
                    unit = unit.trim();
                    if (unit.length() >= 2 && unit.startsWith("\"") && unit.endsWith("\"")) {
                        unit = unit.substring(1, unit.length() - 1);
                    }
                }

                // 如果有单位，进行转换
                if (unit != null) {
                    return performConversion(normalizedField, numValue, unit);
                } else {
                    return performDefaultConversion(normalizedField, numValue);
                }
            } catch (NumberFormatException e) {
                return value;
            }
        }

        return value;
    }
    
    /**
     * 执行特定单位转换
     */
    private static String performConversion(String fieldName, double value, String unit) {
        String unitLower = unit.toLowerCase();

        // 高度/距离单位
        if (fieldName.contains("altitude") || fieldName.contains("elevation")) {
            if (unitLower.contains("ft")) {
                double m = value * 0.3048;
                return String.format("%.2f m", m);
            } else if (unitLower.equals("m")) {
                double km = value / 1000;
                return String.format("%.3f km", km);
            }
        }

        // 速度单位
        if (fieldName.contains("speed") || fieldName.contains("velocity")) {
            if (unitLower.contains("km/h") || unitLower.contains("kmh")) {
                double ms = value / 3.6;
                return String.format("%.6f m/s", ms);
            } else if (unitLower.contains("knots")) {
                double ms = value / 1.94384;
                return String.format("%.6f m/s", ms);
            } else if (unitLower.contains("mph")) {
                double ms = value / 2.23694;
                return String.format("%.6f m/s", ms);
            }
        }

        // 温度
        if (fieldName.contains("temperature")) {
            if (unitLower.contains("c") || unitLower.contains("°c")) {
                double fahrenheit = value * 9 / 5 + 32;
                return String.format("%.2f°F", fahrenheit);
            } else if ((unitLower.contains("frontend") || unitLower.contains("°f")) && !unitLower.contains("ft")) {
                double celsius = (value - 32) * 5 / 9;
                return String.format("%.2f°C", celsius);
            }
        }

        // 重量/负载
        if (fieldName.contains("weight") || fieldName.contains("payload") || fieldName.contains("load") || fieldName.contains("cargo")) {
            if (unitLower.contains("kg")) {
                double lb = value * 2.20462;
                return String.format("%.2f lb", lb);
            } else if (unitLower.contains("generated-src") && !unitLower.contains("m")) {
                double kg = value / 1000;
                return String.format("%.3f kg", kg);
            } else if (unitLower.contains("lb")) {
                double kg = value / 2.20462;
                return String.format("%.2f kg", kg);
            }
        }

        // 电量
        if (fieldName.contains("battery") || fieldName.contains("capacity") || fieldName.contains("charge")) {
            if (unitLower.contains("mah")) {
                double ah = value / 1000;
                return String.format("%.1f Ah", ah);
            } else if (unitLower.contains("ah")) {
                return String.format("%.1f Ah", value);
            }
        }

        // 功率
        if (fieldName.contains("power") || fieldName.contains("watt")) {
            if (unitLower.contains("kw")) {
                double w = value * 1000;
                return String.format("%.0f W", w);
            } else if (unitLower.contains("w")) {
                double kw = value / 1000;
                return String.format("%.3f kW", kw);
            }
        }

        return value + " " + unit;
    }

    /**
     * 执行默认转换
     */
    private static String performDefaultConversion(String fieldName, double value) {
        if (fieldName.contains("speed") || fieldName.contains("velocity")) {
            return String.format("%.2f m/s", value);
        } else if (fieldName.contains("altitude") || fieldName.contains("elevation")) {
            return String.format("%.3f km", value / 1000);
        } else if (fieldName.contains("temperature")) {
            return String.format("%.2f°C", value);
        } else {
            return String.format("%.2f", value);
        }
    }

    /**
     * 转换规则类
     */
    static class ConversionRule {
        double factor;
        String fromUnit;
        String toUnit;

        ConversionRule(double factor, String fromUnit, String toUnit) {
            this.factor = factor;
            this.fromUnit = fromUnit;
            this.toUnit = toUnit;
        }
    }
}
