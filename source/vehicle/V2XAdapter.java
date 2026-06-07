package vehicle;

import java.util.*;

/**
 * V2X (Vehicle-to-Everything) 适配器
 * 
 * 支持的V2X通信模式：
 * - C-V2X: 基于蜂窝网络的V2X
 * - LTE-V: LTE车联网
 * - PC5: 直连通信（车到车）
 * 
 * 支持的V2X消息类型：
 * - BSM (Basic Safety Message): 基本安全消息
 * - SPaT (Signal Phase and Timing): 信号灯信息
 * - MAP (Map): 路口地图信息
 * - RSI (Road Side Information): 路侧信息
 * - RSM (Road Safety Message): 路侧安全消息
 * 
 * @paper_ref 第4章 4.4 协议适配层 -> V2X协议
 */
public class V2XAdapter {
    
    private static final String TAG = "[V2XAdapter]";
    
    // V2X通信模式
    public enum V2XMode {
        C_V2X,   // 蜂窝V2X
        LTE_V,   // LTE车联网
        PC5      // 直连模式
    }
    
    // V2X消息类型
    public enum V2XMessageType {
        BSM,    // 基本安全消息
        SPAT,   // 信号灯信息
        MAP,    // 路口地图
        RSI,    // 路侧信息
        RSM     // 路侧安全消息
    }
    
    // 当前通信模式
    private V2XMode mode = V2XMode.C_V2X;
    
    // 通信状态
    private boolean connected = false;
    private String cellId = "";
    
    // V2X消息监听器
    private List<V2XMessageListener> listeners = new ArrayList<>();
    
    // ========== 消息发送 ==========
    
    /**
     * 发送V2X消息
     */
    public void transmit(VehicleSensorData.V2XMessage msg) {
        if (!connected) {
            System.out.println(TAG + " V2X未连接，消息发送失败");
            return;
        }
        
        // 根据消息类型选择发送方式
        switch (msg.type) {
            case "BSM":
                transmitBSM(msg);
                break;
            case "SPaT":
                transmitSPAT(msg);
                break;
            default:
                System.out.println(TAG + " 发送V2X消息: " + msg.type);
        }
    }
    
    /**
     * 发送BSM基本安全消息
     */
    private void transmitBSM(VehicleSensorData.V2XMessage msg) {
        // 构建BSM消息
        String bsm = buildBSMMessage(msg);
        
        // 选择传输模式
        switch (mode) {
            case C_V2X:
            case LTE_V:
                transmitViaNetwork(bsm);
                break;
            case PC5:
                transmitViaPC5(bsm);
                break;
        }
        
        System.out.println(TAG + " 广播BSM: " + msg.senderId + 
            " 位置=(" + String.format("%.6f", msg.latitude) + "," + 
            String.format("%.6f", msg.longitude) + ")");
    }
    
    /**
     * 发送SPaT信号灯信息
     */
    private void transmitSPAT(VehicleSensorData.V2XMessage msg) {
        String spat = buildSPATMessage(msg);
        transmitViaNetwork(spat);
        
        System.out.println(TAG + " 发送SPaT到信号灯控制器");
    }
    
    /**
     * 通过网络发送
     */
    private void transmitViaNetwork(String message) {
        // 实际实现：通过C-V2X/LTE-V模块发送
        System.out.println(TAG + " 通过网络发送: " + message.length() + " bytes");
    }
    
    /**
     * 通过PC5直连发送
     */
    private void transmitViaPC5(String message) {
        // 实际实现：通过PC5接口广播
        System.out.println(TAG + " 通过PC5广播: " + message.length() + " bytes");
    }
    
    // ========== 消息接收 ==========
    
    /**
     * 处理接收到的V2X消息
     */
    public void receive(String rawMessage) {
        // 解析消息类型
        if (rawMessage.startsWith("BSM:")) {
            VehicleSensorData.V2XMessage msg = parseBSM(rawMessage);
            notifyListeners(msg);
        } else if (rawMessage.startsWith("SPAT:")) {
            VehicleSensorData.V2XMessage msg = parseSPAT(rawMessage);
            notifyListeners(msg);
        } else if (rawMessage.startsWith("RSI:")) {
            VehicleSensorData.V2XMessage msg = parseRSI(rawMessage);
            notifyListeners(msg);
        }
    }
    
    /**
     * 解析BSM消息
     */
    private VehicleSensorData.V2XMessage parseBSM(String raw) {
        VehicleSensorData.V2XMessage msg = new VehicleSensorData.V2XMessage("BSM");
        
        // 格式: BSM:id,lat,lon,speed,heading
        String[] parts = raw.substring(4).split(",");
        if (parts.length >= 5) {
            msg.senderId = parts[0];
            msg.latitude = Double.parseDouble(parts[1]);
            msg.longitude = Double.parseDouble(parts[2]);
            msg.speed = Double.parseDouble(parts[3]);
            msg.heading = Double.parseDouble(parts[4]);
        }
        
        return msg;
    }
    
    /**
     * 解析SPaT消息
     */
    private VehicleSensorData.V2XMessage parseSPAT(String raw) {
        VehicleSensorData.V2XMessage msg = new VehicleSensorData.V2XMessage("SPaT");
        
        // 格式: SPAT:intersectionId,phase,timeToChange
        String[] parts = raw.substring(5).split(",");
        if (parts.length >= 3) {
            msg.senderId = parts[0];
            msg.extraData.put("phase", Integer.parseInt(parts[1]));
            msg.extraData.put("timeToChange", Integer.parseInt(parts[2]));
        }
        
        return msg;
    }
    
    /**
     * 解析RSI消息
     */
    private VehicleSensorData.V2XMessage parseRSI(String raw) {
        VehicleSensorData.V2XMessage msg = new VehicleSensorData.V2XMessage("RSI");
        
        // 格式: RSI:type,lat,lon,severity,description
        String[] parts = raw.substring(4).split(",");
        if (parts.length >= 5) {
            msg.senderId = parts[0];
            msg.extraData.put("type", parts[1]);
            msg.latitude = Double.parseDouble(parts[2]);
            msg.longitude = Double.parseDouble(parts[3]);
            msg.extraData.put("severity", Double.parseDouble(parts[4]));
            msg.extraData.put("description", parts[5]);
        }
        
        return msg;
    }
    
    // ========== 消息构建 ==========
    
    /**
     * 构建BSM消息
     */
    private String buildBSMMessage(VehicleSensorData.V2XMessage msg) {
        return String.format("BSM:%s,%.6f,%.6f,%.1f,%.1f",
            msg.senderId, msg.latitude, msg.longitude, msg.speed, msg.heading);
    }
    
    /**
     * 构建SPaT消息
     */
    private String buildSPATMessage(VehicleSensorData.V2XMessage msg) {
        return String.format("SPAT:%s,%s,%d",
            msg.senderId,
            msg.extraData.getOrDefault("phase", 0),
            msg.extraData.getOrDefault("timeToChange", 0));
    }
    
    // ========== 监听器管理 ==========
    
    /**
     * 添加消息监听器
     */
    public void addListener(V2XMessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除消息监听器
     */
    public void removeListener(V2XMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知监听器
     */
    private void notifyListeners(VehicleSensorData.V2XMessage msg) {
        for (V2XMessageListener listener : listeners) {
            listener.onV2XMessage(msg);
        }
    }
    
    // ========== 连接管理 ==========
    
    /**
     * 连接V2X网络
     */
    public boolean connect() {
        // 模拟连接
        connected = true;
        System.out.println(TAG + " V2X连接成功 (模式: " + mode + ")");
        return true;
    }
    
    /**
     * 断开V2X网络
     */
    public void disconnect() {
        connected = false;
        System.out.println(TAG + " V2X已断开");
    }
    
    /**
     * 设置V2X通信模式
     */
    public void setMode(V2XMode mode) {
        this.mode = mode;
        System.out.println(TAG + " V2X模式切换为: " + mode);
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected;
    }
    
    // ========== 监听器接口 ==========
    
    /**
     * V2X消息监听器
     */
    public interface V2XMessageListener {
        void onV2XMessage(VehicleSensorData.V2XMessage msg);
    }
}
