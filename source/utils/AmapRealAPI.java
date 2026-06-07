package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * 高德地图真实API接入
 * 
 * 接入高德地图Web API v2版本，提供：
 * 1. 地理编码（地址 → 经纬度）
 * 2. 路径规划（驾车/步行/骑行距离和ETA）
 * 3. 关键词搜索（POI检索）
 * 
 * 使用方法：
 * 1. 在高德开放平台申请Web服务API Key: https://console.amap.com/dev/key/app
 * 2. 设置 AMAP_API_KEY 环境变量，或在构造方法中传入
 * 
 * 对应论文: 第4章 - C. 使用真实地图API
 * 
 * @since 2026-04-15
 */
public class AmapRealAPI {
    
    // 高德地图API地址
    private static final String AMAP_GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String AMAP_DIRECTION_URL = "https://restapi.amap.com/v3/direction";
    private static final String AMAP_SEARCH_URL = "https://restapi.amap.com/v3/place/text";
    
    // API Key（需要用户在高德开放平台申请）
    private String apiKey;
    
    // 是否使用模拟模式（当没有API Key时）
    private boolean simulationMode;
    
    public AmapRealAPI() {
        // 尝试从环境变量获取API Key
        this.apiKey = System.getenv("AMAP_API_KEY");
        this.simulationMode = (apiKey == null || apiKey.isEmpty());
        
        if (simulationMode) {
            System.out.println("[AmapRealAPI] 警告: 未设置AMAP_API_KEY环境变量，使用内置数据");
            System.out.println("[AmapRealAPI] 提示: 请访问 https://console.amap.com/dev/key/app 申请API Key");
        } else {
            System.out.println("[AmapRealAPI] 已连接到高德地图API (真实模式)");
        }
    }
    
    public AmapRealAPI(String apiKey) {
        this.apiKey = apiKey;
        this.simulationMode = (apiKey == null || apiKey.isEmpty());
        
        if (simulationMode) {
            System.out.println("[AmapRealAPI] 使用内置数据");
        } else {
            System.out.println("[AmapRealAPI] 已连接到高德地图API (真实模式)");
        }
    }
    
    /**
     * 地理编码：将地址转换为经纬度坐标
     * 
     * @param address 地址文本（如"重庆市渝北区龙溪镇"）
     * @return 地理编码结果
     */
    public AmapGeoResult geocode(String address) {
        String normalizedAddress = normalizeAddressQuery(address);

        // 仅在模拟模式下直接使用本地兜底库
        if (simulationMode) {
            return geocodeFallback(normalizedAddress);
        }
        
        try {
            // 1) 先尝试标准地理编码
            AmapGeoResult result = geocodeByAddress(normalizedAddress);
            if (result != null && result.isSuccess()) {
                return result;
            }

            // 使用POI搜索（带城市参数提高准确性）
            result = geocodeByPOI(normalizedAddress, "重庆");
            if (result != null && result.isSuccess()) {
                return result;
            }
            
            // 全国范围POI搜索
            result = geocodeByPOI(normalizedAddress, "");
            if (result != null && result.isSuccess()) {
                return result;
            }
            
            return geocodeFallback(normalizedAddress);
            
        } catch (Exception e) {
            return geocodeFallback(normalizedAddress);
        }
    }

    private String normalizeAddressQuery(String address) {
        if (address == null) {
            return "";
        }
        String normalized = address.trim();
        normalized = normalized.replaceAll("^[我俺咱]\s*(在|从|于|位于)", "");
        normalized = normalized.replaceAll("^(当前位置|现在在|目前在)\\s*", "");
        normalized = normalized.replaceAll("[，。！？,.!?].*$", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        normalized = canonicalizeChongqingAlias(normalized);
        return normalized;
    }

    private String canonicalizeChongqingAlias(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if ("西南大学".equals(text)) {
            return "重庆市北碚区西南大学北碚校区";
        }
        if ("重庆大学".equals(text)) {
            return "重庆市沙坪坝区重庆大学A区";
        }
        if ("观音桥".equals(text)) {
            return "重庆市江北区观音桥步行街";
        }
        if ("冉家坝".equals(text)) {
            return "重庆市渝北区冉家坝地铁站";
        }
        return text;
    }
    
    /**
     * 地理编码API
     */
    private AmapGeoResult geocodeByAddress(String address) throws Exception {
        String params = "?address=" + URLEncoder.encode(address, "UTF-8") 
                     + "&city=重庆"
                     + "&key=" + apiKey
                     + "&output=json";
        
        String response = sendGet(AMAP_GEOCODE_URL + params);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        if ("1".equals(json.get("status").getAsString())) {
            if (json.has("geocodes") && !json.get("geocodes").isJsonNull()) {
                JsonArray geocodes = json.getAsJsonArray("geocodes");
                if (geocodes.size() > 0) {
                    JsonObject geo = geocodes.get(0).getAsJsonObject();
                    String loc = geo.get("location").getAsString();
                    String[] locParts = loc.split(",");
                    
                    double lng = Double.parseDouble(locParts[0]);
                    double lat = Double.parseDouble(locParts[1]);
                    String province = geo.get("province").getAsString();
                    String city = geo.get("city").getAsString();
                    String district = geo.has("district") && !geo.get("district").isJsonNull() ? geo.get("district").getAsString() : "";
                    String formattedAddress = geo.has("formatted_address") ? geo.get("formatted_address").getAsString() : address;

                    if (!isLikelyChongqing(province, city, district, formattedAddress)) {
                        return null;
                    }
                    
                    return new AmapGeoResult(true, lat, lng, formattedAddress, province + city + district, 100);
                }
            }
        }
        return null;
    }
    
    /**
     * POI搜索地理编码
     */
    private AmapGeoResult geocodeByPOI(String keywords, String city) throws Exception {
        String url = AMAP_SEARCH_URL + "?keywords=" + URLEncoder.encode(keywords, "UTF-8")
                   + "&key=" + apiKey
                   + "&output=json"
                   + "&extensions=base";
        
        if (!city.isEmpty()) {
            url += "&city=" + URLEncoder.encode(city, "UTF-8");
            url += "&citylimit=true";
        }
        
        String response = sendGet(url);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        if ("1".equals(json.get("status").getAsString())) {
            if (json.has("pois") && !json.get("pois").isJsonNull()) {
                JsonArray pois = json.getAsJsonArray("pois");
                if (pois.size() > 0) {
                    JsonObject poi = pickBestPoi(pois, keywords, city);
                    if (poi == null) {
                        return null;
                    }
                    String location = poi.get("location").getAsString();
                    String[] parts = location.split(",");
                    
                    double lng = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    String name = poi.get("name").getAsString();
                    String pname = poi.has("pname") ? poi.get("pname").getAsString() : "";
                    String cityname = poi.has("cityname") ? poi.get("cityname").getAsString() : "";
                    
                    return new AmapGeoResult(true, lat, lng, pname + cityname + name, pname + cityname, 95);
                }
            }
        }
        return null;
    }

    private JsonObject pickBestPoi(JsonArray pois, String keywords, String cityBias) {
        JsonObject bestPoi = null;
        int bestScore = Integer.MIN_VALUE;
        String keyword = keywords == null ? "" : keywords.trim();
        for (int i = 0; i < pois.size(); i++) {
            JsonObject poi = pois.get(i).getAsJsonObject();
            String pname = poi.has("pname") ? poi.get("pname").getAsString() : "";
            String cityname = poi.has("cityname") ? poi.get("cityname").getAsString() : "";
            String adname = poi.has("adname") ? poi.get("adname").getAsString() : "";
            String name = poi.has("name") ? poi.get("name").getAsString() : "";
            String location = poi.has("location") ? poi.get("location").getAsString() : "";

            if (location == null || location.isBlank() || !location.contains(",")) {
                continue;
            }

            int score = 0;
            String area = (pname + cityname + adname).toLowerCase(Locale.ROOT);
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            String lowerName = name.toLowerCase(Locale.ROOT);

            if ("重庆".equals(cityBias)) {
                if (area.contains("重庆")) {
                    score += 100;
                } else {
                    score -= 100;
                }
            }

            if (!lowerKeyword.isBlank() && lowerName.contains(lowerKeyword)) {
                score += 40;
            }
            if (!lowerKeyword.isBlank() && area.contains(lowerKeyword)) {
                score += 20;
            }

            if (score > bestScore) {
                bestScore = score;
                bestPoi = poi;
            }
        }
        return bestPoi;
    }

    private boolean isLikelyChongqing(String province, String city, String district, String formattedAddress) {
        String all = (safeLower(province) + " " + safeLower(city) + " " + safeLower(district) + " " + safeLower(formattedAddress));
        return all.contains("重庆") || all.contains("重慶");
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
    
    /**
     * 驾车路径规划（默认最快路线）
     * 
     * @param origin 起点坐标 "lng,lat"
     * @param destination 终点坐标 "lng,lat"
     * @return 路径规划结果
     */
    public AmapRouteResult driving(String origin, String destination) {
        return drivingWithStrategy(origin, destination, 0);
    }
    
    /**
     * 根据策略驾车路径规划
     * 
     * @param origin 起点坐标 "lng,lat"
     * @param destination 终点坐标 "lng,lat"
     * @param strategy 路线策略:
     *        0 - 速度优先（时间最短）
     *        1 - 费用优先（不走快速路、高速）
     *        2 - 距离优先
     *        3 - 速度优先（多策略）
     *        4 - 躲避拥堵
     * @return 路径规划结果
     */
    public AmapRouteResult drivingWithStrategy(String origin, String destination, int strategy) {
        if (simulationMode) {
            return drivingWithFallback(origin, destination, strategy);
        }
        
        try {
            String params = "/driving?origin=" + origin 
                         + "&destination=" + destination
                         + "&strategy=" + strategy
                         + "&key=" + apiKey
                         + "&output=json";
            
            String response = sendGet(AMAP_DIRECTION_URL + params);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            
            if ("1".equals(json.get("status").getAsString())) {
                JsonObject route = json.getAsJsonObject("route");
                JsonArray paths = route.getAsJsonArray("paths");
                
                if (paths != null && paths.size() > 0) {
                    JsonObject path = paths.get(0).getAsJsonObject();
                    
                    double distance = path.get("distance").getAsDouble(); // 米
                    long duration = path.get("duration").getAsLong(); // 秒
                    
                    // 获取收费信息
                    double tolls = 0;
                    if (path.has("tolls")) {
                        try { tolls = path.get("tolls").getAsDouble(); } catch(Exception e) {}
                    }
                    
                    AmapRouteResult result = new AmapRouteResult(true, distance, duration,
                        distance / 1000.0, duration / 60.0, tolls);
                    result.setStrategyName(strategyToName(strategy));
                    return result;
                }
            }
            
            return drivingWithFallback(origin, destination, strategy);
            
        } catch (Exception e) {
            return drivingWithFallback(origin, destination, strategy);
        }
    }
    
    /**
     * 获取多条备选路线
     * @return 包含多条路线的结果列表
     */
    public List<AmapRouteResult> drivingWithAlternatives(String origin, String destination) {
        List<AmapRouteResult> routes = new ArrayList<>();
        
        if (simulationMode) {
            routes.add(drivingWithFallback(origin, destination, 0)); // 最快
            routes.add(drivingWithFallback(origin, destination, 1)); // 最省钱
            routes.add(drivingWithFallback(origin, destination, 2)); // 最短距离
            return routes;
        }
        
        try {
            // 获取多条路线：速度优先、费用优先、距离优先
            int[] strategies = {0, 1, 2};
            for (int strategy : strategies) {
                AmapRouteResult result = drivingWithStrategy(origin, destination, strategy);
                if (result.isSuccess()) {
                    routes.add(result);
                }
            }
        } catch (Exception e) {
            // fallback
            routes.add(drivingWithFallback(origin, destination, 0));
        }
        
        return routes;
    }
    
    /**
     * 步行路径规划
     */
    public AmapRouteResult walking(String origin, String destination) {
        if (simulationMode) {
            return walkingWithFallback(origin, destination);
        }
        
        try {
            String params = "/walking?origin=" + origin 
                         + "&destination=" + destination
                         + "&key=" + apiKey
                         + "&output=json";
            
            String response = sendGet(AMAP_DIRECTION_URL + params);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            
            if ("1".equals(json.get("status").getAsString())) {
                JsonObject route = json.getAsJsonObject("route");
                JsonArray paths = route.getAsJsonArray("paths");
                
                if (paths != null && paths.size() > 0) {
                    JsonObject path = paths.get(0).getAsJsonObject();
                    
                    double distance = path.get("distance").getAsDouble();
                    long duration = path.get("duration").getAsLong();
                    
                    return new AmapRouteResult(true, distance, duration,
                        distance / 1000.0, duration / 60.0);
                }
            }
            
            return walkingWithFallback(origin, destination);
            
        } catch (Exception e) {
            return walkingWithFallback(origin, destination);
        }
    }
    
    /**
     * POI关键词搜索
     */
    public List<AmapPOI> searchPOI(String keywords, String city) {
        List<AmapPOI> results = new ArrayList<>();
        
        if (simulationMode) {
            // 模拟模式返回空
            return results;
        }
        
        try {
            String params = "?keywords=" + URLEncoder.encode(keywords, "UTF-8")
                          + "&city=" + URLEncoder.encode(city, "UTF-8")
                          + "&key=" + apiKey
                          + "&output=json";
            
            String response = sendGet(AMAP_SEARCH_URL + params);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            
            if ("1".equals(json.get("status").getAsString())) {
                JsonArray pois = json.getAsJsonArray("pois");
                if (pois != null) {
                    for (int i = 0; i < Math.min(pois.size(), 10); i++) {
                        JsonObject poi = pois.get(i).getAsJsonObject();
                        
                        String location = poi.has("location") ? 
                            poi.get("location").getAsString() : "";
                        String[] coords = location.split(",");
                        
                        double lng = coords.length > 0 ? Double.parseDouble(coords[0]) : 0;
                        double lat = coords.length > 1 ? Double.parseDouble(coords[1]) : 0;
                        
                        results.add(new AmapPOI(
                            poi.get("name").getAsString(),
                            poi.has("address") ? poi.get("address").getAsString() : "",
                            poi.has("tel") ? poi.get("tel").getAsString() : "",
                            lat, lng,
                            poi.has("pname") ? poi.get("pname").getAsString() : ""
                        ));
                    }
                }
            }
            
        } catch (Exception e) {
            // 忽略
        }
        
        return results;
    }
    
    /**
     * 内置地理坐标数据库（最后的降级方案）
     */
    private AmapGeoResult geocodeFallback(String address) {
        Map<String, double[]> known = new HashMap<>();
        
        // 主要城市
        known.put("北京", new double[]{39.9042, 116.4074});
        known.put("天津", new double[]{39.3439, 117.3616});
        known.put("上海", new double[]{31.2304, 121.4737});
        known.put("成都", new double[]{30.5728, 104.0668});
        known.put("武汉", new double[]{30.5928, 114.3055});
        known.put("西安", new double[]{34.3416, 108.9398});
        known.put("深圳", new double[]{22.5431, 114.0579});
        known.put("广州", new double[]{23.1291, 113.2644});
        
        // 重庆主城区（纬度, 经度）
        known.put("西南大学荣昌校区", new double[]{29.4078, 105.5946});
        known.put("荣昌校区", new double[]{29.4078, 105.5946});
        known.put("西南大学", new double[]{29.8283, 106.4355});
        known.put("重庆大学", new double[]{29.5686, 106.3182});
        known.put("江北国际机场", new double[]{29.7193, 106.6417});
        known.put("机场", new double[]{29.7193, 106.6417});
        known.put("观音桥", new double[]{29.5630, 106.5516});
        known.put("解放碑", new double[]{29.5587, 106.5783});
        known.put("沙坪坝", new double[]{29.5415, 106.4576});
        known.put("南坪", new double[]{29.5454, 106.5737});
        known.put("杨家坪", new double[]{29.5235, 106.5170});
        known.put("渝北", new double[]{29.6012, 106.6285});
        known.put("北碚", new double[]{29.8254, 106.4377});
        known.put("洪崖洞", new double[]{29.5625, 106.5832});
        known.put("磁器口", new double[]{29.5795, 106.4496});
        known.put("南山", new double[]{29.5523, 106.5956});
        known.put("大坪", new double[]{29.5523, 106.5224});
        known.put("红旗河沟", new double[]{29.5819, 106.5219});
        known.put("石桥铺", new double[]{29.5363, 106.4992});
        known.put("袁家岗", new double[]{29.5483, 106.5048});
        known.put("李子坝", new double[]{29.5593, 106.5216});
        known.put("鹅岭", new double[]{29.5547, 106.5321});
        known.put("两路口", new double[]{29.5556, 106.5389});
        known.put("七星岗", new double[]{29.5601, 106.5489});
        known.put("较场口", new double[]{29.5582, 106.5789});
        known.put("小什字", new double[]{29.5592, 106.5789});
        known.put("上新街", new double[]{29.5432, 106.5699});
        known.put("光电园", new double[]{29.6089, 106.5289});
        known.put("冉家坝", new double[]{29.5968, 106.5189});
        known.put("新南路", new double[]{29.5989, 106.5489});
        known.put("人和", new double[]{29.6312, 106.5899});
        known.put("鸳鸯", new double[]{29.6112, 106.6189});
        known.put("五里店", new double[]{29.5892, 106.5819});
        known.put("五公里", new double[]{29.5292, 106.5819});
        known.put("四公里", new double[]{29.5272, 106.5719});
        known.put("学府大道", new double[]{29.5292, 106.5619});
        
        String key = address.replaceAll("[省市自治区壮族回族维吾尔]", "");
        // 长词优先，避免“西南大学荣昌校区”被“西南大学”提前命中
        List<String> orderedKeys = new ArrayList<>(known.keySet());
        orderedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String k : orderedKeys) {
            if (key.contains(k)) {
                double[] c = known.get(k);
                return new AmapGeoResult(true, c[0], c[1], key, key, 85);
            }
        }
        
        return new AmapGeoResult(false, 0, 0, address, "", 0);
    }
    
    private AmapRouteResult drivingWithFallback(String origin, String destination, int strategy) {
        // origin/destination 格式为 "lng,lat"，需要解析
        try {
            String[] originParts = origin.split(",");
            String[] destParts = destination.split(",");
            
            double lat1 = Double.parseDouble(originParts[1]);
            double lng1 = Double.parseDouble(originParts[0]);
            double lat2 = Double.parseDouble(destParts[1]);
            double lng2 = Double.parseDouble(destParts[0]);
            
            // 使用Haversine计算距离
            double distance = calculateDistance(lat1, lng1, lat2, lng2) * 1000; // 转米
            
            // 根据策略调整估算
            double speed;       // m/s
            double tolls = 0;
            String strategyName;
            
            switch (strategy) {
                case 1: // 费用优先
                    speed = 11.1;       // 40km/h，走普通路
                    tolls = 0;
                    strategyName = strategyToName(strategy);
                    break;
                case 2: // 距离优先
                    speed = 13.9;       // 50km/h
                    tolls = distance > 20000 ? 10 : 0; // 超过20km可能收费
                    strategyName = strategyToName(strategy);
                    break;
                default: // 速度优先
                    speed = 16.7;       // 60km/h，可走高速
                    tolls = distance > 10000 ? 15 : 0;
                    strategyName = strategyToName(strategy);
            }
            
            double duration = distance / speed;
            
            AmapRouteResult result = new AmapRouteResult(true, distance, (long)duration,
                distance / 1000.0, duration / 60.0, tolls);
            result.setStrategyName(strategyName);
            return result;
            
        } catch (Exception e) {
            return new AmapRouteResult(false, 0, 0, 0, 0);
        }
    }
    
    private AmapRouteResult walkingWithFallback(String origin, String destination) {
        try {
            String[] originParts = origin.split(",");
            String[] destParts = destination.split(",");
            
            double lat1 = Double.parseDouble(originParts[1]);
            double lng1 = Double.parseDouble(originParts[0]);
            double lat2 = Double.parseDouble(destParts[1]);
            double lng2 = Double.parseDouble(destParts[0]);
            
            double distance = calculateDistance(lat1, lng1, lat2, lng2) * 1000;
            double duration = distance / 1.4; // 步行约5km/h = 1.4m/s
            
            return new AmapRouteResult(true, distance, (long)duration,
                distance / 1000.0, duration / 60.0);
            
        } catch (Exception e) {
            return new AmapRouteResult(false, 0, 0, 0, 0);
        }
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private String strategyToName(int strategy) {
        switch (strategy) {
            case 1: return "费用优先";
            case 2: return "距离优先";
            case 4: return "躲避拥堵";
            case 3: return "多策略平衡";
            default: return "速度优先";
        }
    }
    
    // ==================== HTTP请求 ====================
    
    /**
     * 发送GET请求
     */
    private String sendGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    // ==================== 结果类 ====================
    
    /**
     * 地理编码结果
     */
    public static class AmapGeoResult {
        private boolean success;
        private double latitude;
        private double longitude;
        private String formattedAddress;
        private String district;
        private int confidence;
        
        public AmapGeoResult(boolean success, double lat, double lng, 
                            String address, String district, int confidence) {
            this.success = success;
            this.latitude = lat;
            this.longitude = lng;
            this.formattedAddress = address;
            this.district = district;
            this.confidence = confidence;
        }
        
        public boolean isSuccess() { return success; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getFormattedAddress() { return formattedAddress; }
        public String getDistrict() { return district; }
        public int getConfidence() { return confidence; }
        
        /**
         * 获取坐标字符串 "lng,lat"（高德格式）
         */
        public String getLocationString() {
            return String.format("%.6f,%.6f", longitude, latitude);
        }
    }
    
    /**
     * 路径规划结果
     */
    public static class AmapRouteResult {
        private boolean success;
        private double distanceMeters;     // 距离(米)
        private long durationSeconds;      // 时间(秒)
        private double distanceKm;         // 距离(公里)
        private double durationMinutes;    // 时间(分钟)
        private double tolls;              // 过路费(元)
        private String strategyName;       // 策略名称
        
        public AmapRouteResult(boolean success, double distanceM, long durationS,
                              double distanceKm, double durationMin) {
            this.success = success;
            this.distanceMeters = distanceM;
            this.durationSeconds = durationS;
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMin;
            this.tolls = 0;
            this.strategyName = "速度优先";
        }
        
        public AmapRouteResult(boolean success, double distanceM, long durationS,
                              double distanceKm, double durationMin, double tolls) {
            this.success = success;
            this.distanceMeters = distanceM;
            this.durationSeconds = durationS;
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMin;
            this.tolls = tolls;
            this.strategyName = "速度优先";
        }
        
        public void setStrategyName(String name) { this.strategyName = name; }
        
        public boolean isSuccess() { return success; }
        public double getDistanceMeters() { return distanceMeters; }
        public long getDurationSeconds() { return durationSeconds; }
        public double getDistanceKm() { return distanceKm; }
        public double getDurationMinutes() { return durationMinutes; }
        public double getTolls() { return tolls; }
        public String getStrategyName() { return strategyName; }
    }
    
    /**
     * POI点
     */
    public static class AmapPOI {
        private String name;
        private String address;
        private String telephone;
        private double latitude;
        private double longitude;
        private String location;
        
        public AmapPOI(String name, String address, String telephone,
                      double lat, double lng, String location) {
            this.name = name;
            this.address = address;
            this.telephone = telephone;
            this.latitude = lat;
            this.longitude = lng;
            this.location = location;
        }
        
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getTelephone() { return telephone; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getLocation() { return location; }
    }
    
    // ==================== 测试主方法 ====================
    
    public static void main(String[] args) throws Exception {
        AmapRealAPI amap = new AmapRealAPI();
        java.util.Scanner sc = new java.util.Scanner(System.in);
        
        System.out.println("\n高德地图API - 实时查询 (输入q退出)");
        System.out.println("命令格式: 1 <地址>   - 地理编码");
        System.out.println("         2 <起点> <终点> - 路径规划");
        System.out.println();
        
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.equals("q") || line.equals("quit")) break;
            
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;
            
            if ("1".equals(parts[0]) && parts.length >= 2) {
                // 地理编码
                StringBuilder addr = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) addr.append(" ");
                    addr.append(parts[i]);
                }
                AmapGeoResult r = amap.geocode(addr.toString());
                if (r.isSuccess()) {
                    System.out.printf("  -> %s%n", r.getFormattedAddress());
                    System.out.printf("   (%.6f, %.6f)%n", r.getLongitude(), r.getLatitude());
                } else {
                    System.out.println("  未找到");
                }
            } else if ("2".equals(parts[0]) && parts.length >= 3) {
                // 路径规划: 2 起点 终点(中间用|分隔)
                StringBuilder origin = new StringBuilder();
                StringBuilder dest = new StringBuilder();
                boolean foundSep = false;
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].equals("|") || parts[i].equals("to")) {
                        foundSep = true;
                        continue;
                    }
                    if (!foundSep) {
                        if (origin.length() > 0) origin.append(" ");
                        origin.append(parts[i]);
                    } else {
                        if (dest.length() > 0) dest.append(" ");
                        dest.append(parts[i]);
                    }
                }
                
                if (dest.length() > 0) {
                    AmapGeoResult o = amap.geocode(origin.toString());
                    AmapGeoResult d = amap.geocode(dest.toString());
                    if (o.isSuccess() && d.isSuccess()) {
                        AmapRouteResult r = amap.driving(o.getLocationString(), d.getLocationString());
                        System.out.printf("  %s -> %s: %.1fkm, %.0f分钟%n", origin, dest, r.getDistanceKm(), r.getDurationMinutes());
                    } else {
                        System.out.println("  起点或终点解析失败: " + origin + " | " + dest);
                    }
                } else {
                    System.out.println("  格式: 2 起点 | 终点 或 2 起点 to 终点");
                }
            } else {
                System.out.println("  格式错误");
            }
        }
        sc.close();
    }
}
