package integration;

import simulation.SimulationTickResult;
import simulation.VirtualTrafficSimulationService;

import java.util.List;
import java.util.Map;

/**
 * End-to-end verification for the structured virtual traffic simulation.
 */
public class VirtualTrafficSimulationServiceTest {
    public static void main(String[] args) {
        VirtualTrafficSimulationService service = new VirtualTrafficSimulationService();
        testScenariosCanBeListed(service);
        testSimulationCanStartAndTick(service);
        testStateIsFrontendReady(service);
        service.stop();
        System.out.println("[PASS] Virtual traffic simulation service test passed.");
    }

    private static void testScenariosCanBeListed(VirtualTrafficSimulationService service) {
        List<Map<String, Object>> scenarios = service.listScenarios();
        require(!scenarios.isEmpty(), "simulation scenarios are available");
        require(scenarios.get(0).containsKey("simulationMode"), "scenario exposes simulation mode");
    }

    private static void testSimulationCanStartAndTick(VirtualTrafficSimulationService service) {
        Map<String, Object> started = service.start("hospital-emergency");
        require(Boolean.TRUE.equals(started.get("ok")), "simulation starts");
        require(Boolean.TRUE.equals(started.get("running")), "world is running after start");

        SimulationTickResult tick = service.tick();
        require(tick.tick == 1, "tick advances to one");
        require(tick.ddsMessages.size() >= 6, "virtual devices publish DDS messages");
        require(tick.agentAdvice.size() >= 4, "DDS messages generate Agent advice");
        require(!tick.plannerResult.isEmpty(), "planner result is produced");
        require(tick.plannerResult.containsKey("riskAware"), "risk-aware planner output exists");
        require(!tick.conflictResolution.isEmpty(), "conflict resolver produces output");
        require(tick.conflictResolution.containsKey("finalRoute"), "conflict resolver selects final route");
        require(!String.valueOf(tick.finalDecision.get("route")).isBlank(), "final route is published");
    }

    @SuppressWarnings("unchecked")
    private static void testStateIsFrontendReady(VirtualTrafficSimulationService service) {
        Map<String, Object> state = service.state();
        require(state.containsKey("world"), "state includes world");
        require(state.containsKey("recentDdsMessages"), "state includes DDS timeline");
        Map<String, Object> world = (Map<String, Object>) state.get("world");
        require(((List<?>) world.get("devices")).size() >= 4, "world exposes virtual device states");
        Map<String, Object> roadNetwork = (Map<String, Object>) world.get("roadNetwork");
        require(((List<?>) roadNetwork.get("nodes")).size() >= 4, "road network exposes nodes");
        require(((List<?>) roadNetwork.get("routes")).size() >= 3, "road network exposes routes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
