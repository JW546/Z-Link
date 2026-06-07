package benchmark;

import dds.DDSManager;
import chongqing.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * DDS 鍘嬪姏娴嬭瘯
 * @paper_ref 绗?绔?5.3 DDS閫氫俊鎬ц兘浼樺寲
 */
public class StressTest {
    
    private static final String RESULTS_DIR = "b";
    private static final int DURATION_SECONDS = 30;
    
    private final DDSManager dds;
    private final PrintWriter csvWriter;
    private final PrintWriter metricsWriter;
    
    private final AtomicLong totalPublished = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile boolean running = false;
    private long initialMemory = 0;
    
    public StressTest() throws IOException {
        this.dds = DDSManager.getInstance();
        Files.createDirectories(Paths.get(RESULTS_DIR));
        
        this.csvWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/stress_test_results.csv"));
        this.metricsWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/stress_metrics.csv"));
        
        csvWriter.println("time_sec,topics,pub_rate,subscribe_rate,latency_avg_ms,errors");
        metricsWriter.println("time_sec,heap_used_mb,heap_total_mb,threads,gc_count");
    }
    
    public void runAllTests() {
        System.out.println("=== DDS 鍘嬪姏娴嬭瘯 ===\n");
        
        if (!dds.connect()) {
            System.err.println("DDS 杩炴帴澶辫触!");
            return;
        }
        
        initialMemory = getUsedMemory();
        
        // [娴嬭瘯1] 澶氫富棰樺苟鍙?
        System.out.println("[娴嬭瘯1] 澶氫富棰樺苟鍙?(2-16涓富棰?");
        testMultiTopic();
        
        // [娴嬭瘯2] 楂樿礋杞?
        System.out.println("\n[娴嬭瘯2] 楂樿礋杞芥祴璇?(100-1000 msg/s)");
        testHighLoad();
        
        // [娴嬭瘯3] 闀挎椂闂寸ǔ瀹氭€?
        System.out.println("\n[娴嬭瘯3] 闀挎椂闂寸ǔ瀹氭€?(" + DURATION_SECONDS + "绉?");
        testLongDuration();
        
        dds.disconnect();
        printSummary();
        
        csvWriter.close();
        metricsWriter.close();
        System.out.println("\n娴嬭瘯瀹屾垚!");
    }
    
    private void testMultiTopic() {
        int[] topicCounts = {2, 4, 8, 16};
        
        for (int count : topicCounts) {
            System.out.println("\n  " + count + " 涓苟鍙戜富棰?..");
            resetStats();
            
            ExecutorService executor = Executors.newFixedThreadPool(count);
            CountDownLatch latch = new CountDownLatch(count);
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < count; i++) {
                final int topicId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 100 && running; j++) {
                            publishEmergencyStatus(topicId);
                            Thread.sleep(10);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            try { latch.await(30, TimeUnit.SECONDS); } catch (InterruptedException e) {}
            
            long duration = System.currentTimeMillis() - startTime;
            double rate = totalPublished.get() * 1000.0 / duration;
            
            System.out.printf("    涓婚鏁?%d, 鍙戝竷鐜?%.0f msg/s, 閿欒=%d\n", 
                count, rate, errorCount.get());
            
            csvWriter.printf("%d,%d,%.2f,%.0f,%.2f,%d\n",
                0, count, rate, 0.0, 0.0, errorCount.get());
            csvWriter.flush();
            
            executor.shutdown();
        }
    }
    
    private void testHighLoad() {
        int[] rates = {100, 200, 500, 1000};
        
        for (int targetRate : rates) {
            System.out.println("\n  " + targetRate + " msg/s...");
            resetStats();
            running = true;
            long startTime = System.currentTimeMillis();
            int intervalMs = 1000 / targetRate;
            
            Thread publisher = new Thread(() -> {
                int count = 0;
                while (running && count < targetRate * 10) {
                    publishEmergencyStatus(count);
                    count++;
                    try { Thread.sleep(intervalMs); } 
                    catch (InterruptedException e) { break; }
                }
            });
            
            publisher.start();
            
            for (int sec = 0; sec < 10 && running; sec++) {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                
                long pubRate = totalPublished.get() * 1000L / (System.currentTimeMillis() - startTime);
                recordMetrics(sec);
                
                System.out.printf("    %ds: 鍙戝竷鐜?%d msg/s, 閿欒=%d\n",
                    sec + 1, pubRate, errorCount.get());
            }
            
            running = false;
            try { publisher.join(1000); } catch (InterruptedException e) {}
            
            long duration = System.currentTimeMillis() - startTime;
            double avgRate = totalPublished.get() * 1000.0 / duration;
            System.out.printf("    骞冲潎鍙戝竷鐜?%.0f msg/source\n", avgRate);
        }
    }
    
    private void testLongDuration() {
        System.out.println("  " + DURATION_SECONDS + " 绉掔ǔ瀹氭€ф祴璇?..");
        resetStats();
        running = true;
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + DURATION_SECONDS * 1000;
        int messageCount = 0;
        
        Thread publisher = new Thread(() -> {
            while (running && System.currentTimeMillis() < endTime) {
                publishEmergencyStatus(messageCount++);
                try { Thread.sleep(10); } 
                catch (InterruptedException e) { break; }
            }
        });
        
        publisher.start();
        
        int sec = 0;
        while (running && System.currentTimeMillis() < endTime) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            
            recordMetrics(sec);
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            double rate = totalPublished.get() * 1000.0 / (elapsed * 1000);
            
            System.out.printf("    %ds/%ds: 鍙戝竷=%d, 閫熺巼=%.0f msg/s, 鍐呭瓨=%.1fMB\n",
                sec + 1, DURATION_SECONDS, totalPublished.get(), rate, getUsedMemory() / 1024.0 / 1024);
            
            csvWriter.printf("%d,%d,%.2f,%.0f,%.2f,%d\n",
                elapsed, 1, rate, 0.0, 0.0, errorCount.get());
            csvWriter.flush();
            
            sec++;
        }
        
        running = false;
        try { publisher.join(2000); } catch (InterruptedException e) {}
    }
    
    private void publishEmergencyStatus(int seed) {
        try {
            EmergencyStatus status = new EmergencyStatus();
            status.drone_id = "STRESS_" + seed;
            status.device_type = DeviceType.DRONE_MULTIROTOR;
            status.mission_type = MissionType.MEDICAL_SUPPLY_DELIVERY;
            status.status = StatusCode.IN_TRANSIT;
            status.battery_level = 50.0 + Math.random() * 40;
            status.speed = 10.0 + Math.random() * 5;
            status.priority = 5;
            
            status.current_position = new Position3D();
            status.current_position.latitude = 29.5 + Math.random() * 0.1;
            status.current_position.longitude = 106.5 + Math.random() * 0.1;
            status.current_position.altitude = 100.0 + Math.random() * 50;
            
            dds.publishEmergencyStatus(status);
            totalPublished.incrementAndGet();
        } catch (Exception e) {
            errorCount.incrementAndGet();
        }
    }
    
    private void recordMetrics(int second) {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapTotal = runtime.totalMemory();
        int threads = Thread.activeCount();
        
        metricsWriter.printf("%d,%.2f,%.2f,%d,%d\n",
            second, heapUsed / 1024.0 / 1024.0,
            heapTotal / 1024.0 / 1024.0, threads, 0);
        metricsWriter.flush();
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private void resetStats() {
        totalPublished.set(0);
        errorCount.set(0);
    }
    
    private void printSummary() {
        System.out.println("\n=== 娴嬭瘯鎽樿 ===");
        System.out.println("  鎬诲彂甯冩秷鎭? " + totalPublished.get());
        System.out.println("  鎬婚敊璇暟:   " + errorCount.get());
        System.out.printf("  鍐呭瓨澧為暱:   %.2f MB\n", 
            (getUsedMemory() - initialMemory) / 1024.0 / 1024.0);
    }
    
    public static void main(String[] args) {
        System.out.println("=== DDS 鍘嬪姏娴嬭瘯 ===\n");
        
        try {
            StressTest test = new StressTest();
            test.runAllTests();
        } catch (Exception e) {
            System.err.println("娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
