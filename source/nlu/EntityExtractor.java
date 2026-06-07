package nlu;

import java.util.*;
import java.util.regex.*;

/**
 * 实体提取器
 * 从自然语言文本中提取关键实体信息
 */
public class EntityExtractor {
    
    // 目的地提取模式
    private static final Pattern[] DESTINATION_PATTERNS = {
        // "前往/去/导航到 + 地点"
        Pattern.compile("(?:前往|去|导航到|开到|开去|去往|驶向)\\s*(.+?)(?:，|。|$|\\s)"),
        Pattern.compile("(?:到|至)\\s*(.+?)(?:，|。|$|\\s)"),
        // "我要去 + 地点"
        Pattern.compile("我要去\\s*(.+?)(?:，|。|$|\\s)"),
        // 常见目的地关键词
        Pattern.compile("(西南大学|机场|医院|火车站|商场|学校|医院|体育馆|图书馆|公园|市政府)")
    };

    // 当前位置提取模式
    private static final Pattern[] START_LOCATION_PATTERNS = {
        Pattern.compile("(?:我在|我就在|当前位置在|现在在)\\s*(.+?)(?:，|。|！|!|？|\\?|$|\\s)"),
        Pattern.compile("(?:人在|身处)\\s*(.+?)(?:，|。|！|!|？|\\?|$|\\s)")
    };
    
    // 紧急程度关键词
    private static final String[] URGENT_KEYWORDS = {
        "着急", "紧急", "尽快", "快点", "速去", "快", "很急", "十万火急", 
        "马上", "立刻", "赶紧", "火速", "飞速"
    };
    
    // 费用相关关键词
    private static final String[] COST_KEYWORDS = {
        "费用", "费用不是问题", "不计较", "不差钱", "随便花", "钱", 
        "贵", "便宜", "省钱", "省点钱", "省一点钱", "经济", "不花钱", "便宜点", "别太贵"
    };
    
    // 高速相关关键词
    private static final String[] HIGHWAY_KEYWORDS = {
        "高速", "高速公路", "收费", "不走高速", "普通路", "省道", "国道"
    };
    
    // 人数提取模式
    private static final Pattern PASSENGER_PATTERN = 
        Pattern.compile("(?:我|我们|共|一共|总共有?)\\s*(\\d+)\\s*(?:个人|人|位)");
    
    /**
     * 从文本中提取目的地
     */
    public String extractDestination(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        for (Pattern pattern : DESTINATION_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String destination = cleanLocationText(matcher.group(1));
                if (!destination.isEmpty() && destination.length() > 1) {
                    return destination;
                }
            }
        }
        
        return null;
    }

    /**
     * 从文本中提取当前位置
     */
    public String extractStartLocation(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        for (Pattern pattern : START_LOCATION_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String location = cleanLocationText(matcher.group(1));
                if (!location.isEmpty() && location.length() > 1) {
                    return location;
                }
            }
        }

        return null;
    }
    
    /**
     * 提取紧急程度
     */
    public NLUResult.UrgencyLevel extractUrgency(String text) {
        if (text == null || text.isEmpty()) {
            return NLUResult.UrgencyLevel.NORMAL;
        }
        
        String lowerText = text.toLowerCase();

        // 否定紧急优先级最高，避免“我不着急”被误判为HIGH
        if (containsAny(lowerText, "不着急", "不急", "别急", "不赶时间", "不需要快", "慢慢来")) {
            return NLUResult.UrgencyLevel.LOW;
        }
        
        // 优先级：很急/十万火急 > 着急/紧急 > 尽快/快点 > 马上/立刻 > 正常
        if (containsAny(lowerText, "十万火急", "非常急", "特别急")) {
            return NLUResult.UrgencyLevel.CRITICAL;
        }
        if (containsAny(lowerText, "着急", "紧急", "很急", "超急")) {
            return NLUResult.UrgencyLevel.HIGH;
        }
        if (containsAny(lowerText, "尽快", "快点", "快点", "赶紧", "速去", "快")) {
            return NLUResult.UrgencyLevel.HIGH;
        }
        if (containsAny(lowerText, "马上", "立刻", "赶紧")) {
            return NLUResult.UrgencyLevel.HIGH;
        }
        if (containsAny(lowerText, "慢慢", "不急", "随便", "无所谓", "普通", "正常")) {
            return NLUResult.UrgencyLevel.LOW;
        }
        
        return NLUResult.UrgencyLevel.NORMAL;
    }
    
    /**
     * 提取费用偏好
     */
    public NLUResult.CostPreference extractCostPreference(String text) {
        if (text == null || text.isEmpty()) {
            return NLUResult.CostPreference.NORMAL;
        }
        
        String lowerText = text.toLowerCase();
        
        if (containsAny(lowerText, "费用不是问题", "不计较", "不差钱", "随便花", "不省钱", "最贵", "最好")) {
            return NLUResult.CostPreference.ANY;
        }
        if (containsAny(lowerText,
            "省钱", "省点钱", "省一点钱", "省些钱", "少花点", "花少点",
            "便宜", "便宜点", "经济", "优惠", "不花钱", "最省", "别太贵")) {
            return NLUResult.CostPreference.ECONOMICAL;
        }
        
        return NLUResult.CostPreference.NORMAL;
    }
    
    /**
     * 提取路线偏好
     */
    public NLUResult.RoutePreference extractRoutePreference(String text) {
        if (text == null || text.isEmpty()) {
            return NLUResult.RoutePreference.BALANCED;
        }
        
        String lowerText = text.toLowerCase();

        // 明确省钱场景优先，避免“过路费便宜一点”被“快”类词误伤
        if (containsAny(lowerText,
            "省钱", "省点钱", "省一点钱", "省些钱", "少花点", "花少点",
            "便宜", "便宜点", "经济", "不收费", "不走高速", "普通路", "过路费", "少收费", "别太贵")) {
            return NLUResult.RoutePreference.COST_FIRST;
        }
        
        // 高速优先
        if (containsAny(lowerText, "高速", "最快", "时间优先", "速去", "快")) {
            return NLUResult.RoutePreference.TIME_FIRST;
        }
        // 风景路线
        if (containsAny(lowerText, "风景", "景色", "绕", "好看", "慢慢开")) {
            return NLUResult.RoutePreference.SCENIC;
        }
        
        return NLUResult.RoutePreference.BALANCED;
    }
    
    /**
     * 提取乘客数量
     */
    public int extractPassengerCount(String text) {
        if (text == null || text.isEmpty()) {
            return 1; // 默认1人
        }
        
        Matcher matcher = PASSENGER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        
        return 1;
    }
    
    /**
     * 提取车辆ID（如果有指定）
     */
    public String extractVehicleId(String text) {
        if (text == null || text.isEmpty()) {
            return "BYD-VEHICLE-001"; // 默认车辆
        }
        
        // 匹配常见车辆ID格式
        Pattern vehiclePattern = Pattern.compile("(?:车牌|车辆|车)(?:号)?[:：]?\\s*([A-Z0-9-]+)");
        Matcher matcher = vehiclePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "BYD-VEHICLE-001";
    }
    
    /**
     * 判断是否包含任意一个关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 完整的实体提取
     */
    public NLUResult extractAll(String text) {
        NLUResult result = new NLUResult(text);
        
        // 提取起始位置
        result.setStartLocation(extractStartLocation(text));

        // 提取目的地
        result.setDestination(extractDestination(text));
        
        // 提取紧急程度
        result.setUrgencyLevel(extractUrgency(text));
        
        // 提取费用偏好
        result.setCostPreference(extractCostPreference(text));
        
        // 提取路线偏好
        result.setRoutePreference(extractRoutePreference(text));
        
        // 提取乘客数量
        result.setEstimatedPassengers(extractPassengerCount(text));
        
        // 提取车辆ID
        result.setVehicleId(extractVehicleId(text));
        
        // 确定导航模式
        result.setNavigationMode(determineNavigationMode(result));
        
        // 计算置信度
        result.setConfidence(calculateConfidence(result));
        
        return result;
    }

    /**
     * 清洗地点文本，剔除问句尾巴和无意义后缀
     */
    private String cleanLocationText(String location) {
        if (location == null) {
            return "";
        }

        String cleaned = location.trim();
        cleaned = cleaned.replaceAll("(你告诉我|请问|我想知道).*$", "");
        cleaned = cleaned.replaceAll("(要|还要)?\\d+\\s*分钟吗?$", "");
        cleaned = cleaned.replaceAll("(要|还要).*$", "");
        cleaned = cleaned.replaceAll("(还要多久|多久到|要多久|吗)$", "");
        cleaned = cleaned.replaceAll("(进行|参加|发表|准备去|去)?(演讲|开会|办事|上课|考试|面试|培训|讲座).*$", "");
        cleaned = cleaned.replaceAll("[，。！？,.!?].*$", "");
        cleaned = cleaned.trim();

        return cleaned;
    }
    
    /**
     * 根据提取的信息确定导航模式
     */
    private String determineNavigationMode(NLUResult result) {
        // 根据路线偏好和紧急程度确定
        if (result.getRoutePreference() == NLUResult.RoutePreference.TIME_FIRST 
            || result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH 
            || result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "highway_fastest";
        }
        
        if (result.getRoutePreference() == NLUResult.RoutePreference.COST_FIRST) {
            return "economy_route";
        }
        
        if (result.getRoutePreference() == NLUResult.RoutePreference.SCENIC) {
            return "scenic_route";
        }
        
        return "balanced_route";
    }
    
    /**
     * 计算结果置信度
     */
    private double calculateConfidence(NLUResult result) {
        double confidence = 0.5; // 基础置信度
        
        // 有目的地增加置信度
        if (result.getDestination() != null && !result.getDestination().isEmpty()) {
            confidence += 0.2;
        }
        
        // 有明确意图增加置信度
        if (result.getUrgencyLevel() != NLUResult.UrgencyLevel.NORMAL) {
            confidence += 0.15;
        }
        
        if (result.getCostPreference() != NLUResult.CostPreference.NORMAL) {
            confidence += 0.1;
        }
        
        if (result.getRoutePreference() != NLUResult.RoutePreference.BALANCED) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }
}
