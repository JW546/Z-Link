package nlu;

import java.util.*;

/**
 * 增强版NLU处理器
 * 整合所有NLU组件，提供完整的自然语言理解能力
 * 
 * 核心功能：
 * 1. 语义引擎集成
 * 2. 车辆上下文感知
 * 3. 置信度管理
 * 4. DSL验证
 * 5. 路径规划
 * 6. 缓存管理
 * 7. 对话管理
 */
public class EnhancedNLUProcessor {
    
    // 核心组件
    private NLUProcessor baseProcessor;
    private SemanticEngine semanticEngine;
    private VehicleContextAwareNLU contextAwareNLU;
    private ConfidenceManager confidenceManager;
    private DSLValidator dslValidator;
    private RoutePlanner routePlanner;
    private NLUCacheManager cacheManager;
    private DialogueManager dialogueManager;
    private GeoParser geoParser;
    
    // 配置
    private boolean enableCache = true;
    private boolean enableContextAwareness = true;
    private boolean enableRoutePlanning = true;
    private boolean enableValidation = true;
    
    public EnhancedNLUProcessor() {
        initializeComponents();
    }
    
    /**
     * 初始化所有组件
     */
    private void initializeComponents() {
        this.baseProcessor = new NLUProcessor();
        this.semanticEngine = new SemanticEngine();
        this.contextAwareNLU = new VehicleContextAwareNLU();
        this.confidenceManager = new ConfidenceManager();
        this.dslValidator = new DSLValidator();
        this.routePlanner = new RoutePlanner();
        this.cacheManager = new NLUCacheManager();
        this.dialogueManager = new DialogueManager();
        this.geoParser = new GeoParser();
        
        // 预加载常用数据
        cacheManager.preloadFrequentDestinations(geoParser);
        cacheManager.preloadHotQueries(baseProcessor);
    }
    
    /**
     * 处理自然语言输入
     */
    public EnhancedResult process(String userInput) {
        EnhancedResult result = new EnhancedResult();
        result.setOriginalInput(userInput);
        
        // 1. 检查缓存
        if (enableCache) {
            NLUResult cachedResult = cacheManager.getCachedIntent(userInput);
            if (cachedResult != null) {
                result.setNluResult(cachedResult);
                result.setFromCache(true);
                return completeResult(result);
            }
        }
        
        // 2. 基础NLU处理
        NLUResult baseResult = baseProcessor.processToResult(userInput);
        result.setNluResult(baseResult);
        
        // 3. 语义增强
        enhanceWithSemantics(result);
        
        // 4. 车辆上下文感知
        if (enableContextAwareness) {
            enhanceWithContext(result);
        }
        
        // 5. 置信度评估
        evaluateConfidence(result);
        
        // 6. DSL生成和验证
        generateAndValidateDSL(result);
        
        // 7. 路径规划
        if (enableRoutePlanning) {
            planRoute(result);
        }
        
        // 8. 缓存结果
        if (enableCache) {
            cacheManager.cacheIntent(userInput, baseResult);
        }
        
        return completeResult(result);
    }
    
    /**
     * 语义增强
     */
    private void enhanceWithSemantics(EnhancedResult result) {
        NLUResult nluResult = result.getNluResult();
        
        // 语义相似度计算
        if (nluResult.getDestination() != null) {
            double maxSimilarity = 0;
            String bestMatch = null;
            
            for (String known : geoParser.getAllKnownLocations()) {
                double similarity = semanticEngine.computeSemanticSimilarity(
                    nluResult.getDestination(), known);
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestMatch = known;
                }
            }
            
            if (maxSimilarity > 0.7 && bestMatch != null) {
                result.setSemanticMatch(bestMatch);
                result.setSemanticSimilarity(maxSimilarity);
                
                // 如果相似度高，更新目的地
                if (maxSimilarity > 0.85) {
                    nluResult.setDestination(bestMatch);
                }
            }
        }
        
        // 意图补全
        SemanticEngine.IntentSuggestion suggestion = 
            semanticEngine.completeIntent(
                nluResult.getOriginalText(), "");
        result.setSemanticSuggestion(suggestion);
    }
    
    /**
     * 车辆上下文感知增强
     */
    private void enhanceWithContext(EnhancedResult result) {
        VehicleContextAwareNLU.ContextAwareResult contextResult = 
            contextAwareNLU.processWithContext(result.getOriginalInput());
        
        result.setContextAwareResult(contextResult);
        
        // 更新警告和建议
        for (String warning : contextResult.getWarnings()) {
            result.addWarning(warning);
        }
        for (String suggestion : contextResult.getSuggestions()) {
            result.addSuggestion(suggestion);
        }
        
        // 检查约束违反
        if (!contextResult.isFeasible()) {
            result.setFeasible(false);
            for (String violation : contextResult.getConstraintViolations()) {
                result.addConstraintViolation(violation);
            }
        }
    }
    
    /**
     * 置信度评估
     */
    private void evaluateConfidence(EnhancedResult result) {
        NLUResult nluResult = result.getNluResult();
        
        // 计算综合置信度
        double confidence = confidenceManager.calculateCompositeConfidence(nluResult);
        result.setConfidence(confidence);
        
        // 获取处理策略
        ConfidenceManager.HandlingStrategy strategy = 
            confidenceManager.getStrategy(confidence, nluResult);
        result.setHandlingStrategy(strategy);
        
        // 根据策略调整结果
        switch (strategy.getAction()) {
            case CONFIRM:
                result.setNeedsConfirmation(true);
                result.setConfirmationQuestion(strategy.getConfirmationQuestion());
                break;
            case CLARIFY:
                result.setNeedsClarification(true);
                result.setClarificationQuestion(strategy.getClarificationQuestion());
                if (strategy.getMissingInfo() != null) {
                    for (String info : strategy.getMissingInfo()) {
                        result.addMissingInfo(info);
                    }
                }
                break;
            case RETRY:
                result.setNeedsRetry(true);
                result.setRetrySuggestion(strategy.getRetrySuggestion());
                break;
        }
        
        // 生成备选方案
        if (result.needsClarification()) {
            List<String> alternatives = confidenceManager.generateAlternatives(nluResult);
            result.setAlternatives(alternatives);
        }
    }
    
    /**
     * DSL生成和验证
     */
    private void generateAndValidateDSL(EnhancedResult result) {
        NLUResult nluResult = result.getNluResult();
        
        // 生成DSL
        DSLGenerator generator = new DSLGenerator();
        String dsl = generator.generateDSL(nluResult);
        result.setGeneratedDSL(dsl);
        
        // 验证DSL
        if (enableValidation) {
            DSLValidator.ValidationResult validationResult = 
                dslValidator.validate(dsl);
            result.setValidationResult(validationResult);
            
        if (!validationResult.isValid()) {
            for (String error : validationResult.getErrors()) {
                result.addError(error);
            }
        }
        for (String warning : validationResult.getWarnings()) {
            result.addWarning(warning);
        }
        }
    }
    
    /**
     * 路径规划
     */
    private void planRoute(EnhancedResult result) {
        NLUResult nluResult = result.getNluResult();
        
        if (nluResult.getDestination() == null) {
            return;
        }
        
        // 获取当前车辆状态
        VehicleContextAwareNLU.VehicleStatus status = 
            contextAwareNLU.getVehicleStatus();
        
        // 规划路线
        RoutePlanner.RoutePlan routePlan = routePlanner.planRoute(
            "当前位置",
            nluResult.getDestination(),
            nluResult.getRoutePreference(),
            status.getBatteryLevel()
        );
        
        result.setRoutePlan(routePlan);
        
        // 添加路线相关的警告和建议
        for (String warning : routePlan.getWarnings()) {
            result.addWarning(warning);
        }
        for (String feature : routePlan.getFeatures()) {
            result.addFeature(feature);
        }
    }
    
    /**
     * 完成结果构建
     */
    private EnhancedResult completeResult(EnhancedResult result) {
        // 生成响应消息
        result.setResponseMessage(generateResponseMessage(result));
        return result;
    }
    
    /**
     * 生成响应消息
     */
    private String generateResponseMessage(EnhancedResult result) {
        StringBuilder message = new StringBuilder();
        
        if (result.needsClarification()) {
            message.append(result.getClarificationQuestion());
            if (!result.getAlternatives().isEmpty()) {
                message.append("\n\n您可能想去：");
                for (String alt : result.getAlternatives()) {
                    message.append("\n- ").append(alt);
                }
            }
        } else if (result.needsConfirmation()) {
            message.append(result.getConfirmationQuestion());
        } else if (result.needsRetry()) {
            message.append(result.getRetrySuggestion());
        } else if (!result.isFeasible()) {
            message.append("抱歉，您的请求存在以下问题：\n");
            for (String violation : result.getConstraintViolations()) {
                message.append("- ").append(violation).append("\n");
            }
            if (!result.getSuggestions().isEmpty()) {
                message.append("\n建议：\n");
                for (String suggestion : result.getSuggestions()) {
                    message.append("- ").append(suggestion).append("\n");
                }
            }
        } else {
            message.append("已理解您的需求，正在为您规划路线...\n\n");
            
            if (result.getRoutePlan() != null) {
                message.append(result.getRoutePlan().toString());
            }
            
            if (!result.getWarnings().isEmpty()) {
                message.append("\n提示：\n");
                for (String warning : result.getWarnings()) {
                    message.append("- ").append(warning).append("\n");
                }
            }
        }
        
        return message.toString();
    }
    
    /**
     * 处理多轮对话
     */
    public DialogueManager.DialogueResponse processDialogue(String userInput) {
        return dialogueManager.processInput(userInput);
    }
    
    /**
     * 更新车辆状态
     */
    public void updateVehicleStatus(VehicleContextAwareNLU.VehicleStatus status) {
        contextAwareNLU.updateVehicleStatus(status);
    }
    
    /**
     * 获取缓存统计
     */
    public NLUCacheManager.CacheStatistics getCacheStatistics() {
        return cacheManager.getStatistics();
    }
    
    /**
     * 设置配置
     */
    public void setEnableCache(boolean enable) { this.enableCache = enable; }
    public void setEnableContextAwareness(boolean enable) { this.enableContextAwareness = enable; }
    public void setEnableRoutePlanning(boolean enable) { this.enableRoutePlanning = enable; }
    public void setEnableValidation(boolean enable) { this.enableValidation = enable; }
    
    /**
     * 增强结果类
     */
    public static class EnhancedResult {
        private String originalInput;
        private NLUResult nluResult;
        private boolean fromCache;
        
        // 语义增强
        private String semanticMatch;
        private double semanticSimilarity;
        private SemanticEngine.IntentSuggestion semanticSuggestion;
        
        // 上下文感知
        private VehicleContextAwareNLU.ContextAwareResult contextAwareResult;
        
        // 置信度
        private double confidence;
        private ConfidenceManager.HandlingStrategy handlingStrategy;
        
        // DSL
        private String generatedDSL;
        private DSLValidator.ValidationResult validationResult;
        
        // 路线规划
        private RoutePlanner.RoutePlan routePlan;
        
        // 交互
        private boolean needsConfirmation;
        private boolean needsClarification;
        private boolean needsRetry;
        private boolean feasible = true;
        private String confirmationQuestion;
        private String clarificationQuestion;
        private String retrySuggestion;
        private List<String> missingInfo;
        private List<String> alternatives;
        
        // 消息
        private String responseMessage;
        
        // 警告、建议、错误
        private List<String> warnings;
        private List<String> suggestions;
        private List<String> errors;
        private List<String> constraintViolations;
        private List<String> features;
        
        public EnhancedResult() {
            this.warnings = new ArrayList<>();
            this.suggestions = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.constraintViolations = new ArrayList<>();
            this.features = new ArrayList<>();
            this.missingInfo = new ArrayList<>();
            this.alternatives = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getOriginalInput() { return originalInput; }
        public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }
        public NLUResult getNluResult() { return nluResult; }
        public void setNluResult(NLUResult nluResult) { this.nluResult = nluResult; }
        public boolean isFromCache() { return fromCache; }
        public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
        public String getSemanticMatch() { return semanticMatch; }
        public void setSemanticMatch(String semanticMatch) { this.semanticMatch = semanticMatch; }
        public double getSemanticSimilarity() { return semanticSimilarity; }
        public void setSemanticSimilarity(double semanticSimilarity) { this.semanticSimilarity = semanticSimilarity; }
        public SemanticEngine.IntentSuggestion getSemanticSuggestion() { return semanticSuggestion; }
        public void setSemanticSuggestion(SemanticEngine.IntentSuggestion semanticSuggestion) { 
            this.semanticSuggestion = semanticSuggestion; 
        }
        public VehicleContextAwareNLU.ContextAwareResult getContextAwareResult() { return contextAwareResult; }
        public void setContextAwareResult(VehicleContextAwareNLU.ContextAwareResult contextAwareResult) { 
            this.contextAwareResult = contextAwareResult; 
        }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public ConfidenceManager.HandlingStrategy getHandlingStrategy() { return handlingStrategy; }
        public void setHandlingStrategy(ConfidenceManager.HandlingStrategy handlingStrategy) { 
            this.handlingStrategy = handlingStrategy; 
        }
        public String getGeneratedDSL() { return generatedDSL; }
        public void setGeneratedDSL(String generatedDSL) { this.generatedDSL = generatedDSL; }
        public DSLValidator.ValidationResult getValidationResult() { return validationResult; }
        public void setValidationResult(DSLValidator.ValidationResult validationResult) { 
            this.validationResult = validationResult; 
        }
        public RoutePlanner.RoutePlan getRoutePlan() { return routePlan; }
        public void setRoutePlan(RoutePlanner.RoutePlan routePlan) { this.routePlan = routePlan; }
        public boolean needsConfirmation() { return needsConfirmation; }
        public void setNeedsConfirmation(boolean needsConfirmation) { 
            this.needsConfirmation = needsConfirmation; 
        }
        public boolean needsClarification() { return needsClarification; }
        public void setNeedsClarification(boolean needsClarification) { 
            this.needsClarification = needsClarification; 
        }
        public boolean needsRetry() { return needsRetry; }
        public void setNeedsRetry(boolean needsRetry) { this.needsRetry = needsRetry; }
        public boolean isFeasible() { return feasible; }
        public void setFeasible(boolean feasible) { this.feasible = feasible; }
        public String getConfirmationQuestion() { return confirmationQuestion; }
        public void setConfirmationQuestion(String confirmationQuestion) { 
            this.confirmationQuestion = confirmationQuestion; 
        }
        public String getClarificationQuestion() { return clarificationQuestion; }
        public void setClarificationQuestion(String clarificationQuestion) { 
            this.clarificationQuestion = clarificationQuestion; 
        }
        public String getRetrySuggestion() { return retrySuggestion; }
        public void setRetrySuggestion(String retrySuggestion) { 
            this.retrySuggestion = retrySuggestion; 
        }
        public List<String> getMissingInfo() { return missingInfo; }
        public void setMissingInfo(List<String> missingInfo) { 
            this.missingInfo = missingInfo; 
        }
        public void addMissingInfo(String info) { this.missingInfo.add(info); }
        public List<String> getAlternatives() { return alternatives; }
        public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }
        public String getResponseMessage() { return responseMessage; }
        public void setResponseMessage(String responseMessage) { 
            this.responseMessage = responseMessage; 
        }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public List<String> getSuggestions() { return suggestions; }
        public void addSuggestion(String suggestion) { this.suggestions.add(suggestion); }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        public List<String> getConstraintViolations() { return constraintViolations; }
        public void addConstraintViolation(String violation) { 
            this.constraintViolations.add(violation); 
        }
        public List<String> getFeatures() { return features; }
        public void addFeature(String feature) { this.features.add(feature); }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║               增强版NLU处理结果                                 ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║ 原始输入: ").append(originalInput).append("\n");
            sb.append("║ 置信度: ").append(String.format("%.1f%%", confidence * 100)).append("\n");
            sb.append("║ 缓存命中: ").append(fromCache ? "是" : "否").append("\n");
            sb.append("╠────────────────────────────────────────────────────────────────╣\n");
            
            if (nluResult != null) {
                sb.append("║ NLU结果:\n");
                sb.append("║   目的地: ").append(nluResult.getDestination()).append("\n");
                sb.append("║   紧急程度: ").append(nluResult.getUrgencyLevel()).append("\n");
                sb.append("║   导航模式: ").append(nluResult.getNavigationMode()).append("\n");
            }
            
            if (routePlan != null) {
                sb.append("╠────────────────────────────────────────────────────────────────╣\n");
                sb.append("║ 路线规划:\n");
                sb.append(routePlan.toString().replace("\n", "\n║ "));
            }
            
            if (!warnings.isEmpty()) {
                sb.append("╠────────────────────────────────────────────────────────────────╣\n");
                sb.append("║ 警告:\n");
                for (String w : warnings) {
                    sb.append("║   - ").append(w).append("\n");
                }
            }
            
            if (!suggestions.isEmpty()) {
                sb.append("╠────────────────────────────────────────────────────────────────╣\n");
                sb.append("║ 建议:\n");
                for (String s : suggestions) {
                    sb.append("║   - ").append(s).append("\n");
                }
            }
            
            sb.append("╚══════════════════════════════════════════════════════════════╝");
            
            return sb.toString();
        }
    }
}
