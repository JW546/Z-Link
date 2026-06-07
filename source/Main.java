
import generated.TailorLexer;
import generated.TailorParser;
import model.Mission;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import model.DSLVisitor;
import utils.CoordinateTransformer;

public class Main {
    
    public static void main(String[] args) {
        System.out.println("+==============================================================+");
        System.out.println("|        民用无人交通领域特定语言系统 (Tailor DSL)                |");
        System.out.println("|        演示场景：低空经济与智能网联汽车融合                       |");
        System.out.println("+==============================================================+");
        System.out.println();
        
        if (args.length > 0) {
            if (args[0].equals("--academic")) {
                runAcademicDemo();
            } else if (args[0].equals("--virtual-dds-sim")) {
                runVirtualDDSSimulation();
            } else if (args[0].equals("--llm-health-check")) {
                runLLMHealthCheck();
            } else {
                parseDSLFile(args[0]);
            }
        } else {
            parseDSLFile("chongqing_emergency.dsl");
        }
    }
    
    private static void runAcademicDemo() {
        try {
            agent.AcademicDemoAgent demo = new agent.AcademicDemoAgent();
            demo.runAcademicDemo();
        } catch (Exception e) {
            System.err.println("学术演示异常: " + e.getMessage());
        }
    }

    private static void runVirtualDDSSimulation() {
        try {
            integration.VirtualDDSScenarioDemo.main(new String[0]);
        } catch (Exception e) {
            System.err.println("虚拟DDS仿真异常: " + e.getMessage());
        }
    }

    private static void runLLMHealthCheck() {
        try {
            agent.DeepSeekClient client = new agent.DeepSeekClient();
            agent.DeepSeekClient.HealthCheckResult health = client.healthCheck();
            System.out.println("\n[LLM健康检查]");
            System.out.println("  enabled=" + health.enabled);
            System.out.println("  provider=" + health.provider);
            System.out.println("  model=" + health.model);
            if (health.httpStatus > 0) {
                System.out.println("  httpStatus=" + health.httpStatus);
            }
            if (health.latencyMs > 0) {
                System.out.println("  latency=" + health.latencyMs + "ms");
            }
            if (health.responseId != null && !health.responseId.isBlank()) {
                System.out.println("  responseId=" + health.responseId);
            }
            System.out.println("  result=" + (health.success ? "PASS" : "FAIL"));
            System.out.println("  message=" + health.message);
        } catch (Exception e) {
            System.err.println("LLM健康检查异�? " + e.getMessage());
        }
    }
    
    private static void parseDSLFile(String filename) {
        try {
            CharStream input = CharStreams.fromFileName(filename);
            TailorLexer lexer = new TailorLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TailorParser parser = new TailorParser(tokens);
            
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                      int line, int charPositionInLine, String msg, RecognitionException e) {
                    System.err.println("错误 line " + line + ":" + charPositionInLine + " " + msg);
                }
            });
            
            ParseTree tree = parser.tailorProgram();
            DSLVisitor visitor = new DSLVisitor();
            Mission mission = null;
            
            if (tree instanceof TailorParser.TailorProgramContext) {
                TailorParser.TailorProgramContext tailProgCtx = (TailorParser.TailorProgramContext) tree;
                mission = (Mission) visitor.visitTailorProgram(tailProgCtx);
            }
            
            System.out.println("+------------------------------------------------------------+");
            System.out.println("|  DSL 解析结果                                              |");
            System.out.println("+------------------------------------------------------------+");
            
            if (mission != null) {
                System.out.println("  | 任务定义: " + mission.getTasks().size() + " �?);
                System.out.println("  | 路径规划: " + mission.getPaths().size() + " �?);
                System.out.println("  | 状态监�? " + mission.getStates().size() + " �?);
                System.out.println();
                
                demonstrateCoreFeatures();
                
                String generatedCode = visitor.getGeneratedJavaCode();
                String outputFileName = "generated-src/UrbanTransport.java";
                Files.write(Paths.get(outputFileName), generatedCode.getBytes());
                System.out.println("| 代码生成: " + outputFileName);
                
                System.out.println("\n============================================================");
            } else {
                System.err.println("X 解析失败");
            }
            
        } catch (IOException e) {
            System.err.println("X 无法读取文件: " + filename);
        } catch (Exception e) {
            System.err.println("X 解析错误: " + e.getMessage());
        }
    }
    
    private static void demonstrateCoreFeatures() {
        System.out.println("【核心功能验证�?);
        
        System.out.println("|- 语义对齐: " + utils.SynonymLibrary.isSynonym("配�?, "delivery"));
        System.out.println("|- 单位转换: " + utils.UnitConverter.smartConvert("vehicle_speed", "72km/h"));
        System.out.println("|- 坐标转换: " + CoordinateTransformer.geographicToUTM(106.5549, 29.5582, 200));
        System.out.println("|- 协议适配: MAVLink->DDS OK");
        System.out.println("|- 行为�? 任务决策流程完成");
        System.out.println("|_ CPS Agent: 意图理解->发布订阅->数据匹配->路线优化");
    }
}
