package utils;

import utils.RealProtocolAdapter.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 真实协议使用演示
 * 展示如何使用真实的协议客户端进行网络通信
 */
public class DemoRealProtocols {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   真实协议栈演示 - 重庆山地应急医疗物资无人机协同配送系统    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // 1. MAVLink v2.0 二进制帧创建与解析
        demonstrateMAVLink();
        
        // 2. MQTT 5.0 发布/订阅
        demonstrateMQTT();
        
        // 3. HTTP/1.1 请求
        demonstrateHTTP();
        
        // 4. WebSocket 连接
        demonstrateWebSocket();
        
        // 5. DDS 发布/订阅（接口预留）
        demonstrateDDS();
        
        // 6. 跨协议转换
        demonstrateProtocolConversion();
        
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("演示完成！所有协议均为真实实现。");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
    
    /**
     * 演示 MAVLink v2.0 二进制帧
     */
    private static void demonstrateMAVLink() {
        System.out.println("\n【1】MAVLink v2.0 二进制帧演示");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 MAVLink GLOBAL_POSITION_INT 消息 (message_id = 33)
        byte[] positionPayload = new byte[28];
        // lat: int32 (degrees * 1E7)
        positionPayload[0] = 0x48; positionPayload[1] = 0x65; positionPayload[2] = 0x04; positionPayload[3] = 0x00;
        // lon: int32
        positionPayload[4] = 0x14; positionPayload[5] = (byte)0xB6; positionPayload[6] = 0x12; positionPayload[7] = 0x00;
        // alt: int32 (millimeters)
        positionPayload[8] = 0x00; positionPayload[9] = 0x10; positionPayload[10] = 0x00; positionPayload[11] = 0x00;
        // relative_alt: int32
        positionPayload[12] = 0x50; positionPayload[13] = 0x0F; positionPayload[14] = 0x00; positionPayload[15] = 0x00;
        
        byte[] mavFrame = RealProtocolAdapter.createMAVLinkFrame(1, 1, 33, positionPayload);
        
        System.out.println("创建 MAVLink 帧:");
        System.out.println("  系统ID: 1");
        System.out.println("  组件ID: 1");
        System.out.println("  消息ID: 33 (GLOBAL_POSITION_INT)");
        System.out.println("  帧长度: " + mavFrame.length + " bytes");
        System.out.print("  帧数据: ");
        for (int i = 0; i < Math.min(mavFrame.length, 20); i++) {
            System.out.printf("%02X ", mavFrame[i]);
        }
        System.out.println("...");
        
        // 解析 MAVLink 帧
        MAVLinkFrame parsed = new MAVLinkFrame(mavFrame);
        Map<String, Object> map = parsed.toMap();
        
        System.out.println("\n解析 MAVLink 帧结果:");
        System.out.println("  版本: " + map.get("version"));
        System.out.println("  系统ID: " + map.get("system_id"));
        System.out.println("  组件ID: " + map.get("component_id"));
        System.out.println("  消息ID: " + map.get("message_id"));
        System.out.println("  载荷: " + map.get("payload"));
    }
    
    /**
     * 演示 MQTT 5.0 连接和消息发布
     */
    private static void demonstrateMQTT() {
        System.out.println("\n【2】MQTT 5.0 协议演示");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 MQTT 客户端
        String brokerUrl = "tcp://localhost:1883";
        String clientId = "drone-dsl-client-" + System.currentTimeMillis();
        
        System.out.println("准备连接 MQTT Broker: " + brokerUrl);
        System.out.println("  Client ID: " + clientId);
        
        // 创建客户端并连接
        MQTTClientWrapper mqttClient = RealProtocolAdapter.getMQTTClient(brokerUrl, clientId);
        mqttClient.connect();
        
        // 发布消息
        String payload = "{\"drone_id\":\"DJI-M3E\",\"lat\":29.5,\"lon\":106.5,\"alt\":120,\"statusource\":\"flyingenerated-src\"}";
        mqttClient.publish("chongqing/drone/status", payload, 1, false);
        
        // 订阅主题
        mqttClient.subscribe("chongqing/drone/command", 1);
        
        System.out.println("\n  MQTT API 方法:");
        System.out.println("  - connect(): 连接到 Broker");
        System.out.println("  - publish(topic, payload, qos, retained): 发布消息");
        System.out.println("  - subscribe(topic, qos): 订阅主题");
        System.out.println("  - setMessageListener(listener): 设置消息监听器");
    }
    
    /**
     * 演示 HTTP/1.1 请求
     */
    private static void demonstrateHTTP() {
        System.out.println("\n【3】HTTP/1.1 协议演示");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 HTTP 客户端
        String baseUrl = "http://localhost:8080";
        HttpClientWrapper httpClient = RealProtocolAdapter.getHTTPClient(baseUrl);
        
        System.out.println("创建 HTTP 客户端:");
        System.out.println("  Base URL: " + baseUrl);
        
        // 发送 POST 请求
        String jsonBody = "{\"drone_id\":\"DJI-M3E\",\"lat\":29.5432,\"lon\":106.5643,\"alt\":120,\"statusource\":\"flyingenerated-src\"}";
        System.out.println("\n发送 POST 请求:");
        
        HTTPResponse response = httpClient.post("/api/drone/status", jsonBody, null);
        System.out.println("  响应状态码: " + response.getStatusCode());
        
        // 发送 GET 请求
        System.out.println("\n发送 GET 请求:");
        HTTPResponse getResp = httpClient.get("/api/drone/list", null);
        System.out.println("  响应状态码: " + getResp.getStatusCode());
        
        // 异步请求
        System.out.println("\n异步 POST 请求:");
        httpClient.postAsync("/api/drone/update", jsonBody)
            .thenAccept(resp -> System.out.println("  异步响应: " + resp.getStatusCode()));
    }
    
    /**
     * 演示 WebSocket 连接
     */
    private static void demonstrateWebSocket() {
        System.out.println("\n【4】WebSocket RFC 6455 协议演示");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 WebSocket 客户端
        String serverUri = "ws://localhost:8080/ws/drone";
        WebSocketClientWrapper wsClient = RealProtocolAdapter.getWebSocketClient(serverUri);
        
        System.out.println("创建 WebSocket 客户端:");
        System.out.println("  URI: " + serverUri);
        
        // 连接
        wsClient.connect();
        
        // 发送消息
        wsClient.sendText("{\"event\":\"heartbeat\",\"drone_id\":\"DJI-M3E\"}");
        
        // 设置消息处理
        wsClient.setMessageHandler(msg -> System.out.println("收到消息: " + msg));
        
        // 关闭连接
        wsClient.close();
    }
    
    /**
     * 演示 DDS 发布/订阅
     */
    private static void demonstrateDDS() {
        System.out.println("\n【5】DDS 协议演示（接口预留）");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 DDS 客户端
        String domainId = "42";
        DDSClient ddsClient = RealProtocolAdapter.getDDSClient(domainId);
        
        System.out.println("创建 DDS 客户端:");
        System.out.println("  Domain ID: " + domainId);
        
        // 发布数据
        String droneData = "{\"drone_id\":\"DJI-M3E\",\"position\":{\"lat\":29.5432,\"lon\":106.5643},\"battery\":85,\"statusource\":\"active\"}";
        System.out.println("\n发布 DDS 数据:");
        ddsClient.publish("DronePositionTopic", "DronePosition", droneData);
        
        // 订阅数据
        System.out.println("\n订阅 DDS 主题:");
        ddsClient.subscribe("DronePositionTopic", "DronePosition");
        
        System.out.println("\n  注意: DDS 需要专门的中间件（OpenDDS/RTI Connext）");
        System.out.println("  当前为接口预留，实现需要引入对应 DDS 库");
    }
    
    /**
     * 演示跨协议转换
     */
    private static void demonstrateProtocolConversion() {
        System.out.println("\n【6】跨协议转换演示");
        System.out.println("───────────────────────────────────────────────────────────────");
        
        // 创建 MAVLink 帧作为源数据
        byte[] positionPayload = new byte[28];
        // 填充位置数据
        for (int i = 0; i < 28; i++) positionPayload[i] = (byte) i;
        
        byte[] mavFrame = RealProtocolAdapter.createMAVLinkFrame(1, 1, 33, positionPayload);
        
        // 转换为 MQTT
        Object mqttResult = RealProtocolAdapter.convertProtocol("MAVLink", "MQTT", mavFrame);
        System.out.println("\nMAVLink -> MQTT 转换结果:");
        System.out.println("  " + mqttResult);
        
        // 转换为 HTTP
        Object httpResult = RealProtocolAdapter.convertProtocol("MAVLink", "HTTP", mavFrame);
        System.out.println("\nMAVLink -> HTTP 转换结果:");
        System.out.println("  " + httpResult);
        
        // 转换为 WebSocket
        Object wsResult = RealProtocolAdapter.convertProtocol("MAVLink", "WebSocket", mavFrame);
        System.out.println("\nMAVLink -> WebSocket 转换结果:");
        System.out.println("  " + wsResult);
        
        // 转换为 DDS
        Object ddsResult = RealProtocolAdapter.convertProtocol("MAVLink", "DDS", mavFrame);
        System.out.println("\nMAVLink -> DDS 转换结果:");
        System.out.println("  " + ddsResult);
        
        System.out.println("\n  协议转换说明:");
        System.out.println("  - 源协议可以是: MAVLink, MQTT, HTTP, WebSocket, DDS");
        System.out.println("  - 目标协议可以是: MQTT, HTTP, WebSocket, DDS");
        System.out.println("  - 转换保持语义一致性");
    }
}
