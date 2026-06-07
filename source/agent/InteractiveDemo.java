package agent;

import java.util.Scanner;

/**
 * 交互式集成测试 - 支持键盘输入
 * 基于重庆山地应急医疗物资无人机协同配送场景
 */
public class InteractiveDemo {
    public static void main(String[] args) {
        printHeader();
        
        CPSAgent agent = new CPSAgent();
        
        System.out.println("\n请选择测试模式：");
        System.out.println("  1. 自动化场景演示 (推荐用于快速验证)");
        System.out.println("  2. 交互式键盘输入 (输入自定义指令)");
        System.out.println("  3. 应急场景库");
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("\n请输入选择 [1-3]: ");
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                runDemoScenarios(agent);
                break;
            case "2":
                runInteractiveMode(agent, scanner);
                break;
            case "3":
                runScenarioLibrary(agent, scanner);
                break;
            default:
                System.out.println("默认运行演示场景");
                runDemoScenarios(agent);
        }
        
        scanner.close();
        System.out.println("\n" + "═".repeat(66));
        System.out.println("测试完成");
        System.out.println("═".repeat(66));
    }
    
    /**
     * 自动化演示 - 展示4个标准场景
     */
    private static void runDemoScenarios(CPSAgent agent) {
        System.out.println("\n" + "═".repeat(66));
        System.out.println(" 自动化场景演示 (4个预设场景)");
        System.out.println("═".repeat(66));
        
        String[] scenarios = {
            "我想去西南大学，很急",
            "启动无人机配送任务到观音桥",
            "发生紧急事故，需要无人机协助",
            "电量还够吗，需要充电吗"
        };
        
        for (int i = 0; i < scenarios.length; i++) {
            System.out.println("\n─ 场景" + (i+1) + " ─");
            System.out.println("输入: " + scenarios[i]);
            
            agent.processInput(scenarios[i]);
            
            System.out.println();
        }
    }
    
    /**
     * 交互式模式 - 支持用户输入
     */
    private static void runInteractiveMode(CPSAgent agent, Scanner scanner) {
        System.out.println("\n" + "═".repeat(66));
        System.out.println(" 交互式键盘输入模式");
        System.out.println("═".repeat(66));
        System.out.println("\n提示：");
        System.out.println("  - 输入自然语言指令（例如: 配送医疗物资到山区）");
        System.out.println("  - 输入 'exit' 或 'quit' 退出");
        System.out.println("  - 输入 'help' 查看示例");
        
        while (true) {
            System.out.print("\n请输入指令> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                printExamples();
                continue;
            }
            
            System.out.println("\n处理中...");
            agent.processInput(input);
            System.out.println();
        }
        
        System.out.println("\n已退出交互模式");
    }
    
    /**
     * 场景库 - 重庆应急场景集合
     */
    private static void runScenarioLibrary(CPSAgent agent, Scanner scanner) {
        System.out.println("\n" + "═".repeat(66));
        System.out.println(" 重庆山地应急场景库");
        System.out.println("═".repeat(66));
        
        String[] scenarios = {
            "配送医疗物资到綦江山区，这很紧急",
            "在雾天中导航到黔江医院",
            "3架无人机协同配送到武隆景区",
            "检测城市交通拥堵，及时调度",
            "电动车电量不足，需要更换电池"
        };
        
        System.out.println("\n可用场景：");
        for (int i = 0; i < scenarios.length; i++) {
            System.out.println("  " + (i+1) + ". " + scenarios[i]);
        }
        
        System.out.print("\n选择场景 [1-" + scenarios.length + "], 或按 Enter 测试全部: ");
        String choice = scanner.nextLine().trim();
        
        if (choice.isEmpty()) {
            System.out.println("\n执行全部场景...\n");
            for (String scenario : scenarios) {
                System.out.println("─ 测试场景 ─");
                System.out.println("输入: " + scenario);
                agent.processInput(scenario);
                System.out.println();
            }
        } else {
            try {
                int idx = Integer.parseInt(choice) - 1;
                if (idx >= 0 && idx < scenarios.length) {
                    System.out.println("\n " + scenarios[idx] + "\n");
                    agent.processInput(scenarios[idx]);
                } else {
                    System.out.println("选择无效");
                }
            } catch (NumberFormatException e) {
                System.out.println("选择无效");
            }
        }
    }
    
    private static void printHeader() {
        System.out.println("\n" + "═".repeat(66));
        System.out.println("|" + " ".repeat(14) + "民用无人交通DSL系统 - 集成测试" + " ".repeat(18) + "|");
        System.out.println("|" + " ".repeat(18) + "CPS协同Agent · 意图理解 · 协议适配" + " ".repeat(8) + "|");
        System.out.println("|" + " ".repeat(20) + "基于重庆山地应急医疗配送场景" + " ".repeat(12) + "|");
        System.out.println("═".repeat(66));
    }
    
    private static void printExamples() {
        System.out.println("\n常见指令示例：");
        System.out.println("  导航类: 我要去西南大学, 请导航到医院");
        System.out.println("  配送类: 配送医疗物资到山区, 启动无人机送货");
        System.out.println("  应急类: 发生紧急事故, 需要立即救援");
        System.out.println("  查询类: 电量还够吗, 检查无人机状态");
    }
}
