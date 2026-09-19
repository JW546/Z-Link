package collaboration.runtime;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class FullClosedLoopScenario {
    public RuntimeScenarioResult run(long timeoutMs) throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        CoordinatorAgent coordinator = new CoordinatorAgent("127.0.0.1", broker.port(), 4, timeoutMs);
        Thread coordinatorThread = thread("closed-loop-coordinator", coordinator);

        try {
            coordinatorThread.start();
            Thread.sleep(180L);
            try (ExternalDeviceStatusIngress ingress = new ExternalDeviceStatusIngress("127.0.0.1", broker.port(), registry)) {
                ingress.publishNativeStatus("UAV-CQ-007", djiStatus());
                ingress.publishNativeStatus("CAR-CQ-001", ros2EgoStatus());
                ingress.publishNativeStatus("PEER-CAR-031", genericPeerStatus());
                ingress.publishNativeStatus("UAV-AEP84-001", aep84Status());
            }
            coordinatorThread.join(Math.max(2_000L, timeoutMs + 500L));
            Thread.sleep(120L);
            return new RuntimeScenarioResult(broker.history(), coordinator.conflictEvent(), coordinator.finalDecision());
        } finally {
            coordinator.close();
            broker.close();
        }
    }

    private Thread thread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    private Map<String, Object> djiStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("uavId", "UAV-REAL-001");
        status.put("flightMode", "MISSION");
        status.put("batteryPercent", 77.0);
        status.put("position", point(13.0, 9.0, 80.0));
        status.put("activeRoute", "route_C_aerial_scout");
        status.put("speedMps", 16.0);
        status.put("sampleRateHz", 2.0);
        return status;
    }

    private Map<String, Object> ros2EgoStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("node", "CAR-REAL-002");
        status.put("role", "EGO_CAR");
        status.put("stamp", System.currentTimeMillis());
        status.put("pose", point(10.0, 6.0, 0.0));
        status.put("speed_mps", 8.5);
        status.put("battery_percent", 84.0);
        status.put("route_id", "route_A_main_road");
        status.put("rate_hz", 2.0);
        return status;
    }

    private Map<String, Object> genericPeerStatus() {
        Map<String, Object> vendor = new LinkedHashMap<String, Object>();
        vendor.put("agentId", "PEER-REAL-003");
        vendor.put("role", "PEER_CAR");
        vendor.put("timestamp", System.currentTimeMillis());
        vendor.put("protocol", "GENERIC-PRIVATE-JSON");
        vendor.put("route", "route_A_main_road");
        vendor.put("x", 11.0);
        vendor.put("y", 7.0);
        vendor.put("z", 0.0);
        vendor.put("speed", 7.0);
        vendor.put("battery", 91.0);
        vendor.put("rateHz", 2.0);
        vendor.put("missionStatus", "EXECUTING");

        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("message", "peer_status_v1");
        status.put("device_id", "PEER-REAL-003");
        status.put("vendor_payload", vendor);
        return status;
    }

    private Map<String, Object> aep84Status() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("test-fixtures/aep84_message_20_vehicle_id.json")));
            return new Gson().fromJson(json, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("AEP84 fixture is required for local closed-loop validation.", e);
        }
    }

    private Map<String, Object> point(double x, double y, double z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("x", x);
        point.put("y", y);
        point.put("z", z);
        return point;
    }

}
