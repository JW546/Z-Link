package integration;

import dds.CoordinationTopicConsumer;
import dds.CoordinationTopicMapper;
import dds.CoordinationTopicProducer;
import dds.DDSManager;
import protocol.MultiLinkStrategyEngine;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 端到端演示：新语义主题 -> DDS IDL映射 -> 生产/消费回调。
 */
public class CoordinationDDSEndToEndDemo {

    public static void main(String[] args) {
        printLinkPlan();

        DDSManager dds = DDSManager.getInstance(0);
        if (!dds.connect()) {
            System.err.println("[FAIL] DDS连接失败，无法执行端到端演示");
            System.exit(1);
        }

        CoordinationTopicProducer producer = new CoordinationTopicProducer(dds);
        CoordinationTopicConsumer consumer = new CoordinationTopicConsumer(dds);
        AtomicInteger consumedCount = new AtomicInteger(0);

        consumer.onSemanticTopic(CoordinationTopicMapper.TOPIC_FORWARD_TRAFFIC, (topic, payload) -> {
            consumedCount.incrementAndGet();
            System.out.println("[CONSUME] " + topic + " -> congestionLevel=" + payload.get("congestionLevel"));
        });
        consumer.onSemanticTopic(CoordinationTopicMapper.TOPIC_FORWARD_CORRIDOR, (topic, payload) -> {
            consumedCount.incrementAndGet();
            System.out.println("[CONSUME] " + topic + " -> visibility=" + payload.get("visibility"));
        });
        consumer.onSemanticTopic(CoordinationTopicMapper.TOPIC_GLOBAL_PERCEPTION, (topic, payload) -> {
            consumedCount.incrementAndGet();
            System.out.println("[CONSUME] " + topic + " -> globalFlowSpeed=" + payload.get("globalFlowSpeed"));
        });
        consumer.onSemanticTopic(CoordinationTopicMapper.TOPIC_REPLAN_TRIGGER, (topic, payload) -> {
            consumedCount.incrementAndGet();
            System.out.println("[CONSUME] " + topic + " -> triggerReason=" + payload.get("triggerReason"));
        });

        consumer.subscribeAll();
        consumer.subscribeReplanTrigger();

        System.out.println("============================================================");
        System.out.println("  场景: 沙坪坝 -> 西南大学, 前方路口拥堵突增, 触发协同重规划");
        System.out.println("============================================================");

        Map<String, Object> forwardTraffic = new HashMap<>();
        forwardTraffic.put("intersectionId", "INT-SWP-001");
        forwardTraffic.put("queueLength", 17);
        forwardTraffic.put("averageSpeed", 11.5);
        forwardTraffic.put("congestionLevel", 0.82);
        forwardTraffic.put("latitude", 29.5652);
        forwardTraffic.put("longitude", 106.4571);

        Map<String, Object> corridor = new HashMap<>();
        corridor.put("sourceId", "UAV-CQ-17");
        corridor.put("visibility", 1350.0);
        corridor.put("windSpeed", 9.3);
        corridor.put("riskScore", 0.63);
        corridor.put("temperature", 21.0);

        Map<String, Object> global = new HashMap<>();
        global.put("sourceId", "CN-BACKBONE-CQ");
        global.put("globalFlowSpeed", 16.8);
        global.put("heading", 45.0);
        global.put("healthScore", 97);

        Map<String, Object> replan = new HashMap<>();
        replan.put("alertId", 9001);
        replan.put("triggerReason", "forward_congestion_spike");
        replan.put("recommendation", "切换经大学城内环的备选路径");
        replan.put("radius", 600.0);

        long seq1 = producer.publish(CoordinationTopicMapper.TOPIC_FORWARD_TRAFFIC, forwardTraffic);
        long seq2 = producer.publish(CoordinationTopicMapper.TOPIC_FORWARD_CORRIDOR, corridor);
        long seq3 = producer.publish(CoordinationTopicMapper.TOPIC_GLOBAL_PERCEPTION, global);
        long seq4 = producer.publish(CoordinationTopicMapper.TOPIC_REPLAN_TRIGGER, replan);

        System.out.println("[PUBLISH] seq=" + seq1 + ", topic=" + CoordinationTopicMapper.TOPIC_FORWARD_TRAFFIC);
        System.out.println("[PUBLISH] seq=" + seq2 + ", topic=" + CoordinationTopicMapper.TOPIC_FORWARD_CORRIDOR);
        System.out.println("[PUBLISH] seq=" + seq3 + ", topic=" + CoordinationTopicMapper.TOPIC_GLOBAL_PERCEPTION);
        System.out.println("[PUBLISH] seq=" + seq4 + ", topic=" + CoordinationTopicMapper.TOPIC_REPLAN_TRIGGER);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[SUMMARY] 消费回调条数=" + consumedCount.get());
        if (consumedCount.get() < 4) {
            System.out.println("[WARN] 未收到全部回调，请确认本机DDS环境与native库加载。映射与生产链路已完成。");
        } else {
            System.out.println("[PASS] 新语义主题已通过DDS映射并完成端到端收发。");
        }

        dds.printStatus();
        dds.disconnect();
    }

    private static void printLinkPlan() {
        MultiLinkStrategyEngine engine = new MultiLinkStrategyEngine();
        MultiLinkStrategyEngine.LinkContext ctx = new MultiLinkStrategyEngine.LinkContext();

        // 默认策略: 国内合规广域链路可插拔、轻边缘启用、支持远距协同。
        ctx.compliantBackboneAllowedInRegion = false;
        ctx.compliantBackboneRuntimeAvailable = false;
        ctx.private5GAvailable = true;
        ctx.beidouShortMessageAvailable = true;
        ctx.edgeNodeAvailable = true;
        ctx.missionCritical = true;
        ctx.longDistanceCoordination = true;

        Properties p = loadSatcomProperties();
        String runMode = p.getProperty("satcom.run.mode", "mock");
        String role = p.getProperty("satcom.backbone.role", "compliant-backbone");
        String policy = p.getProperty("satcom.policy.cn", "approved-systems-only");

        ctx.compliantBackboneRuntimeAvailable = false;
        ctx.compliantBackboneAllowedInRegion = false;
        ctx.cnLeoSatcomAvailable = Boolean.parseBoolean(p.getProperty("satcom.cn-leo.enabled", "false"));
        ctx.private5GAvailable = Boolean.parseBoolean(p.getProperty("satcom.private-5ga.enabled", "true"));
        ctx.beidouShortMessageAvailable = Boolean.parseBoolean(p.getProperty("satcom.beidou-short-message.enabled", "true"));

        MultiLinkStrategyEngine.PlanResult result = engine.plan(ctx);

        System.out.println("============================================================");
        System.out.println("  多链路策略规划 (竞赛/论文/创业共用)");
        System.out.println("============================================================");
        System.out.println("[CONFIG] run.mode=" + runMode + ", backbone.role=" + role + ", cn.policy=" + policy);
        System.out.print(engine.toSummary(result));
        System.out.println("[FAILOVER-ORDER] " + engine.recommendedFailoverOrder(result));
    }

    private static Properties loadSatcomProperties() {
        Properties p = new Properties();
        String[] candidates = {"satcom.properties", "sl.example"};
        for (String file : candidates) {
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
                return p;
            } catch (Exception ignored) {
                // 尝试下一个候选文件。
            }
        }
        return p;
    }
}
