package utils;

import java.util.*;

/**
 * 语义相似度计算器 - 实现多维语义相似度算法
 *
 * 核心算法包括：
 * 1. Jaccard相似度 - 用于计算名称相似度
 * 2. 余弦相似度 - 用于计算描述文本的向量相似度
 * 3. 复合权重计算 - 0.4×名称相似度 + 0.6×描述相似度
 *
 * 场景：民用无人交通领域的异构数据语义对齐
 */
public class SemanticSimilarityCalculator {

    /**
     * 计算Jaccard相似度（基于同义词集）
     * 公式：Jaccard(A,B) = |A∩B| / |A∪B|
     *
     * @param set1 集合1
     * @param set2 集合2
     * @return Jaccard相似度 [0,1]
     */
     /*
     假设我们需要对齐"车辆速度"和"无人机飞行速度"：
    同义词集A = {"车辆速度", "车速", "汽车速度"}
    同义词集B = {"无人机飞行速度", "飞行速度", "航速"}

    A∩B = {"速度"}  → 大小 = 1
    A∪B = {"车辆速度","车速","汽车速度","无人机飞行速度","飞行速度","航速"} → 大小 = 6
    Jaccard相似度 = 1/6 ≈ 0.1667
 */
    public static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if ((set1 == null || set1.isEmpty()) && (set2 == null || set2.isEmpty())) {
            return 1.0; // 两个空集合认为完全相似
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 1.0;
        }

        return (double) intersection.size() / union.size();
    }//Jaccard公式

    /**
     * 计算余弦相似度（用于描述文本）
     * 公式：Cosine(A,B) = (A·B) / (||A|| × ||B||)
     *
     * @param text1 描述文本1
     * @param text2 描述文本2
     * @return 余弦相似度 [0,1]
     */
     /*
     描述1: "车辆实时速度测量 用于智能网联汽车交通管理"
    描述2: "无人机飞行速度记录 用于低空经济配送系统"

    向量1 = {"车辆":1, "实时":1, "速度":1, "测量":1, "智能":1, "网联":1, "汽车":1, "交通":1, "管理":1}
    向量2 = {"无人机":1, "飞行":1, "速度":1, "记录":1, "低空":1, "经济":1, "配送":1, "系统":1}

    共同词汇只有"速度" → 点积 = 1×1 = 1
    向量模 = 各分量平方和的开方
    余弦相似度 ≈ 0.2582
 */
    public static double calculateCosineSimilarity(String text1, String text2) {
        if (text1 == null || text1.trim().isEmpty() || text2 == null || text2.trim().isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> vector1 = vectorizeText(text1);
        Map<String, Integer> vector2 = vectorizeText(text2);

        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        // 计算点积
        double dotProduct = 0.0;
        for (String word : vector1.keySet()) {
            if (vector2.containsKey(word)) {
                dotProduct += vector1.get(word) * vector2.get(word);
            }
        }

        // 计算向量的模
        double norm1 = calculateNorm(vector1);
        double norm2 = calculateNorm(vector2);

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * 文本向量化 - 将描述文本转换为词频向量
     * 使用中文bigram分词
     *
     * @param text 描述文本
     * @return 词频向量 Map<词, 频次>
     */
     
    private static Map<String, Integer> vectorizeText(String text) {
        Map<String, Integer> vector = new HashMap<>();

        if (text == null || text.trim().isEmpty()) {
            return vector;
        }

        // 移除标点符号
        String cleanText = text.replaceAll("[\\p{Punct}\\s]+", " ").trim();

        if (cleanText.isEmpty()) {
            return vector;
        }

        // 检测中文字符
        boolean hasChinese = false;
        StringBuilder currentChinese = new StringBuilder();

        for (int i = 0; i < cleanText.length(); i++) {
            char c = cleanText.charAt(i);

            // 中文字符范围判断
            if (c >= '\u4e00' && c <= '\u9fff') {
                hasChinese = true;
                currentChinese.append(c);
            } else {
                // 英文或其他字符，先处理之前的中文
                if (currentChinese.length() >= 2) {
                    processChineseBigram(currentChinese.toString(), vector);
                    currentChinese.setLength(0);
                }

                // 处理英文单词
                String word = String.valueOf(c).toLowerCase();
                if (!word.matches("[\\s]")) {
                    String currentWord = "";
                    // 向前查找完整的英文单词
                    for (int j = i - 1; j >= 0; j--) {
                        char prev = cleanText.charAt(j);
                        if (prev >= 'a' && prev <= 'z' || prev >= 'A' && prev <= 'Z') {
                            currentWord = prev + currentWord;
                        } else {
                            break;
                        }
                    }
                    if (currentWord.length() > 1) {
                        vector.put(currentWord.toLowerCase(), vector.getOrDefault(currentWord.toLowerCase(), 0) + 1);
                    }
                }
            }
        }

        // 处理剩余的中文bigram
        if (hasChinese && currentChinese.length() >= 2) {
            processChineseBigram(currentChinese.toString(), vector);
        }

        return vector;
    }

    /**
     * 处理中文bigram分词
     *
     * @param chineseText 中文文本
     * @param vector 词频向量
     */
    private static void processChineseBigram(String chineseText, Map<String, Integer> vector) {
        for (int j = 0; j < chineseText.length() - 1; j++) {
            String bigram = chineseText.substring(j, j + 2);
            vector.put(bigram, vector.getOrDefault(bigram, 0) + 1);
        }
    }

    /**
     * 计算向量的模（欧几里得范数）
     * ||A|| = sqrt(Σ a_i²)
     *
     * @param vector 词频向量
     * @return 向量的模
     */
     /* */
    private static double calculateNorm(Map<String, Integer> vector) {
        double sumOfSquares = 0.0;
        for (int count : vector.values()) {
            sumOfSquares += count * count;
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * 计算复合相似度（论文核心算法）
     * Similarity = 0.4 × NameSimilarity + 0.6 × DescriptionSimilarity
     *
     * @param name1 名称1
     * @param name2 名称2
     * @param desc1 描述1
     * @param desc2 描述2
     * @return 复合相似度 [0,1]
     */
     /*
     场景：对齐"车辆速度"和"无人机飞行速度"

1. 名称相似度 (Jaccard):
   "车辆速度" vs "无人机飞行速度"
   共同词只有"速度" → 相似度 ≈ 0.1667

2. 描述相似度 (余弦):
   "车辆实时速度测量..." vs "无人机飞行速度记录..."
   共同词：速度、测量、系统等 → 相似度 ≈ 0.2582

3. 复合相似度:
   0.4 × 0.1667 + 0.6 × 0.2582 = 0.0667 + 0.1549 = 0.2216

结论：虽然名称差异大，但描述语义相近 → 综合相似度适中

      */
    public static double calculateCompositeSimilarity(String name1, String name2,
                                             String desc1, String desc2) {
        double nameSim = calculateJaccardSimilarityByName(name1, name2);
        double descSim = calculateCosineSimilarity(desc1, desc2);

        return 0.4 * nameSim + 0.6 * descSim;
    }

    /**
     * 基于名称计算Jaccard相似度
     * 先从SynonymLibrary获取同义词集，再计算相似度
     *
     * @param name1 术语1
     * @param name2 术语2
     * @return Jaccard相似度
     */
    private static double calculateJaccardSimilarityByName(String name1, String name2) {
        Set<String> synonyms1 = new HashSet<>(SynonymLibrary.getSynonyms(name1));
        Set<String> synonyms2 = new HashSet<>(SynonymLibrary.getSynonyms(name2));

        return calculateJaccardSimilarity(synonyms1, synonyms2);
    }

    /**
     * 批量计算相似度矩阵
     *
     * @param terms 术语列表 [名称, 描述]
     * @return 相似度矩阵
     */
    public static double[][] calculateSimilarityMatrix(List<String[]> terms) {
        int n = terms.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = calculateCompositeSimilarity(
                    terms.get(i)[0],
                    terms.get(j)[0],
                    terms.get(i)[1],
                    terms.get(j)[1]
                );
            }
        }

        return matrix;
    }

    /**
     * 查找最相似的术语
     *
     * @param targetName 目标名称
     * @param targetDesc 目标描述
     * @param candidates 候选术语列表 [名称, 描述]
     * @param threshold 相似度阈值 [0,1]
     * @return 最相似的术语索引，如果没有超过阈值则返回-1
     */
    public static int findMostSimilar(String targetName, String targetDesc,
                                   List<String[]> candidates,
                                   double threshold) {
        int bestIndex = -1;
        double bestSimilarity = threshold; // 至少要达到阈值

        for (int i = 0; i < candidates.size(); i++) {
            double sim = calculateCompositeSimilarity(
                targetName, candidates.get(i)[0],
                targetDesc, candidates.get(i)[1]
            );

            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestIndex = i;
            }
        }

        return bestIndex;
    }
}
