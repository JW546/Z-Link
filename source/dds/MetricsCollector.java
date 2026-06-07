package dds;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DDS 实时指标收集器
 * @paper_ref 第5章 5.3 数据流监控 -> 指标采集实现
 * 
 * 功能特性:
 * 1. 实时统计 DDS 主题的发布/订阅指标
 * 2. 计算延迟（发送时间 vs 接收时间）
 * 3. 输出监控报告
 */
public class MetricsCollector {
    
    /** 单例实例 */
    private static volatile MetricsCollector instance;
    
    /** 指标收集间隔（毫秒） */
    private static final long COLLECTION_INTERVAL_MS = 1000;
    
    /** 时间窗口大小（用于滑动平均） */
    private static final int WINDOW_SIZE = 60; // 60秒滑动窗口
    
    /** 主题指标数据 */
    private final ConcurrentHashMap<String, TopicMetrics> topicMetrics = new ConcurrentHashMap<>();
    
    /** 全局指标 */
    private final GlobalMetrics globalMetrics = new GlobalMetrics();
    
    /** 收集线程 */
    private volatile boolean collecting = false;
    private Thread collectorThread;
    
    /** 时间戳锁 */
    private final ReadWriteLock timestampLock = new ReentrantReadWriteLock();
    private long lastCollectTime = 0;
    private long startTime = 0;
    
    /** 监控回调 */
    private final List<MetricsUpdateCallback> callbacks = new CopyOnWriteArrayList<>();
    
    /** ==================== 内部数据类 ==================== */
    
    /**
     * 单个主题的指标数据
     */
    public static class TopicMetrics {
        // 基础计数
        public final AtomicLong publishCount = new AtomicLong(0);
        public final AtomicLong subscribeCount = new AtomicLong(0);
        public final AtomicLong publishBytes = new AtomicLong(0);
        public final AtomicLong subscribeBytes = new AtomicLong(0);
        
        // 延迟统计
        public final AtomicLong totalLatencyNs = new AtomicLong(0);
        public final AtomicLong latencySamples = new AtomicLong(0);
        public final AtomicLong minLatencyNs = new AtomicLong(Long.MAX_VALUE);
        public final AtomicLong maxLatencyNs = new AtomicLong(0);
        
        // 时间序列数据（用于滑动平均）
        private final Deque<TimeSeriesPoint> timeSeries = new ConcurrentLinkedDeque<>();
        
        // 时间戳
        public volatile long firstPublishTime = 0;
        public volatile long lastPublishTime = 0;
        public volatile long lastSubscribeTime = 0;
        
        // 错误计数
        public final AtomicLong errorCount = new AtomicLong(0);
        
        /** 添加时间序列点 */
        public void addTimeSeriesPoint(double value) {
            long now = System.currentTimeMillis();
            timeSeries.addLast(new TimeSeriesPoint(now, value));
            
            // 清理过期数据（保留60秒）
            long cutoff = now - WINDOW_SIZE * 1000L;
            while (!timeSeries.isEmpty() && timeSeries.peekFirst().timestamp < cutoff) {
                timeSeries.removeFirst();
            }
        }
        
        /** 计算滑动平均 */
        public double getSlidingAverage() {
            if (timeSeries.isEmpty()) return 0;
            double sum = timeSeries.stream().mapToDouble(p -> p.value).sum();
            return sum / timeSeries.size();
        }
        
        /** 获取吞吐量（消息/秒） */
        public double getPublishThroughput() {
            if (timeSeries.isEmpty() || lastPublishTime == 0 || firstPublishTime == 0) {
                return 0;
            }
            double duration = (lastPublishTime - firstPublishTime) / 1000.0;
            return duration > 0 ? publishCount.get() / duration : 0;
        }
    }
    
    /** 时间序列数据点 */
    private static class TimeSeriesPoint {
        final long timestamp;
        final double value;
        
        TimeSeriesPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    
    /**
     * 全局指标数据
     */
    public static class GlobalMetrics {
        public final AtomicLong totalPublishCount = new AtomicLong(0);
        public final AtomicLong totalSubscribeCount = new AtomicLong(0);
        public final AtomicLong totalPublishBytes = new AtomicLong(0);
        public final AtomicLong totalSubscribeBytes = new AtomicLong(0);
        public final AtomicLong totalErrors = new AtomicLong(0);
        
        // 峰值统计
        public final AtomicLong peakPublishPerSec = new AtomicLong(0);
        public final AtomicLong peakSubscribePerSec = new AtomicLong(0);
        public final AtomicLong peakBytesPerSec = new AtomicLong(0);
        
        // 运行时间
        public volatile long startTime = 0;
        public volatile long lastUpdateTime = 0;
    }
    
    /**
     * 监控报告数据
     */
    public static class MonitoringReport {
        public final long timestamp;
        public final long uptimeSeconds;
        public final long totalTopics;
        public final long totalPublishCount;
        public final long totalSubscribeCount;
        public final double avgPublishPerSec;
        public final double avgSubscribePerSec;
        public final double avgBytesPerSec;
        public final double avgLatencyMs;
        public final List<TopicReport> topicReports;
        
        public MonitoringReport(long timestamp, long uptimeSeconds, long totalTopics,
                               long totalPublishCount, long totalSubscribeCount,
                               double avgPublishPerSec, double avgSubscribePerSec,
                               double avgBytesPerSec, double avgLatencyMs,
                               List<TopicReport> topicReports) {
            this.timestamp = timestamp;
            this.uptimeSeconds = uptimeSeconds;
            this.totalTopics = totalTopics;
            this.totalPublishCount = totalPublishCount;
            this.totalSubscribeCount = totalSubscribeCount;
            this.avgPublishPerSec = avgPublishPerSec;
            this.avgSubscribePerSec = avgSubscribePerSec;
            this.avgBytesPerSec = avgBytesPerSec;
            this.avgLatencyMs = avgLatencyMs;
            this.topicReports = topicReports;
        }
    }
    
    /**
     * 单个主题的监控报告
     */
    public static class TopicReport {
        public final String topic;
        public final long publishCount;
        public final long subscribeCount;
        public final double publishPerSec;
        public final double subscribePerSec;
        public final double avgLatencyMs;
        public final double minLatencyMs;
        public final double maxLatencyMs;
        
        public TopicReport(String topic, long publishCount, long subscribeCount,
                          double publishPerSec, double subscribePerSec,
                          double avgLatencyMs, double minLatencyMs, double maxLatencyMs) {
            this.topic = topic;
            this.publishCount = publishCount;
            this.subscribeCount = subscribeCount;
            this.publishPerSec = publishPerSec;
            this.subscribePerSec = subscribePerSec;
            this.avgLatencyMs = avgLatencyMs;
            this.minLatencyMs = minLatencyMs;
            this.maxLatencyMs = maxLatencyMs;
        }
    }
    
    /**
     * 监控更新回调接口
     */
    public interface MetricsUpdateCallback {
        void onMetricsUpdate(MonitoringReport report);
    }
    
    /** ==================== 单例与初始化 ==================== */
    
    private MetricsCollector() {}
    
    /** 获取单例实例 */
    public static MetricsCollector getInstance() {
        if (instance == null) {
            synchronized (MetricsCollector.class) {
                if (instance == null) {
                    instance = new MetricsCollector();
                }
            }
        }
        return instance;
    }
    
    /** 启动指标收集 */
    public synchronized void start() {
        if (collecting) {
            System.out.println("[MetricsCollector] 已启动");
            return;
        }
        
        collecting = true;
        globalMetrics.startTime = System.currentTimeMillis();
        startTime = System.currentTimeMillis();
        
        collectorThread = new Thread(this::collectLoop, "MetricsCollector");
        collectorThread.setDaemon(true);
        collectorThread.start();
        
        System.out.println("[MetricsCollector] 启动 ✓");
        DDSLogger.getInstance().info("MetricsCollector", "指标收集器已启动");
    }
    
    /** 停止指标收集 */
    public synchronized void stop() {
        if (!collecting) return;
        
        collecting = false;
        if (collectorThread != null) {
            collectorThread.interrupt();
            try {
                collectorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("[MetricsCollector] 停止 ✓");
        DDSLogger.getInstance().info("MetricsCollector", "指标收集器已停止");
    }
    
    /** 收集循环 */
    private void collectLoop() {
        while (collecting) {
            try {
                Thread.sleep(COLLECTION_INTERVAL_MS);
                
                // 计算当前吞吐量
                calculateThroughput();
                
                // 通知回调
                notifyCallbacks();
                
                timestampLock.writeLock().lock();
                lastCollectTime = System.currentTimeMillis();
                timestampLock.writeLock().unlock();
                
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /** 计算吞吐量 */
    private void calculateThroughput() {
        for (TopicMetrics metrics : topicMetrics.values()) {
            if (metrics.publishCount.get() > 0 && metrics.lastPublishTime > 0 && metrics.firstPublishTime > 0) {
                double duration = (metrics.lastPublishTime - metrics.firstPublishTime) / 1000.0;
                double throughput = duration > 0 ? metrics.publishCount.get() / duration : 0;
                metrics.addTimeSeriesPoint(throughput);
            }
        }
    }
    
    /** 通知回调 */
    private void notifyCallbacks() {
        if (callbacks.isEmpty()) return;
        
        MonitoringReport report = generateReport();
        for (MetricsUpdateCallback callback : callbacks) {
            try {
                callback.onMetricsUpdate(report);
            } catch (Exception e) {
                System.err.println("[MetricsCollector] 回调异常: " + e.getMessage());
            }
        }
    }
    
    /** ==================== 指标记录方法 ==================== */
    
    /**
     * 记录发布操作
     */
    public void recordPublish(String topic, int byteSize) {
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        
        metrics.publishCount.incrementAndGet();
        metrics.publishBytes.addAndGet(byteSize);
        globalMetrics.totalPublishCount.incrementAndGet();
        globalMetrics.totalPublishBytes.addAndGet(byteSize);
        
        long now = System.currentTimeMillis();
        if (metrics.firstPublishTime == 0) {
            metrics.firstPublishTime = now;
        }
        metrics.lastPublishTime = now;
        globalMetrics.lastUpdateTime = now;
    }
    
    /**
     * 记录订阅操作
     */
    public void recordSubscribe(String topic, int byteSize) {
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        
        metrics.subscribeCount.incrementAndGet();
        metrics.subscribeBytes.addAndGet(byteSize);
        globalMetrics.totalSubscribeCount.incrementAndGet();
        globalMetrics.totalSubscribeBytes.addAndGet(byteSize);
        
        metrics.lastSubscribeTime = System.currentTimeMillis();
        globalMetrics.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 记录延迟
     */
    public void recordLatency(String topic, long latencyNs) {
        TopicMetrics metrics = topicMetrics.get(topic);
        if (metrics == null) return;
        
        metrics.latencySamples.incrementAndGet();
        metrics.totalLatencyNs.addAndGet(latencyNs);
        
        // 更新最小/最大延迟
        long currentMin = metrics.minLatencyNs.get();
        long currentMax = metrics.maxLatencyNs.get();
        
        if (latencyNs < currentMin) {
            metrics.minLatencyNs.set(latencyNs);
        }
        if (latencyNs > currentMax) {
            metrics.maxLatencyNs.set(latencyNs);
        }
    }
    
    /**
     * 记录错误
     */
    public void recordError(String topic) {
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        metrics.errorCount.incrementAndGet();
        globalMetrics.totalErrors.incrementAndGet();
    }
    
    /** ==================== 查询方法 ==================== */
    
    /**
     * 获取主题指标
     */
    public TopicMetrics getTopicMetrics(String topic) {
        return topicMetrics.get(topic);
    }
    
    /**
     * 获取所有主题指标
     */
    public Map<String, TopicMetrics> getAllTopicMetrics() {
        return new HashMap<>(topicMetrics);
    }
    
    /**
     * 获取全局指标
     */
    public GlobalMetrics getGlobalMetrics() {
        return globalMetrics;
    }
    
    /**
     * 获取运行时间（秒）
     */
    public long getUptimeSeconds() {
        if (globalMetrics.startTime == 0) return 0;
        return (System.currentTimeMillis() - globalMetrics.startTime) / 1000;
    }
    
    /**
     * 获取主题平均延迟（毫秒）
     */
    public double getAverageLatencyMs(String topic) {
        TopicMetrics metrics = topicMetrics.get(topic);
        if (metrics == null || metrics.latencySamples.get() == 0) return 0;
        return (metrics.totalLatencyNs.get() / 1_000_000.0) / metrics.latencySamples.get();
    }
    
    /**
     * 获取主题最小延迟（毫秒）
     */
    public double getMinLatencyMs(String topic) {
        TopicMetrics metrics = topicMetrics.get(topic);
        if (metrics == null) return 0;
        long min = metrics.minLatencyNs.get();
        return min == Long.MAX_VALUE ? 0 : min / 1_000_000.0;
    }
    
    /**
     * 获取主题最大延迟（毫秒）
     */
    public double getMaxLatencyMs(String topic) {
        TopicMetrics metrics = topicMetrics.get(topic);
        if (metrics == null) return 0;
        return metrics.maxLatencyNs.get() / 1_000_000.0;
    }
    
    /** ==================== 报告生成 ==================== */
    
    /**
     * 生成监控报告
     */
    public MonitoringReport generateReport() {
        long now = System.currentTimeMillis();
        long uptime = globalMetrics.startTime > 0 ? (now - globalMetrics.startTime) / 1000 : 0;
        
        long totalPub = globalMetrics.totalPublishCount.get();
        long totalSub = globalMetrics.totalSubscribeCount.get();
        
        double avgPubPerSec = uptime > 0 ? totalPub / (double) uptime : 0;
        double avgSubPerSec = uptime > 0 ? totalSub / (double) uptime : 0;
        
        double totalBytes = globalMetrics.totalPublishBytes.get();
        double avgBytesPerSec = uptime > 0 ? totalBytes / uptime : 0;
        
        // 计算全局平均延迟
        double avgLatencyMs = 0;
        long totalLatencyNs = 0;
        long totalLatencySamples = 0;
        for (TopicMetrics m : topicMetrics.values()) {
            totalLatencyNs += m.totalLatencyNs.get();
            totalLatencySamples += m.latencySamples.get();
        }
        if (totalLatencySamples > 0) {
            avgLatencyMs = (totalLatencyNs / 1_000_000.0) / totalLatencySamples;
        }
        
        // 生成主题报告
        List<TopicReport> topicReports = new ArrayList<>();
        for (Map.Entry<String, TopicMetrics> entry : topicMetrics.entrySet()) {
            String topic = entry.getKey();
            TopicMetrics m = entry.getValue();
            
            double pubPerSec = m.firstPublishTime > 0 && m.lastPublishTime > m.firstPublishTime
                ? m.publishCount.get() / ((m.lastPublishTime - m.firstPublishTime) / 1000.0)
                : 0;
            
            double avgLat = m.latencySamples.get() > 0
                ? (m.totalLatencyNs.get() / 1_000_000.0) / m.latencySamples.get()
                : 0;
            
            topicReports.add(new TopicReport(
                topic,
                m.publishCount.get(),
                m.subscribeCount.get(),
                pubPerSec,
                0, // subscribe throughput
                avgLat,
                m.minLatencyNs.get() == Long.MAX_VALUE ? 0 : m.minLatencyNs.get() / 1_000_000.0,
                m.maxLatencyNs.get() / 1_000_000.0
            ));
        }
        
        return new MonitoringReport(
            now, uptime, topicMetrics.size(),
            totalPub, totalSub,
            avgPubPerSec, avgSubPerSec,
            avgBytesPerSec, avgLatencyMs,
            topicReports
        );
    }
    
    /**
     * 生成文本格式报告
     */
    public String generateTextReport() {
        MonitoringReport report = generateReport();
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║              DDS 实时监控报告                                 ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        sb.append(String.format("║ 运行时间: %-47s ║\n", formatDuration(report.uptimeSeconds)));
        sb.append(String.format("║ 活跃主题: %-46s ║\n", report.totalTopics + " 个"));
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        sb.append("║                        消息统计                               ║\n");
        sb.append(String.format("║   发布: %,d 条  |  订阅: %,d 条%25s║\n", 
            report.totalPublishCount, report.totalSubscribeCount, ""));
        sb.append(String.format("║   发布速率: %.2f msg/s  |  订阅速率: %.2f msg/s%12s║\n", 
            report.avgPublishPerSec, report.avgSubscribePerSec, ""));
        sb.append(String.format("║   平均带宽: %.2f KB/s%38s║\n", 
            report.avgBytesPerSec / 1024, ""));
        
        if (report.avgLatencyMs > 0) {
            sb.append(String.format("║   平均延迟: %.2f ms%40s║\n", report.avgLatencyMs, ""));
        }
        
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append("║                        主题详情                               ║\n");
        
        if (report.topicReports.isEmpty()) {
            sb.append("║   (暂无数据)%51s║\n");
        } else {
            for (TopicReport tr : report.topicReports) {
                sb.append(String.format("║ ▶ %-56s ║\n", truncate(tr.topic, 54)));
                sb.append(String.format("║   发布: %,d | 订阅: %,d | 延迟: %.2f ms%17s ║\n",
                    tr.publishCount, tr.subscribeCount, tr.avgLatencyMs, ""));
                sb.append(String.format("║   速率: %.2f msg/s%39s ║\n", tr.publishPerSec, ""));
            }
        }
        
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }
    
    /** 打印报告到控制台 */
    public void printReport() {
        System.out.println(generateTextReport());
    }
    
    /** ==================== 回调管理 ==================== */
    
    /**
     * 注册监控回调
     */
    public void registerCallback(MetricsUpdateCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    /**
     * 移除监控回调
     */
    public void unregisterCallback(MetricsUpdateCallback callback) {
        callbacks.remove(callback);
    }
    
    /** ==================== 工具方法 ==================== */
    
    /** 格式化时长 */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
    
    /** 截断字符串 */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
    
    /** ==================== 生命周期管理 ==================== */
    
    /** 重置所有指标 */
    public synchronized void reset() {
        topicMetrics.clear();
        globalMetrics.totalPublishCount.set(0);
        globalMetrics.totalSubscribeCount.set(0);
        globalMetrics.totalPublishBytes.set(0);
        globalMetrics.totalSubscribeBytes.set(0);
        globalMetrics.totalErrors.set(0);
        globalMetrics.peakPublishPerSec.set(0);
        globalMetrics.peakSubscribePerSec.set(0);
        globalMetrics.peakBytesPerSec.set(0);
        globalMetrics.startTime = System.currentTimeMillis();
        
        System.out.println("[MetricsCollector] 指标已重置");
        DDSLogger.getInstance().info("MetricsCollector", "指标数据已重置");
    }
    
    /** 关闭收集器 */
    public synchronized void shutdown() {
        stop();
        instance = null;
        System.out.println("[MetricsCollector] 已关闭");
    }
}
