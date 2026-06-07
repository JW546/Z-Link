package nlu;

import java.util.*;

/**
 * 增强版语义理解引擎
 * 基于向量空间模型和语义相似度计算
 * 实现李家兴论文中的语义对齐算法
 * 
 * 核心功能：
 * 1. 词向量语义表示
 * 2. 句子级语义编码
 * 3. 余弦相似度计算
 * 4. Jaccard相似度计算
 * 5. 同义词库集成
 * 6. 语义消歧
 */
public class SemanticEngine {
    
    // 词向量维度
    private static final int EMBEDDING_DIM = 100;
    
    // 停用词列表
    private Set<String> stopWords;
    
    // 同义词库（按语义类别组织）
    private Map<String, Set<String>> synonymLibrary;
    
    // 词频统计
    private Map<String, Integer> wordFrequency;
    
    // IDF权重
    private Map<String, Double> idfWeights;
    
    public SemanticEngine() {
        initializeStopWords();
        initializeSynonymLibrary();
        initializeWordFrequency();
    }
    
    /**
     * 初始化停用词
     */
    private void initializeStopWords() {
        stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "那", "它", "他", "她", "们", "这个", "那个", "什么", "怎么",
            "可以", "能", "请", "帮", "一下", "一下", "麻烦", "一下", "能", "吗"
        ));
    }
    
    /**
     * 初始化同义词库
     * 扩展李家兴论文的同义词组，适配民用无人交通领域
     */
    private void initializeSynonymLibrary() {
        synonymLibrary = new HashMap<>();
        
        // ===== 交通工具类同义词 =====
        Set<String> vehicleSynonyms = new HashSet<>(Arrays.asList(
            "车", "汽车", "车辆", "轿车", "比亚迪", "电动车", "新能源车",
            "无人机", "UAV", "drone", "飞行器", "eVTOL",
            "车机", "车载系统", "智能座舱"
        ));
        synonymLibrary.put("vehicle", vehicleSynonyms);
        
        // ===== 任务类型同义词 =====
        Set<String> missionSynonyms = new HashSet<>(Arrays.asList(
            "配送", "送货", "logistics", "delivery", "运输",
            "配送", "外卖", "快递", "物资运输",
            "侦察", "surveillance", "巡查", "patrol", "巡逻",
            "紧急救援", "emergency", "rescue", "急救",
            "巡检", "inspection", "检测"
        ));
        synonymLibrary.put("mission", missionSynonyms);
        
        // ===== 紧急/预警类同义词 =====
        Set<String> alertSynonyms = new HashSet<>(Arrays.asList(
            "紧急", "emergency", "urgent", "着急", "急",
            "预警", "alert", "warning", "disaster", "hazard",
            "应急", "contingency", "crisis",
            "交通事故", "accident", "碰撞", "crash",
            "道路预警", "road_alert", "拥堵", "congestion",
            "危险", "danger", "hazard", "风险", "risk"
        ));
        synonymLibrary.put("alert", alertSynonyms);
        
        // ===== 电池/电量类同义词 =====
        Set<String> batterySynonyms = new HashSet<>(Arrays.asList(
            "电池", "battery", "电量", "power", "charge_level",
            "充电", "charging", "蓄电", "能源", "energy",
            "续航", "range", "里程", "剩余电量"
        ));
        synonymLibrary.put("battery", batterySynonyms);
        
        // ===== 负载/载重类同义词 =====
        Set<String> payloadSynonyms = new HashSet<>(Arrays.asList(
            "负载", "payload", "cargo", "load", "载重",
            "货物", "goods", "包裹", "package", "重量",
            "物资", "supplies", "行李", "luggage"
        ));
        synonymLibrary.put("payload", payloadSynonyms);
        
        // ===== 目的地类同义词 =====
        Set<String> destinationSynonyms = new HashSet<>(Arrays.asList(
            "目的地", "终点", "目标", "goal", "destination",
            "机场", "airport", "火车站", "train_station",
            "医院", "hospital", "学校", "school", "university",
            "商场", "mall", "超市", "supermarket", "公园", "park"
        ));
        synonymLibrary.put("destination", destinationSynonyms);
        
        // ===== 速度/时间类同义词 =====
        Set<String> timeSynonyms = new HashSet<>(Arrays.asList(
            "快", "快速", "迅速", "高速", "fast", "quick", "rapid",
            "慢", "慢速", "低速", "slow", "gradual",
            "时间", "time", "时长", "duration", "多久",
            "预计", "estimated", "大概", "approximately"
        ));
        synonymLibrary.put("time", timeSynonyms);
        
        // ===== 费用/成本类同义词 =====
        Set<String> costSynonyms = new HashSet<>(Arrays.asList(
            "费用", "cost", "费用", "expense", "开销",
            "价格", "price", "价钱", "收费", "charge",
            "便宜", "cheap", "economical", "省钱", "经济",
            "贵", "expensive", " luxury", "高端"
        ));
        synonymLibrary.put("cost", costSynonyms);
        
        // ===== 路线/路径类同义词 =====
        Set<String> routeSynonyms = new HashSet<>(Arrays.asList(
            "路线", "route", "路径", "path", "航线",
            "高速", "highway", "高速公路", "expressway",
            "省道", "national_road", "国道", "provincial_road",
            "小路", "small_road", "捷径", "shortcut"
        ));
        synonymLibrary.put("route", routeSynonyms);
        
        // ===== 天气/环境类同义词 =====
        Set<String> weatherSynonyms = new HashSet<>(Arrays.asList(
            "天气", "weather", "气候", "climate",
            "晴", "sunny", "clear", "阴", "cloudy",
            "雨", "rain", "雨天", "雪", "snow",
            "雾", "fog", "风", "wind", "极端天气"
        ));
        synonymLibrary.put("weather", weatherSynonyms);
    }
    
    /**
     * 初始化词频统计
     */
    private void initializeWordFrequency() {
        wordFrequency = new HashMap<>();
        // 常见词的高频词
        wordFrequency.put("去", 1000);
        wordFrequency.put("到", 950);
        wordFrequency.put("我", 900);
        wordFrequency.put("要", 850);
        wordFrequency.put("快", 500);
        wordFrequency.put("慢", 480);
        wordFrequency.put("充电", 400);
        wordFrequency.put("机场", 380);
        wordFrequency.put("医院", 360);
        wordFrequency.put("火车站", 340);
    }
    
    /**
     * 中文分词（简单实现，基于词典和规则）
     * 实际应用中应使用成熟的中文分词库如jieba
     */
    public List<String> segment(String text) {
        List<String> words = new ArrayList<>();
        
        // 预处理：去除标点和空格
        text = text.replaceAll("[\\p{Punct}\\s]", "");
        
        // 简单最大正向匹配分词
        // 实际应用中应使用jieba或其他专业分词库
        String[] commonWords = {
            "西南大学", "人民医院", "市政府", "火车站", "飞机场", "体育馆",
            "图书馆", "购物中心", "商业街", "科技园", "创业园",
            "比亚迪", "大疆", "灵鸢", "仰望", "腾势",
            "高速公路", "普通公路", "省道", "国道",
            "新能源", "电动车", "无人机", "智能网联"
        };
        
        String temp = text;
        for (String word : commonWords) {
            if (temp.contains(word)) {
                words.add(word);
                temp = temp.replace(word, "");
            }
        }
        
        // 剩余字符按单字或双字切分
        for (int i = 0; i < temp.length(); i += 2) {
            int end = Math.min(i + 2, temp.length());
            String sub = temp.substring(i, end);
            if (!sub.isEmpty()) {
                words.add(sub);
            }
        }
        
        // 过滤停用词
        words.removeIf(w -> stopWords.contains(w) || w.trim().isEmpty());
        
        return words;
    }
    
    /**
     * 计算句子向量（词袋模型 + TF-IDF）
     */
    public double[] computeSentenceVector(String text) {
        List<String> words = segment(text);
        double[] vector = new double[EMBEDDING_DIM];
        
        if (words.isEmpty()) {
            return vector;
        }
        
        // 计算TF
        Map<String, Double> tf = new HashMap<>();
        for (String word : words) {
            tf.put(word, tf.getOrDefault(word, 0.0) + 1.0);
        }
        for (String word : tf.keySet()) {
            tf.put(word, tf.get(word) / words.size());
        }
        
        // 填充向量（前100个词的简化表示）
        int index = 0;
        for (String word : tf.keySet()) {
            if (index < EMBEDDING_DIM) {
                vector[index] = tf.get(word);
                index++;
            }
        }
        
        // 归一化
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
    
    /**
     * 计算余弦相似度
     */
    public double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }
    
    /**
     * 计算Jaccard相似度（基于词集合）
     */
    public double jaccardSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(segment(text1));
        Set<String> words2 = new HashSet<>(segment(text2));
        
        if (words1.isEmpty() && words2.isEmpty()) {
            return 1.0;
        }
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        // 合并同义词
        expandWithSynonyms(words1);
        expandWithSynonyms(words2);
        
        // 计算交集和并集
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 使用同义词库扩展词集合
     */
    private void expandWithSynonyms(Set<String> words) {
        Set<String> expanded = new HashSet<>(words);
        
        for (String word : words) {
            for (Map.Entry<String, Set<String>> entry : synonymLibrary.entrySet()) {
                if (entry.getValue().contains(word)) {
                    expanded.addAll(entry.getValue());
                    break;
                }
            }
        }
        
        words.clear();
        words.addAll(expanded);
    }
    
    /**
     * 语义相似度综合计算
     * 结合余弦相似度和Jaccard相似度
     */
    public double computeSemanticSimilarity(String text1, String text2) {
        // 计算向量相似度
        double[] vec1 = computeSentenceVector(text1);
        double[] vec2 = computeSentenceVector(text2);
        double cosineSim = cosineSimilarity(vec1, vec2);
        
        // 计算Jaccard相似度
        double jaccardSim = jaccardSimilarity(text1, text2);
        
        // 综合得分（加权平均）
        return 0.6 * cosineSim + 0.4 * jaccardSim;
    }
    
    /**
     * 语义消歧
     * 根据上下文选择最合适的语义解释
     */
    public String disambiguate(String word, String context) {
        // 查找可能的语义类别
        for (Map.Entry<String, Set<String>> entry : synonymLibrary.entrySet()) {
            if (entry.getValue().contains(word)) {
                // 检查上下文中是否有相关线索
                List<String> contextWords = segment(context);
                for (String ctxWord : contextWords) {
                    for (String semWord : entry.getValue()) {
                        if (semWord.equals(ctxWord) && !semWord.equals(word)) {
                            // 上下文支持这个语义类别
                            return entry.getKey();
                        }
                    }
                }
                // 没有明确上下文线索，返回默认类别
                return entry.getKey();
            }
        }
        return "unknown";
    }
    
    /**
     * 意图补全
     * 根据部分信息推断完整意图
     */
    public IntentSuggestion completeIntent(String partialText, String context) {
        IntentSuggestion suggestion = new IntentSuggestion();
        
        List<String> words = segment(partialText);
        suggestion.setExtractedKeywords(new ArrayList<>(words));
        
        // 分析上下文中的隐含意图
        List<String> contextWords = segment(context);
        
        // 检查是否有目的地但无动作
        boolean hasDestination = false;
        for (String word : contextWords) {
            if (synonymLibrary.get("destination").contains(word)) {
                hasDestination = true;
                break;
            }
        }
        
        if (hasDestination && !containsNavigationVerb(words)) {
            suggestion.addImpliedIntent("NAVIGATION");
            suggestion.setConfidenceBoost(0.2);
        }
        
        // 检查是否有时间紧迫性
        if (containsTimeUrgency(contextWords)) {
            suggestion.addImpliedIntent("EMERGENCY");
            suggestion.setConfidenceBoost(0.15);
        }
        
        return suggestion;
    }
    
    /**
     * 检查是否包含导航动词
     */
    private boolean containsNavigationVerb(List<String> words) {
        Set<String> navVerbs = new HashSet<>(Arrays.asList(
            "去", "到", "前往", "开", "去往", "驶向", "导航"
        ));
        for (String word : words) {
            if (navVerbs.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含时间紧迫性
     */
    private boolean containsTimeUrgency(List<String> words) {
        Set<String> urgencyWords = new HashSet<>(Arrays.asList(
            "快", "急", "速", "赶紧", "马上", "立刻", "尽快"
        ));
        for (String word : words) {
            if (urgencyWords.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取同义词
     */
    public Set<String> getSynonyms(String word) {
        for (Map.Entry<String, Set<String>> entry : synonymLibrary.entrySet()) {
            if (entry.getValue().contains(word)) {
                return new HashSet<>(entry.getValue());
            }
        }
        return new HashSet<>();
    }
    
    /**
     * 获取语义类别
     */
    public String getSemanticCategory(String word) {
        for (Map.Entry<String, Set<String>> entry : synonymLibrary.entrySet()) {
            if (entry.getValue().contains(word)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }
    
    /**
     * 语义匹配度计算
     * 用于匹配用户输入与模板
     */
    public SemanticMatchResult matchTemplate(String input, String template) {
        SemanticMatchResult result = new SemanticMatchResult();
        
        // 计算整体相似度
        double similarity = computeSemanticSimilarity(input, template);
        result.setOverallSimilarity(similarity);
        
        // 计算词级别的匹配
        List<String> inputWords = segment(input);
        List<String> templateWords = segment(template);
        
        int matchedCount = 0;
        for (String inputWord : inputWords) {
            for (String templateWord : templateWords) {
                if (inputWord.equals(templateWord) || 
                    getSynonyms(inputWord).contains(templateWord)) {
                    matchedCount++;
                    result.addMatchedWord(inputWord);
                    break;
                }
            }
        }
        
        result.setWordMatchRate((double) matchedCount / inputWords.size());
        
        // 综合得分
        double finalScore = 0.7 * similarity + 0.3 * result.getWordMatchRate();
        result.setFinalScore(finalScore);
        
        return result;
    }
    
    /**
     * 意图建议类
     */
    public static class IntentSuggestion {
        private List<String> extractedKeywords;
        private List<String> impliedIntents;
        private double confidenceBoost;
        
        public IntentSuggestion() {
            this.extractedKeywords = new ArrayList<>();
            this.impliedIntents = new ArrayList<>();
            this.confidenceBoost = 0.0;
        }
        
        public void addImpliedIntent(String intent) {
            if (!impliedIntents.contains(intent)) {
                impliedIntents.add(intent);
            }
        }
        
        public List<String> getExtractedKeywords() { return extractedKeywords; }
        public void setExtractedKeywords(List<String> extractedKeywords) { 
            this.extractedKeywords = extractedKeywords; 
        }
        public List<String> getImpliedIntents() { return impliedIntents; }
        public double getConfidenceBoost() { return confidenceBoost; }
        public void setConfidenceBoost(double confidenceBoost) { 
            this.confidenceBoost = confidenceBoost; 
        }
    }
    
    /**
     * 语义匹配结果类
     */
    public static class SemanticMatchResult {
        private double overallSimilarity;
        private double wordMatchRate;
        private double finalScore;
        private List<String> matchedWords;
        
        public SemanticMatchResult() {
            this.matchedWords = new ArrayList<>();
        }
        
        public void addMatchedWord(String word) {
            matchedWords.add(word);
        }
        
        public double getOverallSimilarity() { return overallSimilarity; }
        public void setOverallSimilarity(double overallSimilarity) { 
            this.overallSimilarity = overallSimilarity; 
        }
        public double getWordMatchRate() { return wordMatchRate; }
        public void setWordMatchRate(double wordMatchRate) { 
            this.wordMatchRate = wordMatchRate; 
        }
        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { 
            this.finalScore = finalScore; 
        }
        public List<String> getMatchedWords() { return matchedWords; }
    }
}
