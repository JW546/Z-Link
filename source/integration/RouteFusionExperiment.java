package integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Comparative experiment for the passenger-intent route fusion loop.
 *
 * Variables:
 * 1. DDS QoS mode: reliable vs best-effort.
 * 2. Network latency: low, medium, high.
 * 3. Device-advice conflict: none, moderate, high.
 *
 * Outputs:
 * - final route
 * - risk score
 * - delivered advice ratio
 * - retry count
 * - stale advice count
 * - route stability
 *
 * This class is deterministic and does not require hardware, ROS2, or a real DDS runtime.
 */
public class RouteFusionExperiment {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<DeviceAdvice> BASE_ADVICE = Arrays.asList(
        new DeviceAdvice("EGO_VEHICLE", "route_A_main_road", 0.34, 0.82),
        new DeviceAdvice("PEER_VEHICLE", "route_C_elevated_detour", 0.30, 0.76),
        new DeviceAdvice("UAV", "route_C_elevated_detour", 0.28, 0.88),
        new DeviceAdvice("RSU", "route_D_emergency_green_wave", 0.24, 0.86),
        new DeviceAdvice("COMPLIANT_BACKBONE", "route_D_emergency_green_wave", 0.27, 0.80)
    );

    public static void main(String[] args) {
        List<ExperimentCase> cases = buildCases();
        List<ExperimentResult> results = new ArrayList<>();

        System.out.println("============================================================");
        System.out.println("  Route Fusion Comparative Experiment");
        System.out.println("  variables: QoS, latency, device-advice conflict");
        System.out.println("============================================================");
        for (ExperimentCase experimentCase : cases) {
            ExperimentResult result = runCase(experimentCase);
            results.add(result);
            System.out.println(result.toConsoleRow());
        }
        exportReports(results);
        System.out.println("============================================================");
    }

    private static List<ExperimentCase> buildCases() {
        List<ExperimentCase> cases = new ArrayList<>();
        for (QosMode qos : QosMode.values()) {
            for (LatencyProfile latency : LatencyProfile.values()) {
                for (ConflictProfile conflict : ConflictProfile.values()) {
                    cases.add(new ExperimentCase(qos, latency, conflict));
                }
            }
        }
        return cases;
    }

    private static ExperimentResult runCase(ExperimentCase experimentCase) {
        Random random = new Random(experimentCase.seed());
        List<DeviceAdvice> generated = generateConflictAdvice(experimentCase.conflict);
        List<DeliveredAdvice> delivered = new ArrayList<>();
        int retries = 0;
        int stale = 0;
        int dropped = 0;

        for (DeviceAdvice advice : generated) {
            Delivery delivery = deliver(advice, experimentCase, random);
            retries += delivery.retries;
            if (delivery.delivered && !delivery.stale) {
                delivered.add(new DeliveredAdvice(advice, delivery.latencyMs));
            } else if (delivery.stale) {
                stale++;
            } else {
                dropped++;
            }
        }

        FusionDecision decision = fuse(delivered, experimentCase.conflict);
        double deliveredRatio = generated.isEmpty() ? 0.0 : (double) delivered.size() / generated.size();
        double routeStability = estimateRouteStability(delivered, decision.route);
        return new ExperimentResult(
            experimentCase,
            generated.size(),
            delivered.size(),
            dropped,
            retries,
            stale,
            averageLatency(delivered),
            deliveredRatio,
            routeStability,
            decision.route,
            decision.risk,
            decision.confidence,
            decision.reason);
    }

    private static List<DeviceAdvice> generateConflictAdvice(ConflictProfile conflict) {
        List<DeviceAdvice> advice = new ArrayList<>(BASE_ADVICE);
        if (conflict == ConflictProfile.NONE) {
            advice.set(0, new DeviceAdvice("EGO_VEHICLE", "route_C_elevated_detour", 0.31, 0.84));
            advice.set(3, new DeviceAdvice("RSU", "route_C_elevated_detour", 0.29, 0.87));
            advice.set(4, new DeviceAdvice("COMPLIANT_BACKBONE", "route_C_elevated_detour", 0.30, 0.82));
        } else if (conflict == ConflictProfile.MODERATE) {
            advice.set(0, new DeviceAdvice("EGO_VEHICLE", "route_A_main_road", 0.42, 0.82));
            advice.set(1, new DeviceAdvice("PEER_VEHICLE", "route_C_elevated_detour", 0.30, 0.76));
            advice.set(2, new DeviceAdvice("UAV", "route_C_elevated_detour", 0.28, 0.88));
            advice.set(3, new DeviceAdvice("RSU", "route_D_emergency_green_wave", 0.26, 0.86));
        } else {
            advice.set(0, new DeviceAdvice("EGO_VEHICLE", "route_A_main_road", 0.52, 0.78));
            advice.set(1, new DeviceAdvice("PEER_VEHICLE", "route_C_elevated_detour", 0.37, 0.70));
            advice.set(2, new DeviceAdvice("UAV", "route_B_school_zone_slow_safe", 0.44, 0.66));
            advice.set(3, new DeviceAdvice("RSU", "route_D_emergency_green_wave", 0.25, 0.87));
            advice.set(4, new DeviceAdvice("COMPLIANT_BACKBONE", "route_C_elevated_detour", 0.36, 0.74));
        }
        return advice;
    }

    private static Delivery deliver(DeviceAdvice advice, ExperimentCase experimentCase, Random random) {
        int latency = experimentCase.latency.baseMs
            + random.nextInt(experimentCase.latency.jitterMs + 1)
            + deviceExtraLatency(advice.source);
        double lossRate = experimentCase.qos == QosMode.RELIABLE
            ? experimentCase.latency.reliableLossRate
            : experimentCase.latency.bestEffortLossRate;
        boolean firstAttemptLost = random.nextDouble() < lossRate;
        int retries = 0;
        boolean delivered = !firstAttemptLost;
        if (firstAttemptLost && experimentCase.qos == QosMode.RELIABLE) {
            retries = 1;
            latency += 90;
            delivered = random.nextDouble() > lossRate * 0.35;
        }
        boolean stale = delivered && latency > experimentCase.latency.deadlineMs;
        return new Delivery(delivered, stale, retries, latency);
    }

    private static int deviceExtraLatency(String source) {
        if ("UAV".equals(source)) return 25;
        if ("COMPLIANT_BACKBONE".equals(source)) return 45;
        if ("RSU".equals(source)) return 15;
        return 5;
    }

    private static FusionDecision fuse(List<DeliveredAdvice> delivered, ConflictProfile conflict) {
        if (delivered.isEmpty()) {
            return new FusionDecision("route_A_main_road", 0.72, 0.25, "fallback_no_advice_delivered");
        }

        Map<String, Double> scoreByRoute = new LinkedHashMap<>();
        Map<String, Double> riskByRoute = new LinkedHashMap<>();
        for (DeliveredAdvice item : delivered) {
            DeviceAdvice advice = item.advice;
            double freshness = Math.max(0.55, 1.0 - item.latencyMs / 1200.0);
            double authority = sourceAuthority(advice.source);
            double conflictPenalty = conflict == ConflictProfile.HIGH ? 0.88 : (conflict == ConflictProfile.MODERATE ? 0.94 : 1.0);
            double score = advice.confidence * authority * freshness * conflictPenalty * (1.0 - advice.risk);
            scoreByRoute.merge(advice.route, score, Double::sum);
            riskByRoute.merge(advice.route, advice.risk * score, Double::sum);
        }

        String bestRoute = scoreByRoute.entrySet().stream()
            .max(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse("route_A_main_road");
        double bestScore = scoreByRoute.getOrDefault(bestRoute, 0.01);
        double weightedRisk = riskByRoute.getOrDefault(bestRoute, 0.5) / Math.max(bestScore, 0.01);
        double confidence = Math.min(0.96, 0.45 + bestScore / Math.max(1.0, delivered.size()));
        return new FusionDecision(bestRoute, clamp(weightedRisk, 0.05, 0.95), confidence, "weighted_risk_confidence_fusion");
    }

    private static double sourceAuthority(String source) {
        if ("RSU".equals(source)) return 1.18;
        if ("UAV".equals(source)) return 1.10;
        if ("COMPLIANT_BACKBONE".equals(source)) return 1.04;
        if ("EGO_VEHICLE".equals(source)) return 1.12;
        return 0.96;
    }

    private static double estimateRouteStability(List<DeliveredAdvice> delivered, String finalRoute) {
        if (delivered.isEmpty()) return 0.0;
        long votes = delivered.stream().filter(item -> finalRoute.equals(item.advice.route)).count();
        return (double) votes / delivered.size();
    }

    private static double averageLatency(List<DeliveredAdvice> delivered) {
        if (delivered.isEmpty()) return 0.0;
        double sum = 0.0;
        for (DeliveredAdvice item : delivered) sum += item.latencyMs;
        return sum / delivered.size();
    }

    private static void exportReports(List<ExperimentResult> results) {
        try {
            Path dir = Paths.get("artifacts", "reports");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(TS);
            Path csv = dir.resolve("route_fusion_experiment_" + stamp + ".csv");
            Path md = dir.resolve("route_fusion_experiment_" + stamp + ".md");

            List<String> csvLines = new ArrayList<>();
            csvLines.add("qos,latency,conflict,total_advice,delivered,dropped,retries,stale,avg_latency_ms,delivered_ratio,route_stability,final_route,risk,confidence,reason");
            for (ExperimentResult r : results) csvLines.add(r.toCsv());
            Files.write(csv, csvLines, StandardCharsets.UTF_8);

            List<String> mdLines = new ArrayList<>();
            mdLines.add("# Route Fusion Comparative Experiment");
            mdLines.add("");
            mdLines.add("This experiment compares how DDS QoS, network latency, and device-advice conflict influence the passenger-intent route fusion result.");
            mdLines.add("");
            mdLines.add("| QoS | Latency | Conflict | Delivered | Retry | Stale | Avg Latency | Stability | Final Route | Risk | Confidence |");
            mdLines.add("| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: |");
            for (ExperimentResult r : results) mdLines.add(r.toMarkdownRow());
            mdLines.add("");
            mdLines.add("## How To Read");
            mdLines.add("");
            mdLines.add("- Higher delivered ratio means more device advice reached the fusion agent before the deadline.");
            mdLines.add("- Higher route stability means more delivered devices agreed with the final route.");
            mdLines.add("- Reliable QoS usually increases retries and latency, but reduces lost critical advice.");
            mdLines.add("- High conflict cases test whether the fusion agent can still prefer lower-risk routes.");
            Files.write(md, mdLines, StandardCharsets.UTF_8);

            System.out.println("[REPORT] CSV: " + csv.toAbsolutePath());
            System.out.println("[REPORT] MD : " + md.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("failed to export route fusion experiment reports", e);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private enum QosMode {
        RELIABLE,
        BEST_EFFORT
    }

    private enum LatencyProfile {
        LOW(30, 30, 260, 0.01, 0.04),
        MEDIUM(150, 120, 360, 0.04, 0.12),
        HIGH(380, 260, 520, 0.10, 0.26);

        final int baseMs;
        final int jitterMs;
        final int deadlineMs;
        final double reliableLossRate;
        final double bestEffortLossRate;

        LatencyProfile(int baseMs, int jitterMs, int deadlineMs, double reliableLossRate, double bestEffortLossRate) {
            this.baseMs = baseMs;
            this.jitterMs = jitterMs;
            this.deadlineMs = deadlineMs;
            this.reliableLossRate = reliableLossRate;
            this.bestEffortLossRate = bestEffortLossRate;
        }
    }

    private enum ConflictProfile {
        NONE,
        MODERATE,
        HIGH
    }

    private static final class ExperimentCase {
        final QosMode qos;
        final LatencyProfile latency;
        final ConflictProfile conflict;

        ExperimentCase(QosMode qos, LatencyProfile latency, ConflictProfile conflict) {
            this.qos = qos;
            this.latency = latency;
            this.conflict = conflict;
        }

        long seed() {
            return 20260507L + qos.ordinal() * 1000L + latency.ordinal() * 100L + conflict.ordinal() * 10L;
        }
    }

    private static final class DeviceAdvice {
        final String source;
        final String route;
        final double risk;
        final double confidence;

        DeviceAdvice(String source, String route, double risk, double confidence) {
            this.source = source;
            this.route = route;
            this.risk = risk;
            this.confidence = confidence;
        }
    }

    private static final class DeliveredAdvice {
        final DeviceAdvice advice;
        final int latencyMs;

        DeliveredAdvice(DeviceAdvice advice, int latencyMs) {
            this.advice = advice;
            this.latencyMs = latencyMs;
        }
    }

    private static final class Delivery {
        final boolean delivered;
        final boolean stale;
        final int retries;
        final int latencyMs;

        Delivery(boolean delivered, boolean stale, int retries, int latencyMs) {
            this.delivered = delivered;
            this.stale = stale;
            this.retries = retries;
            this.latencyMs = latencyMs;
        }
    }

    private static final class FusionDecision {
        final String route;
        final double risk;
        final double confidence;
        final String reason;

        FusionDecision(String route, double risk, double confidence, String reason) {
            this.route = route;
            this.risk = risk;
            this.confidence = confidence;
            this.reason = reason;
        }
    }

    private static final class ExperimentResult {
        final ExperimentCase experimentCase;
        final int totalAdvice;
        final int delivered;
        final int dropped;
        final int retries;
        final int stale;
        final double avgLatencyMs;
        final double deliveredRatio;
        final double routeStability;
        final String finalRoute;
        final double risk;
        final double confidence;
        final String reason;

        ExperimentResult(
            ExperimentCase experimentCase,
            int totalAdvice,
            int delivered,
            int dropped,
            int retries,
            int stale,
            double avgLatencyMs,
            double deliveredRatio,
            double routeStability,
            String finalRoute,
            double risk,
            double confidence,
            String reason) {
            this.experimentCase = experimentCase;
            this.totalAdvice = totalAdvice;
            this.delivered = delivered;
            this.dropped = dropped;
            this.retries = retries;
            this.stale = stale;
            this.avgLatencyMs = avgLatencyMs;
            this.deliveredRatio = deliveredRatio;
            this.routeStability = routeStability;
            this.finalRoute = finalRoute;
            this.risk = risk;
            this.confidence = confidence;
            this.reason = reason;
        }

        String toConsoleRow() {
            return String.format(Locale.ROOT,
                "[CASE] qos=%s latency=%s conflict=%s delivered=%d/%d retry=%d stale=%d avgLatency=%.1fms stability=%.2f route=%s risk=%.3f confidence=%.3f",
                experimentCase.qos, experimentCase.latency, experimentCase.conflict,
                delivered, totalAdvice, retries, stale, avgLatencyMs, routeStability, finalRoute, risk, confidence);
        }

        String toCsv() {
            return String.join(",",
                experimentCase.qos.name(),
                experimentCase.latency.name(),
                experimentCase.conflict.name(),
                String.valueOf(totalAdvice),
                String.valueOf(delivered),
                String.valueOf(dropped),
                String.valueOf(retries),
                String.valueOf(stale),
                fmt(avgLatencyMs),
                fmt(deliveredRatio),
                fmt(routeStability),
                finalRoute,
                fmt(risk),
                fmt(confidence),
                reason);
        }

        String toMarkdownRow() {
            return "| " + experimentCase.qos
                + " | " + experimentCase.latency
                + " | " + experimentCase.conflict
                + " | " + delivered + "/" + totalAdvice
                + " | " + retries
                + " | " + stale
                + " | " + fmt(avgLatencyMs) + "ms"
                + " | " + fmt(routeStability)
                + " | " + finalRoute
                + " | " + fmt(risk)
                + " | " + fmt(confidence)
                + " |";
        }
    }
}
