package agent;

import utils.AmapRealAPI;

import java.util.*;

/**
 * 路线优化器
 * 
 * 基于订阅到的数据（路况、交通流、天气等）进行路线优化：
 * 1. 分析拥堵情况
 * 2. 考虑天气因素
 * 3. 评估周边无人机辅助可能性
 * 4. 生成优化后的路线
 * 
 * @paper_ref 第3章 3.3 路径规划 -> 动态优化
 */
public class RouteOptimizer {
    
    private final AmapRealAPI amap;
    
    // 优化权重
    private static final double CONGESTION_PENALTY = 1.5;   // 拥堵惩罚系数
    private static final double WEATHER_PENALTY = 1.3;      // 天气惩罚系数
    private static final double DRONE_ASSIST_BONUS = 0.8;   // 无人机辅助加成
    
    public RouteOptimizer() {
        this.amap = new AmapRealAPI();
    }
    
    /**
     * 优化路线
     * 
     * @param originalRoute 原始路线描述
     * @param baseTime 基础时间（分钟）
     * @param congestionLevel 拥堵程度 (0.0-1.0)
     * @param hasClearPath 是否有畅通路线
     * @return 优化后的路线
     */
    public CPSAgent.OptimizedRoute optimize(String originalRoute, int baseTime, 
            double congestionLevel, boolean hasClearPath) {
        return optimize(originalRoute, baseTime, congestionLevel, hasClearPath, 
            null, null, null);
    }
    
    /**
     * 完整参数优化
     */
    public CPSAgent.OptimizedRoute optimize(String originalRoute, int baseTime,
            double congestionLevel, boolean hasClearPath,
            Map<String, Object> weatherData,
            Map<String, Object> trafficData,
            List<Map<String, Object>> availableDrones) {
        
        double multiplier = 1.0;
        String optimizationReason = "";
        boolean rerouted = false;
        
        // 1. 分析拥堵
        if (congestionLevel > 0.7) {
            multiplier *= CONGESTION_PENALTY;
            optimizationReason += "严重拥堵(+50%), ";
            rerouted = true;
        } else if (congestionLevel > 0.4) {
            multiplier *= 1.2;
            optimizationReason += "中度拥堵(+20%), ";
        }
        
        // 2. 分析天气
        if (weatherData != null) {
            String weather = (String) weatherData.getOrDefault("weather", "CLEAR");
            double visibility = ((Number) weatherData.getOrDefault("visibility", 5000)).doubleValue();
            
            if (visibility < 1000) {
                multiplier *= WEATHER_PENALTY;
                optimizationReason += "低能见度(+30%), ";
                rerouted = true;
            }
            
            if (weather.contains("RAIN") || weather.contains("STORM")) {
                multiplier *= 1.2;
                optimizationReason += "雨天/暴风雨, ";
            }
        }
        
        // 3. 分析无人机辅助
        boolean droneAssisted = false;
        if (availableDrones != null && !availableDrones.isEmpty()) {
            for (Map<String, Object> drone : availableDrones) {
                Boolean available = (Boolean) drone.getOrDefault("available", false);
                Double battery = ((Number) drone.getOrDefault("batteryLevel", 0)).doubleValue();
                
                if (available && battery > 50) {
                    droneAssisted = true;
                    multiplier *= DRONE_ASSIST_BONUS;
                    optimizationReason += "无人机辅助(-20%), ";
                    break;
                }
            }
        }
        
        // 4. 是否有畅通路线可绕行
        if (hasClearPath && rerouted) {
            optimizationReason += "已切换畅通路线, ";
        }
        
        // 计算优化后的时间和距离
        int optimizedTime = (int) Math.round(baseTime * multiplier);
        double optimizedDistance = baseTime * 0.5 * multiplier; // 简化估算
        
        // 生成描述
        String description;
        if (rerouted) {
            description = String.format("优化路线 [%s] 拥堵%.0f%%, 预计%d分钟", 
                originalRoute, congestionLevel * 100, optimizedTime);
        } else {
            description = String.format("当前路线 [%s] 正常, 预计%d分钟", 
                originalRoute, optimizedTime);
        }
        
        if (!optimizationReason.isEmpty()) {
            description += " (" + optimizationReason.substring(0, optimizationReason.length() - 2) + ")";
        }
        
        return new CPSAgent.OptimizedRoute(description, optimizedTime, optimizedDistance, rerouted);
    }
    
    /**
     * 基于高德API进行真实路线规划
     * 
     * @param origin 起点
     * @param destination 终点
     * @param subscribedData 订阅到的外部数据
     * @return 优化后的路线
     */
    public CPSAgent.OptimizedRoute optimizeWithAPI(String origin, String destination,
            Map<String, List<Object>> subscribedData) {
        
        // 调用高德API获取基础路线
        AmapRealAPI.AmapGeoResult originGeo = amap.geocode(origin);
        AmapRealAPI.AmapGeoResult destGeo = amap.geocode(destination);
        
        if (!originGeo.isSuccess() || !destGeo.isSuccess()) {
            return new CPSAgent.OptimizedRoute(
                "无法规划路线-地址解析失败", 0, 0, false);
        }
        
        AmapRealAPI.AmapRouteResult baseRoute = amap.driving(
            originGeo.getLocationString(), destGeo.getLocationString());
        
        if (baseRoute == null || !baseRoute.isSuccess()) {
            return new CPSAgent.OptimizedRoute(
                "无法规划路线-路径计算失败", 0, 0, false);
        }
        
        // 分析订阅数据
        double congestionLevel = analyzeTrafficData(subscribedData.get("TrafficFlow"));
        boolean hasClearPath = analyzePerceptionData(subscribedData.get("IntersectionPerception"));
        Map<String, Object> weatherData = extractWeatherData(subscribedData.get("WeatherData"));
        List<Map<String, Object>> drones = extractDroneData(subscribedData.get("DroneStatus"));
        
        // 优化
        return optimize(
            String.format("%s -> %s", origin, destination),
            (int) baseRoute.getDurationMinutes(),
            congestionLevel,
            hasClearPath,
            weatherData,
            null,
            drones
        );
    }
    
    /**
     * 分析交通流数据
     */
    private double analyzeTrafficData(List<Object> trafficData) {
        if (trafficData == null || trafficData.isEmpty()) {
            return 0.3; // 默认轻度拥堵
        }
        
        double totalCongestion = 0;
        int count = 0;
        
        for (Object data : trafficData) {
            if (data instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) data;
                if (map.containsKey("congestionLevel")) {
                    totalCongestion += ((Number) map.get("congestionLevel")).doubleValue();
                    count++;
                } else if (map.containsKey("congestion")) {
                    String congestion = (String) map.get("congestion");
                    totalCongestion += parseCongestionLevel(congestion);
                    count++;
                }
            }
        }
        
        return count > 0 ? totalCongestion / count : 0.3;
    }
    
    /**
     * 解析拥堵等级
     */
    private double parseCongestionLevel(String level) {
        if (level == null) return 0.3;
        switch (level.toUpperCase()) {
            case "CLEAR": return 0.1;
            case "SMOOTH": return 0.2;
            case "NORMAL": return 0.4;
            case "MEDIUM": return 0.5;
            case "CONGESTED": return 0.7;
            case "HEAVY": return 0.9;
            case "BLOCKED": return 1.0;
            default: return 0.3;
        }
    }
    
    /**
     * 分析感知数据
     */
    private boolean analyzePerceptionData(List<Object> perceptionData) {
        if (perceptionData == null || perceptionData.isEmpty()) {
            return false;
        }
        
        for (Object data : perceptionData) {
            if (data instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) data;
                if (Boolean.TRUE.equals(map.get("clearPath"))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 提取天气数据
     */
    private Map<String, Object> extractWeatherData(List<Object> weatherData) {
        Map<String, Object> result = new HashMap<>();
        if (weatherData == null || weatherData.isEmpty()) {
            result.put("weather", "CLEAR");
            result.put("visibility", 10000);
            return result;
        }
        
        for (Object data : weatherData) {
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                return new HashMap<>(dataMap);
            }
        }
        return result;
    }
    
    /**
     * 提取无人机数据
     */
    private List<Map<String, Object>> extractDroneData(List<Object> droneData) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (droneData == null || droneData.isEmpty()) {
            return result;
        }
        
        for (Object data : droneData) {
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                result.add(new HashMap<>(dataMap));
            }
        }
        return result;
    }
    
    /**
     * 计算多路径选择的评分
     */
    public double calculateRouteScore(CPSAgent.OptimizedRoute route) {
        double score = 100.0;
        
        // 时间评分
        if (route.estimatedTime > 60) score -= 20;
        else if (route.estimatedTime > 30) score -= 10;
        
        // 距离评分
        if (route.distanceKm > 50) score -= 15;
        else if (route.distanceKm > 20) score -= 5;
        
        // 绕行惩罚
        if (route.rerouted) score -= 10;
        
        return Math.max(0, score);
    }
    
    // ========== 测试 ==========
    public static void main(String[] args) {
        RouteOptimizer optimizer = new RouteOptimizer();
        
        System.out.println("路线优化器测试\n");
        
        // 测试1: 严重拥堵
        CPSAgent.OptimizedRoute route1 = optimizer.optimize(
            "观音桥 -> 西南大学", 45, 0.85, false);
        System.out.println("场景1 - 严重拥堵:");
        System.out.println("  " + route1.description);
        System.out.println("  评分: " + optimizer.calculateRouteScore(route1));
        
        // 测试2: 轻度拥堵+无人机辅助
        Map<String, Object> drone = new HashMap<>();
        drone.put("available", true);
        drone.put("batteryLevel", 85.0);
        
        CPSAgent.OptimizedRoute route2 = optimizer.optimize(
            "大坪 -> 解放碑", 20, 0.3, true,
            null, null, Arrays.asList(drone));
        System.out.println("\n场景2 - 轻度拥堵+无人机辅助:");
        System.out.println("  " + route2.description);
        System.out.println("  评分: " + optimizer.calculateRouteScore(route2));
        
        // 测试3: 模拟外部数据
        Map<String, List<Object>> subscribedData = new HashMap<>();
        
        // 交通流数据
        List<Object> traffic = new ArrayList<>();
        Map<String, Object> traffic1 = new HashMap<>();
        traffic1.put("congestionLevel", 0.6);
        traffic.add(traffic1);
        subscribedData.put("TrafficFlow", traffic);
        
        // 感知数据
        List<Object> perception = new ArrayList<>();
        Map<String, Object> perc1 = new HashMap<>();
        perc1.put("clearPath", true);
        perception.add(perc1);
        subscribedData.put("IntersectionPerception", perception);
        
        System.out.println("\n场景3 - 综合优化:");
        CPSAgent.OptimizedRoute route3 = optimizer.optimizeWithAPI(
            "观音桥", "西南大学", subscribedData);
        System.out.println("  " + route3.description);
    }
}
