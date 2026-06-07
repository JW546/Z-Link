package dds;

import model.*;
import integration.DSLIDLMapper;
import java.util.List;

/**
 * DDS 集成演示类
 * 展示从 DSL -> IDL -> DDS 的完整数据流
 * 
 * @paper_ref 第4章 4.2 三层无缝集成 -> DDS层实现
 */
public class DDSDemo {
    
    private final DSLIDLMapper mapper = DSLIDLMapper.getInstance();
    private final DDSManager dds = DDSManager.getInstance();
    private final IDLDataConverter converter = IDLDataConverter.getInstance();
    
    public static void main(String[] args) {
        DDSDemo demo = new DDSDemo();
        demo.runQuickDemo();
    }
    
    /**
     * 执行完整的 DSL -> IDL -> DDS 集成演示
     */
    public void runFullDDSDemo(Mission mission) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  DSL -> IDL -> DDS 完整数据流演示");
        System.out.println("=".repeat(70));
        
        // 1. 连接 DDS 域
        System.out.println("\n[步骤1] 连接 DDS 域...");
        dds.connect();
        
        // 2. 发布 EmergencyStatus 数据
        System.out.println("\n[步骤2] 发布 EmergencyStatus 数据...");
        publishEmergencyStatuses(mission);
        
        // 3. 发布 MissionCommand 数据
        System.out.println("\n[步骤3] 发布 MissionCommand 数据...");
        publishMissionCommands(mission);
        
        // 4. 发布 SwarmMission 数据
        System.out.println("\n[步骤4] 发布 SwarmMission 数据...");
        publishSwarmMission(mission);
        
        // 5. 发布 EmergencyAlert 数据
        System.out.println("\n[步骤5] 发布 EmergencyAlert 数据...");
        publishEmergencyAlerts(mission);
        
        // 6. 打印 DDS 状态摘要
        System.out.println("\n[步骤6] DDS 状态摘要...");
        dds.printStatus();
        
        // 7. 演示订阅功能
        System.out.println("\n[步骤7] 演示订阅功能...");
        demonstrateSubscription();
        
        // 断开连接
        dds.disconnect();
        
        System.out.println("\n✓ DDS 集成演示完成!");
    }
    
    /**
     * 发布 EmergencyStatus 数据到 DDS
     */
    private void publishEmergencyStatuses(Mission mission) {
        for (State state : mission.getStates()) {
            DSLIDLMapper.IDLEmergencyStatus status = mapper.mapStateToStatus(state, mission.getName());
            chongqing.EmergencyStatus idlStatus = converter.toIDLEmergencyStatus(status);
            
            long seq = dds.publishEmergencyStatus(idlStatus);
            System.out.println("  ✓ " + status.getDroneId() + " -> DDS [seq=" + seq + "]");
            System.out.println("    类型: " + idlStatus.device_type + ", 状态: " + idlStatus.status);
            System.out.println("    位置: (" + idlStatus.current_position.latitude + ", " + idlStatus.current_position.longitude + ")");
            System.out.println("    电池: " + idlStatus.battery_level + "%");
        }
    }
    
    /**
     * 发布 MissionCommand 数据到 DDS
     */
    private void publishMissionCommands(Mission mission) {
        List<DSLIDLMapper.IDLMissionCommand> commands = mapper.mapMissionToCommands(mission);
        
        for (DSLIDLMapper.IDLMissionCommand cmd : commands) {
            chongqing.MissionCommand idlCmd = converter.toIDLMissionCommand(cmd);
            
            long seq = dds.publishMissionCommand(idlCmd);
            System.out.println("  ✓ " + cmd.getCommandId() + " -> DDS [seq=" + seq + "]");
            System.out.println("    目标: " + idlCmd.drone_id + ", 类型: " + idlCmd.mission_type);
            System.out.println("    优先级: " + idlCmd.priority);
        }
    }
    
    /**
     * 发布 SwarmMission 数据到 DDS
     */
    private void publishSwarmMission(Mission mission) {
        DSLIDLMapper.IDLSwarmMission swarm = mapper.mapMissionToSwarmMission(mission);
        urban.SwarmMission idlSwarm = converter.toIDLSwarmMission(swarm);
        
        long seq = dds.publishSwarmMission(idlSwarm);
        System.out.println("  ✓ " + swarm.getMissionId() + " -> DDS [seq=" + seq + "]");
        System.out.println("    团队规模: " + idlSwarm.team_size + ", 队长: " + idlSwarm.leader_drone_id);
        System.out.println("    成员: " + idlSwarm.drone_ids.size() + " 个");
    }
    
    /**
     * 发布 EmergencyAlert 数据到 DDS
     */
    private void publishEmergencyAlerts(Mission mission) {
        List<DSLIDLMapper.IDLEmergencyAlert> alerts = mapper.mapMissionToAlerts(mission);
        
        for (DSLIDLMapper.IDLEmergencyAlert alert : alerts) {
            urban.EmergencyAlert idlAlert = converter.toIDLEmergencyAlert(alert);
            
            long seq = dds.publishEmergencyAlert(idlAlert);
            System.out.println("  ✓ 预警 #" + alert.getAlertId() + " -> DDS [seq=" + seq + "]");
            System.out.println("    级别: " + idlAlert.level + ", 类型: " + idlAlert.alert_type);
            System.out.println("    位置: (" + idlAlert.latitude + ", " + idlAlert.longitude + ")");
        }
    }
    
    /**
     * 演示订阅功能
     */
    private void demonstrateSubscription() {
        System.out.println("\n  订阅 EmergencyStatus 主题...");
        
        dds.subscribeEmergencyStatus(status -> {
            System.out.println("  [订阅回调] 收到 EmergencyStatus:");
            System.out.println("    drone_id: " + status.drone_id);
            System.out.println("    status: " + status.status);
            System.out.println("    battery: " + status.battery_level + "%");
        });
        
        System.out.println("  订阅 MissionCommand 主题...");
        
        dds.subscribeMissionCommand(cmd -> {
            System.out.println("  [订阅回调] 收到 MissionCommand:");
            System.out.println("    command_id: " + cmd.command_id);
            System.out.println("    drone_id: " + cmd.drone_id);
            System.out.println("    mission_type: " + cmd.mission_type);
        });
        
        System.out.println("\n  订阅设置完成，等待数据...");
    }
    
    /**
     * 运行快速演示
     */
    public void runQuickDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  DDS IDL 数据类型演示");
        System.out.println("=".repeat(70));
        
        // 连接 DDS
        dds.connect();
        
        // 创建并发布 EmergencyStatus
        chongqing.EmergencyStatus status = new chongqing.EmergencyStatus();
        status.drone_id = "DEMO-DRONE-001";
        status.device_type = chongqing.DeviceType.DRONE_MULTIROTOR;
        status.mission_type = chongqing.MissionType.SURVEILLANCE_PATROL;
        status.status = chongqing.StatusCode.IN_TRANSIT;
        status.priority = 5;
        
        status.current_position = new chongqing.Position3D();
        status.current_position.latitude = 29.5582;
        status.current_position.longitude = 106.5549;
        status.current_position.altitude = 300.0;
        
        status.battery_level = 85.0;
        status.speed = 12.5;
        status.heading = 45.0;
        
        long seq = dds.publishEmergencyStatus(status);
        System.out.println("\n  发布的 EmergencyStatus:");
        System.out.println(status.toString());
        
        // 创建并发布 EmergencyAlert
        urban.EmergencyAlert alert = new urban.EmergencyAlert();
        alert.alert_id = 1;
        alert.level = urban.AlertLevel.HIGH;
        alert.alert_type = "weather_disaster";
        alert.latitude = 29.5582;
        alert.longitude = 106.5549;
        alert.affected_radius = 500.0;
        alert.recommendation = "无人机应降低飞行高度或返航";
        alert.timestamp = (int)(System.currentTimeMillis() / 1000);
        
        dds.publishEmergencyAlert(alert);
        System.out.println("\n  发布的 EmergencyAlert:");
        System.out.println("  alert_id: " + alert.alert_id);
        System.out.println("  level: " + alert.level);
        System.out.println("  type: " + alert.alert_type);
        System.out.println("  location: (" + alert.latitude + ", " + alert.longitude + ")");
        System.out.println("  radius: " + alert.affected_radius + "m");
        System.out.println("  recommendation: " + alert.recommendation);
        
        // 打印 DDS 状态
        dds.printStatus();
        
        // 断开连接
        dds.disconnect();
    }
}
