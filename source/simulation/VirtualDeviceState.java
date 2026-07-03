package simulation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base state for a virtual device in the structured 2D traffic simulation.
 */
public class VirtualDeviceState {
    public final String deviceId;
    public final String deviceType;
    public String label;
    public String roadNodeId;
    public double x;
    public double y;
    public double speedMps;
    public double batteryLevel;
    public String status;
    public String currentRouteId;

    public VirtualDeviceState(String deviceId, String deviceType, String label,
                              String roadNodeId, double x, double y) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.label = label;
        this.roadNodeId = roadNodeId;
        this.x = x;
        this.y = y;
        this.speedMps = 8.0;
        this.batteryLevel = 0.88;
        this.status = "READY";
        this.currentRouteId = "route_A_main_road";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("deviceId", deviceId);
        map.put("deviceType", deviceType);
        map.put("label", label);
        map.put("roadNodeId", roadNodeId);
        map.put("x", round(x));
        map.put("y", round(y));
        map.put("speedMps", round(speedMps));
        map.put("batteryLevel", round(batteryLevel));
        map.put("status", status);
        map.put("currentRouteId", currentRouteId);
        return map;
    }

    protected double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
