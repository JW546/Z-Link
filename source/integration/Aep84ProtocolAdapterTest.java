package integration;

import collaboration.runtime.Aep84DroneProtocolAdapter;
import collaboration.runtime.ProtocolAdapterRegistry;
import collaboration.runtime.RuntimeAgentRole;
import collaboration.runtime.DeviceProfile;

import java.util.LinkedHashMap;
import java.util.Map;

public class Aep84ProtocolAdapterTest {
    public static void main(String[] args) {
        testAep84NativeStatusNormalizesToRuntimeStatus();
        testAep84UnifiedStatusRoundTripsToNativeFrame();
        testProtocolAdapterRegistryRecognizesAep84Profile();
        System.out.println("[PASS] AEP84/STANAG4586 protocol adapter test passed.");
    }

    private static void testAep84NativeStatusNormalizesToRuntimeStatus() {
        Aep84DroneProtocolAdapter adapter = new Aep84DroneProtocolAdapter();
        Map<String, Object> nativeStatus = new LinkedHashMap<String, Object>();
        nativeStatus.put("messageType", "20");
        nativeStatus.put("messageInstanceId", 101);
        nativeStatus.put("streamId", 3);
        nativeStatus.put("ackRequired", true);
        nativeStatus.put("validTimeMs", 1000);
        nativeStatus.put("latencyMs", 80);
        nativeStatus.put("checksum", "A1B2C3");
        nativeStatus.put("vehicleId", "UAV-AEP84-001");
        nativeStatus.put("cucsId", "CUCS-CQ-001");
        nativeStatus.put("vsmId", "VSM-UAV-001");
        nativeStatus.put("vehicleType", "UAV");
        nativeStatus.put("position", point(29.592, 106.568, 120.0));
        nativeStatus.put("speedMps", 12.0);
        nativeStatus.put("batteryPercent", 78.0);
        nativeStatus.put("publishRateHz", 2.0);
        nativeStatus.put("missionStatus", "EXECUTING");
        nativeStatus.put("activeRoute", "route_B_emergency_bypass");

        Map<String, Object> unified = adapter.fromNativeStatus(nativeStatus);

        require("UAV-AEP84-001".equals(unified.get("agentId")), "AEP84 status preserves vehicle id");
        require("UAV".equals(unified.get("role")), "AEP84 status normalizes role");
        require("AEP84-STANAG4586".equals(unified.get("protocol")), "AEP84 status uses STANAG 4586 protocol name");
        require("route_B_emergency_bypass".equals(unified.get("route")), "AEP84 route normalizes");
        require(number(unified.get("x")) == 29.592, "AEP84 latitude maps to unified x");
        require(number(unified.get("y")) == 106.568, "AEP84 longitude maps to unified y");
        require(number(unified.get("z")) == 120.0, "AEP84 altitude maps to unified z");
        require(number(unified.get("speed")) == 12.0, "AEP84 speed normalizes");
        require(number(unified.get("battery")) == 78.0, "AEP84 battery normalizes");
        require(number(unified.get("rateHz")) == 2.0, "AEP84 publish rate normalizes");
        require("20".equals(unified.get("messageType")), "AEP84 message type is retained");
        require("A1B2C3".equals(unified.get("checksum")), "AEP84 checksum is retained");
        require(Boolean.TRUE.equals(unified.get("ackRequired")), "AEP84 ack flag is retained");
        require(number(unified.get("validTimeMs")) == 1000, "AEP84 validity window is retained");
        require(number(unified.get("latencyMs")) == 80, "AEP84 latency is retained");
    }

    private static void testAep84UnifiedStatusRoundTripsToNativeFrame() {
        Aep84DroneProtocolAdapter adapter = new Aep84DroneProtocolAdapter();
        Map<String, Object> unified = new LinkedHashMap<String, Object>();
        unified.put("agentId", "UAV-AEP84-002");
        unified.put("role", "UAV");
        unified.put("protocol", "AEP84-STANAG4586");
        unified.put("route", "route_C_aerial_scout");
        unified.put("x", 29.61);
        unified.put("y", 106.57);
        unified.put("z", 140.0);
        unified.put("speed", 9.5);
        unified.put("battery", 66.0);
        unified.put("rateHz", 5.0);
        unified.put("missionStatus", "RETURNING");
        unified.put("messageType", "21");
        unified.put("messageInstanceId", 202);
        unified.put("streamId", 5);
        unified.put("ackRequired", false);
        unified.put("validTimeMs", 500);
        unified.put("latencyMs", 40);

        Map<String, Object> nativeStatus = adapter.toNativeStatus(unified);

        require("AEP84-STANAG4586".equals(nativeStatus.get("protocol")), "AEP84 native frame carries protocol");
        require("21".equals(nativeStatus.get("messageType")), "AEP84 native frame carries message type");
        require("UAV-AEP84-002".equals(nativeStatus.get("vehicleId")), "AEP84 native frame preserves vehicle id");
        require(number(nativeStatus.get("publishRateHz")) == 5.0, "AEP84 native frame carries publish rate");
        require("route_C_aerial_scout".equals(nativeStatus.get("activeRoute")), "AEP84 native frame carries active route");
        require(nativeStatus.get("checksum") instanceof String, "AEP84 native frame includes checksum placeholder");
    }

    private static void testProtocolAdapterRegistryRecognizesAep84Profile() {
        ProtocolAdapterRegistry registry = new ProtocolAdapterRegistry();
        registry.registerProfile(new DeviceProfile("UAV-AEP84-001", RuntimeAgentRole.UAV, "AEP84-STANAG4586", "route_B_emergency_bypass", "NATO STANAG 4586 / AEP-84"));
        require("AEP84-STANAG4586".equals(registry.adapterForAgent("UAV-AEP84-001").protocolName()), "registry resolves AEP84 adapter");
    }

    private static Map<String, Object> point(double x, double y, double z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("lat", x);
        point.put("lon", y);
        point.put("alt_m", z);
        return point;
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
