package nlu;

import java.util.*;
import java.util.regex.*;

/**
 * 智能任务规划器 - 实现"意图拆解与任务规划"
 * 
 * 核心功能：
 * 1. 多步骤逻辑生成 - 将模糊高层目标拆解为具体执行步骤
 * 2. 缺省参数补全 - 自动补全缺失的TCL字段
 * 3. 任务依赖分析 - 分析任务间的依赖关系
 * 4. 执行顺序优化 - 优化任务执行顺序
 * 
 * 对应论文: 第3章 - 意图拆解与任务规划
 */
public class IntelligentTaskPlanner {
    
    /**
     * TCL任务步骤
     */
    public static class TaskStep {
        private String stepId;
        private String action;
        private String target;
        private Map<String, Object> parameters;
        private List<String> prerequisites;
        private List<String> postconditions;
        private int estimatedDuration;
        private double priority;
        
        public TaskStep(String stepId, String action, String target) {
            this.stepId = stepId;
            this.action = action;
            this.target = target;
            this.parameters = new HashMap<>();
            this.prerequisites = new ArrayList<>();
            this.postconditions = new ArrayList<>();
            this.priority = 1.0;
        }
        
        public String getStepId() { return stepId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public Map<String, Object> getParameters() { return parameters; }
        public void addParameter(String key, Object value) { this.parameters.put(key, value); }
        public List<String> getPrerequisites() { return prerequisites; }
        public void addPrerequisite(String prereq) { this.prerequisites.add(prereq); }
        public List<String> getPostconditions() { return postconditions; }
        public void addPostcondition(String post) { this.postconditions.add(post); }
        public int getEstimatedDuration() { return estimatedDuration; }
        public void setEstimatedDuration(int duration) { this.estimatedDuration = duration; }
        public double getPriority() { return priority; }
        public void setPriority(double priority) { this.priority = priority; }
        
        public String toTCL() {
            StringBuilder sb = new StringBuilder();
            sb.append("task ").append(stepId).append(" {\n");
            sb.append("    action: ").append(action).append("\n");
            sb.append("    target: ").append(target).append("\n");
            if (!parameters.isEmpty()) {
                sb.append("    parameters {\n");
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    sb.append("        ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                sb.append("    }\n");
            }
            if (!prerequisites.isEmpty()) {
                sb.append("    requires: [").append(String.join(", ", prerequisites)).append("]\n");
            }
            sb.append("    priority: ").append(String.format("%.1f", priority)).append("\n");
            sb.append("}");
            return sb.toString();
        }
    }
    
    /**
     * 完整任务规划
     */
    public static class TaskPlan {
        private String planId;
        private String originalIntent;
        private List<TaskStep> steps;
        private Map<String, Object> globalContext;
        private List<String> assumptions;
        private List<String> risks;
        private double feasibilityScore;
        private boolean validated;
        
        public TaskPlan(String planId, String originalIntent) {
            this.planId = planId;
            this.originalIntent = originalIntent;
            this.steps = new ArrayList<>();
            this.globalContext = new HashMap<>();
            this.assumptions = new ArrayList<>();
            this.risks = new ArrayList<>();
            this.feasibilityScore = 0.0;
            this.validated = false;
        }
        
        public void addStep(TaskStep step) { steps.add(step); }
        public void addAssumption(String assumption) { assumptions.add(assumption); }
        public void addRisk(String risk) { risks.add(risk); }
        
        public String generateTCL() {
            StringBuilder sb = new StringBuilder();
            sb.append("// 智能生成的任务规划 - ").append(planId).append("\n");
            sb.append("// 原始意图: ").append(originalIntent).append("\n");
            sb.append("// 可行性评分: ").append(String.format("%.1f%%", feasibilityScore * 100)).append("\n\n");
            
            if (!globalContext.isEmpty()) {
                sb.append("context {\n");
                for (Map.Entry<String, Object> entry : globalContext.entrySet()) {
                    sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                sb.append("}\n\n");
            }
            
            if (!assumptions.isEmpty()) {
                sb.append("assumptions {\n");
                for (String assumption : assumptions) {
                    sb.append("    - ").append(assumption).append("\n");
                }
                sb.append("}\n\n");
            }
            
            sb.append("mission ").append(planId).append(" {\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append("    // Step ").append(i + 1).append(": ").append(steps.get(i).getAction());
                sb.append(" -> ").append(steps.get(i).getTarget()).append("\n");
                sb.append("    ").append(steps.get(i).toTCL().replace("\n", "\n    "));
                if (i < steps.size() - 1) sb.append("\n\n");
            }
            sb.append("\n}\n");
            
            if (!risks.isEmpty()) {
                sb.append("\n// Risk Assessment:\n");
                for (String risk : risks) {
                    sb.append("// WARNING: ").append(risk).append("\n");
                }
            }
            return sb.toString();
        }
        
        public String getPlanId() { return planId; }
        public String getOriginalIntent() { return originalIntent; }
        public List<TaskStep> getSteps() { return steps; }
        public Map<String, Object> getGlobalContext() { return globalContext; }
        public List<String> getAssumptions() { return assumptions; }
        public List<String> getRisks() { return risks; }
        public double getFeasibilityScore() { return feasibilityScore; }
        public void setFeasibilityScore(double score) { this.feasibilityScore = score; }
        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }
    }
    
    private Map<String, ActionTemplate> actionTemplates;
    
    private static class ActionTemplate {
        String action;
        List<String> requiredParams;
        List<String> optionalParams;
        String tclEquivalent;
        int defaultDuration;
        
        ActionTemplate(String action, List<String> required, List<String> optional, String tcl, int duration) {
            this.action = action;
            this.requiredParams = required;
            this.optionalParams = optional;
            this.tclEquivalent = tcl;
            this.defaultDuration = duration;
        }
    }
    
    public IntelligentTaskPlanner() {
        initializeActionTemplates();
    }
    
    private void initializeActionTemplates() {
        actionTemplates = new HashMap<>();
        
        actionTemplates.put("前往", new ActionTemplate("navigate", Arrays.asList("destination"), Arrays.asList("speed", "altitude", "route_preference"), "NAVIGATE_TO", 60));
        actionTemplates.put("去", new ActionTemplate("navigate", Arrays.asList("destination"), Arrays.asList("speed", "altitude"), "NAVIGATE_TO", 60));
        actionTemplates.put("到达", new ActionTemplate("arrive", Arrays.asList("destination"), Arrays.asList("arrival_action"), "ARRIVE_AT", 10));
        actionTemplates.put("绕过", new ActionTemplate("avoid", Arrays.asList("obstacle"), Arrays.asList("detour_distance", "alternative_route"), "ROUTE_AVOID", 30));
        actionTemplates.put("避开", new ActionTemplate("avoid", Arrays.asList("hazard"), Arrays.asList("safe_distance"), "AVOID_HAZARD", 20));
        actionTemplates.put("加速", new ActionTemplate("accelerate", Arrays.asList("target_speed"), Arrays.asList("max_speed", "acceleration_rate"), "SET_SPEED", 5));
        actionTemplates.put("减速", new ActionTemplate("decelerate", Arrays.asList("target_speed"), Arrays.asList("min_speed", "deceleration_rate"), "REDUCE_SPEED", 5));
        actionTemplates.put("救援", new ActionTemplate("rescue", Arrays.asList("target_location"), Arrays.asList("priority", "equipment", "personnel"), "EMERGENCY_RESCUE", 300));
        actionTemplates.put("配送", new ActionTemplate("deliver", Arrays.asList("destination", "cargo"), Arrays.asList("time_window", "signature_required"), "DELIVERY_MISSION", 180));
        actionTemplates.put("侦察", new ActionTemplate("surveil", Arrays.asList("area"), Arrays.asList("duration", "resolution", "live_feed"), "SURVEILLANCE", 600));
        actionTemplates.put("爬升", new ActionTemplate("climb", Arrays.asList("target_altitude"), Arrays.asList("climb_rate"), "CHANGE_ALTITUDE", 30));
        actionTemplates.put("下降", new ActionTemplate("descend", Arrays.asList("target_altitude"), Arrays.asList("descent_rate"), "CHANGE_ALTITUDE", 30));
        actionTemplates.put("降低高度", new ActionTemplate("descend", Arrays.asList("target_altitude"), Arrays.asList("descent_rate"), "CHANGE_ALTITUDE", 30));
    }
    
    /**
     * 从自然语言意图生成任务规划
     */
    public TaskPlan generateTaskPlan(String userIntent, Map<String, Object> context) {
        System.out.println("[IntelligentTaskPlanner] Parsing intent: " + userIntent);
        
        TaskPlan plan = new TaskPlan("PLAN-" + System.currentTimeMillis(), userIntent);
        
        List<String> phrases = tokenizeIntent(userIntent);
        System.out.println("[IntelligentTaskPlanner] Tokens: " + phrases);
        
        List<String> actions = identifyActions(phrases);
        System.out.println("[IntelligentTaskPlanner] Actions: " + actions);
        
        List<String> constraints = identifyConstraints(phrases);
        System.out.println("[IntelligentTaskPlanner] Constraints: " + constraints);
        
        int stepIndex = 1;
        for (String actionPhrase : actions) {
            TaskStep step = createTaskStep(stepIndex++, actionPhrase, constraints, context);
            if (step != null) {
                plan.addStep(step);
            }
        }
        
        fillDefaultParameters(plan, context);
        setGlobalContext(plan, context);
        evaluateFeasibility(plan);
        addAssumptions(plan, context);
        assessRisks(plan, constraints);
        
        return plan;
    }
    
    private List<String> tokenizeIntent(String intent) {
        List<String> tokens = new ArrayList<>();
        String[] words = intent.split("[\\s，。、,;.!?]+");
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                tokens.add(word.trim());
            }
        }
        return tokens;
    }
    
    private List<String> identifyActions(List<String> tokens) {
        List<String> actions = new ArrayList<>();
        StringBuilder currentAction = new StringBuilder();
        
        for (String token : tokens) {
            boolean isAction = false;
            for (String actionKey : actionTemplates.keySet()) {
                if (token.contains(actionKey) || actionKey.contains(token)) {
                    if (currentAction.length() > 0) {
                        currentAction.append(" ").append(token);
                    } else {
                        currentAction.append(token);
                    }
                    isAction = true;
                    break;
                }
            }
            
            if (!isAction && currentAction.length() > 0) {
                if (isLocationWord(token)) {
                    currentAction.append(" ").append(token);
                    actions.add(currentAction.toString());
                    currentAction = new StringBuilder();
                }
            }
        }
        
        if (currentAction.length() > 0) {
            actions.add(currentAction.toString());
        }
        
        if (actions.isEmpty() && !tokens.isEmpty()) {
            String inferredAction = tokens.get(0);
            if (tokens.size() > 1) {
                inferredAction += " " + tokens.get(1);
            }
            actions.add(inferredAction);
        }
        
        return actions;
    }
    
    private boolean isLocationWord(String word) {
        String[] locationMarkers = {"地", "点", "站", "区", "位置", "处", "中心", "站点"};
        for (String marker : locationMarkers) {
            if (word.contains(marker)) return true;
        }
        return word.matches("[A-Z]地|[A-Z]点|[A-Z]站");
    }
    
    private List<String> identifyConstraints(List<String> tokens) {
        List<String> constraints = new ArrayList<>();
        
        for (String token : tokens) {
            if (token.contains("最快") || token.contains("最快速") || token.contains("高速")) {
                constraints.add("SPEED_MAX");
            }
            if (token.contains("最慢") || token.contains("低速") || token.contains("慢速")) {
                constraints.add("SPEED_MIN");
            }
            if (token.contains("尽快") || token.contains("紧急") || token.contains("速")) {
                constraints.add("TIME_URGENT");
            }
            if (token.contains("安全") || token.contains("稳妥")) {
                constraints.add("SAFETY_FIRST");
            }
            Pattern distPattern = Pattern.compile("(\\d+)\\s*(公里|km|米|m)");
            Matcher distMatcher = distPattern.matcher(token);
            if (distMatcher.find()) {
                constraints.add("DISTANCE:" + distMatcher.group(1) + distMatcher.group(2));
            }
        }
        return constraints;
    }
    
    private TaskStep createTaskStep(int index, String actionPhrase, List<String> constraints, Map<String, Object> context) {
        for (Map.Entry<String, ActionTemplate> entry : actionTemplates.entrySet()) {
            if (actionPhrase.contains(entry.getKey())) {
                String action = entry.getValue().action;
                ActionTemplate template = entry.getValue();
                
                TaskStep step = new TaskStep("STEP_" + index, action, "auto_detected");
                step.addParameter("tcl_action", template.tclEquivalent);
                step.setEstimatedDuration(template.defaultDuration);
                
                if (context.containsKey("destination")) {
                    step.setTarget(context.get("destination").toString());
                } else {
                    String target = extractTargetFromPhrase(actionPhrase);
                    step.setTarget(target != null ? target : "UNKNOWN_DESTINATION");
                }
                
                applyConstraints(step, constraints);
                return step;
            }
        }
        
        TaskStep step = new TaskStep("STEP_" + index, "navigate", "auto_detected");
        step.addParameter("tcl_action", "NAVIGATE_TO");
        step.setEstimatedDuration(60);
        
        if (context.containsKey("destination")) {
            step.setTarget(context.get("destination").toString());
        }
        return step;
    }
    
    private String extractTargetFromPhrase(String phrase) {
        Pattern pattern = Pattern.compile("[A-Z][\\u4e00-\\u9fa5]+|[\\u4e00-\\u9fa5]+[地站点区]");
        Matcher matcher = pattern.matcher(phrase);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group();
        }
        return lastMatch;
    }
    
    private void applyConstraints(TaskStep step, List<String> constraints) {
        for (String constraint : constraints) {
            if (constraint.startsWith("SPEED_")) {
                if (constraint.equals("SPEED_MAX")) {
                    step.addParameter("max_speed", "30");
                    step.addPostcondition("speed_constraint: max_safe");
                } else if (constraint.equals("SPEED_MIN")) {
                    step.addParameter("min_speed", "5");
                }
            }
            if (constraint.equals("TIME_URGENT")) {
                step.setPriority(2.0);
                step.addPostcondition("time_critical: true");
            }
            if (constraint.equals("SAFETY_FIRST")) {
                step.addParameter("safety_mode", "strict");
            }
        }
    }
    
    private void fillDefaultParameters(TaskPlan plan, Map<String, Object> context) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("cost_optimization", context.containsKey("cost_preference") ? context.get("cost_preference") : "balanced");
        defaults.put("urgency_level", context.containsKey("urgency") ? context.get("urgency") : "normal");
        if (context.containsKey("battery_level")) defaults.put("min_battery_threshold", "20%");
        defaults.put("preferred_device", context.containsKey("device_type") ? context.get("device_type") : "auto_select");
        plan.getGlobalContext().putAll(defaults);
    }
    
    private void setGlobalContext(TaskPlan plan, Map<String, Object> context) {
        if (context.containsKey("user_preferences")) plan.getGlobalContext().put("user_preferences", context.get("user_preferences"));
        if (context.containsKey("current_location")) plan.getGlobalContext().put("start_position", context.get("current_location"));
        if (context.containsKey("weather")) plan.getGlobalContext().put("weather_conditions", context.get("weather"));
    }
    
    private void evaluateFeasibility(TaskPlan plan) {
        double score = 1.0;
        List<String> issues = new ArrayList<>();
        
        boolean hasUnknownTarget = false;
        for (TaskStep step : plan.getSteps()) {
            if ("UNKNOWN_DESTINATION".equals(step.getTarget())) {
                hasUnknownTarget = true;
                score -= 0.3;
                issues.add("目的地不明确");
            }
        }
        
        int totalDuration = 0;
        for (TaskStep step : plan.getSteps()) {
            totalDuration += step.getEstimatedDuration();
        }
        
        if (totalDuration > 3600) {
            score -= 0.2;
            issues.add("任务预计时长较长: " + (totalDuration / 60) + "分钟");
        }
        
        for (TaskStep step : plan.getSteps()) {
            if (step.getPriority() > 1.5) {
                score -= 0.1;
                issues.add("存在紧急任务，可能需要额外资源");
            }
        }
        
        plan.setFeasibilityScore(Math.max(0, score));
        
        for (String issue : issues) {
            plan.addAssumption("Note: " + issue);
        }
    }
    
    private void addAssumptions(TaskPlan plan, Map<String, Object> context) {
        plan.addAssumption("Weather conditions suitable for operation");
        plan.addAssumption("Compliant backbone link available when approved and configured");
        plan.addAssumption("Device battery sufficient (>50%)");
        for (TaskStep step : plan.getSteps()) {
            if (!"UNKNOWN_DESTINATION".equals(step.getTarget())) {
                plan.addAssumption("Destination " + step.getTarget() + " within range");
            }
        }
    }
    
    private void assessRisks(TaskPlan plan, List<String> constraints) {
        for (String constraint : constraints) {
            if (constraint.equals("SPEED_MAX")) {
                plan.addRisk("High speed may exceed device physical limits, recommend safe speed");
            }
        }
        for (String constraint : constraints) {
            if (constraint.equals("TIME_URGENT")) {
                plan.addRisk("Urgent task may increase safety risk, recommend priority evaluation");
            }
        }
        for (String constraint : constraints) {
            if (constraint.startsWith("DISTANCE:")) {
                String dist = constraint.split(":")[1];
                plan.addRisk("Long distance task (" + dist + ") may exceed single-flight range");
            }
        }
        for (TaskStep step : plan.getSteps()) {
            if ("UNKNOWN_DESTINATION".equals(step.getTarget())) {
                plan.addRisk("Destination not specified, cannot generate valid path");
            }
        }
    }
    
    /**
     * 补全缺失参数
     */
    public Map<String, Object> fillMissingParameters(String partialIntent, Map<String, Object> knownParams) {
        Map<String, Object> completeParams = new HashMap<>(knownParams);
        if (!completeParams.containsKey("destination")) completeParams.put("destination", "DEFAULT_WAYPOINT");
        if (!completeParams.containsKey("urgency")) completeParams.put("urgency", "normal");
        if (!completeParams.containsKey("speed_preference")) completeParams.put("speed_preference", "balanced");
        if (!completeParams.containsKey("device_type")) completeParams.put("device_type", "auto_select");
        return completeParams;
    }
}
