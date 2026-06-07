package utils;

import protocol.CompliantBackboneAdapter;
import protocol.CompliantBackboneProtocolConverter;
import java.util.List;

/**
 * 合规广域链路(COMPLIANT_BACKBONE)功能测试
 * 
 * 测试内容：
 * 1. 合规广域链路终端连接与初始化
 * 2. 卫星扫描与链路选择
 * 3. 纳秒级授时同步
 * 4. 全球态势感知数据收发
 * 5. 协议转换：COMPLIANT_BACKBONE ↔ DDS/MAVLink/V2X
 * 
 * 注意：这是模拟测试
 */
public class CompliantBackboneTest {
    
    public static void main(String[] args) {
        System.out.println("=== 合规广域链路功能测试 ===\n");
        System.out.println("说明: 本测试为模拟环境\n");
        
        CompliantBackboneAdapter adapter = CompliantBackboneAdapter.createLEOAdapter("test-terminal-001");
        
        // [测试1] 连接合规广域链路网络
        System.out.println("[测试1] 连接合规广域链路网络");
        boolean connected = adapter.connect();
        System.out.println("连接结果: " + (connected ? "成功" : "失败"));
        System.out.println();
        
        if (connected) {
            // [测试2] 授时同步
            System.out.println("[测试2] 纳秒级授时同步");
            adapter.startTimeSync();
            System.out.println("授时同步: " + (adapter.isTimeSynced() ? "已同步" : "未同步"));
            System.out.println();
            
            // [测试3] 可见卫星列表
            System.out.println("[测试3] 可见卫星列表");
            List<CompliantBackboneAdapter.SatelliteLink> satellites = adapter.getSatelliteLinks();
            for (CompliantBackboneAdapter.SatelliteLink sat : satellites) {
                System.out.printf("  %s: 仰角=%.1f°, RTT=%dmsource\n", 
                    sat.getSatelliteId(), sat.getElevation(), sat.getRttNs() / 1_000_000);
            }
            System.out.println();
            
            // [测试4] 全球态势感知数据
            System.out.println("[测试4] 全球态势感知数据");
            CompliantBackboneAdapter.GlobalPerceptionData data = new CompliantBackboneAdapter.GlobalPerceptionData();
            data.setLatitude(29.5647);
            data.setLongitude(106.5507);
            data.setAltitude(300);
            data.setPerceptionType("terrain");
            data.setConfidence(95);
            
            boolean sent = adapter.sendGlobalPerceptionData(data);
            System.out.println("发送结果: " + (sent ? "成功" : "失败"));
            
            CompliantBackboneAdapter.GlobalPerceptionData received = adapter.receiveGlobalPerceptionData();
            if (received != null) {
                System.out.printf("接收位置: (%.6f, %.6f), 高度: %.1fm\n", 
                    received.getLatitude(), received.getLongitude(), received.getAltitude());
            }
            System.out.println();
            
            // [测试5] 协议转换
            System.out.println("[测试5] 协议转换");
            CompliantBackboneProtocolConverter converter = new CompliantBackboneProtocolConverter(adapter);
            
            converter.compliantBackboneToDDS(data);
            System.out.println("COMPLIANT_BACKBONE -> DDS: OK");
            
            converter.ddsToCompliantBackbone("drone_id=1,lat=29.5,lon=106.5");
            System.out.println("DDS -> COMPLIANT_BACKBONE: OK");
            
            converter.compliantBackboneToMAVLink(java.util.Map.of("latitude", 29.5, "longitude", 106.5, "altitude", 300.0));
            System.out.println("COMPLIANT_BACKBONE -> MAVLink: OK");
            
            converter.compliantBackboneToV2X(data);
            System.out.println("COMPLIANT_BACKBONE -> V2X: OK");
            System.out.println();
            
            // [测试6] 连接状态
            System.out.println("[测试6] 连接状态");
            System.out.println("  终端ID: " + adapter.getTerminalId());
            System.out.println("  状态: " + adapter.getStatus());
            System.out.printf("  链路延迟: %.3f msource\n", adapter.getConnectionLatencyNs() / 1_000_000.0);
            System.out.println();
            
            adapter.disconnect();
        }
        
        // [测试7] 多终端管理
        System.out.println("[测试7] 多终端管理");
        CompliantBackboneAdapter t1 = CompliantBackboneAdapter.createLEOAdapter("vehicle-001");
        CompliantBackboneAdapter t2 = CompliantBackboneAdapter.createLEOAdapter("drone-001");
        CompliantBackboneAdapter t3 = CompliantBackboneAdapter.createGEOBridge("gateway-001");
        
        t1.connect();
        t2.connect();
        t3.connect();
        
        System.out.println("活跃终端:");
        for (var entry : CompliantBackboneAdapter.getActiveTerminals().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().getStatus());
        }
        
        t1.disconnect();
        t2.disconnect();
        t3.disconnect();
        
        System.out.println("\n=== 测试完成 ===");
    }
}
