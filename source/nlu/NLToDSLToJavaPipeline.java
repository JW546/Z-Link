package nlu;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 鑷劧璇█ 鈫?DSL 鈫?Java 瀹屾暣娴佺▼锛堝鎰忓浘锛?
 * 
 * 娴佺▼锛?
 * 1. 鑷劧璇█杈撳叆 -> NLU瑙ｆ瀽
 * 2. NLU缁撴灉 -> DSL浠ｇ爜鐢熸垚
 * 3. DSL浠ｇ爜 -> 淇濆瓨涓?dsl鏂囦欢
 * 4. .dsl鏂囦欢 -> 缂栬瘧涓篔ava浠ｇ爜
 * 
 * @paper_ref 绗?绔?鑷劧璇█鍒癉SL杞崲寮曟搸
 */
public class NLToDSLToJavaPipeline {
    
    private static final String DSL_OUTPUT_DIR = "dsl-output";
    private static final String JAVA_OUTPUT_DIR = "generated-java";
    
    private NLUProcessor nlu;
    private DSLGenerator dslGen;
    
    public NLToDSLToJavaPipeline() {
        this.nlu = new NLUProcessor();
        this.dslGen = new DSLGenerator();
    }
    
    /**
     * 鎵ц瀹屾暣娴佺▼
     */
    public Result execute(String naturalLanguage, String outputName) {
        Result result = new Result();
        result.naturalLanguage = naturalLanguage;
        result.startTime = System.currentTimeMillis();
        
        try {
            // Step 1: 鑷劧璇█ -> NLU
            System.out.println("\n[Step 1] 鑷劧璇█瑙ｆ瀽");
            NLUResult nluResult = nlu.processToResult(naturalLanguage);
            result.nluResult = nluResult;
            System.out.println("  鎰忓浘: " + nluResult.getPrimaryIntent());
            System.out.println("  鐩殑鍦? " + nluResult.getDestination());
            System.out.println("  缃俊搴? " + String.format("%.1f%%", nluResult.getConfidence() * 100));
            
            // Step 2: NLU -> DSL
            System.out.println("\n[Step 2] 鐢熸垚DSL浠ｇ爜");
            String dslCode = dslGen.generateDSL(nluResult);
            result.dslCode = dslCode;
            
            // Step 3: 淇濆瓨DSL鏂囦欢
            System.out.println("\n[Step 3] 淇濆瓨DSL鏂囦欢");
            String dslFile = saveDSLFile(dslCode, outputName);
            result.dslFilePath = dslFile;
            System.out.println("  淇濆瓨鑷? " + dslFile);
            
            // Step 4: DSL -> Java
            System.out.println("\n[Step 4] 缂栬瘧涓篔ava浠ｇ爜");
            String javaFile = compileDSLToJava(dslFile, outputName);
            result.javaFilePath = javaFile;
            System.out.println("  鐢熸垚: " + javaFile);
            
            result.success = true;
            result.endTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            result.error = e.getMessage();
            result.success = false;
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 浜や簰寮忔ā寮?
     */
    public void interactive() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== 鑷劧璇█ -> DSL -> Java 杞崲宸ュ叿锛堝鎰忓浘锛?===");
        System.out.println("Input natural language, then convert it to DSL and Java.");
        System.out.println("杈撳叆 'exit' 閫€鍑篭n");
        
        int count = 1;
        while (true) {
            System.out.print("璇疯緭鍏ヤ换鍔℃寚浠? ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("鍐嶈!");
                break;
            }
            
            if (input.isEmpty()) {
                System.out.println("璇疯緭鍏ユ湁鏁堝唴瀹筡n");
                continue;
            }
            
            String outputName = "intent_task_" + count++;
            Result result = execute(input, outputName);
            
            if (result.success) {
                printSummary(result);
            } else {
                System.out.println("\n杞崲澶辫触: " + result.error);
            }
            
            System.out.println();
        }
        
        scanner.close();
    }
    
    /**
     * 淇濆瓨DSL鏂囦欢
     */
    private String saveDSLFile(String dslCode, String name) throws IOException {
        // 鍒涘缓杈撳嚭鐩綍
        Files.createDirectories(Paths.get(DSL_OUTPUT_DIR));
        
        // 鐢熸垚鏂囦欢鍚?
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = name + "_" + timestamp + ".dsl";
        String filePath = DSL_OUTPUT_DIR + "/" + fileName;
        
        // 鍐欏叆鏂囦欢
        Files.writeString(Paths.get(filePath), dslCode, StandardCharsets.UTF_8);
        
        return filePath;
    }
    
    /**
     * 灏咲SL缂栬瘧涓篔ava
     */
    private String compileDSLToJava(String dslFile, String name) throws IOException {
        // 鍒涘缓杈撳嚭鐩綍
        Files.createDirectories(Paths.get(JAVA_OUTPUT_DIR));
        
        // 璇诲彇DSL鏂囦欢
        String dslContent = Files.readString(Paths.get(dslFile), StandardCharsets.UTF_8);
        
        // 鐢熸垚Java浠ｇ爜
        String javaCode = generateJavaFromDSL(dslContent, name);
        
        // 鐢熸垚鏂囦欢鍚?
        String fileName = name + ".java";
        String filePath = JAVA_OUTPUT_DIR + "/" + fileName;
        
        // 鍐欏叆鏂囦欢
        Files.writeString(Paths.get(filePath), javaCode, StandardCharsets.UTF_8);
        
        return filePath;
    }
    
    /**
     * 鏍规嵁DSL鍐呭鐢熸垚Java绫?
     */
    private String generateJavaFromDSL(String dslContent, String name) {
        StringBuilder java = new StringBuilder();
        String className = extractClassName(dslContent, name);
        String topicName = safeJavaValue(extractTopicName(dslContent), "Unknown");
        String destination = safeJavaValue(extractField(dslContent, "destination"), "none");
        String vehicleId = safeJavaValue(extractField(dslContent, "vehicle_id"), "VEH_AUTO");
        String intent = safeJavaValue(extractField(dslContent, "intent"), "UNKNOWN");
        String goal = safeJavaValue(extractField(dslContent, "goal"), destination);
        String distance = extractField(dslContent, "estimated_distance_km");
        String time = extractField(dslContent, "estimated_time_min");

        java.append("package generated;\n\n");
        java.append("import chongqing.*;\n");
        java.append("import dds.DDSManager;\n\n");

        java.append("/**\n");
        java.append(" * 自动生成的任务执行类\n");
        java.append(" * 由自然语言 DSL 转换器生成\n");
        java.append(" * 原始 DSL: ").append(escapeJava(name)).append(".dslib\n");
        java.append(" */\n");
        java.append("public class ").append(className).append(" {\n\n");

        java.append("    // 任务配置\n");
        java.append("    private static final String TOPIC_NAME = \"").append(escapeJava(topicName)).append("\";\n");
        java.append("    private static final String INTENT = \"").append(escapeJava(intent)).append("\";\n");
        java.append("    private static final String GOAL = \"").append(escapeJava(goal)).append("\";\n");
        java.append("    private static final String DESTINATION = \"").append(escapeJava(destination)).append("\";\n");
        java.append("    private static final String VEHICLE_ID = \"").append(escapeJava(vehicleId)).append("\";\n");
        if (isInteger(distance)) {
            java.append("    private static final int ESTIMATED_DISTANCE_KM = ").append(distance).append(";\n");
        }
        if (isInteger(time)) {
            java.append("    private static final int ESTIMATED_TIME_MIN = ").append(time).append(";\n");
        }

        java.append("\n    // DDS 管理器\n");
        java.append("    private DDSManager ddsManager;\n\n");

        java.append("    public ").append(className).append("() {\n");
        java.append("        this.ddsManager = DDSManager.getInstance();\n");
        java.append("    }\n\n");

        java.append("    /**\n");
        java.append("     * 执行协同任务\n");
        java.append("     */\n");
        java.append("    public void execute() {\n");
        java.append("        System.out.println(\"[任务] 开始执行: \" + TOPIC_NAME);\n");
        java.append("        System.out.println(\"[任务] 意图: \" + INTENT);\n");
        java.append("        System.out.println(\"[任务] 目标: \" + GOAL);\n");
        java.append("        System.out.println(\"[任务] 目的地: \" + DESTINATION);\n");
        java.append("        System.out.println(\"[任务] 执行载体: \" + VEHICLE_ID);\n");
        if (isInteger(distance)) {
            java.append("        System.out.println(\"[任务] 预计距离: \" + ESTIMATED_DISTANCE_KM + \" km\");\n");
        }
        if (isInteger(time)) {
            java.append("        System.out.println(\"[任务] 预计时间: \" + ESTIMATED_TIME_MIN + \" 分钟\");\n");
        }
        java.append("\n        // 连接 DDS 并发布任务。\n");
        java.append("        ddsManager.connect();\n");
        java.append("        System.out.println(\"[任务] DDS 已连接，任务已进入协同执行流程\");\n");
        java.append("        System.out.println(\"[任务] 任务完成\");\n");
        java.append("    }\n\n");

        java.append("    public static void main(String[] args) {\n");
        java.append("        ").append(className).append(" task = new ").append(className).append("();\n");
        java.append("        task.execute();\n");
        java.append("    }\n");
        java.append("}\n");

        return java.toString();
    }

    private String safeJavaValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isInteger(String value) {
        return value != null && value.matches("-?\\d+");
    }

    private String escapeJava(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }

    private String extractClassName(String dsl, String defaultName) {
        // 灏濊瘯浠巘opic鍚嶇О鎻愬彇
        for (String line : dsl.split("\n")) {
            if (line.trim().startsWith("topic ")) {
                String topic = line.trim().replace("topic ", "").replace(" {", "").trim();
                if (!topic.isEmpty()) {
                    // 杞崲涓洪┘宄板懡鍚?
                    return toCamelCase(topic);
                }
            }
        }
        return toCamelCase(defaultName);
    }
    
    private String extractTopicName(String dsl) {
        for (String line : dsl.split("\n")) {
            if (line.trim().startsWith("topic ")) {
                return line.trim().replace("topic ", "").replace(" {", "").trim();
            }
        }
        return "Unknown";
    }
    
    private String extractField(String dsl, String fieldName) {
        for (String line : dsl.split("\n")) {
            if (line.contains(fieldName + " = ")) {
                int start = line.indexOf("= \"") + 3;
                int end = line.lastIndexOf("\"");
                if (start > 2 && end > start) {
                    return line.substring(start, end);
                }
                // 灏濊瘯鏁板瓧绫诲瀷
                start = line.indexOf("= ") + 2;
                end = line.indexOf(";", start);
                if (start > 1 && end > start) {
                    return line.substring(start, end).trim();
                }
            }
        }
        return null;
    }
    
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : str.toCharArray()) {
            if (c == '_' || c == ' ' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        // 棣栧瓧姣嶅ぇ鍐?
        if (result.length() > 0) {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        }
        return result.toString();
    }
    
    public void printSummary(Result result) {
        System.out.println("\n=== 杞崲缁撴灉 ===");
        System.out.println("  鍘熷杈撳叆: " + result.naturalLanguage);
        System.out.println("  鎰忓浘: " + result.nluResult.getPrimaryIntent());
        System.out.println("  鐩殑鍦? " + result.nluResult.getDestination());
        System.out.println("  DSL鏂囦欢: " + result.dslFilePath);
        System.out.println("  Java鏂囦欢: " + result.javaFilePath);
        System.out.printf("  鑰楁椂: %dmsource\n", result.endTime - result.startTime);
    }
    
    /**
     * 缁撴灉灏佽
     */
    public static class Result {
        public String naturalLanguage;
        public NLUResult nluResult;
        public String dslCode;
        public String dslFilePath;
        public String javaFilePath;
        public boolean success;
        public String error;
        public long startTime;
        public long endTime;
    }
    
    public static void main(String[] args) {
        NLToDSLToJavaPipeline pipeline = new NLToDSLToJavaPipeline();
        
        if (args.length > 0) {
            // 鍛戒护琛屾ā寮?
            String input = String.join(" ", args);
            Result result = pipeline.execute(input, "intent_task_1");
            pipeline.printSummary(result);
        } else {
            // 浜や簰妯″紡
            pipeline.interactive();
        }
    }
}
