package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务执行仿真服务。
 *
 * 每次 execute/simulate 都生成 runId 和可回放时间线，覆盖意图、DSL、IDL、QoS、路径、
 * 调度、DDS 发布订阅、设备确认、风险检查和完成状态。
 */
public class MissionExecutionService {
    private static final MissionExecutionService INSTANCE = new MissionExecutionService();
    private final ConcurrentHashMap<String, Map<String, Object>> runs = new ConcurrentHashMap<String, Map<String, Object>>();
    private final FaultInjectionService faults = FaultInjectionService.getInstance();
    private final MissionHistoryService history = MissionHistoryService.getInstance();

    public static MissionExecutionService getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> createExecution(Map<String, Object> platformPlan) {
        Map<String, Object> plan = platformPlan == null ? new LinkedHashMap<String, Object>() : platformPlan;
        String missionId = String.valueOf(plan.getOrDefault("missionId", "MSN-" + System.currentTimeMillis()));
        String runId = "RUN-" + missionId + "-" + System.currentTimeMillis();
        List<ExecutionTimelineEvent> events = new ArrayList<ExecutionTimelineEvent>();

        events.add(event("INTENT_PARSED", "NLU", "", "parse", "DONE", "自然语言意图已解析", 18, 0, "LOW"));
        events.add(event("DSL_GENERATED", "MissionDslModel", "", "build-schema", "DONE", "结构化 DSL 已生成并保留 legacy DSL 兼容字段", 12, 0, "LOW"));
        events.add(event("IDL_BOUND", "IDL Binder", topicName(plan, 0), "bind", "DONE", "Topic 已匹配 IDL 类型", 16, 0, "LOW"));
        events.add(event("QOS_SELECTED", "QoS Selector", topicName(plan, 0), "select-qos", "DONE", "已按任务紧急程度选择 QoS", 10, 0, "LOW"));
        events.add(event("ROUTE_PLANNED", "AdaptiveRoutePlanner", "", "a-star-search", "DONE", "已完成 A*/Dijkstra 动态边权路径规划", 24, 0, risk(plan)));
        events.add(event("TASK_SCHEDULED", "DynamicTaskScheduler", "", "greedy-schedule", "DONE", "已完成动态权重贪心任务调度", 20, 0, risk(plan)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ddsLogs = (List<Map<String, Object>>) plan.get("ddsSimulation");
        if (ddsLogs != null && !ddsLogs.isEmpty()) {
            int added = 0;
            for (Map<String, Object> log : ddsLogs) {
                String phase = String.valueOf(log.getOrDefault("phase", "DDS"));
                String topic = String.valueOf(log.getOrDefault("topic", ""));
                int retry = number(log.get("retryCount"), 0);
                String status = String.valueOf(log.getOrDefault("status", "DONE"));
                String stage = phase.contains("SUB") ? "DDS_SUBSCRIBE" : phase.contains("PUBLISH") ? "DDS_PUBLISH" : phase.contains("RETRY") ? "FALLBACK_TRIGGERED" : "DDS_PUBLISH";
                events.add(event(stage, "DdsCommunicationService", topic, phase, status,
                    String.valueOf(log.getOrDefault("reason", "DDS exchange")), number(log.get("latencyMs"), 30), retry,
                    status.contains("DROP") || status.contains("DEGRADE") ? "MEDIUM" : "LOW"));
                added++;
                if (added >= 8) break;
            }
        } else {
            events.add(event("DDS_PUBLISH", "DdsCommunicationService", "CollaborationPlanTopic", "publish", "DONE", "模拟发布协同计划", 32, 0, "LOW"));
            events.add(event("DDS_SUBSCRIBE", "DdsCommunicationService", "VehicleStatusTopic", "subscribe", "DONE", "模拟订阅设备反馈", 28, 0, "LOW"));
        }

        events.add(event("DEVICE_ACK", "DeviceRegistry", "VehicleStatusTopic", "ack", "DONE", "设备注册中心已确认可调度设备状态", 18, 0, risk(plan)));
        events.add(event("RISK_CHECK", "RiskEngine", "TrafficRiskTopic", "evaluate", "DONE", "已完成风险与降级策略检查", 15, 0, risk(plan)));
        if (!faults.list().isEmpty()) {
            events.add(event("FALLBACK_TRIGGERED", "FaultInjectionService", "CollaborationPlanTopic", "fallback", "DEGRADED",
                "检测到故障注入，已启用降级策略", 35, 1, "HIGH"));
        }
        events.add(event("COMPLETED", "MissionExecutionService", "CollaborationPlanTopic", "complete", "DONE", "任务执行仿真闭环完成", 12, 0, "LOW"));

        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        for (ExecutionTimelineEvent event : events) {
            timeline.add(event.toMap());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("accepted", true);
        result.put("missionId", missionId);
        result.put("runId", runId);
        result.put("status", "SIMULATED_EXECUTION_COMPLETED");
        result.put("timeline", timeline);
        result.put("executionTimeline", timeline);
        result.put("timelineSummary", timelineSummary(timeline));
        result.put("riskAssessment", plan.get("riskAssessment"));
        result.put("message", "任务 " + missionId + " 已生成可回放执行时间线。");
        runs.put(runId, result);
        history.recordExecution(plan, result);
        return result;
    }

    public List<Map<String, Object>> logs(String missionId, String runId) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> run : runs.values()) {
            boolean missionMatch = missionId == null || missionId.isBlank() || missionId.equals(String.valueOf(run.get("missionId")));
            boolean runMatch = runId == null || runId.isBlank() || runId.equals(String.valueOf(run.get("runId")));
            if (missionMatch && runMatch) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> timeline = (List<Map<String, Object>>) run.get("timeline");
                if (timeline != null) result.addAll(timeline);
            }
        }
        return result;
    }

    public Map<String, Object> latestRun(String missionId) {
        Map<String, Object> latest = null;
        for (Map<String, Object> run : runs.values()) {
            if (missionId == null || missionId.isBlank() || missionId.equals(String.valueOf(run.get("missionId")))) {
                latest = run;
            }
        }
        return latest;
    }

    private ExecutionTimelineEvent event(String stage, String actor, String topic, String action, String status,
                                         String message, int latencyMs, int retryCount, String riskLevel) {
        return new ExecutionTimelineEvent(stage, actor, topic, action, status, message, latencyMs, retryCount, riskLevel);
    }

    private Map<String, Object> timelineSummary(List<Map<String, Object>> timeline) {
        int retries = 0;
        int warnings = 0;
        for (Map<String, Object> item : timeline) {
            retries += number(item.get("retryCount"), 0);
            String risk = String.valueOf(item.getOrDefault("riskLevel", "LOW"));
            if (!"LOW".equals(risk)) warnings++;
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("eventCount", timeline.size());
        summary.put("retryCount", retries);
        summary.put("warningCount", warnings);
        summary.put("finalStage", timeline.isEmpty() ? "UNKNOWN" : timeline.get(timeline.size() - 1).get("stage"));
        return summary;
    }

    private String topicName(Map<String, Object> plan, int index) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topics = (List<Map<String, Object>>) plan.get("topics");
        if (topics == null || topics.isEmpty() || index >= topics.size()) return "CollaborationPlanTopic";
        return String.valueOf(topics.get(index).getOrDefault("name", "CollaborationPlanTopic"));
    }

    private String risk(Map<String, Object> plan) {
        @SuppressWarnings("unchecked")
        List<String> risks = (List<String>) plan.get("riskWarnings");
        return risks != null && risks.size() > 2 ? "MEDIUM" : "LOW";
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
