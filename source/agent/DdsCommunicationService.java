package agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DDS 通信统一入口。
 *
 * 优先探测真实 DDS 管理类；真实环境不可用时自动回退到内存模拟，保证免安装版本可运行。
 */
public class DdsCommunicationService {
    public static final String REAL_DDS = "REAL_DDS";
    public static final String SIMULATED_DDS = "SIMULATED_DDS";
    public static final String FALLBACK_SIMULATION = "FALLBACK_SIMULATION";

    private static final DdsCommunicationService INSTANCE = new DdsCommunicationService();
    private final List<Map<String, Object>> logs = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
    private final Random random = new Random(20260511);
    private final FaultInjectionService faults = FaultInjectionService.getInstance();
    private String runtimeMode = FALLBACK_SIMULATION;
    private Object ddsManager;

    private DdsCommunicationService() {
        detectRuntime();
    }

    public static DdsCommunicationService getInstance() {
        return INSTANCE;
    }

    public String getRuntimeMode() {
        return runtimeMode;
    }

    public Map<String, Object> publish(String missionId, String topic, Object payload, QosPolicyModel qos) {
        QosPolicyModel policy = qos == null ? QosPolicyModel.create(topic, true, "默认可靠发布策略") : qos;
        Map<String, Object> event = event(missionId, "", "PUBLISH", topic, "SENT",
            "message accepted by " + runtimeMode, 0, policy, 0);
        if (REAL_DDS.equals(runtimeMode)) {
            tryInvokeRealPublish(topic, payload);
        }
        logs.add(event);
        return event;
    }

    public Map<String, Object> subscribe(String missionId, String topic, String filter, QosPolicyModel qos) {
        QosPolicyModel policy = qos == null ? QosPolicyModel.create(topic, true, "默认可靠订阅策略") : qos;
        Map<String, Object> event = event(missionId, "", "SUBSCRIBE", topic, "DELIVERED",
            "subscriber callback completed" + (filter == null || filter.isBlank() ? "" : " filter=" + filter), 0, policy, 0);
        logs.add(event);
        return event;
    }

    public List<Map<String, Object>> simulateExchange(MissionDslModel mission, List<Map<String, Object>> topics,
                                                      List<QosPolicyModel> qosPolicies,
                                                      DynamicTaskScheduler.ScheduleResult schedule) {
        List<Map<String, Object>> trace = new ArrayList<Map<String, Object>>();
        String missionId = mission == null ? "MSN-UNKNOWN" : mission.missionId;
        List<Map<String, Object>> safeTopics = topics == null ? Collections.<Map<String, Object>>emptyList() : topics;
        for (Map<String, Object> topic : safeTopics) {
            String name = String.valueOf(topic.getOrDefault("name", "CollaborationPlanTopic"));
            QosPolicyModel qos = findQos(qosPolicies, name);
            if (faults.hasType("DDS_PUBLISH_FAILED")) {
                Map<String, Object> failed = event(missionId, "", "PUBLISH", name, "FAILED",
                    "fault injected: DDS publish failed, fallback required", 0, qos, qos.latencyBudgetMs + 80);
                Map<String, Object> retry = event(missionId, "", "RETRY", name, "RETRYING",
                    "retry after injected DDS publish failure", 1, qos, qos.latencyBudgetMs + 120);
                trace.add(failed);
                trace.add(retry);
                logs.add(failed);
                logs.add(retry);
                continue;
            }
            trace.add(publish(missionId, name, mission == null ? Collections.emptyMap() : mission.toMap(), qos));
            boolean reliable = "RELIABLE".equals(qos.reliability);
            boolean firstFail = reliable && random.nextDouble() < 0.25;
            if (firstFail) {
                Map<String, Object> retry = event(missionId, "", "RETRY", name, "RETRYING",
                    "simulated wireless loss, retry with reliable QoS", 1, qos, qos.latencyBudgetMs + 20);
                Map<String, Object> delivered = event(missionId, "", "SUBSCRIBE", name, "DELIVERED",
                    "retry delivered before deadline", 1, qos, qos.latencyBudgetMs - 10);
                trace.add(retry);
                trace.add(delivered);
                logs.add(retry);
                logs.add(delivered);
            } else if (!reliable && random.nextDouble() < 0.15) {
                Map<String, Object> dropped = event(missionId, "", "DEGRADE", name, "DROPPED",
                    "best-effort packet dropped, waiting next sample", 0, qos, qos.latencyBudgetMs);
                trace.add(dropped);
                logs.add(dropped);
            } else {
                trace.add(subscribe(missionId, name, "mission_id=" + missionId, qos));
            }
        }
        if (schedule != null && !schedule.unassignedTasks.isEmpty()) {
            QosPolicyModel qos = QosPolicyModel.create("CollaborationPlanTopic", true, "未分配任务触发可靠降级通知");
            Map<String, Object> degrade = event(missionId, "", "DEGRADE", "CollaborationPlanTopic", "DEGRADED",
                "unassigned tasks trigger fallback strategy", 0, qos, 120);
            trace.add(degrade);
            logs.add(degrade);
        }
        return trace;
    }

    public List<Map<String, Object>> getCommunicationLogs() {
        synchronized (logs) {
            return new ArrayList<Map<String, Object>>(logs);
        }
    }

    public List<Map<String, Object>> getCommunicationLogs(String missionId, String runId, int limit) {
        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        synchronized (logs) {
            for (Map<String, Object> log : logs) {
                boolean missionMatch = missionId == null || missionId.isBlank() || missionId.equals(String.valueOf(log.get("missionId")));
                boolean runMatch = runId == null || runId.isBlank() || runId.equals(String.valueOf(log.get("runId")));
                if (missionMatch && runMatch) filtered.add(new LinkedHashMap<String, Object>(log));
            }
        }
        int from = Math.max(0, filtered.size() - Math.max(1, limit));
        return new ArrayList<Map<String, Object>>(filtered.subList(from, filtered.size()));
    }

    private QosPolicyModel findQos(List<QosPolicyModel> policies, String topic) {
        if (policies != null) {
            for (QosPolicyModel policy : policies) {
                if (policy != null && topic.equals(policy.topic)) return policy;
            }
        }
        return QosPolicyModel.create(topic, true, "未找到显式 QoS，采用可靠协同默认策略。");
    }

    private Map<String, Object> event(String missionId, String runId, String phase, String topic, String status,
                                      String reason, int retryCount, QosPolicyModel qos, int latencyMs) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("timestamp", System.currentTimeMillis());
        event.put("missionId", missionId);
        event.put("runId", runId);
        event.put("phase", phase);
        event.put("topic", topic);
        event.put("status", status);
        event.put("reason", reason);
        event.put("retryCount", retryCount);
        event.put("latencyMs", latencyMs <= 0 ? Math.max(8, qos.latencyBudgetMs / 2) : latencyMs);
        event.put("runtimeMode", runtimeMode);
        event.put("qos", qos.toMap());
        event.put("fallbackPolicy", qos.fallbackPolicy);
        return event;
    }

    private void detectRuntime() {
        try {
            Class<?> clazz = Class.forName("dds.DDSManager");
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            ddsManager = constructor.newInstance();
            runtimeMode = SIMULATED_DDS;
            // 当前免安装版通常没有原生 DDS 运行时；探测到管理类但未确认 native runtime，标记为模拟兼容模式。
        } catch (Throwable ex) {
            runtimeMode = FALLBACK_SIMULATION;
            ddsManager = null;
        }
    }

    private void tryInvokeRealPublish(String topic, Object payload) {
        if (ddsManager == null) return;
        try {
            Method method = ddsManager.getClass().getMethod("publish", String.class, Object.class);
            method.invoke(ddsManager, topic, payload);
            runtimeMode = REAL_DDS;
        } catch (Throwable ignored) {
            runtimeMode = SIMULATED_DDS;
        }
    }
}
