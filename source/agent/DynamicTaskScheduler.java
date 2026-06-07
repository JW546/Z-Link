package agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 多设备协同任务调度器。
 *
 * 应用场景：乘客意图触发后，车辆需要决定哪些设备负责前视感知、路侧事件查询、
 * 邻车建议、广域态势同步等任务。这里不是中心派单，而是为车端自治协同生成可解释的建议分工。
 *
 * 算法原理：
 * 1. 为每个“任务-设备”组合计算动态权重评分，评分由能力匹配、任务优先级、链路延迟、电量、
 *    当前负载和链路健康度加权得到。
 * 2. 使用最大评分优先队列做贪心调度，每个任务分配给当前最合适的可用设备。
 * 3. 该实现不是严格的二分图最大匹配或匈牙利算法；它优先保证实时性和解释性。
 *
 * 复杂度：设任务数为 T、设备数为 D，建堆 O(TD log(TD))，空间复杂度 O(TD)。
 */
public class DynamicTaskScheduler {
    public ScheduleResult schedule(List<TaskRequest> tasks, List<DeviceState> devices) {
        ScheduleResult result = new ScheduleResult();
        if (tasks == null || tasks.isEmpty() || devices == null || devices.isEmpty()) {
            result.note = "任务或设备为空，无法生成调度方案。";
            return result;
        }

        PriorityQueue<Candidate> candidates = new PriorityQueue<Candidate>(
            Comparator.comparingDouble((Candidate c) -> c.score).reversed());
        for (TaskRequest task : tasks) {
            if (task == null || task.id == null || task.id.isBlank()) {
                continue;
            }
            for (DeviceState device : devices) {
                if (device == null || device.id == null || device.id.isBlank()) {
                    continue;
                }
                double score = score(task, device);
                if (score > 0.0) {
                    candidates.add(new Candidate(task, device, score));
                }
            }
        }

        Set<String> assignedTasks = new HashSet<String>();
        Set<String> saturatedDevices = new HashSet<String>();
        while (!candidates.isEmpty() && assignedTasks.size() < tasks.size()) {
            Candidate candidate = candidates.poll();
            if (assignedTasks.contains(candidate.task.id)) {
                continue;
            }
            if (saturatedDevices.contains(candidate.device.id)) {
                continue;
            }
            assignedTasks.add(candidate.task.id);
            if (!candidate.device.multiTaskCapable) {
                saturatedDevices.add(candidate.device.id);
            }
            result.assignments.add(new Assignment(candidate.task, candidate.device, candidate.score));
        }

        for (TaskRequest task : tasks) {
            if (task != null && task.id != null && !task.id.isBlank() && !assignedTasks.contains(task.id)) {
                result.unassignedTasks.add(task.id);
            }
        }
        result.note = result.unassignedTasks.isEmpty()
            ? "所有协同任务均已匹配到设备。"
            : "部分任务未匹配，建议增加设备能力或放宽延迟/电量约束。";
        return result;
    }

    private double score(TaskRequest task, DeviceState device) {
        if (task == null || device == null || task.requiredCapabilities == null || device.capabilities == null) {
            return 0.0;
        }
        if (!device.online || device.batteryPercent < task.minBatteryPercent) {
            return 0.0;
        }
        if (!device.capabilities.containsAll(task.requiredCapabilities)) {
            return 0.0;
        }
        double capabilityScore = 1.0;
        double priorityScore = clamp(task.priority / 10.0, 0.0, 1.0);
        double latencyScore = 1.0 - clamp(device.latencyMs / Math.max(1.0, task.maxLatencyMs), 0.0, 1.0);
        double batteryScore = clamp(device.batteryPercent / 100.0, 0.0, 1.0);
        double loadScore = 1.0 - clamp(device.load, 0.0, 1.0);
        double linkScore = clamp(device.linkHealth, 0.0, 1.0);

        return capabilityScore * 0.35
            + priorityScore * 0.20
            + latencyScore * 0.15
            + batteryScore * 0.10
            + loadScore * 0.10
            + linkScore * 0.10;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static TaskRequest task(String id, String name, int priority, double maxLatencyMs,
                                   double minBatteryPercent, String... capabilities) {
        TaskRequest task = new TaskRequest();
        task.id = id;
        task.name = name;
        task.priority = priority;
        task.maxLatencyMs = maxLatencyMs;
        task.minBatteryPercent = minBatteryPercent;
        if (capabilities != null) {
            Collections.addAll(task.requiredCapabilities, capabilities);
        }
        return task;
    }

    public static DeviceState device(String id, String role, boolean online, double latencyMs,
                                     double batteryPercent, double load, double linkHealth,
                                     boolean multiTaskCapable, String... capabilities) {
        DeviceState device = new DeviceState();
        device.id = id;
        device.role = role;
        device.online = online;
        device.latencyMs = latencyMs;
        device.batteryPercent = batteryPercent;
        device.load = load;
        device.linkHealth = linkHealth;
        device.multiTaskCapable = multiTaskCapable;
        if (capabilities != null) {
            Collections.addAll(device.capabilities, capabilities);
        }
        return device;
    }

    public static class TaskRequest {
        public String id;
        public String name;
        public int priority;
        public double maxLatencyMs;
        public double minBatteryPercent;
        public Set<String> requiredCapabilities = new HashSet<String>();
    }

    public static class DeviceState {
        public String id;
        public String role;
        public boolean online;
        public double latencyMs;
        public double batteryPercent;
        public double load;
        public double linkHealth;
        public boolean multiTaskCapable;
        public Set<String> capabilities = new HashSet<String>();
    }

    public static class Assignment {
        public final TaskRequest task;
        public final DeviceState device;
        public final double score;

        Assignment(TaskRequest task, DeviceState device, double score) {
            this.task = task;
            this.device = device;
            this.score = score;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("taskId", task.id);
            map.put("taskName", task.name == null ? task.id : task.name);
            map.put("deviceId", device.id);
            map.put("deviceRole", device.role == null ? "UNKNOWN" : device.role);
            map.put("score", Math.round(score * 100.0) / 100.0);
            map.put("reason", String.format(Locale.ROOT,
                "capability=%s latency=%.0fms battery=%.0f%% load=%.2f link=%.2f",
                task.requiredCapabilities, device.latencyMs, device.batteryPercent, device.load, device.linkHealth));
            return map;
        }
    }

    public static class ScheduleResult {
        public List<Assignment> assignments = new ArrayList<Assignment>();
        public List<String> unassignedTasks = new ArrayList<String>();
        public String note;

        public Map<String, Object> toMap() {
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (Assignment assignment : assignments) {
                items.add(assignment.toMap());
            }
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("algorithm", "dynamic weighted greedy task scheduling");
            map.put("assignments", items);
            map.put("unassignedTasks", unassignedTasks);
            map.put("note", note);
            map.put("timeComplexity", "O(TD log(TD))");
            map.put("spaceComplexity", "O(TD)");
            return map;
        }
    }

    private static class Candidate {
        final TaskRequest task;
        final DeviceState device;
        final double score;

        Candidate(TaskRequest task, DeviceState device, double score) {
            this.task = task;
            this.device = device;
            this.score = score;
        }
    }
}
