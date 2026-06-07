package nlu;

import java.util.*;

/**
 * DSL验证器和优化器
 * 实现DSL代码的语法检查、语义验证和优化
 * 
 * 核心功能：
 * 1. 语法检查
 * 2. 语义合理性检查
 * 3. 资源约束验证
 * 4. 安全性检查
 * 5. DSL代码优化
 */
public class DSLValidator {
    
    // DSL保留关键字
    private static final Set<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList(
        "topic", "task", "path", "state", "behaviorTree",
        "sequence", "selector", "true", "false"
    ));
    
    // 有效导航模式
    private static final Set<String> VALID_NAVIGATION_MODES = new HashSet<>(Arrays.asList(
        "highway_fastest", "economy_route", "balanced_route", "scenic_route",
        "avoid_highway", "shortest", "pedestrian"
    ));

    // 支持的高层意图
    private static final Set<String> VALID_INTENTS = new HashSet<>(Arrays.asList(
        "NAVIGATION", "DELIVERY", "COORDINATION", "EMERGENCY", "QUERY_STATUS", "CONTROL",
        "COST_AWARE", "URGENCY_HIGH", "URGENCY_MEDIUM", "URGENCY_LOW"
    ));
    
    // 有效紧急程度
    private static final Set<String> VALID_URGENCY_LEVELS = new HashSet<>(Arrays.asList(
        "NORMAL", "LOW", "HIGH", "CRITICAL"
    ));
    
    /**
     * 验证DSL代码
     */
    public ValidationResult validate(String dslCode) {
        ValidationResult result = new ValidationResult();
        
        if (dslCode == null || dslCode.trim().isEmpty()) {
            result.addError("DSL代码为空");
            return result;
        }
        
        // 1. 语法检查
        SyntaxError syntaxError = checkSyntax(dslCode);
        if (syntaxError != null) {
            result.addError("语法错误：" + syntaxError.getMessage());
            result.setSyntaxError(syntaxError);
        }
        
        // 2. 语义合理性检查
        List<String> semanticErrors = checkSemantics(dslCode);
        result.addErrors(semanticErrors);
        
        // 3. 约束检查
        List<String> constraintErrors = checkConstraints(dslCode);
        result.addErrors(constraintErrors);
        
        // 4. 安全性检查
        List<String> securityWarnings = checkSecurity(dslCode);
        result.addWarnings(securityWarnings);
        
        return result;
    }
    
    /**
     * 语法检查
     */
    private SyntaxError checkSyntax(String dslCode) {
        // 检查括号匹配
        if (!areParenthesesBalanced(dslCode)) {
            return new SyntaxError("括号不匹配", -1);
        }
        
        // 检查引号匹配
        if (!areQuotesBalanced(dslCode)) {
            return new SyntaxError("引号不匹配", -1);
        }
        
        // 检查是否有未闭合的块
        if (!areBlocksBalanced(dslCode)) {
            return new SyntaxError("代码块未正确闭合", -1);
        }
        
        // 检查是否有重复命名声明
        if (hasDuplicateDeclarations(dslCode)) {
            return new SyntaxError("存在重复的声明", -1);
        }
        
        return null;
    }
    
    /**
     * 检查括号平衡
     */
    private boolean areParenthesesBalanced(String code) {
        int count = 0;
        for (char c : code.toCharArray()) {
            if (c == '(') count++;
            if (c == ')') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }
    
    /**
     * 检查引号平衡
     */
    private boolean areQuotesBalanced(String code) {
        int singleQuotes = 0;
        int doubleQuotes = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        
        for (char c : code.toCharArray()) {
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                singleQuotes++;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                doubleQuotes++;
            }
        }
        
        return singleQuotes % 2 == 0 && doubleQuotes % 2 == 0;
    }
    
    /**
     * 检查代码块平衡
     */
    private boolean areBlocksBalanced(String code) {
        int braces = 0;
        int brackets = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') braces++;
            if (c == '}') braces--;
            if (c == '[') brackets++;
            if (c == ']') brackets--;
        }
        
        return braces == 0 && brackets == 0;
    }
    
    /**
     * 检查重复声明
     */
    private boolean hasDuplicateDeclarations(String code) {
        // 简单的重复检测: task/path/state 名称不得重复
        String[] lines = code.split("\n");
        Set<String> declarations = new HashSet<>();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("task ") || line.startsWith("path ") || line.startsWith("state ")) {
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                String declaration = parts[0] + ":" + parts[1];
                if (!declarations.add(declaration)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 语义检查
     */
    private List<String> checkSemantics(String dslCode) {
        List<String> errors = new ArrayList<>();
        
        if (!dslCode.contains("topic ")) {
            errors.add("语义错误：缺少topic声明");
        }
        
        if (!dslCode.contains("task ")) {
            errors.add("语义错误：缺少task声明");
        }
        
        if (dslCode.contains("intent = \"") && !containsAnyIntent(dslCode)) {
            errors.add("语义错误：intent字段值不在支持列表中");
        }
        
        return errors;
    }
    
    /**
     * 约束检查
     */
    private List<String> checkConstraints(String dslCode) {
        List<String> errors = new ArrayList<>();
        
        // 检查导航模式是否合法
        String mode = extractQuotedValue(dslCode, "navigation_mode");
        if (mode != null && !mode.isBlank()) {
            boolean known = VALID_NAVIGATION_MODES.contains(mode);
            boolean custom = mode.contains("route") || mode.contains("adaptive");
            if (!known && !custom) {
                errors.add("约束错误：navigation_mode 非法: " + mode);
            }
        }

        // 检查紧急程度是否合法
        String urgency = extractQuotedValue(dslCode, "urgency");
        if (urgency != null && !VALID_URGENCY_LEVELS.contains(urgency)) {
            errors.add("约束错误：urgency 非法: " + urgency);
        }

        // 检查路径数据有效性
        if (dslCode.contains("estimated_distance_km = 0") || dslCode.contains("estimated_time_min = 0")) {
            errors.add("约束错误：路径估算不能为0");
        }

        // 检查关键保留字是否作为标识符误用
        for (String keyword : RESERVED_KEYWORDS) {
            if (dslCode.matches("(?s).*\\b(task|path|state)\\s+" + keyword + "\\b.*")) {
                errors.add("约束错误：保留关键字被用作声明名: " + keyword);
            }
        }
        
        return errors;
    }
    
    /**
     * 安全性检查
     */
    private List<String> checkSecurity(String dslCode) {
        List<String> warnings = new ArrayList<>();
        
        // 检查是否有潜在的恶意代码
        if (dslCode.contains("System.exit")) {
            warnings.add("警告：检测到系统退出调用");
        }
        
        if (dslCode.contains("Runtime.getRuntime")) {
            warnings.add("警告：检测到运行时调用");
        }
        
        if (dslCode.contains("ProcessBuilder")) {
            warnings.add("警告：检测到进程构建调用");
        }
        
        // 检查是否有硬编码的敏感信息
        if (dslCode.matches(".*password\\s*:\\s*\"[^\"]+\".*")) {
            warnings.add("警告：检测到可能的密码硬编码");
        }
        
        if (dslCode.matches(".*api_key\\s*:\\s*\"[^\"]+\".*")) {
            warnings.add("警告：检测到可能的API密钥硬编码");
        }
        
        return warnings;
    }
    
    /**
     * 优化DSL代码
     */
    public String optimize(String dslCode) {
        String optimized = dslCode;
        
        // 1. 移除空白行
        optimized = optimized.replaceAll("(?m)^\\s*$", "");
        
        // 2. 合并连续空格
        optimized = optimized.replaceAll("\\s+", " ");
        
        // 3. 移除注释
        optimized = optimized.replaceAll("//.*", "");
        optimized = optimized.replaceAll("/\\*.*?\\*/", "");
        
        // 4. 标准化换行
        optimized = optimized.replaceAll("\\runtime\\n", "\n");
        
        // 5. 移除末尾逗号
        optimized = optimized.replaceAll(",\\s*}", "}");
        
        return optimized.trim();
    }
    
    /**
     * 格式化DSL代码
     */
    public String format(String dslCode, int indentSize) {
        StringBuilder formatted = new StringBuilder();
        int indent = 0;
        
        for (char c : dslCode.toCharArray()) {
            if (c == '{') {
                formatted.append(" {\n");
                indent += indentSize;
                formatted.append(" ".repeat(indent));
            } else if (c == '}') {
                indent -= indentSize;
                formatted.append("\n");
                formatted.append(" ".repeat(indent));
                formatted.append("}");
            } else if (c == ';') {
                formatted.append(";\n");
                formatted.append(" ".repeat(indent));
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * 从NLUResult生成并验证DSL
     */
    public ValidationResult generateAndValidate(NLUResult nluResult) {
        DSLGenerator generator = new DSLGenerator();
        String dslCode = generator.generateDSL(nluResult);
        
        ValidationResult result = validate(dslCode);
        result.setGeneratedDSL(dslCode);
        
        return result;
    }

    private boolean containsAnyIntent(String dslCode) {
        for (String intent : VALID_INTENTS) {
            if (dslCode.contains("intent = \"" + intent + "\"")) {
                return true;
            }
        }
        return false;
    }

    private String extractQuotedValue(String code, String field) {
        String marker = field + " = \"";
        int start = code.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + marker.length();
        int valueEnd = code.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return code.substring(valueStart, valueEnd).trim();
    }
    
    /**
     * 语法错误类
     */
    public static class SyntaxError {
        private String message;
        private int line;
        private int column;
        
        public SyntaxError(String message, int line) {
            this.message = message;
            this.line = line;
        }
        
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private SyntaxError syntaxError;
        private String generatedDSL;
        
        public ValidationResult() {
            this.valid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }
        
        public boolean isValid() { 
            return valid && errors.isEmpty(); 
        }
        
        public void setValid(boolean valid) { 
            this.valid = valid; 
        }
        
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        
        public void addErrors(List<String> errors) {
            this.errors.addAll(errors);
            if (!errors.isEmpty()) {
                this.valid = false;
            }
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        
        public void addWarnings(List<String> warnings) {
            this.warnings.addAll(warnings);
        }
        
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public SyntaxError getSyntaxError() { return syntaxError; }
        public void setSyntaxError(SyntaxError syntaxError) { 
            this.syntaxError = syntaxError; 
        }
        public String getGeneratedDSL() { return generatedDSL; }
        public void setGeneratedDSL(String generatedDSL) { 
            this.generatedDSL = generatedDSL; 
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("验证结果：").append(isValid() ? "通过" : "失败").append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("错误：\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("警告：\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}
