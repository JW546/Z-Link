package policy;

import dds.DDSMessage;
import worldmodel.SystemWorldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentState {
    public final String agentId;
    public final double battery;
    public final double linkQuality;
    public final double localRisk;
    public final String passengerIntent;
    public final Map<String, Object> dslConstraints;
    public final String currentRoute;
    public final List<String> candidateRoutes;
    public final List<Map<String, Object>> ddsMessages;
    public final Map<String, Object> droneSuggestion;
    public final Map<String, Object> roadsideSuggestion;
    public final Map<String, Object> neighborVehicleSuggestion;
    public final double congestionLevel;
    public final double safetyRisk;
    public final double sourceTrust;
    public final double messageConfidence;
    public final double messageFreshness;
    public final boolean conflictStatus;
    public final double emergencyLevel;
    public final boolean passengerSafetyFirst;
    public final boolean passengerUrgent;
    public final boolean hasConflict;
    public final boolean replanRequired;

    public AgentState(String agentId, double battery, double linkQuality, double localRisk) {
        this(builder(agentId)
            .battery(battery)
            .linkQuality(linkQuality)
            .safetyRisk(localRisk));
    }

    public AgentState(String agentId, double battery, double linkQuality, double localRisk,
                      double congestionLevel, double messageConfidence, double messageFreshness,
                      double sourceTrust, boolean passengerSafetyFirst, boolean passengerUrgent,
                      boolean hasConflict, boolean replanRequired) {
        this(builder(agentId)
            .battery(battery)
            .linkQuality(linkQuality)
            .safetyRisk(localRisk)
            .congestionLevel(congestionLevel)
            .messageConfidence(messageConfidence)
            .messageFreshness(messageFreshness)
            .sourceTrust(sourceTrust)
            .passengerSafetyFirst(passengerSafetyFirst)
            .passengerUrgent(passengerUrgent)
            .conflictStatus(hasConflict)
            .replanRequired(replanRequired));
    }

    private AgentState(Builder builder) {
        this.agentId = builder.agentId;
        this.battery = builder.battery;
        this.linkQuality = builder.linkQuality;
        this.passengerIntent = builder.passengerIntent;
        this.dslConstraints = new LinkedHashMap<String, Object>(builder.dslConstraints);
        this.currentRoute = builder.currentRoute;
        this.candidateRoutes = new ArrayList<String>(builder.candidateRoutes);
        this.ddsMessages = new ArrayList<Map<String, Object>>(builder.ddsMessages);
        this.droneSuggestion = new LinkedHashMap<String, Object>(builder.droneSuggestion);
        this.roadsideSuggestion = new LinkedHashMap<String, Object>(builder.roadsideSuggestion);
        this.neighborVehicleSuggestion = new LinkedHashMap<String, Object>(builder.neighborVehicleSuggestion);
        this.congestionLevel = clamp(builder.congestionLevel);
        this.safetyRisk = clamp(builder.safetyRisk);
        this.localRisk = this.safetyRisk;
        this.sourceTrust = clamp(builder.sourceTrust);
        this.messageConfidence = clamp(builder.messageConfidence);
        this.messageFreshness = clamp(builder.messageFreshness);
        this.conflictStatus = builder.conflictStatus;
        this.emergencyLevel = clamp(builder.emergencyLevel);
        this.passengerSafetyFirst = builder.passengerSafetyFirst || containsAny(passengerIntent, "安全", "避开", "safe");
        this.passengerUrgent = builder.passengerUrgent || containsAny(passengerIntent, "尽快", "马上", "快", "urgent");
        this.hasConflict = this.conflictStatus;
        this.replanRequired = builder.replanRequired;
    }

    public static Builder builder(String agentId) {
        return new Builder(agentId);
    }

    public static AgentState fromWorldModel(String agentId, SystemWorldModel model, String currentRoute) {
        Builder builder = builder(agentId);
        if (model == null) {
            return builder.currentRoute(currentRoute).build();
        }
        builder.passengerIntent(model.passengerIntent)
            .dslConstraints(model.dslConstraints)
            .currentRoute(currentRoute)
            .candidateRoutes(model.candidateRoutes)
            .congestionLevel(model.congestionLevel)
            .safetyRisk(model.roadRisk)
            .messageConfidence(model.averageConfidence)
            .messageFreshness(model.averageFreshness)
            .sourceTrust(model.trustFor(agentId))
            .conflictStatus(model.hasConflict)
            .replanRequired(model.replanRequired)
            .droneSuggestion(last(model.droneSuggestions))
            .roadsideSuggestion(last(model.roadsideSuggestions))
            .neighborVehicleSuggestion(last(model.neighborSuggestions));
        for (Map<String, Object> item : model.timeline) {
            builder.addDdsMessage(item);
        }
        return builder.build();
    }

    public static AgentState fromMessages(String agentId, String passengerIntent, List<DDSMessage> messages) {
        Builder builder = builder(agentId).passengerIntent(passengerIntent);
        if (messages != null) {
            for (DDSMessage message : messages) {
                builder.addDdsMessage(message.toMap());
                builder.messageConfidence(Math.max(builder.messageConfidence, message.confidence));
                builder.messageFreshness(Math.min(builder.messageFreshness, message.messageFreshness()));
                Object route = message.payload.get("route");
                if (route != null) builder.addCandidateRoute(String.valueOf(route));
                Object risk = message.payload.get("riskScore");
                if (!(risk instanceof Number)) risk = message.payload.get("risk_score");
                if (risk instanceof Number) builder.safetyRisk(Math.max(builder.safetyRisk, ((Number) risk).doubleValue()));
            }
        }
        return builder.build();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("agentId", agentId);
        map.put("battery", battery);
        map.put("linkQuality", linkQuality);
        map.put("localRisk", localRisk);
        map.put("passengerIntent", passengerIntent);
        map.put("dslConstraints", dslConstraints);
        map.put("currentRoute", currentRoute);
        map.put("candidateRoutes", candidateRoutes);
        map.put("ddsMessages", ddsMessages);
        map.put("droneSuggestion", droneSuggestion);
        map.put("roadsideSuggestion", roadsideSuggestion);
        map.put("neighborVehicleSuggestion", neighborVehicleSuggestion);
        map.put("congestionLevel", congestionLevel);
        map.put("safetyRisk", safetyRisk);
        map.put("sourceTrust", sourceTrust);
        map.put("messageConfidence", messageConfidence);
        map.put("messageFreshness", messageFreshness);
        map.put("conflictStatus", conflictStatus);
        map.put("emergencyLevel", emergencyLevel);
        map.put("passengerSafetyFirst", passengerSafetyFirst);
        map.put("passengerUrgent", passengerUrgent);
        map.put("hasConflict", hasConflict);
        map.put("replanRequired", replanRequired);
        return map;
    }

    private static Map<String, Object> last(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return new LinkedHashMap<String, Object>();
        return items.get(items.size() - 1);
    }

    private static boolean containsAny(String text, String... tokens) {
        String value = text == null ? "" : text;
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static class Builder {
        private final String agentId;
        private double battery = 0.82;
        private double linkQuality = 0.84;
        private String passengerIntent = "";
        private Map<String, Object> dslConstraints = new LinkedHashMap<String, Object>();
        private String currentRoute = "";
        private List<String> candidateRoutes = new ArrayList<String>();
        private List<Map<String, Object>> ddsMessages = new ArrayList<Map<String, Object>>();
        private Map<String, Object> droneSuggestion = new LinkedHashMap<String, Object>();
        private Map<String, Object> roadsideSuggestion = new LinkedHashMap<String, Object>();
        private Map<String, Object> neighborVehicleSuggestion = new LinkedHashMap<String, Object>();
        private double congestionLevel = 0.30;
        private double safetyRisk = 0.20;
        private double sourceTrust = 0.70;
        private double messageConfidence = 0.0;
        private double messageFreshness = 1.0;
        private boolean conflictStatus;
        private double emergencyLevel;
        private boolean passengerSafetyFirst;
        private boolean passengerUrgent;
        private boolean replanRequired;

        private Builder(String agentId) {
            this.agentId = agentId == null || agentId.isBlank() ? "AGENT-UNKNOWN" : agentId;
        }

        public Builder battery(double value) { this.battery = value; return this; }
        public Builder linkQuality(double value) { this.linkQuality = value; return this; }
        public Builder passengerIntent(String value) { this.passengerIntent = value == null ? "" : value; return this; }
        public Builder dslConstraints(Map<String, Object> value) { this.dslConstraints = value == null ? new LinkedHashMap<String, Object>() : value; return this; }
        public Builder currentRoute(String value) { this.currentRoute = value == null ? "" : value; return this; }
        public Builder candidateRoutes(List<String> value) { this.candidateRoutes = value == null ? new ArrayList<String>() : value; return this; }
        public Builder addCandidateRoute(String value) { if (value != null && !candidateRoutes.contains(value)) candidateRoutes.add(value); return this; }
        public Builder ddsMessages(List<Map<String, Object>> value) { this.ddsMessages = value == null ? new ArrayList<Map<String, Object>>() : value; return this; }
        public Builder addDdsMessage(Map<String, Object> value) { if (value != null) ddsMessages.add(value); return this; }
        public Builder droneSuggestion(Map<String, Object> value) { this.droneSuggestion = value == null ? new LinkedHashMap<String, Object>() : value; return this; }
        public Builder roadsideSuggestion(Map<String, Object> value) { this.roadsideSuggestion = value == null ? new LinkedHashMap<String, Object>() : value; return this; }
        public Builder neighborVehicleSuggestion(Map<String, Object> value) { this.neighborVehicleSuggestion = value == null ? new LinkedHashMap<String, Object>() : value; return this; }
        public Builder congestionLevel(double value) { this.congestionLevel = value; return this; }
        public Builder safetyRisk(double value) { this.safetyRisk = value; return this; }
        public Builder sourceTrust(double value) { this.sourceTrust = value; return this; }
        public Builder messageConfidence(double value) { this.messageConfidence = value; return this; }
        public Builder messageFreshness(double value) { this.messageFreshness = value; return this; }
        public Builder conflictStatus(boolean value) { this.conflictStatus = value; return this; }
        public Builder emergencyLevel(double value) { this.emergencyLevel = value; return this; }
        public Builder passengerSafetyFirst(boolean value) { this.passengerSafetyFirst = value; return this; }
        public Builder passengerUrgent(boolean value) { this.passengerUrgent = value; return this; }
        public Builder replanRequired(boolean value) { this.replanRequired = value; return this; }
        public AgentState build() { return new AgentState(this); }
    }
}
