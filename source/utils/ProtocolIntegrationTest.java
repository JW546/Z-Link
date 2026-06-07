package utils;

import protocol.*;
import java.util.Map;

/**
 * 协议适配器集成测试
 */
public class ProtocolIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== 协议适配器集成测试 ===\n");
        
        // 初始化
        System.out.println("[初始化] 协议注册中心");
        RealProtocolAdapter.initializeProtocolRegistry();
        
        // 测试适配器
        System.out.println("\n[测试1] 新协议适配器");
        testNewAdapters();
        
        // 测试转换
        System.out.println("\n[测试2] 协议转换");
        testProtocolConversions();
        
        // 测试路由
        System.out.println("\n[测试3] 协议路由");
        testProtocolRouting();
        
        // 性能测试
        System.out.println("\n[测试4] 性能基准");
        runPerformanceTest();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static void testNewAdapters() {
        // CAN-Bus
        System.out.println("CAN-Bus适配器:");
        CANBusAdapter canAdapter = RealProtocolAdapter.getCANBusAdapter();
        canAdapter.addAcceptableId(0x100);
        CANBusAdapter.CANFrame frame = canAdapter.createStandardFrame(
            0x123, new byte[]{0x11, 0x22, 0x33, 0x44}
        );
        canAdapter.sendFrame(frame);
        System.out.println("  帧统计: " + canAdapter.getStatistics());
        
        // V2X
        System.out.println("\nV2X适配器:");
        V2XAdapter v2xAdapter = RealProtocolAdapter.getV2XAdapter();
        v2xAdapter.updateVehicleState(29.564709, 106.550781, 36.0, 45.0);
        v2xAdapter.broadcastBSM();
        System.out.println("  V2X统计: " + v2xAdapter.getStatistics());
        
        // ISO-TP
        System.out.println("\nISO-TP适配器:");
        ISO_TPAdapter isoAdapter = RealProtocolAdapter.getISOTPAdapter();
        byte[] diagRequest = isoAdapter.createDiagnosticRequest(0x10, null);
        System.out.println("  诊断请求: " + formatHex(diagRequest));
        
        // DroneCAN
        System.out.println("\nDroneCAN适配器:");
        DroneCANAdapter droneAdapter = RealProtocolAdapter.getDroneCANAdapter();
        droneAdapter.sendHeartbeat();
        System.out.println("  DroneCAN统计: " + droneAdapter.getStatistics());
    }
    
    private static void testProtocolConversions() {
        String ddsData = "drone_id=1,lat=29.564709,lon=106.550781,speed=36.5,heading=45.0";
        
        // DDS -> V2X
        V2XAdapter.BSMMessage bsm = RealProtocolAdapter.convertDDSToV2X(ddsData);
        System.out.println("DDS -> V2X: " + bsm);
        
        // V2X -> DDS
        String ddsResult = RealProtocolAdapter.convertV2XToDDS(bsm);
        System.out.println("V2X -> DDS: " + ddsResult);
        
        // DDS -> CAN
        CANBusAdapter.CANFrame canFrame = RealProtocolAdapter.convertDDSToCAN(ddsData);
        System.out.println("DDS -> CAN: " + canFrame);
        
        // MAVLink -> DroneCAN
        byte[] mavPayload = new byte[]{0x01, 0x02, 0x03, 0x04};
        DroneCANAdapter dc = new DroneCANAdapter();
        DroneCANAdapter.DroneCANMessage droneMsg = dc.createDataMessage(33, mavPayload);
        System.out.println("MAVLink -> DroneCAN: " + droneMsg);
    }
    
    private static void testProtocolRouting() {
        ProtocolRouter router = ProtocolRouter.getInstance();
        
        // 注册设备
        router.registerDevice("DRONE-001", ProtocolRouter.DeviceType.DRONE, 
            ProtocolRouter.ProtocolType.MAVLINK, ProtocolRouter.ProtocolType.DRONECAN);
        router.registerDevice("VEHICLE-001", ProtocolRouter.DeviceType.VEHICLE,
            ProtocolRouter.ProtocolType.V2X, ProtocolRouter.ProtocolType.CAN_BUS);
        router.registerDevice("GCS-001", ProtocolRouter.DeviceType.GCS,
            ProtocolRouter.ProtocolType.MQTT, ProtocolRouter.ProtocolType.DDS);
        
        // 路由决策
        ProtocolRouter.RoutingDecision r1 = router.route("DRONE-001", "GCS-001");
        System.out.println("无人机 -> 地面站: " + r1);
        
        ProtocolRouter.RoutingDecision r2 = router.route("VEHICLE-001", "GCS-001");
        System.out.println("车辆 -> 地面站: " + r2);
        
        // 类型推断
        byte[] v2xData = new byte[]{0x12, 0x34, 0x00, 0x00, 0x00};
        ProtocolRouter.DeviceType inferred = router.inferDeviceType(v2xData);
        System.out.println("V2X数据推断: " + inferred.getDescription());
    }
    
    private static void runPerformanceTest() {
        int iterations = 10000;
        
        // V2X 编码
        V2XAdapter v2x = new V2XAdapter();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            V2XAdapter.BSMMessage bsm = v2x.createBSM();
            bsm.setLatitude(29.5 + i * 0.000001);
            v2x.encode(bsm);
        }
        long end = System.nanoTime();
        double v2xTime = (end - start) / 1_000_000.0;
        System.out.printf("V2X编码 (%d次): 总耗时=%.2fms, 平均=%.3fusource\n", 
            iterations, v2xTime, v2xTime / iterations * 1000);
        
        // CAN-Bus 编码
        CANBusAdapter can = new CANBusAdapter();
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int canId = 0x100 + (i % 0x700);
            CANBusAdapter.CANFrame frame = can.createStandardFrame(canId, 
                new byte[]{(byte) i, (byte) (i >> 8)});
            can.encode(frame);
        }
        end = System.nanoTime();
        double canTime = (end - start) / 1_000_000.0;
        System.out.printf("CAN编码 (%d次): 总耗时=%.2fms, 平均=%.3fusource\n", 
            iterations, canTime, canTime / iterations * 1000);
        
        // ISO-TP 编码
        ISO_TPAdapter iso = new ISO_TPAdapter();
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            iso.createDiagnosticRequest(0x10, null);
        }
        end = System.nanoTime();
        double isoTime = (end - start) / 1_000_000.0;
        System.out.printf("ISO-TP (%d次): 总耗时=%.2fms, 平均=%.3fusource\n", 
            iterations, isoTime, isoTime / iterations * 1000);
        
        // 路由决策
        int routeIterations = 1000;
        ProtocolRouter router = ProtocolRouter.getInstance();
        router.registerDevice("TEST-DRONE", ProtocolRouter.DeviceType.DRONE);
        router.registerDevice("TEST-VEHICLE", ProtocolRouter.DeviceType.VEHICLE);
        
        start = System.nanoTime();
        for (int i = 0; i < routeIterations; i++) {
            router.route("TEST-DRONE", "TEST-VEHICLE");
        }
        end = System.nanoTime();
        double routeTime = (end - start) / 1_000_000.0;
        System.out.printf("路由决策 (%d次): 总耗时=%.2fms, 平均=%.3fusource\n", 
            routeIterations, routeTime, routeTime / routeIterations * 1000);
    }
    
    private static String formatHex(byte[] data) {
        if (data == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i < data.length - 1) sb.append(" ");
        }
        return sb.toString();
    }
}
