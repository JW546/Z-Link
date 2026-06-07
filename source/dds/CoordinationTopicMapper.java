package dds;

import chongqing.TrafficFlow;
import urban.EnvironmentPerception;
import urban.EmergencyAlert;
import urban.VehicleData;

import java.util.HashMap;
import java.util.Map;

/**
 * 新协同主题与IDL实体的映射器。
 */
public class CoordinationTopicMapper {

    public static final String TOPIC_FORWARD_TRAFFIC = "Traffic/Intersection/Forward";
    public static final String TOPIC_FORWARD_CORRIDOR = "UAV/Perception/ForwardCorridor";
    public static final String TOPIC_GLOBAL_PERCEPTION = "CompliantBackbone/GlobalPerception";
    public static final String TOPIC_REPLAN_TRIGGER = "Route/Replan/Trigger";

    public TrafficFlow toTrafficFlow(Map<String, Object> payload) {
        TrafficFlow flow = new TrafficFlow();
        flow.zone_id = String.valueOf(payload.getOrDefault("intersectionId", "INT-UNKNOWN"));
        flow.vehicle_count = ((Number) payload.getOrDefault("queueLength", 0)).intValue();
        flow.average_speed = ((Number) payload.getOrDefault("averageSpeed", 18.0)).doubleValue();
        flow.congestion_level = ((Number) payload.getOrDefault("congestionLevel", 0.3)).doubleValue();
        flow.latitude = ((Number) payload.getOrDefault("latitude", 29.56)).doubleValue();
        flow.longitude = ((Number) payload.getOrDefault("longitude", 106.55)).doubleValue();
        flow.radius = ((Number) payload.getOrDefault("radius", 300.0)).doubleValue();
        flow.timestamp = (int) (System.currentTimeMillis() / 1000);
        return flow;
    }

    public EnvironmentPerception toEnvironmentPerception(Map<String, Object> payload) {
        EnvironmentPerception perception = new EnvironmentPerception();
        perception.drone_id = String.valueOf(payload.getOrDefault("sourceId", "UAV-UNKNOWN"));
        perception.temperature = ((Number) payload.getOrDefault("temperature", 24.0)).doubleValue();
        perception.wind_speed = ((Number) payload.getOrDefault("windSpeed", 7.5)).doubleValue();
        perception.visibility = ((Number) payload.getOrDefault("visibility", 1800.0)).doubleValue();
        perception.turbulence_level = ((Number) payload.getOrDefault("riskScore", 0.25)).doubleValue();
        perception.illumination = ((Number) payload.getOrDefault("illumination", 70)).intValue();
        perception.timestamp = (int) (System.currentTimeMillis() / 1000);
        return perception;
    }

    public EmergencyAlert toReplanTriggerAlert(Map<String, Object> payload) {
        EmergencyAlert alert = new EmergencyAlert();
        alert.alert_id = ((Number) payload.getOrDefault("alertId", System.currentTimeMillis() % Integer.MAX_VALUE)).intValue();
        alert.level = urban.AlertLevel.HIGH;
        alert.alert_type = String.valueOf(payload.getOrDefault("triggerReason", "replan_required"));
        alert.latitude = ((Number) payload.getOrDefault("latitude", 29.56)).doubleValue();
        alert.longitude = ((Number) payload.getOrDefault("longitude", 106.55)).doubleValue();
        alert.affected_radius = ((Number) payload.getOrDefault("radius", 450.0)).doubleValue();
        alert.recommendation = String.valueOf(payload.getOrDefault("recommendation", "切换前方无拥堵备选路线"));
        alert.timestamp = (int) (System.currentTimeMillis() / 1000);
        return alert;
    }

    public VehicleData toGlobalPerceptionVehicleData(Map<String, Object> payload) {
        VehicleData vehicle = new VehicleData();
        vehicle.vehicle_id = String.valueOf(payload.getOrDefault("sourceId", "CN-BACKBONE-GLOBAL"));
        vehicle.latitude = ((Number) payload.getOrDefault("latitude", 29.56)).doubleValue();
        vehicle.longitude = ((Number) payload.getOrDefault("longitude", 106.55)).doubleValue();
        vehicle.altitude = ((Number) payload.getOrDefault("altitude", 500.0)).doubleValue();
        vehicle.speed = ((Number) payload.getOrDefault("globalFlowSpeed", 20.0)).doubleValue();
        vehicle.heading = ((Number) payload.getOrDefault("heading", 0.0)).doubleValue();
        vehicle.acceleration = 0.0;
        vehicle.battery_level = ((Number) payload.getOrDefault("healthScore", 100)).intValue();
        vehicle.drive_mode = "GLOBAL_PERCEPTION";
        vehicle.timestamp = (int) (System.currentTimeMillis() / 1000);
        return vehicle;
    }

    public Map<String, Object> fromTrafficFlow(TrafficFlow flow) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("intersectionId", flow.zone_id);
        payload.put("queueLength", flow.vehicle_count);
        payload.put("averageSpeed", flow.average_speed);
        payload.put("congestionLevel", flow.congestion_level);
        payload.put("latitude", flow.latitude);
        payload.put("longitude", flow.longitude);
        payload.put("radius", flow.radius);
        payload.put("timestamp", flow.timestamp);
        return payload;
    }

    public Map<String, Object> fromEnvironmentPerception(EnvironmentPerception perception) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", perception.drone_id);
        payload.put("temperature", perception.temperature);
        payload.put("windSpeed", perception.wind_speed);
        payload.put("visibility", perception.visibility);
        payload.put("riskScore", perception.turbulence_level);
        payload.put("illumination", perception.illumination);
        payload.put("timestamp", perception.timestamp);
        return payload;
    }

    public Map<String, Object> fromEmergencyAlert(EmergencyAlert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", alert.alert_id);
        payload.put("triggerReason", alert.alert_type);
        payload.put("recommendation", alert.recommendation);
        payload.put("latitude", alert.latitude);
        payload.put("longitude", alert.longitude);
        payload.put("radius", alert.affected_radius);
        payload.put("timestamp", alert.timestamp);
        return payload;
    }

    public Map<String, Object> fromVehicleData(VehicleData data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceId", data.vehicle_id);
        payload.put("latitude", data.latitude);
        payload.put("longitude", data.longitude);
        payload.put("altitude", data.altitude);
        payload.put("globalFlowSpeed", data.speed);
        payload.put("heading", data.heading);
        payload.put("healthScore", data.battery_level);
        payload.put("timestamp", data.timestamp);
        return payload;
    }
}
