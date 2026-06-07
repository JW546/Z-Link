package agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    public Map<String, Object> build(DeviceRegistry registry, MissionHistoryService history,
                                     DdsCommunicationService dds, FaultInjectionService faults,
                                     List<Map<String, Object>> topics) {
        Map<String, Object> dashboard = new LinkedHashMap<String, Object>();
        dashboard.putAll(history.stats());
        @SuppressWarnings("unchecked")
        Map<String, Object> deviceSummary = registry.summary();
        dashboard.put("onlineDevices", deviceSummary.get("available"));
        dashboard.put("offlineDevices", deviceSummary.get("offline"));
        dashboard.put("ddsRuntimeMode", dds.getRuntimeMode());
        dashboard.put("recentFaultCount", faults.count());
        int retries = 0;
        for (Map<String, Object> log : dds.getCommunicationLogs(null, null, 100)) {
            Object retry = log.get("retryCount");
            if (retry instanceof Number) retries += ((Number) retry).intValue();
        }
        dashboard.put("recentRetryCount", retries);
        dashboard.put("topicCount", topics == null ? 0 : topics.size());
        dashboard.put("lastUpdated", System.currentTimeMillis());
        dashboard.put("recentMissions", history.list("").subList(0, Math.min(5, history.list("").size())));
        return dashboard;
    }
}
