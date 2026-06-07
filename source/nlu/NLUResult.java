package nlu;

import java.util.HashMap;
import java.util.Map;

/**
 * NLU处理结果封装类
 * 存储从自然语言中提取的所有信息
 */
public class NLUResult {
    // 意图识别结果
    private Intent primaryIntent;
    private Map<Intent, Double> intentScores;
    
    // 实体提取结果
    private String destination;           // 目的地
    private String startLocation;         // 起始位置
    private UrgencyLevel urgencyLevel;    // 紧急程度
    private CostPreference costPreference; // 费用偏好
    private RoutePreference routePreference; // 路线偏好
    private int estimatedPassengers;      // 预估乘客数
    private String vehicleId;             // 车辆ID
    private String navigationMode;        // 导航模式
    
    // 原始输入
    private String originalText;
    
    // 置信度
    private double confidence;
    
    public NLUResult() {
        this.intentScores = new HashMap<>();
        this.urgencyLevel = UrgencyLevel.NORMAL;
        this.costPreference = CostPreference.NORMAL;
        this.routePreference = RoutePreference.BALANCED;
        this.confidence = 0.0;
    }
    
    public NLUResult(String originalText) {
        this();
        this.originalText = originalText;
    }
    
    // Getters and Setters
    public Intent getPrimaryIntent() {
        return primaryIntent;
    }
    
    public void setPrimaryIntent(Intent primaryIntent) {
        this.primaryIntent = primaryIntent;
    }
    
    public Map<Intent, Double> getIntentScores() {
        return intentScores;
    }
    
    public void addIntentScore(Intent intent, double score) {
        this.intentScores.put(intent, score);
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getStartLocation() {
        return startLocation;
    }
    
    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }
    
    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }
    
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }
    
    public CostPreference getCostPreference() {
        return costPreference;
    }
    
    public void setCostPreference(CostPreference costPreference) {
        this.costPreference = costPreference;
    }
    
    public RoutePreference getRoutePreference() {
        return routePreference;
    }
    
    public void setRoutePreference(RoutePreference routePreference) {
        this.routePreference = routePreference;
    }
    
    public int getEstimatedPassengers() {
        return estimatedPassengers;
    }
    
    public void setEstimatedPassengers(int estimatedPassengers) {
        this.estimatedPassengers = estimatedPassengers;
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    public String getNavigationMode() {
        return navigationMode;
    }
    
    public void setNavigationMode(String navigationMode) {
        this.navigationMode = navigationMode;
    }
    
    public String getOriginalText() {
        return originalText;
    }
    
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    @Override
    public String toString() {
        return "NLUResult{" +
                "primaryIntent=" + primaryIntent +
                ", destination='" + destination + '\'' +
                ", urgencyLevel=" + urgencyLevel +
                ", costPreference=" + costPreference +
                ", routePreference=" + routePreference +
                ", navigationMode='" + navigationMode + '\'' +
                ", confidence=" + String.format("%.2f", confidence * 100) + "%" +
                '}';
    }
    
    /**
     * 紧急程度枚举
     */
    public enum UrgencyLevel {
        LOW("低", "普通", "慢慢来"),
        NORMAL("正常", "一般", "标准"),
        HIGH("高", "紧急", "着急"),
        CRITICAL("紧急", "很急", "十万火急");
        
        private final String[] expressions;
        private final int responseTimeMinutes;
        
        UrgencyLevel(String... expressions) {
            this.expressions = expressions;
            switch (this.name()) {
                case "LOW": this.responseTimeMinutes = 60; break;
                case "NORMAL": this.responseTimeMinutes = 30; break;
                case "HIGH": this.responseTimeMinutes = 15; break;
                case "CRITICAL": this.responseTimeMinutes = 10; break;
                default: this.responseTimeMinutes = 30;
            }
        }
        
        public int getResponseTimeMinutes() {
            return responseTimeMinutes;
        }
        
        public static UrgencyLevel fromText(String text) {
            if (text == null) return NORMAL;
            String lower = text.toLowerCase();
            
            for (UrgencyLevel level : values()) {
                for (String expr : level.expressions) {
                    if (lower.contains(expr)) {
                        return level;
                    }
                }
            }
            return NORMAL;
        }
    }
    
    /**
     * 费用偏好枚举
     */
    public enum CostPreference {
        ANY("费用不是问题", "不计较", "不差钱", "随便花", "最贵", "最好"),
        NORMAL("正常", "普通", "一般", "都可以"),
        ECONOMICAL("省", "便宜", "经济", "优惠", "不花钱");
        
        private final String[] expressions;
        
        CostPreference(String... expressions) {
            this.expressions = expressions;
        }
        
        public static CostPreference fromText(String text) {
            if (text == null) return NORMAL;
            String lower = text.toLowerCase();
            
            for (CostPreference pref : values()) {
                for (String expr : pref.expressions) {
                    if (lower.contains(expr)) {
                        return pref;
                    }
                }
            }
            return NORMAL;
        }
    }
    
    /**
     * 路线偏好枚举
     */
    public enum RoutePreference {
        TIME_FIRST("最快", "时间优先", "高速", "速去", "快"),
        COST_FIRST("最省", "省钱", "不收费", "经济"),
        BALANCED("正常", "普通", "一般", "都可以"),
        SCENIC("风景", "景色", "绕", "好看");
        
        private final String[] expressions;
        
        RoutePreference(String... expressions) {
            this.expressions = expressions;
        }
        
        public static RoutePreference fromText(String text) {
            if (text == null) return BALANCED;
            String lower = text.toLowerCase();
            
            for (RoutePreference pref : values()) {
                for (String expr : pref.expressions) {
                    if (lower.contains(expr)) {
                        return pref;
                    }
                }
            }
            return BALANCED;
        }
    }
}
