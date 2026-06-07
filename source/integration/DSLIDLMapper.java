package integration;

import model.*;
import java.util.*;

/**
 * DSL-IDL 映射器
 * 实现从 DSL 领域模型到 IDL 类型的自动转换
 * 
 * 映射关系表：
 * ┌─────────────┬─────────────────────────────────────────────────┐
 * │ DSL 类型     │ IDL 类型                                        │
 * ├─────────────┼─────────────────────────────────────────────────┤
 * │ Task        │ chongqing.MissionCommand / urban.SwarmMission   │
 * │ PathPlan    │ chongqing.Position3D[] (航点序列)                │
 * │ State       │ chongqing.EmergencyStatus / urban.VehicleData   │
 * └─────────────┴─────────────────────────────────────────────────┘
 * 
 * @paper_ref 第3章 DSL设计与实现 -> 3.3 语义映射机制
 */
public class DSLIDLMapper {
    
    // 单例实例
    private static DSLIDLMapper instance;
    
    // 任务类型到 IDL MissionType 的映射
    private static final Map<String, MissionType> TASK_TYPE_MAP = new HashMap<>();
    
    // DSL 字段名到 IDL 字段名的映射
    private static final Map<String, String> FIELD_NAME_MAP = new HashMap<>();
    
    // 状态名到 IDL 类型名的映射
    private static final Map<String, String> STATE_TYPE_MAP = new HashMap<>();
    
    static {
        // 初始化任务类型映射
        TASK_TYPE_MAP.put("delivery", MissionType.MEDICAL_SUPPLY_DELIVERY);
        TASK_TYPE_MAP.put("logistics", MissionType.MEDICAL_SUPPLY_DELIVERY);
        TASK_TYPE_MAP.put("配送", MissionType.MEDICAL_SUPPLY_DELIVERY);
        TASK_TYPE_MAP.put("rescue", MissionType.EMERGENCY_RESCUE);
        TASK_TYPE_MAP.put("emergency", MissionType.EMERGENCY_RESCUE);
        TASK_TYPE_MAP.put("应急", MissionType.EMERGENCY_RESCUE);
        TASK_TYPE_MAP.put("patrol", MissionType.SURVEILLANCE_PATROL);
        TASK_TYPE_MAP.put("surveillance", MissionType.SURVEILLANCE_PATROL);
        TASK_TYPE_MAP.put("monitoring", MissionType.SURVEILLANCE_PATROL);
        TASK_TYPE_MAP.put("侦察", MissionType.SURVEILLANCE_PATROL);
        TASK_TYPE_MAP.put("巡逻", MissionType.SURVEILLANCE_PATROL);
        TASK_TYPE_MAP.put("cargo", MissionType.CARGO_TRANSPORT);
        TASK_TYPE_MAP.put("transport", MissionType.CARGO_TRANSPORT);
        TASK_TYPE_MAP.put("运输", MissionType.CARGO_TRANSPORT);
        
        // 初始化字段名映射 (snake_case -> camelCase)
        FIELD_NAME_MAP.put("drone_id", "drone_id");
        FIELD_NAME_MAP.put("device_type", "device_type");
        FIELD_NAME_MAP.put("mission_type", "mission_type");
        FIELD_NAME_MAP.put("current_position", "current_position");
        FIELD_NAME_MAP.put("target_position", "target_position");
        FIELD_NAME_MAP.put("battery_level", "battery_level");
        FIELD_NAME_MAP.put("battery_status", "battery_level");
        FIELD_NAME_MAP.put("power_consumption", "power_consumption");
        FIELD_NAME_MAP.put("payload_weight", "payload_weight");
        FIELD_NAME_MAP.put("payload", "payload_weight");
        FIELD_NAME_MAP.put("cargo_type", "cargo_type");
        FIELD_NAME_MAP.put("priority", "priority");
        FIELD_NAME_MAP.put("status", "status");
        FIELD_NAME_MAP.put("speed", "speed");
        FIELD_NAME_MAP.put("heading", "heading");
        FIELD_NAME_MAP.put("altitude", "altitude");
        FIELD_NAME_MAP.put("latitude", "latitude");
        FIELD_NAME_MAP.put("longitude", "longitude");
        FIELD_NAME_MAP.put("count", "team_size");
        FIELD_NAME_MAP.put("drone_count", "team_size");
        FIELD_NAME_MAP.put("drone_types", "drone_ids");
        FIELD_NAME_MAP.put("waypoints", "waypoints");
        FIELD_NAME_MAP.put("start", "start_location");
        FIELD_NAME_MAP.put("end", "end_location");
        FIELD_NAME_MAP.put("route_type", "route_type");
        FIELD_NAME_MAP.put("obstacle_avoidance", "obstacle_avoidance");
        FIELD_NAME_MAP.put("vehicle_id", "vehicle_id");
        FIELD_NAME_MAP.put("vehicle_type", "vehicle_type");
        FIELD_NAME_MAP.put("location", "location");
        FIELD_NAME_MAP.put("network_id", "network_id");
        FIELD_NAME_MAP.put("protocols", "protocols");
        FIELD_NAME_MAP.put("latency", "latency");
        FIELD_NAME_MAP.put("bandwidth", "bandwidth");
        FIELD_NAME_MAP.put("temperature", "temperature");
        FIELD_NAME_MAP.put("humidity", "humidity");
        FIELD_NAME_MAP.put("wind_speed", "wind_speed");
        FIELD_NAME_MAP.put("visibility", "visibility");
        FIELD_NAME_MAP.put("fog_density", "fog_density");
        FIELD_NAME_MAP.put("alert_status", "alert_status");
        
        // 初始化状态类型映射
        STATE_TYPE_MAP.put("droneFleet", "DroneFleetStatus");
        STATE_TYPE_MAP.put("drone_fleet", "DroneFleetStatus");
        STATE_TYPE_MAP.put("fogWeather", "FogWeatherStatus");
        STATE_TYPE_MAP.put("fog_weather", "FogWeatherStatus");
        STATE_TYPE_MAP.put("deliveryVehicle", "VehicleData");
        STATE_TYPE_MAP.put("delivery_vehicle", "VehicleData");
        STATE_TYPE_MAP.put("communicationNetwork", "NetworkStatus");
        STATE_TYPE_MAP.put("communication_network", "NetworkStatus");
    }
    
    private DSLIDLMapper() {}
    
    public static synchronized DSLIDLMapper getInstance() {
        if (instance == null) {
            instance = new DSLIDLMapper();
        }
        return instance;
    }
    
    /**
     * 将 Mission 映射为 IDL 命令列表
     * @param mission DSL 任务领域模型
     * @return IDL 命令对象列表
     */
    public List<IDLMissionCommand> mapMissionToCommands(Mission mission) {
        List<IDLMissionCommand> commands = new ArrayList<>();
        
        // 映射每个 Task 为 MissionCommand
        for (Task task : mission.getTasks()) {
            IDLMissionCommand cmd = mapTaskToCommand(task, mission.getName());
            commands.add(cmd);
        }
        
        return commands;
    }
    
    /**
     * 将 Task 映射为 MissionCommand
     * @param task DSL 任务
     * @param missionName 任务所属场景名
     * @return IDL MissionCommand
     */
    public IDLMissionCommand mapTaskToCommand(Task task, String missionName) {
        IDLMissionCommand cmd = new IDLMissionCommand();
        
        // 生成命令ID
        cmd.setCommandId(generateId("CMD"));
        cmd.setDroneId(extractDroneId(task));
        cmd.setMissionType(extractMissionType(task));
        cmd.setPriority(extractPriority(task));
        cmd.setSourceSystem("DSL-Mapper");
        cmd.setValidUntil(System.currentTimeMillis() / 1000 + 3600); // 1小时后过期
        
        // 转换参数为 JSON 格式
        cmd.setParameters(mapFieldsToJson(task.getFields()));
        
        return cmd;
    }
    
    /**
     * 将 PathPlan 映射为航点序列
     * @param path DSL 路径规划
     * @return IDL 航点列表
     */
    public List<IDLPosition3D> mapPathToWaypoints(PathPlan path) {
        List<IDLPosition3D> waypoints = new ArrayList<>();
        Map<String, String> fields = path.getFields();
        
        // 从航点字段解析坐标
        String waypointsStr = fields.get("waypoints");
        if (waypointsStr != null) {
            String[] wpList = waypointsStr.split(",");
            for (int i = 0; i < wpList.length; i++) {
                IDLPosition3D pos = new IDLPosition3D();
                // 模拟坐标生成（实际应从地理编码服务获取）
                pos.setLatitude(29.5 + i * 0.01);
                pos.setLongitude(106.5 + i * 0.01);
                pos.setAltitude(parseAltitude(fields.get("altitude_profile")));
                waypoints.add(pos);
            }
        }
        
        return waypoints;
    }
    
    /**
     * 将 State 映射为 EmergencyStatus
     * @param state DSL 状态
     * @param missionName 所属场景名
     * @return IDL EmergencyStatus 或其子类
     */
    public IDLEmergencyStatus mapStateToStatus(State state, String missionName) {
        IDLEmergencyStatus status = new IDLEmergencyStatus();
        
        status.setDroneId(extractDroneId(state));
        status.setDeviceType(extractDeviceType(state));
        status.setMissionType(extractMissionType(state));
        status.setStatus(extractStatusCode(state));
        status.setPriority(extractPriority(state));
        
        // 映射位置信息
        IDLPosition3D position = new IDLPosition3D();
        position.setLatitude(parseDouble(state.getFields().get("latitude"), 29.55));
        position.setLongitude(parseDouble(state.getFields().get("longitude"), 106.55));
        position.setAltitude(parseDouble(state.getFields().get("altitude"), 100.0));
        status.setCurrentPosition(position);
        
        // 映射电池/电量
        String batteryField = cleanValue(state.getFields().get("battery_level"));
        if (batteryField.isEmpty()) {
            batteryField = cleanValue(state.getFields().get("battery_status"));
        }
        status.setBatteryLevel(parsePercentage(batteryField));
        
        // 映射载荷
        String payload = cleanValue(state.getFields().get("payload"));
        if (payload != null && !payload.isEmpty()) {
            status.setPayloadWeight(parseWeight(payload));
            status.setCargoType(extractCargoType(state));
        }
        
        // 时间戳
        long timestamp = System.currentTimeMillis() / 1000;
        status.setTimestampSec(timestamp);
        status.setTimestampUsec((int)((System.currentTimeMillis() % 1000) * 1000));
        
        return status;
    }
    
    /**
     * 将 Mission 映射为 SwarmMission (无人机群协同)
     * @param mission DSL 任务领域模型
     * @return IDL SwarmMission
     */
    public IDLSwarmMission mapMissionToSwarmMission(Mission mission) {
        IDLSwarmMission swarm = new IDLSwarmMission();
        
        swarm.setMissionId(generateId("SWARM"));
        swarm.setMissionType(mission.getName());
        
        // 从状态中提取集群信息
        for (State state : mission.getStates()) {
            if (state.getName().toLowerCase().contains("drone") 
                || state.getName().toLowerCase().contains("fleet")) {
                
                Map<String, String> fields = state.getFields();
                
                // 提取集群规模
                String countStr = cleanValue(fields.get("count"));
                if (countStr.isEmpty()) {
                    countStr = cleanValue(fields.get("drone_count"));
                }
                swarm.setTeamSize(parseInt(countStr, 5));
                
                // 提取领队无人机
                String fleetId = cleanValue(fields.get("fleet_id"));
                if (!fleetId.isEmpty()) {
                    swarm.setLeaderDroneId(fleetId);
                }
                
                // 提取无人机ID列表
                String droneTypes = cleanValue(fields.get("drone_types"));
                if (!droneTypes.isEmpty()) {
                    String[] types = droneTypes.split(",");
                    // 清理每个类型
                    for (int i = 0; i < types.length; i++) {
                        types[i] = types[i].trim();
                    }
                    swarm.setDroneIds(Arrays.asList(types));
                }
                
                break;
            }
        }
        
        // 从路径规划中提取航点
        for (PathPlan path : mission.getPaths()) {
            String waypoints = path.getFields().get("waypoints");
            if (waypoints != null) {
                swarm.setWaypoints("[" + waypoints + "]");
                break;
            }
        }
        
        swarm.setTimestamp(System.currentTimeMillis() / 1000);
        
        return swarm;
    }
    
    /**
     * 将 Mission 映射为 VehicleData (车机协同)
     * @param mission DSL 任务领域模型
     * @return IDL VehicleData 列表
     */
    public List<IDLVehicleData> mapMissionToVehicleData(Mission mission) {
        List<IDLVehicleData> vehicles = new ArrayList<>();
        
        for (State state : mission.getStates()) {
            if (state.getName().toLowerCase().contains("vehicle") 
                || state.getName().toLowerCase().contains("delivery")) {
                
                IDLVehicleData vehicle = new IDLVehicleData();
                Map<String, String> fields = state.getFields();
                
                vehicle.setVehicleId(cleanValue(fields.get("vehicle_id")));
                vehicle.setLatitude(parseDouble(fields.get("latitude"), 29.55));
                vehicle.setLongitude(parseDouble(fields.get("longitude"), 106.55));
                vehicle.setSpeed(parseSpeed(cleanValue(fields.get("speed"))));
                vehicle.setBatteryLevel(parseBattery(fields.get("battery_level")));
                vehicle.setDriveMode(cleanValue(fields.get("vehicle_type")));
                vehicle.setTimestamp(System.currentTimeMillis() / 1000);
                
                vehicles.add(vehicle);
            }
        }
        
        return vehicles;
    }
    
    /**
     * 将 Mission 映射为 EmergencyAlert (应急预警)
     * @param mission DSL 任务领域模型
     * @return IDL EmergencyAlert 列表
     */
    public List<IDLEmergencyAlert> mapMissionToAlerts(Mission mission) {
        List<IDLEmergencyAlert> alerts = new ArrayList<>();
        
        for (State state : mission.getStates()) {
            if (state.getName().toLowerCase().contains("weather") 
                || state.getName().toLowerCase().contains("fog")
                || state.getName().toLowerCase().contains("alert")) {
                
                IDLEmergencyAlert alert = new IDLEmergencyAlert();
                Map<String, String> fields = state.getFields();
                
                alert.setAlertId(generateAlertId());
                alert.setLevel(extractAlertLevel(fields.get("alert_status")));
                alert.setAlertType("weather_disaster");
                alert.setLatitude(parseDouble(fields.get("latitude"), 29.55));
                alert.setLongitude(parseDouble(fields.get("longitude"), 106.55));
                alert.setAffectedRadius(parseDouble(fields.get("visibility"), 500));
                alert.setRecommendation(generateRecommendation(fields));
                alert.setTimestamp(System.currentTimeMillis() / 1000);
                alert.setExpireTime(alert.getTimestamp() + 3600);
                
                alerts.add(alert);
            }
        }
        
        return alerts;
    }
    
    // ==================== 辅助方法 ====================
    
    private String generateId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" 
               + String.format("%04d", new Random().nextInt(10000));
    }
    
    private long generateAlertId() {
        return System.currentTimeMillis() % 100000;
    }
    
    private String extractDroneId(AbstractDSLEntity entity) {
        Map<String, String> fields = entity.getFields();
        String droneId = fields.get("drone_id");
        if (droneId == null) {
            droneId = fields.get("fleet_id");
        }
        if (droneId == null) {
            droneId = "DRONE-" + generateId("");
        }
        return droneId;
    }
    
    private MissionType extractMissionType(AbstractDSLEntity entity) {
        AnnotationInfo annotation = entity.getAnnotation();
        if (annotation != null) {
            String name = annotation.getName();
            String taskName = annotation.getFields().get("name");
            
            // 尝试从注解名称匹配
            for (Map.Entry<String, MissionType> entry : TASK_TYPE_MAP.entrySet()) {
                if ((name != null && name.toLowerCase().contains(entry.getKey()))
                    || (taskName != null && taskName.toLowerCase().contains(entry.getKey()))) {
                    return entry.getValue();
                }
            }
        }
        
        // 从实体名称推断
        String entityName = entity.getName().toLowerCase();
        if (entityName.contains("delivery") || entityName.contains("配送")) {
            return MissionType.MEDICAL_SUPPLY_DELIVERY;
        } else if (entityName.contains("emergency") || entityName.contains("rescue")) {
            return MissionType.EMERGENCY_RESCUE;
        } else if (entityName.contains("monitor") || entityName.contains("patrol")) {
            return MissionType.SURVEILLANCE_PATROL;
        }
        
        return MissionType.MEDICAL_SUPPLY_DELIVERY; // 默认
    }
    
    private DeviceType extractDeviceType(State state) {
        String name = state.getName().toLowerCase();
        if (name.contains("vehicle") || name.contains("delivery")) {
            return DeviceType.VEHICLE_CAR;
        } else if (name.contains("drone") || name.contains("fleet")) {
            return DeviceType.DRONE_MULTIROTOR;
        }
        return DeviceType.DRONE_VTOL;
    }
    
    private StatusCode extractStatusCode(State state) {
        String status = state.getFields().get("status");
        if (status == null) return StatusCode.IDLE;
        
        status = status.toUpperCase();
        if (status.contains("ACTIVE") || status.contains("IN_TRANSIT")) {
            return StatusCode.IN_TRANSIT;
        } else if (status.contains("ARRIVED")) {
            return StatusCode.ARRIVED;
        } else if (status.contains("EMERGENCY")) {
            return StatusCode.EMERGENCY;
        } else if (status.contains("ERROR")) {
            return StatusCode.ERROR;
        }
        return StatusCode.IDLE;
    }
    
    private int extractPriority(AbstractDSLEntity entity) {
        AnnotationInfo annotation = entity.getAnnotation();
        if (annotation != null) {
            String priority = annotation.getFields().get("priority");
            if (priority != null) {
                try {
                    return Integer.parseInt(priority);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        return 5; // 默认优先级
    }
    
    private AlertLevel extractAlertLevel(String alertStatus) {
        if (alertStatus == null) return AlertLevel.LOW;
        
        alertStatus = alertStatus.toUpperCase();
        if (alertStatus.contains("CRITICAL") || alertStatus.contains("RED")) {
            return AlertLevel.CRITICAL;
        } else if (alertStatus.contains("HIGH") || alertStatus.contains("ORANGE")) {
            return AlertLevel.HIGH;
        } else if (alertStatus.contains("MEDIUM") || alertStatus.contains("YELLOW")) {
            return AlertLevel.MEDIUM;
        }
        return AlertLevel.LOW;
    }
    
    private String mapFieldsToJson(Map<String, String> fields) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    private double parseDouble(String value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            // 移除单位
            String numStr = value.replaceAll("[^0-9.-]", "");
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            // 直接提取数字
            String numStr = value.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) return defaultValue;
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private double parsePercentage(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            // 直接提取数字
            String numStr = value.replaceAll("[^0-9.]", "");
            if (numStr.isEmpty()) return 0;
            double val = Double.parseDouble(numStr);
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 清理 DSL 解析的值（去除引号等）
     */
    private String cleanValue(String value) {
        if (value == null) return "";
        // 去除首尾空白
        String cleaned = value.trim();
        
        // 处理 \"85%\" 格式（转义引号）
        if (cleaned.startsWith("\\\"") && cleaned.endsWith("\\\"")) {
            cleaned = cleaned.substring(2, cleaned.length() - 2);
        }
        // 处理 "85%" 格式（普通引号）
        else if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                 || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        return cleaned.trim();
    }
    
    /**
     * 解析电池电量值（处理百分比格式）
     */
    private int parseBattery(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            // 清理值
            String cleaned = cleanValue(value);
            // 移除 % 符号
            cleaned = cleaned.replace("%", "").trim();
            // 提取数字
            String numStr = cleaned.replaceAll("[^0-9.]", "");
            if (numStr.isEmpty()) return 0;
            // 如果有小数点，先转 double 再转 int（保留数值）
            if (numStr.contains(".")) {
                return (int) Double.parseDouble(numStr);
            }
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double parseWeight(String value) {
        if (value == null) return 0;
        try {
            String numStr = value.replaceAll("[^0-9.]", "");
            double val = Double.parseDouble(numStr);
            // 转换为 kg（假设输入已经是kg或需要转换）
            if (value.toLowerCase().contains("kg")) {
                return val;
            } else if (value.toLowerCase().contains("generated-src")) {
                return val / 1000;
            } else if (value.toLowerCase().contains("lb")) {
                return val * 0.453592;
            }
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double parseSpeed(String value) {
        if (value == null) return 0;
        try {
            String numStr = value.replaceAll("[^0-9.]", "");
            double val = Double.parseDouble(numStr);
            // 转换为 m/s
            if (value.toLowerCase().contains("km/h") || value.toLowerCase().contains("kmh")) {
                return val / 3.6;
            } else if (value.toLowerCase().contains("mph")) {
                return val * 0.44704;
            }
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private double parseAltitude(String value) {
        if (value == null) return 100;
        try {
            // 安全检查：确保值包含 "-" 且分割后至少有 2 个元素
            if (value.contains("-")) {
                String[] parts = value.split("-");
                if (parts.length >= 2) {
                    // 范围格式 "200-500m"
                    double low = Double.parseDouble(parts[0].replaceAll("[^0-9.]", ""));
                    double high = Double.parseDouble(parts[1].replaceAll("[^0-9.]", ""));
                    return (low + high) / 2;
                }
            }
            // 单值格式或已转换的格式（如 "0.200 km"）
            String numStr = value.replaceAll("[^0-9.]", "");
            if (!numStr.isEmpty()) {
                return Double.parseDouble(numStr);
            }
            return 100;
        } catch (NumberFormatException e) {
            return 100;
        }
    }
    
    private String extractCargoType(State state) {
        Map<String, String> fields = state.getFields();
        if (fields.containsKey("cargo_type")) {
            return fields.get("cargo_type");
        }
        // 从任务名推断
        String name = state.getName().toLowerCase();
        if (name.contains("medical") || name.contains("医疗")) {
            return "medical_supplies";
        } else if (name.contains("cargo")) {
            return "general_cargo";
        }
        return "unknown";
    }
    
    private String generateRecommendation(Map<String, String> fields) {
        StringBuilder rec = new StringBuilder();
        
        String fogDensity = fields.get("fog_density");
        String visibility = fields.get("visibility");
        String windSpeed = fields.get("wind_speed");
        
        if (fogDensity != null) {
            double fog = parseDouble(fogDensity, 0);
            if (fog > 0.7) {
                rec.append("高雾区 - 建议启用低能见度飞行模式; ");
            }
        }
        
        if (visibility != null) {
            double vis = parseDouble(visibility, 1000);
            if (vis < 500) {
                rec.append("能见度低于500m - 启用视觉辅助系统; ");
            }
        }
        
        if (windSpeed != null) {
            rec.append("监测风速变化 - 及时调整飞行高度; ");
        }
        
        if (rec.length() == 0) {
            rec.append("继续执行任务监控");
        }
        
        return rec.toString();
    }
    
    // ==================== 内部类：IDL 类型模拟 ====================
    
    /**
     * IDL MissionType 枚举
     */
    public enum MissionType {
        MEDICAL_SUPPLY_DELIVERY,
        EMERGENCY_RESCUE,
        SURVEILLANCE_PATROL,
        CARGO_TRANSPORT
    }
    
    /**
     * IDL DeviceType 枚举
     */
    public enum DeviceType {
        DRONE_VTOL,
        DRONE_MULTIROTOR,
        VEHICLE_CAR,
        VEHICLE_TRUCK
    }
    
    /**
     * IDL StatusCode 枚举
     */
    public enum StatusCode {
        IDLE,
        MISSION_PLANNED,
        IN_TRANSIT,
        ARRIVED,
        RETURNING,
        EMERGENCY,
        ERROR
    }
    
    /**
     * IDL AlertLevel 枚举
     */
    public enum AlertLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * IDL Position3D 结构
     */
    public static class IDLPosition3D {
        private double latitude;
        private double longitude;
        private double altitude;
        
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getAltitude() { return altitude; }
        public void setAltitude(double altitude) { this.altitude = altitude; }
        
        @Override
        public String toString() {
            return String.format("Position3D(lat=%.6f, lon=%.6f, alt=%.1fm)", 
                                latitude, longitude, altitude);
        }
    }
    
    /**
     * IDL MissionCommand 结构
     */
    public static class IDLMissionCommand {
        private String commandId;
        private String droneId;
        private MissionType missionType;
        private String parameters;
        private int priority;
        private long validUntil;
        private String sourceSystem;
        private String operatorId;
        
        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        public String getDroneId() { return droneId; }
        public void setDroneId(String droneId) { this.droneId = droneId; }
        public MissionType getMissionType() { return missionType; }
        public void setMissionType(MissionType missionType) { this.missionType = missionType; }
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public long getValidUntil() { return validUntil; }
        public void setValidUntil(long validUntil) { this.validUntil = validUntil; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        public String getOperatorId() { return operatorId; }
        public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
        
        @Override
        public String toString() {
            return String.format(
                "MissionCommand{id=%s, drone=%s, type=%s, priority=%d, params=%s}",
                commandId, droneId, missionType, priority, parameters
            );
        }
    }
    
    /**
     * IDL EmergencyStatus 结构
     */
    public static class IDLEmergencyStatus {
        private String droneId;
        private DeviceType deviceType;
        private MissionType missionType;
        private StatusCode status;
        private IDLPosition3D currentPosition;
        private double speed;
        private double heading;
        private double batteryLevel;
        private double powerConsumption;
        private Double payloadWeight;
        private String cargoType;
        private long timestampSec;
        private int timestampUsec;
        private int priority;
        private IDLPosition3D targetPosition;
        private String destination;
        
        // Getters and Setters
        public String getDroneId() { return droneId; }
        public void setDroneId(String droneId) { this.droneId = droneId; }
        public DeviceType getDeviceType() { return deviceType; }
        public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
        public MissionType getMissionType() { return missionType; }
        public void setMissionType(MissionType missionType) { this.missionType = missionType; }
        public StatusCode getStatus() { return status; }
        public void setStatus(StatusCode status) { this.status = status; }
        public IDLPosition3D getCurrentPosition() { return currentPosition; }
        public void setCurrentPosition(IDLPosition3D currentPosition) { this.currentPosition = currentPosition; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public double getHeading() { return heading; }
        public void setHeading(double heading) { this.heading = heading; }
        public double getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }
        public double getPowerConsumption() { return powerConsumption; }
        public void setPowerConsumption(double powerConsumption) { this.powerConsumption = powerConsumption; }
        public Double getPayloadWeight() { return payloadWeight; }
        public void setPayloadWeight(Double payloadWeight) { this.payloadWeight = payloadWeight; }
        public String getCargoType() { return cargoType; }
        public void setCargoType(String cargoType) { this.cargoType = cargoType; }
        public long getTimestampSec() { return timestampSec; }
        public void setTimestampSec(long timestampSec) { this.timestampSec = timestampSec; }
        public int getTimestampUsec() { return timestampUsec; }
        public void setTimestampUsec(int timestampUsec) { this.timestampUsec = timestampUsec; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public IDLPosition3D getTargetPosition() { return targetPosition; }
        public void setTargetPosition(IDLPosition3D targetPosition) { this.targetPosition = targetPosition; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        @Override
        public String toString() {
            return String.format(
                "EmergencyStatus{drone=%s, device=%s, mission=%s, status=%s, battery=%.1f%%, pos=%s}",
                droneId, deviceType, missionType, status, batteryLevel, currentPosition
            );
        }
    }
    
    /**
     * IDL SwarmMission 结构
     */
    public static class IDLSwarmMission {
        private String missionId;
        private int teamSize;
        private String leaderDroneId;
        private String missionType;
        private List<String> droneIds;
        private String waypoints;
        private long timestamp;
        
        public String getMissionId() { return missionId; }
        public void setMissionId(String missionId) { this.missionId = missionId; }
        public int getTeamSize() { return teamSize; }
        public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
        public String getLeaderDroneId() { return leaderDroneId; }
        public void setLeaderDroneId(String leaderDroneId) { this.leaderDroneId = leaderDroneId; }
        public String getMissionType() { return missionType; }
        public void setMissionType(String missionType) { this.missionType = missionType; }
        public List<String> getDroneIds() { return droneIds; }
        public void setDroneIds(List<String> droneIds) { this.droneIds = droneIds; }
        public String getWaypoints() { return waypoints; }
        public void setWaypoints(String waypoints) { this.waypoints = waypoints; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return String.format(
                "SwarmMission{id=%s, type=%s, team=%d, leader=%s, drones=%s}",
                missionId, missionType, teamSize, leaderDroneId, droneIds
            );
        }
    }
    
    /**
     * IDL VehicleData 结构
     */
    public static class IDLVehicleData {
        private String vehicleId;
        private double latitude;
        private double longitude;
        private double altitude;
        private double speed;
        private double heading;
        private double acceleration;
        private int batteryLevel;
        private String driveMode;
        private long timestamp;
        
        public String getVehicleId() { return vehicleId; }
        public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getAltitude() { return altitude; }
        public void setAltitude(double altitude) { this.altitude = altitude; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public double getHeading() { return heading; }
        public void setHeading(double heading) { this.heading = heading; }
        public double getAcceleration() { return acceleration; }
        public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
        public int getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }
        public String getDriveMode() { return driveMode; }
        public void setDriveMode(String driveMode) { this.driveMode = driveMode; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return String.format(
                "VehicleData{id=%s, lat=%.6f, lon=%.6f, speed=%.1fm/s, battery=%d%%}",
                vehicleId, latitude, longitude, speed, batteryLevel
            );
        }
    }
    
    /**
     * IDL EmergencyAlert 结构
     */
    public static class IDLEmergencyAlert {
        private long alertId;
        private AlertLevel level;
        private String alertType;
        private double latitude;
        private double longitude;
        private double affectedRadius;
        private String recommendation;
        private long timestamp;
        private long expireTime;
        
        public long getAlertId() { return alertId; }
        public void setAlertId(long alertId) { this.alertId = alertId; }
        public AlertLevel getLevel() { return level; }
        public void setLevel(AlertLevel level) { this.level = level; }
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getAffectedRadius() { return affectedRadius; }
        public void setAffectedRadius(double affectedRadius) { this.affectedRadius = affectedRadius; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        
        @Override
        public String toString() {
            return String.format(
                "EmergencyAlert{id=%d, level=%s, type=%s, pos=(%.6f,%.6f), radius=%.0fm}",
                alertId, level, alertType, latitude, longitude, affectedRadius
            );
        }
    }
}
