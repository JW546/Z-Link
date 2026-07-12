package integration;

import dds.DDSManager;
import intentdds.urban.QosObservation;
import intentdds.urban.RouteAdvice;
import intentdds.urban.RouteDecision;

import java.time.Instant;
import java.util.Locale;

/**
 * Two-host RTI DDS demo for intent-driven unmanned-device collaboration.
 *
 * Receiver:
 *   java integration.RealDDSIntentPeerDemo receive 43
 *
 * Sender:
 *   java integration.RealDDSIntentPeerDemo send 43 UAV-CQ-007 CAR-CQ-001
 */
public class RealDDSIntentPeerDemo {
    private static final int DEFAULT_DOMAIN_ID = 43;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "receive";
        int domainId = args.length > 1 ? parseInt(args[1], DEFAULT_DOMAIN_ID) : DEFAULT_DOMAIN_ID;
        String sourceId = args.length > 2 ? args[2] : "UAV-CQ-007";
        String targetId = args.length > 3 ? args[3] : "CAR-CQ-001";

        if ("send".equals(mode) || "pub".equals(mode) || "publish".equals(mode)) {
            runSender(domainId, sourceId, targetId);
        } else {
            runReceiver(domainId);
        }
    }

    private static void runReceiver(int domainId) {
        System.out.println("============================================================");
        System.out.println("  Real DDS Intent Receiver");
        System.out.println("  Domain: " + domainId);
        System.out.println("  Topics: " + DDSManager.TOPIC_ROUTE_ADVICE + ", "
            + DDSManager.TOPIC_ROUTE_DECISION + ", " + DDSManager.TOPIC_QOS_OBSERVATION);
        System.out.println("============================================================");

        DDSManager dds = DDSManager.getInstance(domainId);
        if (!dds.connect()) {
            System.err.println("[FAIL] DDS connection failed, receiver cannot start.");
            System.exit(1);
        }

        dds.subscribeRouteAdvice(advice -> System.out.println(
            "[RECV][RouteAdvice] from=" + advice.header.source_id
                + " sourceType=" + advice.source_type
                + " target=" + advice.header.target_id
                + " route=" + advice.route_candidate
                + " risk=" + fmt(advice.risk_score)
                + " confidence=" + fmt(advice.confidence)
                + " reason=" + advice.advice_reason));

        dds.subscribeRouteDecision(decision -> System.out.println(
            "[RECV][RouteDecision] owner=" + decision.decision_owner
                + " target=" + decision.header.target_id
                + " finalRoute=" + decision.final_route
                + " risk=" + fmt(decision.final_risk_score)
                + " explanation=" + decision.passenger_explanation));

        dds.subscribeQosObservation(observation -> System.out.println(
            "[RECV][QosObservation] profile=" + observation.qos_profile
                + " delivered=" + observation.delivered_count
                + " dropped=" + observation.dropped_count
                + " ratio=" + fmt(observation.delivered_ratio)
                + " latencyMs=" + fmt(observation.average_latency_ms)));

        System.out.println("[READY] Receiver is waiting. Press Ctrl+C to stop.");
        keepAlive();
    }

    private static void runSender(int domainId, String sourceId, String targetId) {
        System.out.println("============================================================");
        System.out.println("  Real DDS Intent Sender");
        System.out.println("  Domain: " + domainId);
        System.out.println("  Source: " + sourceId + " -> Target: " + targetId);
        System.out.println("============================================================");

        DDSManager dds = DDSManager.getInstance(domainId);
        if (!dds.connect()) {
            System.err.println("[FAIL] DDS connection failed, sender cannot start.");
            System.exit(1);
        }

        for (int i = 1; i <= 5; i++) {
            RouteAdvice advice = routeAdvice(i, sourceId, targetId);
            long adviceSeq = dds.publishRouteAdvice(advice);
            System.out.println("[SEND][RouteAdvice] seq=" + adviceSeq
                + " route=" + advice.route_candidate + " risk=" + fmt(advice.risk_score));

            if (i == 3) {
                RouteDecision decision = routeDecision(sourceId, targetId);
                long decisionSeq = dds.publishRouteDecision(decision);
                System.out.println("[SEND][RouteDecision] seq=" + decisionSeq
                    + " finalRoute=" + decision.final_route);
            }

            QosObservation observation = qosObservation(i, sourceId);
            long qosSeq = dds.publishQosObservation(observation);
            System.out.println("[SEND][QosObservation] seq=" + qosSeq
                + " ratio=" + fmt(observation.delivered_ratio));

            sleep(1000L);
        }

        dds.disconnect();
        System.out.println("[PASS] Sender published intent collaboration samples.");
    }

    private static RouteAdvice routeAdvice(int index, String sourceId, String targetId) {
        RouteAdvice advice = new RouteAdvice();
        fillHeader(advice.header, "ADVICE-" + index, sourceId, targetId);
        advice.source_type = sourceTypeFromId(sourceId);
        advice.destination = "Southwest University Hospital";
        advice.route_candidate = index >= 3 ? "route_D_emergency_green_wave" : "route_C_elevated_detour";
        advice.risk_score = index >= 3 ? 0.20 : 0.28 + index * 0.02;
        advice.confidence = 0.86;
        advice.estimated_time_min = index >= 3 ? 9.5 : 11.5;
        advice.advice_reason = "device publishes autonomous route advice through RTI DDS";
        advice.required_topics.add(DDSManager.TOPIC_ROUTE_DECISION);
        return advice;
    }

    private static RouteDecision routeDecision(String sourceId, String targetId) {
        RouteDecision decision = new RouteDecision();
        fillHeader(decision.header, "DECISION-1", targetId, sourceId);
        decision.decision_owner = targetId;
        decision.autonomy_mode = "vehicle_self_decision_after_dds_consensus";
        decision.final_route = "route_D_emergency_green_wave";
        decision.final_risk_score = 0.20;
        decision.confidence = 0.88;
        decision.advice_sources.add(sourceId);
        decision.passenger_explanation = "Fused remote device advice over real DDS.";
        decision.fusion_reason = "lowest explainable risk under urgent intent";
        return decision;
    }

    private static QosObservation qosObservation(int index, String sourceId) {
        QosObservation observation = new QosObservation();
        fillHeader(observation.header, "QOS-" + index, sourceId, "ALL");
        observation.qos_profile = "reliable_keep_last_10_deadline_300ms";
        observation.published_count = index * 2;
        observation.delivered_count = index * 2;
        observation.dropped_count = 0;
        observation.retry_count = 0;
        observation.stale_count = 0;
        observation.average_latency_ms = 12.0 + index;
        observation.delivered_ratio = 1.0;
        return observation;
    }

    private static void fillHeader(intentdds.urban.Header header, String messageId, String sourceId, String targetId) {
        header.message_id = messageId;
        header.source_id = sourceId;
        header.target_id = targetId;
        header.timestamp_iso8601 = Instant.now().toString();
        header.frame_id = "map";
    }

    private static String sourceTypeFromId(String sourceId) {
        String normalized = sourceId.toUpperCase(Locale.ROOT);
        if (normalized.contains("UAV")) return "UAV";
        if (normalized.contains("RSU")) return "RSU";
        if (normalized.contains("BACKBONE")) return "COMPLIANT_BACKBONE";
        if (normalized.contains("PEER")) return "PEER_VEHICLE";
        return "EGO_VEHICLE";
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void keepAlive() {
        try {
            while (true) {
                Thread.sleep(1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
