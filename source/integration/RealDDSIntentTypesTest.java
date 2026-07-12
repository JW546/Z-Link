package integration;

import dds.DDSManager;
import intentdds.urban.QosObservation;
import intentdds.urban.RouteAdvice;
import intentdds.urban.RouteDecision;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that the RTI-generated intent-collaboration IDL types can be
 * published and received through the real DDSManager adapter.
 */
public class RealDDSIntentTypesTest {
    public static void main(String[] args) throws Exception {
        DDSManager dds = DDSManager.getInstance(42);
        dds.setLoggingEnabled(false);

        if (!dds.connect()) {
            throw new IllegalStateException("[FAIL] DDS connection failed");
        }

        CountDownLatch latch = new CountDownLatch(3);
        dds.subscribeRouteAdvice(advice -> {
            require("UAV".equals(advice.source_type), "RouteAdvice source type survives DDS");
            require("route_C_elevated_detour".equals(advice.route_candidate), "RouteAdvice route survives DDS");
            latch.countDown();
        });
        dds.subscribeRouteDecision(decision -> {
            require("CAR-CQ-001".equals(decision.decision_owner), "RouteDecision owner survives DDS");
            require("route_D_emergency_green_wave".equals(decision.final_route), "RouteDecision route survives DDS");
            latch.countDown();
        });
        dds.subscribeQosObservation(observation -> {
            require("reliable_keep_last_10_deadline_300ms".equals(observation.qos_profile), "QosObservation profile survives DDS");
            require(observation.delivered_ratio > 0.9, "QosObservation ratio survives DDS");
            latch.countDown();
        });

        Thread.sleep(300L);

        dds.publishRouteAdvice(routeAdvice());
        dds.publishRouteDecision(routeDecision());
        dds.publishQosObservation(qosObservation());

        require(latch.await(5, TimeUnit.SECONDS), "real DDS loopback delivers intent collaboration samples");
        dds.disconnect();
        System.out.println("[PASS] Real DDS intent type test passed.");
    }

    private static RouteAdvice routeAdvice() {
        RouteAdvice advice = new RouteAdvice();
        fillHeader(advice.header, "MSG-ADVICE-001", "UAV-CQ-007", "CAR-CQ-001");
        advice.source_type = "UAV";
        advice.destination = "Southwest University Hospital";
        advice.route_candidate = "route_C_elevated_detour";
        advice.risk_score = 0.28;
        advice.confidence = 0.90;
        advice.estimated_time_min = 11.5;
        advice.advice_reason = "forward corridor is clear enough for safe detour";
        advice.required_topics.add(DDSManager.TOPIC_ROUTE_DECISION);
        return advice;
    }

    private static RouteDecision routeDecision() {
        RouteDecision decision = new RouteDecision();
        fillHeader(decision.header, "MSG-DECISION-001", "CAR-CQ-001", "CABIN-CAR-CQ-001");
        decision.decision_owner = "CAR-CQ-001";
        decision.autonomy_mode = "vehicle_self_decision_after_dds_consensus";
        decision.final_route = "route_D_emergency_green_wave";
        decision.final_risk_score = 0.20;
        decision.confidence = 0.88;
        decision.advice_sources.add("UAV-CQ-007");
        decision.advice_sources.add("RSU-SCHOOL-009");
        decision.passenger_explanation = "Fused UAV, RSU, and peer vehicle advice.";
        decision.fusion_reason = "lowest explainable risk under urgent passenger intent";
        return decision;
    }

    private static QosObservation qosObservation() {
        QosObservation observation = new QosObservation();
        fillHeader(observation.header, "MSG-QOS-001", "DDS-MONITOR", "ALL");
        observation.qos_profile = "reliable_keep_last_10_deadline_300ms";
        observation.published_count = 3;
        observation.delivered_count = 3;
        observation.dropped_count = 0;
        observation.retry_count = 0;
        observation.stale_count = 0;
        observation.average_latency_ms = 12.5;
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
