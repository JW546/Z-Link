package nlu;

import java.util.*;

/**
 * 意图识别器
 * 识别用户的核心意图
 */
public class IntentRecognizer {

    private static final List<Intent> PRIMARY_INTENT_ORDER = Arrays.asList(
        Intent.EMERGENCY,
        Intent.DELIVERY,
        Intent.COORDINATION,
        Intent.CONTROL,
        Intent.QUERY_STATUS,
        Intent.NAVIGATION
    );
    
    // 意图模式库
    private Map<Intent, List<IntentPattern>> intentPatterns;
    
    public IntentRecognizer() {
        initIntentPatterns();
    }
    
    /**
     * 初始化意图模式库
     */
    private void initIntentPatterns() {
        intentPatterns = new HashMap<>();
        
        // ===== 导航意图 =====
        List<IntentPattern> navPatterns = new ArrayList<>();
        navPatterns.add(new IntentPattern("前往|去|导航到|开到", 0.9));
        navPatterns.add(new IntentPattern("我要去", 0.95));
        navPatterns.add(new IntentPattern("送我去", 0.9));
        navPatterns.add(new IntentPattern("载我去", 0.85));
        intentPatterns.put(Intent.NAVIGATION, navPatterns);
        
        // ===== 紧急意图 =====
        List<IntentPattern> urgentPatterns = new ArrayList<>();
        urgentPatterns.add(new IntentPattern("着急|紧急|很急", 0.95));
        urgentPatterns.add(new IntentPattern("尽快|快点|赶紧", 0.9));
        urgentPatterns.add(new IntentPattern("马上|立刻|速去", 0.85));
        urgentPatterns.add(new IntentPattern("快.{0,3}点|赶.{0,3}快|越快越好", 0.8));
        intentPatterns.put(Intent.EMERGENCY, urgentPatterns);

        // ===== 配送任务意图 =====
        List<IntentPattern> deliveryPatterns = new ArrayList<>();
        deliveryPatterns.add(new IntentPattern("配送|送达|送货|运送|投递", 0.96));
        deliveryPatterns.add(new IntentPattern("医疗物资|急救包|药品|血液", 0.92));
        intentPatterns.put(Intent.DELIVERY, deliveryPatterns);

        // ===== 乘客意图驱动的多设备协同 =====
        List<IntentPattern> coordinationPatterns = new ArrayList<>();
        coordinationPatterns.add(new IntentPattern("协同|联动|邻车|前车|后车|无人机.*建议|路线建议", 0.96));
        coordinationPatterns.add(new IntentPattern("合规广域链路|5G专网|北斗短报文|国内卫星|卫星移动通信", 0.95));
        coordinationPatterns.add(new IntentPattern("自主订阅|自主发布|互相建议|协商路线|多设备协作", 0.94));
        coordinationPatterns.add(new IntentPattern("编队|多机协作|多车协作", 0.9));
        intentPatterns.put(Intent.COORDINATION, coordinationPatterns);
        
        // ===== 费用相关意图 =====
        List<IntentPattern> costPatterns = new ArrayList<>();
        costPatterns.add(new IntentPattern("费用|成本|开销", 0.9));
        costPatterns.add(new IntentPattern("不差钱|不计较|随便花", 0.95));
        costPatterns.add(new IntentPattern("省钱|省点钱|省一点钱|省些钱|便宜|便宜点|经济|别太贵", 0.9));
        costPatterns.add(new IntentPattern("少花点|花少点|控制开销", 0.85));
        intentPatterns.put(Intent.COST_AWARE, costPatterns);
        
        // ===== 状态查询意图 =====
        List<IntentPattern> queryPatterns = new ArrayList<>();
        queryPatterns.add(new IntentPattern("到哪里了|还有多久|多久到", 0.9));
        queryPatterns.add(new IntentPattern("电量|电量还有多少", 0.85));
        queryPatterns.add(new IntentPattern("现在在哪|位置", 0.8));
        queryPatterns.add(new IntentPattern("我就在|我在.*(还要|多久)|要\\d+分钟吗|到了吗", 0.95));
        queryPatterns.add(new IntentPattern("查询|状态|情况|监控|进度", 0.92));
        intentPatterns.put(Intent.QUERY_STATUS, queryPatterns);
        
        // ===== 车辆控制意图 =====
        List<IntentPattern> controlPatterns = new ArrayList<>();
        controlPatterns.add(new IntentPattern("停车|停下", 0.95));
        controlPatterns.add(new IntentPattern("减速|慢点", 0.9));
        controlPatterns.add(new IntentPattern("加速|快点", 0.85));
        controlPatterns.add(new IntentPattern("充电|加油", 0.9));
        controlPatterns.add(new IntentPattern("执行动作|制动|刹车|空调|车窗", 0.88));
        intentPatterns.put(Intent.CONTROL, controlPatterns);
        
        // ===== 高速偏好意图 =====
        List<IntentPattern> highwayPatterns = new ArrayList<>();
        highwayPatterns.add(new IntentPattern("高速|收费", 0.9));
        highwayPatterns.add(new IntentPattern("走高速|上高速", 0.95));
        highwayPatterns.add(new IntentPattern("最快|时间优先", 0.85));
        intentPatterns.put(Intent.PREFER_HIGHWAY, highwayPatterns);
        
        // ===== 经济路线意图 =====
        List<IntentPattern> economyPatterns = new ArrayList<>();
        economyPatterns.add(new IntentPattern("省钱|省点钱|省一点钱|省些钱|经济|不收费|少花点|花少点", 0.9));
        economyPatterns.add(new IntentPattern("不走高速|普通路|少过路费|过路费少", 0.85));
        intentPatterns.put(Intent.PREFER_ECONOMY, economyPatterns);
    }
    
    /**
     * 识别主要意图
     */
    public Intent recognizePrimaryIntent(String text) {
        if (text == null || text.isEmpty()) {
            return Intent.QUERY_STATUS;
        }

        String lower = text.toLowerCase();

        boolean negatedUrgency = hasNegatedUrgency(text);

        if (isStrongStatusQuery(lower)) {
            return Intent.QUERY_STATUS;
        }
        if (isStrongControlCommand(lower)) {
            return Intent.CONTROL;
        }

        Map<Intent, Double> scores = scorePrimaryIntents(text, negatedUrgency);
        Intent bestIntent = Intent.NAVIGATION;
        double bestScore = 0.0;

        for (Intent intent : PRIMARY_INTENT_ORDER) {
            double score = scores.getOrDefault(intent, 0.0);
            if (score > bestScore) {
                bestScore = score;
                bestIntent = intent;
            }
        }

        if (bestScore <= 0.0) {
            if (isStrongStatusQuery(lower)) {
                return Intent.QUERY_STATUS;
            }
            if (containsAny(lower, "调度", "协同", "联动", "跨品牌", "全域")) {
                return Intent.COORDINATION;
            }
            if (containsAny(lower, "配送", "送达", "送货", "运送", "投递")) {
                return Intent.DELIVERY;
            }
            if (containsAny(lower, "停车", "减速", "加速", "充电", "制动", "空调")) {
                return Intent.CONTROL;
            }
            return Intent.NAVIGATION;
        }

        boolean hasDestinationCue = containsAny(lower, "去", "到", "前往", "导航", "驶向", "路线", "目的地")
            && !isStrongStatusQuery(lower)
            && !isStrongControlCommand(lower);
        boolean hasEmergencyIncidentCue = containsAny(lower,
            "事故", "救援", "报警", "险情", "火灾", "灾害", "伤员", "应急", "求救", "故障");

        if (bestIntent == Intent.EMERGENCY && hasDestinationCue && !hasEmergencyIncidentCue) {
            return Intent.NAVIGATION;
        }

        if ((bestIntent == Intent.COST_AWARE || bestIntent == Intent.PREFER_ECONOMY || bestIntent == Intent.PREFER_HIGHWAY)
            && hasDestinationCue) {
            return Intent.NAVIGATION;
        }

        return bestIntent;
    }
    
    /**
     * 识别所有可能的意图及其分数
     */
    public Map<Intent, Double> recognizeAllIntents(String text) {
        Map<Intent, Double> intentScores = new HashMap<>();
        
        if (text == null || text.isEmpty()) {
            return intentScores;
        }
        
        boolean negatedUrgency = hasNegatedUrgency(text);

        for (Map.Entry<Intent, List<IntentPattern>> entry : intentPatterns.entrySet()) {
            Intent intent = entry.getKey();
            if (negatedUrgency && intent == Intent.EMERGENCY) {
                continue;
            }
            List<IntentPattern> patterns = entry.getValue();
            
            double bestScore = 0.0;
            for (IntentPattern pattern : patterns) {
                double score = pattern.match(text);
                bestScore = Math.max(bestScore, score);
            }
            
            if (bestScore > 0) {
                intentScores.put(intent, bestScore);
            }
        }
        
        return intentScores;
    }

    private Map<Intent, Double> scorePrimaryIntents(String text, boolean negatedUrgency) {
        Map<Intent, Double> scores = new HashMap<>();
        for (Intent intent : PRIMARY_INTENT_ORDER) {
            if (negatedUrgency && intent == Intent.EMERGENCY) {
                continue;
            }
            List<IntentPattern> patterns = intentPatterns.get(intent);
            if (patterns == null || patterns.isEmpty()) {
                continue;
            }
            double bestScore = 0.0;
            for (IntentPattern pattern : patterns) {
                bestScore = Math.max(bestScore, pattern.match(text));
            }
            if (bestScore > 0.0) {
                scores.put(intent, bestScore);
            }
        }
        return scores;
    }

    private boolean isStrongStatusQuery(String lower) {
        return containsAny(lower,
            "到哪里了", "还有多久", "多久到", "到了吗", "现在在哪", "位置", "状态", "情况",
            "查询", "监控", "进度", "电量还有多少", "剩余电量", "剩多少电")
            || (containsAny(lower, "吗", "？", "?") && containsAny(lower, "多久", "哪里", "状态", "电量"));
    }

    private boolean isStrongControlCommand(String lower) {
        if (containsAny(lower, "控制中心", "调度中心") && containsAny(lower, "协同", "调度", "联动", "跨域", "全域")) {
            return false;
        }
        return containsAny(lower,
            "停车", "停下", "立刻停车", "紧急制动", "减速", "加速", "制动", "刹车", "执行动作", "空调", "车窗");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNegatedUrgency(String text) {
        String lower = text.toLowerCase();
        return lower.contains("不着急")
            || lower.contains("不急")
            || lower.contains("别急")
            || lower.contains("不赶时间")
            || lower.contains("不需要快")
            || lower.contains("慢慢来");
    }
    
    /**
     * 意图模式类
     */
    private static class IntentPattern {
        private String regex;
        private double baseScore;
        
        public IntentPattern(String regex, double baseScore) {
            this.regex = regex;
            this.baseScore = baseScore;
        }
        
        public double match(String text) {
            if (text == null) return 0.0;
            if (text.toLowerCase().contains(regex.toLowerCase())) {
                return baseScore;
            }
            // 尝试正则匹配
            try {
                if (java.util.regex.Pattern.compile(regex).matcher(text).find()) {
                    return baseScore;
                }
            } catch (Exception e) {
                // 忽略正则错误
            }
            return 0.0;
        }
    }
    
    /**
     * 完整识别结果
     */
    public NLUResult recognize(String text) {
        NLUResult result = new NLUResult(text);
        
        // 识别主要意图
        result.setPrimaryIntent(recognizePrimaryIntent(text));
        
        // 识别所有意图分数
        Map<Intent, Double> allIntents = recognizeAllIntents(text);
        for (Map.Entry<Intent, Double> entry : allIntents.entrySet()) {
            result.addIntentScore(entry.getKey(), entry.getValue());
        }

        Double primaryScore = allIntents.get(result.getPrimaryIntent());
        if (primaryScore != null) {
            result.setConfidence(primaryScore);
        }
        
        return result;
    }
}
