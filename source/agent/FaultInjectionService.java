package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 实验故障注入服务。
 *
 * 用于模拟设备离线、低电量、链路退化、DDS 发布失败、道路拥堵和路径阻断等情况。
 */
public class FaultInjectionService {
    private static final FaultInjectionService INSTANCE = new FaultInjectionService();
    private final CopyOnWriteArrayList<Map<String, Object>> faults = new CopyOnWriteArrayList<Map<String, Object>>();
    private final DeviceRegistry registry = DeviceRegistry.getInstance();

    public static FaultInjectionService getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> inject(String type, String target, String reason) {
        String normalized = type == null || type.isBlank() ? "DEVICE_OFFLINE" : type.trim().toUpperCase();
        String safeTarget = target == null || target.isBlank() ? "CAR-CQ-001" : target.trim();
        Map<String, Object> fault = new LinkedHashMap<String, Object>();
        fault.put("faultId", "FLT-" + System.currentTimeMillis());
        fault.put("type", normalized);
        fault.put("target", safeTarget);
        fault.put("reason", reason == null || reason.isBlank() ? defaultReason(normalized) : reason);
        fault.put("timestamp", System.currentTimeMillis());
        fault.put("active", true);
        faults.add(fault);
        applyDeviceImpact(normalized, safeTarget);
        return fault;
    }

    public Map<String, Object> clear() {
        faults.clear();
        registry.resetDefaults();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("ok", true);
        result.put("message", "故障已清除，设备注册中心恢复默认状态。");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> fault : faults) copy.add(new LinkedHashMap<String, Object>(fault));
        return copy;
    }

    public boolean hasType(String type) {
        for (Map<String, Object> fault : faults) {
            if (Boolean.TRUE.equals(fault.get("active")) && String.valueOf(fault.get("type")).equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    public int count() {
        return faults.size();
    }

    private void applyDeviceImpact(String type, String target) {
        if ("DEVICE_OFFLINE".equals(type) || "SENSOR_UNAVAILABLE".equals(type)) {
            registry.markFailure(target, defaultReason(type));
        } else if ("LOW_BATTERY".equals(type)) {
            registry.updateDeviceMetrics(target, 18, null, null, null);
        } else if ("LINK_DEGRADED".equals(type)) {
            registry.updateDeviceMetrics(target, null, null, 0.42, null);
        }
    }

    private String defaultReason(String type) {
        if ("LOW_BATTERY".equals(type)) return "模拟设备低电量";
        if ("LINK_DEGRADED".equals(type)) return "模拟链路质量下降";
        if ("DDS_PUBLISH_FAILED".equals(type)) return "模拟 DDS 发布失败";
        if ("HIGH_CONGESTION".equals(type)) return "模拟道路拥堵升高";
        if ("ROUTE_BLOCKED".equals(type)) return "模拟路径阻断";
        if ("SENSOR_UNAVAILABLE".equals(type)) return "模拟传感器不可用";
        return "模拟设备离线";
    }
}
