package simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Roadside unit state for events, signal phase, and local risk publication.
 */
public class VirtualRsuState extends VirtualDeviceState {
    public String signalPhase;
    public final List<String> activeEvents = new ArrayList<String>();

    public VirtualRsuState(String deviceId, String label, String roadNodeId, double x, double y) {
        super(deviceId, "ROADSIDE_UNIT", label, roadNodeId, x, y);
        this.signalPhase = "GREEN";
        this.speedMps = 0.0;
        this.batteryLevel = 1.0;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("signalPhase", signalPhase);
        map.put("activeEvents", new ArrayList<String>(activeEvents));
        return map;
    }
}
