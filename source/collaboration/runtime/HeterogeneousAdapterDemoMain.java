package collaboration.runtime;

import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeterogeneousAdapterDemoMain {
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        ProtocolAdapterRegistry registry = ProtocolAdapterRegistry.loadDefault();
        System.out.println("[HETEROGENEOUS] configuredDevices=" + GSON.toJson(registry.configuredDevices()));
        printNormalized(registry, "DJI-STYLE", djiStatus());
        printNormalized(registry, "ROS2-DDS", ros2VehicleStatus());
        printNormalized(registry, "GENERIC-PRIVATE-JSON", genericPeerStatus());
    }

    private static void printNormalized(ProtocolAdapterRegistry registry, String protocol, Map<String, Object> nativeStatus) {
        Map<String, Object> unified = registry.normalizeStatus(protocol, nativeStatus);
        System.out.println("[HETEROGENEOUS] protocol=" + protocol);
        System.out.println("[HETEROGENEOUS] native=" + GSON.toJson(nativeStatus));
        System.out.println("[HETEROGENEOUS] unified=" + GSON.toJson(unified));
    }

    private static Map<String, Object> djiStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("uavId", "UAV-REAL-001");
        status.put("flightMode", "MISSION");
        status.put("batteryPercent", 77.0);
        status.put("position", point(3.0, 4.0, 80.0));
        status.put("activeRoute", "route_C_aerial_scout");
        status.put("sampleRateHz", 2.0);
        return status;
    }

    private static Map<String, Object> ros2VehicleStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("node", "CAR-REAL-002");
        status.put("role", "EGO_CAR");
        status.put("stamp", System.currentTimeMillis());
        status.put("pose", point(8.0, 9.0, 0.0));
        status.put("speed_mps", 11.0);
        status.put("battery_percent", 65.0);
        status.put("route_id", "route_D_emergency_green_wave");
        return status;
    }

    private static Map<String, Object> genericPeerStatus() {
        Map<String, Object> vendor = new LinkedHashMap<String, Object>();
        vendor.put("agentId", "PEER-REAL-003");
        vendor.put("role", "PEER_CAR");
        vendor.put("route", "route_A_main_road");
        vendor.put("x", 1.0);
        vendor.put("y", 2.0);
        vendor.put("battery", 91.0);
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("message", "peer_status_v1");
        status.put("device_id", "PEER-REAL-003");
        status.put("vendor_payload", vendor);
        return status;
    }

    private static Map<String, Object> point(double x, double y, double z) {
        Map<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("x", x);
        point.put("y", y);
        point.put("z", z);
        return point;
    }
}
