package vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * 车辆控制指令模型
 * 
 * 用于本地ECU和协议网关之间的指令传递
 * 
 * @paper_ref 无人汽车本地DSL引擎控制指令
 */
public class VehicleCommand {
    
    // ========== 指令类型 ==========
    public enum CommandType {
        // 底盘控制
        EMERGENCY_BRAKE,
        NORMAL_BRAKE,
        ACCELERATE,
        STEERING,
        GEAR_SHIFT,
        
        // 车身控制
        DOOR_LOCK,
        DOOR_UNLOCK,
        HORN,
        LIGHTS_ON,
        LIGHTS_OFF,
        
        // 空调控制
        HVAC_ON,
        HVAC_OFF,
        SET_TEMPERATURE,
        
        // 任务控制
        START_MISSION,
        PAUSE_MISSION,
        RESUME_MISSION,
        ABORT_MISSION,
        
        // V2X 控制
        BROADCAST_BSM,
        REQUEST_SPAT,
        SEND_INTENTION
    }
    
    // ========== 指令目标 ==========
    public enum Target {
        ECU_CHASSIS,    // 底盘ECU
        ECU_BODY,       // 车身ECU
        ECU_BMS,        // 电池管理ECU
        ECU_VCU,        // 整车控制ECU
        HVAC,           // 空调系统
        V2X_MODULE,     // V2X模块
        DDS_NETWORK    // DDS网络
    }
    
    // 指令属性
    public CommandType type;
    public Target target;
    public int priority;              // 优先级 0-255
    public long timestamp;
    public long validUntil;           // 有效截止时间
    
    // 控制参数
    public double targetSpeed;         // 目标速度 (m/s)
    public double targetSteering;      // 目标转向角度 (度)
    public double targetAcceleration; // 目标加速度 (m/s²)
    
    // 扩展参数
    public Map<String, Object> parameters;
    
    // 构造函数
    public VehicleCommand() {
        this.priority = 128;  // 默认中等优先级
        this.timestamp = System.currentTimeMillis();
        this.parameters = new HashMap<>();
    }
    
    public VehicleCommand(CommandType type, Target target) {
        this();
        this.type = type;
        this.target = target;
    }
    
    // ========== 便捷工厂方法 ==========
    
    /**
     * 创建紧急制动指令
     */
    public static VehicleCommand emergencyBrake() {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.EMERGENCY_BRAKE;
        cmd.target = Target.ECU_CHASSIS;
        cmd.priority = 255;  // 最高优先级
        cmd.targetSpeed = 0.0;
        cmd.targetAcceleration = -8.0;  // 最大减速度
        return cmd;
    }
    
    /**
     * 创建速度控制指令
     */
    public static VehicleCommand speedControl(double targetSpeed, double maxAcceleration) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.ACCELERATE;
        cmd.target = Target.ECU_CHASSIS;
        cmd.targetSpeed = targetSpeed;
        cmd.targetAcceleration = maxAcceleration;
        return cmd;
    }
    
    /**
     * 创建转向指令
     */
    public static VehicleCommand steering(double angle) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.STEERING;
        cmd.target = Target.ECU_CHASSIS;
        cmd.targetSteering = angle;
        return cmd;
    }
    
    /**
     * 创建车门控制指令
     */
    public static VehicleCommand doorLock(boolean lock) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = lock ? CommandType.DOOR_LOCK : CommandType.DOOR_UNLOCK;
        cmd.target = Target.ECU_BODY;
        return cmd;
    }
    
    /**
     * 创建空调控制指令
     */
    public static VehicleCommand setHVAC(double temperature) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.SET_TEMPERATURE;
        cmd.target = Target.HVAC;
        cmd.parameters.put("temperature", temperature);
        return cmd;
    }
    
    /**
     * 创建任务开始指令
     */
    public static VehicleCommand startMission(String missionId) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.START_MISSION;
        cmd.target = Target.DDS_NETWORK;
        cmd.parameters.put("mission_id", missionId);
        return cmd;
    }
    
    /**
     * 创建V2X广播指令
     */
    public static VehicleCommand broadcastBSM(String vehicleId, double lat, double lon, 
                                              double speed, double heading) {
        VehicleCommand cmd = new VehicleCommand();
        cmd.type = CommandType.BROADCAST_BSM;
        cmd.target = Target.V2X_MODULE;
        cmd.parameters.put("vehicle_id", vehicleId);
        cmd.parameters.put("latitude", lat);
        cmd.parameters.put("longitude", lon);
        cmd.parameters.put("speed", speed);
        cmd.parameters.put("heading", heading);
        return cmd;
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 检查指令是否过期
     */
    public boolean isExpired() {
        if (validUntil == 0) return false;
        return System.currentTimeMillis() > validUntil;
    }
    
    /**
     * 检查是否是紧急指令
     */
    public boolean isEmergency() {
        return type == CommandType.EMERGENCY_BRAKE || priority >= 250;
    }
    
    /**
     * 设置有效期（秒）
     */
    public VehicleCommand setValidFor(int seconds) {
        this.validUntil = System.currentTimeMillis() + seconds * 1000L;
        return this;
    }
    
    /**
     * 添加扩展参数
     */
    public VehicleCommand addParam(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("VehicleCommand[type=%s, target=%s, priority=%d, speed=%.1f, steering=%.1f]",
            type, target, priority, targetSpeed, targetSteering);
    }
}
