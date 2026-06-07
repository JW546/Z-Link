package nlu;

import java.util.*;

/**
 * NLU缓存管理器
 * 实现LRU缓存和预加载机制，提高NLU处理性能
 * 
 * 核心功能：
 * 1. LRU缓存实现
 * 2. 意图缓存
 * 3. DSL缓存
 * 4. 预加载机制
 * 5. 缓存统计
 */
public class NLUCacheManager {
    
    // 意图缓存
    private LRUCache<String, NLUResult> intentCache;
    
    // DSL缓存
    private LRUCache<String, String> dslCache;
    
    // 地理编码缓存
    private LRUCache<String, GeoParser.GeoLocation> geoCache;
    
    // 缓存统计
    private CacheStatistics statistics;
    
    // 预加载的热门目的地
    private List<String> frequentDestinations;
    
    public NLUCacheManager() {
        this.intentCache = new LRUCache<>(100); // 默认100条
        this.dslCache = new LRUCache<>(50);
        this.geoCache = new LRUCache<>(200);
        this.statistics = new CacheStatistics();
        this.frequentDestinations = new ArrayList<>();
        initializeFrequentDestinations();
    }
    
    /**
     * 初始化常用目的地
     */
    private void initializeFrequentDestinations() {
        frequentDestinations.add("西南大学");
        frequentDestinations.add("机场");
        frequentDestinations.add("火车站");
        frequentDestinations.add("观音桥");
        frequentDestinations.add("解放碑");
        frequentDestinations.add("市政府");
        frequentDestinations.add("人民医院");
    }
    
    /**
     * 获取缓存的意图结果
     */
    public NLUResult getCachedIntent(String text) {
        String key = normalizeText(text);
        NLUResult result = intentCache.get(key);
        
        if (result != null) {
            statistics.recordHit("intent");
        } else {
            statistics.recordMiss("intent");
        }
        
        return result;
    }
    
    /**
     * 缓存意图结果
     */
    public void cacheIntent(String text, NLUResult result) {
        String key = normalizeText(text);
        intentCache.put(key, result);
        statistics.recordPut("intent");
    }
    
    /**
     * 获取缓存的DSL代码
     */
    public String getCachedDSL(String text) {
        String key = normalizeText(text);
        String dsl = dslCache.get(key);
        
        if (dsl != null) {
            statistics.recordHit("dsl");
        } else {
            statistics.recordMiss("dsl");
        }
        
        return dsl;
    }
    
    /**
     * 缓存DSL代码
     */
    public void cacheDSL(String text, String dsl) {
        String key = normalizeText(text);
        dslCache.put(key, dsl);
        statistics.recordPut("dsl");
    }
    
    /**
     * 获取缓存的地理编码
     */
    public GeoParser.GeoLocation getCachedGeo(String address) {
        String key = normalizeText(address);
        return geoCache.get(key);
    }
    
    /**
     * 缓存地理编码
     */
    public void cacheGeo(String address, GeoParser.GeoLocation location) {
        String key = normalizeText(address);
        geoCache.put(key, location);
    }
    
    /**
     * 预加载常用目的地
     */
    public void preloadFrequentDestinations(GeoParser geoParser) {
        for (String destination : frequentDestinations) {
            GeoParser.GeoLocation location = geoParser.geocode(destination);
            if (location != null) {
                cacheGeo(destination, location);
            }
        }
        statistics.setPreloadedItems(frequentDestinations.size());
    }
    
    /**
     * 预加载热门查询
     */
    public void preloadHotQueries(NLUProcessor processor) {
        List<String> hotQueries = Arrays.asList(
            "我要去西南大学",
            "导航到机场",
            "去最近的充电站",
            "送我去火车站",
            "到观音桥商圈"
        );
        
        for (String query : hotQueries) {
            NLUResult result = processor.processToResult(query);
            cacheIntent(query, result);
            String dsl = new DSLGenerator().generateDSL(result);
            cacheDSL(query, dsl);
        }
    }
    
    /**
     * 标准化文本（用于缓存键）
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        // 去除多余空格，转小写
        return text.trim().toLowerCase();
    }
    
    /**
     * 获取缓存统计
     */
    public CacheStatistics getStatistics() {
        statistics.setIntentCacheSize(intentCache.size());
        statistics.setDslCacheSize(dslCache.size());
        statistics.setGeoCacheSize(geoCache.size());
        return statistics;
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAll() {
        intentCache.clear();
        dslCache.clear();
        geoCache.clear();
        statistics.reset();
    }
    
    /**
     * 调整缓存大小
     */
    public void resize(int intentSize, int dslSize, int geoSize) {
        intentCache.resize(intentSize);
        dslCache.resize(dslSize);
        geoCache.resize(geoSize);
    }
    
    /**
     * LRU缓存实现
     */
    private static class LRUCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> map;
        
        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LRUCache.this.capacity;
                }
            };
        }
        
        public V get(K key) {
            return map.get(key);
        }
        
        public void put(K key, V value) {
            map.put(key, value);
        }
        
        public int size() {
            return map.size();
        }
        
        public void clear() {
            map.clear();
        }
        
        public void resize(int newCapacity) {
            if (newCapacity < capacity) {
                // 创建新的缓存并复制部分数据
                LinkedHashMap<K, V> newMap = new LinkedHashMap<>(newCapacity, 0.75f, true);
                int count = 0;
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    if (count++ >= newCapacity) break;
                    newMap.put(entry.getKey(), entry.getValue());
                }
                map.clear();
                map.putAll(newMap);
            }
        }
    }
    
    /**
     * 缓存统计类
     */
    public static class CacheStatistics {
        private int intentHits;
        private int intentMisses;
        private int dslHits;
        private int dslMisses;
        private int intentPuts;
        private int dslPuts;
        private int preloadedItems;
        private int intentCacheSize;
        private int dslCacheSize;
        private int geoCacheSize;
        
        public void recordHit(String type) {
            if ("intent".equals(type)) {
                intentHits++;
            } else if ("dsl".equals(type)) {
                dslHits++;
            }
        }
        
        public void recordMiss(String type) {
            if ("intent".equals(type)) {
                intentMisses++;
            } else if ("dsl".equals(type)) {
                dslMisses++;
            }
        }
        
        public void recordPut(String type) {
            if ("intent".equals(type)) {
                intentPuts++;
            } else if ("dsl".equals(type)) {
                dslPuts++;
            }
        }
        
        public void reset() {
            intentHits = 0;
            intentMisses = 0;
            dslHits = 0;
            dslMisses = 0;
            intentPuts = 0;
            dslPuts = 0;
            preloadedItems = 0;
        }
        
        public double getIntentHitRate() {
            int total = intentHits + intentMisses;
            return total == 0 ? 0 : (double) intentHits / total;
        }
        
        public double getDSLHitRate() {
            int total = dslHits + dslMisses;
            return total == 0 ? 0 : (double) dslHits / total;
        }
        
        public int getIntentHits() { return intentHits; }
        public int getIntentMisses() { return intentMisses; }
        public int getDslHits() { return dslHits; }
        public int getDslMisses() { return dslMisses; }
        public int getIntentPuts() { return intentPuts; }
        public int getDslPuts() { return dslPuts; }
        public int getPreloadedItems() { return preloadedItems; }
        public void setPreloadedItems(int preloadedItems) { this.preloadedItems = preloadedItems; }
        public int getIntentCacheSize() { return intentCacheSize; }
        public void setIntentCacheSize(int intentCacheSize) { this.intentCacheSize = intentCacheSize; }
        public int getDslCacheSize() { return dslCacheSize; }
        public void setDslCacheSize(int dslCacheSize) { this.dslCacheSize = dslCacheSize; }
        public int getGeoCacheSize() { return geoCacheSize; }
        public void setGeoCacheSize(int geoCacheSize) { this.geoCacheSize = geoCacheSize; }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("缓存统计：\n");
            sb.append("意图缓存命中率：").append(String.format("%.1f%%", getIntentHitRate() * 100));
            sb.append(" (").append(intentHits).append("/").append(intentHits + intentMisses).append(")\n");
            sb.append("DSL缓存命中率：").append(String.format("%.1f%%", getDSLHitRate() * 100));
            sb.append(" (").append(dslHits).append("/").append(dslHits + dslMisses).append(")\n");
            sb.append("意图缓存大小：").append(intentCacheSize).append("\n");
            sb.append("DSL缓存大小：").append(dslCacheSize).append("\n");
            sb.append("地理缓存大小：").append(geoCacheSize).append("\n");
            sb.append("预加载项目：").append(preloadedItems).append("\n");
            return sb.toString();
        }
    }
}
