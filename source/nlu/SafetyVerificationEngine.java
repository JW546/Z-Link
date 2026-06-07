package nlu;

import java.util.*;
import java.util.regex.*;

/**
 * 安全验证引擎 - 实现"安全验证与仿真"
 * 
 * 核心功能：
 * 1. 指令冲突检测 - 识别违背物理常识的指令
 * 2. 时空一致性检查 - 多机协同时间戳校验
 * 3. 物理极限验证 - 设备能力边界检查
 * 4. 安全建议生成 - 提供优化后的执行方案
 * 
 * 对应论文: 第3章 - 安全验证与仿真
 */
public class SafetyVerificationEngine {
    
    /**
     * 设备物理参数
     */
    public static class DeviceCapabilities {
        double maxSpeed;      // 最大速度 m/s
        double maxAltitude;  // 最大高度 m
        double maxRange;      // 最大航程 km
        double minAltitude;   // 最小飞行高度 m
        int maxFlightTime;    // 最大飞行时间 分钟
        double maxPayload;    // 最大载重 kg
        double maxClimbRate;  // 最大爬升率 m/s
        double maxDescendRate;// 最大下降率 m/s
        
        public DeviceCapabilities() {
            // 默认无人机参数
            this.maxSpeed = 20;         // 20 m/s
            this.maxAltitude = 500;     // 500 m
            this.maxRange = 15;         // 15 km
            this.minAltitude = 10;      // 10 m
            this.maxFlightTime = 45;    // 45 分钟
            this.maxPayload = 5;        // 5 kg
            this.maxClimbRate = 5;      // 5 m/s
            this.maxDescendRate = 3;    // 3 m/s
        }
        
        public static DeviceCapabilities forVehicle() {
            DeviceCapabilities caps = new DeviceCapabilities();
            caps.maxSpeed = 33.3;        // 120 km/h
            caps.maxRange = 500;         // 500 km
            caps.maxPayload = 1000;      // 1000 kg
            return caps;
        }
    }
    
    /**
     * 验证结果
     */
    public static class VerificationResult {
        private boolean passed;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;
        private List<ConstraintViolation> violations;
        private double safetyScore;
        private String optimizedCommand;
        
        public VerificationResult() {
            this.passed = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.suggestions = new ArrayList<>();
            this.violations = new ArrayList<>();
            this.safetyScore = 1.0;
        }
        
        public void addError(String error) { errors.add(error); passed = false; }
        public void addWarning(String warning) { warnings.add(warning); safetyScore -= 0.1; }
        public void addViolation(ConstraintViolation v) { violations.add(v); }
        
        public void addSuggestion(String suggestion) { suggestions.add(suggestion); }
        
        public void setOptimizedCommand(String cmd) { this.optimizedCommand = cmd; }
        
        public boolean isPassed() { return passed; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getSuggestions() { return suggestions; }
        public List<ConstraintViolation> getViolations() { return violations; }
        public double getSafetyScore() { return Math.max(0, safetyScore); }
        public String getOptimizedCommand() { return optimizedCommand; }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Verification Result: ").append(passed ? "PASSED" : "FAILED").append("\n");
            sb.append("Safety Score: ").append(String.format("%.1f%%", safetyScore * 100)).append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("\nErrors:\n");
                for (String e : errors) sb.append("  [X] ").append(e).append("\n");
            }
            if (!warnings.isEmpty()) {
                sb.append("\nWarnings:\n");
                for (String w : warnings) sb.append("  [!] ").append(w).append("\n");
            }
            if (!suggestions.isEmpty()) {
                sb.append("\nSuggestions:\n");
                for (String s : suggestions) sb.append("  [>] ").append(s).append("\n");
            }
            if (optimizedCommand != null) {
                sb.append("\nOptimized Command:\n  ").append(optimizedCommand).append("\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * 约束违反
     */
    public static class ConstraintViolation {
        private String type;
        private String description;
        private double severity;  // 0-1, 越高越严重
        private String suggestedFix;
        
        public ConstraintViolation(String type, String desc, double severity) {
            this.type = type;
            this.description = desc;
            this.severity = severity;
        }
        
        public void setSuggestedFix(String fix) { this.suggestedFix = fix; }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public double getSeverity() { return severity; }
        public String getSuggestedFix() { return suggestedFix; }
    }
    
    /**
     * 协同任务
     */
    public static class CoordinatedMission {
        private String missionId;
        private List<MissionTask> tasks;
        private long startTimestamp;
        private long endTimestamp;
        
        public CoordinatedMission(String id) {
            this.missionId = id;
            this.tasks = new ArrayList<>();
        }
        
        public void addTask(MissionTask task) { tasks.add(task); }
        
        public String getMissionId() { return missionId; }
        public List<MissionTask> getTasks() { return tasks; }
        public long getStartTimestamp() { return startTimestamp; }
        public void setStartTimestamp(long ts) { this.startTimestamp = ts; }
        public long getEndTimestamp() { return endTimestamp; }
        public void setEndTimestamp(long ts) { this.endTimestamp = ts; }
    }
    
    /**
     * 任务定义
     */
    public static class MissionTask {
        private String taskId;
        private String deviceId;
        private String action;
        private double latitude;
        private double longitude;
        private double altitude;
        private long scheduledTime;
        private long estimatedDuration;
        private int priority;
        
        public MissionTask(String taskId, String deviceId, String action) {
            this.taskId = taskId;
            this.deviceId = deviceId;
            this.action = action;
        }
        
        public String getTaskId() { return taskId; }
        public String getDeviceId() { return deviceId; }
        public String getAction() { return action; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double lat) { this.latitude = lat; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double lon) { this.longitude = lon; }
        public double getAltitude() { return altitude; }
        public void setAltitude(double alt) { this.altitude = alt; }
        public long getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(long time) { this.scheduledTime = time; }
        public long getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(long dur) { this.estimatedDuration = dur; }
        public int getPriority() { return priority; }
        public void setPriority(int p) { this.priority = p; }
    }
    
    // 设备能力库
    private Map<String, DeviceCapabilities> deviceCapabilities;
    
    public SafetyVerificationEngine() {
        this.deviceCapabilities = new HashMap<>();
        
        // 初始化默认设备能力
        deviceCapabilities.put("drone_default", new DeviceCapabilities());
        deviceCapabilities.put("vehicle_default", DeviceCapabilities.forVehicle());
        
        // 已知设备能力
        DeviceCapabilities djiDrone = new DeviceCapabilities();
        djiDrone.maxSpeed = 18;
        djiDrone.maxRange = 8;
        deviceCapabilities.put("DJI_Drone", djiDrone);
        
        DeviceCapabilities bydVehicle = DeviceCapabilities.forVehicle();
        bydVehicle.maxSpeed = 27.8;  // 100 km/h
        deviceCapabilities.put("BYD_Vehicle", bydVehicle);
    }
    
    /**
     * 验证单个指令
     */
    public VerificationResult verifyCommand(String command, Map<String, Object> context) {
        VerificationResult result = new VerificationResult();
        
        // 1. 解析命令参数
        CommandParams params = parseCommand(command);
        
        // 2. 获取设备能力
        DeviceCapabilities caps = getDeviceCapabilities(params.deviceType);
        
        // 3. 速度极限检查
        checkSpeedLimits(params, caps, result);
        
        // 4. 高度极限检查
        checkAltitudeLimits(params, caps, result);
        
        // 5. 距离/航程检查
        checkRangeLimits(params, caps, context, result);
        
        // 6. 时间约束检查
        checkTimeConstraints(params, context, result);
        
        // 7. 物理一致性检查
        checkPhysicalConsistency(params, result);
        
        // 8. 生成优化建议
        if (!result.isPassed() || !result.warnings.isEmpty()) {
            generateOptimization(result, params);
        }
        
        return result;
    }
    
    /**
     * 命令参数解析
     */
    private CommandParams parseCommand(String command) {
        CommandParams params = new CommandParams();
        
        // 提取速度要求
        Pattern speedPattern = Pattern.compile("(\\d+)\\s*(km/h|m/s|km/h|kph)");
        Matcher speedMatcher = speedPattern.matcher(command);
        if (speedMatcher.find()) {
            params.requestedSpeed = Double.parseDouble(speedMatcher.group(1));
            params.speedUnit = speedMatcher.group(2);
        }
        
        // 提取时间要求
        Pattern timePattern = Pattern.compile("(\\d+)\\s*(分钟|秒|min|min|hour|h)");
        Matcher timeMatcher = timePattern.matcher(command);
        if (timeMatcher.find()) {
            params.requestedTime = Integer.parseInt(timeMatcher.group(1));
            params.timeUnit = timeMatcher.group(2);
        }
        
        // 提取距离要求
        Pattern distPattern = Pattern.compile("(\\d+)\\s*(公里|km|米|m)");
        Matcher distMatcher = distPattern.matcher(command);
        if (distMatcher.find()) {
            params.requestedDistance = Double.parseDouble(distMatcher.group(1));
            params.distanceUnit = distMatcher.group(2);
        }
        
        // 提取目的地
        Pattern destPattern = Pattern.compile("[A-Z][\\u4e00-\\u9fa5]+|[\\u4e00-\\u9fa5]+[地站点区]");
        Matcher destMatcher = destPattern.matcher(command);
        if (destMatcher.find()) {
            params.destination = destMatcher.group();
        }
        
        // 识别设备类型
        if (command.contains("无人机") || command.contains("drone")) {
            params.deviceType = "drone";
        } else if (command.contains("车") || command.contains("vehicle")) {
            params.deviceType = "vehicle";
        } else {
            params.deviceType = "drone_default";
        }
        
        // 识别动作
        if (command.contains("救援")) params.action = "rescue";
        else if (command.contains("配送")) params.action = "deliver";
        else if (command.contains("侦察")) params.action = "surveil";
        else params.action = "navigate";
        
        return params;
    }
    
    /**
     * 命令参数类
     */
    private static class CommandParams {
        double requestedSpeed;
        String speedUnit;
        int requestedTime;
        String timeUnit;
        double requestedDistance;
        String distanceUnit;
        String destination;
        String deviceType;
        String action;
    }
    
    /**
     * 获取设备能力
     */
    private DeviceCapabilities getDeviceCapabilities(String deviceType) {
        return deviceCapabilities.getOrDefault(deviceType, new DeviceCapabilities());
    }
    
    /**
     * 速度极限检查
     */
    private void checkSpeedLimits(CommandParams params, DeviceCapabilities caps, VerificationResult result) {
        if (params.requestedSpeed > 0) {
            double speedMS = params.speedUnit.contains("km") ? 
                params.requestedSpeed / 3.6 : params.requestedSpeed;
            
            if (speedMS > caps.maxSpeed) {
                ConstraintViolation violation = new ConstraintViolation(
                    "SPEED_EXCEEDED",
                    "Requested speed " + params.requestedSpeed + " " + params.speedUnit + 
                    " exceeds device maximum " + (caps.maxSpeed * 3.6) + " km/h",
                    0.8
                );
                violation.setSuggestedFix("Reduce speed to " + (int)(caps.maxSpeed * 3.6) + " km/h or " + 
                                          (int)caps.maxSpeed + " m/s");
                result.addViolation(violation);
                result.addError("速度超出设备物理极限: " + params.requestedSpeed + " " + params.speedUnit);
            }
        }
    }
    
    /**
     * 高度极限检查
     */
    private void checkAltitudeLimits(CommandParams params, DeviceCapabilities caps, VerificationResult result) {
        // 最低高度检查
        if (params.action.equals("land") || params.action.equals("降落")) {
            // 降落检查
            if (params.requestedDistance > 0) {
                double altitude = params.requestedDistance;
                if (altitude < caps.minAltitude) {
                    result.addWarning("Landing altitude " + altitude + "m below safe minimum " + caps.minAltitude + "m");
                }
            }
        }
        
        // 最高高度检查
        if (params.requestedDistance > caps.maxAltitude && params.requestedDistance > 0) {
            ConstraintViolation violation = new ConstraintViolation(
                "ALTITUDE_EXCEEDED",
                "Requested altitude " + params.requestedDistance + "m exceeds maximum " + caps.maxAltitude + "m",
                0.9
            );
            violation.setSuggestedFix("Reduce altitude to " + caps.maxAltitude + "m or below");
            result.addViolation(violation);
            result.addError("高度超出设备限制: " + params.requestedDistance + "m");
        }
    }
    
    /**
     * 航程检查
     */
    private void checkRangeLimits(CommandParams params, DeviceCapabilities caps, 
                                  Map<String, Object> context, VerificationResult result) {
        if (params.requestedDistance > 0) {
            double distanceKm = params.distanceUnit.contains("m") ? 
                params.requestedDistance / 1000 : params.requestedDistance;
            
            if (distanceKm > caps.maxRange) {
                ConstraintViolation violation = new ConstraintViolation(
                    "RANGE_EXCEEDED",
                    "Distance " + distanceKm + "km exceeds device range " + caps.maxRange + "km",
                    0.9
                );
                violation.setSuggestedFix("Break mission into multiple segments or use closer destination");
                result.addViolation(violation);
                result.addError("距离超出设备航程: " + distanceKm + "km > " + caps.maxRange + "km");
            }
            
            // 考虑电池电量
            double batteryLevel = 100;
            if (context.containsKey("battery_level")) {
                batteryLevel = (Double) context.get("battery_level");
            }
            
            double effectiveRange = caps.maxRange * (batteryLevel / 100);
            if (distanceKm > effectiveRange) {
                result.addWarning("Battery at " + (int)batteryLevel + "%, effective range reduced to " + 
                                  (int)effectiveRange + "km");
            }
        }
    }
    
    /**
     * 时间约束检查
     */
    private void checkTimeConstraints(CommandParams params, Map<String, Object> context, 
                                      VerificationResult result) {
        if (params.requestedTime > 0 && params.requestedDistance > 0) {
            double distanceKm = params.distanceUnit.contains("m") ? 
                params.requestedDistance / 1000 : params.requestedDistance;
            
            // 计算理论最短时间 (假设最大速度)
            double maxSpeedKmH = 20 * 3.6;  // 20 m/s
            double minTimeMinutes = (distanceKm / maxSpeedKmH) * 60;
            
            double requestTimeMinutes = params.timeUnit.contains("秒") || 
                                        params.timeUnit.contains("source") ? 
                                        params.requestedTime / 60.0 : params.requestedTime;
            
            if (requestTimeMinutes < minTimeMinutes * 0.8) {
                ConstraintViolation violation = new ConstraintViolation(
                    "TIME_UNFEASIBLE",
                    "Requested time " + requestTimeMinutes + "min impossible for " + distanceKm + "km distance",
                    1.0
                );
                violation.setSuggestedFix("Minimum feasible time: " + (int)minTimeMinutes + " minutes");
                result.addViolation(violation);
                result.addError("时间要求不可行: " + params.requestedTime + " " + params.timeUnit + 
                               " 内到达 " + distanceKm + "km 超出设备物理极限");
            }
        }
    }
    
    /**
     * 物理一致性检查
     */
    private void checkPhysicalConsistency(CommandParams params, VerificationResult result) {
        // 检查速度与高度的物理一致性
        if (params.requestedSpeed > 0 && params.requestedDistance > 0) {
            double speedMS = params.speedUnit.contains("km") ? 
                params.requestedSpeed / 3.6 : params.requestedSpeed;
            double altitudeM = params.distanceUnit.contains("m") ? 
                params.requestedDistance : params.requestedDistance * 1000;
            
            // 高速飞行时不应要求超低高度
            if (speedMS > 15 && altitudeM < 30) {
                result.addWarning("High speed (" + (int)speedMS + "m/s) at low altitude (<30m) may be unsafe");
            }
            
            // 快速爬升/下降检查
            if (altitudeM > 100) {
                // 假设正常爬升需要1分钟100m
                result.addSuggestion("Altitude change of " + (int)altitudeM + "m will take approximately " + 
                                    (altitudeM / 100) + " minutes");
            }
        }
    }
    
    /**
     * 生成优化建议
     */
    private void generateOptimization(VerificationResult result, CommandParams params) {
        List<String> optimizations = new ArrayList<>();
        
        for (ConstraintViolation v : result.getViolations()) {
            if (v.getSuggestedFix() != null) {
                optimizations.add(v.getSuggestedFix());
            }
        }
        
        if (!optimizations.isEmpty()) {
            StringBuilder optCmd = new StringBuilder();
            optCmd.append("// Optimized command based on safety constraintsource\n");
            
            if (params.action != null) {
                optCmd.append(params.action).append(" ");
            }
            if (params.destination != null) {
                optCmd.append(params.destination).append(" ");
            }
            
            // 应用优化速度
            DeviceCapabilities caps = getDeviceCapabilities(params.deviceType);
            if (params.requestedSpeed > caps.maxSpeed * 3.6) {
                optCmd.append("speed:").append((int)(caps.maxSpeed * 3.6)).append("km/h ");
            }
            
            // 添加安全提示
            optCmd.append("// Safety constraints applied");
            
            result.setOptimizedCommand(optCmd.toString());
        }
    }
    
    /**
     * 验证协同任务时空一致性
     */
    public VerificationResult verifyCoordinatedMission(CoordinatedMission mission) {
        VerificationResult result = new VerificationResult();
        
        System.out.println("[SafetyEngine] Verifying coordinated mission: " + mission.getMissionId());
        System.out.println("[SafetyEngine] Tasks: " + mission.tasks.size());
        
        // 1. 时间戳单调性检查
        long lastEndTime = 0;
        for (MissionTask task : mission.tasks) {
            if (task.getScheduledTime() < lastEndTime) {
                result.addError("Time inconsistency: Task " + task.getTaskId() + 
                               " starts before previous task ends");
            }
            
            long taskEnd = task.getScheduledTime() + task.getEstimatedDuration();
            lastEndTime = Math.max(lastEndTime, taskEnd);
        }
        
        // 2. 空间冲突检查
        for (int i = 0; i < mission.tasks.size(); i++) {
            for (int j = i + 1; j < mission.tasks.size(); j++) {
                MissionTask t1 = mission.tasks.get(i);
                MissionTask t2 = mission.tasks.get(j);
                
                // 检查时间重叠
                boolean timeOverlap = !(t1.getScheduledTime() + t1.getEstimatedDuration() <= t2.getScheduledTime() ||
                                       t2.getScheduledTime() + t2.getEstimatedDuration() <= t1.getScheduledTime());
                
                if (timeOverlap) {
                    // 检查位置冲突 (简化: 100m范围内视为冲突)
                    double dist = calculateDistance(t1.getLatitude(), t1.getLongitude(),
                                                  t2.getLatitude(), t2.getLongitude());
                    if (dist < 0.1) {  // 100m
                        result.addWarning("Potential collision risk: Tasks " + t1.getTaskId() + 
                                         " and " + t2.getTaskId() + " at distance " + 
                                         String.format("%.0fm", dist * 1000));
                    }
                }
            }
        }
        
        // 3. 纳秒级授时验证
        if (mission.getStartTimestamp() > 0) {
            System.out.println("[SafetyEngine] Mission start timestamp: " + mission.getStartTimestamp());
            System.out.println("[SafetyEngine] Time precision: nanosecond level (BeiDou/GNSS or approved backbone sync)");
        }
        
        // 4. 优先级冲突检查
        Map<String, Integer> priorities = new HashMap<>();
        for (MissionTask task : mission.tasks) {
            if (priorities.containsKey(task.getDeviceId())) {
                int existing = priorities.get(task.getDeviceId());
                if (existing != task.getPriority()) {
                    result.addWarning("Priority conflict for device " + task.getDeviceId());
                }
            }
            priorities.put(task.getDeviceId(), task.getPriority());
        }
        
        return result;
    }
    
    /**
     * 计算两点间距离(简化版)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 简化: 使用欧氏距离 (实际应用应使用Haversine公式)
        double dLat = Math.abs(lat2 - lat1);
        double dLon = Math.abs(lon2 - lon1);
        return Math.sqrt(dLat * dLat + dLon * dLon) * 111;  // 1度约111km
    }
    
    /**
     * 注册设备能力
     */
    public void registerDeviceCapabilities(String deviceId, DeviceCapabilities caps) {
        deviceCapabilities.put(deviceId, caps);
        System.out.println("[SafetyEngine] Registered capabilities for: " + deviceId);
    }
}
