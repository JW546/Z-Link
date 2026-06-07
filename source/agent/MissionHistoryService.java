package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class MissionHistoryService {
    private static final MissionHistoryService INSTANCE = new MissionHistoryService();
    private final CopyOnWriteArrayList<Map<String, Object>> records = new CopyOnWriteArrayList<Map<String, Object>>();
    private final int maxRecords = 50;

    public static MissionHistoryService getInstance() {
        return INSTANCE;
    }

    public void recordPlan(Map<String, Object> plan) {
        if (plan == null) return;
        Map<String, Object> record = baseRecord(plan);
        record.put("status", "PLANNED");
        record.put("startTime", System.currentTimeMillis());
        record.put("endTime", 0L);
        put(record);
    }

    public void recordExecution(Map<String, Object> plan, Map<String, Object> execution) {
        if (plan == null || execution == null) return;
        Map<String, Object> record = baseRecord(plan);
        record.put("runId", execution.get("runId"));
        record.put("status", execution.getOrDefault("status", "SIMULATED_EXECUTION_COMPLETED"));
        record.put("timelineSummary", execution.get("timelineSummary"));
        record.put("startTime", System.currentTimeMillis());
        record.put("endTime", System.currentTimeMillis());
        put(record);
    }

    public List<Map<String, Object>> list(String missionId) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> record : records) {
            if (missionId == null || missionId.isBlank() || missionId.equals(String.valueOf(record.get("missionId")))) {
                result.add(new LinkedHashMap<String, Object>(record));
            }
        }
        return result;
    }

    public void clear() {
        records.clear();
    }

    public Map<String, Object> stats() {
        int running = 0, completed = 0, failed = 0;
        int totalRisk = 0, countRisk = 0;
        for (Map<String, Object> record : records) {
            String status = String.valueOf(record.get("status"));
            if (status.contains("RUN") || status.contains("PLAN")) running++;
            if (status.contains("COMPLETED") || status.contains("success")) completed++;
            if (status.contains("FAIL")) failed++;
            Object risk = record.get("riskScore");
            if (risk instanceof Number) {
                totalRisk += ((Number) risk).intValue();
                countRisk++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("totalMissions", records.size());
        stats.put("runningMissions", running);
        stats.put("completedMissions", completed);
        stats.put("failedMissions", failed);
        stats.put("averageRiskScore", countRisk == 0 ? 0 : Math.round(totalRisk * 10.0 / countRisk) / 10.0);
        return stats;
    }

    private void put(Map<String, Object> record) {
        records.add(0, record);
        while (records.size() > maxRecords) records.remove(records.size() - 1);
    }

    private Map<String, Object> baseRecord(Map<String, Object> plan) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("missionId", plan.get("missionId"));
        record.put("originalText", plan.get("input"));
        @SuppressWarnings("unchecked")
        Map<String, Object> intent = (Map<String, Object>) plan.get("intent");
        record.put("taskType", intent == null ? "" : intent.get("taskType"));
        record.put("priority", intent == null ? "" : intent.get("urgency"));
        @SuppressWarnings("unchecked")
        Map<String, Object> routePlan = (Map<String, Object>) plan.get("routePlan");
        record.put("routePath", routePlan == null ? new ArrayList<String>() : routePlan.get("path"));
        record.put("assignedDevices", plan.get("deviceAssignments"));
        @SuppressWarnings("unchecked")
        Map<String, Object> risk = (Map<String, Object>) plan.get("riskAssessment");
        record.put("riskLevel", risk == null ? "LOW" : risk.get("riskLevel"));
        record.put("riskScore", risk == null ? 0 : risk.get("riskScore"));
        record.put("timelineSummary", new LinkedHashMap<String, Object>());
        record.put("structuredDsl", plan.get("structuredDsl"));
        record.put("communicationLogs", plan.get("communicationLogs"));
        return record;
    }
}
