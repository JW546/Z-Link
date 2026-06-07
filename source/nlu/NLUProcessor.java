package nlu;

import agent.CPSEnums;
import agent.DeepSeekClient;

import java.util.*;

/**
 * 自然语言处理主处理器
 * 将用户输入的自然语言转换为DSL代码
 * 
 * 使用示例：
 *   NLUProcessor processor = new NLUProcessor();
 *   String dsl = processor.process("我要前往西南大学，我着急，费用不是问题");
 *   
 *   或者获取完整结果对象：
 *   NLUResult result = processor.processToResult("我要去机场，麻烦快点");
 *   
 * 新增：支持语音/键盘输入混合处理
 *   NLUProcessor processor = new NLUProcessor(new KeyboardInputHandler());
 *   processor.interactiveMode();
 */
public class NLUProcessor {
    
    private IntentRecognizer intentRecognizer;
    private EntityExtractor entityExtractor;
    private DSLGenerator dslGenerator;
    private VoiceInputHandler inputHandler;
    private DeepSeekClient llmIntentClient;
    
    public NLUProcessor() {
        this.intentRecognizer = new IntentRecognizer();
        this.entityExtractor = new EntityExtractor();
        this.dslGenerator = new DSLGenerator();
        this.inputHandler = new KeyboardInputHandler();  // 默认键盘输入
        this.llmIntentClient = new DeepSeekClient();
    }
    
    /**
     * 支持自定义输入处理器的构造方法
     */
    public NLUProcessor(VoiceInputHandler inputHandler) {
        this.intentRecognizer = new IntentRecognizer();
        this.entityExtractor = new EntityExtractor();
        this.dslGenerator = new DSLGenerator();
        this.inputHandler = inputHandler;
        this.llmIntentClient = new DeepSeekClient();
    }
    
    /**
     * 处理自然语言输入，返回DSL代码
     * 
     * @param naturalLanguage 自然语言输入，如 "我要前往西南大学，我着急，费用不是问题"
     * @return DSL格式的代码字符串
     */
    public String process(String naturalLanguage) {
        NLUResult result = processToResult(naturalLanguage);
        return dslGenerator.generateDSL(result);
    }
    
    /**
     * 处理自然语言输入，返回详细结果对象
     * 
     * @param naturalLanguage 自然语言输入
     * @return 包含所有提取信息的NLUResult对象
     */
    public NLUResult processToResult(String naturalLanguage) {
        // 第一步：意图识别
        NLUResult result = intentRecognizer.recognize(naturalLanguage);

        // 第二步：大模型增强意图识别（优先使用Qwen兼容端点，失败自动回退规则）
        enrichIntentByLLM(naturalLanguage, result);
        
        // 第三步：实体提取（补充到结果中）
        NLUResult extracted = entityExtractor.extractAll(naturalLanguage);
        
        // 合并结果
        mergeResults(result, extracted);
        
        return result;
    }

    private void enrichIntentByLLM(String naturalLanguage, NLUResult result) {
        if (llmIntentClient == null || !llmIntentClient.isEnabled() || result == null) {
            return;
        }
        try {
            String currentIntent = result.getPrimaryIntent() == null ? "UNKNOWN" : result.getPrimaryIntent().name();
            DeepSeekClient.DeepSeekSuggestion suggestion = llmIntentClient.suggest(
                naturalLanguage,
                currentIntent,
                CPSEnums.DeviceRole.VEHICLE,
                "nlu_intent_recognition"
            );
            if (suggestion == null || suggestion.intent == null || suggestion.intent.isBlank()) {
                return;
            }

            Intent llmIntent = mapExternalIntent(suggestion.intent);
            if (llmIntent == null) {
                return;
            }

            double llmConfidence = Math.max(0.0, Math.min(1.0, suggestion.confidence));
            result.addIntentScore(llmIntent, llmConfidence);

            boolean shouldAdopt = result.getPrimaryIntent() == null
                || llmIntent == result.getPrimaryIntent()
                || llmConfidence >= llmIntentClient.getAdoptConfidenceThreshold()
                || result.getConfidence() < 0.55;

            if (shouldAdopt) {
                result.setPrimaryIntent(llmIntent);
                result.setConfidence(Math.max(result.getConfidence(), llmConfidence));
                System.out.println("[NLU-LLM] 接管意图: " + llmIntent
                    + " (provider=" + suggestion.provider
                    + ", model=" + suggestion.model
                    + ", confidence=" + String.format(Locale.ROOT, "%.2f", llmConfidence)
                    + ")");
            } else {
                System.out.println("[NLU-LLM] 提供辅助意图: " + llmIntent
                    + " (provider=" + suggestion.provider
                    + ", model=" + suggestion.model
                    + ", confidence=" + String.format(Locale.ROOT, "%.2f", llmConfidence)
                    + ")");
            }
        } catch (Exception e) {
            System.out.println("[NLU-LLM] 意图增强失败，继续使用规则识别: " + e.getMessage());
        }
    }

    private Intent mapExternalIntent(String externalIntent) {
        if (externalIntent == null || externalIntent.isBlank()) {
            return null;
        }
        String token = externalIntent.trim().toUpperCase(Locale.ROOT);
        switch (token) {
            case "NAVIGATION":
                return Intent.NAVIGATION;
            case "DELIVERY":
                return Intent.DELIVERY;
            case "COORDINATION":
                return Intent.COORDINATION;
            case "EMERGENCY":
                return Intent.EMERGENCY;
            case "QUERY_STATUS":
            case "QUERY_BATTERY":
                return Intent.QUERY_STATUS;
            case "STOP":
            case "CHARGE":
                return Intent.CONTROL;
            case "PATROL":
            case "SURVEILLANCE":
                return Intent.COORDINATION;
            case "UNKNOWN":
            default:
                return null;
        }
    }
    
    /**
     * 合并提取结果到识别结果
     */
    private void mergeResults(NLUResult target, NLUResult source) {
        double intentConfidence = target.getConfidence();
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
        target.setEstimatedPassengers(source.getEstimatedPassengers());
        target.setVehicleId(source.getVehicleId());
        target.setNavigationMode(source.getNavigationMode());

        // 保留意图识别置信度，同时吸收实体提取置信度，避免被单侧覆盖。
        target.setConfidence(Math.max(intentConfidence, source.getConfidence()));
    }
    
    /**
     * 批量处理多条自然语言输入
     * 
     * @param inputs 多条自然语言输入
     * @return DSL代码列表
     */
    public List<String> processBatch(List<String> inputs) {
        List<String> results = new ArrayList<>();
        for (String input : inputs) {
            results.add(process(input));
        }
        return results;
    }
    
    /**
     * 交互式对话模式（模拟）
     * 支持多轮对话和上下文记忆
     */
    public static class ConversationContext {
        private String currentDestination;
        private NLUResult.UrgencyLevel currentUrgency;
        private NLUResult.RoutePreference currentRoutePref;
        private boolean contextInitialized = false;
        
        public void updateContext(NLUResult result) {
            if (result.getDestination() != null) {
                this.currentDestination = result.getDestination();
            }
            if (result.getUrgencyLevel() != null) {
                this.currentUrgency = result.getUrgencyLevel();
            }
            if (result.getRoutePreference() != null) {
                this.currentRoutePref = result.getRoutePreference();
            }
            contextInitialized = true;
        }
        
        public String getCurrentDestination() {
            return currentDestination;
        }
        
        public NLUResult.UrgencyLevel getCurrentUrgency() {
            return currentUrgency;
        }
        
        public NLUResult.RoutePreference getCurrentRoutePref() {
            return currentRoutePref;
        }
        
        public boolean isContextInitialized() {
            return contextInitialized;
        }
    }
    
    /**
     * 创建对话上下文
     */
    public ConversationContext createConversationContext() {
        return new ConversationContext();
    }
    
    /**
     * 获取支持的示例语句
     */
    public static List<String> getExampleInputs() {
        List<String> examples = new ArrayList<>();
        
        // 基础导航
        examples.add("我要去西南大学");
        examples.add("导航到机场");
        examples.add("送我去火车站");
        
        // 带紧急程度
        examples.add("我要去机场，我着急");
        examples.add("到人民医院，尽快");
        examples.add("导航去市政府，马上要");
        
        // 带费用偏好
        examples.add("去西南大学，费用不是问题");
        examples.add("到机场，不计成本");
        examples.add("我要去商场，省点钱");
        
        // 完整场景
        examples.add("我要前往西南大学，我着急，费用不是问题");
        examples.add("去机场，越快越好，费用无所谓");
        examples.add("到火车站，尽快到，走高速");

        // 配送与协同
        examples.add("启动无人机配送任务到观音桥");
        examples.add("乘客说前方拥堵，请无人车联动后车和无人机给出路线建议");

        // 查询与控制
        examples.add("查询车辆和无人机当前状态");
        examples.add("我在解放碑，还要多久到江北机场");
        examples.add("请在观音桥附近停车并减速");
        
        // 特殊场景
        examples.add("我要去最近的充电站");
        examples.add("导航到最近的加油站，快没油了");
        examples.add("送我去机场，我还有两个人");
        
        return examples;
    }
    
    /**
     * 打印处理结果详细信息
     */
    public void printResultDetails(NLUResult result) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    NLU 处理结果详情                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        
        System.out.println("║ 原始输入: \"" + result.getOriginalText() + "\"");
        System.out.println("╠────────────────────────────────────────────────────────────────╣");
        
        System.out.printf("║ 主意图: %-52s ║\n", result.getPrimaryIntent());
        System.out.printf("║ 目的地: %-52s ║\n", 
            result.getDestination() != null ? result.getDestination() : "未识别");
        System.out.printf("║ 紧急程度: %-50s ║\n", result.getUrgencyLevel());
        System.out.printf("║ 费用偏好: %-50s ║\n", result.getCostPreference());
        System.out.printf("║ 路线偏好: %-50s ║\n", result.getRoutePreference());
        System.out.printf("║ 导航模式: %-50s ║\n", result.getNavigationMode());
        System.out.printf("║ 车辆ID: %-52s ║\n", result.getVehicleId());
        System.out.printf("║ 预估人数: %-51d ║\n", result.getEstimatedPassengers());
        System.out.printf("║ 置信度: %-52s ║\n", 
            String.format("%.1f%%", result.getConfidence() * 100));
        
        System.out.println("╠────────────────────────────────────────────────────────────────╣");
        System.out.println("║ 意图分数:                                                      ║");
        for (Map.Entry<Intent, Double> entry : result.getIntentScores().entrySet()) {
            System.out.printf("║   %-15s : %-45s ║\n", 
                entry.getKey(), 
                String.format("%.2f", entry.getValue() * 100) + "%");
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * 交互式模式 - 支持语音/键盘连续输入
     * 用于演示和轻量级部署
     */
    public void interactiveMode() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          NLU 交互式模式 - 支持语音/键盘输入                 ║");
        System.out.println("║          (输入'quit'退出, 'help'查看示例)                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        while (true) {
            String input = inputHandler.getInput();
            
            if (input == null || input.isEmpty()) {
                System.out.println("❌ 输入为空，请重试");
                continue;
            }
            
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("\n👋 再见！");
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                System.out.println("\n📋 支持的示例输入：");
                for (int i = 0; i < getExampleInputs().size(); i++) {
                    System.out.printf("  %d. %source\n", i + 1, getExampleInputs().get(i));
                }
                continue;
            }
            
            // 处理输入
            NLUResult result = processToResult(input);
            
            // 显示详细结果
            printResultDetails(result);
            
            // 生成DSL
            String dsl = dslGenerator.generateDSL(result);
            System.out.println("\n📝 生成的DSL代码：");
            System.out.println("```");
            System.out.println(dsl);
            System.out.println("```");
        }
    }
}
