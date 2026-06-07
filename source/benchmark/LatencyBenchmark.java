package benchmark;

import dds.DDSManager;
import chongqing.*;
import urban.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * DDS 寤惰繜鍩哄噯娴嬭瘯
 * 娴嬮噺绔埌绔秷鎭欢杩熷強寤惰繜鍒嗗竷
 * 
 * @paper_ref 绗?绔?5.3 DDS閫氫俊鎬ц兘浼樺寲 -> 寤惰繜鍩哄噯娴嬭瘯
 */
public class LatencyBenchmark {

    private static final String RESULTS_DIR = "b";
    private static final int WARMUP_ITERATIONS = 500;
    private static final int TEST_ITERATIONS = 5000;
    private static final int[] MESSAGE_SIZES = {64, 256, 1024};
    
    private final DDSManager dds;
    private final PrintWriter csvWriter;
    private final PrintWriter histogramWriter;
    
    // 寤惰繜缁熻
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatency = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong sampleCount = new AtomicLong(0);
    private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    
    public LatencyBenchmark() throws IOException {
        this.dds = DDSManager.getInstance();
        Files.createDirectories(Paths.get(RESULTS_DIR));
        this.csvWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/latency_results.csv"));
        this.histogramWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/latency_histogram.csv"));
        
        csvWriter.println("message_size_bytes,min_latency_us,max_latency_us,avg_latency_us,p50_latency_us,p95_latency_us,p99_latency_us");
    }
    
    /**
     * 杩愯鎵€鏈夊欢杩熸祴璇?
     */
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  DDS 寤惰繜鍩哄噯娴嬭瘯");
        System.out.println("=".repeat(70));
        
        // 杩炴帴 DDS
        if (!dds.connect()) {
            System.err.println("DDS 杩炴帴澶辫触!");
            return;
        }
        
        // 璁剧疆璁㈤槄鍥炶皟
        setupSubscriber();
        
        // 绛夊緟璁㈤槄灏辩华
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // 棰勭儹
        System.out.println("\n[棰勭儹] " + WARMUP_ITERATIONS + " 娆¤凯浠?..");
        warmup();
        
        // 娴嬭瘯涓嶅悓娑堟伅澶у皬
        for (int msgSize : MESSAGE_SIZES) {
            testMessageSize(msgSize);
        }
        
        // 鏂紑杩炴帴
        dds.disconnect();
        
        System.out.println("\n鉁?寤惰繜娴嬭瘯瀹屾垚!");
        System.out.println("  - 缁撴灉宸蹭繚瀛樺埌 " + RESULTS_DIR + "/latency_results.csv");
        System.out.println("  - 鐩存柟鍥惧凡淇濆瓨鍒?" + RESULTS_DIR + "/latency_histogram.csv");
    }
    
    /**
     * 璁剧疆璁㈤槄鍥炶皟浠ユ祴閲忓欢杩?
     */
    private void setupSubscriber() {
        System.out.println("[璁剧疆] 璁㈤槄 EmergencyStatus 涓婚...");
        
        dds.subscribeEmergencyStatus(status -> {
            // 浠庢秷鎭腑鎻愬彇鍙戦€佹椂闂存埑
            if (status.timestamp_sec > 0) {
                long sendTime = status.timestamp_sec * 1000L + status.timestamp_usec / 1000L;
                long receiveTime = System.currentTimeMillis();
                long latency = receiveTime - sendTime;
                
                recordLatency(latency);
            }
        });
    }
    
    /**
     * 璁板綍寤惰繜鏍锋湰
     */
    private void recordLatency(long latencyMicros) {
        // 杞崲寰涓烘绉?(DDS timestamp_usec 鏄井绉?
        long latencyMs = latencyMicros / 1000;
        
        minLatency.updateAndGet(v -> Math.min(v, latencyMs));
        maxLatency.updateAndGet(v -> Math.max(v, latencyMs));
        totalLatency.addAndGet(latencyMs);
        sampleCount.incrementAndGet();
        latencies.add(latencyMs);
    }
    
    /**
     * 棰勭儹娴嬭瘯
     */
    private void warmup() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            publishTestMessage(64);
        }
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        latencies.clear();
    }
    
    /**
     * 娴嬭瘯鎸囧畾娑堟伅澶у皬鐨勫欢杩?
     */
    private void testMessageSize(int msgSize) {
        System.out.println("\n[娴嬭瘯] 娑堟伅澶у皬: " + msgSize + " 瀛楄妭, 杩唬: " + TEST_ITERATIONS);
        
        // 閲嶇疆缁熻
        resetStats();
        
        String payload = generatePayload(msgSize);
        
        // 鍙戝竷娑堟伅
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            publishTestMessage(msgSize, payload);
            // 灏忕殑寤惰繜閬垮厤鎷ュ
            if (i % 100 == 0) {
                try { Thread.sleep(1); } catch (InterruptedException e) {}
            }
        }
        
        // 绛夊緟鎵€鏈夋秷鎭帴鏀?
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        
        long testDuration = System.currentTimeMillis() - startTime;
        
        // 璁＄畻缁熻鏁版嵁
        long count = sampleCount.get();
        if (count > 0) {
            double avgLatency = totalLatency.get() / (double) count;
            
            // 璁＄畻鐧惧垎浣嶆暟
            List<Long> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);
            long p50 = sorted.get((int)(count * 0.50));
            long p95 = sorted.get((int)(count * 0.95));
            long p99 = sorted.get((int)(count * 0.99));
            
            System.out.printf("  缁撴灉: min=%.2fms, avg=%.2fms, p50=%.2fms, p95=%.2fms, p99=%.2fms, max=%.2fms%n",
                minLatency.get() / 1000.0,
                avgLatency / 1000.0,
                p50 / 1000.0,
                p95 / 1000.0,
                p99 / 1000.0,
                maxLatency.get() / 1000.0);
            
            // 淇濆瓨缁撴灉
            csvWriter.printf("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f%n",
                msgSize,
                minLatency.get() / 1000.0,
                maxLatency.get() / 1000.0,
                avgLatency / 1000.0,
                p50 / 1000.0,
                p95 / 1000.0,
                p99 / 1000.0);
            csvWriter.flush();
            
            // 鐢熸垚鐩存柟鍥?
            generateHistogram(sorted, msgSize);
        }
    }
    
    /**
     * 鐢熸垚寤惰繜鐩存柟鍥?
     */
    private void generateHistogram(List<Long> sortedLatencies, int msgSize) {
        if (sortedLatencies.isEmpty()) return;
        
        histogramWriter.println("\n=== Message Size: " + msgSize + " bytes ===");
        histogramWriter.println("bin_start_ms,bin_end_ms,count,percentage");
        
        // 鍒涘缓鐩存柟鍥惧尯闂?
        long min = sortedLatencies.get(0);
        long max = sortedLatencies.get(sortedLatencies.size() - 1);
        int numBins = 20;
        long binSize = Math.max(1, (max - min) / numBins);
        
        int[] bins = new int[numBins];
        for (long lat : sortedLatencies) {
            int binIndex = (int) Math.min((lat - min) / binSize, numBins - 1);
            bins[binIndex]++;
        }
        
        for (int i = 0; i < numBins; i++) {
            double binStart = (min + i * binSize) / 1000.0;
            double binEnd = (min + (i + 1) * binSize) / 1000.0;
            double percentage = (bins[i] * 100.0) / sortedLatencies.size();
            histogramWriter.printf("%.3f,%.3f,%d,%.2f%n", binStart, binEnd, bins[i], percentage);
        }
        histogramWriter.flush();
    }
    
    /**
     * 閲嶇疆缁熻
     */
    private void resetStats() {
        minLatency.set(Long.MAX_VALUE);
        maxLatency.set(0);
        totalLatency.set(0);
        sampleCount.set(0);
        latencies.clear();
    }
    
    /**
     * 鍙戝竷娴嬭瘯娑堟伅
     */
    private void publishTestMessage(int msgSize) {
        publishTestMessage(msgSize, generatePayload(msgSize));
    }
    
    private void publishTestMessage(int msgSize, String payload) {
        EmergencyStatus status = new EmergencyStatus();
        status.drone_id = "LAT_" + System.nanoTime();
        status.device_type = DeviceType.DRONE_MULTIROTOR;
        status.mission_type = MissionType.MEDICAL_SUPPLY_DELIVERY;
        status.status = StatusCode.IN_TRANSIT;
        status.battery_level = 85.0;
        status.speed = 10.0;
        status.priority = 5;
        
        status.current_position = new Position3D();
        status.current_position.latitude = 29.5 + Math.random() * 0.1;
        status.current_position.longitude = 106.5 + Math.random() * 0.1;
        status.current_position.altitude = 100.0;
        
        // 璁板綍鍙戦€佹椂闂存埑
        long now = System.currentTimeMillis();
        status.timestamp_sec = (int) (now / 1000);
        status.timestamp_usec = (int) ((now % 1000) * 1000);
        
        status.destination = payload;
        
        dds.publishEmergencyStatus(status);
    }
    
    /**
     * 鐢熸垚娴嬭瘯杞借嵎
     */
    private String generatePayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        Random rand = new Random(42);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        while (sb.length() < size) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 鍏抽棴璧勬簮
     */
    public void close() {
        csvWriter.close();
        histogramWriter.close();
    }
    
    /**
     * 涓诲叆鍙?- 鐙珛杩愯
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  DDS 寤惰繜鍩哄噯娴嬭瘯");
        System.out.println("===========================================");
        
        try {
            LatencyBenchmark benchmark = new LatencyBenchmark();
            benchmark.runAllTests();
            benchmark.close();
        } catch (Exception e) {
            System.err.println("娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
