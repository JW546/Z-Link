package simulation;

import java.util.Map;

/**
 * Ego autonomous vehicle state.
 */
public class VirtualVehicleState extends VirtualDeviceState {
    public double routeProgress;
    public String missionStatus;

    public VirtualVehicleState(String deviceId, String label, String roadNodeId, double x, double y) {
        super(deviceId, "EGO_AUTONOMOUS_VEHICLE", label, roadNodeId, x, y);
        this.routeProgress = 0.0;
        this.missionStatus = "WAITING_FOR_INTENT";
        this.speedMps = 9.5;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("routeProgress", round(routeProgress));
        map.put("missionStatus", missionStatus);
        return map;
    }
}
