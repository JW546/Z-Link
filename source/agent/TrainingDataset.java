package agent;

import java.util.*;

/**
 * Agent训练数据集
 * 
 * 包含：
 * - 标注的NLU训练样本
 * - 不同场景下的预期行为
 * - 重庆应急场景的特化数据
 * 
 * 用于：
 * - 验证Agent的学习能力
 * - 评估NLU准确性
 * - 基准测试
 */
public class TrainingDataset {
    
    /**
     * 训练样本
     */
    public static class TrainingSample {
        public String input;                    // 输入的自然语言
        public String expectedIntent;           // 期望的意图
        public String expectedDestination;      // 期望的目的地
        public String emergencyLevel;           // 紧急程度
        public String scenarioCategory;         // 场景分类
        
        TrainingSample(String input, String intent, String destination, String emergency, String scenario) {
            this.input = input;
            this.expectedIntent = normalizeIntent(intent);
            this.expectedDestination = destination;
            this.emergencyLevel = emergency;
            this.scenarioCategory = scenario;
        }

        private static String normalizeIntent(String intent) {
            if (intent == null) {
                return null;
            }
            if ("EMERGENCY_DELIVERY".equalsIgnoreCase(intent)) {
                return "DELIVERY";
            }
            return intent;
        }
    }
    
    /**
     * 获取导航类训练数据
     */
    public static List<TrainingSample> getNavigationTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        
        // 基础导航
        samples.add(new TrainingSample("我要去西南大学", "NAVIGATION", "西南大学", "NORMAL", "基础导航"));
        samples.add(new TrainingSample("导航到机场", "NAVIGATION", "机场", "NORMAL", "基础导航"));
        samples.add(new TrainingSample("送我去火车站", "NAVIGATION", "火车站", "NORMAL", "基础导航"));
        samples.add(new TrainingSample("前往人民医院", "NAVIGATION", "人民医院", "NORMAL", "基础导航"));
        samples.add(new TrainingSample("去商场", "NAVIGATION", "商场", "NORMAL", "基础导航"));
        
        // 带紧急程度的导航
        samples.add(new TrainingSample("我要去机场，我着急", "NAVIGATION", "机场", "HIGH", "紧急导航"));
        samples.add(new TrainingSample("到医院，尽快", "NAVIGATION", "医院", "HIGH", "紧急导航"));
        samples.add(new TrainingSample("导航去市政府，马上要", "NAVIGATION", "市政府", "CRITICAL", "紧急导航"));
        samples.add(new TrainingSample("这很紧急，我要立刻去医院", "NAVIGATION", "医院", "CRITICAL", "紧急导航"));
        
        // 带费用偏好的导航
        samples.add(new TrainingSample("去西南大学，费用不是问题", "NAVIGATION", "西南大学", "NORMAL", "费用优化"));
        samples.add(new TrainingSample("到机场，不计成本", "NAVIGATION", "机场", "NORMAL", "费用优化"));
        samples.add(new TrainingSample("我要去商场，省点钱", "NAVIGATION", "商场", "NORMAL", "费用优化"));
        
        return samples;
    }
    
    /**
     * 获取应急配送训练数据（重庆山地场景）
     */
    public static List<TrainingSample> getEmergencyDeliveryTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        
        // 医疗配送
        samples.add(new TrainingSample("配送医疗物资到山区", "DELIVERY", "山区", "CRITICAL", "医疗配送"));
        samples.add(new TrainingSample("我要配送医疗物资到山区，这是应急任务，很紧急", "DELIVERY", "山区", "CRITICAL", "医疗配送"));
        samples.add(new TrainingSample("去西南大学附属医院，需要快速送到，这是紧急医疗配送", "DELIVERY", "西南大学附属医院", "CRITICAL", "医疗配送"));
        samples.add(new TrainingSample("在雾天条件下导航到山区医院，避开高山，走安全路线", "DELIVERY", "山区医院", "CRITICAL", "医疗配送"));
        
        // 物资配送
        samples.add(new TrainingSample("配送紧急物资到灾区", "DELIVERY", "灾区", "CRITICAL", "物资配送"));
        samples.add(new TrainingSample("运送救援物资到受灾地点", "DELIVERY", "受灾地点", "CRITICAL", "物资配送"));
        
        // 路线规划特殊需求
        samples.add(new TrainingSample("在山地环境中规划避开高风险区域的配送路线", "NAVIGATION", "目标地点", "HIGH", "山地配送"));
        samples.add(new TrainingSample("在雾天中进行配送，需要增加安全裕度", "DELIVERY", "目标地点", "HIGH", "恶劣天气"));
        
        return samples;
    }

    /**
     * 获取协同调度训练数据
     */
    public static List<TrainingSample> getCoordinationTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();

        samples.add(new TrainingSample("乘客说我要尽快去医院，请无人车联动后车和无人机规划路线", "COORDINATION", null, "HIGH", "乘客意图协同"));
        samples.add(new TrainingSample("无人车收到乘客意图后，自主订阅无人机前方视角和邻车建议", "COORDINATION", null, "HIGH", "自治订阅发布"));
        samples.add(new TrainingSample("启用合规广域链路同步全局路况，并由车端自主决策路线", "COORDINATION", null, "HIGH", "合规广域协同"));
        samples.add(new TrainingSample("前车、后车和无人机互相发布路线建议，车辆融合后选择最佳路线", "COORDINATION", null, "HIGH", "多设备建议融合"));
        
        return samples;
    }
    
    /**
     * 获取查询类训练数据
     */
    public static List<TrainingSample> getQueryTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        
        samples.add(new TrainingSample("到哪里了", "QUERY_STATUS", null, "NORMAL", "状态查询"));
        samples.add(new TrainingSample("还有多久到", "QUERY_ETA", null, "NORMAL", "ETA查询"));
        samples.add(new TrainingSample("电量还有多少", "QUERY_BATTERY", null, "NORMAL", "电量查询"));
        samples.add(new TrainingSample("现在在哪", "QUERY_STATUS", null, "NORMAL", "位置查询"));
        
        return samples;
    }
    
    /**
     * 获取控制类训练数据
     */
    public static List<TrainingSample> getControlTrainingSamples() {
        List<TrainingSample> samples = new ArrayList<>();
        
        samples.add(new TrainingSample("停车", "CONTROL", null, "HIGH", "停止控制"));
        samples.add(new TrainingSample("停下", "CONTROL", null, "HIGH", "停止控制"));
        samples.add(new TrainingSample("减速", "CONTROL", null, "NORMAL", "速度控制"));
        samples.add(new TrainingSample("加速", "CONTROL", null, "NORMAL", "速度控制"));
        samples.add(new TrainingSample("充电", "CONTROL", null, "NORMAL", "能源管理"));
        
        return samples;
    }
    
    /**
     * 获取所有训练数据
     */
    public static List<TrainingSample> getAllTrainingSamples() {
        List<TrainingSample> allSamples = new ArrayList<>();
        allSamples.addAll(getNavigationTrainingSamples());
        allSamples.addAll(getEmergencyDeliveryTrainingSamples());
        allSamples.addAll(getCoordinationTrainingSamples());
        allSamples.addAll(getQueryTrainingSamples());
        allSamples.addAll(getControlTrainingSamples());
        return allSamples;
    }
    
    /**
     * 统计信息
     */
    public static void printStatistics() {
        List<TrainingSample> allSamples = getAllTrainingSamples();
        
        System.out.println("\n╔═══════════════════════════════════════════════════╗");
        System.out.println("║         Agent 训练数据集统计信息                    ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        
        System.out.printf("║ 总样本数: %-44d ║\n", allSamples.size());
        System.out.printf("║   - 导航样本: %-41d ║\n", getNavigationTrainingSamples().size());
        System.out.printf("║   - 应急配送样本: %-36d ║\n", getEmergencyDeliveryTrainingSamples().size());
        System.out.printf("║   - 协同样本: %-41d ║\n", getCoordinationTrainingSamples().size());
        System.out.printf("║   - 查询样本: %-41d ║\n", getQueryTrainingSamples().size());
        System.out.printf("║   - 控制样本: %-41d ║\n", getControlTrainingSamples().size());
        
        // 统计意图分布
        Map<String, Integer> intentCount = new HashMap<>();
        for (TrainingSample sample : allSamples) {
            intentCount.put(sample.expectedIntent, intentCount.getOrDefault(sample.expectedIntent, 0) + 1);
        }
        
        System.out.println("║                                                   ║");
        System.out.println("║ 意图分布：                                          ║");
        for (Map.Entry<String, Integer> entry : intentCount.entrySet()) {
            System.out.printf("║   %-22s : %-22d ║\n", entry.getKey(), entry.getValue());
        }
        
        // 统计场景分布
        Map<String, Integer> scenarioCount = new HashMap<>();
        for (TrainingSample sample : allSamples) {
            scenarioCount.put(sample.scenarioCategory, scenarioCount.getOrDefault(sample.scenarioCategory, 0) + 1);
        }
        
        System.out.println("║                                                   ║");
        System.out.println("║ 场景分布：                                          ║");
        for (Map.Entry<String, Integer> entry : scenarioCount.entrySet()) {
            System.out.printf("║   %-22s : %-22d ║\n", entry.getKey(), entry.getValue());
        }
        
        System.out.println("╚═══════════════════════════════════════════════════╝");
    }
    
    /**
     * 按类别打印训练数据
     */
    public static void printTrainingData() {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("           Agent 训练数据集详细信息");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        printCategory("导航训练数据", getNavigationTrainingSamples());
        printCategory("应急配送训练数据", getEmergencyDeliveryTrainingSamples());
        printCategory("协同训练数据", getCoordinationTrainingSamples());
        printCategory("查询训练数据", getQueryTrainingSamples());
        printCategory("控制训练数据", getControlTrainingSamples());
    }
    
    /**
     * 打印分类数据
     */
    private static void printCategory(String categoryName, List<TrainingSample> samples) {
        System.out.println("\n【" + categoryName + "】(" + samples.size() + "个样本)");
        System.out.println("─".repeat(59));
        
        for (int i = 0; i < samples.size(); i++) {
            TrainingSample s = samples.get(i);
            System.out.printf("%2d. 输入: %source\n", i + 1, s.input);
            System.out.printf("    意图: %s | 目的地: %s | 紧急度: %source\n", 
                s.expectedIntent, 
                s.expectedDestination != null ? s.expectedDestination : "N/A",
                s.emergencyLevel);
        }
    }
    
    /**
     * 主程序 - 显示训练数据
     */
    public static void main(String[] args) {
        printStatistics();
        printTrainingData();
    }
}
