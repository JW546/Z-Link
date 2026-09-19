package collaboration.runtime;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CoordinatorAgent implements Runnable, Closeable {
    private final String host;
    private final int port;
    private final int expectedDevices;
    private final long timeoutMs;
    private final RuntimeCoordinationPolicy policy = new RuntimeCoordinationPolicy();
    private final Map<String, RuntimeMessage> latestStatus = new LinkedHashMap<String, RuntimeMessage>();
    private final CountDownLatch finalDecisionLatch = new CountDownLatch(1);
    private volatile LocalDDSClient client;
    private volatile RuntimeMessage finalDecision;
    private volatile RuntimeMessage conflictEvent;

    public CoordinatorAgent(String host, int port, int expectedDevices, long timeoutMs) {
        this.host = host;
        this.port = port;
        this.expectedDevices = expectedDevices <= 0 ? 3 : expectedDevices;
        this.timeoutMs = timeoutMs <= 0L ? 8_000L : timeoutMs;
    }

    public void run() {
        try {
            client = new LocalDDSClient(host, port, "COORDINATOR-CQ-001");
            client.onMessage(this::handle);
            client.start();
            client.subscribe(LocalDDSBroker.TOPIC_AGENT_STATUS);
            publishIntentContract();
            finalDecisionLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Coordinator agent failed.", e);
        } finally {
            close();
        }
    }

    public RuntimeMessage finalDecision() {
        return finalDecision;
    }

    public RuntimeMessage conflictEvent() {
        return conflictEvent;
    }

    public void close() {
        LocalDDSClient openClient = client;
        if (openClient != null) {
            openClient.close();
        }
    }

    private synchronized void handle(RuntimeMessage message) {
        latestStatus.put(message.sourceId, message);
        if (latestStatus.size() >= expectedDevices) {
            evaluate();
        }
    }

    private void publishIntentContract() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("missionId", "MSN-RUNTIME-LOCAL-DDS");
        payload.put("naturalLanguage", "无人机先侦察 A 区，自车接近目标点，peer car 协同避让；发生路线冲突时动态重规划。");
        payload.put("dslContract", "intent -> DSL task contract -> DDS topics -> multi-agent coordination");
        payload.put("requiredAgents", "UAV-CQ-007,CAR-CQ-001,PEER-CAR-031,COORDINATOR-CQ-001");
        payload.put("defaultStatusIntervalMs", 500);
        client.publish(LocalDDSBroker.TOPIC_INTENT_CONTRACT, payload);
    }

    private void evaluate() {
        RuntimeMessage ego = latestStatus.get("CAR-CQ-001");
        RuntimeMessage peer = latestStatus.get("PEER-CAR-031");
        RuntimeMessage uav = latestStatus.get("UAV-CQ-007");
        if (ego == null || peer == null || uav == null || finalDecision != null) {
            return;
        }

        RuntimeCoordinationPolicy.RuntimeCoordinationDecision policyDecision = policy.decide(ego, peer, uav);
        String egoRoute = String.valueOf(ego.payload.get("route"));
        if (policyDecision.occupancyConflict) {
            Map<String, Object> conflict = new LinkedHashMap<String, Object>();
            conflict.put("conflictId", "CFL-" + System.currentTimeMillis());
            conflict.put("conflictType", policyDecision.conflictType());
            conflict.put("involvedAgents", "CAR-CQ-001,PEER-CAR-031");
            conflict.put("route", egoRoute);
            conflict.put("severity", 0.74);
            conflict.put("explanation", "自车与 peer car 同时选择主路，协调 Agent 调用冲突消解策略触发动态重规划。");
            conflict.put("resolution", policyDecision.resolution);
            client.publish(LocalDDSBroker.TOPIC_CONFLICT_EVENT, conflict);
            conflictEvent = RuntimeMessage.publish(LocalDDSBroker.TOPIC_CONFLICT_EVENT, "COORDINATOR-CQ-001", conflict);

            Map<String, Object> replan = new LinkedHashMap<String, Object>();
            replan.put("missionId", "MSN-RUNTIME-LOCAL-DDS");
            replan.put("reason", "route conflict between ego car and peer car");
            replan.put("request", "ego car switches to green-wave detour, peer car keeps low-risk spacing");
            replan.put("resolution", policyDecision.resolution);
            client.publish(LocalDDSBroker.TOPIC_REPLAN_REQUEST, replan);
        }

        Map<String, Object> decision = new LinkedHashMap<String, Object>();
        decision.put("missionId", "MSN-RUNTIME-LOCAL-DDS");
        decision.put("selectedStrategy", policyDecision.selectedStrategy());
        decision.put("selectedRoute", policyDecision.selectedRoute());
        decision.put("baselineComparison", "dijkstra=shorter_but_conflicted; greedy=fast_but_risky; conflict_resolver=lower_risk_with_replanning");
        decision.put("explanation", policyDecision.explanation());
        decision.put("resolution", policyDecision.resolution);
        decision.put("ros2DdsReady", true);
        client.publish(LocalDDSBroker.TOPIC_FINAL_DECISION, decision);
        finalDecision = RuntimeMessage.publish(LocalDDSBroker.TOPIC_FINAL_DECISION, "COORDINATOR-CQ-001", decision);

        Map<String, Object> evaluation = new LinkedHashMap<String, Object>();
        evaluation.put("dijkstraRisk", 0.62);
        evaluation.put("greedyRisk", 0.55);
        evaluation.put("cooperativeRisk", 0.24);
        evaluation.put("conflictCount", policyDecision.occupancyConflict ? 1 : 0);
        evaluation.put("winner", policyDecision.selectedStrategy());
        evaluation.put("resolutionConfidence", policyDecision.resolution.get("confidence"));
        client.publish(LocalDDSBroker.TOPIC_POLICY_EVALUATION, evaluation);
        waitForBrokerFlush();
        finalDecisionLatch.countDown();
    }

    private void waitForBrokerFlush() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
