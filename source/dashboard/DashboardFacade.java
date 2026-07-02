package dashboard;

import agent.DashboardService;
import agent.DdsCommunicationService;
import agent.DeviceRegistry;
import agent.FaultInjectionService;
import agent.MissionHistoryService;

import java.util.List;
import java.util.Map;

/**
 * Backend dashboard data adapter for frontend visualization.
 */
public class DashboardFacade {
    private final DashboardService dashboardService = new DashboardService();

    public Map<String, Object> build(List<Map<String, Object>> topics) {
        return dashboardService.build(
            DeviceRegistry.getInstance(),
            MissionHistoryService.getInstance(),
            DdsCommunicationService.getInstance(),
            FaultInjectionService.getInstance(),
            topics);
    }
}
