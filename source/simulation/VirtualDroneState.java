package simulation;

import java.util.Map;

/**
 * Aerial forward perception device state.
 */
public class VirtualDroneState extends VirtualDeviceState {
    public double altitudeM;
    public double observationRangeM;
    public String observedRouteId;

    public VirtualDroneState(String deviceId, String label, String roadNodeId, double x, double y) {
        super(deviceId, "AERIAL_PERCEPTION_UAV", label, roadNodeId, x, y);
        this.altitudeM = 120.0;
        this.observationRangeM = 1800.0;
        this.observedRouteId = "route_A_main_road";
        this.speedMps = 12.0;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("altitudeM", round(altitudeM));
        map.put("observationRangeM", round(observationRangeM));
        map.put("observedRouteId", observedRouteId);
        return map;
    }
}
