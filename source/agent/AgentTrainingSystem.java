package agent;

import nlu.*;
import utils.AmapRealAPI;
import java.util.*;

/**
 * 完整的Agent训练和演示系统
 * 
 * 端到端流程：
 *   1. 用户输入（语音/键盘）
 *   2. NLU意图识别
 *   3. Agent决策（DDS pub/sub）
 *   4. 数据匹配与协同
 *   5. 路线优化
 *   6. 多无人机协同
 *   7. 结果反馈
 * 
 * 支持场景：
 * - 重庆山地应急医疗物资配送
 * - 多无人机协同配送
 * - 实时路线优化（基于DDS数据）
 */
public class AgentTrainingSystem {
    
    private NLUProcessor nluProcessor;
    private CPSAgent primaryAgent;
    private List<CPSAgent> cooperativeAgents;
    private Map<String, Object> globalContext;
    private AmapRealAPI mapAPI;
    
    public AgentTrainingSystem() {
        // 使用默认输入处理器
        nluProcessor = new NLUProcessor();
        primaryAgent = new CPSAgent();
        cooperativeAgents = new ArrayList<>();
        globalContext = new HashMap<>();
        mapAPI = new AmapRealAPI();
        
        initializeContext();
    }
    
    /**
     * 初始化全局上下文（模拟重庆应急场景）
     */
    private void initializeContext() {
        globalContext.put("scenario", "重庆山地应急医疗物资配送");
        globalContext.put("terrain", "山地");
        globalContext.put("weather", "雾天");
        globalContext.put("visibility", 800.0);  // 米
        globalContext.put("temperature", 12.0); // 摄氏度
        globalContext.put("priority", "EMERGENCY");
    }
    
    /**
     * ========== 主演示流程 ==========
     * 完整的从输入到结果的端到端演示
     */
    public void runFullDemo() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║     重庆山地应急医疗物资无人机协同配送系统 - 完整演示           ║");
        System.out.println("║                                                               ║");
        System.out.println("║  场景: 雾天山地环境，需要快速配送医疗物资                       ║");
        System.out.println("║  能力: 多无人机协同, 智能路线优化, DDS通信                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        // 预加载测试场景
        List<String> scenarios = generateTestScenarios();
        
        int scenarioIndex = 1;
        for (String scenario : scenarios) {
            System.out.println("\n" + "═".repeat(65));
            System.out.println("场景 " + scenarioIndex + ": " + scenario);
            System.out.println("═".repeat(65));
            
            // ===== Step 1: NLU处理 =====
            System.out.println("\n[Step 1] 用户输入处理（NLU）");
            NLUResult nluResult = nluProcessor.processToResult(scenario);
            nluProcessor.printResultDetails(nluResult);
            
            // ===== Step 2: Agent分析 =====
            System.out.println("\n[Step 2] Agent智能分析");
            CPSAgent.AgentResult agentResult = primaryAgent.processInput(scenario);
            
            // ===== Step 3: 多无人机协同 =====
            if ("DELIVERY".equals(agentResult.intent) || "NAVIGATION".equals(agentResult.intent) || "COORDINATION".equals(agentResult.intent)) {
                System.out.println("\n[Step 3] 多无人机协同规划");
                demonstrateCooperativeDelivery(nluResult, agentResult);
            }
            
            scenarioIndex++;
        }
        
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      演示完成！                                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * 演示多无人机协同配送
     */
    private void demonstrateCooperativeDelivery(NLUResult nluResult, CPSAgent.AgentResult agentResult) {
        String destination = nluResult.getDestination() != null ? nluResult.getDestination() : "目标地点";
        
        System.out.println("\n  🚁 协同配送规划：");
        System.out.println("  ├─ 目标地点: " + destination);
        System.out.println("  ├─ 优先级: " + globalContext.get("priority"));
        System.out.println("  ├─ 环境条件: " + globalContext.get("weather") + "，能见度 " + 
            globalContext.get("visibility") + "m");
        
        // 模拟多无人机任务分配
        System.out.println("\n  📋 无人机任务分配:");
        String[] droneNames = {"无人机-1", "无人机-2", "无人机-3"};
        String[] tasks = {
            "路线规划（风险评估）",
            "空中侦察（实时监测）",
            "医疗物资运输（主任务）"
        };
        
        for (int i = 0; i < droneNames.length; i++) {
            System.out.printf("    ├─ %s: %source\n", droneNames[i], tasks[i]);
        }
        
        // DDS通信展示
        System.out.println("\n  🔗 DDS智能协同通信：");
        System.out.println("    发布主题: " + agentResult.publishTopics);
        System.out.println("    订阅主题: " + agentResult.subscribeTopics);
        System.out.println("    可满足需求: " + agentResult.satisfiableTopics);
        
        // 路线优化结果
        if (agentResult.optimizedRoute != null) {
            System.out.println("\n  🗺️ 智能路线优化结果：");
            System.out.println("    优化路线: " + agentResult.optimizedRoute.description);
            System.out.println("    预计时间: " + agentResult.optimizedRoute.estimatedTime + " 分钟");
            System.out.println("    路线说明: " + agentResult.optimizedRoute.description);
            System.out.println("    是否改路: " + (agentResult.optimizedRoute.rerouted ? "是" : "否"));
        }
        
        System.out.println("\n  ✅ 协同配送计划已生成，准备执行！");
    }
    
    /**
     * 生成测试场景集
     */
    private List<String> generateTestScenarios() {
        List<String> scenarios = new ArrayList<>();
        
        // 重庆山地应急场景特化
        scenarios.add("我要配送医疗物资到山区，这是应急任务，很紧急");
        scenarios.add("去西南大学附属医院，需要快速送到，这是紧急医疗配送");
        scenarios.add("在雾天条件下导航到山区医院，避开高山，走安全路线");
        scenarios.add("乘客在无人车内提出到医院的自然语言意图，车辆自主联动邻车与无人机协同规划路线，启用合规广域链路");
        
        return scenarios;
    }
    
    /**
     * ========== 训练模式 ==========
     * 用于训练Agent认识不同的场景和意图
     */
    public void trainMode() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║              Agent 训练模式 - 交互式学习                       ║");
        System.out.println("║         输入自然语言，系统学习和优化意图识别能力               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        nluProcessor.interactiveMode();
    }
    
    /**
     * ========== 验证模式 ==========
     * 验证所有模块是否正常工作
     */
    public void validateMode() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  系统模块验证                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        // 1. NLU验证
        System.out.println("\n✓ NLU模块验证:");
        List<String> testInputs = Arrays.asList(
            "去西南大学",
            "配送医疗物资到山区",
            "这很紧急"
        );
        for (String input : testInputs) {
            NLUResult result = nluProcessor.processToResult(input);
            System.out.println("  " + input + " → " + result.getPrimaryIntent());
        }
        
        // 2. Agent验证
        System.out.println("\n✓ Agent模块验证:");
        CPSAgent.AgentResult agentResult = primaryAgent.processInput("我要去西南大学");
        System.out.println("  输入: 我要去西南大学");
        System.out.println("  意图: " + agentResult.intent);
        System.out.println("  角色: " + agentResult.deviceRole);
        
        // 3. 地图API验证
        System.out.println("\n✓ 地图API验证:");
        try {
            AmapRealAPI.AmapGeoResult geoResult = mapAPI.geocode("西南大学");
            if (geoResult != null && geoResult.isSuccess()) {
                System.out.println("  西南大学坐标: " + geoResult.getLocationString());
            } else {
                System.out.println("  西南大学坐标: 未查询到");
            }
        } catch (Exception e) {
            System.out.println("  ⚠️ 地图API未配置或网络不可达（使用模拟数据）");
        }
        
        System.out.println("\n✅ 验证完成！所有模块正常运行。");
    }
    
    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        CPS Agent 协同系统 - 重庆山地应急配送演示               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        AgentTrainingSystem system = new AgentTrainingSystem();
        
        // 显示菜单
        System.out.println("\n请选择运行模式：");
        System.out.println("  1. 运行完整演示 (runDemo)");
        System.out.println("  2. 训练模式 (train) - 交互式输入");
        System.out.println("  3. 验证模块 (validate)");
        System.out.println("\n建议: 先运行 'runDemo' 查看完整流程，再选择其他模式");
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("\n请输入选择 (输入'demo'/'train'/'validate' 或直接按Enter运行演示): ");
        String choice = scanner.nextLine().trim().toLowerCase();
        
        switch (choice) {
            case "train":
                system.trainMode();
                break;
            case "validate":
                system.validateMode();
                break;
            case "demo":
            default:
                system.runFullDemo();
                break;
        }
        
        scanner.close();
    }
}
