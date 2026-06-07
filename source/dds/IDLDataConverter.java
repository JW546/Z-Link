package dds;

import integration.DSLIDLMapper;
import integration.DSLIDLMapper.MissionType;
import integration.DSLIDLMapper.DeviceType;
import integration.DSLIDLMapper.StatusCode;
import integration.DSLIDLMapper.AlertLevel;

/**
 * DSL 到 IDL 数据对象的转换器
 * 将 DSLIDLMapper 的中间对象转换为真实的 IDL 生成的数据类型
 * 
 * @paper_ref 第4章 4.2 三层无缝集成 -> IDL层转换
 */
public class IDLDataConverter {
    
    private static final IDLDataConverter instance = new IDLDataConverter();
    
    public static IDLDataConverter getInstance() {
        return instance;
    }
    
    // ==================== EmergencyStatus 转换 ====================
    
    /**
     * 将映射器的 EmergencyStatus 转换为真实的 IDL EmergencyStatus
     */
    public chongqing.EmergencyStatus toIDLEmergencyStatus(DSLIDLMapper.IDLEmergencyStatus status) {
        chongqing.EmergencyStatus idlStatus = new chongqing.EmergencyStatus();
        
        // 基本信息
        idlStatus.drone_id = status.getDroneId();
        idlStatus.device_type = toDeviceType(status.getDeviceType());
        idlStatus.mission_type = toMissionType(status.getMissionType());
        idlStatus.status = toStatusCode(status.getStatus());
        idlStatus.priority = status.getPriority();
        
        // 位置信息
        DSLIDLMapper.IDLPosition3D pos = status.getCurrentPosition();
        if (pos != null) {
            idlStatus.current_position = new chongqing.Position3D();
            idlStatus.current_position.latitude = pos.getLatitude();
            idlStatus.current_position.longitude = pos.getLongitude();
            idlStatus.current_position.altitude = pos.getAltitude();
        }
        
        // 电池电量
        idlStatus.battery_level = status.getBatteryLevel();
        
        // 载荷
        idlStatus.payload_weight = status.getPayloadWeight();
        idlStatus.cargo_type = status.getCargoType();
        
        // 时间戳
        long now = System.currentTimeMillis();
        idlStatus.timestamp_sec = (int)(now / 1000);
        idlStatus.timestamp_usec = (int)((now % 1000) * 1000);
        
        return idlStatus;
    }
    
    // ==================== MissionCommand 转换 ====================
    
    /**
     * 将映射器的 MissionCommand 转换为真实的 IDL MissionCommand
     */
    public chongqing.MissionCommand toIDLMissionCommand(DSLIDLMapper.IDLMissionCommand cmd) {
        chongqing.MissionCommand idlCmd = new chongqing.MissionCommand();
        
        idlCmd.command_id = cmd.getCommandId();
        idlCmd.drone_id = cmd.getDroneId();
        idlCmd.mission_type = toMissionType(cmd.getMissionType());
        idlCmd.parameters = cmd.getParameters();
        idlCmd.priority = cmd.getPriority();
        idlCmd.source_system = "DSL-Converter";
        idlCmd.operator_id = "system";
        
        return idlCmd;
    }
    
    // ==================== SwarmMission 转换 ====================
    
    /**
     * 将映射器的 SwarmMission 转换为真实的 IDL SwarmMission
     */
    public urban.SwarmMission toIDLSwarmMission(DSLIDLMapper.IDLSwarmMission swarm) {
        urban.SwarmMission idlSwarm = new urban.SwarmMission();
        
        idlSwarm.mission_id = swarm.getMissionId();
        idlSwarm.team_size = swarm.getTeamSize();
        idlSwarm.leader_drone_id = swarm.getLeaderDroneId();
        idlSwarm.mission_type = swarm.getMissionType();
        idlSwarm.timestamp = (int)(System.currentTimeMillis() / 1000);
        
        // 转换航点为字符串格式
        String waypoints = swarm.getWaypoints();
        if (waypoints != null && !waypoints.isEmpty()) {
            idlSwarm.waypoints = waypoints;
        }
        
        return idlSwarm;
    }
    
    // ==================== EmergencyAlert 转换 ====================
    
    /**
     * 将映射器的 EmergencyAlert 转换为真实的 IDL EmergencyAlert
     */
    public urban.EmergencyAlert toIDLEmergencyAlert(DSLIDLMapper.IDLEmergencyAlert alert) {
        urban.EmergencyAlert idlAlert = new urban.EmergencyAlert();
        
        idlAlert.alert_id = (int)alert.getAlertId();
        idlAlert.level = toAlertLevel(alert.getLevel());
        idlAlert.alert_type = alert.getAlertType();
        idlAlert.recommendation = alert.getRecommendation();
        
        // 位置
        idlAlert.latitude = alert.getLatitude();
        idlAlert.longitude = alert.getLongitude();
        
        idlAlert.affected_radius = alert.getAffectedRadius();
        idlAlert.timestamp = (int)(System.currentTimeMillis() / 1000);
        
        return idlAlert;
    }

    // ==================== VehicleData 转换 ====================

    /**
     * 将映射器的 VehicleData 转换为真实的 DDS VehicleData
     */
    public urban.VehicleData toIDLVehicleData(DSLIDLMapper.IDLVehicleData vehicle) {
        urban.VehicleData idlVehicle = new urban.VehicleData();

        idlVehicle.vehicle_id = vehicle.getVehicleId() != null ? vehicle.getVehicleId() : "";
        idlVehicle.latitude = vehicle.getLatitude();
        idlVehicle.longitude = vehicle.getLongitude();
        idlVehicle.altitude = vehicle.getAltitude();
        idlVehicle.speed = vehicle.getSpeed();
        idlVehicle.heading = vehicle.getHeading();
        idlVehicle.acceleration = vehicle.getAcceleration();
        idlVehicle.battery_level = vehicle.getBatteryLevel();
        idlVehicle.drive_mode = vehicle.getDriveMode() != null ? vehicle.getDriveMode() : "";
        idlVehicle.timestamp = (int) vehicle.getTimestamp();

        return idlVehicle;
    }
    
    // ==================== 枚举类型转换 ====================
    
    /**
     * 转换 DeviceType 枚举
     * DSL Mapper 枚举 -> IDL 枚举（通过 ordinal）
     */
    private chongqing.DeviceType toDeviceType(DeviceType deviceType) {
        if (deviceType == null) return chongqing.DeviceType.DRONE_MULTIROTOR;
        return chongqing.DeviceType.valueOf(deviceType.ordinal());
    }
    
    /**
     * 转换 MissionType 枚举
     * DSL Mapper 枚举 -> IDL 枚举（通过 ordinal）
     */
    private chongqing.MissionType toMissionType(MissionType missionType) {
        if (missionType == null) return chongqing.MissionType.MEDICAL_SUPPLY_DELIVERY;
        return chongqing.MissionType.valueOf(missionType.ordinal());
    }
    
    /**
     * 转换 StatusCode 枚举
     * DSL Mapper 枚举 -> IDL 枚举（通过 ordinal）
     */
    private chongqing.StatusCode toStatusCode(StatusCode statusCode) {
        if (statusCode == null) return chongqing.StatusCode.IDLE;
        return chongqing.StatusCode.valueOf(statusCode.ordinal());
    }
    
    /**
     * 转换 AlertLevel 枚举
     * DSL Mapper 枚举 -> IDL 枚举（通过 ordinal）
     */
    private urban.AlertLevel toAlertLevel(AlertLevel level) {
        if (level == null) return urban.AlertLevel.MEDIUM;
        return urban.AlertLevel.valueOf(level.ordinal());
    }
}
