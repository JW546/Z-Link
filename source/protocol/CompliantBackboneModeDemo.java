package protocol;

/**
 * 合规广域链路真实模式接入示例
 * 
 * 如何连接真实合规广域链路网络：
 * 1. 获取国内批准系统或境内运营服务方API访问权限
 * 2. 获取合规广域链路终端设备
 * 3. 配置环境变量
 * 4. 运行本示例
 */
public class CompliantBackboneModeDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 合规广域链路真实模式接入演示 ===\n");
        
        CompliantBackboneAdapter adapter = CompliantBackboneAdapter.createLEOAdapter("demo-terminal-001");
        
        // [方式1] 环境变量
        System.out.println("[方式1] 通过环境变量启用");
        System.out.println("  set COMPLIANT_BACKBONE_API_KEY=your_key");
        System.out.println("  set COMPLIANT_BACKBONE_TERMINAL_ID=your_id");
        System.out.println("  adapter.enableRealModeFromEnv();");
        System.out.println();
        
        String apiKey = System.getenv("COMPLIANT_BACKBONE_API_KEY");
        String terminalId = System.getenv("COMPLIANT_BACKBONE_TERMINAL_ID");
        
        if (apiKey != null && terminalId != null) {
            System.out.println("检测到环境变量，连接真实合规广域链路...");
            boolean success = adapter.enableRealModeFromEnv();
            if (success) {
                testRealModeFeatures(adapter);
            }
        } else {
            System.out.println("未检测到环境变量，使用模拟模式\n");
            demonstrateMockMode(adapter);
        }
        
        // [方式2] 直接参数
        System.out.println("\n[方式2] 直接传入参数");
        System.out.println("  adapter.enableRealMode(\"api_key\", \"ut-001234567890\");");
        
        // [方式3] 配置文件
        System.out.println("\n[方式3] 使用配置文件");
        System.out.println("  CompliantBackboneRealClient client = CompliantBackboneRealClient.createFromConfig();");
        
        // 真实模式功能列表
        System.out.println("\n=== 真实模式API ===");
        System.out.println("  getRealTerminalStatus()     - 获取终端状态");
        System.out.println("  getRealVisibleSatellites()  - 获取可见卫星");
        System.out.println("  sendRealTelemetry(...)      - 发送遥测数据");
        System.out.println("  getRealConnectionLatencyNs() - 获取真实延迟");
        
        // 注意事项
        System.out.println("\n=== 注意事项 ===");
        System.out.println("1. API密钥不要提交到代码仓库");
        System.out.println("2. 使用环境变量或密钥管理服务");
        System.out.println("3. 合规广域链路终端需要露天环境");
        System.out.println("4. 部分API需额外权限申请");
        
        System.out.println("\n=== 演示结束 ===");
    }
    
    private static void demonstrateMockMode(CompliantBackboneAdapter adapter) {
        System.out.println("启动模拟模式...");
        adapter.connect();
        System.out.println("\n模拟模式功能:");
        System.out.println("  - 卫星扫描");
        System.out.println("  - 授时同步");
        System.out.println("  - 数据收发");
        System.out.println("  - 协议转换");
        adapter.disconnect();
    }
    
    private static void testRealModeFeatures(CompliantBackboneAdapter adapter) {
        System.out.println("\n测试真实模式功能:");
        
        CompliantBackboneRealClient.TerminalStatus status = adapter.getRealTerminalStatus();
        if (status != null) {
            System.out.println("\n终端状态:");
            System.out.println("  硬件ID: " + status.hardwareId);
            System.out.println("  下行带宽: " + status.downlinkThroughputMbps + " Mbps");
            System.out.println("  上行带宽: " + status.uplinkThroughputMbps + " Mbps");
            System.out.println("  Ping延迟: " + status.pingMs + " ms");
            System.out.println("  天线对准: " + status.dishAligned);
        }
        
        CompliantBackboneRealClient.SatelliteStatus[] satellites = adapter.getRealVisibleSatellites();
        if (satellites != null && satellites.length > 0) {
            System.out.println("\n可见卫星 (" + satellites.length + "颗)");
            for (CompliantBackboneRealClient.SatelliteStatus sat : satellites) {
                System.out.println("  " + sat);
            }
        }
        
        boolean sent = adapter.sendRealTelemetry(29.5647, 106.5507, 300.0, 60.0, 45.0, 85.0, -65.0);
        System.out.println("\n遥测数据发送: " + (sent ? "成功" : "失败"));
        
        long latencyNs = adapter.getRealConnectionLatencyNs();
        System.out.println("真实延迟: " + (latencyNs / 1_000_000.0) + " ms");
    }
}
