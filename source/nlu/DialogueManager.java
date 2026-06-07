package nlu;

import java.util.*;

/**
 * 多轮对话管理器
 * 支持上下文记忆、槽位填充、澄清确认等高级对话功能
 * 
 * 核心功能：
 * 1. 对话状态管理
 * 2. 上下文继承和更新
 * 3. 槽位填充和补全
 * 4. 澄清和确认机制
 * 5. 多轮对话上下文
 */
public class DialogueManager {
    
    // 对话状态栈
    private Stack<DialogueState> stateStack;
    
    // 用户偏好存储
    private Map<String, Object> userPreferences;
    
    // 历史对话记录
    private List<DialogueTurn> dialogueHistory;
    
    // 当前对话ID
    private String currentDialogueId;
    
    // 需要澄清的槽位
    private List<String> pendingClarifications;
    
    // 确认状态
    private boolean awaitingConfirmation;
    
    // 最后一次识别结果
    private NLUResult lastResult;
    
    // 对话超时时间（毫秒）
    private long dialogueTimeout = 300000; // 5分钟
    
    public DialogueManager() {
        this.stateStack = new Stack<>();
        this.userPreferences = new HashMap<>();
        this.dialogueHistory = new ArrayList<>();
        this.pendingClarifications = new ArrayList<>();
        this.awaitingConfirmation = false;
        this.currentDialogueId = UUID.randomUUID().toString();
    }
    
    /**
     * 处理用户输入
     * 返回对话响应
     */
    public DialogueResponse processInput(String userInput) {
        DialogueResponse response = new DialogueResponse();
        
        // 创建NLU处理器进行处理
        NLUProcessor processor = new NLUProcessor();
        NLUResult currentResult = processor.processToResult(userInput);
        
        // 检查是否是确认/否定回复
        if (awaitingConfirmation) {
            return handleConfirmation(userInput, currentResult);
        }
        
        // 检查是否需要澄清
        List<String> missingSlots = checkMissingSlots(currentResult);
        if (!missingSlots.isEmpty()) {
            response.setType(DialogueResponse.Type.CLARIFICATION);
            response.setClarificationQuestion(generateClarificationQuestion(missingSlots));
            response.setMissingSlots(missingSlots);
            pendingClarifications.addAll(missingSlots);
            return response;
        }
        
        // 正常处理：更新上下文
        updateContext(currentResult);
        
        // 记录对话轮次
        DialogueTurn turn = new DialogueTurn(userInput, currentResult);
        dialogueHistory.add(turn);
        
        // 生成响应
        response.setType(DialogueResponse.Type.EXECUTION);
        response.setResult(currentResult);
        response.setContextSummary(generateContextSummary());
        
        // 如果有多个可能的意图，需要确认
        if (currentResult.getIntentScores().size() > 1) {
            double topScore = Collections.max(currentResult.getIntentScores().values());
            double secondScore = getSecondHighestScore(currentResult.getIntentScores());
            
            if (topScore - secondScore < 0.2) {
                response.setType(DialogueResponse.Type.CONFIRMATION);
                response.setConfirmationQuestion(generateIntentConfirmation(currentResult));
                awaitingConfirmation = true;
            }
        }
        
        lastResult = currentResult;
        return response;
    }
    
    /**
     * 处理确认/否定回复
     */
    private DialogueResponse handleConfirmation(String userInput, NLUResult currentResult) {
        DialogueResponse response = new DialogueResponse();
        
        // 简单确认/否定识别
        boolean confirmed = isConfirmation(userInput);
        
        if (confirmed) {
            response.setType(DialogueResponse.Type.EXECUTION);
            response.setResult(lastResult);
            response.setMessage("好的，正在执行任务...");
        } else {
            response.setType(DialogueResponse.Type.REJECTION);
            response.setMessage("好的，已取消。请重新告诉我您的需求。");
            stateStack.clear();
            pendingClarifications.clear();
        }
        
        awaitingConfirmation = false;
        return response;
    }
    
    /**
     * 检查缺失的槽位
     */
    private List<String> checkMissingSlots(NLUResult result) {
        List<String> missing = new ArrayList<>();
        
        // 必须的槽位：目的地
        if (result.getDestination() == null || result.getDestination().isEmpty()) {
            missing.add("destination");
        }
        
        // 可选的槽位：紧急程度（可以根据上下文推断）
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.NORMAL) {
            // 检查上下文是否有暗示
            if (!hasUrgencyImplication(result.getOriginalText())) {
                // 可能需要询问
                // missing.add("urgency");
            }
        }
        
        return missing;
    }
    
    /**
     * 检查文本中是否有紧急程度的暗示
     */
    private boolean hasUrgencyImplication(String text) {
        String[] urgencyIndicators = {"快", "急", "赶紧", "马上", "立刻", "迅速"};
        for (String indicator : urgencyIndicators) {
            if (text.contains(indicator)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成澄清问题
     */
    private String generateClarificationQuestion(List<String> missingSlots) {
        StringBuilder question = new StringBuilder();
        
        if (missingSlots.contains("destination")) {
            question.append("请问您想去哪里？");
        }
        
        if (missingSlots.contains("urgency")) {
            if (question.length() > 0) {
                question.append(" ");
            }
            question.append("请问您着急吗？");
        }
        
        if (missingSlots.contains("passengers")) {
            if (question.length() > 0) {
                question.append(" ");
            }
            question.append("请问有几个人一起？");
        }
        
        return question.toString();
    }
    
    /**
     * 生成意图确认问题
     */
    private String generateIntentConfirmation(NLUResult result) {
        StringBuilder sb = new StringBuilder("您是想要");
        
        // 获取最高分的意图
        Map<Intent, Double> scores = result.getIntentScores();
        Intent topIntent = Collections.max(scores.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
        
        switch (topIntent) {
            case NAVIGATION:
                if (result.getDestination() != null) {
                    sb.append("导航到").append(result.getDestination()).append("吗？");
                } else {
                    sb.append("进行导航吗？");
                }
                break;
            case EMERGENCY:
                sb.append("执行紧急任务吗？");
                break;
            case QUERY_STATUS:
                sb.append("查询车辆状态吗？");
                break;
            default:
                sb.append("执行这个操作吗？");
        }
        
        return sb.toString();
    }
    
    /**
     * 更新对话上下文
     */
    private void updateContext(NLUResult result) {
        // 创建新的对话状态
        DialogueState newState = new DialogueState();
        newState.setResult(result);
        newState.setTimestamp(System.currentTimeMillis());
        
        // 如果有之前的状态，继承未填充的槽位
        if (!stateStack.isEmpty()) {
            DialogueState previousState = stateStack.peek();
            inheritSlots(newState, previousState);
        }
        
        stateStack.push(newState);
        
        // 更新用户偏好
        updatePreferences(result);
    }
    
    /**
     * 继承上一个状态的槽位
     */
    private void inheritSlots(DialogueState newState, DialogueState previousState) {
        NLUResult newResult = newState.getResult();
        NLUResult prevResult = previousState.getResult();
        
        // 如果新结果没有目的地但之前有，继承
        if ((newResult.getDestination() == null || newResult.getDestination().isEmpty())
            && prevResult.getDestination() != null) {
            newResult.setDestination(prevResult.getDestination());
        }
        
        // 继承其他未明确指定的槽位
        if (newResult.getVehicleId() == null) {
            newResult.setVehicleId(prevResult.getVehicleId());
        }
    }
    
    /**
     * 更新用户偏好
     */
    private void updatePreferences(NLUResult result) {
        if (result.getDestination() != null) {
            // 记录常用的目的地
            List<String> frequentDestinations = getFrequentDestinations();
            if (!frequentDestinations.contains(result.getDestination())) {
                frequentDestinations.add(result.getDestination());
            }
            userPreferences.put("frequentDestinations", frequentDestinations);
        }
        
        if (result.getCostPreference() != null) {
            userPreferences.put("preferredCostLevel", result.getCostPreference());
        }
        
        if (result.getRoutePreference() != null) {
            userPreferences.put("preferredRoute", result.getRoutePreference());
        }
    }
    
    /**
     * 获取常用目的地列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getFrequentDestinations() {
        Object obj = userPreferences.get("frequentDestinations");
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
    }
    
    /**
     * 生成上下文摘要
     */
    private String generateContextSummary() {
        if (stateStack.isEmpty()) {
            return "当前无对话上下文";
        }
        
        DialogueState currentState = stateStack.peek();
        NLUResult result = currentState.getResult();
        
        StringBuilder summary = new StringBuilder();
        summary.append("当前目的地: ");
        summary.append(result.getDestination() != null ? result.getDestination() : "未指定");
        summary.append("\n紧急程度: ");
        summary.append(result.getUrgencyLevel());
        summary.append("\n费用偏好: ");
        summary.append(result.getCostPreference());
        summary.append("\n路线偏好: ");
        summary.append(result.getRoutePreference());
        
        return summary.toString();
    }
    
    /**
     * 判断是否为确认回复
     */
    private boolean isConfirmation(String text) {
        String[] confirmWords = {"是", "对", "好的", "ok", "OK", "可以", "行", "确定", "没错"};
        String[] rejectWords = {"不", "否", "算了", "取消", "不用", "没有"};
        
        for (String word : confirmWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        
        for (String word : rejectWords) {
            if (text.contains(word)) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 获取第二高分的值
     */
    private double getSecondHighestScore(Map<Intent, Double> scores) {
        if (scores.size() < 2) {
            return 0.0;
        }
        
        List<Double> sortedScores = new ArrayList<>(scores.values());
        Collections.sort(sortedScores, Collections.reverseOrder());
        return sortedScores.get(1);
    }
    
    /**
     * 修改当前任务
     */
    public DialogueResponse modifyCurrentTask(String modification) {
        DialogueResponse response = new DialogueResponse();
        
        if (stateStack.isEmpty()) {
            response.setType(DialogueResponse.Type.ERROR);
            response.setMessage("当前没有正在进行的任务");
            return response;
        }
        
        // 处理修改请求
        NLUProcessor processor = new NLUProcessor();
        NLUResult modificationResult = processor.processToResult(modification);
        
        // 获取当前状态
        DialogueState currentState = stateStack.peek();
        NLUResult currentResult = currentState.getResult();
        
        // 应用修改
        applyModification(currentResult, modificationResult);
        
        response.setType(DialogueResponse.Type.MODIFICATION);
        response.setResult(currentResult);
        response.setMessage("任务已更新：" + generateContextSummary());
        
        return response;
    }
    
    /**
     * 应用修改到当前结果
     */
    private void applyModification(NLUResult target, NLUResult source) {
        if (source.getDestination() != null) {
            target.setDestination(source.getDestination());
        }
        if (source.getUrgencyLevel() != null) {
            target.setUrgencyLevel(source.getUrgencyLevel());
        }
        if (source.getCostPreference() != null) {
            target.setCostPreference(source.getCostPreference());
        }
        if (source.getRoutePreference() != null) {
            target.setRoutePreference(source.getRoutePreference());
        }
    }
    
    /**
     * 取消当前任务
     */
    public DialogueResponse cancelCurrentTask() {
        DialogueResponse response = new DialogueResponse();
        
        if (stateStack.isEmpty()) {
            response.setType(DialogueResponse.Type.ERROR);
            response.setMessage("当前没有正在进行的任务");
            return response;
        }
        
        stateStack.clear();
        pendingClarifications.clear();
        awaitingConfirmation = false;
        
        response.setType(DialogueResponse.Type.CANCELLATION);
        response.setMessage("任务已成功取消");
        
        return response;
    }
    
    /**
     * 获取对话历史
     */
    public List<DialogueTurn> getDialogueHistory() {
        return new ArrayList<>(dialogueHistory);
    }
    
    /**
     * 获取当前对话状态
     */
    public DialogueState getCurrentState() {
        return stateStack.isEmpty() ? null : stateStack.peek();
    }
    
    /**
     * 重置对话
     */
    public void reset() {
        stateStack.clear();
        pendingClarifications.clear();
        awaitingConfirmation = false;
        dialogueHistory.clear();
        currentDialogueId = UUID.randomUUID().toString();
    }
    
    /**
     * 对话状态类
     */
    public static class DialogueState {
        private NLUResult result;
        private long timestamp;
        private Map<String, Object> contextData;
        
        public DialogueState() {
            this.contextData = new HashMap<>();
        }
        
        public NLUResult getResult() { return result; }
        public void setResult(NLUResult result) { this.result = result; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getContextData() { return contextData; }
    }
    
    /**
     * 对话轮次类
     */
    public static class DialogueTurn {
        private String userInput;
        private NLUResult result;
        private long timestamp;
        
        public DialogueTurn(String userInput, NLUResult result) {
            this.userInput = userInput;
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getUserInput() { return userInput; }
        public NLUResult getResult() { return result; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 对话响应类
     */
    public static class DialogueResponse {
        public enum Type {
            EXECUTION,      // 执行任务
            CLARIFICATION,  // 需要澄清
            CONFIRMATION,   // 需要确认
            MODIFICATION,   // 任务已修改
            CANCELLATION,   // 任务已取消
            REJECTION,      // 用户拒绝
            ERROR           // 错误
        }
        
        private Type type;
        private String message;
        private String clarificationQuestion;
        private String confirmationQuestion;
        private List<String> missingSlots;
        private NLUResult result;
        private String contextSummary;
        
        public DialogueResponse() {
            this.missingSlots = new ArrayList<>();
        }
        
        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getClarificationQuestion() { return clarificationQuestion; }
        public void setClarificationQuestion(String question) { 
            this.clarificationQuestion = question; 
        }
        public String getConfirmationQuestion() { return confirmationQuestion; }
        public void setConfirmationQuestion(String question) { 
            this.confirmationQuestion = question; 
        }
        public List<String> getMissingSlots() { return missingSlots; }
        public void setMissingSlots(List<String> missingSlots) { 
            this.missingSlots = missingSlots; 
        }
        public NLUResult getResult() { return result; }
        public void setResult(NLUResult result) { this.result = result; }
        public String getContextSummary() { return contextSummary; }
        public void setContextSummary(String contextSummary) { 
            this.contextSummary = contextSummary; 
        }
    }
}
