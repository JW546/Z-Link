package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存级设备注册中心。
 *
 * 负责维护无人车、无人机、RSU、应急车辆和广域链路的实时状态。调度算法只读取这里的状态，
 * 因此前端设备动作、心跳、故障标记会真实影响后续任务分配。
 */
public class DeviceRegistry {
    private static final DeviceRegistry INSTANCE = new DeviceRegistry();
    private final ConcurrentHashMap<String, DeviceRecord> devices = new ConcurrentHashMap<String, DeviceRecord>();

    private DeviceRegistry() {
        resetDefaults();
    }

    public synchronized void resetDefaults() {
        devices.clear();
        register(record("CAR-CQ-001", "无人车", "车端自主决策节点", "观音桥", 82, 0.38, 0.90, "ONLINE", 14,
            Arrays.asList("道路行驶", "物资运输", "医疗转运", "路径执行", "VehicleStatusTopic", "CollaborationPlanTopic"),
            Arrays.asList("VehicleStatusTopic", "CollaborationPlanTopic", "TrafficRiskTopic")));
        register(record("UAV-CQ-007", "无人机", "低空前视感知节点", "主路上空", 76, 0.44, 0.82, "ONLINE", 32,
            Arrays.asList("高空巡检", "快速侦察", "道路拥堵探测", "事故检测", "DroneMissionTopic", "TrafficRiskTopic"),
            Arrays.asList("DroneMissionTopic", "TrafficRiskTopic")));
        register(record("RSU-SCHOOL-009", "RSU", "路侧事件发布节点", "学校路口", 100, 0.30, 0.92, "ONLINE", 18,
            Arrays.asList("路侧感知", "信号灯协同", "局部通信中继", "RoadSideUnitTopic", "EmergencyDispatchTopic"),
            Arrays.asList("RoadSideUnitTopic", "EmergencyDispatchTopic", "TrafficRiskTopic")));
        register(record("EMG-CQ-120", "应急车辆", "应急医疗车辆", "医院入口", 88, 0.22, 0.86, "STANDBY", 26,
            Arrays.asList("应急救援", "医疗转运", "消防救援", "EmergencyDispatchTopic", "VehicleStatusTopic"),
            Arrays.asList("EmergencyDispatchTopic", "VehicleStatusTopic")));
        register(record("CN-BACKBONE-CQ", "广域链路", "合规广域态势节点", "重庆区域节点", 100, 0.26, 0.84, "ONLINE", 41,
            Arrays.asList("跨区域态势共享", "远程指挥", "DDS Router", "CollaborationPlanTopic", "TrafficRiskTopic"),
            Arrays.asList("CollaborationPlanTopic", "TrafficRiskTopic")));
    }

    public static DeviceRegistry getInstance() {
        return INSTANCE;
    }

    public void register(DeviceRecord record) {
        if (record == null || record.deviceId == null || record.deviceId.isBlank()) return;
        record.lastHeartbeat = System.currentTimeMillis();
        devices.put(record.deviceId, record);
    }

    public boolean unregister(String deviceId) {
        return deviceId != null && devices.remove(deviceId) != null;
    }

    public DeviceRecord heartbeat(String deviceId) {
        DeviceRecord record = devices.get(deviceId);
        if (record != null) {
            record.lastHeartbeat = System.currentTimeMillis();
            if ("OFFLINE".equals(record.status)) record.status = "ONLINE";
            record.failureReason = "";
        }
        return record;
    }

    public DeviceRecord applyAction(String deviceId, String action) {
        DeviceRecord record = devices.get(deviceId);
        if (record == null) return null;
        String normalized = action == null ? "" : action.trim().toLowerCase();
        if ("unbind".equals(normalized)) {
            markFailure(deviceId, "用户手动解绑，设备暂不可调度");
        } else if ("bind".equals(normalized) || "reconnect".equals(normalized)) {
            recover(deviceId);
            record.latencyMs = Math.max(6, record.latencyMs - 4);
            record.linkQuality = Math.min(0.99, record.linkQuality + 0.04);
        } else if ("check".equals(normalized) || "inspect".equals(normalized)) {
            heartbeat(deviceId);
            record.currentLoad = Math.max(0.05, record.currentLoad - 0.02);
        }
        return record;
    }

    public DeviceRecord markFailure(String deviceId, String reason) {
        DeviceRecord record = devices.get(deviceId);
        if (record != null) {
            record.status = "OFFLINE";
            record.failureReason = reason == null || reason.isBlank() ? "设备故障或通信中断" : reason;
            record.lastHeartbeat = System.currentTimeMillis();
        }
        return record;
    }

    public DeviceRecord recover(String deviceId) {
        DeviceRecord record = devices.get(deviceId);
        if (record != null) {
            record.status = "ONLINE";
            record.failureReason = "";
            record.lastHeartbeat = System.currentTimeMillis();
        }
        return record;
    }

    public DeviceRecord updateDeviceMetrics(String deviceId, Integer battery, Double load, Double linkQuality, String status) {
        DeviceRecord record = devices.get(deviceId);
        if (record != null) {
            if (battery != null) record.batteryLevel = Math.max(0, Math.min(100, battery));
            if (load != null) record.currentLoad = Math.max(0.0, Math.min(1.0, load));
            if (linkQuality != null) record.linkQuality = Math.max(0.0, Math.min(1.0, linkQuality));
            if (status != null && !status.isBlank()) record.status = status;
            record.lastHeartbeat = System.currentTimeMillis();
        }
        return record;
    }

    public DeviceRecord assignMission(String deviceId, String missionId) {
        DeviceRecord record = devices.get(deviceId);
        if (record != null) {
            record.assignedMissionId = missionId == null ? "" : missionId;
            record.currentLoad = Math.min(0.95, record.currentLoad + 0.08);
            record.lastHeartbeat = System.currentTimeMillis();
        }
        return record;
    }

    public List<Map<String, Object>> listDevices() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceRecord record : devices.values()) {
            result.add(record.toMap());
        }
        result.sort((a, b) -> String.valueOf(a.get("deviceId")).compareTo(String.valueOf(b.get("deviceId"))));
        return result;
    }

    public List<DynamicTaskScheduler.DeviceState> schedulerDevices() {
        List<DynamicTaskScheduler.DeviceState> result = new ArrayList<DynamicTaskScheduler.DeviceState>();
        for (DeviceRecord record : devices.values()) {
            result.add(DynamicTaskScheduler.device(
                record.deviceId,
                record.deviceType,
                record.isAvailable(),
                record.latencyMs,
                record.batteryLevel,
                record.currentLoad,
                record.linkQuality,
                record.multiTaskCapable(),
                record.capabilities.toArray(new String[0])
            ));
        }
        return result;
    }

    public String backupDeviceFor(List<String> requiredCapabilities, String excludedDeviceId) {
        if (requiredCapabilities == null) return "";
        DeviceRecord best = null;
        double bestScore = -1;
        for (DeviceRecord record : devices.values()) {
            if (record.deviceId.equals(excludedDeviceId) || !record.isAvailable()) continue;
            if (!record.capabilities.containsAll(requiredCapabilities)) continue;
            double score = record.batteryLevel / 100.0 + record.linkQuality - record.currentLoad;
            if (score > bestScore) {
                best = record;
                bestScore = score;
            }
        }
        return best == null ? "" : best.deviceId;
    }

    public String riskFor(String deviceId) {
        DeviceRecord record = devices.get(deviceId);
        if (record == null) return "设备不存在，需人工确认或切换备选设备。";
        List<String> risks = new ArrayList<String>();
        if (!record.isAvailable()) risks.add("设备离线或故障: " + record.failureReason);
        if (record.batteryLevel < 30) risks.add("电量低于 30%");
        if (record.linkQuality < 0.70) risks.add("链路质量低于 0.70");
        if (record.currentLoad > 0.80) risks.add("当前负载较高");
        return risks.isEmpty() ? "风险可控" : String.join("；", risks);
    }

    public Map<String, Object> summary() {
        int online = 0;
        for (DeviceRecord record : devices.values()) {
            if (record.isAvailable()) online++;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("total", devices.size());
        map.put("available", online);
        map.put("offline", devices.size() - online);
        return map;
    }

    private DeviceRecord record(String id, String type, String name, String location, int battery, double load,
                                double linkQuality, String status, int latencyMs, List<String> capabilities,
                                List<String> supportedTopics) {
        DeviceRecord record = new DeviceRecord();
        record.deviceId = id;
        record.deviceType = type;
        record.name = name;
        record.location = location;
        record.batteryLevel = battery;
        record.currentLoad = load;
        record.linkQuality = linkQuality;
        record.status = status;
        record.latencyMs = latencyMs;
        record.capabilities = new ArrayList<String>(capabilities == null ? Collections.<String>emptyList() : capabilities);
        record.supportedTopics = new ArrayList<String>(supportedTopics == null ? Collections.<String>emptyList() : supportedTopics);
        return record;
    }

    public static class DeviceRecord {
        public String deviceId;
        public String deviceType;
        public String name;
        public List<String> capabilities = new ArrayList<String>();
        public List<String> supportedTopics = new ArrayList<String>();
        public int batteryLevel;
        public double currentLoad;
        public double linkQuality;
        public String location;
        public String status;
        public long lastHeartbeat;
        public String failureReason = "";
        public String assignedMissionId = "";
        public int latencyMs;

        boolean isAvailable() {
            return ("ONLINE".equals(status) || "STANDBY".equals(status))
                && batteryLevel > 10
                && linkQuality > 0.35;
        }

        boolean multiTaskCapable() {
            return "RSU".equals(deviceType) || "广域链路".equals(deviceType) || "无人车".equals(deviceType);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("deviceId", deviceId);
            map.put("deviceType", deviceType);
            map.put("name", name);
            map.put("capabilities", capabilities);
            map.put("supportedTopics", supportedTopics);
            map.put("batteryLevel", batteryLevel);
            map.put("currentLoad", Math.round(currentLoad * 100.0) / 100.0);
            map.put("linkQuality", Math.round(linkQuality * 100.0) / 100.0);
            map.put("location", location);
            map.put("status", status);
            map.put("lastHeartbeat", lastHeartbeat);
            map.put("failureReason", failureReason);
            map.put("assignedMissionId", assignedMissionId);
            map.put("latencyMs", latencyMs);
            map.put("available", isAvailable());

            // legacy frontend fields
            map.put("id", deviceId);
            map.put("role", deviceType);
            map.put("online", isAvailable());
            map.put("battery", batteryLevel);
            map.put("link", "DDS link=" + Math.round(linkQuality * 100) + "%");
            return map;
        }
    }
}
