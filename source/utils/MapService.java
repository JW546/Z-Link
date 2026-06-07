package utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟地图API服务
 * 
 * 模拟高德地图/百度地图API，提供：
 * 1. 地理编码（地址→坐标转换）
 * 2. 逆地理编码（坐标→地址）
 * 3. 路径规划（多策略路线计算）
 * 4. 距离计算（直线/驾车/步行）
 * 5. ETA估算（考虑实时交通）
 * 
 * 对应论文: 第4章 - 智能导航系统:真实地图API集成
 * 
 * @since 2026-04-15
 */
public class MapService {
    
    // 地图服务提供商
    public enum MapProvider {
        AMAP,      // 高德地图
        BAIDU,     // 百度地图
        GOOGLE,    // Google Maps
        TENGXUN    // 腾讯地图
    }
    
    // 出行方式
    public enum TravelMode {
        DRIVING,      // 驾车
        RIDING,       // 骑行
        WALKING,      // 步行
        TRANSIT,      // 公交
        DRONE         // 无人机
    }
    
    // 路径规划策略
    public enum RouteStrategy {
        FASTEST_TIME,      // 时间最短
        SHORTEST_DISTANCE, // 距离最短
        AVOID_HIGHWAY,      // 避开高速
        SCENIC_ROUTE        // 风景路线
    }
    
    // 重庆市主要地点坐标数据库（模拟）
    private static final Map<String, GeoPoint> CHONGQING_LOCATIONS = new HashMap<>();
    
    // 中国主要城市坐标数据库
    private static final Map<String, GeoPoint> CHINA_LOCATIONS = new HashMap<>();
    
    // 全球主要城市坐标
    private static final Map<String, GeoPoint> GLOBAL_LOCATIONS = new HashMap<>();
    
    // 地点别名映射（用于模糊匹配）
    private static final Map<String, String> LOCATION_ALIASES = new HashMap<>();
    
    // 道路距离修正系数（直线距离→实际道路距离）
    private static final double ROAD_FACTOR_CHONGQING = 1.35; // 重庆多山，道路曲折
    private static final double ROAD_FACTOR_OTHER = 1.25;
    
    static {
        initializeChongqingLocations();
        initializeChinaLocations();
        initializeGlobalLocations();
        initializeAliases();
    }
    
    private MapProvider currentProvider;
    private TravelMode currentMode;
    private RouteStrategy currentStrategy;
    
    public MapService() {
        this.currentProvider = MapProvider.AMAP; // 默认高德
        this.currentMode = TravelMode.DRIVING;
        this.currentStrategy = RouteStrategy.FASTEST_TIME;
    }
    
    /**
     * 初始化重庆市主要地点
     */
    private static void initializeChongqingLocations() {
        // 机场/车站
        addLocation("江北国际机场", 29.7193, 106.6417, "机场", "渝北区");
        addLocation("重庆北站", 29.5578, 106.5489, "火车站", "渝北区");
        addLocation("重庆西站", 29.4923, 106.4856, "火车站", "沙坪坝区");
        addLocation("重庆站", 29.5502, 106.5217, "火车站", "渝中区");
        
        // 商业中心
        addLocation("观音桥", 29.5789, 106.5623, "商圈", "江北区");
        addLocation("解放碑", 29.5589, 106.5785, "商圈", "渝中区");
        addLocation("杨家坪", 29.5123, 106.4876, "商圈", "九龙坡区");
        addLocation("南坪", 29.5234, 106.5678, "商圈", "南岸区");
        addLocation("三峡广场", 29.5412, 106.4567, "商圈", "沙坪坝区");
        
        // 大学/医院
        addLocation("西南大学", 29.8283, 106.4355, "大学", "北碚区");
        addLocation("重庆大学A区", 29.5656, 106.5345, "大学", "沙坪坝区");
        addLocation("重庆大学B区", 29.5623, 106.4678, "大学", "沙坪坝区");
        addLocation("重庆医科大学", 29.5378, 106.5789, "大学", "渝中区");
        addLocation("重庆交通大学", 29.4989, 106.5678, "大学", "南岸区");
        addLocation("重庆师范大学", 29.8234, 106.4234, "大学", "沙坪坝区");
        addLocation("重庆人民医院", 29.5590, 106.5789, "医院", "渝中区");
        addLocation("西南医院", 29.5367, 106.5234, "医院", "沙坪坝区");
        addLocation("新桥医院", 29.5123, 106.6234, "医院", "沙坪坝区");
        
        // 景点
        addLocation("洪崖洞", 29.5623, 106.5878, "景点", "渝中区");
        addLocation("磁器口", 29.5789, 106.4456, "景点", "沙坪坝区");
        addLocation("长江索道", 29.5534, 106.5867, "景点", "渝中区");
        addLocation("南山一棵树", 29.5582, 106.5549, "景点", "南岸区");
        addLocation("武隆天生三桥", 29.3323, 107.8945, "景点", "武隆区");
        addLocation("大足石刻", 29.7234, 105.8234, "景点", "大足区");
        
        // 政府机构
        addLocation("市政府", 29.5623, 106.5456, "政府", "渝中区");
        addLocation("公安局", 29.5567, 106.5789, "政府", "渝中区");
        
        // 住宅区
        addLocation("照母山", 29.6123, 106.5234, "住宅", "渝北区");
        addLocation("礼嘉", 29.6234, 106.6123, "住宅", "两江新区");
        addLocation("蔡家", 29.6789, 106.4567, "住宅", "北碚区");
        addLocation("悦来", 29.6989, 106.5234, "住宅", "渝北区");
    }
    
    /**
     * 初始化中国主要城市
     */
    private static void initializeChinaLocations() {
        // 一线城市
        addGlobalLocation("北京", 39.9042, 116.4074, "首都", "中国");
        addGlobalLocation("上海", 31.2304, 121.4737, "直辖市", "中国");
        addGlobalLocation("广州", 23.1291, 113.2644, "省会", "中国");
        addGlobalLocation("深圳", 22.5431, 114.0579, "特区", "中国");
        
        // 西南城市
        addGlobalLocation("成都", 30.5728, 104.0668, "省会", "中国");
        addGlobalLocation("昆明", 25.0406, 102.7129, "省会", "中国");
        addGlobalLocation("贵阳", 26.6470, 106.6302, "省会", "中国");
        addGlobalLocation("西安", 34.3416, 108.9398, "省会", "中国");
        
        // 直辖市
        addGlobalLocation("天津", 39.3434, 117.3616, "直辖市", "中国");
        addGlobalLocation("重庆", 29.5630, 106.5516, "直辖市", "中国");
    }
    
    /**
     * 初始化全球主要城市
     */
    private static void initializeGlobalLocations() {
        addGlobalLocation("美国", 37.0902, -95.7129, "国家", "美国");
        addGlobalLocation("纽约", 40.7128, -74.0060, "城市", "美国");
        addGlobalLocation("洛杉矶", 34.0522, -118.2437, "城市", "美国");
        addGlobalLocation("旧金山", 37.7749, -122.4194, "城市", "美国");
        
        addGlobalLocation("英国", 55.3781, -3.4360, "国家", "英国");
        addGlobalLocation("伦敦", 51.5074, -0.1278, "首都", "英国");
        
        addGlobalLocation("日本", 36.2048, 138.2529, "国家", "日本");
        addGlobalLocation("东京", 35.6762, 139.6503, "首都", "日本");
        
        addGlobalLocation("德国", 51.1657, 10.4515, "国家", "德国");
        addGlobalLocation("柏林", 52.5200, 13.4050, "首都", "德国");
    }
    
    /**
     * 初始化地点别名映射
     */
    private static void initializeAliases() {
        // 当前位置/默认位置 -> 西南大学
        LOCATION_ALIASES.put("当前位置", "西南大学");
        LOCATION_ALIASES.put("这里", "西南大学");
        LOCATION_ALIASES.put("我现在的地方", "西南大学");
        LOCATION_ALIASES.put("我的位置", "西南大学");
        LOCATION_ALIASES.put("起点", "西南大学");
        
        // 机场别名
        LOCATION_ALIASES.put("机场", "江北国际机场");
        LOCATION_ALIASES.put("飞机场", "江北国际机场");
        LOCATION_ALIASES.put("江北机场", "江北国际机场");
        
        // 火车站别名
        LOCATION_ALIASES.put("火车站", "重庆站");
        LOCATION_ALIASES.put("北站", "重庆北站");
        
        // 商圈别名
        LOCATION_ALIASES.put("沙坪坝", "三峡广场");
        
        // 景点别名
        LOCATION_ALIASES.put("南山", "南山一棵树");
        LOCATION_ALIASES.put("一棵树", "南山一棵树");
        
        // 大学别名
        LOCATION_ALIASES.put("重大", "重庆大学A区");
        LOCATION_ALIASES.put("重医", "重庆医科大学");
    }
    
    private static void addLocation(String name, double lat, double lon, String type, String district) {
        CHONGQING_LOCATIONS.put(name, new GeoPoint(name, lat, lon, type, district));
    }
    
    private static void addGlobalLocation(String name, double lat, double lon, String type, String country) {
        GLOBAL_LOCATIONS.put(name, new GeoPoint(name, lat, lon, type, country));
    }
    
    // ==================== 核心API方法 ====================
    
    /**
     * 地理编码：将地址转换为坐标
     * 
     * @param address 地址文本
     * @return 地理坐标结果
     */
    public GeoCodeResult geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new GeoCodeResult(false, "地址不能为空");
        }
        
        String searchKey = address.trim();
        
        // 0. 先检查别名映射
        if (LOCATION_ALIASES.containsKey(searchKey)) {
            String canonical = LOCATION_ALIASES.get(searchKey);
            GeoPoint aliasMatch = CHONGQING_LOCATIONS.get(canonical);
            if (aliasMatch != null) {
                return new GeoCodeResult(true, aliasMatch.lat, aliasMatch.lon,
                    aliasMatch.name, "别名→" + canonical, 100);
            }
        }
        
        // 1. 精确匹配重庆本地数据库
        GeoPoint localMatch = findBestMatch(CHONGQING_LOCATIONS, searchKey);
        if (localMatch != null) {
            return new GeoCodeResult(true, localMatch.lat, localMatch.lon,
                localMatch.name, "精确匹配", 100);
        }
        
        // 2. 匹配中国城市
        GeoPoint chinaMatch = findBestMatch(CHINA_LOCATIONS, searchKey);
        if (chinaMatch != null) {
            return new GeoCodeResult(true, chinaMatch.lat, chinaMatch.lon,
                chinaMatch.name, "城市匹配", 95);
        }
        
        // 3. 匹配全球城市
        GeoPoint globalMatch = findBestMatch(GLOBAL_LOCATIONS, searchKey);
        if (globalMatch != null) {
            return new GeoCodeResult(true, globalMatch.lat, globalMatch.lon,
                globalMatch.name, "全球匹配", 90);
        }
        
        // 4. 模糊匹配
        GeoPoint fuzzyMatch = fuzzyMatchLocation(searchKey);
        if (fuzzyMatch != null) {
            return new GeoCodeResult(true, fuzzyMatch.lat, fuzzyMatch.lon,
                fuzzyMatch.name, "模糊匹配", 70);
        }
        
        return new GeoCodeResult(false, "未找到匹配地点: " + searchKey);
    }
    
    /**
     * 计算两点间的驾驶距离
     * 
     * @param origin 起点地址
     * @param destination 终点地址
     * @return 距离计算结果
     */
    public DistanceResult calculateDrivingDistance(String origin, String destination) {
        return calculateRoute(origin, destination, TravelMode.DRIVING);
    }
    
    /**
     * 计算两点间的距离（通用方法）
     * 
     * @param origin 起点
     * @param destination 终点
     * @param mode 出行方式
     * @return 距离结果
     */
    public DistanceResult calculateRoute(String origin, String destination, TravelMode mode) {
        // 1. 地理编码
        GeoCodeResult originGeo = geocode(origin);
        GeoCodeResult destGeo = geocode(destination);
        
        if (!originGeo.isSuccess() || !destGeo.isSuccess()) {
            return new DistanceResult(0, 0, 0, 
                "地理编码失败: " + originGeo.getMessage() + " / " + destGeo.getMessage());
        }
        
        // 2. 计算直线距离
        double straightDistance = calculateHaversineDistance(
            originGeo.getLatitude(), originGeo.getLongitude(),
            destGeo.getLatitude(), destGeo.getLongitude()
        );
        
        // 3. 根据出行方式和策略计算实际距离
        double actualDistance = calculateActualDistance(
            straightDistance, originGeo, destGeo, mode);
        
        // 4. 计算ETA
        double etaMinutes = calculateETA(actualDistance, mode);
        
        return new DistanceResult(straightDistance, actualDistance, etaMinutes,
            "成功", originGeo, destGeo);
    }
    
    /**
     * 完整路径规划
     * 
     * @param origin 起点
     * @param destination 终点
     * @param mode 出行方式
     * @param strategy 路径策略
     * @return 路径规划结果
     */
    public RouteResult planRoute(String origin, String destination, 
                                 TravelMode mode, RouteStrategy strategy) {
        // 获取距离结果
        DistanceResult distResult = calculateRoute(origin, destination, mode);
        
        if (distResult.getActualDistance() == 0 && distResult.getEtaMinutes() == 0) {
            return new RouteResult(false, distResult.getMessage());
        }
        
        // 生成途经点
        List<Waypoint> waypoints = generateWaypoints(
            distResult.getOriginGeo(), distResult.getDestGeo(), strategy);
        
        // 生成详细路线
        String routeDescription = generateRouteDescription(
            distResult, waypoints, strategy);
        
        return new RouteResult(true, distResult, waypoints, 
            routeDescription, strategy);
    }
    
    // ==================== 辅助计算方法 ====================
    
    /**
     * Haversine公式计算球面距离
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 地球半径(km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 计算实际道路距离
     */
    private double calculateActualDistance(double straightDistance, 
                                            GeoCodeResult origin, GeoCodeResult dest,
                                            TravelMode mode) {
        // 判断区域
        boolean isChongqing = isInChongqing(origin) && isInChongqing(dest);
        double roadFactor = isChongqing ? ROAD_FACTOR_CHONGQING : ROAD_FACTOR_OTHER;
        
        // 根据出行方式调整
        switch (mode) {
            case WALKING:
                roadFactor *= 1.15; // 步行绕路更多
                break;
            case RIDING:
                roadFactor *= 1.1;
                break;
            case DRIVING:
                roadFactor *= 1.0;
                break;
            case DRONE:
                roadFactor *= 1.05; // 无人机可直线飞行
                break;
            default:
                break;
        }
        
        // 计算实际距离
        return Math.round(straightDistance * roadFactor * 10) / 10.0;
    }
    
    /**
     * 计算ETA（预计到达时间）
     */
    private double calculateETA(double distanceKm, TravelMode mode) {
        // 平均速度（km/h）
        double avgSpeed;
        switch (mode) {
            case DRIVING:
                avgSpeed = 45; // 考虑城市路况
                break;
            case RIDING:
                avgSpeed = 20;
                break;
            case WALKING:
                avgSpeed = 5;
                break;
            case DRONE:
                avgSpeed = 80; // 无人机巡航速度
                break;
            default:
                avgSpeed = 40;
        }
        
        return Math.round(distanceKm / avgSpeed * 60); // 返回分钟
    }
    
    /**
     * 判断是否在重庆
     */
    private boolean isInChongqing(GeoCodeResult geo) {
        if (geo == null) return false;
        double lat = geo.getLatitude();
        double lon = geo.getLongitude();
        // 重庆大致范围
        return lat >= 28.5 && lat <= 32.0 && lon >= 105.5 && lon <= 110.0;
    }
    
    /**
     * 查找最佳匹配
     */
    private GeoPoint findBestMatch(Map<String, GeoPoint> database, String searchKey) {
        // 精确匹配
        if (database.containsKey(searchKey)) {
            return database.get(searchKey);
        }
        
        // 包含匹配
        for (Map.Entry<String, GeoPoint> entry : database.entrySet()) {
            if (entry.getKey().contains(searchKey) || searchKey.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 模糊匹配
     */
    private GeoPoint fuzzyMatchLocation(String searchKey) {
        // 关键词提取
        List<String> keywords = extractKeywords(searchKey);
        
        // 遍历数据库查找匹配
        for (Map.Entry<String, GeoPoint> entry : CHONGQING_LOCATIONS.entrySet()) {
            String name = entry.getKey();
            for (String keyword : keywords) {
                if (name.contains(keyword) || keyword.length() >= 2 && name.contains(keyword)) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        
        // 移除常见无意义词
        String[] noise = {"去", "到", "往", "至", "的", "我要", "请带我去", "导航到"};
        String cleaned = text;
        for (String n : noise) {
            cleaned = cleaned.replace(n, "");
        }
        
        // 提取连续中文字符
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]+");
        Matcher matcher = pattern.matcher(cleaned);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }
        
        return keywords;
    }
    
    /**
     * 生成途经点
     */
    private List<Waypoint> generateWaypoints(GeoCodeResult origin, GeoCodeResult dest, 
                                             RouteStrategy strategy) {
        List<Waypoint> waypoints = new ArrayList<>();
        
        // 添加起点
        waypoints.add(new Waypoint(origin.getFormattedAddress(), 
            origin.getLatitude(), origin.getLongitude(), "起点"));
        
        // 根据策略添加途经点
        switch (strategy) {
            case SCENIC_ROUTE:
                if (isInChongqing(origin) && isInChongqing(dest)) {
                    waypoints.add(new Waypoint("磁器口", 29.5789, 106.4456, "景点"));
                    waypoints.add(new Waypoint("洪崖洞", 29.5623, 106.5878, "景点"));
                }
                break;
            case AVOID_HIGHWAY:
                if (isInChongqing(origin) && isInChongqing(dest)) {
                    waypoints.add(new Waypoint("沙坪坝城区", 29.5412, 106.4567, "普通道路"));
                }
                break;
            default:
                // 不添加额外途经点
                break;
        }
        
        // 添加终点
        waypoints.add(new Waypoint(dest.getFormattedAddress(), 
            dest.getLatitude(), dest.getLongitude(), "终点"));
        
        return waypoints;
    }
    
    /**
     * 生成路线描述
     */
    private String generateRouteDescription(DistanceResult dist, List<Waypoint> waypoints,
                                             RouteStrategy strategy) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("【").append(strategy.name()).append("】\n");
        sb.append("总距离: ").append(dist.getActualDistance()).append("公里\n");
        sb.append("预计时间: ").append(dist.getEtaMinutes()).append("分钟\n");
        sb.append("途经地点:\n");
        
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            String prefix = (i == 0) ? "起点" : (i == waypoints.size() - 1) ? "终点" : "途经";
            sb.append("  ").append(i + 1).append(". [")
              .append(prefix).append("] ")
              .append(wp.name).append(" (")
              .append(String.format("%.4f", wp.lat)).append(", ")
              .append(String.format("%.4f", wp.lon)).append(")\n");
        }
        
        return sb.toString();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 地理坐标点
     */
    private static class GeoPoint {
        String name;
        double lat, lon;
        String type;
        String district;
        
        GeoPoint(String name, double lat, double lon, String type, String district) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.type = type;
            this.district = district;
        }
    }
    
    /**
     * 地理编码结果
     */
    public static class GeoCodeResult {
        private boolean success;
        private double latitude;
        private double longitude;
        private String formattedAddress;
        private String message;
        private int confidence; // 置信度 0-100
        
        GeoCodeResult(boolean success, double lat, double lon, String address, 
                      String msg, int confidence) {
            this.success = success;
            this.latitude = lat;
            this.longitude = lon;
            this.formattedAddress = address;
            this.message = msg;
            this.confidence = confidence;
        }
        
        GeoCodeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.latitude = 0;
            this.longitude = 0;
        }
        
        public boolean isSuccess() { return success; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getFormattedAddress() { return formattedAddress; }
        public String getMessage() { return message; }
        public int getConfidence() { return confidence; }
    }
    
    /**
     * 距离计算结果
     */
    public static class DistanceResult {
        private double straightDistance; // 直线距离(km)
        private double actualDistance;   // 实际距离(km)
        private double etaMinutes;       // 预计时间(分钟)
        private String message;
        private GeoCodeResult originGeo;
        private GeoCodeResult destGeo;
        
        DistanceResult(double straight, double actual, double eta, String msg) {
            this.straightDistance = straight;
            this.actualDistance = actual;
            this.etaMinutes = eta;
            this.message = msg;
        }
        
        DistanceResult(double straight, double actual, double eta, String msg,
                       GeoCodeResult origin, GeoCodeResult dest) {
            this(straight, actual, eta, msg);
            this.originGeo = origin;
            this.destGeo = dest;
        }
        
        public double getStraightDistance() { return straightDistance; }
        public double getActualDistance() { return actualDistance; }
        public double getEtaMinutes() { return etaMinutes; }
        public String getMessage() { return message; }
        public GeoCodeResult getOriginGeo() { return originGeo; }
        public GeoCodeResult getDestGeo() { return destGeo; }
    }
    
    /**
     * 途经点
     */
    public static class Waypoint {
        String name;
        double lat, lon;
        String type;
        
        Waypoint(String name, double lat, double lon, String type) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.type = type;
        }
    }
    
    /**
     * 路径规划结果
     */
    public static class RouteResult {
        private boolean success;
        private DistanceResult distanceResult;
        private List<Waypoint> waypoints;
        private String routeDescription;
        private RouteStrategy strategy;
        
        RouteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        RouteResult(boolean success, DistanceResult dist, List<Waypoint> waypoints,
                    String description, RouteStrategy strategy) {
            this.success = success;
            this.distanceResult = dist;
            this.waypoints = waypoints;
            this.routeDescription = description;
            this.strategy = strategy;
        }
        
        public boolean isSuccess() { return success; }
        public DistanceResult getDistanceResult() { return distanceResult; }
        public List<Waypoint> getWaypoints() { return waypoints; }
        public String getRouteDescription() { return routeDescription; }
        public RouteStrategy getStrategy() { return strategy; }
        
        private String message;
        public String getMessage() { return message; }
    }
    
    // ==================== 便捷静态方法 ====================
    
    /**
     * 快速计算距离（静态便捷方法）
     * 
     * @param origin 起点
     * @param destination 终点
     * @return [直线距离km, 实际距离km, ETA分钟]
     */
    public static double[] quickCalculate(String origin, String destination) {
        MapService service = new MapService();
        DistanceResult result = service.calculateDrivingDistance(origin, destination);
        return new double[] { 
            result.getStraightDistance(), 
            result.getActualDistance(), 
            result.getEtaMinutes() 
        };
    }
    
    /**
     * 获取预估到达时间（分钟）
     */
    public static int getEstimatedArrivalMinutes(String origin, String destination) {
        double[] result = quickCalculate(origin, destination);
        return (int) result[2];
    }
    
    /**
     * 获取预估距离（公里）
     */
    public static double getEstimatedDistanceKm(String origin, String destination) {
        double[] result = quickCalculate(origin, destination);
        return result[1];
    }
    
    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        MapService service = new MapService();
        
        System.out.println("=== 模拟地图API服务测试 ===\n");
        
        // 测试用例
        String[][] testCases = {
            {"西南大学", "江北国际机场"},
            {"观音桥", "解放碑"},
            {"重庆大学", "洪崖洞"},
            {"磁器口", "南山一棵树"},
            {"沙坪坝", "南坪"},
        };
        
        for (String[] test : testCases) {
            System.out.println("路线: " + test[0] + " → " + test[1]);
            
            // 地理编码测试
            GeoCodeResult originGeo = service.geocode(test[0]);
            GeoCodeResult destGeo = service.geocode(test[1]);
            
            if (originGeo.isSuccess() && destGeo.isSuccess()) {
                System.out.println("  起点坐标: (" + originGeo.getLatitude() + ", " 
                    + originGeo.getLongitude() + ") - " + originGeo.getMessage());
                System.out.println("  终点坐标: (" + destGeo.getLatitude() + ", " 
                    + destGeo.getLongitude() + ") - " + destGeo.getMessage());
            }
            
            // 距离计算
            DistanceResult distResult = service.calculateDrivingDistance(test[0], test[1]);
            System.out.println("  直线距离: " + distResult.getStraightDistance() + " km");
            System.out.println("  实际距离: " + distResult.getActualDistance() + " km");
            System.out.println("  预计时间: " + distResult.getEtaMinutes() + " 分钟");
            System.out.println();
        }
        
        // 完整路径规划测试
        System.out.println("=== 完整路径规划测试 ===");
        RouteResult routeResult = service.planRoute("西南大学", "江北国际机场",
            TravelMode.DRIVING, RouteStrategy.FASTEST_TIME);
        
        if (routeResult.isSuccess()) {
            System.out.println(routeResult.getRouteDescription());
        }
    }
}
