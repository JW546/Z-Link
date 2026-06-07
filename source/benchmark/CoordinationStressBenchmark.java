package benchmark;

import agent.CPSAgent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 协同场景压力与稳定性基准测试。
 * 独立于 DDS 吞吐测试，专注验证 COORDINATION 意图链路在高频输入下的稳定性。
 */
public class CoordinationStressBenchmark {

    private static final String RESULTS_DIR = "b";
    private static final int DEFAULT_ITERATIONS = 300;

    private final CPSAgent agent;
    private final PrintWriter csvWriter;

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong topicCoverageCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());

    public CoordinationStressBenchmark() throws IOException {
        this.agent = new CPSAgent();
        Files.createDirectories(Paths.get(RESULTS_DIR));
        this.csvWriter = new PrintWriter(new FileWriter(RESULTS_DIR + "/coordination_stress_results.csv"));
        csvWriter.println("round,input,intent_ok,topic_coverage_ok,latency_ms,error");
    }

    public void runAllTests() {
        run(DEFAULT_ITERATIONS);
    }

    public void run(int iterations) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  协同意图稳定性基准测试");
        System.out.println("=".repeat(70));

        String[] inputs = {
            "乘客说我要尽快去西南大学附属医院，请无人车联动后车和无人机协同规划路线",
            "车内乘客要求更稳路线，无人车自主订阅无人机和前车建议",
            "无人车根据乘客自然语言意图发布路线建议并融合合规广域态势"
        };

        for (int i = 0; i < iterations; i++) {
            String input = inputs[i % inputs.length];
            long start = System.nanoTime();
            boolean intentOk = false;
            boolean topicOk = false;
            String error = "";

            try {
                CPSAgent.AgentResult result = agent.processInput(input);
                intentOk = "COORDINATION".equals(result.intent);
                topicOk = result.publishTopics.contains("CompliantBackboneSync")
                    && result.publishTopics.contains("SemanticTaskDSL")
                    && result.subscribeTopics.contains("GlobalPerception");

                if (intentOk) {
                    successCount.incrementAndGet();
                }
                if (topicOk) {
                    topicCoverageCount.incrementAndGet();
                }
            } catch (Exception ex) {
                errorCount.incrementAndGet();
                error = ex.getClass().getSimpleName() + ":" + ex.getMessage();
            }

            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            latenciesMs.add(latencyMs);

            csvWriter.printf("%d,\"%source\",%s,%s,%d,\"%source\"%n",
                i + 1,
                input,
                intentOk ? "1" : "0",
                topicOk ? "1" : "0",
                latencyMs,
                error == null ? "" : error.replace("\"", "'"));
        }

        csvWriter.flush();
        printSummary(iterations);
    }

    private void printSummary(int iterations) {
        if (latenciesMs.isEmpty()) {
            System.out.println("无样本，跳过统计。");
            return;
        }

        List<Long> sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);

        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50 = percentile(sorted, 0.50);
        long p95 = percentile(sorted, 0.95);
        long p99 = percentile(sorted, 0.99);
        long max = sorted.get(sorted.size() - 1);

        System.out.printf("总轮次: %d%n", iterations);
        System.out.printf("COORDINATION识别成功率: %.2f%%%n", successCount.get() * 100.0 / iterations);
        System.out.printf("策略主题覆盖率: %.2f%%%n", topicCoverageCount.get() * 100.0 / iterations);
        System.out.printf("错误率: %.2f%%%n", errorCount.get() * 100.0 / iterations);
        System.out.printf("时延统计(ms): avg=%.2f, p50=%d, p95=%d, p99=%d, max=%d%n", avg, p50, p95, p99, max);
        System.out.println("结果文件: " + RESULTS_DIR + "/coordination_stress_results.csv");
    }

    private long percentile(List<Long> sorted, double p) {
        int idx = (int) Math.floor((sorted.size() - 1) * p);
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, idx)));
    }

    public void close() {
        csvWriter.close();
    }

    public static void main(String[] args) {
        int iterations = DEFAULT_ITERATIONS;
        if (args.length > 0) {
            try {
                iterations = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
                // 使用默认值
            }
        }

        try {
            CoordinationStressBenchmark benchmark = new CoordinationStressBenchmark();
            benchmark.run(iterations);
            benchmark.close();
        } catch (Exception e) {
            System.err.println("协同压测失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
