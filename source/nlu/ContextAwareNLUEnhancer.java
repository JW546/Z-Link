package nlu;

import java.util.*;
import java.util.regex.*;

/**
 * 上下文感知NLU增强器
 * 实现"上下文感知能力"和"多轮交互纠偏"
 * 
 * 核心功能：
 * 1. 动态环境约束 - 接入合规广域链路/DDS实时数据
 * 2. 多轮交互纠偏 - 反问机制处理歧义
 * 3. 上下文记忆 - 维护对话历史
 * 4. 品牌中立抽象 - 异构设备语义对齐
 * 
 * 对应论文: 第3章 - 增强上下文感知能力
 */
public class ContextAwareNLUEnhancer {
    
    /**
     * 环境感知数据
     */
    public static class EnvironmentContext {
        private String weather;           // 天气状况
        private double visibility;        // 能见度(m)
        private double windSpeed;         // 风速(m/s)
        private double temperature;       // 温度(C)
        private String trafficCondition;  // 交通状况
        private String noFlyZoneStatus;  // 禁飞区状态
        private double batteryLevel;      // 电池电量(%)
        private int signalStrength;       // 信号强度(0-100)
        private long timestamp;           // 数据时间戳
        
        // 构造方法
        public EnvironmentContext() {
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getWeather() { return weather; }
        public void setWeather(String weather) { this.weather = weather; }
        public double getVisibility() { return visibility; }
        public void setVisibility(double visibility) { this.visibility = visibility; }
        public double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public String getTrafficCondition() { return trafficCondition; }
        public void setTrafficCondition(String trafficCondition) { this.trafficCondition = trafficCondition; }
        public String getNoFlyZoneStatus() { return noFlyZoneStatus; }
        public void setNoFlyZoneStatus(String noFlyZoneStatus) { this.noFlyZoneStatus = noFlyZoneStatus; }
        public double getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }
        public int getSignalStrength() { return signalStrength; }
        public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 歧义问题
     */
    public static class AmbiguityCase {
        private String ambiguityType;     // 歧义类型
        private String question;           // 反问问题
        private List<String> options;      // 选项列表
        private String context;            // 上下文信息
        private Map<String, Object> hints; // 提示信息
        
        public AmbiguityCase(String type, String question) {
            this.ambiguityType = type;
            this.question = question;
            this.options = new ArrayList<>();
            this.hints = new HashMap<>();
        }
        
        public void addOption(String option) { options.add(option); }
        public void addHint(String key, Object value) { hints.put(key, value); }
        
        public String getAmbiguityType() { return ambiguityType; }
        public String getQuestion() { return question; }
        public List<String> getOptions() { return options; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public Map<String, Object> getHints() { return hints; }
    }
    
    /**
     * 解析结果增强
     */
    public static class EnhancedInterpretation {
        private String originalText;       // 原始文本
        private String interpretedIntent;  // 解释后的意图
        private Map<String, Object> parameters; // 参数
        private List<String> constraints;  // 约束条件
        private List<String> warnings;     // 警告
        private List<AmbiguityCase> ambiguities; // 歧义列表
        private EnvironmentContext envContext; // 环境上下文
        private double confidence;         // 置信度
        private boolean requiresConfirmation; // 需要确认
        
        public EnhancedInterpretation(String text) {
            this.originalText = text;
            this.parameters = new HashMap<>();
            this.constraints = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.ambiguities = new ArrayList<>();
            this.confidence = 1.0;
        }
        
        public void addAmbiguity(AmbiguityCase ambiguity) { ambiguities.add(ambiguity); requiresConfirmation = true; }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addConstraint(String constraint) { constraints.add(constraint); }
        
        public String getOriginalText() { return originalText; }
        public String getInterpretedIntent() { return interpretedIntent; }
        public void setInterpretedIntent(String intent) { this.interpretedIntent = intent; }
        public Map<String, Object> getParameters() { return parameters; }
        public List<String> getConstraints() { return constraints; }
        public List<String> getWarnings() { return warnings; }
        public List<AmbiguityCase> getAmbiguities() { return ambiguities; }
        public EnvironmentContext getEnvContext() { return envContext; }
        public void setEnvContext(EnvironmentContext ctx) { this.envContext = ctx; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double conf) { this.confidence = conf; }
        public boolean requiresConfirmation() { return requiresConfirmation; }
    }
    
    // 对话历史
    private List<EnhancedInterpretation> dialogueHistory;
    private Map<String, Object> userPreferences;
    private EnvironmentContext currentEnvironment;
    
    // 合规广域态势数据接入（当前复用 legacy 适配器的数据结构）。
    private boolean backboneConnected;
    private protocol.CompliantBackboneAdapter backboneAdapter;
    
    public ContextAwareNLUEnhancer() {
        this.dialogueHistory = new ArrayList<>();
        this.userPreferences = new HashMap<>();
        this.currentEnvironment = new EnvironmentContext();
        this.backboneConnected = false;
        
        // 设置默认环境
        setDefaultEnvironment();
    }
    
    /**
     * 设置默认环境（模拟数据）
     */
    private void setDefaultEnvironment() {
        currentEnvironment.setWeather("Cloudy");
        currentEnvironment.setVisibility(450); // 能见度450米
        currentEnvironment.setWindSpeed(3.5);
        currentEnvironment.setTemperature(18);
        currentEnvironment.setTrafficCondition("congested");
        currentEnvironment.setNoFlyZoneStatus("clear");
        currentEnvironment.setBatteryLevel(75);
        currentEnvironment.setSignalStrength(85);
    }
    
    /**
     * 接入合规广域实时环境数据。
     */
    public void connectCompliantBackbone() {
        try {
            backboneAdapter = protocol.CompliantBackboneAdapter.createLEOAdapter("NLU-ENHANCER");
            if (backboneAdapter.connect()) {
                backboneConnected = true;
                System.out.println("[ContextAwareNLU] Compliant backbone connected for real-time environment data");
                
                // 获取全球态势数据
                protocol.CompliantBackboneAdapter.GlobalPerceptionData perceptionData = backboneAdapter.receiveGlobalPerceptionData();
                if (perceptionData != null) {
                    updateEnvironmentFromBackbone(perceptionData);
                }
            }
        } catch (Exception e) {
            System.out.println("[ContextAwareNLU] Compliant backbone connection failed: " + e.getMessage());
            backboneConnected = false;
        }
    }

    /**
     * 兼容旧调用入口；比赛/论文默认应使用 connectCompliantBackbone。
     */
    public void connectCOMPLIANT_BACKBONE() {
        connectCompliantBackbone();
    }
    
    /**
     * 从合规广域态势数据更新环境信息
     */
    private void updateEnvironmentFromBackbone(protocol.CompliantBackboneAdapter.GlobalPerceptionData data) {
        if (data.getPerceptionType().equals("weather")) {
            // 解析天气数据
            String content = data.getContent();
            if (content.contains("visibility")) {
                Pattern p = Pattern.compile("\"visibility\":(\\d+)");
                Matcher m = p.matcher(content);
                if (m.find()) {
                    currentEnvironment.setVisibility(Double.parseDouble(m.group(1)));
                }
            }
            if (content.contains("fog_density")) {
                Pattern p = Pattern.compile("\"fog_density\":([\\d.]+)");
                Matcher m = p.matcher(content);
                if (m.find()) {
                    double fogDensity = Double.parseDouble(m.group(1));
                    // 根据雾密度调整能见度
                    currentEnvironment.setVisibility(1000 * (1 - fogDensity));
                }
            }
        }
        System.out.println("[ContextAwareNLU] Environment updated: visibility=" + currentEnvironment.getVisibility() + "m");
    }
    
    /**
     * 处理自然语言输入
     */
    public EnhancedInterpretation process(String userInput) {
        EnhancedInterpretation result = new EnhancedInterpretation(userInput);
        
        // 1. 意图识别
        result.setInterpretedIntent(recognizeIntent(userInput));
        
        // 2. 参数提取
        extractParameters(userInput, result);
        
        // 3. 环境约束应用
        applyEnvironmentConstraints(result);
        
        // 4. 歧义检测
        detectAmbiguities(userInput, result);
        
        // 5. 品牌中立化
        neutralizeBrandSpecifics(result);
        
        // 6. 计算置信度
        calculateConfidence(result);
        
        // 7. 保存到历史
        dialogueHistory.add(result);
        
        return result;
    }
    
    /**
     * 意图识别
     */
    private String recognizeIntent(String input) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("救援") || lowerInput.contains("rescue") || lowerInput.contains("emergency")) {
            return "EMERGENCY_RESCUE";
        }
        if (lowerInput.contains("配送") || lowerInput.contains("delivery") || lowerInput.contains("送")) {
            return "DELIVERY";
        }
        if (lowerInput.contains("侦察") || lowerInput.contains("surveil") || lowerInput.contains("巡")) {
            return "SURVEILLANCE";
        }
        if (lowerInput.contains("去") || lowerInput.contains("前往") || lowerInput.contains("go") || lowerInput.contains("导航")) {
            return "NAVIGATION";
        }
        if (lowerInput.contains("绕过") || lowerInput.contains("avoid") || lowerInput.contains("避")) {
            return "ROUTE_AVOID";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 参数提取
     */
    private void extractParameters(String input, EnhancedInterpretation result) {
        Map<String, Object> params = result.getParameters();
        
        // 提取目的地
        Pattern destPattern = Pattern.compile("[A-Z][\\u4e00-\\u9fa5]+|[\\u4e00-\\u9fa5]+[地站点区]");
        Matcher destMatcher = destPattern.matcher(input);
        if (destMatcher.find()) {
            params.put("destination", destMatcher.group());
        }
        
        // 提取速度要求
        if (input.contains("最快") || input.contains("最快速")) {
            params.put("speed_mode", "max");
            result.addConstraint("MAX_SPEED_RESTRICTION");
        }
        if (input.contains("低速") || input.contains("慢速")) {
            params.put("speed_mode", "min");
        }
        
        // 提取紧急程度
        if (input.contains("紧急") || input.contains("尽快") || input.contains("马上")) {
            params.put("urgency", "high");
            result.addConstraint("URGENT_TIME_CONSTRAINT");
        }
        
        // 提取设备类型
        if (input.contains("无人机") || input.contains("drone")) {
            params.put("device_type", "drone");
        } else if (input.contains("车") || input.contains("vehicle")) {
            params.put("device_type", "vehicle");
        } else {
            params.put("device_type", "auto_select");
        }
    }
    
    /**
     * 应用环境约束
     */
    private void applyEnvironmentConstraints(EnhancedInterpretation result) {
        // 天气约束
        if (currentEnvironment.getVisibility() < 500) {
            result.addWarning("Low visibility (" + currentEnvironment.getVisibility() + "m), recommend reduced speed");
            result.addConstraint("VISIBILITY_CONSTRAINT");
            
            // 如果用户要求"最快速度"，自动添加安全限制
            if (result.getParameters().containsKey("speed_mode") && 
                "max".equals(result.getParameters().get("speed_mode"))) {
                result.addWarning("Speed limit applied due to low visibility: max 10 m/s");
                result.getParameters().put("max_speed_override", 10);
            }
        }
        
        // 雾天约束
        if (currentEnvironment.getVisibility() < 300) {
            result.addWarning("Heavy fog detected, maximum altitude restriction may apply");
            result.addConstraint("FOG_OPERATION_LIMIT");
        }
        
        // 交通约束
        if ("congested".equals(currentEnvironment.getTrafficCondition())) {
            if (result.getInterpretedIntent().equals("NAVIGATION")) {
                result.addWarning("Traffic congestion detected on route, ETA increased");
                result.addConstraint("TRAFFIC_DELAY_EXPECTED");
            }
        }
        
        // 电池约束
        if (currentEnvironment.getBatteryLevel() < 30) {
            result.addWarning("Low battery (" + currentEnvironment.getBatteryLevel() + "%), consider nearby charging");
            result.addConstraint("BATTERY_LIMIT");
        }
        
        // 禁飞区检查
        if (!"clear".equals(currentEnvironment.getNoFlyZoneStatus())) {
            result.addWarning("No-fly zone active: " + currentEnvironment.getNoFlyZoneStatus());
            result.addConstraint("NO_FLY_ZONE_RESTRICTION");
        }
    }
    
    /**
     * 歧义检测
     */
    private void detectAmbiguities(String input, EnhancedInterpretation result) {
        // 检测多设备歧义
        if (containsMultipleDevices(input)) {
            AmbiguityCase ambiguity = new AmbiguityCase("MULTI_DEVICE", 
                "检测到多个设备，请确认操作对象：");
            ambiguity.addOption("无人机 (drone)");
            ambiguity.addOption("车辆 (vehicle)");
            ambiguity.addOption("两者都执行");
            ambiguity.addHint("detected_devices", detectDevices(input));
            result.addAmbiguity(ambiguity);
        }
        
        // 检测目的地歧义
        if (result.getParameters().containsKey("destination")) {
            String dest = (String) result.getParameters().get("destination");
            if (isAmbiguousLocation(dest)) {
                AmbiguityCase ambiguity = new AmbiguityCase("AMBIGUOUS_DESTINATION",
                    "检测到模糊目的地 '" + dest + "'，请确认具体位置：");
                for (String option : getSimilarLocations(dest)) {
                    ambiguity.addOption(option);
                }
                result.addAmbiguity(ambiguity);
            }
        }
        
        // 检测时间歧义
        if (input.contains("尽快")) {
            AmbiguityCase ambiguity = new AmbiguityCase("URGENT_LEVEL",
                "请确认紧急程度：");
            ambiguity.addOption("立即执行 (最高优先级)");
            ambiguity.addOption("优先执行 (30分钟内)");
            ambiguity.addOption("尽快执行 (1小时内)");
            result.addAmbiguity(ambiguity);
        }
    }
    
    /**
     * 检测多个设备
     */
    private boolean containsMultipleDevices(String input) {
        boolean hasDrone = input.contains("无人机") || input.contains("drone") || input.contains("飞机");
        boolean hasVehicle = input.contains("车") || input.contains("车辆") || input.contains("car") || input.contains("vehicle");
        return hasDrone && hasVehicle;
    }
    
    /**
     * 识别设备类型
     */
    private List<String> detectDevices(String input) {
        List<String> devices = new ArrayList<>();
        if (input.contains("无人机") || input.contains("drone")) devices.add("drone");
        if (input.contains("车") || input.contains("车辆") || input.contains("vehicle")) devices.add("vehicle");
        return devices;
    }
    
    /**
     * 检查是否为模糊位置
     */
    private boolean isAmbiguousLocation(String location) {
        // 简单实现：单字符目的地视为模糊
        return location != null && location.length() <= 2;
    }
    
    /**
     * 获取相似位置列表
     */
    private List<String> getSimilarLocations(String ambiguous) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("A地 (A区应急中心)");
        suggestions.add("B地 (B区配送站点)");
        suggestions.add("C地 (C区医疗点)");
        return suggestions;
    }
    
    /**
     * 品牌中立化
     */
    private void neutralizeBrandSpecifics(EnhancedInterpretation result) {
        Map<String, Object> params = result.getParameters();
        
        // 将具体品牌动作映射到标准TCL动作
        // "起飞" -> TAKEOFF (品牌中立)
        // "降落" -> LAND
        // "悬停" -> HOVER
        // "加速" -> ACCELERATE
        
        // 设备特定术语标准化
        if (result.getOriginalText().contains("起飞")) {
            params.put("standard_action", "TAKEOFF");
        }
        if (result.getOriginalText().contains("降落") || result.getOriginalText().contains("下降落地")) {
            params.put("standard_action", "LAND");
        }
        if (result.getOriginalText().contains("悬停")) {
            params.put("standard_action", "HOVER");
        }
    }
    
    /**
     * 计算置信度
     */
    private void calculateConfidence(EnhancedInterpretation result) {
        double conf = 1.0;
        
        // 歧义降低置信度
        conf -= result.getAmbiguities().size() * 0.2;
        
        // 警告降低置信度
        conf -= result.getWarnings().size() * 0.05;
        
        // 约束越多，置信度越低
        conf -= result.getConstraints().size() * 0.03;
        
        result.setConfidence(Math.max(0, conf));
    }
    
    /**
     * 生成反问消息
     */
    public String generateClarificationQuestion(AmbiguityCase ambiguity) {
        StringBuilder question = new StringBuilder();
        question.append(ambiguity.getQuestion()).append("\n");
        
        for (int i = 0; i < ambiguity.getOptions().size(); i++) {
            question.append("  ").append(i + 1).append(". ").append(ambiguity.getOptions().get(i)).append("\n");
        }
        
        return question.toString();
    }
    
    /**
     * 处理用户确认
     */
    public void processConfirmation(String selectedOption) {
        if (!dialogueHistory.isEmpty()) {
            EnhancedInterpretation last = dialogueHistory.get(dialogueHistory.size() - 1);
            last.getParameters().put("confirmed_option", selectedOption);
            
            // 清除歧义标记
            last.getAmbiguities().clear();
            last.setConfidence(Math.min(1.0, last.getConfidence() + 0.3));
        }
    }
    
    /**
     * 获取对话历史
     */
    public List<EnhancedInterpretation> getDialogueHistory() {
        return dialogueHistory;
    }
    
    /**
     * 设置用户偏好
     */
    public void setUserPreference(String key, Object value) {
        userPreferences.put(key, value);
    }
    
    /**
     * 获取用户偏好
     */
    public Object getUserPreference(String key) {
        return userPreferences.get(key);
    }
    
    /**
     * 更新环境数据
     */
    public void updateEnvironment(EnvironmentContext env) {
        this.currentEnvironment = env;
    }
    
    /**
     * 获取当前环境
     */
    public EnvironmentContext getCurrentEnvironment() {
        return currentEnvironment;
    }
}
