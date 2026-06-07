package vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * 车辆传感器数据模型
 * 
 * 用于统一表示各类传感器的原始数据
 * 将在传感器接口层转换为 DSL 领域模型
 * 
 * @paper_ref 无人汽车传感器接口层
 */
public class VehicleSensorData {
    
    // ========== 传感器类型枚举 ==========
    public enum SensorType {
        CAMERA,
        RADAR,
        LIDAR,
        GNSS,
        IMU,
        CAN_BUS,
        V2X,
        ULTRASONIC
    }
    
    // ========== 摄像头检测目标 ==========
    public static class DetectedObject {
        public String className;      // 类别名称
        public double confidence;    // 置信度
        public double x, y;          // 图像坐标
        public double width, height;  // 边界框尺寸
        public double distance;       // 估算距离(m)
        
        public DetectedObject(String className, double confidence, 
                             double x, double y, double w, double h) {
            this.className = className;
            this.confidence = confidence;
            this.x = x; this.y = y;
            this.width = w; this.height = h;
            this.distance = estimateDistance();
        }
        
        private double estimateDistance() {
            // 基于边界框高度估算距离（需要标定参数）
            double focalLength = 1000;  // 假设焦距
            double realHeight = 1.7;    // 假设行人的实际高度
            return (realHeight * focalLength) / (height + 0.001);
        }
        
        @Override
        public String toString() {
            return String.format("DetectedObject[class=%s, conf=%.2f, dist=%.1fm]",
                className, confidence, distance);
        }
    }
    
    // ========== 雷达目标 ==========
    public static class RadarTarget {
        public int targetId;
        public double range;      // 距离(m)
        public double speed;      // 相对速度(m/s)
        public double angle;      // 角度(度)
        public double rcs;        // 雷达截面积
        public long timestamp;
        
        public RadarTarget(int id, double range, double speed, double angle) {
            this.targetId = id;
            this.range = range;
            this.speed = speed;
            this.angle = angle;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("RadarTarget[id=%d, range=%.1fm, speed=%.1fm/s, angle=%.1f°]",
                targetId, range, speed, angle);
        }
    }
    
    // ========== GNSS 位置 ==========
    public static class GNSSPosition {
        public double latitude;      // 纬度
        public double longitude;     // 经度
        public double altitude;      // 高度(m)
        public double heading;       // 航向(度)
        public double speed;         // 速度(m/s)
        public double accuracy;      // 精度(m)
        public long timestamp;
        
        public GNSSPosition(double lat, double lon, double alt) {
            this.latitude = lat;
            this.longitude = lon;
            this.altitude = alt;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("GNSS[%.6f, %.6f, %.1fm]", latitude, longitude, altitude);
        }
    }
    
    // ========== IMU 数据 ==========
    public static class IMUData {
        public double accelX, accelY, accelZ;    // 加速度(m/s²)
        public double gyroX, gyroY, gyroZ;        // 角速度(rad/s)
        public double pitch, roll, yaw;            // 姿态角(度)
        public long timestamp;
        
        @Override
        public String toString() {
            return String.format("IMU[pitch=%.1f°, roll=%.1f°, yaw=%.1f°]",
                pitch, roll, yaw);
        }
    }
    
    // ========== CAN 总线数据 ==========
    public static class CANData {
        public int canId;
        public byte[] data;
        public long timestamp;
        
        public CANData(int canId, byte[] data) {
            this.canId = canId;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("CAN[ID=0x%03X, data=", canId));
            for (byte b : data) {
                sb.append(String.format("%02X ", b & 0xFF));
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    // ========== V2X 消息 ==========
    public static class V2XMessage {
        public String type;      // BSM, SPaT, MAP, RSI, RSM
        public String senderId;
        public double latitude;
        public double longitude;
        public double speed;
        public double heading;
        public long timestamp;
        public Map<String, Object> extraData;
        
        public V2XMessage(String type) {
            this.type = type;
            this.extraData = new HashMap<>();
        }
        
        @Override
        public String toString() {
            return String.format("V2X[%s from %s: %.6f, %.6f]",
                type, senderId, latitude, longitude);
        }
    }
    
    // ========== 车辆状态 ==========
    public static class VehicleStatus {
        public double speed;         // 车速(km/h)
        public double rpm;           // 电机转速
        public double batterySOC;    // 电池SOC(%)
        public double batteryTemp;   // 电池温度(°C)
        public double steeringAngle; // 方向盘角度(度)
        public double throttle;      // 油门开度(%)
        public int gear;             // 档位
        public boolean doorLocked;   // 车门锁止状态
        public long timestamp;
        
        public VehicleStatus() {
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("VehicleStatus[speed=%.1fkm/h, SOC=%.1f%%, temp=%.1f°C]",
                speed, batterySOC, batteryTemp);
        }
    }
    
    // ========== 融合后的感知结果 ==========
    public static class FusionResult {
        // 融合位置
        public double latitude;
        public double longitude;
        public double altitude;
        public double heading;
        public double speed;
        
        // 障碍物列表（融合多传感器）
        public Map<String, Obstacle> obstacles;
        
        // 交通标志
        public String trafficSign;
        
        // 车道线
        public LaneInfo laneInfo;
        
        // 传感器健康状态
        public Map<SensorType, Double> sensorHealth;
        
        // 时间戳
        public long timestamp;
        
        public FusionResult() {
            this.obstacles = new HashMap<>();
            this.sensorHealth = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public double getSensorHealthStatus() {
            if (sensorHealth.isEmpty()) return 1.0;
            return sensorHealth.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
        }
        
        @Override
        public String toString() {
            return String.format("FusionResult[pos=(%.6f, %.6f), obstacles=%d, health=%.1f%%",
                latitude, longitude, obstacles.size(), getSensorHealthStatus() * 100);
        }
    }
    
    // ========== 障碍物 ==========
    public static class Obstacle {
        public String type;       // pedestrian, vehicle, cyclist, unknown
        public double distance;   // 距离(m)
        public double relativeSpeed; // 相对速度(m/s)
        public double angle;      // 角度(度)
        public String source;      // 检测来源
        public double confidence; // 置信度
        
        public Obstacle(String type, double distance, double angle, String source) {
            this.type = type;
            this.distance = distance;
            this.angle = angle;
            this.source = source;
        }
        
        @Override
        public String toString() {
            return String.format("Obstacle[%s: %.1fm @ %.1f° (conf=%.2f)]",
                type, distance, angle, confidence);
        }
    }
    
    // ========== 车道信息 ==========
    public static class LaneInfo {
        public double leftLaneOffset;   // 左车道线偏移
        public double rightLaneOffset;  // 右车道线偏移
        public double curvature;        // 曲率
        public String laneType;          // 车道类型
        
        @Override
        public String toString() {
            return String.format("Lane[offset=(%.2f, %.2f), curvature=%.4f, type=%s]",
                leftLaneOffset, rightLaneOffset, curvature, laneType);
        }
    }
}
