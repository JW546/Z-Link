package vehicle;

import dds.DDSManager;
import model.*;
import integration.DSLIDLMapper;
import chongqing.*;
import urban.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * 无人汽车系统集成演示
 * 
 * 演示四个核心组件的协同工作：
 * 1. 传感器接口层 - SensorAdapter
 * 2. 本地DSL引擎 - VehicleDSLEngine
 * 3. 边缘DDS节点 - VehicleDDSNode
 * 4. 协议网关 - VehicleProtocolGateway
 * 
 * @paper_ref 无人汽车DSL系统集成
 */
public class VehicleIntegrationDemo {
    
    private static final String TAG = "[VehicleIntegrationDemo]";
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("  无人汽车 DSL 系统集成演示");
        System.out.println("  场景：比亚迪汉 EV + 大疆'灵鸢'无人机协同");
        System.out.println("=".repeat(70));
        System.out.println();
        
        // 演示各组件功能
        demoSensorAdapter();
        demoDSLEngine();
        demoProtocolGateway();
        demoDDSNode();
        
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  演示完成！");
        System.out.println("=".repeat(70));
    }
    
    // ========== 1. 传感器接口层演示 ==========
    
    /**
     * 演示传感器接口层功能
     */
    private static void demoSensorAdapter() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 传感器接口层演示 (SensorAdapter)                            │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        
        // 1.1 模拟摄像头检测
        System.out.println();
        System.out.println("【1.1 摄像头检测 - YOLO目标检测】");
        
        List<Map<String, Object>> yoloResults = new ArrayList<>();
        
        // 模拟检测结果
        Map<String, Object> det1 = new HashMap<>();
        det1.put("class", "car");
        det1.put("confidence", 0.92);
        det1.put("x", 320.0);
        det1.put("y", 240.0);
        det1.put("width", 150.0);
        det1.put("height", 120.0);
        yoloResults.add(det1);
        
        Map<String, Object> det2 = new HashMap<>();
        det2.put("class", "pedestrian");
        det2.put("confidence", 0.88);
        det2.put("x", 200.0);
        det2.put("y", 350.0);
        det2.put("width", 60.0);
        det2.put("height", 100.0);
        yoloResults.add(det2);
        
        VehicleSensorData.Obstacle[] cameraObstacles = 
            SensorAdapter.CameraAdapter.parseYOLOResult(yoloResults);
        
        System.out.println("  检测到障碍物:");
        for (VehicleSensorData.Obstacle obs : cameraObstacles) {
            System.out.println("    - " + obs);
        }
        
        // 1.2 模拟雷达数据
        System.out.println();
        System.out.println("【1.2 毫米波雷达 - 目标跟踪】");
        
        // 模拟CAN数据 - 使用byte类型避免转换错误
        byte[] radarCAN = new byte[] {
            0x01, (byte)0xE8, 0x03, (byte)0xF4, 0x1E, 0x00, 0x0A, 0x00,  // 目标1
        };
        
        VehicleSensorData.RadarTarget[] radarTargets = 
            SensorAdapter.RadarAdapter.parseCANMessage(radarCAN);
        
        System.out.println("  雷达检测到目标:");
        for (VehicleSensorData.RadarTarget target : radarTargets) {
            System.out.println("    - " + target);
        }
        
        // 1.3 模拟GNSS数据
        System.out.println();
        System.out.println("【1.3 GNSS定位 - NMEA解析】");
        
        String nmeaSentence = "$GPRMC,064951.00,A,2958.54215,N,10654.32487,E,15.5,120.5,150424,,,A*6B";
        VehicleSensorData.GNSSPosition gnss = SensorAdapter.GNSSAdapter.parseNMEA(nmeaSentence);
        
        if (gnss != null) {
            System.out.println("  位置: " + gnss);
            System.out.println("  速度: " + String.format("%.1f", gnss.speed) + " m/s");
            System.out.println("  航向: " + String.format("%.1f", gnss.heading) + "°");
        }
        
        // 1.4 数据融合
        System.out.println();
        System.out.println("【1.4 多传感器数据融合】");
        
        VehicleSensorData.FusionResult fusion = new VehicleSensorData.FusionResult();
        fusion.latitude = 29.5582;
        fusion.longitude = 106.5549;
        fusion.altitude = 320;
        fusion.speed = 15.5;
        fusion.heading = 120.5;
        
        // 添加障碍物
        for (VehicleSensorData.Obstacle obs : cameraObstacles) {
            fusion.obstacles.put(obs.type + "_" + fusion.obstacles.size(), obs);
        }
        
        // 添加传感器健康状态
        fusion.sensorHealth.put(VehicleSensorData.SensorType.CAMERA, 0.95);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.RADAR, 0.90);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.GNSS, 0.98);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.IMU, 0.85);
        
        System.out.println("  " + fusion);
        System.out.println("  传感器健康度: " + 
            String.format("%.0f%%", fusion.getSensorHealthStatus() * 100));
        
        // 1.5 DSL转换
        System.out.println();
        System.out.println("【1.5 DSL状态转换】");
        
        State dslState = SensorAdapter.toDSLState(fusion, "BYD-HAN-001");
        System.out.println("  DSL State: " + dslState.getName());
        System.out.println("  字段:");
        dslState.getFields().forEach((k, v) -> 
            System.out.println("    " + k + " = " + v));
    }
    
    // ========== 2. 本地DSL引擎演示 ==========
    
    /**
     * 演示本地DSL引擎功能
     */
    private static void demoDSLEngine() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  2. 本地DSL引擎演示 (VehicleDSLEngine)                        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        
        VehicleDSLEngine engine = new VehicleDSLEngine();
        
        // 2.1 模拟传感器数据更新
        System.out.println();
        System.out.println("【2.1 更新传感器数据】");
        
        VehicleSensorData.FusionResult fusion = createTestFusion();
        engine.updateSensorData(fusion);
        
        VehicleSensorData.VehicleStatus status = new VehicleSensorData.VehicleStatus();
        status.speed = 72;  // km/h
        status.batterySOC = 85;
        status.batteryTemp = 28;
        status.steeringAngle = 5;
        engine.updateVehicleStatus(status);
        
        System.out.println("  传感器数据已更新");
        System.out.println("  当前速度: " + String.format("%.1f", engine.getCurrentSpeed()) + " m/s");
        System.out.println("  最近障碍物: " + engine.getMinObstacleDistance() + " m");
        
        // 2.2 本地决策执行
        System.out.println();
        System.out.println("【2.2 本地决策执行】");
        
        VehicleDSLEngine.ExecutionResult result = engine.executeLocal();
        System.out.println("  执行结果: " + result);
    }
    
    // ========== 3. 协议网关演示 ==========
    
    /**
     * 演示协议网关功能
     */
    private static void demoProtocolGateway() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  3. 协议网关演示 (VehicleProtocolGateway)                     │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        
        VehicleProtocolGateway gateway = new VehicleProtocolGateway();
        
        // 3.1 CAN → DDS 转换
        System.out.println();
        System.out.println("【3.1 CAN → DDS 转换】");
        
        // 模拟动力系统CAN消息
        byte[] powertrainCAN = new byte[] {
            (byte)0xC3, 0x5A,  // 速度: 72 km/h
            0x27, 0x10,        // 转速: 4200 rpm
            0x00, 0x00, 0x00, 0x00  // 填充
        };
        
        VehicleData vehicleData = gateway.canToDDS(0x100, powertrainCAN, "BYD-HAN-001");
        if (vehicleData != null) {
            System.out.println("  CAN → DDS 转换成功:");
            System.out.println("    speed: " + String.format("%.1f", vehicleData.speed) + " m/s");
        }
        
        // 3.2 DDS → CAN 转换
        System.out.println();
        System.out.println("【3.2 DDS → CAN 转换】");
        
        VehicleCommand cmd = VehicleCommand.emergencyBrake();
        byte[] canCmd = gateway.ddsToCAN(cmd);
        System.out.println("  生成紧急制动CAN指令: " + 
            (canCmd != null ? "成功 (" + canCmd.length + " bytes)" : "失败"));
        
        // 3.3 速度控制命令
        System.out.println();
        System.out.println("【3.3 速度控制命令】");
        
        VehicleCommand speedCmd = VehicleCommand.speedControl(20.0, 2.0);
        byte[] speedCAN = gateway.ddsToCAN(speedCmd);
        System.out.println("  速度控制: 目标速度=" + speedCmd.targetSpeed + " m/s");
        System.out.println("  CAN编码: " + (speedCAN != null ? "成功" : "失败"));
        
        // 3.4 协议路由
        System.out.println();
        System.out.println("【3.4 协议自动路由】");
        
        String[] destinations = {"ECU_BCM", "ECU_VCU", "UAV_DJI", "RSU_TrafficLight"};
        for (String dest : destinations) {
            String protocol = gateway.autoSelectProtocol(dest);
            System.out.println("  " + dest + " → " + protocol);
        }
    }
    
    // ========== 4. 边缘DDS节点演示 ==========
    
    /**
     * 演示边缘DDS节点功能
     */
    private static void demoDDSNode() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  4. 边缘DDS节点演示 (VehicleDDSNode)                          │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        
        VehicleDDSNode node = new VehicleDDSNode("BYD-HAN-001", 0);
        
        // 4.1 启动节点
        System.out.println();
        System.out.println("【4.1 启动边缘节点】");
        node.start();
        
        // 4.2 模拟协同感知
        System.out.println();
        System.out.println("【4.2 模拟协同感知】");
        
        simulateCommunication(node);
        
        // 4.3 状态查询
        System.out.println();
        System.out.println("【4.3 状态查询】");
        
        System.out.println("  节点ID: " + node.getVehicleId());
        System.out.println("  DDS连接: " + (node.isConnected() ? "已连接" : "未连接"));
        System.out.println("  本地引擎运行: " + (node.getEngine().isNetworkConnected() ? "在线" : "离线"));
        System.out.println("  附近车辆: " + node.getNearbyVehicles().size());
        System.out.println("  活跃预警: " + node.getActiveAlerts().size());
        
        // 4.4 停止节点
        System.out.println();
        System.out.println("【4.4 停止边缘节点】");
        node.stop();
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 创建测试用融合数据
     */
    private static VehicleSensorData.FusionResult createTestFusion() {
        VehicleSensorData.FusionResult fusion = new VehicleSensorData.FusionResult();
        
        fusion.latitude = 29.5582;
        fusion.longitude = 106.5549;
        fusion.altitude = 320;
        fusion.speed = 20.0;
        fusion.heading = 120.5;
        
        // 添加障碍物
        VehicleSensorData.Obstacle obs1 = 
            new VehicleSensorData.Obstacle("vehicle", 25.0, 10.0, "camera");
        obs1.confidence = 0.92;
        fusion.obstacles.put("vehicle_1", obs1);
        
        VehicleSensorData.Obstacle obs2 = 
            new VehicleSensorData.Obstacle("pedestrian", 15.0, -5.0, "radar");
        obs2.confidence = 0.85;
        fusion.obstacles.put("pedestrian_1", obs2);
        
        // 传感器健康
        fusion.sensorHealth.put(VehicleSensorData.SensorType.CAMERA, 0.95);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.RADAR, 0.90);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.GNSS, 0.98);
        fusion.sensorHealth.put(VehicleSensorData.SensorType.IMU, 0.85);
        
        return fusion;
    }
    
    /**
     * 模拟与其他车辆的通信
     */
    private static void simulateCommunication(VehicleDDSNode node) {
        // 模拟收到其他车辆的状态
        VehicleData otherVehicle = new VehicleData();
        otherVehicle.vehicle_id = "BYD-TANG-002";
        otherVehicle.latitude = 29.5585;
        otherVehicle.longitude = 106.5550;
        otherVehicle.speed = 18.0;
        otherVehicle.heading = 300.0;
        
        // 模拟收到预警
        EmergencyAlert alert = new EmergencyAlert();
        alert.alert_id = 1001;
        alert.level = AlertLevel.HIGH;
        alert.alert_type = "weather_disaster";
        alert.latitude = 29.56;
        alert.longitude = 106.56;
        alert.affected_radius = 5000;
        alert.recommendation = "前方雾区，建议减速";
        
        System.out.println("  模拟收到车辆状态: " + otherVehicle.vehicle_id);
        System.out.println("  模拟收到预警: " + alert.alert_type + 
            " (" + alert.level + ")");
    }
}
