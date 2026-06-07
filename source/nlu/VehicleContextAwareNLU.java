package nlu;

import java.util.*;

/**
 * 车辆上下文感知NLU处理器
 * 结合车辆实时状态进行智能NLU处理
 * 
 * 核心功能：
 * 1. 实时车辆状态感知
 * 2. 基于车辆状态的意图调整
 * 3. 约束感知处理
 * 4. 传感器数据融合
 */
public class VehicleContextAwareNLU {
    
    // 车辆状态
    private VehicleStatus vehicleStatus;
    
    // 传感器融合结果
    private SensorFusionResult fusionResult;
    
    // NLU处理器
    private NLUProcessor nluProcessor;
    
    // 地理解析器
    private GeoParser geoParser;
    
    public VehicleContextAwareNLU() {
        this.vehicleStatus = new VehicleStatus();
        this.fusionResult = new SensorFusionResult();
        this.nluProcessor = new NLUProcessor();
        this.geoParser = new GeoParser();
        initializeDefaultStatus();
    }
    
    /**
     * 初始化默认车辆状态
     */
    private void initializeDefaultStatus() {
        vehicleStatus.setBatteryLevel(80.0);
        vehicleStatus.setFuelLevel(0.0);
        vehicleStatus.setSpeed(0.0);
        vehicleStatus.setLatitude(29.5623);
        vehicleStatus.setLongitude(106.5789);
        vehicleStatus.setWeather("晴");
        vehicleStatus.setRoadCondition("良好");
        vehicleStatus.setTrafficCondition("畅通");
        vehicleStatus.setMaxRange(500.0); // 公里
        vehicleStatus.setMaxPayload(500.0); // 公斤
    }
    
    /**
     * 处理用户输入，结合车辆上下文
     */
    public ContextAwareResult processWithContext(String userInput) {
        ContextAwareResult result = new ContextAwareResult();
        
        // 1. 基础NLU处理
        NLUResult nluResult = nluProcessor.processToResult(userInput);
        result.setNluResult(nluResult);
        
        // 2. 结合车辆上下文进行调整
        adjustBasedOnContext(result);
        
        // 3. 检查约束
        List<String> constraintViolations = checkConstraints(result);
        result.setConstraintViolations(constraintViolations);
        
        // 4. 生成建议
        if (!constraintViolations.isEmpty()) {
            for (String suggestion : generateSuggestions(result)) {
                result.addSuggestion(suggestion);
            }
        }
        
        // 5. 评估可行性
        result.setFeasible(constraintViolations.isEmpty());
        
        return result;
    }
    
    /**
     * 根据车辆上下文调整结果
     */
    private void adjustBasedOnContext(ContextAwareResult result) {
        NLUResult nluResult = result.getNluResult();
        
        // 根据电池状态调整充电相关建议
        if (vehicleStatus.getBatteryLevel() < 20) {
            if (nluResult.getDestination() != null) {
                // 检查目的地是否可达
                double distance = estimateDistance(nluResult.getDestination());
                if (distance > vehicleStatus.getBatteryLevel() * 5) { // 粗略估算
                    result.addWarning("当前电量可能不足以到达目的地，建议先充电");
                    result.addSuggestion("建议前往最近的充电站补充电量");
                }
            }
            
            // 如果用户没有提到充电，主动建议
            String text = nluResult.getOriginalText();
            if (text != null && !text.contains("充电") && !text.contains("加油站")) {
                result.addSuggestion("您的电量较低（" + 
                    String.format("%.0f%%", vehicleStatus.getBatteryLevel()) + 
                    "），可能需要考虑充电");
            }
        }
        
        // 根据天气条件调整导航建议
        if ("雨".equals(vehicleStatus.getWeather()) || "雪".equals(vehicleStatus.getWeather())) {
            if (nluResult.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH ||
                nluResult.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
                result.addWarning("当前天气条件较差（" + vehicleStatus.getWeather() + 
                    "），高速模式可能不安全");
                result.addSuggestion("建议使用经济模式或普通路线，谨慎驾驶");
            }
        }
        
        // 根据交通状况调整路线建议
        if ("拥堵".equals(vehicleStatus.getTrafficCondition())) {
            if (nluResult.getRoutePreference() == NLUResult.RoutePreference.TIME_FIRST) {
                result.addWarning("当前路况拥堵（" + vehicleStatus.getTrafficCondition() + 
                    "），即使选择高速也可能无法节省时间");
                result.addSuggestion("建议考虑备选路线或稍后再出发");
            }
        }
        
        // 根据车速调整操作建议
        if (vehicleStatus.getSpeed() > 100) {
            String text = nluResult.getOriginalText();
            if (text != null && (text.contains("加速") || text.contains("快点"))) {
                result.addWarning("当前车速已较快（" + 
                    String.format("%.0f", vehicleStatus.getSpeed()) + "km/h），不建议继续加速");
            }
        }
        
        // 根据负载情况调整任务建议
        if (vehicleStatus.getCurrentPayload() > vehicleStatus.getMaxPayload() * 0.9) {
            if (nluResult.getPrimaryIntent() == Intent.EMERGENCY) {
                result.addWarning("当前载重接近上限，超载可能影响紧急任务的执行");
                result.addSuggestion("建议卸载部分货物后再执行紧急任务");
            }
        }
    }
    
    /**
     * 检查约束违反情况
     */
    private List<String> checkConstraints(ContextAwareResult result) {
        List<String> violations = new ArrayList<>();
        NLUResult nluResult = result.getNluResult();
        
        // 电池/续航约束
        if (nluResult.getDestination() != null) {
            double distance = estimateDistance(nluResult.getDestination());
            double requiredEnergy = distance / 10; // 粗略估算
            
            if (vehicleStatus.getBatteryLevel() < requiredEnergy) {
                violations.add("电量不足：到达目的地需要约" + 
                    String.format("%.0f%%", requiredEnergy) + "电量，当前剩余" +
                    String.format("%.0f%%", vehicleStatus.getBatteryLevel()));
            }
        }
        
        // 续航距离约束
        double maxDistance = vehicleStatus.getBatteryLevel() * 5; // 粗略估算
        if (nluResult.getDestination() != null) {
            double distance = estimateDistance(nluResult.getDestination());
            if (distance > maxDistance) {
                violations.add("续航不足：当前电量最远可行驶约" + 
                    String.format("%.0f", maxDistance) + "公里，目的地距离约" +
                    String.format("%.0f", distance) + "公里");
            }
        }
        
        // 载重约束
        if (vehicleStatus.getCurrentPayload() > vehicleStatus.getMaxPayload()) {
            violations.add("载重超限：当前载重" + 
                String.format("%.0f", vehicleStatus.getCurrentPayload()) + "kg，超过最大载重" +
                String.format("%.0f", vehicleStatus.getMaxPayload()) + "kg");
        }
        
        // 速度约束
        if (vehicleStatus.getSpeed() > 120) {
            violations.add("超速警告：当前车速" + 
                String.format("%.0f", vehicleStatus.getSpeed()) + "km/h，超过限速");
        }
        
        return violations;
    }
    
    /**
     * 生成建议
     */
    private List<String> generateSuggestions(ContextAwareResult result) {
        List<String> suggestions = new ArrayList<>();
        
        for (String violation : result.getConstraintViolations()) {
            if (violation.contains("电量不足") || violation.contains("续航不足")) {
                // 建议最近的充电站
                GeoParser.GeoLocation nearestStation = 
                    geoParser.findNearestChargingStation(
                        vehicleStatus.getLatitude(), 
                        vehicleStatus.getLongitude()
                    );
                if (nearestStation != null) {
                    double distance = geoParser.calculateDistance(
                        vehicleStatus.getLatitude(), 
                        vehicleStatus.getLongitude(),
                        nearestStation.getLatitude(),
                        nearestStation.getLongitude()
                    );
                    suggestions.add("最近的充电站：" + nearestStation.getName() + 
                        "（约" + String.format("%.1f", distance) + "公里）");
                }
            }
            
            if (violation.contains("载重超限")) {
                suggestions.add("建议卸载部分货物后再执行任务");
            }
        }
        
        return suggestions;
    }
    
    /**
     * 估算到目的地的距离
     */
    private double estimateDistance(String destination) {
        GeoParser.GeoLocation location = geoParser.geocode(destination);
        if (location != null) {
            return geoParser.calculateDistance(
                vehicleStatus.getLatitude(),
                vehicleStatus.getLongitude(),
                location.getLatitude(),
                location.getLongitude()
            );
        }
        return 0; // 无法估算
    }
    
    /**
     * 更新车辆状态
     */
    public void updateVehicleStatus(VehicleStatus status) {
        this.vehicleStatus = status;
    }
    
    /**
     * 更新传感器融合结果
     */
    public void updateSensorFusion(SensorFusionResult fusion) {
        this.fusionResult = fusion;
    }
    
    /**
     * 获取当前车辆状态
     */
    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }
    
    /**
     * 车辆状态类
     */
    public static class VehicleStatus {
        private double batteryLevel;      // 电量百分比 0-100
        private double fuelLevel;          // 油量百分比 0-100
        private double speed;              // 当前速度 km/h
        private double latitude;           // 纬度
        private double longitude;         // 经度
        private String weather;            // 天气状况
        private String roadCondition;      // 路面状况
        private String trafficCondition;   // 交通状况
        private double maxRange;           // 最大续航里程 km
        private double maxPayload;          // 最大载重 kg
        private double currentPayload;     // 当前载重 kg
        
        public double getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }
        public double getFuelLevel() { return fuelLevel; }
        public void setFuelLevel(double fuelLevel) { this.fuelLevel = fuelLevel; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getWeather() { return weather; }
        public void setWeather(String weather) { this.weather = weather; }
        public String getRoadCondition() { return roadCondition; }
        public void setRoadCondition(String roadCondition) { this.roadCondition = roadCondition; }
        public String getTrafficCondition() { return trafficCondition; }
        public void setTrafficCondition(String trafficCondition) { 
            this.trafficCondition = trafficCondition; 
        }
        public double getMaxRange() { return maxRange; }
        public void setMaxRange(double maxRange) { this.maxRange = maxRange; }
        public double getMaxPayload() { return maxPayload; }
        public void setMaxPayload(double maxPayload) { this.maxPayload = maxPayload; }
        public double getCurrentPayload() { return currentPayload; }
        public void setCurrentPayload(double currentPayload) { 
            this.currentPayload = currentPayload; 
        }
    }
    
    /**
     * 传感器融合结果类
     */
    public static class SensorFusionResult {
        private double fusedSpeed;
        private double fusedHeading;
        private double confidence;
        private List<String> activeSensors;
        
        public SensorFusionResult() {
            this.activeSensors = new ArrayList<>();
        }
        
        public double getFusedSpeed() { return fusedSpeed; }
        public void setFusedSpeed(double fusedSpeed) { this.fusedSpeed = fusedSpeed; }
        public double getFusedHeading() { return fusedHeading; }
        public void setFusedHeading(double fusedHeading) { this.fusedHeading = fusedHeading; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public List<String> getActiveSensors() { return activeSensors; }
    }
    
    /**
     * 上下文感知结果类
     */
    public static class ContextAwareResult {
        private NLUResult nluResult;
        private boolean feasible;
        private List<String> constraintViolations;
        private List<String> warnings;
        private List<String> suggestions;
        private double adjustedConfidence;
        
        public ContextAwareResult() {
            this.constraintViolations = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.suggestions = new ArrayList<>();
            this.feasible = true;
        }
        
        public NLUResult getNluResult() { return nluResult; }
        public void setNluResult(NLUResult nluResult) { this.nluResult = nluResult; }
        public boolean isFeasible() { return feasible; }
        public void setFeasible(boolean feasible) { this.feasible = feasible; }
        public List<String> getConstraintViolations() { return constraintViolations; }
        public void setConstraintViolations(List<String> constraintViolations) { 
            this.constraintViolations = constraintViolations; 
        }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public List<String> getSuggestions() { return suggestions; }
        public void addSuggestion(String suggestion) { this.suggestions.add(suggestion); }
        public double getAdjustedConfidence() { return adjustedConfidence; }
        public void setAdjustedConfidence(double adjustedConfidence) { 
            this.adjustedConfidence = adjustedConfidence; 
        }
    }
}
