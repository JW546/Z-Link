package integration;

import collaboration.runtime.ProtocolAdapterRegistry;
import collaboration.runtime.RuntimeAgentConfig;
import collaboration.runtime.RuntimeAgentRole;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeterogeneousProtocolAdapterTest {
    public static void main(String[] args) {
        testConfiguredDevicesSelectAdapters();
        testDjiStatusNormalizesToRuntimeStatus();
        testRos2VehicleStatusNormalizesToRuntimeStatus();
        testGenericPeerStatusNormalizesToRuntimeStatus();
        System.out.println("[PASS] Heterogeneous protocol adapter test passed.");
    }

    private static void testConfiguredDevicesSelectAdapters() {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        RuntimeAgentConfig uav = registry.runtimeConfig("UAV-CQ-007", 500L, 2);
        RuntimeAgentConfig car = registry.runtimeConfig("CAR-CQ-001", 500L, 2);
        RuntimeAgentConfig peer = registry.runtimeConfig("PEER-CAR-031", 500L, 2);

        require(uav.role == RuntimeAgentRole.UAV, "configured UAV role is loaded");
        require("DJI-STYLE".equals(uav.adapter.protocolName()), "configured UAV uses DJI adapter");
        require("ROS2-DDS".equals(car.adapter.protocolName()), "configured ego car uses ROS2 vehicle adapter");
        require("GENERIC-PRIVATE-JSON".equals(peer.adapter.protocolName()), "configured peer car uses generic adapter");
        require(registry.configuredDevices().size() >= 3, "registry exposes configured heterogeneous devices");
    }

    private static void testDjiStatusNormalizesToRuntimeStatus() {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        Map<String, Object> nativeStatus = new LinkedHashMap<String, Object>();
        nativeStatus.put("uavId", "UAV-REAL-001");
        nativeStatus.put("flightMode", "MISSION");
        nativeStatus.put("batteryPercent", 77.0);
        nativeStatus.put("position", point(3.0, 4.0, 80.0));
        nativeStatus.put("activeRoute", "route_C_aerial_scout");
        nativeStatus.put("sampleRateHz", 2.0);

        Map<String, Object> unified = registry.normalizeStatus("DJI-STYLE", nativeStatus);
        require("UAV-REAL-001".equals(unified.get("agentId")), "DJI status preserves UAV id");
        require("UAV".equals(unified.get("role")), "DJI status normalizes role");
        require("route_C_aerial_scout".equals(unified.get("route")), "DJI route normalizes");
        require(number(unified.get("z")) == 80.0, "DJI altitude normalizes");
    }

    private static void testRos2VehicleStatusNormalizesToRuntimeStatus() {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        Map<String, Object> nativeStatus = new LinkedHashMap<String, Object>();
        nativeStatus.put("node", "CAR-REAL-002");
        nativeStatus.put("role", "EGO_CAR");
        nativeStatus.put("stamp", 12345L);
        nativeStatus.put("pose", point(8.0, 9.0, 0.0));
        nativeStatus.put("speed_mps", 11.0);
        nativeStatus.put("battery_percent", 65.0);
        nativeStatus.put("route_id", "route_D_emergency_green_wave");

        Map<String, Object> unified = registry.normalizeStatus("ROS2-DDS", nativeStatus);
        require("CAR-REAL-002".equals(unified.get("agentId")), "ROS2 vehicle status preserves node id");
        require("EGO_CAR".equals(unified.get("role")), "ROS2 vehicle role normalizes");
        require("route_D_emergency_green_wave".equals(unified.get("route")), "ROS2 vehicle route normalizes");
        require(number(unified.get("speed")) == 11.0, "ROS2 vehicle speed normalizes");
    }

    private static void testGenericPeerStatusNormalizesToRuntimeStatus() {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        Map<String, Object> vendorPayload = new LinkedHashMap<String, Object>();
        vendorPayload.put("agentId", "PEER-REAL-003");
        vendorPayload.put("role", "PEER_CAR");
        vendorPayload.put("route", "route_A_main_road");
        vendorPayload.put("x", 1.0);
        vendorPayload.put("battery", 91.0);
        Map<String, Object> nativeStatus = new LinkedHashMap<String, Object>();
        nativeStatus.put("message", "peer_status_v1");
        nativeStatus.put("device_id", "PEER-REAL-003");
        nativeStatus.put("vendor_payload", vendorPayload);

        Map<String, Object> unified = registry.normalizeStatus("GENERIC-PRIVATE-JSON", nativeStatus);
        require("PEER-REAL-003".equals(unified.get("agentId")), "generic peer status preserves agent id");
        require("PEER_CAR".equals(unified.get("role")), "generic peer role normalizes");
        require("route_A_main_road".equals(unified.get("route")), "generic peer route normalizes");
        require(number(unified.get("battery")) == 91.0, "generic peer battery normalizes");
    }

    private static Map<String, Object> point(double x, double y, double z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("x", x);
        point.put("y", y);
        point.put("z", z);
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
