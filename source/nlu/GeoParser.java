package nlu;

import java.util.*;

/**
 * 地理信息解析器
 * 实现地址解析、地理编码、POI检索、路径计算等功能
 * 
 * 核心功能：
 * 1. 地址标准化
 * 2. 地理编码（地址→经纬度）
 * 3. 逆地理编码（经纬度→地址）
 * 4. POI检索
 * 5. 距离和路径计算
 * 6. 常用地点预定义
 */
public class GeoParser {
    
    // 常用地点坐标库（模拟数据，实际应调用地图API）
    private Map<String, GeoLocation> knownLocations;
    
    // POI类别库
    private Map<String, List<String>> poiCategories;
    
    public GeoParser() {
        initializeKnownLocations();
        initializePOICategories();
    }
    
    /**
     * 初始化已知地点
     */
    private void initializeKnownLocations() {
        knownLocations = new HashMap<>();
        
        // ===== 重庆地区常用地点 =====
        // 大学
        knownLocations.put("西南大学", new GeoLocation(29.8283, 106.4355, "重庆市北碚区天生路2号"));
        knownLocations.put("重庆大学", new GeoLocation(29.5641, 106.3178, "重庆市沙坪坝区沙正街174号"));
        knownLocations.put("重庆医科大学", new GeoLocation(29.5347, 106.4567, "重庆市渝中区医学院路1号"));
        knownLocations.put("重庆师范大学", new GeoLocation(29.8346, 106.3526, "重庆市沙坪坝区天陈路12号"));
        
        // 医院
        knownLocations.put("西南医院", new GeoLocation(29.5423, 106.4234, "重庆市沙坪坝区高滩岩正街30号"));
        knownLocations.put("新桥医院", new GeoLocation(29.5023, 106.4856, "重庆市沙坪坝区新桥正街183号"));
        knownLocations.put("大坪医院", new GeoLocation(29.5623, 106.5234, "重庆市渝中区长江支路10号"));
        knownLocations.put("人民医院", new GeoLocation(29.5590, 106.5789, "重庆市渝中区道门口街40号"));
        
        // 交通枢纽
        knownLocations.put("重庆江北机场", new GeoLocation(29.7193, 106.6417, "重庆市渝北区江北街道"));
        knownLocations.put("机场", new GeoLocation(29.7193, 106.6417, "重庆市渝北区江北街道"));
        knownLocations.put("重庆北站", new GeoLocation(29.6082, 106.5496, "重庆市渝北区昆仑大道49号"));
        knownLocations.put("重庆站", new GeoLocation(29.5502, 106.5217, "重庆市渝中区南区路1号"));
        knownLocations.put("火车站", new GeoLocation(29.5502, 106.5217, "重庆市渝中区南区路1号"));
        
        // 商业中心
        knownLocations.put("解放碑", new GeoLocation(29.5589, 106.5785, "重庆市渝中区解放碑商圈"));
        knownLocations.put("观音桥", new GeoLocation(29.5789, 106.5623, "重庆市江北区观音桥商圈"));
        knownLocations.put("沙坪坝商圈", new GeoLocation(29.5412, 106.4567, "重庆市沙坪坝区三峡广场"));
        knownLocations.put("南坪商圈", new GeoLocation(29.5234, 106.5678, "重庆市南岸区南坪商圈"));
        knownLocations.put("杨家坪商圈", new GeoLocation(29.5123, 106.4876, "重庆市九龙坡区杨家坪"));
        
        // 政府部门
        knownLocations.put("市政府", new GeoLocation(29.5623, 106.5456, "重庆市渝中区人民路232号"));
        knownLocations.put("公安局", new GeoLocation(29.5612, 106.5567, "重庆市渝中区长滨路305号"));
        
        // 景区
        knownLocations.put("洪崖洞", new GeoLocation(29.5623, 106.5867, "重庆市渝中区嘉滨路88号"));
        knownLocations.put("磁器口", new GeoLocation(29.5812, 106.4478, "重庆市沙坪坝区磁南街1号"));
        knownLocations.put("长江索道", new GeoLocation(29.5534, 106.5876, "重庆市渝中区新华路151号"));
        
        // ===== 充电站/加油站 =====
        knownLocations.put("最近的充电站", null); // 需要动态计算
        knownLocations.put("最近的加油站", null);
        
        // ===== 比亚迪/大疆相关 =====
        knownLocations.put("比亚迪4S店", new GeoLocation(29.6123, 106.5234, "重庆市渝北区金开大道"));
        knownLocations.put("仰望体验店", new GeoLocation(29.5856, 106.5678, "重庆市江北区IFS国金中心"));
        knownLocations.put("腾势4S店", new GeoLocation(29.5923, 106.5432, "重庆市渝北区新南路"));
    }
    
    /**
     * 初始化POI类别
     */
    private void initializePOICategories() {
        poiCategories = new HashMap<>();
        
        poiCategories.put("教育", Arrays.asList("大学", "中学", "小学", "幼儿园", "培训机构"));
        poiCategories.put("医疗", Arrays.asList("医院", "诊所", "药店", "体检中心"));
        poiCategories.put("交通", Arrays.asList("机场", "火车站", "汽车站", "地铁站", "公交站"));
        poiCategories.put("商业", Arrays.asList("商场", "超市", "便利店", "餐厅", "酒店"));
        poiCategories.put("政府", Arrays.asList("政府机关", "派出所", "法院", "税务局"));
        poiCategories.put("景点", Arrays.asList("公园", "景区", "博物馆", "纪念馆"));
        poiCategories.put("住宅", Arrays.asList("小区", "公寓", "别墅"));
        poiCategories.put("汽车服务", Arrays.asList("加油站", "充电站", "停车场", "4S店", "维修站"));
    }
    
    /**
     * 地址标准化
     * 将用户输入的地址转换为标准格式
     */
    public String normalizeAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        String normalized = address.trim();
        
        // 常见缩写和别名的标准化
        Map<String, String> aliasMap = new HashMap<>();
        aliasMap.put("重医", "重庆医科大学");
        aliasMap.put("重师", "重庆师范大学");
        aliasMap.put("交大", "重庆交通大学");
        aliasMap.put("重大", "重庆大学");
        aliasMap.put("西大", "西南大学");
        aliasMap.put("机杨", "机场");
        aliasMap.put("火剪站", "火车站");
        aliasMap.put("汽车站", "汽车站");
        
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                normalized = normalized.replace(entry.getKey(), entry.getValue());
            }
        }
        
        // 如果是已知地点，返回标准化名称
        for (String knownName : knownLocations.keySet()) {
            if (normalized.contains(knownName) || knownName.contains(normalized)) {
                return knownName;
            }
        }
        
        return normalized;
    }
    
    /**
     * 地理编码：地址转经纬度
     */
    public GeoLocation geocode(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        String normalized = normalizeAddress(address);
        
        // 检查已知地点库
        if (knownLocations.containsKey(normalized)) {
            return knownLocations.get(normalized);
        }
        
        // 模糊匹配
        for (Map.Entry<String, GeoLocation> entry : knownLocations.entrySet()) {
            String knownName = entry.getKey();
            // 包含匹配
            if (normalized.contains(knownName) || knownName.contains(normalized)) {
                return entry.getValue();
            }
            // 关键词匹配
            for (String keyword : extractKeywords(normalized)) {
                if (knownName.contains(keyword) && keyword.length() >= 2) {
                    return entry.getValue();
                }
            }
        }
        
        // 无法识别，返回null
        return null;
    }
    
    /**
     * 逆地理编码：经纬度转地址
     */
    public String reverseGeocode(double latitude, double longitude) {
        // 查找最近的已知地点
        GeoLocation nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (GeoLocation location : knownLocations.values()) {
            if (location != null) {
                double distance = calculateDistance(
                    latitude, longitude, 
                    location.getLatitude(), location.getLongitude()
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = location;
                }
            }
        }
        
        // 如果在100米范围内，返回该地点
        if (nearest != null && minDistance < 0.1) { // 约100米
            return nearest.getAddress();
        }
        
        // 否则返回大致位置描述
        return String.format("重庆市(%.4f, %.4f)", latitude, longitude);
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        
        // 常见关键词
        String[] commonKeywords = {
            "大学", "医院", "机场", "火车站", "商场", "公园", "超市",
            "酒店", "银行", "饭店", "政府", "公安局", "派出所",
            "小学", "中学", "学院", "车站", "广场", "大厦"
        };
        
        for (String keyword : commonKeywords) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    /**
     * 计算两点之间的距离（公里）
     * 使用Haversine公式
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 估算到达时间（分钟）
     * 考虑道路类型和交通状况
     */
    public int estimateTravelTime(double distance, String routeType) {
        // 平均速度（公里/小时）
        double avgSpeed;
        switch (routeType) {
            case "highway":
                avgSpeed = 100;
                break;
            case "urban":
                avgSpeed = 30;
                break;
            case "scenic":
                avgSpeed = 40;
                break;
            default:
                avgSpeed = 50;
        }
        
        return (int) (distance / avgSpeed * 60);
    }
    
    /**
     * 查找最近的充电站
     */
    public GeoLocation findNearestChargingStation(double lat, double lon) {
        // 模拟充电站数据
        List<GeoLocation> chargingStations = Arrays.asList(
            new GeoLocation(29.6234, 106.5234, "渝北充电站"),
            new GeoLocation(29.5432, 106.5678, "沙坪坝充电站"),
            new GeoLocation(29.5789, 106.5345, "江北充电站"),
            new GeoLocation(29.5123, 106.5876, "南岸充电站"),
            new GeoLocation(29.5923, 106.4567, "九龙坡充电站")
        );
        
        GeoLocation nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (GeoLocation station : chargingStations) {
            double distance = calculateDistance(lat, lon, station.getLatitude(), station.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = station;
            }
        }
        
        return nearest;
    }
    
    /**
     * 解析目的地
     * 返回完整的地理信息
     */
    public GeoParseResult parseDestination(String text) {
        GeoParseResult result = new GeoParseResult();
        
        // 标准化地址
        String normalized = normalizeAddress(text);
        result.setOriginalText(text);
        result.setNormalizedAddress(normalized);
        
        // 地理编码
        GeoLocation location = geocode(normalized);
        if (location != null) {
            result.setLocation(location);
            result.setLatitude(location.getLatitude());
            result.setLongitude(location.getLongitude());
            result.setFormattedAddress(location.getAddress());
            result.setSuccess(true);
        } else {
            result.setSuccess(false);
            result.setErrorMessage("无法识别目的地：" + text);
        }
        
        return result;
    }
    
    /**
     * 批量地理编码
     */
    public Map<String, GeoLocation> batchGeocode(List<String> addresses) {
        Map<String, GeoLocation> results = new HashMap<>();
        for (String address : addresses) {
            results.put(address, geocode(address));
        }
        return results;
    }
    
    /**
     * POI搜索
     */
    public List<String> searchPOI(String category, String location) {
        return poiCategories.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * 获取所有已知地点
     */
    public Set<String> getAllKnownLocations() {
        return new HashSet<>(knownLocations.keySet());
    }
    
    /**
     * 地理坐标类
     */
    public static class GeoLocation {
        private double latitude;
        private double longitude;
        private String address;
        private String name;
        
        public GeoLocation(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.name = address;
        }
        
        public GeoLocation(double latitude, double longitude, String name, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.address = address;
        }
        
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getAddress() { return address; }
        public String getName() { return name; }
        
        @Override
        public String toString() {
            return String.format("%s (%.4f, %.4f)", name, latitude, longitude);
        }
    }
    
    /**
     * 地理解析结果类
     */
    public static class GeoParseResult {
        private String originalText;
        private String normalizedAddress;
        private double latitude;
        private double longitude;
        private String formattedAddress;
        private GeoLocation location;
        private boolean success;
        private String errorMessage;
        
        public String getOriginalText() { return originalText; }
        public void setOriginalText(String originalText) { this.originalText = originalText; }
        public String getNormalizedAddress() { return normalizedAddress; }
        public void setNormalizedAddress(String normalizedAddress) { 
            this.normalizedAddress = normalizedAddress; 
        }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getFormattedAddress() { return formattedAddress; }
        public void setFormattedAddress(String formattedAddress) { 
            this.formattedAddress = formattedAddress; 
        }
        public GeoLocation getLocation() { return location; }
        public void setLocation(GeoLocation location) { this.location = location; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
