package simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Running virtual traffic scene used when real unmanned devices are unavailable.
 */
public class VirtualTrafficWorld {
    public final String scenarioId;
    public final String scenarioName;
    public final String naturalLanguageTask;
    public final VirtualRoadNetwork roadNetwork;
    public final VirtualVehicleState vehicle;
    public final VirtualDroneState drone;
    public final VirtualRsuState rsu;
    public final VirtualNeighborVehicleState neighborVehicle;
    public int tick;
    public boolean running;
    public String finalRouteId = "";
    public String finalExplanation = "";

    public VirtualTrafficWorld(String scenarioId, String scenarioName, String naturalLanguageTask,
                               VirtualRoadNetwork roadNetwork, VirtualVehicleState vehicle,
                               VirtualDroneState drone, VirtualRsuState rsu,
                               VirtualNeighborVehicleState neighborVehicle) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.naturalLanguageTask = naturalLanguageTask;
        this.roadNetwork = roadNetwork;
        this.vehicle = vehicle;
        this.drone = drone;
        this.rsu = rsu;
        this.neighborVehicle = neighborVehicle;
        this.running = true;
    }

    public void advanceKinematics() {
        tick += 1;
        roadNetwork.advance(tick);
        vehicle.routeProgress = Math.min(1.0, vehicle.routeProgress + 0.12);
        vehicle.missionStatus = vehicle.routeProgress >= 1.0 ? "ARRIVED" : "EXECUTING_COLLABORATIVE_ROUTE";
        moveDevice(vehicle, vehicle.currentRouteId, vehicle.routeProgress);
        moveDevice(drone, drone.observedRouteId, Math.min(1.0, vehicle.routeProgress + 0.28));
        moveDevice(neighborVehicle, "route_A_main_road", Math.min(1.0, vehicle.routeProgress + 0.10));
        updateRsu();
    }

    public List<VirtualDeviceState> devices() {
        List<VirtualDeviceState> devices = new ArrayList<VirtualDeviceState>();
        devices.add(vehicle);
        devices.add(drone);
        devices.add(rsu);
        devices.add(neighborVehicle);
        return devices;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("scenarioId", scenarioId);
        map.put("scenarioName", scenarioName);
        map.put("naturalLanguageTask", naturalLanguageTask);
        map.put("tick", tick);
        map.put("running", running);
        map.put("roadNetwork", roadNetwork.toMap());
        map.put("devices", devicesToMap());
        map.put("vehicle", vehicle.toMap());
        map.put("drone", drone.toMap());
        map.put("rsu", rsu.toMap());
        map.put("neighborVehicle", neighborVehicle.toMap());
        map.put("finalRouteId", finalRouteId);
        map.put("finalExplanation", finalExplanation);
        return map;
    }

    private List<Map<String, Object>> devicesToMap() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (VirtualDeviceState device : devices()) {
            result.add(device.toMap());
        }
        return result;
    }

    private void moveDevice(VirtualDeviceState device, String routeId, double progress) {
        VirtualRoadNetwork.VirtualRoute route = roadNetwork.route(routeId);
        if (route == null || route.nodeIds.isEmpty()) return;
        int index = Math.min(route.nodeIds.size() - 1,
            Math.max(0, (int) Math.floor(progress * (route.nodeIds.size() - 1))));
        VirtualRoadNetwork.RoadNode node = roadNetwork.node(route.nodeIds.get(index));
        if (node == null) return;
        device.roadNodeId = node.nodeId;
        device.x = node.x;
        device.y = node.y;
        device.currentRouteId = routeId;
        device.status = running ? "ONLINE_TICK_" + tick : "STOPPED";
    }

    private void updateRsu() {
        VirtualRoadNetwork.RoadEdge risky = null;
        for (VirtualRoadNetwork.RoadEdge edge : roadNetwork.edges) {
            if (risky == null || edge.riskScore > risky.riskScore) risky = edge;
        }
        rsu.signalPhase = tick % 3 == 0 ? "YELLOW" : "GREEN";
        rsu.activeEvents.clear();
        if (risky != null && risky.riskScore > 0.55) {
            rsu.activeEvents.add(risky.event == null || risky.event.isBlank() ? "ELEVATED_RISK" : risky.event);
        }
    }
}
