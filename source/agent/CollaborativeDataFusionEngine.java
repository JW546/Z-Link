package agent;

import java.util.*;

/**
 * 多源协同数据融合引擎。
 */
public class CollaborativeDataFusionEngine {

    private static final double SOURCE_WEIGHT = 0.45;
    private static final double FRESHNESS_WEIGHT = 0.35;
    private static final double CONSISTENCY_WEIGHT = 0.20;

    public FusionResult fuse(List<CollaborativeDataPacket> packets, String targetScope) {
        if (packets == null || packets.isEmpty()) {
            return FusionResult.empty();
        }

        double totalScore = 0.0;
        int validCount = 0;
        double congestionWeighted = 0.0;
        double confidenceWeighted = 0.0;
        long now = System.currentTimeMillis();
        Set<String> sources = new HashSet<>();

        for (CollaborativeDataPacket packet : packets) {
            if (packet == null) {
                continue;
            }
            if (packet.scope != null && targetScope != null && !packet.scope.equals(targetScope)) {
                continue;
            }

            double sourceScore = scoreSource(packet.sourceType);
            double freshnessScore = scoreFreshness(now - packet.timestamp);
            double consistencyScore = scoreConsistency(packet.scope, targetScope);
            double packetScore = SOURCE_WEIGHT * sourceScore
                + FRESHNESS_WEIGHT * freshnessScore
                + CONSISTENCY_WEIGHT * consistencyScore;

            totalScore += packetScore;
            validCount++;
            sources.add(packet.sourceType + ":" + packet.sourceId);

            if (packet.payload instanceof Map) {
                Map<?, ?> payload = (Map<?, ?>) packet.payload;
                if (payload.containsKey("congestionLevel")) {
                    double c = ((Number) payload.get("congestionLevel")).doubleValue();
                    congestionWeighted += c * packetScore;
                    confidenceWeighted += packetScore;
                }
            }
        }

        if (validCount == 0) {
            return FusionResult.empty();
        }

        double averageScore = totalScore / validCount;
        double fusedCongestion = confidenceWeighted > 0.0 ? (congestionWeighted / confidenceWeighted) : 0.3;

        FusionResult result = new FusionResult();
        result.fusionScore = averageScore;
        result.fusedCongestion = fusedCongestion;
        result.sourceCount = sources.size();
        result.sources = new ArrayList<>(sources);
        result.replanRecommended = fusedCongestion > 0.6 || averageScore < 0.45;
        return result;
    }

    private double scoreSource(String sourceType) {
        if (sourceType == null) {
            return 0.4;
        }
        switch (sourceType) {
            case "CN_LEO_SATCOM":
            case "CN_LEO_SATCOM_OR_5GA_EDGE":
                return 0.95;
            case "PRIVATE_5GA_EDGE":
                return 0.90;
            case "BEIDOU_SHORT_MESSAGE":
                return 0.70;
            case "COMPLIANT_BACKBONE":
                return 0.60;
            case "DRONE":
                return 0.85;
            case "VEHICLE":
                return 0.75;
            case "RSU":
                return 0.80;
            default:
                return 0.60;
        }
    }

    private double scoreFreshness(long ageMillis) {
        if (ageMillis < 3000) return 1.0;
        if (ageMillis < 10000) return 0.8;
        if (ageMillis < 30000) return 0.6;
        return 0.3;
    }

    private double scoreConsistency(String scope, String targetScope) {
        if (targetScope == null) return 0.7;
        if (scope == null) return 0.5;
        if (scope.equals(targetScope)) return 1.0;
        if (scope.startsWith(targetScope) || targetScope.startsWith(scope)) return 0.75;
        return 0.4;
    }

    public static class FusionResult {
        public double fusionScore;
        public double fusedCongestion;
        public int sourceCount;
        public List<String> sources = new ArrayList<>();
        public boolean replanRecommended;

        public static FusionResult empty() {
            FusionResult result = new FusionResult();
            result.fusionScore = 0.0;
            result.fusedCongestion = 0.3;
            result.sourceCount = 0;
            result.replanRecommended = false;
            return result;
        }
    }
}
