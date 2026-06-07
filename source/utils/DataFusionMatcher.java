package utils;

import java.util.*;

/**
 * 数据融合匹配框架 - 实现异构数据融合算法
 * 
 * 核心功能:
 * 1. 二分图建模 - 订阅方需求集D和发布方数据集S映射为二分图顶点
 * 2. 匈牙利算法 - 求解二分图最大匹配
 * 3. 约束验证 - 硬约束(安全等级) + 软约束(单位、精度)分层处理
 * 4. 增量式流水线处理框架
 * 
 * 场景: 民用无人交通领域的跨协议数据融合匹配
 */

/**
 * 数据项 - 表示发布方或订阅方的数据
 */
class DataItem {
    String id;              // 唯一标识符
    String name;            // 数据名称
    String description;     // 描述文本
    String protocol;         // 来源协议 (BYD-Protocol, DJI-Protocol等)
    String unit;            // 数据单位
    double value;           // 数据值
    Map<String, Object> metadata; // 元数据 (约束、QoS等)
    
    public DataItem(String id, String name, String description, 
                   String protocol, String unit, double value) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.protocol = protocol;
        this.unit = unit;
        this.value = value;
        this.metadata = new HashMap<>();
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s:%s=%.2f%s]", 
            id, protocol, name, value, unit);
    }
}

/**
 * 匹配结果 - 记录匹配对和相似度
 */
class MatchResult {
    DataItem subscription;  // 订阅方需求
    DataItem publication;  // 发布方数据
    double similarity;     // 相似度
    boolean converted;      // 是否需要单位转换
    
    public MatchResult(DataItem subscription, DataItem publication, 
                      double similarity, boolean converted) {
        this.subscription = subscription;
        this.publication = publication;
        this.similarity = similarity;
        this.converted = converted;
    }
    
    @Override
    public String toString() {
        return String.format("%s -> %s (sim=%.3f, converted=%s)",
            subscription.id, publication.id, similarity, converted);
    }
}

/**
 * 数据融合匹配引擎
 */
public class DataFusionMatcher {
    
    // 订阅方需求集 D
    private List<DataItem> subscriptions;
    // 发布方数据集 S
    private List<DataItem> publications;
    // 相似度矩阵
    private double[][] similarityMatrix;
    // 匹配结果列表
    private List<MatchResult> matches;
    
    // 配置参数
    private double similarityThreshold = 0.6;  // 相似度阈值
    private boolean enableHardConstraints = true; // 是否启用硬约束
    private boolean enableSoftConstraints = true; // 是否启用软约束
    
    /**
     * 构造函数
     */
    public DataFusionMatcher() {
        this.subscriptions = new ArrayList<>();
        this.publications = new ArrayList<>();
        this.matches = new ArrayList<>();
    }
    
    /**
     * 添加订阅方需求
     */
    public void addSubscription(DataItem subscription) {
        subscriptions.add(subscription);
    }
    
    /**
     * 创建订阅方需求 (工厂方法,带元数据)
     */
    public void addSubscription(String id, String name, String description, 
                               String protocol, String unit, double value,
                               Map<String, Object> metadata) {
        DataItem item = new DataItem(id, name, description, protocol, unit, value);
        if (metadata != null) {
            item.metadata.putAll(metadata);
        }
        subscriptions.add(item);
    }
    
    /**
     * 添加发布方数据
     */
    public void addPublication(DataItem publication) {
        publications.add(publication);
    }
    
    /**
     * 创建发布方数据 (工厂方法,带元数据)
     */
    public void addPublication(String id, String name, String description, 
                              String protocol, String unit, double value,
                              Map<String, Object> metadata) {
        DataItem item = new DataItem(id, name, description, protocol, unit, value);
        if (metadata != null) {
            item.metadata.putAll(metadata);
        }
        publications.add(item);
    }
    
    /**
     * 获取订阅方列表
     */
    public List<DataItem> getSubscriptions() {
        return subscriptions;
    }
    
    /**
     * 获取发布方列表
     */
    public List<DataItem> getPublications() {
        return publications;
    }
    
    /**
     * 二分图建模 - 构建相似度矩阵
     * 
     * 矩阵 M[i][j] = similarity(subscriptions[i], publications[j])
     */
    public void buildBipartiteGraph() {
        int n = subscriptions.size();
        int m = publications.size();
        
        similarityMatrix = new double[n][m];
        
        System.out.println("\n========== 二分图建模 ==========");
        System.out.println("订阅方数量: " + n + ", 发布方数量: " + m);
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                DataItem sub = subscriptions.get(i);
                DataItem pub = publications.get(j);
                
                // 计算语义相似度
                double sim = SemanticSimilarityCalculator.calculateCompositeSimilarity(
                    sub.name, pub.name,
                    sub.description, pub.description
                );
                
                // 应用约束过滤
                if (enableHardConstraints) {
                    if (!checkHardConstraints(sub, pub)) {
                        sim = 0.0; // 硬约束不满足,相似度置0
                    }
                }
                
                if (enableSoftConstraints) {
                    double penalty = calculateSoftConstraintPenalty(sub, pub);
                    sim *= (1.0 - penalty); // 软约束不满足,降低相似度
                }
                
                similarityMatrix[i][j] = sim;
                
                if (sim > 0) {
                    System.out.println(String.format("  边: %s -> %s, 相似度: %.4f", 
                        sub.id, pub.id, sim));
                }
            }
        }
        System.out.println("========== 二分图建模完成 ==========\n");
    }
    
    /**
     * 检查硬约束 - 必须满足才能匹配
     * 
     * 硬约束包括:
     * - 安全等级一致性
     * - 数据类型兼容性
     */
    private boolean checkHardConstraints(DataItem sub, DataItem pub) {
        // 安全等级约束
        Integer subSecurity = (Integer) sub.getMetadata("securityLevel");
        Integer pubSecurity = (Integer) pub.getMetadata("securityLevel");
        
        if (subSecurity != null && pubSecurity != null) {
            if (pubSecurity < subSecurity) {
                System.out.println("    [硬约束失败] 安全等级不匹配: " + 
                    sub.id + "(" + subSecurity + ") vs " + 
                    pub.id + "(" + pubSecurity + ")");
                return false;
            }
        }
        
        // 数据类型约束
        String subType = (String) sub.getMetadata("dataType");
        String pubType = (String) pub.getMetadata("dataType");
        
        if (subType != null && pubType != null) {
            if (!isTypeCompatible(subType, pubType)) {
                System.out.println("    [硬约束失败] 数据类型不兼容: " + 
                    subType + " vs " + pubType);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 计算软约束惩罚 - 降低相似度
     * 
     * 软约束包括:
     * - 单位不匹配 (需要转换)
     * - 精度不足
     */
    private double calculateSoftConstraintPenalty(DataItem sub, DataItem pub) {
        double penalty = 0.0;
        
        // 单位约束
        if (!sub.unit.equals(pub.unit)) {
            penalty += 0.1; // 需要单位转换,惩罚10%
        }
        
        // 精度约束
        Double subPrecision = (Double) sub.getMetadata("precision");
        Double pubPrecision = (Double) pub.getMetadata("precision");
        
        if (subPrecision != null && pubPrecision != null) {
            if (pubPrecision < subPrecision) {
                penalty += 0.2; // 精度不足,惩罚20%
            }
        }
        
        return penalty;
    }
    
    /**
     * 检查数据类型兼容性
     */
    private boolean isTypeCompatible(String type1, String type2) {
        return type1.equals(type2) || 
               (type1.equals("float") && type2.equals("double")) ||
               (type1.equals("int") && type2.equals("long"));
    }
    
    /**
     * 匈牙利算法 - 求解二分图最大权重匹配
     * 
     * 时间复杂度: O(n³)
     * 
     * @param maxMatch 是否最大化匹配数量 (否则最大化总权重)
     * @return 匹配对数组, matchResult[i] = j 表示订阅i匹配到发布j
     */
    public int[] hungarianAlgorithm(boolean maxMatch) {
        int n = subscriptions.size();
        int m = publications.size();
        
        // 确保订阅方数量 <= 发布方数量
        int size = Math.max(n, m);
        double[][] costMatrix = new double[size][size];
        
        // 填充成本矩阵 (使用负相似度,因为匈牙利算法求最小化)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i < n && j < m) {
                    // 相似度低于阈值则无法匹配
                    if (similarityMatrix[i][j] >= similarityThreshold) {
                        costMatrix[i][j] = -similarityMatrix[i][j];
                    } else {
                        costMatrix[i][j] = Double.MAX_VALUE;
                    }
                } else {
                    costMatrix[i][j] = Double.MAX_VALUE;
                }
            }
        }
        
        // 实现匈牙利算法
        return hungarianAlgorithmImpl(costMatrix);
    }
    
    /**
     * 匈牙利算法实现 (简化版 - KM算法)
     */
    private int[] hungarianAlgorithmImpl(double[][] cost) {
        int n = cost.length;
        
        // 标记数组
        int[] match = new int[n]; // match[j] = i, 匹配状态
        Arrays.fill(match, -1);
        
        double[] lx = new double[n]; // 左边顶点标记
        double[] ly = new double[n]; // 右边顶点标记
        boolean[] visX = new boolean[n]; // 左边访问标记
        boolean[] visY = new boolean[n]; // 右边访问标记
        
        // 初始化标记
        for (int i = 0; i < n; i++) {
            lx[i] = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (cost[i][j] < lx[i]) {
                    lx[i] = cost[i][j];
                }
            }
        }
        
        // 对每个左边顶点寻找增广路
        for (int i = 0; i < n; i++) {
            while (true) {
                Arrays.fill(visX, false);
                Arrays.fill(visY, false);
                
                if (dfs(i, cost, lx, ly, visX, visY, match)) {
                    break; // 找到增广路
                }
                
                // 更新标记
                double delta = Double.MAX_VALUE;
                for (int x = 0; x < n; x++) {
                    if (visX[x]) {
                        for (int y = 0; y < n; y++) {
                            if (!visY[y]) {
                                delta = Math.min(delta, lx[x] + ly[y] - cost[x][y]);
                            }
                        }
                    }
                }
                
                for (int j = 0; j < n; j++) {
                    if (visX[j]) lx[j] -= delta;
                    if (visY[j]) ly[j] += delta;
                }
            }
        }
        
        return match;
    }
    
    /**
     * DFS寻找增广路
     */
    private boolean dfs(int x, double[][] cost, double[] lx, double[] ly,
                        boolean[] visX, boolean[] visY, int[] match) {
        visX[x] = true;
        int n = cost.length;
        
        for (int y = 0; y < n; y++) {
            if (!visY[y] && Math.abs(lx[x] + ly[y] - cost[x][y]) < 1e-9) {
                visY[y] = true;
                if (match[y] == -1 || dfs(match[y], cost, lx, ly, visX, visY, match)) {
                    match[y] = x;
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 执行匹配流程
     */
    public void executeMatching(boolean maxMatch) {
        System.out.println("\n========== 开始数据融合匹配 ==========");
        
        // 1. 构建二分图
        buildBipartiteGraph();
        
        // 2. 执行匈牙利算法
        System.out.println("========== 执行匈牙利算法 ==========");
        int[] match = hungarianAlgorithm(maxMatch);
        
        // 3. 生成匹配结果
        matches.clear();
        for (int j = 0; j < match.length; j++) {
            int i = match[j];
            if (i >= 0 && i < subscriptions.size() && j < publications.size()) {
                double sim = similarityMatrix[i][j];
                if (sim >= similarityThreshold) {
                    // 检查是否需要单位转换
                    boolean converted = !subscriptions.get(i).unit.equals(publications.get(j).unit);
                    
                    matches.add(new MatchResult(
                        subscriptions.get(i),
                        publications.get(j),
                        sim,
                        converted
                    ));
                }
            }
        }
        
        System.out.println("========== 匹配完成 ==========\n");
    }
    
    /**
     * 获取匹配结果
     */
    public List<MatchResult> getMatches() {
        return matches;
    }
    
    /**
     * 打印匹配结果
     */
    public void printMatchResults() {
        if (matches.isEmpty()) {
            System.out.println("  无匹配结果");
            return;
        }

        System.out.println("  匹配结果 (" + matches.size() + " 对):");
        for (MatchResult match : matches) {
            System.out.println("    " + match.subscription.name + " → " + match.publication.name);
        }
    }
    
    /**
     * 计算并打印匹配评价指标
     */
    private void printMatchMetrics() {
        int matchedCount = matches.size();
        int totalSubs = subscriptions.size();
        double matchRate = totalSubs > 0 ? (double) matchedCount / totalSubs : 0;
        
        double avgSimilarity = 0;
        int convertedCount = 0;
        
        for (MatchResult match : matches) {
            avgSimilarity += match.similarity;
            if (match.converted) convertedCount++;
        }
        
        if (matchedCount > 0) {
            avgSimilarity /= matchedCount;
        }
        
        System.out.println("\n[评价指标]");
        System.out.println("  匹配率: " + String.format("%.2f%%", matchRate * 100));
        System.out.println("  平均相似度: " + String.format("%.4f", avgSimilarity));
        System.out.println("  需要转换的匹配: " + convertedCount);
    }
    
    /**
     * 设置相似度阈值
     */
    public void setSimilarityThreshold(double threshold) {
        this.similarityThreshold = threshold;
    }
    
    /**
     * 启用/禁用硬约束
     */
    public void setHardConstraints(boolean enable) {
        this.enableHardConstraints = enable;
    }
    
    /**
     * 启用/禁用软约束
     */
    public void setSoftConstraints(boolean enable) {
        this.enableSoftConstraints = enable;
    }
    
    /**
     * 清空所有数据
     */
    public void clear() {
        subscriptions.clear();
        publications.clear();
        matches.clear();
        similarityMatrix = null;
    }
}
