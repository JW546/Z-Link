package vehicle;

import model.*;
import utils.SynonymLibrary;
import integration.DSLIDLMapper;
import urban.*;
import chongqing.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * 本地 DSL 引擎 - 车辆ECU嵌入式DSL执行器
 * 
 * 功能：
 * 1. DSL任务解析与验证
 * 2. 本地决策执行
 * 3. 状态机管理
 * 4. 紧急情况本地响应（不依赖网络）
 * 
 * @paper_ref 第4章 4.3 行为树层 -> 本地决策执行
 */
public class VehicleDSLEngine {
    
    private static final String TAG = "[VehicleDSLEngine]";
    
    // 本地数据存储（替代Blackboard）
    private final Map<String, Object> localData;
    
    // DSL解析器
    private final DSLIDLMapper mapper;
    
    // 当前任务和状态
    private Mission currentMission;
    private State currentState;
    private VehicleSensorData.VehicleStatus vehicleStatus;
    
    // 紧急模式标志
    private volatile boolean emergencyMode = false;
    private volatile boolean connected = false;  // 网络连接状态
    
    // 执行结果
    private ExecutionResult lastResult;
    
    // 传感器数据缓存
    private final ConcurrentLinkedQueue<VehicleSensorData.FusionResult> sensorBuffer;
    private VehicleSensorData.FusionResult latestFusion;
    
    // 传感器健康状态
    private boolean cameraActive = false;
    private boolean radarActive = false;
    private boolean gnssActive = false;
    private boolean imuActive = false;
    
    // 构造函数
    public VehicleDSLEngine() {
        this.localData = new ConcurrentHashMap<>();
        this.mapper = DSLIDLMapper.getInstance();
        this.sensorBuffer = new ConcurrentLinkedQueue<>();
        
        System.out.println(TAG + " 本地DSL引擎初始化完成");
    }
    
    // ========== 公共接口 ==========
    
    /**
     * 解析DSL任务文件
     */
    public Mission parseMission(String dslCode) {
        // 实际应使用ANTLR解析器
        // 这里简化处理，返回模拟任务
        Mission mission = new Mission("ParsedMission");
        
        State state = new State("vehicleState");
        state.addField("vehicle_id", "VEHICLE-001");
        state.addField("speed", "0km/h");
        state.addField("battery_level", "85%");
        state.addField("latitude", "29.5582");
        state.addField("longitude", "106.5549");
        mission.addState(state);
        
        this.currentMission = mission;
        System.out.println(TAG + " 解析DSL任务: " + mission);
        return mission;
    }
    
    /**
     * 更新传感器数据
     */
    public void updateSensorData(VehicleSensorData.FusionResult fusion) {
        this.latestFusion = fusion;
        this.sensorBuffer.offer(fusion);
        
        // 保持缓冲区大小
        while (sensorBuffer.size() > 10) {
            sensorBuffer.poll();
        }
        
        // 更新本地数据
        updateLocalData(fusion);
        
        // 检查传感器健康
        updateSensorHealth(fusion);
    }
    
    /**
     * 更新车辆状态（来自CAN总线）
     */
    public void updateVehicleStatus(VehicleSensorData.VehicleStatus status) {
        this.vehicleStatus = status;
        localData.put("vehicle_status", status);
    }
    
    /**
     * 执行本地决策（不依赖网络）
     * 这是核心方法，被主循环调用
     */
    public ExecutionResult executeLocal() {
        ExecutionResult result = new ExecutionResult();
        
        // 1. 获取最新传感器数据
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        if (fusion == null) {
            result.action = "WAIT_SENSORS";
            result.reason = "等待传感器数据";
            return result;
        }
        
        // 2. 检查紧急情况（本地判断）
        if (checkEmergencyConditions(fusion)) {
            result.action = "EMERGENCY_STOP";
            result.reason = emergencyReason;
            result.priority = 255;
            executeEmergencyStop();
            return result;
        }
        
        // 3. 检查网络状态
        updateConnectionStatus();
        
        // 4. 根据连接状态选择执行路径
        if (connected) {
            result = executeOnlineMode();
        } else {
            result = executeOfflineMode();
        }
        
        this.lastResult = result;
        return result;
    }
    
    /**
     * 获取当前状态对应的IDL数据
     */
    public VehicleData toIDLData() {
        VehicleData data = new VehicleData();
        
        data.vehicle_id = getVehicleId();
        
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        if (fusion != null) {
            data.latitude = fusion.latitude;
            data.longitude = fusion.longitude;
            data.speed = fusion.speed;
            data.heading = fusion.heading;
        }
        
        if (vehicleStatus != null) {
            data.battery_level = (int) vehicleStatus.batterySOC;
        }
        
        data.drive_mode = emergencyMode ? "EMERGENCY" : "AUTONOMOUS";
        data.timestamp = (int)(System.currentTimeMillis() / 1000);
        
        return data;
    }
    
    /**
     * 发送紧急制动指令
     */
    public void sendEmergencyBrake() {
        VehicleCommand cmd = VehicleCommand.emergencyBrake();
        System.out.println(TAG + " 发送紧急制动指令");
        // 实际通过CAN总线发送
        // sendToCANBus(cmd);
    }
    
    /**
     * 获取当前位置
     */
    public Position getCurrentPosition() {
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        if (fusion != null) {
            return new Position(fusion.latitude, fusion.longitude);
        }
        return new Position(0, 0);
    }
    
    /**
     * 获取当前速度 (m/s)
     */
    public double getCurrentSpeed() {
        if (vehicleStatus != null) {
            return vehicleStatus.speed / 3.6;  // km/h -> m/s
        }
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        return fusion != null ? fusion.speed : 0;
    }
    
    /**
     * 获取最近障碍物距离
     */
    public double getMinObstacleDistance() {
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        if (fusion != null && !fusion.obstacles.isEmpty()) {
            return fusion.obstacles.values().stream()
                .mapToDouble(o -> o.distance)
                .min().orElse(999);
        }
        return 999;
    }
    
    /**
     * 检查网络连接状态
     */
    public boolean isNetworkConnected() {
        return connected;
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 更新本地数据
     */
    private void updateLocalData(VehicleSensorData.FusionResult fusion) {
        localData.put("position_lat", fusion.latitude);
        localData.put("position_lon", fusion.longitude);
        localData.put("heading", fusion.heading);
        localData.put("speed", fusion.speed);
        localData.put("obstacles_count", fusion.obstacles.size());
        localData.put("timestamp", fusion.timestamp);
    }
    
    /**
     * 更新传感器健康状态
     */
    private void updateSensorHealth(VehicleSensorData.FusionResult fusion) {
        Map<VehicleSensorData.SensorType, Double> health = fusion.sensorHealth;
        
        cameraActive = health.getOrDefault(VehicleSensorData.SensorType.CAMERA, 0.0) > 0.5;
        radarActive = health.getOrDefault(VehicleSensorData.SensorType.RADAR, 0.0) > 0.5;
        gnssActive = health.getOrDefault(VehicleSensorData.SensorType.GNSS, 0.0) > 0.5;
        imuActive = health.getOrDefault(VehicleSensorData.SensorType.IMU, 0.0) > 0.5;
    }
    
    // 紧急原因描述
    private String emergencyReason = "";
    
    /**
     * 检查紧急情况条件
     */
    private boolean checkEmergencyConditions(VehicleSensorData.FusionResult fusion) {
        emergencyReason = "";
        
        // 1. 障碍物距离 < 5米
        if (!fusion.obstacles.isEmpty()) {
            double minDistance = fusion.obstacles.values().stream()
                .mapToDouble(o -> o.distance)
                .min().orElse(999);
            
            if (minDistance < 5.0) {
                emergencyReason = "障碍物过近: " + minDistance + "m";
                emergencyMode = true;
                return true;
            }
        }
        
        // 2. 电池SOC < 10%
        if (vehicleStatus != null && vehicleStatus.batterySOC < 10.0) {
            emergencyReason = "电池电量过低: " + vehicleStatus.batterySOC + "%";
            emergencyMode = true;
            return true;
        }
        
        // 3. 传感器故障
        if (fusion.getSensorHealthStatus() < 0.5) {
            emergencyReason = "传感器故障，健康度: " + 
                String.format("%.0f%%", fusion.getSensorHealthStatus() * 100);
            emergencyMode = true;
            return true;
        }
        
        // 4. 无GNSS信号（导航丢失）
        if (!gnssActive) {
            emergencyReason = "GNSS信号丢失";
            emergencyMode = true;
            return true;
        }
        
        emergencyMode = false;
        return false;
    }
    
    /**
     * 执行紧急停车
     */
    private void executeEmergencyStop() {
        System.out.println(TAG + " 紧急停车: " + emergencyReason);
        
        // 生成紧急控制指令
        VehicleCommand cmd = VehicleCommand.emergencyBrake();
        
        // 通过CAN总线发送到底盘ECU
        // 实际实现需要通过协议网关
        System.out.println(TAG + " 发送CAN指令: " + cmd);
    }
    
    /**
     * 更新网络连接状态
     */
    private void updateConnectionStatus() {
        // 实际应检查DDS连接状态
        // 这里简化处理，假设连接正常
        this.connected = true;
    }
    
    /**
     * 在线模式执行
     */
    private ExecutionResult executeOnlineMode() {
        ExecutionResult result = new ExecutionResult();
        
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        double speed = getCurrentSpeed();
        double distance = getMinObstacleDistance();
        
        // 根据前方障碍物调整速度
        if (distance < 20) {
            // 减速
            double targetSpeed = Math.min(speed, distance / 3);  // 保持3秒距离
            result.action = "SPEED_CONTROL";
            result.targetSpeed = targetSpeed;
            result.reason = "前方有障碍物，保持安全距离";
        } else if (distance < 50) {
            // 谨慎驾驶
            result.action = "CAUTIOUS_DRIVE";
            result.targetSpeed = speed * 0.8;
            result.reason = "接近障碍物区域";
        } else {
            // 正常行驶
            result.action = "NORMAL_DRIVE";
            result.targetSpeed = 15.0;  // 目标速度 15 m/s
            result.reason = "正常行驶";
        }
        
        // 检查是否需要发布状态
        if (shouldPublishStatus()) {
            result.shouldPublish = true;
        }
        
        return result;
    }
    
    /**
     * 离线模式执行
     */
    private ExecutionResult executeOfflineMode() {
        ExecutionResult result = new ExecutionResult();
        
        VehicleSensorData.FusionResult fusion = getLatestFusion();
        double speed = getCurrentSpeed();
        double distance = getMinObstacleDistance();
        
        // 离线模式更加保守
        if (distance < 30) {
            // 强制减速（离线模式更保守）
            result.action = "SPEED_CONTROL";
            result.targetSpeed = Math.min(speed, distance / 5);  // 保持5秒距离
            result.reason = "[离线] 减速，保持安全距离";
        } else if (distance < 100) {
            result.action = "CAUTIOUS_DRIVE";
            result.targetSpeed = speed * 0.6;
            result.reason = "[离线] 谨慎驾驶";
        } else {
            // 维持低速行驶
            result.action = "SAFE_DRIVE";
            result.targetSpeed = 10.0;  // 离线模式限速 10 m/s
            result.reason = "[离线] 安全模式行驶";
        }
        
        result.offlineMode = true;
        return result;
    }
    
    /**
     * 是否应该发布状态
     */
    private long lastPublishTime = 0;
    private static final long PUBLISH_INTERVAL = 100;  // 100ms = 10Hz
    
    private boolean shouldPublishStatus() {
        long now = System.currentTimeMillis();
        if (now - lastPublishTime >= PUBLISH_INTERVAL) {
            lastPublishTime = now;
            return true;
        }
        return false;
    }
    
    /**
     * 获取最新融合数据
     */
    private VehicleSensorData.FusionResult getLatestFusion() {
        if (latestFusion != null) {
            return latestFusion;
        }
        return sensorBuffer.peek();
    }
    
    /**
     * 获取车辆ID
     */
    private String getVehicleId() {
        if (currentMission != null) {
            for (State state : currentMission.getStates()) {
                String id = state.getFields().get("vehicle_id");
                if (id != null) return id;
            }
        }
        return "VEHICLE-DEFAULT";
    }
    
    // ========== 内部类 ==========
    
    /**
     * 执行结果
     */
    public static class ExecutionResult {
        public String action;
        public String reason;
        public double targetSpeed;
        public double targetSteering;
        public int priority;
        public boolean shouldPublish;
        public boolean offlineMode;
        
        public ExecutionResult() {
            this.priority = 128;
            this.shouldPublish = false;
            this.offlineMode = false;
        }
        
        @Override
        public String toString() {
            return String.format("ExecutionResult[action=%s, speed=%.1fm/s, reason=%s]",
                action, targetSpeed, reason);
        }
    }
    
    /**
     * 位置
     */
    public static class Position {
        public double lat, lon;
        
        public Position(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
