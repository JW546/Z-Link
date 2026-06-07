package nlu;

import java.util.*;

/**
 * 置信度管理器
 * 实现多级置信度处理和交互式澄清机制
 * 
 * 核心功能：
 * 1. 置信度等级划分
 * 2. 不同置信度级别的处理策略
 * 3. 澄清问题生成
 * 4. 备选方案建议
 */
public class ConfidenceManager {
    
    // 置信度阈值配置
    public static final double HIGH_THRESHOLD = 0.9;
    public static final double MEDIUM_THRESHOLD = 0.6;
    public static final double LOW_THRESHOLD = 0.3;
    public static final double VERY_LOW_THRESHOLD = 0.1;
    
    // 置信度等级枚举
    public enum ConfidenceLevel {
        HIGH("高置信度", "直接执行", HIGH_THRESHOLD),
        MEDIUM("中置信度", "建议确认", MEDIUM_THRESHOLD),
        LOW("低置信度", "需要澄清", LOW_THRESHOLD),
        VERY_LOW("极低置信度", "请求重新输入", VERY_LOW_THRESHOLD);
        
        private String description;
        private String suggestedAction;
        private double threshold;
        
        ConfidenceLevel(String description, String suggestedAction, double threshold) {
            this.description = description;
            this.suggestedAction = suggestedAction;
            this.threshold = threshold;
        }
        
        public String getDescription() { return description; }
        public String getSuggestedAction() { return suggestedAction; }
        public double getThreshold() { return threshold; }
    }
    
    /**
     * 根据置信度值获取等级
     */
    public ConfidenceLevel getConfidenceLevel(double confidence) {
        if (confidence >= HIGH_THRESHOLD) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= MEDIUM_THRESHOLD) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence >= LOW_THRESHOLD) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.VERY_LOW;
        }
    }
    
    /**
     * 获取处理策略
     */
    public HandlingStrategy getStrategy(double confidence, NLUResult result) {
        ConfidenceLevel level = getConfidenceLevel(confidence);
        HandlingStrategy strategy = new HandlingStrategy();
        
        strategy.setConfidenceLevel(level);
        strategy.setConfidence(confidence);
        
        switch (level) {
            case HIGH:
                // 高置信度：直接执行
                strategy.setAction(HandlingStrategy.Action.EXECUTE);
                strategy.setMessage("置信度较高，将直接执行任务");
                break;
                
            case MEDIUM:
                // 中置信度：需要确认
                strategy.setAction(HandlingStrategy.Action.CONFIRM);
                strategy.setConfirmationQuestion(generateConfirmationQuestion(result));
                strategy.setMessage("置信度中等，需要您确认");
                break;
                
            case LOW:
                // 低置信度：需要澄清
                strategy.setAction(HandlingStrategy.Action.CLARIFY);
                strategy.setClarificationQuestion(generateClarificationQuestion(result));
                strategy.setMissingInfo(identifyMissingInfo(result));
                strategy.setMessage("置信度较低，请提供更多信息");
                break;
                
            case VERY_LOW:
                // 极低置信度：请求重新输入
                strategy.setAction(HandlingStrategy.Action.RETRY);
                strategy.setRetrySuggestion(generateRetrySuggestion(result));
                strategy.setMessage("无法理解您的意思，请重新描述");
                break;
        }
        
        return strategy;
    }
    
    /**
     * 生成确认问题
     */
    private String generateConfirmationQuestion(NLUResult result) {
        StringBuilder question = new StringBuilder();
        question.append("您是想要");
        
        if (result.getDestination() != null) {
            question.append("去").append(result.getDestination());
        } else {
            question.append("进行导航");
        }
        
        if (result.getUrgencyLevel() != NLUResult.UrgencyLevel.NORMAL) {
            question.append("，紧急程度：").append(result.getUrgencyLevel());
        }
        
        question.append("，对吗？");
        
        return question.toString();
    }
    
    /**
     * 生成澄清问题
     */
    private String generateClarificationQuestion(NLUResult result) {
        StringBuilder question = new StringBuilder();
        List<String> clarifications = new ArrayList<>();
        
        if (result.getDestination() == null || result.getDestination().isEmpty()) {
            clarifications.add("目的地（您想去哪里？）");
        }
        
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.NORMAL) {
            clarifications.add("紧急程度（是否着急？）");
        }
        
        if (result.getCostPreference() == NLUResult.CostPreference.NORMAL) {
            clarifications.add("费用偏好（是否在意费用？）");
        }
        
        if (clarifications.isEmpty()) {
            return "请确认您的具体需求是什么？";
        }
        
        question.append("请您补充以下信息：");
        for (int i = 0; i < clarifications.size(); i++) {
            question.append("\n").append(i + 1).append(". ").append(clarifications.get(i));
        }
        
        return question.toString();
    }
    
    /**
     * 识别缺失信息
     */
    private List<String> identifyMissingInfo(NLUResult result) {
        List<String> missing = new ArrayList<>();
        
        if (result.getDestination() == null || result.getDestination().isEmpty()) {
            missing.add("destination");
        }
        
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.NORMAL) {
            // 检查是否有隐含的紧急程度
            String text = result.getOriginalText();
            if (text != null && !containsUrgencyIndicators(text)) {
                missing.add("urgency");
            }
        }
        
        if (missing.isEmpty()) {
            missing.add("intent_clarity");
        }
        
        return missing;
    }
    
    /**
     * 检查是否包含紧急程度指示词
     */
    private boolean containsUrgencyIndicators(String text) {
        String[] indicators = {"快", "急", "赶", "马上", "立刻", "赶紧", "速"};
        for (String indicator : indicators) {
            if (text.contains(indicator)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成重试建议
     */
    private String generateRetrySuggestion(NLUResult result) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("请您用更清晰的方式描述您的需求，例如：\n");
        suggestion.append("1. \"我要去[目的地]\"\n");
        suggestion.append("2. \"导航到[目的地]，我很着急\"\n");
        suggestion.append("3. \"送我去[目的地]，费用不是问题\"\n");
        suggestion.append("\n您可以参考以上格式重新输入。");
        
        return suggestion.toString();
    }
    
    /**
     * 计算综合置信度
     * 基于多个因素加权计算
     */
    public double calculateCompositeConfidence(NLUResult result) {
        double baseConfidence = 0.5;
        
        // 目的地识别加成
        if (result.getDestination() != null && !result.getDestination().isEmpty()) {
            baseConfidence += 0.25;
        }
        
        // 紧急程度明确加成
        if (result.getUrgencyLevel() != NLUResult.UrgencyLevel.NORMAL) {
            baseConfidence += 0.1;
        }
        
        // 费用偏好明确加成
        if (result.getCostPreference() != NLUResult.CostPreference.NORMAL) {
            baseConfidence += 0.05;
        }
        
        // 路线偏好明确加成
        if (result.getRoutePreference() != NLUResult.RoutePreference.BALANCED) {
            baseConfidence += 0.05;
        }
        
        // 意图分数加成
        if (!result.getIntentScores().isEmpty()) {
            double maxIntentScore = Collections.max(result.getIntentScores().values());
            baseConfidence += maxIntentScore * 0.1;
        }
        
        return Math.min(baseConfidence, 1.0);
    }
    
    /**
     * 置信度校准
     * 根据历史数据调整置信度
     */
    public double calibrateConfidence(double rawConfidence, String userFeedback) {
        // 简单的反馈调整
        if ("confirmed".equals(userFeedback)) {
            // 用户确认，增加置信度
            return Math.min(rawConfidence + 0.1, 1.0);
        } else if ("rejected".equals(userFeedback)) {
            // 用户拒绝，降低置信度
            return Math.max(rawConfidence - 0.2, 0.0);
        } else if ("modified".equals(userFeedback)) {
            // 用户修改，中等调整
            return rawConfidence * 0.9;
        }
        
        return rawConfidence;
    }
    
    /**
     * 生成备选方案
     */
    public List<String> generateAlternatives(NLUResult result) {
        List<String> alternatives = new ArrayList<>();
        
        if (result.getDestination() == null) {
            // 如果没有目的地，提供常用目的地建议
            alternatives.add("西南大学（常用目的地）");
            alternatives.add("重庆江北机场");
            alternatives.add("重庆北站（火车站）");
            alternatives.add("最近的充电站");
        }
        
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.NORMAL) {
            // 如果没有指定紧急程度，提供选项
            alternatives.add("普通导航模式");
            alternatives.add("紧急快速模式");
        }
        
        return alternatives;
    }
    
    /**
     * 处理策略类
     */
    public static class HandlingStrategy {
        public enum Action {
            EXECUTE,     // 直接执行
            CONFIRM,     // 确认
            CLARIFY,     // 澄清
            RETRY        // 重试
        }
        
        private ConfidenceLevel confidenceLevel;
        private Action action;
        private double confidence;
        private String message;
        private String confirmationQuestion;
        private String clarificationQuestion;
        private List<String> missingInfo;
        private String retrySuggestion;
        
        public ConfidenceLevel getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(ConfidenceLevel confidenceLevel) { 
            this.confidenceLevel = confidenceLevel; 
        }
        public Action getAction() { return action; }
        public void setAction(Action action) { this.action = action; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getConfirmationQuestion() { return confirmationQuestion; }
        public void setConfirmationQuestion(String confirmationQuestion) { 
            this.confirmationQuestion = confirmationQuestion; 
        }
        public String getClarificationQuestion() { return clarificationQuestion; }
        public void setClarificationQuestion(String clarificationQuestion) { 
            this.clarificationQuestion = clarificationQuestion; 
        }
        public List<String> getMissingInfo() { return missingInfo; }
        public void setMissingInfo(List<String> missingInfo) { 
            this.missingInfo = missingInfo; 
        }
        public String getRetrySuggestion() { return retrySuggestion; }
        public void setRetrySuggestion(String retrySuggestion) { 
            this.retrySuggestion = retrySuggestion; 
        }
    }
}
