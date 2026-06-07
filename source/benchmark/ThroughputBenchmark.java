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
 * DDS 鍚炲悙閲忓熀鍑嗘祴璇?
 * 娴嬭瘯涓嶅悓娑堟伅澶у皬鍜屽彂閫佹ā寮忎笅鐨勬秷鎭悶鍚愰噺
 * 
 * @paper_ref 绗?绔?5.3 DDS閫氫俊鎬ц兘浼樺寲 -> 鍚炲悙閲忓熀鍑嗘祴璇?
 */
public class ThroughputBenchmark {

    private static final String RESULTS_DIR = "b";
    private static final int WARMUP_MESSAGES = 1000;
    private static final int TEST_MESSAGES = 10000;
    private static final int[] MESSAGE_SIZES = {64, 256, 1024, 4096, 16384};
    
    private final DDSManager dds;
    private final PrintWriter csvWriter;
    
    public ThroughputBenchmark() throws IOException {
        this.dds = DDSManager.getInstance();
        Files.createDirectories(Paths.get(RESULTS_DIR));
        this.csvWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/throughput_results.csv"));
        csvWriter.println("message_size_bytes,messages_per_second,mb_per_second,batch_size,test_duration_ms,total_messages");
    }
    
    /**
     * 杩愯鎵€鏈夊悶鍚愰噺娴嬭瘯
     */
    public void runAllTests() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  DDS 鍚炲悙閲忓熀鍑嗘祴璇?);
        System.out.println("=".repeat(70));
        
        // 杩炴帴 DDS
        if (!dds.connect()) {
            System.err.println("DDS 杩炴帴澶辫触!");
            return;
        }
        
        // 棰勭儹
        System.out.println("\n[棰勭儹] 鍙戦€?" + WARMUP_MESSAGES + " 鏉℃秷鎭?..");
        warmup();
        
        // 娴嬭瘯涓嶅悓娑堟伅澶у皬
        for (int msgSize : MESSAGE_SIZES) {
            testMessageSize(msgSize);
        }
        
        // 娴嬭瘯鎵归噺鍙戦€?
        testBatchModes();
        
        // 鏂紑杩炴帴
        dds.disconnect();
        
        System.out.println("\n鉁?鍚炲悙閲忔祴璇曞畬鎴? 缁撴灉宸蹭繚瀛樺埌 " + RESULTS_DIR + "/throughput_results.csv");
    }
    
    /**
     * 棰勭儹 DDS 杩炴帴
     */
    private void warmup() {
        for (int i = 0; i < WARMUP_MESSAGES; i++) {
            publishTestMessage(64);
        }
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
    
    /**
     * 娴嬭瘯鎸囧畾娑堟伅澶у皬鐨勫悶鍚愰噺
     */
    private void testMessageSize(int msgSize) {
        System.out.println("\n[娴嬭瘯] 娑堟伅澶у皬: " + msgSize + " 瀛楄妭");
        
        // 娴嬭瘯涓嶅悓鎵归噺澶у皬
        int[] batchSizes = {1, 10, 50, 100};
        
        for (int batchSize : batchSizes) {
            if (batchSize > TEST_MESSAGES / 10) continue;
            
            double throughput = runTest(msgSize, batchSize);
            double mbPerSec = throughput * msgSize / (1024.0 * 1024.0);
            
            System.out.printf("  鎵归噺澶у皬 %3d: %,.0f msg/s (%.2f MB/s)%n", 
                batchSize, throughput, mbPerSec);
        }
    }
    
    /**
     * 娴嬭瘯鎵归噺鍙戦€佹ā寮?
     */
    private void testBatchModes() {
        System.out.println("\n[娴嬭瘯] 鎵归噺鍙戦€佹ā寮忓姣?(娑堟伅澶у皬=1024)");
        int msgSize = 1024;
        
        // 鍗曟潯鍙戦€?
        double singleRate = runTest(msgSize, 1);
        System.out.printf("  鍗曟潯鍙戦€?   %,.0f msg/s%n", singleRate);
        
        // 鎵归噺鍙戦€?
        double batchRate = runTest(msgSize, 100);
        System.out.printf("  鎵归噺鍙戦€?   %,.0f msg/s%n", batchRate);
        
        double improvement = ((batchRate - singleRate) / singleRate) * 100;
        System.out.printf("  鎻愬崌:       %.1f%%%n", improvement);
    }
    
    /**
     * 鎵ц涓€娆″悶鍚愰噺娴嬭瘯
     */
    private double runTest(int msgSize, int batchSize) {
        AtomicLong successCount = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        
        // 鍑嗗娴嬭瘯鏁版嵁
        String payload = generatePayload(msgSize);
        
        // 鍙戝竷绾跨▼
        Thread publisher = new Thread(() -> {
            for (int i = 0; i < TEST_MESSAGES; i += batchSize) {
                int currentBatch = Math.min(batchSize, TEST_MESSAGES - i);
                for (int j = 0; j < currentBatch; j++) {
                    publishTestMessage(msgSize, payload);
                    successCount.incrementAndGet();
                }
            }
        });
        
        publisher.start();
        try { publisher.join(); } catch (InterruptedException e) {}
        
        long duration = System.currentTimeMillis() - startTime;
        double messagesPerSecond = (successCount.get() * 1000.0) / duration;
        
        // 璁板綍缁撴灉
        csvWriter.printf("%d,%.2f,%.4f,%d,%d,%d%n",
            msgSize, messagesPerSecond, 
            messagesPerSecond * msgSize / (1024.0 * 1024.0),
            batchSize, duration, successCount.get());
        csvWriter.flush();
        
        return messagesPerSecond;
    }
    
    /**
     * 鍙戝竷娴嬭瘯娑堟伅
     */
    private void publishTestMessage(int msgSize) {
        publishTestMessage(msgSize, generatePayload(msgSize));
    }
    
    private void publishTestMessage(int msgSize, String payload) {
        EmergencyStatus status = new EmergencyStatus();
        status.drone_id = "BENCH_" + System.nanoTime();
        status.device_type = DeviceType.DRONE_MULTIROTOR;
        status.mission_type = MissionType.MEDICAL_SUPPLY_DELIVERY;
        status.status = StatusCode.IN_TRANSIT;
        status.battery_level = 85.0;
        status.speed = 10.0;
        status.priority = 5;
        
        status.current_position = new Position3D();
        status.current_position.latitude = 29.5 + Math.random() * 0.1;
        status.current_position.longitude = 106.5 + Math.random() * 0.1;
        status.current_position.altitude = 100.0 + Math.random() * 50;
        
        status.destination = payload;
        
        dds.publishEmergencyStatus(status);
    }
    
    /**
     * 鐢熸垚鎸囧畾澶у皬鐨勬祴璇曡浇鑽?
     */
    private String generatePayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rand = new Random(42);
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
    }
    
    /**
     * 涓诲叆鍙?- 鐙珛杩愯
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  DDS 鍚炲悙閲忓熀鍑嗘祴璇?);
        System.out.println("===========================================");
        
        try {
            ThroughputBenchmark benchmark = new ThroughputBenchmark();
            benchmark.runAllTests();
            benchmark.close();
        } catch (Exception e) {
            System.err.println("娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
