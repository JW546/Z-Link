package agent;

import dds.DDSMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageAssessment {
    public final DDSMessage message;
    public final double sourceTrust;
    public final double confidence;
    public final double freshness;
    public final double riskLevel;
    public final double intentAlignment;
    public final double conflictPenalty;
    public final double valueScore;

    public MessageAssessment(DDSMessage message, double sourceTrust, double confidence, double freshness,
                             double riskLevel, double intentAlignment, double conflictPenalty) {
        this.message = message;
        this.sourceTrust = sourceTrust;
        this.confidence = confidence;
        this.freshness = freshness;
        this.riskLevel = riskLevel;
        this.intentAlignment = intentAlignment;
        this.conflictPenalty = conflictPenalty;
        this.valueScore = clamp(sourceTrust * 0.25 + confidence * 0.25 + freshness * 0.20
            + riskLevel * 0.18 + intentAlignment * 0.17 - conflictPenalty * 0.15);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("sourceTrust", sourceTrust);
        map.put("confidence", confidence);
        map.put("freshness", freshness);
        map.put("riskLevel", riskLevel);
        map.put("intentAlignment", intentAlignment);
        map.put("conflictPenalty", conflictPenalty);
        map.put("valueScore", valueScore);
        return map;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
