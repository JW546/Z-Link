package nlu;

import java.util.*;
import java.util.regex.*;

/**
 * 实时交互式自然语言对话系统
 * 
 * 功能�?
 * 1. 等待用户键盘输入
 * 2. 实时识别意图并生成任�?
 * 3. 支持连续添加多个目的�?
 * 4. 最后生成完整的导航计划
 * 
 * 使用方式�?
 * java -cp "build-classes;l/*;i;g" nlu.InteractiveNavigator
 */
public class InteractiveNavigator {
    
    private NLUProcessor processor;
    private List<NavigationTask> tasks;
    private Scanner scanner;
    private boolean running;
    
    /**
     * 导航任务封装�?
     */
    public static class NavigationTask {
        public int id;
        public String destination;
        public String originalInput;
        public NLUResult.UrgencyLevel urgency;
        public NLUResult.CostPreference costPreference;
        public NLUResult.RoutePreference routePreference;
        public String navigationMode;
        public String dslCode;
        
        public NavigationTask(int id, String input, NLUResult result, String dsl) {
            this.id = id;
            this.destination = result.getDestination() != null ? result.getDestination() : "未识�?;
            this.originalInput = input;
            this.urgency = result.getUrgencyLevel();
            this.costPreference = result.getCostPreference();
            this.routePreference = result.getRoutePreference();
            this.navigationMode = result.getNavigationMode();
            this.dslCode = dsl;
        }
        
        public void printTaskCard() {
            System.out.println("┌──────────────────────────────────────────────────────────────�?);
            System.out.println("�? 任务 #" + id + "                                                      �?);
            System.out.println("├──────────────────────────────────────────────────────────────�?);
            System.out.printf ("�? 目的地：%-50s │\n", destination);
            System.out.printf ("�? 路线选择�?-49s │\n", getRouteTypeName());
            System.out.printf ("�? 紧急程度：%-49s │\n", urgency.name());
            System.out.printf ("�? 费用偏好�?-49s │\n", costPreference.name());
            System.out.printf ("�? 原始输入：\"%-48s │\n", truncate(originalInput, 48));
            System.out.println("└──────────────────────────────────────────────────────────────�?);
        }
        
        private String getRouteTypeName() {
            switch (routePreference) {
                case TIME_FIRST: return "高速优先（最快到达）";
                case COST_FIRST: return "经济路线（省钱优先）";
                case SCENIC: return "景观路线（欣赏风景）";
                default: return "均衡路线（综合最优）";
            }
        }
        
        private String truncate(String s, int maxLen) {
            if (s == null) return "";
            if (s.length() <= maxLen) return s;
            return s.substring(0, maxLen - 3) + "...";
        }
    }
    
    public InteractiveNavigator() {
        this.processor = new NLUProcessor();
        this.tasks = new ArrayList<>();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }
    
    /**
     * 启动交互式对�?
     */
    public void start() {
        printWelcome();
        
        while (running) {
            printPrompt();
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                System.out.println("⚠️  请输入您的目的地或指令\n");
                continue;
            }
            
            processInput(input);
        }
        
        scanner.close();
    }
    
    /**
     * 处理用户输入
     */
    private void processInput(String input) {
        if (isEndCommand(input)) {
            finishConversation();
            return;
        }
        
        if (isViewCommand(input)) {
            showCurrentPlan();
            return;
        }
        
        if (isDeleteCommand(input)) {
            deleteTask(input);
            return;
        }
        
        if (isModifyCommand(input)) {
            modifyTask(input);
            return;
        }
        
        addNavigationTask(input);
    }
    
    /**
     * 添加导航任务
     */
    private void addNavigationTask(String input) {
        System.out.println("\n�?正在理解您的意图...\n");
        
        try {
            NLUResult result = processor.processToResult(input);
            int taskId = tasks.size() + 1;
            String dslCode = processor.process(input);
            
            NavigationTask task = new NavigationTask(taskId, input, result, dslCode);
            tasks.add(task);
            
            task.printTaskCard();
            
            System.out.println("\n�?任务已添加到计划！\n");
            showPlanSummary();
            
        } catch (Exception e) {
            System.out.println("�?处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 完成对话并生成完整计�?
     */
    private void finishConversation() {
        if (tasks.isEmpty()) {
            System.out.println("\n📋 您还没有添加任何目的地。\n");
            running = false;
            return;
        }
        
        System.out.println("\n\n" + "�?.repeat(70));
        System.out.println("                    📋 完整导航计划生成");
        System.out.println("�?.repeat(70));
        
        System.out.println("\n【任务列表】\n");
        for (NavigationTask task : tasks) {
            task.printTaskCard();
            System.out.println();
        }
        
        printOverview();
        
        String fullDSL = generateFullDSL();
        System.out.println("\n【完整DSL代码�?);
        System.out.println("─".repeat(70));
        System.out.println(fullDSL);
        System.out.println("─".repeat(70));
        
        saveToFile(fullDSL);
        
        System.out.println("\n" + "�?.repeat(70));
        System.out.println("                    �?计划已生成完毕！");
        System.out.println("�?.repeat(70));
        System.out.println("\n💡 提示�?);
        System.out.println("   �?DSL代码已保存到 generated/ 目录\n");
        
        running = false;
    }
    
    /**
     * 生成完整的DSL代码 - 包含丰富的元数据注解
     */
    private String generateFullDSL() {
        StringBuilder dsl = new StringBuilder();
        
        // 文件头注�?
        dsl.append("// ═══════════════════════════════════════════════════════════════════\n");
        dsl.append("// 自然语言转DSL - 实时交互生成的多目的地导航计划\n");
        dsl.append("// 生成时间: ").append(java.time.LocalDateTime.now()).append("\n");
        dsl.append("// 任务数量: ").append(tasks.size()).append(" 个目的地\n");
        dsl.append("// 设备类型: 比亚迪智能网联汽�?+ 大疆灵鸢无人机系统\n");
        dsl.append("// ═══════════════════════════════════════════════════════════════════\n\n");
        
        // Topic定义
        dsl.append("topic MultiDestinationNavigation {\n\n");
        
        // 全局任务注解
        dsl.append("    // ==================== 多目的地导航计划 ====================\n");
        dsl.append("    @TaskAnnotation(name=\"multi_destination_navigation\", priority=\"1\")\n");
        dsl.append("    @MultiDestination(waypoint_count=\"").append(tasks.size()).append("\")\n");
        dsl.append("    @VehicleType(type=\"BYD_INTELLIGENT_VEHICLE\", protocol=\"CAN-BUS\")\n");
        dsl.append("    @DroneType(type=\"DJI_MAVIC\", protocol=\"MAVLink\")\n");
        dsl.append("    @WeatherCondition(normal=\"GOOD\", adverse=\"FOG_RAIN\")\n\n");
        
        for (int i = 0; i < tasks.size(); i++) {
            NavigationTask task = tasks.get(i);
            
            dsl.append("    // ─────────────────────────────────────────────────────────\n");
            dsl.append("    // 任务 #").append(task.id).append(": ").append(task.destination).append("\n");
            dsl.append("    // ─────────────────────────────────────────────────────────\n");
            
            // 任务级注�?
            dsl.append("    @TaskAnnotation(name=\"nav_task_").append(task.id).append("\", priority=\"").append(task.id).append("\")\n");
            dsl.append("    @Waypoint(index=\"").append(task.id).append("\", destination=\"").append(task.destination).append("\")\n");
            
            if (task.urgency == NLUResult.UrgencyLevel.HIGH || 
                task.urgency == NLUResult.UrgencyLevel.CRITICAL) {
                dsl.append("    @EmergencyLevel(level=\"").append(task.urgency.name())
                   .append("\", response_time=\"").append(task.urgency.getResponseTimeMinutes())
                   .append("min\")\n");
            }
            
            // 任务定义
            dsl.append("    task waypoint_").append(task.id).append(" {\n");
            
            // 目的地属�?
            dsl.append("        @attr(\"DESTINATION\")\n");
            dsl.append("        @name(\"目的地\")\n");
            dsl.append("        @description(\"导航目标地点\")\n");
            dsl.append("        string destination = \"").append(task.destination).append("\";\n\n");
            
            // 序列�?
            dsl.append("        @attr(\"SEQUENCE\")\n");
            dsl.append("        @name(\"任务序号\")\n");
            dsl.append("        @description(\"在导航序列中的顺序\")\n");
            dsl.append("        int sequence = ").append(task.id).append(";\n\n");
            
            // 车辆ID
            dsl.append("        @attr(\"VEHICLE_ID\")\n");
            dsl.append("        @name(\"车辆ID\")\n");
            dsl.append("        @description(\"执行任务的车辆标识\")\n");
            dsl.append("        string vehicle_id = \"BYD-VEHICLE-001\";\n\n");
            
            // 导航模式
            dsl.append("        @attr(\"NAVIGATION_MODE\")\n");
            dsl.append("        @name(\"导航模式\")\n");
            dsl.append("        @description(\"路径选择策略\")\n");
            dsl.append("        string navigation_mode = \"").append(task.navigationMode).append("\";\n\n");
            
            // 紧急程�?
            dsl.append("        @attr(\"URGENCY_LEVEL\")\n");
            dsl.append("        @name(\"紧急程度\")\n");
            dsl.append("        @description(\"任务优先级\")\n");
            dsl.append("        string urgency_level = \"").append(task.urgency.name()).append("\";\n\n");
            
            // 费用偏好
            dsl.append("        @attr(\"COST_PREFERENCE\")\n");
            dsl.append("        @name(\"费用偏好\")\n");
            dsl.append("        @description(\"用户费用选择\")\n");
            dsl.append("        string cost_preference = \"").append(task.costPreference.name()).append("\";\n\n");
            
            // 路线偏好
            dsl.append("        @attr(\"ROUTE_PREFERENCE\")\n");
            dsl.append("        @name(\"路线偏好\")\n");
            dsl.append("        @description(\"路线选择策略\")\n");
            dsl.append("        string route_preference = \"").append(task.routePreference.name()).append("\";\n");
            
            dsl.append("    }\n\n");
            
            // 路径规划
            dsl.append("    @PathAnnotation(start=\"当前位置\", end=\"").append(task.destination).append("\")\n");
            dsl.append("    path route_").append(task.id).append(" {\n");
            
            dsl.append("        @attr(\"ROUTE_TYPE\")\n");
            dsl.append("        @name(\"路线类型\")\n");
            dsl.append("        @description(\"路径分类\")\n");
            dsl.append("        string route_type = \"").append(getRouteType(task.routePreference)).append("\";\n\n");
            
            dsl.append("        @attr(\"PRIORITY\")\n");
            dsl.append("        @name(\"优先级\")\n");
            dsl.append("        @description(\"路线优先级\")\n");
            dsl.append("        string priority = \"").append(task.routePreference.name().toLowerCase()).append("\";\n\n");
            
            dsl.append("        @attr(\"TOLL_PREFERENCE\")\n");
            dsl.append("        @name(\"收费偏好\")\n");
            dsl.append("        @description(\"是否走收费道路\")\n");
            dsl.append("        string toll_preference = \"").append(task.costPreference.name().toLowerCase()).append("\";\n\n");
            
            dsl.append("        @attr(\"COORDINATE_SYSTEM\")\n");
            dsl.append("        @name(\"坐标系\")\n");
            dsl.append("        @description(\"导航使用的坐标系\")\n");
            dsl.append("        string coordinate_system = \"WGS84\";\n");
            
            dsl.append("    }\n\n");
        }
        
        // 导航状�?
        dsl.append("    // ==================== 导航状态监�?====================\n");
        dsl.append("    @StateAnnotation(name=\"navigation_status_monitoruntime\")\n");
        dsl.append("    state navigationStatus {\n");
        
        dsl.append("        @attr(\"PLAN_STATUS\")\n");
        dsl.append("        @name(\"计划状态\")\n");
        dsl.append("        @description(\"导航计划当前状态\")\n");
        dsl.append("        string plan_status = \"ready_to_execute\";\n\n");
        
        dsl.append("        @attr(\"TOTAL_WAYPOINTS\")\n");
        dsl.append("        @name(\"总航点数\")\n");
        dsl.append("        @description(\"计划中的总目的地数量\")\n");
        dsl.append("        int total_waypoints = ").append(tasks.size()).append(";\n\n");
        
        dsl.append("        @attr(\"COMPLETED_WAYPOINTS\")\n");
        dsl.append("        @name(\"已完成航点\")\n");
        dsl.append("        @description(\"已到达的目的地数量\")\n");
        dsl.append("        int completed_waypoints = 0;\n");
        
        dsl.append("    }\n");
        
        // 协议配置
        dsl.append("\n    // ==================== 协议配置 ====================\n");
        dsl.append("    @ProtocolConfig(vehicle_protocol=\"CAN-BUS\", drone_protocol=\"MAVLink\", cloud_protocol=\"DDS\")\n");
        dsl.append("    @DataFormat(serialization=\"JSON\", compression=\"LZ4\")\n");
        dsl.append("    @SecurityLevel(level=\"STANDARD\", encryption=\"AES256\")\n");
        
        dsl.append("}\n");
        
        return dsl.toString();
    }
    
    private String getRouteType(NLUResult.RoutePreference pref) {
        switch (pref) {
            case TIME_FIRST: return "highway_toll_road";
            case COST_FIRST: return "local_economy_route";
            case SCENIC: return "scenic_route";
            default: return "balanced_route";
        }
    }
    
    /**
     * 打印总览信息
     */
    private void printOverview() {
        System.out.println("【计划总览�?);
        System.out.println("─".repeat(70));
        System.out.printf ("  目的地数量：%d 个\n", tasks.size());
        System.out.println("─".repeat(70));
    }
    
    /**
     * 显示当前计划摘要
     */
    private void showPlanSummary() {
        if (tasks.isEmpty()) return;
        
        System.out.println("【当前计�?- �?" + tasks.size() + " 个目的地�?);
        for (NavigationTask task : tasks) {
            System.out.printf("  %d. %s [%s]\n", task.id, task.destination, task.urgency.name());
        }
        System.out.println();
        System.out.println("提示�?);
        System.out.println("   - 继续添加目的�?);
        System.out.println("   - 输入'完成' - 生成完整计划");
        System.out.println("   - 输入'查看' - 查看当前计划\n");
    }
    
    /**
     * 显示当前完整计划
     */
    private void showCurrentPlan() {
        if (tasks.isEmpty()) {
            System.out.println("\n📋 计划为空，请先添加目的地。\n");
            return;
        }
        
        System.out.println("\n" + "�?.repeat(70));
        System.out.println("                    📋 当前导航计划");
        System.out.println("�?.repeat(70) + "\n");
        
        for (NavigationTask task : tasks) {
            task.printTaskCard();
            System.out.println();
        }
        
        printOverview();
    }
    
    /**
     * 删除任务
     */
    private void deleteTask(String input) {
        Pattern pattern = Pattern.compile("删除(\\d+)");
        Matcher matcher = pattern.matcher(input);
        
        if (matcher.find()) {
            int taskId = Integer.parseInt(matcher.group(1));
            
            if (taskId < 1 || taskId > tasks.size()) {
                System.out.println("\n�?无效的任务编号！\n");
                return;
            }
            
            tasks.remove(taskId - 1);
            
            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).id = i + 1;
            }
            
            System.out.println("\n�?任务 #" + taskId + " 已删除！\n");
            
            if (!tasks.isEmpty()) {
                showPlanSummary();
            }
        } else {
            System.out.println("\n⚠️  格式错误，请使用：删�?\n");
        }
    }
    
    /**
     * 修改任务
     */
    private void modifyTask(String input) {
        Pattern pattern = Pattern.compile("修改(\\d+)");
        Matcher matcher = pattern.matcher(input);
        
        if (matcher.find()) {
            int taskId = Integer.parseInt(matcher.group(1));
            
            if (taskId < 1 || taskId > tasks.size()) {
                System.out.println("\n�?无效的任务编号！\n");
                return;
            }
            
            System.out.println("\n请输入新的目的地描述�?);
            System.out.print("> ");
            String newInput = scanner.nextLine().trim();
            
            if (newInput.isEmpty()) {
                System.out.println("\n⚠️  修改取消。\n");
                return;
            }
            
            tasks.remove(taskId - 1);
            
            NLUResult result = processor.processToResult(newInput);
            String dslCode = processor.process(newInput);
            NavigationTask newTask = new NavigationTask(taskId, newInput, result, dslCode);
            tasks.add(taskId - 1, newTask);
            
            System.out.println("\n�?任务 #" + taskId + " 已修改！\n");
            newTask.printTaskCard();
            showPlanSummary();
            
        } else {
            System.out.println("\n⚠️  格式错误，请使用：修�?\n");
        }
    }
    
    private boolean isEndCommand(String input) {
        String[] endCommands = {"完成", "够了", "生成计划", "可以�?, "结束", "执行"};
        for (String cmd : endCommands) {
            if (input.contains(cmd)) return true;
        }
        return false;
    }
    
    private boolean isViewCommand(String input) {
        String[] viewCommands = {"查看", "显示", "列表"};
        for (String cmd : viewCommands) {
            if (input.contains(cmd)) return true;
        }
        return false;
    }
    
    private boolean isDeleteCommand(String input) {
        return input.contains("删除") || input.contains("去掉") || input.contains("移除");
    }
    
    private boolean isModifyCommand(String input) {
        return input.contains("修改") || input.contains("更正") || input.contains("改变");
    }
    
    private void saveToFile(String content) {
        try {
            String filename = "g/InteractivePlan_" + System.currentTimeMillis() + ".dsl";
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), content.getBytes());
            System.out.println("💾 计划已保存到: " + filename);
        } catch (Exception e) {
            System.err.println("�?保存失败: " + e.getMessage());
        }
    }
    
    private void printWelcome() {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════�?);
        System.out.println("�?       🚗  自然语言转DSL - 实时交互式导航规划系�?🚗                   �?);
        System.out.println("�?                                                                      �?);
        System.out.println("�?    您可以实时添加多个目的地，系统将为您生成完整的导航计�?           �?);
        System.out.println("╚═══════════════════════════════════════════════════════════════════════�?);
        System.out.println();
        System.out.println("💡 使用说明�?);
        System.out.println("   1. 输入您想去的地方，如：\"我想去西南大学\"");
        System.out.println("   2. 系统会识别您的意图并生成任务");
        System.out.println("   3. 可以继续添加更多目的�?);
        System.out.println("   4. 输入\"完成\"后生成完整的导航计划");
        System.out.println();
        System.out.println("📝 示例指令�?);
        System.out.println("   �?\"我要去西南大学\"");
        System.out.println("   �?\"我想去天津\"");
        System.out.println("   �?\"去机场，尽快\"");
        System.out.println("   �?\"查看\" - 查看当前计划");
        System.out.println("   �?\"完成\" - 生成完整计划");
        System.out.println();
        System.out.println("─".repeat(70));
        System.out.println();
    }
    
    private void printPrompt() {
        System.out.print("🗣�? 请输入目的地（输入\"完成\"生成计划）：");
    }
    
    public static void main(String[] args) {
        InteractiveNavigator navigator = new InteractiveNavigator();
        navigator.start();
    }
}
