package vehicle;

import protocol.*;
import dds.DDSManager;
import utils.ProtocolAdapter;
import urban.*;
import chongqing.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆协议网关
 * 
 * 功能：
 * 1. CAN-Bus ↔ DDS 双向转换（车规ECU ↔ 车载计算平台）
 * 2. V2X ↔ DDS 双向转换（路侧单元 ↔ 车载系统）
 * 3. MAVLink ↔ DDS 转换（与无人机通信）
 * 4. 协议自动识别与选择
 * 
 * @paper_ref 第4章 4.4 协议适配层
 */
public class VehicleProtocolGateway {
    
    private static final String TAG = "[ProtocolGateway]";
    
    // 协议注册表
    private final ProtocolRegistry registry;
    
    // DDS管理器
    private final DDSManager dds;
    
    // 协议转换器实例
    private final CANBusAdapter canBus;
    private final V2XAdapter v2x;
    private final DroneCANAdapter droneCAN;
    
    // CAN消息处理回调
    private final Map<Integer, CANMessageHandler> canHandlers = new ConcurrentHashMap<>();
    
    // V2X消息处理回调
    private final List<V2XMessageHandler> v2xHandlers = new ArrayList<>();
    
    // 协议路由表
    private static final Map<String, String[]> PROTOCOL_ROUTING = new HashMap<>();
    
    static {
        // CAN-Bus 用于车内ECU通信
        PROTOCOL_ROUTING.put("ECU_BCM", new String[]{"CAN-Bus", "0x100"});   // 车身控制
        PROTOCOL_ROUTING.put("ECU_VCU", new String[]{"CAN-Bus", "0x200"});   // 整车控制
        PROTOCOL_ROUTING.put("ECU_BMS", new String[]{"CAN-Bus", "0x300"});   // 电池管理
        PROTOCOL_ROUTING.put("ECU_ADAS", new String[]{"CAN-Bus", "0x400"});  // ADAS系统
        
        // V2X 用于车外V2I/V2V通信
        PROTOCOL_ROUTING.put("RSU_TrafficLight", new String[]{"V2X", "C-V2X"});
        PROTOCOL_ROUTING.put("RSU_RoadCondition", new String[]{"V2X", "LTE-V"});
        PROTOCOL_ROUTING.put("VEHICLE_OTHER", new String[]{"V2X", "PC5"});
        
        // 无人机通信
        PROTOCOL_ROUTING.put("UAV_DJI", new String[]{"MAVLink", "DJI_Pro"});
        PROTOCOL_ROUTING.put("UAV_PX4", new String[]{"MAVLink", "PX4"});
        PROTOCOL_ROUTING.put("UAV_DroneCAN", new String[]{"DroneCAN", "v1"});
    }
    
    // 构造函数
    public VehicleProtocolGateway() {
        this.registry = ProtocolRegistry.getInstance();
        this.dds = DDSManager.getInstance();
        this.canBus = new CANBusAdapter();
        this.v2x = new V2XAdapter();
        this.droneCAN = new DroneCANAdapter();
        
        // 初始化CAN处理器
        initCANHandlers();
        
        System.out.println(TAG + " 协议网关初始化完成");
    }
    
    // ========== CAN处理器初始化 ==========
    
    private void initCANHandlers() {
        // 动力系统 (0x100)
        canHandlers.put(0x100, this::handlePowertrainCAN);
        
        // 电池系统 (0x200)
        canHandlers.put(0x200, this::handleBatteryCAN);
        
        // 底盘系统 (0x110)
        canHandlers.put(0x110, this::handleChassisCAN);
        
        // ADAS/雷达 (0x300)
        canHandlers.put(0x300, this::handleRadarCAN);
        
        // IMU (0x400)
        canHandlers.put(0x400, this::handleIMUCAN);
    }
    
    // ========== CAN-Bus 处理 ==========
    
    /**
     * 处理收到的CAN消息
     * 
     * @param canId CAN ID
     * @param data CAN数据
     * @return 解析后的车辆状态
     */
    public VehicleSensorData.VehicleStatus handleCANMessage(int canId, byte[] data) {
        CANMessageHandler handler = canHandlers.get(canId);
        if (handler != null) {
            return handler.handle(canId, data);
        }
        
        System.out.println(TAG + " 未处理的CAN ID: 0x" + 
            Integer.toHexString(canId).toUpperCase());
        return null;
    }
    
    /**
     * 处理动力系统CAN消息
     */
    private VehicleSensorData.VehicleStatus handlePowertrainCAN(int canId, byte[] data) {
        return SensorAdapter.VehicleStatusAdapter.parseCANFrame(canId, data);
    }
    
    /**
     * 处理电池系统CAN消息
     */
    private VehicleSensorData.VehicleStatus handleBatteryCAN(int canId, byte[] data) {
        return SensorAdapter.VehicleStatusAdapter.parseCANFrame(canId, data);
    }
    
    /**
     * 处理底盘系统CAN消息
     */
    private VehicleSensorData.VehicleStatus handleChassisCAN(int canId, byte[] data) {
        return SensorAdapter.VehicleStatusAdapter.parseCANFrame(canId, data);
    }
    
    /**
     * 处理雷达CAN消息
     */
    private VehicleSensorData.VehicleStatus handleRadarCAN(int canId, byte[] data) {
        // 雷达数据不直接返回车辆状态，而是返回感知结果
        VehicleSensorData.RadarTarget[] targets = 
            SensorAdapter.RadarAdapter.parseCANMessage(data);
        
        System.out.println(TAG + " 雷达检测到 " + targets.length + " 个目标");
        return null;
    }
    
    /**
     * 处理IMU CAN消息
     */
    private VehicleSensorData.VehicleStatus handleIMUCAN(int canId, byte[] data) {
        VehicleSensorData.IMUData imu = SensorAdapter.IMUAdapter.parseCANMessage(data);
        
        System.out.println(TAG + " IMU: " + imu);
        return null;
    }
    
    // ========== CAN → DDS 转换 ==========
    
    /**
     * CAN消息 → DDS车辆数据
     * 
     * 应用场景：从ECU获取数据后发布到DDS网络
     */
    public VehicleData canToDDS(int canId, byte[] data, String vehicleId) {
        VehicleSensorData.VehicleStatus status = handleCANMessage(canId, data);
        if (status == null) return null;
        
        VehicleData vehicleData = new VehicleData();
        vehicleData.vehicle_id = vehicleId;
        vehicleData.speed = status.speed / 3.6;  // km/h → m/s
        vehicleData.battery_level = (int) status.batterySOC;
        vehicleData.heading = status.steeringAngle;
        vehicleData.timestamp = (int)(System.currentTimeMillis() / 1000);
        
        // 发布到DDS
        if (dds.isConnected()) {
            dds.publishVehicleData(vehicleData);
        }
        
        return vehicleData;
    }
    
    // ========== DDS → CAN 转换 ==========
    
    /**
     * DDS车辆命令 → CAN控制指令
     * 
     * 应用场景：将DDS接收到的控制命令发送到ECU
     */
    public byte[] ddsToCAN(VehicleCommand command) {
        CANBusAdapter.CANFrame frame = new CANBusAdapter.CANFrame();
        
        // 根据命令目标选择CAN ID
        switch (command.target) {
            case ECU_CHASSIS:
                frame.setCanId(getCANIdForTarget(command));
                frame.setData(encodeChassisCommand(command));
                break;
                
            case ECU_BODY:
                frame.setCanId(0x110);
                frame.setData(encodeBodyCommand(command));
                break;
                
            case HVAC:
                frame.setCanId(0x120);
                frame.setData(encodeHVACCommand(command));
                break;
                
            default:
                System.err.println(TAG + " 不支持的命令目标: " + command.target);
                return null;
        }
        
        // CAN编码
        byte[] canData = canBus.encode(frame);
        
        // 发送到CAN总线
        sendToCANBus(canData, frame.getCanId());
        
        System.out.println(TAG + " 发送CAN: ID=0x" + 
            Integer.toHexString(frame.getCanId()).toUpperCase());
        
        return canData;
    }
    
    /**
     * 获取命令目标对应的CAN ID
     */
    private int getCANIdForTarget(VehicleCommand command) {
        switch (command.type) {
            case EMERGENCY_BRAKE:
            case NORMAL_BRAKE:
                return 0x101;  // 制动控制
            case ACCELERATE:
                return 0x102;  // 油门控制
            case STEERING:
                return 0x103;  // 转向控制
            default:
                return 0x100;
        }
    }
    
    /**
     * 编码底盘控制命令
     */
    private byte[] encodeChassisCommand(VehicleCommand cmd) {
        byte[] data = new byte[8];
        
        switch (cmd.type) {
            case EMERGENCY_BRAKE:
            case NORMAL_BRAKE:
                // 制动: [brake_cmd(2), unused(6)]
                int brakeCmd = (int)(-cmd.targetAcceleration * 100);
                data[0] = (byte) (brakeCmd & 0xFF);
                data[1] = (byte) ((brakeCmd >> 8) & 0xFF);
                break;
                
            case ACCELERATE:
                // 油门: [speed_cmd(2), accel_cmd(2), unused(4)]
                int speedCmd = (int)(cmd.targetSpeed * 100);
                int accelCmd = (int)(cmd.targetAcceleration * 100);
                data[0] = (byte) (speedCmd & 0xFF);
                data[1] = (byte) ((speedCmd >> 8) & 0xFF);
                data[2] = (byte) (accelCmd & 0xFF);
                data[3] = (byte) ((accelCmd >> 8) & 0xFF);
                break;
                
            case STEERING:
                // 转向: [steer_cmd(2), unused(6)]
                int steerCmd = (int)(cmd.targetSteering * 100) + 32768;
                data[0] = (byte) (steerCmd & 0xFF);
                data[1] = (byte) ((steerCmd >> 8) & 0xFF);
                break;
        }
        
        return data;
    }
    
    /**
     * 编码车身控制命令
     */
    private byte[] encodeBodyCommand(VehicleCommand cmd) {
        byte[] data = new byte[8];
        
        switch (cmd.type) {
            case DOOR_LOCK:
                data[0] = 0x01;  // 锁止
                break;
            case DOOR_UNLOCK:
                data[0] = 0x02;  // 解锁
                break;
            case HORN:
                data[0] = 0x10;  // 鸣笛
                break;
            case LIGHTS_ON:
                data[0] = 0x20;  // 开灯
                break;
            case LIGHTS_OFF:
                data[0] = 0x30;  // 关灯
                break;
        }
        
        return data;
    }
    
    /**
     * 编码空调控制命令
     */
    private byte[] encodeHVACCommand(VehicleCommand cmd) {
        byte[] data = new byte[8];
        
        if (cmd.type == VehicleCommand.CommandType.SET_TEMPERATURE) {
            Double temp = (Double) cmd.parameters.get("temperature");
            if (temp != null) {
                int tempCmd = (int)((temp + 40) * 2);  // -40°C ~ +40°C
                data[0] = (byte) (tempCmd & 0xFF);
            }
        } else if (cmd.type == VehicleCommand.CommandType.HVAC_ON) {
            data[0] = 0x01;
        } else if (cmd.type == VehicleCommand.CommandType.HVAC_OFF) {
            data[0] = 0x00;
        }
        
        return data;
    }
    
    // ========== V2X 处理 ==========
    
    /**
     * 处理收到的V2X消息
     */
    public void handleV2XMessage(VehicleSensorData.V2XMessage msg) {
        System.out.println(TAG + " 收到V2X消息: " + msg.type + " from " + msg.senderId);
        
        // 分发到处理器
        for (V2XMessageHandler handler : v2xHandlers) {
            handler.handle(msg);
        }
        
        // 转换为DDS格式
        if ("BSM".equals(msg.type)) {
            handleV2XBSM(msg);
        } else if ("SPaT".equals(msg.type)) {
            handleV2XSPAT(msg);
        }
    }
    
    /**
     * 处理V2X BSM消息
     */
    private void handleV2XBSM(VehicleSensorData.V2XMessage msg) {
        // 转换为DDS VehicleData
        VehicleData data = new VehicleData();
        data.vehicle_id = msg.senderId;
        data.latitude = msg.latitude;
        data.longitude = msg.longitude;
        data.speed = msg.speed;
        data.heading = msg.heading;
        
        // 发布到DDS
        if (dds.isConnected()) {
            dds.publishVehicleData(data);
        }
    }
    
    /**
     * 处理V2X SPaT消息
     */
    private void handleV2XSPAT(VehicleSensorData.V2XMessage msg) {
        // 转换为DDS TrafficFlow
        TrafficFlow flow = new TrafficFlow();
        flow.zone_id = "intersection_" + msg.senderId;
        flow.latitude = msg.latitude;
        flow.longitude = msg.longitude;
        
        // 从extraData解析相位信息并转换为拥堵级别
        Object phase = msg.extraData.get("phase");
        Object timeToChange = msg.extraData.get("timeToChange");
        if (timeToChange != null) {
            // 信号灯时间转换为拥堵信息
            flow.congestion_level = 0.5;  // 简化处理
        }
        
        // 发布到DDS
        if (dds.isConnected()) {
            dds.publishTrafficFlow(flow);
        }
    }
    
    // ========== DDS → V2X 转换 ==========
    
    /**
     * DDS车辆数据 → V2X BSM广播
     * 
     * 应用场景：将本车状态广播给周围车辆
     */
    public VehicleSensorData.V2XMessage ddsToV2X(VehicleData data) {
        VehicleSensorData.V2XMessage msg = new VehicleSensorData.V2XMessage("BSM");
        
        msg.senderId = data.vehicle_id;
        msg.latitude = data.latitude;
        msg.longitude = data.longitude;
        msg.speed = data.speed;
        msg.heading = data.heading;
        
        // 通过V2X模块发送
        // v2x.transmit(msg);
        
        System.out.println(TAG + " 广播V2X BSM: " + data.vehicle_id);
        
        return msg;
    }
    
    // ========== MAVLink 处理 ==========
    
    /**
     * MAVLink消息 → DDS无人机状态
     */
    public EmergencyStatus mavlinkToDDS(byte[] mavlinkData) {
        // 解析MAVLink
        Map<String, Object> msg = parseMAVLink(mavlinkData);
        if (msg == null) return null;
        
        int msgId = (int) msg.getOrDefault("msg_id", -1);
        
        chongqing.EmergencyStatus status = new chongqing.EmergencyStatus();
        
        switch (msgId) {
            case 33:  // GLOBAL_POSITION_INT
                chongqing.Position3D pos = new chongqing.Position3D();
                pos.latitude = (int) msg.get("lat") / 1E7;
                pos.longitude = (int) msg.get("lon") / 1E7;
                pos.altitude = (int) msg.get("relative_alt") / 1000;
                status.current_position = pos;
                status.speed = (int) msg.getOrDefault("vx", 0) / 100;
                break;
                
            case 1:  // SYS_STATUS
                status.battery_level = (int) msg.getOrDefault("battery_remaining", 100);
                break;
                
            default:
                System.out.println(TAG + " 未处理的MAVLink消息: " + msgId);
                return null;
        }
        
        status.drone_id = "UAV_" + msg.getOrDefault("sys_id", "unknown");
        status.device_type = chongqing.DeviceType.DRONE_MULTIROTOR;
        
        // 发布到DDS
        if (dds.isConnected()) {
            dds.publishEmergencyStatus(status);
        }
        
        return status;
    }
    
    /**
     * DDS任务命令 → MAVLink控制指令
     */
    public byte[] ddsToMAVLink(chongqing.MissionCommand cmd) {
        Map<String, Object> mavMsg = new HashMap<>();
        
        mavMsg.put("msg_id", 39);  // MISSION_ITEM
        mavMsg.put("target_system", extractSystemId(cmd.drone_id));
        mavMsg.put("target_component", 1);
        mavMsg.put("command", 16);  // MAV_CMD_NAV_WAYPOINT
        
        // 解析目标位置
        // cmd.parameters 应包含目标坐标
        mavMsg.put("x", 29.55);  // 简化
        mavMsg.put("y", 106.55);
        mavMsg.put("z", 100.0);
        
        // 编码为MAVLink二进制
        return encodeMAVLink(mavMsg);
    }
    
    /**
     * 解析MAVLink消息
     */
    private Map<String, Object> parseMAVLink(byte[] data) {
        if (data == null || data.length < 10) return null;
        
        Map<String, Object> msg = new HashMap<>();
        
        // 简化解析：实际应实现完整的MAVLink解析
        msg.put("msg_id", data[5] & 0xFF);
        msg.put("sys_id", data[3] & 0xFF);
        msg.put("comp_id", data[4] & 0xFF);
        
        // 解析载荷（简化）
        if (msg.get("msg_id").equals(33)) {  // GLOBAL_POSITION_INT
            if (data.length >= 28) {
                msg.put("lat", (data[6] & 0xFF) | ((data[7] & 0xFF) << 8) |
                    ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 24));
                // ... 更多字段
            }
        }
        
        return msg;
    }
    
    /**
     * 编码MAVLink消息
     */
    private byte[] encodeMAVLink(Map<String, Object> msg) {
        // 简化实现
        byte[] packet = new byte[32];
        
        // MAVLink头部
        packet[0] = (byte) 0xFE;  // 起始符
        packet[1] = (byte) 24;  // 长度
        packet[2] = (byte) 0x01;  // 不打包
        packet[3] = (byte) 1;  // 系统ID
        packet[4] = (byte) 1;  // 组件ID
        packet[5] = (byte) ((int) msg.getOrDefault("msg_id", 0));
        
        // 载荷将在后续字节中编码
        
        return packet;
    }
    
    // ========== 协议路由 ==========
    
    /**
     * 根据目标设备自动选择协议
     */
    public String autoSelectProtocol(String destination) {
        String[] route = PROTOCOL_ROUTING.get(destination);
        if (route != null) {
            return route[0];  // 返回协议类型
        }
        
        // 默认协议
        return "DDS";
    }
    
    /**
     * 统一协议转换入口
     */
    public Object convert(String fromProtocol, String toProtocol, Object data) {
        return registry.convert(fromProtocol, toProtocol, data);
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 发送数据到CAN总线（模拟）
     */
    private void sendToCANBus(byte[] data, int canId) {
        // 实际实现需要与CAN驱动交互
        System.out.println(TAG + " 发送CAN: ID=0x" + 
            Integer.toHexString(canId).toUpperCase() + 
            " 数据长度=" + data.length);
    }
    
    /**
     * 从设备ID提取系统ID
     */
    private int extractSystemId(String deviceId) {
        // 简化：假设ID格式为 "TYPE_XXX"
        try {
            String num = deviceId.replaceAll("[^0-9]", "");
            return num.isEmpty() ? 1 : Integer.parseInt(num);
        } catch (Exception e) {
            return 1;
        }
    }
    
    // ========== 回调接口 ==========
    
    /**
     * CAN消息处理器
     */
    @FunctionalInterface
    public interface CANMessageHandler {
        VehicleSensorData.VehicleStatus handle(int canId, byte[] data);
    }
    
    /**
     * V2X消息处理器
     */
    public interface V2XMessageHandler {
        void handle(VehicleSensorData.V2XMessage msg);
    }
    
    /**
     * 注册V2X消息处理器
     */
    public void registerV2XHandler(V2XMessageHandler handler) {
        v2xHandlers.add(handler);
    }
}
