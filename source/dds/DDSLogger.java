package dds;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * DDS 数据流日志管理器
 * @paper_ref 第5章 5.3 数据流监控 -> 日志系统实现
 * 
 * 功能特性:
 * 1. 记录所有发布/订阅操作（主题名、时间戳、数据内容摘要）
 * 2. 支持不同日志级别（DEBUG/INFO/WARN/ERROR）
 * 3. 记录数据吞吐量统计（消息数/秒、字节数/秒）
 * 4. 生成 JSON 格式日志文件便于后续分析
 */
public class DDSLogger {
    
    /** 日志级别枚举 */
    public enum LogLevel {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");
        
        private final int priority;
        private final String name;
        
        LogLevel(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }
        
        public int getPriority() { return priority; }
        public String getName() { return name; }
        
        public boolean isEnabled(LogLevel threshold) {
            return this.priority >= threshold.priority;
        }
    }
    
    /** 单例实例 */
    private static DDSLogger instance;
    
    /** 当前日志级别阈值 */
    private volatile LogLevel threshold = LogLevel.INFO;
    
    /** 日志输出目录 */
    private final String logDir;
    
    /** 日志文件 */
    private volatile PrintWriter logWriter;
    private volatile PrintWriter jsonWriter;
    
    /** 日期格式化 */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    /** 统计数据 */
    private final AtomicLong totalPublishCount = new AtomicLong(0);
    private final AtomicLong totalSubscribeCount = new AtomicLong(0);
    private final AtomicLong totalPublishBytes = new AtomicLong(0);
    private final AtomicLong totalSubscribeBytes = new AtomicLong(0);
    
    /** 主题级别统计 */
    private final ConcurrentHashMap<String, TopicStats> topicStats = new ConcurrentHashMap<>();
    
    /** 时间窗口统计（用于计算吞吐量） */
    private final ConcurrentHashMap<String, Deque<LogEntry>> recentEntries = new ConcurrentHashMap<>();
    private static final long WINDOW_SIZE_MS = 5000; // 5秒窗口
    private static final int MAX_ENTRIES_PER_TOPIC = 1000;
    
    /** 统计类 */
    private static class TopicStats {
        AtomicLong publishCount = new AtomicLong(0);
        AtomicLong subscribeCount = new AtomicLong(0);
        AtomicLong publishBytes = new AtomicLong(0);
        AtomicLong subscribeBytes = new AtomicLong(0);
        AtomicLong totalLatencyNs = new AtomicLong(0);
        AtomicLong latencySampleCount = new AtomicLong(0);
        long firstPublishTime = 0;
        long lastPublishTime = 0;
    }
    
    /** 日志条目 */
    private static class LogEntry {
        long timestamp;
        String type; // "publish" or "subscribe"
        String topic;
        String summary;
        int byteSize;
        Long sendTimeNs; // 用于延迟计算
        Long receiveTimeNs;
        
        LogEntry(String type, String topic, String summary, int byteSize) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.topic = topic;
            this.summary = summary;
            this.byteSize = byteSize;
        }
        
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", timestamp);
            map.put("timestamp_formatted", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp)));
            map.put("type", type);
            map.put("topic", topic);
            map.put("summary", summary);
            map.put("bytes", byteSize);
            if (sendTimeNs != null && receiveTimeNs != null) {
                map.put("latency_ms", (receiveTimeNs - sendTimeNs) / 1_000_000.0);
            }
            return map;
        }
    }
    
    /** 私有构造函数（单例模式） */
    private DDSLogger(String logDir) {
        this.logDir = logDir;
        initLogFiles();
    }
    
    /** 获取单例实例 */
    public static synchronized DDSLogger getInstance() {
        return getInstance("logs/dds");
    }
    
    public static synchronized DDSLogger getInstance(String logDir) {
        if (instance == null) {
            instance = new DDSLogger(logDir);
        }
        return instance;
    }
    
    /** 初始化日志文件 */
    private void initLogFiles() {
        try {
            // 确保目录存在
            Files.createDirectories(Paths.get(logDir));
            
            String timestamp = fileNameFormat.format(new Date());
            String logFile = logDir + "/dds_" + timestamp + ".log";
            String jsonFile = logDir + "/dds_" + timestamp + ".json";
            
            this.logWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Paths.get(logFile), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                StandardCharsets.UTF_8)));
            this.jsonWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(Paths.get(jsonFile), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                StandardCharsets.UTF_8)));
            
            // 写入 JSON 文件头
            jsonWriter.println("{");
            jsonWriter.println("  \"log_version\": \"1.0\",");
            jsonWriter.println("  \"start_time\": \"" + dateFormat.format(new Date()) + "\",");
            jsonWriter.println("  \"recordsource\": [");
            jsonWriter.flush();
            
            log("DDSLogger", LogLevel.INFO, "日志系统初始化完成，输出目录: " + logDir);
            log("DDSLogger", LogLevel.INFO, "文本日志: " + logFile);
            log("DDSLogger", LogLevel.INFO, "JSON日志: " + jsonFile);
            
        } catch (IOException e) {
            System.err.println("[DDSLogger] 初始化失败: " + e.getMessage());
        }
    }
    
    /** 设置日志级别阈值 */
    public void setThreshold(LogLevel level) {
        this.threshold = level;
        log("DDSLogger", LogLevel.INFO, "日志级别阈值设置为: " + level.getName());
    }
    
    /** 获取当前日志级别 */
    public LogLevel getThreshold() {
        return threshold;
    }
    
    /**
     * 记录发布操作
     */
    public void logPublish(String topic, String dataSummary, int byteSize, Long sendTimeNs) {
        totalPublishCount.incrementAndGet();
        totalPublishBytes.addAndGet(byteSize);
        
        // 更新主题统计
        TopicStats stats = topicStats.computeIfAbsent(topic, k -> new TopicStats());
        stats.publishCount.incrementAndGet();
        stats.publishBytes.addAndGet(byteSize);
        
        long now = System.currentTimeMillis();
        if (stats.firstPublishTime == 0) {
            stats.firstPublishTime = now;
        }
        stats.lastPublishTime = now;
        
        // 添加到时间窗口
        addToWindow(topic, "publish", topic, dataSummary, byteSize, sendTimeNs, now * 1_000_000);
        
        // 写入日志
        String message = String.format("[%s] 主题=%s 摘要=%s 大小=%d bytes", 
            "PUBLISH", topic, truncateSummary(dataSummary), byteSize);
        writeLog("DDS", LogLevel.INFO, message);
    }
    
    /**
     * 记录订阅操作
     */
    public void logSubscribe(String topic, String dataSummary, int byteSize, Long receiveTimeNs) {
        totalSubscribeCount.incrementAndGet();
        totalSubscribeBytes.addAndGet(byteSize);
        
        // 更新主题统计
        TopicStats stats = topicStats.computeIfAbsent(topic, k -> new TopicStats());
        stats.subscribeCount.incrementAndGet();
        stats.subscribeBytes.addAndGet(byteSize);
        
        // 添加到时间窗口
        addToWindow(topic, "subscribe", topic, dataSummary, byteSize, null, receiveTimeNs);
        
        // 写入日志
        String message = String.format("[%s] 主题=%s 摘要=%s 大小=%d bytes", 
            "SUBSCRIBE", topic, truncateSummary(dataSummary), byteSize);
        writeLog("DDS", LogLevel.INFO, message);
    }
    
    /**
     * 记录延迟测量
     */
    public void recordLatency(String topic, long sendTimeNs, long receiveTimeNs) {
        TopicStats stats = topicStats.get(topic);
        if (stats != null) {
            long latencyNs = receiveTimeNs - sendTimeNs;
            stats.totalLatencyNs.addAndGet(latencyNs);
            stats.latencySampleCount.incrementAndGet();
        }
    }
    
    /** 添加到时间窗口 */
    private void addToWindow(String topic, String type, String topicName, String summary, 
                             int byteSize, Long sendTimeNs, Long receiveTimeNs) {
        Deque<LogEntry> window = recentEntries.computeIfAbsent(topic, k -> new ConcurrentLinkedDeque<>());
        LogEntry entry = new LogEntry(type, topicName, summary, byteSize);
        entry.sendTimeNs = sendTimeNs;
        entry.receiveTimeNs = receiveTimeNs;
        
        window.addLast(entry);
        
        // 清理旧条目
        long cutoff = System.currentTimeMillis() - WINDOW_SIZE_MS;
        while (!window.isEmpty() && window.peekFirst().timestamp < cutoff) {
            window.removeFirst();
        }
        
        // 限制最大条目数
        while (window.size() > MAX_ENTRIES_PER_TOPIC) {
            window.removeFirst();
        }
    }
    
    /**
     * 通用日志方法
     */
    public void log(String source, LogLevel level, String message) {
        if (!level.isEnabled(threshold)) return;
        
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] [%s] [%s] %s", timestamp, level.getName(), source, message);
        
        // 控制台输出
        if (level == LogLevel.ERROR) {
            System.err.println(logLine);
        } else {
            System.out.println(logLine);
        }
        
        // 文件输出
        if (logWriter != null) {
            logWriter.println(logLine);
            logWriter.flush();
        }
    }
    
    /** 快捷 DEBUG 日志 */
    public void debug(String source, String message) {
        log(source, LogLevel.DEBUG, message);
    }
    
    /** 快捷 INFO 日志 */
    public void info(String source, String message) {
        log(source, LogLevel.INFO, message);
    }
    
    /** 快捷 WARN 日志 */
    public void warn(String source, String message) {
        log(source, LogLevel.WARN, message);
    }
    
    /** 快捷 ERROR 日志 */
    public void error(String source, String message) {
        log(source, LogLevel.ERROR, message);
    }
    
    /** 写入日志（内部使用） */
    private void writeLog(String source, LogLevel level, String message) {
        if (!level.isEnabled(threshold)) return;
        
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] [%s] [%s] %s", timestamp, level.getName(), source, message);
        
        if (logWriter != null) {
            logWriter.println(logLine);
            logWriter.flush();
        }
        
        // 同时写入 JSON
        writeJsonLog(level.getName().toLowerCase(), message);
    }
    
    /** 写入 JSON 格式日志 */
    private void writeJsonLog(String level, String message) {
        if (jsonWriter == null) return;
        
        try {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("timestamp", System.currentTimeMillis());
            record.put("level", level);
            record.put("message", message);
            
            String json = mapToJson(record);
            jsonWriter.println("    " + json + ",");
            jsonWriter.flush();
        } catch (Exception e) {
            System.err.println("[DDSLogger] JSON写入失败: " + e.getMessage());
        }
    }
    
    /** Map转JSON字符串（简化版） */
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    /** 截断摘要（过长时截断） */
    private String truncateSummary(String summary) {
        if (summary == null) return "null";
        if (summary.length() <= 50) return summary;
        return summary.substring(0, 47) + "...";
    }
    
    /**
     * 获取吞吐量统计（消息数/秒、字节数/秒）
     */
    public ThroughputStats getThroughput(String topic) {
        Deque<LogEntry> window = recentEntries.get(topic);
        if (window == null || window.isEmpty()) {
            return new ThroughputStats(0, 0, 0, 0);
        }
        
        long now = System.currentTimeMillis();
        long oldest = window.peekFirst().timestamp;
        long durationMs = now - oldest;
        
        if (durationMs <= 0) {
            return new ThroughputStats(0, 0, 0, 0);
        }
        
        long durationSec = durationMs / 1000;
        
        long publishCount = window.stream()
            .filter(e -> "publish".equals(e.type))
            .count();
        long subscribeCount = window.stream()
            .filter(e -> "subscribe".equals(e.type))
            .count();
        long publishBytes = window.stream()
            .filter(e -> "publish".equals(e.type))
            .mapToLong(e -> e.byteSize)
            .sum();
        long subscribeBytes = window.stream()
            .filter(e -> "subscribe".equals(e.type))
            .mapToLong(e -> e.byteSize)
            .sum();
        
        return new ThroughputStats(
            publishCount / durationSec,
            subscribeCount / durationSec,
            publishBytes / durationSec,
            subscribeBytes / durationSec
        );
    }
    
    /** 吞吐量统计类 */
    public static class ThroughputStats {
        public final double publishPerSec;
        public final double subscribePerSec;
        public final double publishBytesPerSec;
        public final double subscribeBytesPerSec;
        
        public ThroughputStats(double publishPerSec, double subscribePerSec, 
                               double publishBytesPerSec, double subscribeBytesPerSec) {
            this.publishPerSec = publishPerSec;
            this.subscribePerSec = subscribePerSec;
            this.publishBytesPerSec = publishBytesPerSec;
            this.subscribeBytesPerSec = subscribeBytesPerSec;
        }
        
        @Override
        public String toString() {
            return String.format("吞吐量[pub=%.2f msg/s (%.2f KB/s), sub=%.2f msg/s (%.2f KB/s)]",
                publishPerSec, publishBytesPerSec / 1024,
                subscribePerSec, subscribeBytesPerSec / 1024);
        }
    }
    
    /**
     * 生成监控报告
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n═══════════════════════════════════════════════════════\n");
        sb.append("           DDS 数据流日志与监控报告\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");
        
        // 总体统计
        sb.append("【总体统计】\n");
        sb.append(String.format("  发布消息总数:     %,d\n", totalPublishCount.get()));
        sb.append(String.format("  订阅消息总数:     %,d\n", totalSubscribeCount.get()));
        sb.append(String.format("  发布总字节数:     %,d bytes (%.2f MB)\n", 
            totalPublishBytes.get(), totalPublishBytes.get() / 1024.0 / 1024.0));
        sb.append(String.format("  订阅总字节数:     %,d bytes (%.2f MB)\n", 
            totalSubscribeBytes.get(), totalSubscribeBytes.get() / 1024.0 / 1024.0));
        
        // 主题统计
        sb.append("\n【主题级别统计】\n");
        if (topicStats.isEmpty()) {
            sb.append("  (暂无数据)\n");
        } else {
            for (Map.Entry<String, TopicStats> entry : topicStats.entrySet()) {
                String topic = entry.getKey();
                TopicStats stats = entry.getValue();
                
                sb.append(String.format("\n  主题: %source\n", topic));
                sb.append(String.format("    发布: %,d 条 (%.2f KB)\n", 
                    stats.publishCount.get(), stats.publishBytes.get() / 1024.0));
                sb.append(String.format("    订阅: %,d 条 (%.2f KB)\n", 
                    stats.subscribeCount.get(), stats.subscribeBytes.get() / 1024.0));
                
                // 计算平均延迟
                if (stats.latencySampleCount.get() > 0) {
                    double avgLatencyMs = (stats.totalLatencyNs.get() / 1_000_000.0) 
                        / stats.latencySampleCount.get();
                    sb.append(String.format("    平均延迟: %.2f msource\n", avgLatencyMs));
                }
                
                // 吞吐量
                ThroughputStats throughput = getThroughput(topic);
                sb.append(String.format("    %source\n", throughput));
            }
        }
        
        // 活跃主题
        sb.append("\n【活跃主题】\n");
        if (recentEntries.isEmpty()) {
            sb.append("  (暂无活跃数据流)\n");
        } else {
            for (String topic : recentEntries.keySet()) {
                Deque<LogEntry> window = recentEntries.get(topic);
                if (!window.isEmpty()) {
                    sb.append(String.format("  • %s (最近 %d 条)\n", topic, window.size()));
                }
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════════\n");
        return sb.toString();
    }
    
    /** 打印报告到控制台 */
    public void printReport() {
        String report = generateReport();
        System.out.println(report);
    }
    
    /** 获取总发布计数 */
    public long getTotalPublishCount() {
        return totalPublishCount.get();
    }
    
    /** 获取总订阅计数 */
    public long getTotalSubscribeCount() {
        return totalSubscribeCount.get();
    }
    
    /** 获取总发布字节数 */
    public long getTotalPublishBytes() {
        return totalPublishBytes.get();
    }
    
    /** 获取总订阅字节数 */
    public long getTotalSubscribeBytes() {
        return totalSubscribeBytes.get();
    }
    
    /** 获取主题统计 */
    public Map<String, TopicStats> getTopicStats() {
        return new HashMap<>(topicStats);
    }
    
    /** 重置统计 */
    public void resetStats() {
        totalPublishCount.set(0);
        totalSubscribeCount.set(0);
        totalPublishBytes.set(0);
        totalSubscribeBytes.set(0);
        topicStats.clear();
        recentEntries.clear();
        log("DDSLogger", LogLevel.INFO, "统计数据已重置");
    }
    
    /** 关闭日志系统 */
    public synchronized void shutdown() {
        log("DDSLogger", LogLevel.INFO, "关闭 DDS 日志系统");
        
        // 写入 JSON 文件尾
        if (jsonWriter != null) {
            jsonWriter.println("  ],");
            jsonWriter.println("  \"end_time\": \"" + dateFormat.format(new Date()) + "\",");
            jsonWriter.println("  \"total_publish\": " + totalPublishCount.get() + ",");
            jsonWriter.println("  \"total_subscribe\": " + totalSubscribeCount.get());
            jsonWriter.println("}");
            jsonWriter.close();
        }
        
        if (logWriter != null) {
            logWriter.close();
        }
        
        // 打印最终报告
        printReport();
    }
    
    /**
     * 监控回调接口 - 用于集成到 DDSManager
     */
    public interface PublishCallback {
        void onPublish(String topic, String dataSummary, int byteSize);
    }
    
    public interface SubscribeCallback {
        void onSubscribe(String topic, String dataSummary, int byteSize);
    }
}
