package nlu;

import java.util.*;
import utils.AmapRealAPI;

/**
 * 智能路径规划器
 * 基于高德API的真实路径规划
 * 
 * 核心功能：
 * 1. 高德API实时路线查询
 * 2. 多目标优化（时间/距离/费用）
 * 3. 充电站规划
 */
public class RoutePlanner {
    
    // 高德API服务
    private AmapRealAPI amapService;
    
    // 充电站位置
    private List<GeoParser.GeoLocation> chargingStations;
    
    public RoutePlanner() {
        this.amapService = createAmapService();
        this.chargingStations = new ArrayList<>();
        initializeChargingStations();
    }
    
    /**
     * 创建高德API服务
     */
    private AmapRealAPI createAmapService() {
        String apiKey = System.getenv("AMAP_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return new AmapRealAPI(apiKey);
        }
        return new AmapRealAPI();
    }
    
    /**
     * 初始化充电站
     */
    private void initializeChargingStations() {
        chargingStations.add(new GeoParser.GeoLocation(29.6234, 106.5234, "渝北充电站"));
        chargingStations.add(new GeoParser.GeoLocation(29.5432, 106.5678, "沙坪坝充电站"));
        chargingStations.add(new GeoParser.GeoLocation(29.5789, 106.5345, "江北充电站"));
        chargingStations.add(new GeoParser.GeoLocation(29.5923, 106.4567, "九龙坡充电站"));
    }
    
    /**
     * 规划路线
     */
    public RoutePlan planRoute(String origin, String destination, 
                               NLUResult.RoutePreference preference,
                               double batteryLevel) {
        RoutePlan plan = new RoutePlan();
        plan.setOrigin(origin);
        plan.setDestination(destination);
        
        // 使用高德API获取真实路线
        AmapRealAPI.AmapGeoResult originGeo = amapService.geocode(origin);
        AmapRealAPI.AmapGeoResult destGeo = amapService.geocode(destination);
        
        if (!originGeo.isSuccess() || !destGeo.isSuccess()) {
            // 地理编码失败，使用模拟数据
            plan.addWaypoint(origin);
            plan.addWaypoint(destination);
            plan.setEstimatedDistance(20);
            plan.setEstimatedTime(30);
            plan.setRouteType(preference.name());
            return plan;
        }
        
        // 根据偏好选择高德策略
        int strategy = getAmapStrategy(preference);
        AmapRealAPI.AmapRouteResult route = amapService.drivingWithStrategy(
            originGeo.getLocationString(), destGeo.getLocationString(), strategy);
        
        if (route.isSuccess()) {
            plan.addWaypoint(origin);
            plan.addWaypoint(destination);
            plan.setEstimatedDistance(route.getDistanceKm());
            plan.setEstimatedTime(route.getDurationMinutes());
            if (route.getTolls() > 0) {
                plan.setEstimatedCost(route.getTolls());
            }
            plan.setRouteType(preference.name());
        } else {
            // API失败，使用模拟
            plan.addWaypoint(origin);
            plan.addWaypoint(destination);
            plan.setEstimatedDistance(25);
            plan.setEstimatedTime(35);
            plan.setRouteType(preference.name());
        }
        
        // 检查是否需要中途充电
        if (batteryLevel < 30) {
            plan = enhanceWithChargingStops(plan, origin, batteryLevel);
        }
        
        return plan;
    }
    
    /**
     * 将偏好转换为高德策略
     */
    private int getAmapStrategy(NLUResult.RoutePreference preference) {
        switch (preference) {
            case TIME_FIRST: return 0;  // 速度优先
            case COST_FIRST: return 1;  // 费用优先
            case SCENIC: return 2;      // 距离优先
            default: return 0;
        }
    }
    
    /**
     * 增强路线：添加充电站停靠
     */
    private RoutePlan enhanceWithChargingStops(RoutePlan plan, String origin, double batteryLevel) {
        // 找到最近的充电站
        GeoParser.GeoLocation nearestStation = null;
        double minDistance = Double.MAX_VALUE;
        
        AmapRealAPI.AmapGeoResult originGeo = amapService.geocode(origin);
        
        if (originGeo.isSuccess()) {
            for (GeoParser.GeoLocation station : chargingStations) {
                AmapRealAPI.AmapGeoResult stationGeo = amapService.geocode(station.getName());
                if (stationGeo.isSuccess()) {
                    AmapRealAPI.AmapRouteResult r = amapService.driving(
                        originGeo.getLocationString(), stationGeo.getLocationString());
                    if (r.isSuccess() && r.getDistanceKm() < minDistance) {
                        minDistance = r.getDistanceKm();
                        nearestStation = station;
                    }
                }
            }
        }
        
        if (nearestStation != null && minDistance < 50) {
            List<String> waypoints = new ArrayList<>(plan.getWaypoints());
            waypoints.add(1, nearestStation.getName());
            plan.setWaypoints(waypoints);
            plan.addWarning("建议在" + nearestStation.getName() + "补充电量");
            plan.setRequiresCharging(true);
            plan.setChargingStop(nearestStation.getName());
            plan.setEstimatedTime(plan.getEstimatedTime() + 30);
        }
        
        return plan;
    }
    
    /**
     * 交通信息类
     */
    public static class TrafficInfo {
        private String condition;
        private double speedRatio;
        
        TrafficInfo(String condition, double speedRatio) {
            this.condition = condition;
            this.speedRatio = speedRatio;
        }
        
        public String getCondition() { return condition; }
        public double getSpeedRatio() { return speedRatio; }
    }
    
    /**
     * 路线规划结果类
     */
    public static class RoutePlan {
        private String origin;
        private String destination;
        private List<String> waypoints;
        private double estimatedDistance;
        private double estimatedTime;
        private double estimatedCost;
        private String routeType;
        private List<String> features;
        private List<String> warnings;
        private boolean requiresCharging;
        private String chargingStop;
        
        public RoutePlan() {
            this.waypoints = new ArrayList<>();
            this.features = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }
        
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public List<String> getWaypoints() { return waypoints; }
        public void addWaypoint(String waypoint) { this.waypoints.add(waypoint); }
        public void setWaypoints(List<String> waypoints) { this.waypoints = waypoints; }
        public double getEstimatedDistance() { return estimatedDistance; }
        public void setEstimatedDistance(double estimatedDistance) { 
            this.estimatedDistance = estimatedDistance; 
        }
        public double getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(double estimatedTime) { 
            this.estimatedTime = estimatedTime; 
        }
        public double getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(double estimatedCost) { 
            this.estimatedCost = estimatedCost; 
        }
        public String getRouteType() { return routeType; }
        public void setRouteType(String routeType) { this.routeType = routeType; }
        public List<String> getFeatures() { return features; }
        public void addFeature(String feature) { this.features.add(feature); }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public boolean isRequiresCharging() { return requiresCharging; }
        public void setRequiresCharging(boolean requiresCharging) { 
            this.requiresCharging = requiresCharging; 
        }
        public String getChargingStop() { return chargingStop; }
        public void setChargingStop(String chargingStop) { 
            this.chargingStop = chargingStop; 
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("路线规划：\n");
            sb.append("起点：").append(origin).append("\n");
            sb.append("终点：").append(destination).append("\n");
            sb.append("途经点：").append(String.join(" → ", waypoints)).append("\n");
            sb.append("预计距离：").append(String.format("%.1f", estimatedDistance)).append("公里\n");
            sb.append("预计时间：").append(String.format("%.0f", estimatedTime)).append("分钟\n");
            if (estimatedCost > 0) {
                sb.append("预计费用：").append(String.format("%.1f", estimatedCost)).append("元\n");
            }
            sb.append("路线类型：").append(routeType).append("\n");
            
            if (!features.isEmpty()) {
                sb.append("特色：\n");
                for (String feature : features) {
                    sb.append("  - ").append(feature).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("提示：\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}
