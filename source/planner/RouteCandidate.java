package planner;

import java.util.LinkedHashMap;
import java.util.Map;

public class RouteCandidate {
    public final String routeId;
    public final double travelTime;
    public final double safetyRisk;
    public final double congestionLevel;
    public final double emergencyLevel;
    public final double messageConfidence;
    public final double sourceTrust;
    public final double messageFreshness;
    public final double conflictPenalty;
    public final double ddsDelayMs;
    public final double packetLossRate;

    public RouteCandidate(String routeId, double travelTime, double safetyRisk, double congestionLevel,
                          double emergencyLevel, double messageConfidence, double sourceTrust,
                          double messageFreshness, double conflictPenalty, double ddsDelayMs,
                          double packetLossRate) {
        this.routeId = routeId;
        this.travelTime = travelTime;
        this.safetyRisk = safetyRisk;
        this.congestionLevel = congestionLevel;
        this.emergencyLevel = emergencyLevel;
        this.messageConfidence = messageConfidence;
        this.sourceTrust = sourceTrust;
        this.messageFreshness = messageFreshness;
        this.conflictPenalty = conflictPenalty;
        this.ddsDelayMs = ddsDelayMs;
        this.packetLossRate = packetLossRate;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("routeId", routeId);
        map.put("travelTime", travelTime);
        map.put("safetyRisk", safetyRisk);
        map.put("congestionLevel", congestionLevel);
        map.put("emergencyLevel", emergencyLevel);
        map.put("messageConfidence", messageConfidence);
        map.put("sourceTrust", sourceTrust);
        map.put("messageFreshness", messageFreshness);
        map.put("conflictPenalty", conflictPenalty);
        map.put("ddsDelayMs", ddsDelayMs);
        map.put("packetLossRate", packetLossRate);
        return map;
    }
}
