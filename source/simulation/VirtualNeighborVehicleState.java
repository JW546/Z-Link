package simulation;

import java.util.Map;

/**
 * Neighbor vehicle state used for local traffic-flow and following-risk messages.
 */
public class VirtualNeighborVehicleState extends VirtualDeviceState {
    public double queueLengthM;
    public double followingRisk;

    public VirtualNeighborVehicleState(String deviceId, String label, String roadNodeId, double x, double y) {
        super(deviceId, "NEIGHBOR_AUTONOMOUS_VEHICLE", label, roadNodeId, x, y);
        this.queueLengthM = 80.0;
        this.followingRisk = 0.28;
        this.speedMps = 7.0;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("queueLengthM", round(queueLengthM));
        map.put("followingRisk", round(followingRisk));
        return map;
    }
}
