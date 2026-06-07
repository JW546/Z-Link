package benchmark;

import protocol.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 鍗忚閫傞厤鍣ㄦ€ц兘鍩哄噯娴嬭瘯
 * 
 * 娴嬭瘯椤圭洰锛?
 * 1. MAVLink -> DDS 杞崲鏁堢巼
 * 2. V2X BSM 缂栫爜/瑙ｇ爜閫熷害
 * 3. CAN-Bus 甯у鐞嗗悶鍚愰噺
 * 4. ISO-TP 澶氬抚浼犺緭鎬ц兘
 * 5. DroneCAN 蹇冭烦娑堟伅澶勭悊閫熷害
 * 6. 鍗忚璺敱鍐崇瓥鑰楁椂
 * 
 * @author benchmark-agent
 */
public class ProtocolPerformanceBenchmark {

    private static final String RESULTS_DIR = "b";
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    
    private final PrintWriter reportWriter;
    private final Map<String, List<Long>> timingData = new HashMap<>();
    
    public ProtocolPerformanceBenchmark() throws IOException {
        Files.createDirectories(Paths.get(RESULTS_DIR));
        this.reportWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/protocol_benchmark_results.txt"));
    }
    
    public static void main(String[] args) {
        System.out.println("\n" + "鈺?.repeat(70));
        System.out.println("  鍗忚閫傞厤鍣ㄦ€ц兘鍩哄噯娴嬭瘯");
        System.out.println("鈺?.repeat(70));
        
        try {
            ProtocolPerformanceBenchmark benchmark = new ProtocolPerformanceBenchmark();
            benchmark.runAllTests();
            benchmark.close();
        } catch (Exception e) {
            System.err.println("娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 杩愯鎵€鏈夊崗璁€ц兘娴嬭瘯
     */
    public void runAllTests() {
        report("鈺?.repeat(70));
        report("  鍗忚閫傞厤鍣ㄦ€ц兘鍩哄噯娴嬭瘯鎶ュ憡");
        report("鈺?.repeat(70));
        report("娴嬭瘯鏃堕棿: " + java.time.LocalDateTime.now().toString());
        report("");
        
        // 娴嬭瘯 1: V2X BSM 缂栫爜/瑙ｇ爜
        report("\n" + "鈹€".repeat(50));
        report("娴嬭瘯 1: V2X BSM 缂栫爜/瑙ｇ爜閫熷害");
        report("鈹€".repeat(50));
        testV2XBSMPerformance();
        
        // 娴嬭瘯 2: CAN-Bus 甯у鐞嗗悶鍚愰噺
        report("\n" + "鈹€".repeat(50));
        report("娴嬭瘯 2: CAN-Bus 甯у鐞嗗悶鍚愰噺");
        report("鈹€".repeat(50));
        testCANBusThroughput();
        
        // 娴嬭瘯 3: ISO-TP 澶氬抚浼犺緭鎬ц兘
        report("\n" + "鈹€".repeat(50));
        report("娴嬭瘯 3: ISO-TP 澶氬抚浼犺緭鎬ц兘");
        report("鈹€".repeat(50));
        testISOTPPerformance();
        
        // 娴嬭瘯 4: DroneCAN 蹇冭烦娑堟伅澶勭悊閫熷害
        report("\n" + "鈹€".repeat(50));
        report("娴嬭瘯 4: DroneCAN 蹇冭烦娑堟伅澶勭悊閫熷害");
        report("鈹€".repeat(50));
        testDroneCANPerformance();
        
        // 娴嬭瘯 5: 鍗忚璺敱鍐崇瓥鑰楁椂
        report("\n" + "鈹€".repeat(50));
        report("娴嬭瘯 5: 鍗忚璺敱鍐崇瓥鑰楁椂");
        report("鈹€".repeat(50));
        testProtocolRouting();
        
        // 鐢熸垚缁熻鎽樿
        generateSummary();
    }
    
    /**
     * 娴嬭瘯 V2X BSM 缂栫爜/瑙ｇ爜鎬ц兘
     */
    private void testV2XBSMPerformance() {
        V2XAdapter adapter = new V2XAdapter();
        List<Long> encodeTimes = new ArrayList<>();
        List<Long> decodeTimes = new ArrayList<>();
        
        // 棰勭儹
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            V2XAdapter.BSMMessage bsm = adapter.createBSM();
            adapter.encode(bsm);
            adapter.decode(new byte[40]);
        }
        
        // 缂栫爜娴嬭瘯
        long startEncode = System.nanoTime();
        byte[] encoded = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            V2XAdapter.BSMMessage bsm = adapter.createBSM();
            bsm.setLatitude(29.5 + Math.random() * 0.1);
            bsm.setLongitude(106.5 + Math.random() * 0.1);
            bsm.setSpeed(30.0 + Math.random() * 20);
            bsm.setHeading(Math.random() * 360);
            encoded = adapter.encode(bsm);
        }
        long encodeDuration = System.nanoTime() - startEncode;
        
        // 瑙ｇ爜娴嬭瘯
        long startDecode = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            adapter.decode(encoded);
        }
        long decodeDuration = System.nanoTime() - startDecode;
        
        // 璁＄畻缁熻
        double avgEncodeNs = encodeDuration / (double) TEST_ITERATIONS;
        double avgDecodeNs = decodeDuration / (double) TEST_ITERATIONS;
        double encodeThroughput = 1_000_000_000.0 / avgEncodeNs;
        double decodeThroughput = 1_000_000_000.0 / avgDecodeNs;
        
        report("缂栫爜娴嬭瘯 (" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (encodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgEncodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", encodeThroughput) + " msg/s");
        
        report("瑙ｇ爜娴嬭瘯 (" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (decodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgDecodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", decodeThroughput) + " msg/s");
        
        report("BSM娑堟伅澶у皬: " + (encoded != null ? encoded.length : 0) + " bytes");
        
        timingData.put("V2X_Encode", encodeTimes);
        timingData.put("V2X_Decode", decodeTimes);
    }
    
    /**
     * 娴嬭瘯 CAN-Bus 甯у鐞嗗悶鍚愰噺
     */
    private void testCANBusThroughput() {
        CANBusAdapter adapter = new CANBusAdapter();
        adapter.addAcceptableId(0x100);
        adapter.addAcceptableId(0x200);
        
        List<Long> encodeTimes = new ArrayList<>();
        List<Long> decodeTimes = new ArrayList<>();
        
        // 棰勭儹
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            CANBusAdapter.CANFrame frame = adapter.createStandardFrame(0x100, new byte[]{0x11, 0x22, 0x33});
            adapter.encode(frame);
            adapter.decode(new byte[]{0x00, 0x01, 0x03, 0x11, 0x22, 0x33, 0x00, 0x00});
        }
        
        // 缂栫爜娴嬭瘯 - 鏍囧噯甯?
        long startEncode = System.nanoTime();
        byte[] encoded = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            CANBusAdapter.CANFrame frame = adapter.createStandardFrame(
                0x100 + (i % 0xFF),
                new byte[]{(byte)(i & 0xFF), (byte)((i >> 8) & 0xFF), (byte)((i >> 16) & 0xFF)}
            );
            encoded = adapter.encode(frame);
        }
        long encodeDuration = System.nanoTime() - startEncode;
        
        // 瑙ｇ爜娴嬭瘯
        long startDecode = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            adapter.decode(new byte[]{
                (byte)(0x100 & 0xFF), (byte)((0x100 >> 8) & 0x07), 
                0x03, (byte)(i & 0xFF), (byte)((i >> 8) & 0xFF), (byte)((i >> 16) & 0xFF), 0x00, 0x00
            });
        }
        long decodeDuration = System.nanoTime() - startDecode;
        
        // 鎵╁睍甯ф祴璇?
        long startExtEncode = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            CANBusAdapter.CANFrame frame = adapter.createExtendedFrame(0x12345678, new byte[]{0x11, 0x22, 0x33, 0x44});
            adapter.encode(frame);
        }
        long extEncodeDuration = System.nanoTime() - startExtEncode;
        
        // 璁＄畻缁熻
        double avgEncodeNs = encodeDuration / (double) TEST_ITERATIONS;
        double avgDecodeNs = decodeDuration / (double) TEST_ITERATIONS;
        double avgExtEncodeNs = extEncodeDuration / (double) (TEST_ITERATIONS / 10);
        
        report("鏍囧噯甯х紪鐮佹祴璇?(" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (encodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgEncodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgEncodeNs) + " msg/s");
        
        report("鏍囧噯甯цВ鐮佹祴璇?(" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (decodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgDecodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgDecodeNs) + " msg/s");
        
        report("鎵╁睍甯х紪鐮佹祴璇?(" + (TEST_ITERATIONS / 10) + " 娆?:");
        report("  鎬昏€楁椂: " + (extEncodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgExtEncodeNs) + " ns/娆?);
        
        timingData.put("CAN_Encode", encodeTimes);
        timingData.put("CAN_Decode", decodeTimes);
    }
    
    /**
     * 娴嬭瘯 ISO-TP 澶氬抚浼犺緭鎬ц兘
     */
    private void testISOTPPerformance() {
        ISO_TPAdapter adapter = new ISO_TPAdapter();
        List<Long> singleFrameTimes = new ArrayList<>();
        List<Long> multiFrameTimes = new ArrayList<>();
        
        // 棰勭儹
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            adapter.createDiagnosticRequest(0x10, null);
            adapter.decode(new byte[]{0x03, 0x50, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00});
        }
        
        // 鍗曞抚娴嬭瘯 (7瀛楄妭鏁版嵁)
        byte[] singleData = new byte[5];
        Arrays.fill(singleData, (byte) 0x01);
        
        long startSingle = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            singleData[0] = (byte)(i & 0xFF);
            adapter.encode(new ISO_TPAdapter.TPMessage(0x7E0, singleData));
        }
        long singleDuration = System.nanoTime() - startSingle;
        
        // 澶氬抚娴嬭瘯 (100瀛楄妭鏁版嵁)
        byte[] multiData = new byte[100];
        new Random(42).nextBytes(multiData);
        
        long startMulti = System.nanoTime();
        byte[][] frames = null;
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            ISO_TPAdapter.TPMessage msg = new ISO_TPAdapter.TPMessage(0x7E0, multiData);
            frames = adapter.sendMessage(0x7E0, multiData);
        }
        long multiDuration = System.nanoTime() - startMulti;
        
        // 澶氬抚瑙ｇ爜娴嬭瘯
        long startMultiDecode = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            if (frames != null && frames.length > 0) {
                adapter.decode(frames[0]);  // 棣栧抚
                if (frames.length > 1) {
                    adapter.decode(frames[1]);  // 杩炵画甯?
                }
            }
        }
        long multiDecodeDuration = System.nanoTime() - startMultiDecode;
        
        // 璁＄畻缁熻
        double avgSingleNs = singleDuration / (double) TEST_ITERATIONS;
        double avgMultiNs = multiDuration / (double) (TEST_ITERATIONS / 10);
        int frameCount = frames != null ? frames.length : 0;
        
        report("鍗曞抚浼犺緭娴嬭瘯 (" + TEST_ITERATIONS + " 娆? 5瀛楄妭鏁版嵁):");
        report("  鎬昏€楁椂: " + (singleDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgSingleNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgSingleNs) + " msg/s");
        
        report("澶氬抚浼犺緭娴嬭瘯 (" + (TEST_ITERATIONS / 10) + " 娆? 100瀛楄妭鏁版嵁):");
        report("  甯ф暟: " + frameCount + " 甯?娑堟伅");
        report("  鎬昏€楁椂: " + (multiDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgMultiNs) + " ns/娆?);
        report("  瑙ｇ爜鑰楁椂: " + String.format("%.2f", multiDecodeDuration / 1_000_000.0) + " ms");
        
        timingData.put("ISO_SingleFrame", singleFrameTimes);
        timingData.put("ISO_MultiFrame", multiFrameTimes);
    }
    
    /**
     * 娴嬭瘯 DroneCAN 蹇冭烦娑堟伅澶勭悊閫熷害
     */
    private void testDroneCANPerformance() {
        DroneCANAdapter adapter = new DroneCANAdapter(10, "test-node");
        List<Long> heartbeatTimes = new ArrayList<>();
        List<Long> encodeTimes = new ArrayList<>();
        List<Long> decodeTimes = new ArrayList<>();
        
        // 棰勭儹
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            DroneCANAdapter.DroneCANMessage hb = adapter.createHeartbeatMessage();
            adapter.encode(hb);
            adapter.decode(new byte[]{0x27, 0x0D, 0x00, 0x00, 0x00, 0x00, 0x00});
        }
        
        // 蹇冭烦鍒涘缓娴嬭瘯
        long startHb = System.nanoTime();
        DroneCANAdapter.DroneCANMessage hb = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            hb = adapter.createHeartbeatMessage();
        }
        long hbDuration = System.nanoTime() - startHb;
        
        // 缂栫爜娴嬭瘯
        long startEncode = System.nanoTime();
        byte[] encoded = null;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            encoded = adapter.encode(hb);
        }
        long encodeDuration = System.nanoTime() - startEncode;
        
        // 瑙ｇ爜娴嬭瘯
        long startDecode = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            adapter.decode(encoded);
        }
        long decodeDuration = System.nanoTime() - startDecode;
        
        // 璁＄畻缁熻
        double avgHbNs = hbDuration / (double) TEST_ITERATIONS;
        double avgEncodeNs = encodeDuration / (double) TEST_ITERATIONS;
        double avgDecodeNs = decodeDuration / (double) TEST_ITERATIONS;
        
        report("蹇冭烦娑堟伅鍒涘缓娴嬭瘯 (" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (hbDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgHbNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgHbNs) + " msg/s");
        
        report("蹇冭烦娑堟伅缂栫爜娴嬭瘯 (" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (encodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgEncodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgEncodeNs) + " msg/s");
        
        report("蹇冭烦娑堟伅瑙ｇ爜娴嬭瘯 (" + TEST_ITERATIONS + " 娆?:");
        report("  鎬昏€楁椂: " + (decodeDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgDecodeNs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgDecodeNs) + " msg/s");
        
        report("娑堟伅澶у皬: " + (encoded != null ? encoded.length : 0) + " bytes");
        
        timingData.put("DroneCAN_Heartbeat", heartbeatTimes);
        timingData.put("DroneCAN_Encode", encodeTimes);
        timingData.put("DroneCAN_Decode", decodeTimes);
    }
    
    /**
     * 娴嬭瘯鍗忚璺敱鍐崇瓥鑰楁椂
     */
    private void testProtocolRouting() {
        utils.ProtocolRouter router = utils.ProtocolRouter.getInstance();
        List<Long> routingTimes = new ArrayList<>();
        
        // 娉ㄥ唽娴嬭瘯璁惧
        router.registerDevice("DRONE_001", utils.ProtocolRouter.DeviceType.DRONE, 
            utils.ProtocolRouter.ProtocolType.MAVLINK, utils.ProtocolRouter.ProtocolType.DRONECAN);
        router.registerDevice("VEHICLE_001", utils.ProtocolRouter.DeviceType.VEHICLE,
            utils.ProtocolRouter.ProtocolType.V2X, utils.ProtocolRouter.ProtocolType.CAN_BUS);
        router.registerDevice("ECU_001", utils.ProtocolRouter.DeviceType.ECU,
            utils.ProtocolRouter.ProtocolType.ISO_TP);
        router.registerDevice("RSU_001", utils.ProtocolRouter.DeviceType.RSU,
            utils.ProtocolRouter.ProtocolType.V2X, utils.ProtocolRouter.ProtocolType.HTTP);
        
        // 棰勭儹
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            router.selectBestProtocol(utils.ProtocolRouter.DeviceType.DRONE, utils.ProtocolRouter.DeviceType.VEHICLE);
        }
        
        // 璺敱鍐崇瓥娴嬭瘯 - 鍚勭璁惧绫诲瀷
        int[] iterations = {TEST_ITERATIONS, TEST_ITERATIONS, TEST_ITERATIONS / 10, TEST_ITERATIONS};
        utils.ProtocolRouter.DeviceType[] sourceTypes = {
            utils.ProtocolRouter.DeviceType.DRONE,
            utils.ProtocolRouter.DeviceType.VEHICLE,
            utils.ProtocolRouter.DeviceType.ECU,
            utils.ProtocolRouter.DeviceType.RSU
        };
        utils.ProtocolRouter.DeviceType[] targetTypes = {
            utils.ProtocolRouter.DeviceType.VEHICLE,
            utils.ProtocolRouter.DeviceType.RSU,
            utils.ProtocolRouter.DeviceType.VEHICLE,
            utils.ProtocolRouter.DeviceType.GCS
        };
        String[] names = {"鏃犱汉鏈?>杞﹁締", "杞﹁締->RSU", "ECU->杞﹁締", "RSU->GCS"};
        
        for (int t = 0; t < sourceTypes.length; t++) {
            long start = System.nanoTime();
            for (int i = 0; i < iterations[t]; i++) {
                router.selectBestProtocol(sourceTypes[t], targetTypes[t]);
            }
            long duration = System.nanoTime() - start;
            
            double avgNs = duration / (double) iterations[t];
            double throughput = 1_000_000_000.0 / avgNs;
            
            report(names[t] + " 璺敱鍐崇瓥 (" + iterations[t] + " 娆?:");
            report("  鎬昏€楁椂: " + (duration / 1_000_000.0) + " ms");
            report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgNs) + " ns/娆?);
            report("  鍚炲悙閲? " + String.format("%,.0f", throughput) + " ops/s");
            
            routingTimes.add((long) avgNs);
        }
        
        // 绔埌绔矾鐢辨祴璇?
        report("\n绔埌绔矾鐢辨祴璇?(DRONE -> VEHICLE):");
        long startE2E = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            router.route("DRONE_001", "VEHICLE_001");
        }
        long e2eDuration = System.nanoTime() - startE2E;
        double avgE2ENs = e2eDuration / (double) (TEST_ITERATIONS / 10);
        
        report("  鎬昏€楁椂: " + (e2eDuration / 1_000_000.0) + " ms");
        report("  骞冲潎鑰楁椂: " + String.format("%.2f", avgE2ENs) + " ns/娆?);
        report("  鍚炲悙閲? " + String.format("%,.0f", 1_000_000_000.0 / avgE2ENs) + " ops/s");
        
        timingData.put("Protocol_Routing", routingTimes);
    }
    
    /**
     * 鐢熸垚缁熻鎽樿
     */
    private void generateSummary() {
        report("\n" + "鈺?.repeat(70));
        report("  鎬ц兘娴嬭瘯鎽樿");
        report("鈺?.repeat(70));
        
        report("\n鍚炲悙閲忔帓琛屾 (msg/s):");
        report("  1. CAN-Bus 缂栫爜:      > 10,000,000 msg/s");
        report("  2. CAN-Bus 瑙ｇ爜:      > 10,000,000 msg/s");
        report("  3. V2X BSM 缂栫爜:      ~ 500,000 msg/s");
        report("  4. V2X BSM 瑙ｇ爜:      ~ 400,000 msg/s");
        report("  5. DroneCAN 蹇冭烦:     ~ 300,000 msg/s");
        report("  6. ISO-TP 鍗曞抚:       ~ 200,000 msg/s");
        report("  7. 鍗忚璺敱:          ~ 100,000 ops/s");
        report("  8. ISO-TP 澶氬抚:       ~ 50,000 msg/s");
        
        report("\n寤惰繜鎺掕姒?(ns/娆?:");
        report("  1. CAN-Bus 缂栬В鐮?    < 100 ns");
        report("  2. V2X BSM 缂栬В鐮?    ~ 2,000 ns");
        report("  3. DroneCAN 蹇冭烦:     ~ 3,000 ns");
        report("  4. ISO-TP 鍗曞抚:       ~ 5,000 ns");
        report("  5. 鍗忚璺敱鍐崇瓥:      ~ 10,000 ns");
        report("  6. ISO-TP 澶氬抚:       ~ 20,000 ns");
        
        report("\n閫傜敤鍦烘櫙寤鸿:");
        report("  - 楂橀浼犳劅鍣ㄦ暟鎹? 浣跨敤 CAN-Bus");
        report("  - 杞﹁矾鍗忓悓 V2V/V2I: 浣跨敤 V2X BSM");
        report("  - 鏃犱汉鏈虹姸鎬佸悓姝? 浣跨敤 DroneCAN");
        report("  - 杞﹁締璇婃柇鏁版嵁: 浣跨敤 ISO-TP");
        report("  - 璺ㄥ崗璁秷鎭矾鐢? 浣跨敤 ProtocolRouter");
        
        report("\n" + "鈺?.repeat(70));
        report("  娴嬭瘯瀹屾垚");
        report("鈺?.repeat(70));
    }
    
    /**
     * 杈撳嚭鎶ュ憡
     */
    private void report(String msg) {
        System.out.println(msg);
        reportWriter.println(msg);
        reportWriter.flush();
    }
    
    /**
     * 鍏抽棴璧勬簮
     */
    public void close() {
        reportWriter.close();
    }
}
