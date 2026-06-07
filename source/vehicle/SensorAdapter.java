package vehicle;

import utils.SynonymLibrary;
import utils.UnitConverter;

import java.util.*;

/**
 * 传感器接口层 - 将各类传感器原始数据转换为 DSL 领域模型
 * 
 * 无人汽车传感器类型：
 * - 摄像头 (Camera) → 计算机视觉处理 → DSL目标检测
 * - 毫米波雷达 (Radar) → 目标跟踪 → DSL距离/速度
 * - 激光雷达 (LiDAR) → 点云处理 → DSL障碍物地图
 * - GPS/IMU → 定位融合 → DSL位置/姿态
 * - CAN总线 → 车辆状态 → DSL车速/电量
 * 
 * @paper_ref 无人汽车传感器接口层设计
 */
public class SensorAdapter {
    
    private static final String TAG = "[SensorAdapter]";
    
    // 传感器健康阈值
    private static final double HEALTH_THRESHOLD = 0.7;
    
    // ========== 摄像头适配器 ==========
    
    /**
     * 摄像头适配器 - 将视觉检测结果转换为DSL障碍物列表
     */
    public static class CameraAdapter {
        
        // YOLO类别映射到DSL标准化类别
        private static final Map<String, String> CLASS_MAPPING = new HashMap<>();
        
        static {
            // 行人
            CLASS_MAPPING.put("person", "pedestrian");
            CLASS_MAPPING.put("pedestrian", "pedestrian");
            CLASS_MAPPING.put("cyclist", "cyclist");
            CLASS_MAPPING.put("bicycle", "cyclist");
            // 车辆
            CLASS_MAPPING.put("car", "vehicle");
            CLASS_MAPPING.put("truck", "vehicle");
            CLASS_MAPPING.put("bus", "vehicle");
            CLASS_MAPPING.put("motorcycle", "vehicle");
            // 障碍物
            CLASS_MAPPING.put("cone", "obstacle");
            CLASS_MAPPING.put("barrier", "obstacle");
            CLASS_MAPPING.put("traffic_sign", "traffic_sign");
            CLASS_MAPPING.put("stop_sign", "traffic_sign");
        }
        
        /**
         * 将YOLO检测结果转换为DSL障碍物列表
         * 
         * @param detections YOLO检测结果列表
         * @return 障碍物列表
         */
        public static VehicleSensorData.Obstacle[] parseYOLOResult(
                List<Map<String, Object>> detections) {
            
            List<VehicleSensorData.Obstacle> obstacles = new ArrayList<>();
            
            for (Map<String, Object> det : detections) {
                String objClass = (String) det.get("class");
                Double confidence = (Double) det.getOrDefault("confidence", 0.0);
                Double x = (Double) det.getOrDefault("x", 0.0);
                Double y = (Double) det.getOrDefault("y", 0.0);
                Double width = (Double) det.getOrDefault("width", 0.0);
                Double height = (Double) det.getOrDefault("height", 0.0);
                
                // 语义对齐：统一不同检测模型的类别名称
                String normalizedClass = normalizeClass(objClass);
                
                // 计算距离
                double distance = estimateDistance(width, height, normalizedClass);
                
                // 计算角度（基于图像x坐标）
                double angle = calculateAngle(x, 640.0);  // 假设图像宽度640
                
                VehicleSensorData.Obstacle obstacle = 
                    new VehicleSensorData.Obstacle(normalizedClass, distance, angle, "camera");
                obstacle.confidence = confidence;
                
                obstacles.add(obstacle);
            }
            
            System.out.println(TAG + " Camera: detected " + obstacles.size() + " objects");
            return obstacles.toArray(new VehicleSensorData.Obstacle[0]);
        }
        
        /**
         * 统一类别名称
         */
        private static String normalizeClass(String objClass) {
            if (objClass == null) return "unknown";
            String normalized = CLASS_MAPPING.get(objClass.toLowerCase());
            return normalized != null ? normalized : objClass;
        }
        
        /**
         * 基于边界框估算距离
         */
        private static double estimateDistance(double width, double height, String objClass) {
            // 预设的真实尺寸（米）
            double realHeight;
            switch (objClass) {
                case "pedestrian": realHeight = 1.7; break;
                case "cyclist": realHeight = 1.8; break;
                case "vehicle": realHeight = 1.5; break;
                default: realHeight = 1.0;
            }
            
            // 简单估算（实际需要相机标定参数）
            double focalLength = 1000;  // 假设焦距
            return (realHeight * focalLength) / (height + 0.001);
        }
        
        /**
         * 计算目标角度
         */
        private static double calculateAngle(double x, double imageWidth) {
            double fov = 60.0;  // 水平视场角
            return (x / imageWidth - 0.5) * fov;
        }
    }
    
    // ========== 雷达适配器 ==========
    
    /**
     * 毫米波雷达适配器 - 解析雷达目标列表
     */
    public static class RadarAdapter {
        
        // CAN ID 定义
        public static final int RADAR_TARGET_CAN_ID = 0x300;
        public static final int RADAR_CONFIG_CAN_ID = 0x301;
        
        /**
         * 解析毫米波雷达CAN消息
         * 
         * CAN消息格式: [target_id(1), range(2), speed(2), angle(2), rcs(1)]
         * 
         * @param canData CAN数据
         * @return 雷达目标列表
         */
        public static VehicleSensorData.RadarTarget[] parseCANMessage(byte[] canData) {
            List<VehicleSensorData.RadarTarget> targets = new ArrayList<>();
            
            if (canData == null || canData.length < 8) {
                return targets.toArray(new VehicleSensorData.RadarTarget[0]);
            }
            
            // 解析雷达目标（最多支持8个目标）
            int maxTargets = Math.min(8, canData.length / 8);
            int offset = 0;
            
            for (int i = 0; i < maxTargets; i++) {
                try {
                    int targetId = canData[offset] & 0xFF;
                    if (targetId == 0) break;  // 0表示无效目标
                    
                    // 距离: 0.1m分辨率, 最大255m
                    int rangeRaw = (canData[offset + 1] & 0xFF) | ((canData[offset + 2] & 0xFF) << 8);
                    double rangeMeters = rangeRaw * 0.1;
                    
                    // 速度: 0.05m/s分辨率, 有符号
                    int speedRaw = (canData[offset + 3] & 0xFF) | ((canData[offset + 4] & 0xFF) << 8);
                    if (speedRaw > 32767) speedRaw -= 65536;
                    double speedMs = speedRaw * 0.05;
                    
                    // 角度: 0.1°分辨率, 范围 -90° ~ +90°
                    int angleRaw = (canData[offset + 5] & 0xFF) | ((canData[offset + 6] & 0xFF) << 8);
                    double angleDeg = (angleRaw - 900) * 0.1;
                    
                    // RCS: 雷达截面积 (dBsm)
                    double rcs = canData[offset + 7] & 0xFF;
                    
                    if (rangeMeters > 0 && Math.abs(angleDeg) <= 90) {
                        targets.add(new VehicleSensorData.RadarTarget(
                            targetId, rangeMeters, speedMs, angleDeg));
                        targets.get(targets.size() - 1).rcs = rcs;
                    }
                    
                    offset += 8;
                } catch (Exception e) {
                    System.err.println(TAG + " Radar parse error: " + e.getMessage());
                    break;
                }
            }
            
            System.out.println(TAG + " Radar: parsed " + targets.size() + " targets");
            return targets.toArray(new VehicleSensorData.RadarTarget[0]);
        }
        
        /**
         * 将雷达目标转换为障碍物
         */
        public static VehicleSensorData.Obstacle[] toObstacles(
                VehicleSensorData.RadarTarget[] targets) {
            
            List<VehicleSensorData.Obstacle> obstacles = new ArrayList<>();
            
            for (VehicleSensorData.RadarTarget target : targets) {
                // 雷达只能检测到反射物体，无法分类
                String type = target.rcs > 10 ? "large_metal" : "unknown";
                
                VehicleSensorData.Obstacle obstacle = 
                    new VehicleSensorData.Obstacle(type, target.range, target.angle, "radar");
                obstacle.relativeSpeed = target.speed;
                obstacle.confidence = 0.9;
                
                obstacles.add(obstacle);
            }
            
            return obstacles.toArray(new VehicleSensorData.Obstacle[0]);
        }
    }
    
    // ========== GNSS 适配器 ==========
    
    /**
     * GNSS适配器 - 解析GPS定位数据
     */
    public static class GNSSAdapter {
        
        /**
         * 解析NMEA RMC语句
         * 
         * 格式: $GPRMC,hhmmss.ss,A,ddmm.mmm,N,dddmm.mmm,E,spd,hdg,ddmmyy,md,m*cs
         * 
         * @param nmeaSentence NMEA语句
         * @return GNSS位置
         */
        public static VehicleSensorData.GNSSPosition parseNMEA(String nmeaSentence) {
            if (nmeaSentence == null || !nmeaSentence.startsWith("$GPRMC")) {
                return null;
            }
            
            try {
                String[] fields = nmeaSentence.split(",");
                
                // 解析时间
                // String time = fields[1];  // hhmmss.ss
                
                // 解析状态
                String status = fields[2];  // A=有效, V=无效
                if (!"A".equals(status)) {
                    System.err.println(TAG + " GNSS: Invalid position data");
                    return null;
                }
                
                // 解析纬度
                double lat = parseNMEACoordinate(fields[3], fields[4]);
                
                // 解析经度
                double lon = parseNMEACoordinate(fields[5], fields[6]);
                
                // 解析速度(节)
                double speedKnots = parseDouble(fields[7]);
                
                // 解析航向(度)
                double heading = parseDouble(fields[8]);
                
                // 解析日期
                // String date = fields[9];
                
                // 转换为标准单位
                VehicleSensorData.GNSSPosition pos = new VehicleSensorData.GNSSPosition(lat, lon, 0);
                pos.speed = speedKnots * 0.514444;  // 节转m/s
                pos.heading = heading;
                pos.accuracy = 2.0;  // 假设精度2米
                
                System.out.println(TAG + " GNSS: " + pos);
                return pos;
                
            } catch (Exception e) {
                System.err.println(TAG + " GNSS parse error: " + e.getMessage());
                return null;
            }
        }
        
        /**
         * 解析NMEA坐标格式
         * 格式: ddmm.mmm,N 或 dddmm.mmm,E
         */
        private static double parseNMEACoordinate(String coord, String direction) {
            if (coord == null || coord.isEmpty()) return 0;
            
            double value = parseDouble(coord);
            double degrees = Math.floor(value / 100);
            double minutes = value - degrees * 100;
            double decimal = degrees + minutes / 60;
            
            // 南半球或西经为负
            if ("S".equals(direction) || "W".equals(direction)) {
                decimal = -decimal;
            }
            
            return decimal;
        }
        
        private static double parseDouble(String s) {
            try {
                return s.isEmpty() ? 0 : Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
    
    // ========== IMU 适配器 ==========
    
    /**
     * IMU适配器 - 解析惯性测量单元数据
     */
    public static class IMUAdapter {
        
        // CAN ID 定义
        public static final int IMU_CAN_ID = 0x400;
        
        /**
         * 解析IMU CAN消息
         * 
         * CAN消息格式: [accel_x(2), accel_y(2), accel_z(2), gyro_x(2), gyro_y(2), gyro_z(2)]
         * 分辨率: 加速度 0.01m/s², 角速度 0.01°/s
         */
        public static VehicleSensorData.IMUData parseCANMessage(byte[] canData) {
            VehicleSensorData.IMUData imu = new VehicleSensorData.IMUData();
            
            if (canData == null || canData.length < 12) {
                return imu;
            }
            
            try {
                // 解析加速度
                imu.accelX = parseInt16(canData, 0) * 0.01;
                imu.accelY = parseInt16(canData, 2) * 0.01;
                imu.accelZ = parseInt16(canData, 4) * 0.01;
                
                // 解析角速度 (需要额外的CAN帧)
                if (canData.length >= 18) {
                    imu.gyroX = parseInt16(canData, 6) * 0.01;
                    imu.gyroY = parseInt16(canData, 8) * 0.01;
                    imu.gyroZ = parseInt16(canData, 10) * 0.01;
                }
                
                // 计算姿态角（简化处理）
                imu.pitch = Math.toDegrees(Math.atan2(imu.accelY, imu.accelZ));
                imu.roll = Math.toDegrees(Math.atan2(-imu.accelX, 
                    Math.sqrt(imu.accelY * imu.accelY + imu.accelZ * imu.accelZ)));
                
                System.out.println(TAG + " IMU: " + imu);
                
            } catch (Exception e) {
                System.err.println(TAG + " IMU parse error: " + e.getMessage());
            }
            
            return imu;
        }
        
        private static short parseInt16(byte[] data, int offset) {
            int value = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            return (short) value;
        }
    }
    
    // ========== 车辆状态适配器 (CAN总线) ==========
    
    /**
     * 车辆状态适配器 - 从CAN总线解析车辆状态
     */
    public static class VehicleStatusAdapter {
        
        // CAN ID 定义
        public static final int POWERTRAIN_CAN_ID = 0x100;   // 动力系统
        public static final int BATTERY_CAN_ID = 0x200;       // 电池系统
        public static final int CHASSIS_CAN_ID = 0x110;       // 底盘系统
        
        /**
         * 从CAN总线解析车辆状态
         */
        public static VehicleSensorData.VehicleStatus parseCANFrame(
                int canId, byte[] data) {
            
            VehicleSensorData.VehicleStatus status = 
                new VehicleSensorData.VehicleStatus();
            
            switch (canId) {
                case POWERTRAIN_CAN_ID:  // 0x100: 车速、转速
                    parsePowertrain(status, data);
                    break;
                    
                case BATTERY_CAN_ID:  // 0x200: 电池SOC、温度
                    parseBattery(status, data);
                    break;
                    
                case CHASSIS_CAN_ID:  // 0x110: 方向盘角度、档位
                    parseChassis(status, data);
                    break;
                    
                default:
                    System.out.println(TAG + " Unknown CAN ID: 0x" + 
                        Integer.toHexString(canId).toUpperCase());
            }
            
            return status;
        }
        
        private static void parsePowertrain(
                VehicleSensorData.VehicleStatus status, byte[] data) {
            if (data.length < 4) return;
            
            // 车速: 0.01km/h分辨率
            int speedRaw = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
            status.speed = speedRaw * 0.01;
            
            // 电机转速: 1rpm分辨率
            int rpmRaw = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
            status.rpm = rpmRaw;
        }
        
        private static void parseBattery(
                VehicleSensorData.VehicleStatus status, byte[] data) {
            if (data.length < 4) return;
            
            // 电池SOC: 0.5%分辨率
            status.batterySOC = (data[0] & 0xFF) * 0.5;
            
            // 电池温度: -40°C偏移, 1°C分辨率
            status.batteryTemp = (data[1] & 0xFF) - 40;
        }
        
        private static void parseChassis(
                VehicleSensorData.VehicleStatus status, byte[] data) {
            if (data.length < 3) return;
            
            // 方向盘角度: -327.68° ~ +327.68°
            int steeringRaw = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
            status.steeringAngle = (steeringRaw - 32768) * 0.01;
            
            // 油门开度: 0.4%分辨率
            status.throttle = (data[2] & 0xFF) * 0.4;
            
            // 档位
            status.gear = data[3] & 0xFF;
        }
    }
    
    // ========== V2X 适配器 ==========
    
    /**
     * V2X适配器 - 解析车联网消息
     */
    public static class V2XAdapter {
        
        /**
         * 解析BSM (Basic Safety Message)
         * 
         * BSM消息通常通过UDP/TCP传输，这里简化处理
         */
        public static VehicleSensorData.V2XMessage parseBSM(Map<String, Object> bsmData) {
            VehicleSensorData.V2XMessage msg = new VehicleSensorData.V2XMessage("BSM");
            
            msg.senderId = (String) bsmData.getOrDefault("id", "unknown");
            msg.latitude = (Double) bsmData.getOrDefault("lat", 0.0);
            msg.longitude = (Double) bsmData.getOrDefault("lon", 0.0);
            msg.speed = (Double) bsmData.getOrDefault("speed", 0.0);
            msg.heading = (Double) bsmData.getOrDefault("heading", 0.0);
            
            return msg;
        }
        
        /**
         * 解析SPaT (Signal Phase and Timing)
         */
        public static Map<String, Object> parseSPaT(Map<String, Object> spatData) {
            Map<String, Object> result = new HashMap<>();
            
            result.put("intersection_id", spatData.getOrDefault("intersectionId", 0));
            result.put("phase", spatData.getOrDefault("phase", 0));  // 0-8
            result.put("time_to_change", spatData.getOrDefault("timeToChange", 0));  // 秒
            result.put("min_time", spatData.getOrDefault("minTime", 0));
            result.put("max_time", spatData.getOrDefault("maxTime", 0));
            
            return result;
        }
    }
    
    // ========== 数据融合 ==========
    
    /**
     * 多传感器数据融合
     * 
     * 将摄像头、雷达、GNSS等多源数据融合为统一的感知结果
     */
    public static class DataFusion {
        
        /**
         * 融合摄像头和雷达的障碍物检测
         */
        public static VehicleSensorData.Obstacle[] fuseObstacles(
                VehicleSensorData.Obstacle[] cameraObstacles,
                VehicleSensorData.RadarTarget[] radarTargets) {
            
            List<VehicleSensorData.Obstacle> fused = new ArrayList<>();
            Set<Integer> matched = new HashSet<>();
            
            // 优先使用摄像头分类，雷达提供距离补充
            for (int i = 0; i < cameraObstacles.length; i++) {
                VehicleSensorData.Obstacle camObs = cameraObstacles[i];
                
                // 在雷达目标中寻找匹配
                for (int j = 0; j < radarTargets.length; j++) {
                    if (matched.contains(j)) continue;
                    
                    VehicleSensorData.RadarTarget radar = radarTargets[j];
                    
                    // 角度匹配
                    if (Math.abs(camObs.angle - radar.angle) < 10) {
                        // 更新距离和速度
                        camObs.distance = radar.range;
                        camObs.relativeSpeed = radar.speed;
                        camObs.confidence = Math.min(0.95, camObs.confidence + 0.1);
                        matched.add(j);
                        break;
                    }
                }
                
                fused.add(camObs);
            }
            
            // 添加未被匹配的雷达目标
            for (int j = 0; j < radarTargets.length; j++) {
                if (!matched.contains(j)) {
                    VehicleSensorData.RadarTarget radar = radarTargets[j];
                    VehicleSensorData.Obstacle obs = new VehicleSensorData.Obstacle(
                        "unknown", radar.range, radar.angle, "radar"
                    );
                    obs.relativeSpeed = radar.speed;
                    fused.add(obs);
                }
            }
            
            return fused.toArray(new VehicleSensorData.Obstacle[0]);
        }
        
        /**
         * 融合位置信息
         */
        public static VehicleSensorData.GNSSPosition fusePosition(
                VehicleSensorData.GNSSPosition gnss,
                VehicleSensorData.IMUData imu,
                VehicleSensorData.VehicleStatus vehicleStatus) {
            
            // 简化处理：主要使用GNSS，IMU用于平滑
            VehicleSensorData.GNSSPosition fused = new VehicleSensorData.GNSSPosition(
                gnss.latitude, gnss.longitude, gnss.altitude);
            
            fused.heading = vehicleStatus.steeringAngle != 0 
                ? vehicleStatus.steeringAngle 
                : gnss.heading;
            fused.speed = vehicleStatus.speed / 3.6;  // km/h -> m/s
            
            return fused;
        }
        
        /**
         * 评估传感器健康状态
         */
        public static Map<VehicleSensorData.SensorType, Double> evaluateSensorHealth(
                boolean cameraActive,
                boolean radarActive,
                boolean gnssActive,
                boolean imuActive) {
            
            Map<VehicleSensorData.SensorType, Double> health = new HashMap<>();
            
            health.put(VehicleSensorData.SensorType.CAMERA, cameraActive ? 0.95 : 0.0);
            health.put(VehicleSensorData.SensorType.RADAR, radarActive ? 0.90 : 0.0);
            health.put(VehicleSensorData.SensorType.GNSS, gnssActive ? 0.98 : 0.0);
            health.put(VehicleSensorData.SensorType.IMU, imuActive ? 0.85 : 0.0);
            
            return health;
        }
    }
    
    // ========== DSL 转换 ==========
    
    /**
     * 将融合后的传感器数据转换为 DSL State
     * 
     * 这是传感器接口层的最终输出
     */
    public static model.State toDSLState(VehicleSensorData.FusionResult fusion, 
                                         String vehicleId) {
        
        model.State state = new model.State("vehicle_perception_" + vehicleId);
        
        // 位置信息
        state.addField("vehicle_id", vehicleId);
        state.addField("latitude", String.valueOf(fusion.latitude));
        state.addField("longitude", String.valueOf(fusion.longitude));
        state.addField("altitude", String.valueOf(fusion.altitude));
        state.addField("heading", String.valueOf(fusion.heading) + "°");
        state.addField("speed", String.valueOf(fusion.speed) + "m/s");
        
        // 障碍物信息
        if (!fusion.obstacles.isEmpty()) {
            StringBuilder obstacleStr = new StringBuilder();
            for (VehicleSensorData.Obstacle obs : fusion.obstacles.values()) {
                obstacleStr.append(obs.type)
                    .append(":").append(String.format("%.1f", obs.distance)).append("m,");
            }
            state.addField("obstacles", obstacleStr.toString());
            
            // 最近障碍物距离
            double minDistance = fusion.obstacles.values().stream()
                .mapToDouble(o -> o.distance)
                .min().orElse(999);
            state.addField("min_obstacle_distance", String.valueOf(minDistance) + "m");
        }
        
        // 传感器健康状态
        double health = fusion.getSensorHealthStatus();
        state.addField("sensor_health", String.valueOf((int)(health * 100)) + "%");
        
        // 时间戳
        state.addField("timestamp", String.valueOf(fusion.timestamp));
        
        return state;
    }
}
