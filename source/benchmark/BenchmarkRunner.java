package benchmark;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 鍩哄噯娴嬭瘯杩愯鍣?
 * 缁勫悎鎵ц鎵€鏈夋祴璇曞苟鐢熸垚鎶ュ憡
 * 
 * @paper_ref 绗?绔?5.3 DDS閫氫俊鎬ц兘浼樺寲 -> 娴嬭瘯妗嗘灦
 */
public class BenchmarkRunner {
    
    private static final String RESULTS_DIR = "b";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    private boolean runThroughput = true;
    private boolean runLatency = true;
    private boolean runStress = true;
    private boolean runCoordinationStress = true;
    private int iterations = 1;
    
    public static void main(String[] args) {
        BenchmarkRunner runner = new BenchmarkRunner();
        
        // 瑙ｆ瀽鍙傛暟
        for (String arg : args) {
            switch (arg) {
                case "-t": runner.runThroughput = false; break;
                case "-l": runner.runLatency = false; break;
                case "-s": runner.runStress = false; break;
                case "-c": runner.runCoordinationStress = false; break;
                case "-a": 
                    runner.runThroughput = true;
                    runner.runLatency = true;
                    runner.runStress = true;
                    runner.runCoordinationStress = true;
                    break;
                default:
                    if (arg.startsWith("-i")) {
                        try {
                            runner.iterations = Integer.parseInt(arg.substring(2));
                        } catch (NumberFormatException e) {}
                    }
            }
        }
        
        runner.runAll();
    }
    
    public void runAll() {
        System.out.println("\n" + "鈺? + "鈺?.repeat(68) + "鈺?);
        System.out.println("鈺? + " ".repeat(15) + "DDS 鍩哄噯娴嬭瘯濂椾欢" + " ".repeat(37) + "鈺?);
        System.out.println("鈺? + "鈺?.repeat(68) + "鈺漒n");
        
        // 纭繚杈撳嚭鐩綍瀛樺湪
        try {
            Files.createDirectories(Paths.get(RESULTS_DIR));
        } catch (IOException e) {
            System.err.println("鍒涘缓缁撴灉鐩綍澶辫触: " + e.getMessage());
            return;
        }
        
        long overallStart = System.currentTimeMillis();
        
        // 杩愯鍚勯」娴嬭瘯
        if (runThroughput) {
            runThroughputTest();
        }
        
        if (runLatency) {
            runLatencyTest();
        }
        
        if (runStress) {
            runStressTest();
        }

        if (runCoordinationStress) {
            runCoordinationStressTest();
        }
        
        long overallDuration = System.currentTimeMillis() - overallStart;
        
        // 鐢熸垚缁煎悎鎶ュ憡
        generateSummaryReport(overallDuration);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  鎵€鏈夋祴璇曞畬鎴?");
        System.out.println("  鎬昏€楁椂: " + formatDuration(overallDuration));
        System.out.println("  缁撴灉鐩綍: " + RESULTS_DIR);
        System.out.println("=".repeat(70));
    }
    
    private void runThroughputTest() {
        System.out.println("[1/4] 鍚炲悙閲忔祴璇?);
        System.out.println("-".repeat(50));
        
        try {
            ThroughputBenchmark benchmark = new ThroughputBenchmark();
            benchmark.runAllTests();
            benchmark.close();
            System.out.println("鉁?鍚炲悙閲忔祴璇曞畬鎴怽n");
        } catch (Exception e) {
            System.err.println("鉁?鍚炲悙閲忔祴璇曞け璐? " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void runLatencyTest() {
        System.out.println("[2/4] 寤惰繜娴嬭瘯");
        System.out.println("-".repeat(50));
        
        try {
            LatencyBenchmark benchmark = new LatencyBenchmark();
            benchmark.runAllTests();
            benchmark.close();
            System.out.println("鉁?寤惰繜娴嬭瘯瀹屾垚\n");
        } catch (Exception e) {
            System.err.println("鉁?寤惰繜娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void runStressTest() {
        System.out.println("[3/4] 鍘嬪姏娴嬭瘯");
        System.out.println("-".repeat(50));
        
        try {
            StressTest test = new StressTest();
            test.runAllTests();
            System.out.println("鉁?鍘嬪姏娴嬭瘯瀹屾垚\n");
        } catch (Exception e) {
            System.err.println("鉁?鍘嬪姏娴嬭瘯澶辫触: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runCoordinationStressTest() {
        System.out.println("[4/4] 鍗忓悓绋冲畾鎬у帇娴?);
        System.out.println("-".repeat(50));

        try {
            CoordinationStressBenchmark benchmark = new CoordinationStressBenchmark();
            benchmark.run(Math.max(50, iterations * 100));
            benchmark.close();
            System.out.println("鉁?鍗忓悓绋冲畾鎬у帇娴嬪畬鎴怽n");
        } catch (Exception e) {
            System.err.println("鉁?鍗忓悓绋冲畾鎬у帇娴嬪け璐? " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 鐢熸垚娴嬭瘯鎽樿鎶ュ憡
     */
    private void generateSummaryReport(long totalDuration) {
        String timestamp = DATE_FORMAT.format(new Date());
        String reportFile = RESULTS_DIR + "/summary_" + timestamp + ".txt";
        String htmlFile = RESULTS_DIR + "/benchmark_report_" + timestamp + ".html";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("DDS 鍩哄噯娴嬭瘯鎽樿鎶ュ憡");
            writer.println("鐢熸垚鏃堕棿: " + new Date());
            writer.println("鎬昏€楁椂: " + formatDuration(totalDuration));
            writer.println();
            
            writer.println("娴嬭瘯閰嶇疆:");
            writer.println("  - 鍚炲悙閲忔祴璇? " + (runThroughput ? "鍚敤" : "绂佺敤"));
            writer.println("  - 寤惰繜娴嬭瘯: " + (runLatency ? "鍚敤" : "绂佺敤"));
            writer.println("  - 鍘嬪姏娴嬭瘯: " + (runStress ? "鍚敤" : "绂佺敤"));
            writer.println("  - 鍗忓悓绋冲畾鎬у帇娴? " + (runCoordinationStress ? "鍚敤" : "绂佺敤"));
            writer.println();
            
            // 璇诲彇骞跺悎骞剁粨鏋?
            writer.println("璇︾粏缁撴灉鏂囦欢:");
            writer.println("  - 鍚炲悙閲忕粨鏋? " + RESULTS_DIR + "/throughput_results.csv");
            writer.println("  - 寤惰繜缁撴灉: " + RESULTS_DIR + "/latency_results.csv");
            writer.println("  - 寤惰繜鐩存柟鍥? " + RESULTS_DIR + "/latency_histogram.csv");
            writer.println("  - 鍘嬪姏娴嬭瘯缁撴灉: " + RESULTS_DIR + "/stress_test_results.csv");
            writer.println("  - 鍘嬪姏娴嬭瘯鎸囨爣: " + RESULTS_DIR + "/stress_metrics.csv");
            writer.println("  - 鍗忓悓绋冲畾鎬у帇娴? " + RESULTS_DIR + "/coordination_stress_results.csv");
            
            System.out.println("鎽樿鎶ュ憡宸茬敓鎴? " + reportFile);
            
        } catch (IOException e) {
            System.err.println("鐢熸垚鎽樿鎶ュ憡澶辫触: " + e.getMessage());
        }
        
        // 鐢熸垚 HTML 鎶ュ憡
        generateHtmlReport(htmlFile);
    }
    
    /**
     * 鐢熸垚 HTML 鎶ュ憡
     */
    private void generateHtmlReport(String htmlFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlFile))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='zh-CN'>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <title>DDS 鍩哄噯娴嬭瘯鎶ュ憡</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
            writer.println("        .container { max-width: 1000px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            writer.println("        h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
            writer.println("        h2 { color: #555; margin-top: 30px; }");
            writer.println("        table { width: 100%; border-collapse: collapse; margin: 15px 0; }");
            writer.println("        th, td { padding: 10px; text-align: left; border: 1px solid #ddd; }");
            writer.println("        th { background-color: #007bff; color: white; }");
            writer.println("        tr:nth-child(even) { background-color: #f8f9fa; }");
            writer.println("        .metric { font-weight: bold; color: #28a745; }");
            writer.println("        .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class='container'>");
            writer.println("        <h1>馃И DDS 鍩哄噯娴嬭瘯鎶ュ憡</h1>");
            writer.println("        <p><strong>鐢熸垚鏃堕棿:</strong> " + new Date() + "</p>");
            writer.println("        <p><strong>娴嬭瘯妯″潡:</strong> DSL-IDL-DDS 闆嗘垚骞冲彴</p>");
            
            writer.println("        <h2>馃搳 鍚炲悙閲忔祴璇曠粨鏋?/h2>");
            writer.println("        <p>娴嬭瘯涓嶅悓娑堟伅澶у皬鐨勫彂甯冨悶鍚愰噺</p>");
            writer.println("        <p><a href='throughput_results.csv'>鏌ョ湅璇︾粏鏁版嵁 (CSV)</a></p>");
            
            writer.println("        <h2>鈴憋笍 寤惰繜娴嬭瘯缁撴灉</h2>");
            writer.println("        <p>娴嬮噺绔埌绔秷鎭欢杩熷強鍒嗗竷</p>");
            writer.println("        <p><a href='latency_results.csv'>寤惰繜缁熻</a> | <a href='latency_histogram.csv'>寤惰繜鐩存柟鍥?/a></p>");
            
            writer.println("        <h2>馃挭 鍘嬪姏娴嬭瘯缁撴灉</h2>");
            writer.println("        <p>澶氫富棰樺苟鍙戝拰闀挎椂闂寸ǔ瀹氭€ф祴璇?/p>");
            writer.println("        <p><a href='stress_test_results.csv'>娴嬭瘯缁撴灉</a> | <a href='stress_metrics.csv'>杩愯鏃舵寚鏍?/a></p>");

            writer.println("        <h2>馃 鍗忓悓绋冲畾鎬у帇娴?/h2>");
            writer.println("        <p>楠岃瘉 COORDINATION 鎰忓浘鍦ㄩ珮棰戣緭鍏ヤ笅鐨勮瘑鍒ǔ瀹氭€т笌绛栫暐涓婚瑕嗙洊鐜?/p>");
            writer.println("        <p><a href='coordination_stress_results.csv'>鍗忓悓鍘嬫祴缁撴灉</a></p>");
            
            writer.println("        <div class='footer'>");
            writer.println("            <p>鍩轰簬鏉庡鍏磋鏂囪璁＄殑 DDS/DSL 闆嗘垚骞冲彴</p>");
            writer.println("            <p>绗?绔?5.3 DDS閫氫俊鎬ц兘浼樺寲</p>");
            writer.println("        </div>");
            writer.println("    </div>");
            writer.println("</body>");
            writer.println("</html>");
            
            System.out.println("HTML 鎶ュ憡宸茬敓鎴? " + htmlFile);
            
        } catch (IOException e) {
            System.err.println("鐢熸垚 HTML 鎶ュ憡澶辫触: " + e.getMessage());
        }
    }
    
    /**
     * 鏍煎紡鍖栨椂闀?
     */
    private String formatDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        if (ms < 60000) return String.format("%.1f 绉?, ms / 1000.0);
        return String.format("%d 鍒?%d 绉?, ms / 60000, (ms % 60000) / 1000);
    }
}
