package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险评估引擎。
 *
 * 综合任务优先级、设备状态、路径风险、拥堵、DDS 失败和故障注入输出可解释风险结果。
 */
public class RiskAssessmentEngine {
    public Map<String, Object> assess(MissionDslModel mission, List<Map<String, Object>> devices,
                                      Map<String, Object> routePlan, List<Map<String, Object>> ddsLogs,
                                      List<Map<String, Object>> faults) {
        int score = 10;
        List<String> factors = new ArrayList<String>();
        List<String> mitigations = new ArrayList<String>();

        if (mission != null && "HIGH".equals(mission.priority)) {
            score += 12;
            factors.add("高优先级任务对时延和可靠性敏感");
            mitigations.add("提升关键 Topic QoS，并优先调度低负载设备");
        }

        if (devices != null) {
            for (Map<String, Object> device : devices) {
                int battery = number(device.get("batteryLevel"), 100);
                double load = decimal(device.get("currentLoad"), 0.0);
                double link = decimal(device.get("linkQuality"), 1.0);
                boolean online = Boolean.TRUE.equals(device.get("available")) || Boolean.TRUE.equals(device.get("online"));
                if (!online) {
                    score += 18;
                    factors.add(device.get("deviceId") + " 不可用");
                    mitigations.add("切换备选设备或启用 RSU/广域链路接管");
                }
                if (battery < 30) {
                    score += 12;
                    factors.add(device.get("deviceId") + " 电量低");
                    mitigations.add("降低该设备任务权重，优先分配短时任务");
                }
                if (load > 0.80) {
                    score += 8;
                    factors.add(device.get("deviceId") + " 负载高");
                    mitigations.add("将感知或同步任务迁移至备用节点");
                }
                if (link < 0.70) {
                    score += 10;
                    factors.add(device.get("deviceId") + " 链路质量下降");
                    mitigations.add("启用可靠 QoS 重试和广域链路摘要同步");
                }
            }
        }

        double routeRisk = decimal(routePlan == null ? null : routePlan.get("riskScore"), 0.0);
        if (routeRisk > 0.45) {
            score += 15;
            factors.add("推荐路径风险偏高");
            mitigations.add("持续订阅 TrafficRiskTopic 并准备保守路线");
        }
        long failures = 0;
        if (ddsLogs != null) {
            for (Map<String, Object> log : ddsLogs) {
                String status = String.valueOf(log.get("status"));
                if (status.contains("DROP") || status.contains("FAIL") || status.contains("DEGRADE")) failures++;
            }
        }
        if (failures > 0) {
            score += Math.min(20, failures * 6);
            factors.add("DDS 通信存在失败或降级事件 " + failures + " 次");
            mitigations.add("增加重试、切换可靠 Topic 或等待下一周期样本");
        }
        if (faults != null && !faults.isEmpty()) {
            score += Math.min(25, faults.size() * 10);
            factors.add("存在故障注入: " + faults.size() + " 项");
            mitigations.add("执行故障清除或触发备选设备/fallback 策略");
        }
        score = Math.max(0, Math.min(100, score));
        String level = score >= 85 ? "CRITICAL" : score >= 65 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW";
        if (factors.isEmpty()) factors.add("当前任务风险可控");
        if (mitigations.isEmpty()) mitigations.add("维持现有 QoS、周期性状态反馈和风险订阅");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("riskScore", score);
        result.put("riskLevel", level);
        result.put("riskFactors", factors);
        result.put("mitigationActions", mitigations);
        result.put("whetherFallbackRequired", "HIGH".equals(level) || "CRITICAL".equals(level));
        return result;
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    private double decimal(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }
}
