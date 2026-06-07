package integration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Future autonomous intelligent transportation scenario over a virtual DDS bus.
 *
 * The scene is not a control-center dispatch model. A passenger speaks natural
 * language inside an autonomous vehicle. The vehicle, peer vehicle, UAV, RSU,
 * and compliant wide-area link decide what to publish and subscribe by
 * themselves. The ego vehicle fuses advice and publishes the final route.
 */
public class VirtualDDSScenarioDemo {
    private static final String TOPIC_PASSENGER_INTENT = "cabin/passenger/intent.dsl";
    private static final String TOPIC_VEHICLE_TELEMETRY = "urban/vehicle/telemetry.dsl";
    private static final String TOPIC_PEER_ADVICE = "urban/vehicle/peer_advice.dsl";
    private static final String TOPIC_UAV_PERCEPTION = "urban/uav/aerial_perception.dsl";
    private static final String TOPIC_RSU_EVENT = "urban/rsu/road_event.dsl";
    private static final String TOPIC_BACKBONE_CONTEXT = "backbone/compliant/context.dsl";
    private static final String TOPIC_ROUTE_ADVICE = "urban/route/advice.dsl";
    private static final String TOPIC_FINAL_DECISION = "urban/route/final_decision.dsl";

    public static void main(String[] args) {
        int seconds = parseDurationSeconds(args, 22);
        System.out.println("============================================================");
        System.out.println("  Future Autonomous Traffic Virtual DDS Scenario");
        System.out.println("  Passenger NL -> DSL -> IDL/DDS -> Device Pub/Sub -> Route Decision");
        System.out.println("============================================================");

        VirtualWorld world = new VirtualWorld();
        VirtualDDSBus bus = new VirtualDDSBus(0.035);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

        PassengerCabinNode cabin = new PassengerCabinNode("CABIN-CAR-CQ-001", "CAR-CQ-001", bus, world);
        AutonomousVehicleNode egoCar = new AutonomousVehicleNode("CAR-CQ-001", true, bus, world);
        AutonomousVehicleNode peerCar = new AutonomousVehicleNode("CAR-CQ-002", false, bus, world);
        AerialUavNode uav = new AerialUavNode("UAV-CQ-007", bus, world);
        RoadSideUnitNode rsu = new RoadSideUnitNode("RSU-SCHOOL-009", bus, world);
        CompliantBackboneNode backbone = new CompliantBackboneNode("CN-BACKBONE-CQ", bus, world);

        cabin.bindSubscriptions();
        egoCar.bindSubscriptions();
        peerCar.bindSubscriptions();
        uav.bindSubscriptions();
        rsu.bindSubscriptions();
        backbone.bindSubscriptions();

        scheduler.scheduleAtFixedRate(() -> {
            world.tick();
            renderScene(world);
        }, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(cabin::tick, 1, 7, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(egoCar::tick, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(peerCar::tick, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(uav::tick, 1, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(rsu::tick, 1, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(backbone::tick, 2, 3, TimeUnit.SECONDS);

        System.out.println("[SCENE] virtual scenario started, duration=" + seconds + "s");
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.shutdownNow();
        bus.shutdown();
        System.out.println("------------------------------------------------------------");
        System.out.println("[SUMMARY] DDS published messages: " + bus.getPublishedCount());
        System.out.println("[SUMMARY] DDS delivered messages: " + bus.getDeliveredCount());
        System.out.println("[SUMMARY] DDS dropped messages: " + bus.getDroppedCount());
        System.out.println("[SUMMARY] DDS retry deliveries: " + bus.getRetriedCount());
        System.out.println("[SUMMARY] final route: " + world.finalRoute);
        System.out.println("[SUMMARY] conclusion: device-side autonomous collaboration loop completed without real hardware.");
        System.out.println("============================================================");
    }

    private static int parseDurationSeconds(String[] args, int fallback) {
        if (args == null || args.length == 0) return fallback;
        try {
            return Math.max(5, Math.min(120, Integer.parseInt(args[0])));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void renderScene(VirtualWorld world) {
        System.out.println();
        System.out.println("--------------------------- DIGITAL TWIN ---------------------------");
        System.out.println(world.describe());
        System.out.println("--------------------------------------------------------------------");
    }

    private static final class VirtualWorld {
        int frame;
        double egoX = 12.0;
        double peerX = 7.5;
        double uavX = 10.0;
        double ambulanceLaneClearance = 0.62;
        double roadFriction = 0.83;
        double private5gSignal = 0.92;
        double rsuLoad = 0.34;
        double uavVisibility = 2600.0;
        double windSpeed = 6.2;
        int pedestrianCount;
        String trafficSignal = "GREEN";
        String roadEvent = "normal";
        String finalRoute = "pending";

        void tick() {
            frame++;
            egoX += 1.6;
            peerX += 1.45;
            uavX += 1.2;
            if (frame >= 4 && frame < 9) {
                roadEvent = "school_zone_pedestrian_crossing";
                pedestrianCount = 5;
                trafficSignal = "YELLOW";
                rsuLoad = 0.58;
            } else if (frame >= 9 && frame < 15) {
                roadEvent = "main_road_accident_and_congestion";
                pedestrianCount = 1;
                trafficSignal = "RED";
                rsuLoad = 0.79;
                ambulanceLaneClearance = 0.86;
            } else if (frame >= 15) {
                roadEvent = "rain_wet_road_with_partial_detour";
                pedestrianCount = 0;
                trafficSignal = "GREEN";
                roadFriction = 0.68;
                windSpeed = 10.5;
                uavVisibility = 1700.0;
            }
            private5gSignal = clamp(private5gSignal + (frame % 2 == 0 ? -0.015 : 0.01), 0.72, 0.97);
            rsuLoad = clamp(rsuLoad + (frame % 3 == 0 ? 0.03 : -0.01), 0.25, 0.88);
            uavVisibility = clamp(uavVisibility + (frame % 4 == 0 ? -120.0 : 40.0), 1200.0, 3000.0);
        }

        String describe() {
            return "frame=" + frame
                + " | egoCarX=" + fmt(egoX)
                + " | peerCarX=" + fmt(peerX)
                + " | uavX=" + fmt(uavX)
                + " | roadEvent=" + roadEvent
                + " | signal=" + trafficSignal
                + " | pedestrians=" + pedestrianCount
                + " | friction=" + fmt(roadFriction)
                + " | rsuLoad=" + fmt(rsuLoad)
                + " | 5G/privateLink=" + fmt(private5gSignal)
                + " | finalRoute=" + finalRoute;
        }
    }

    private static final class VirtualDDSBus {
        private final AtomicLong seq = new AtomicLong();
        private final AtomicInteger publishedCount = new AtomicInteger();
        private final AtomicInteger deliveredCount = new AtomicInteger();
        private final AtomicInteger droppedCount = new AtomicInteger();
        private final AtomicInteger retriedCount = new AtomicInteger();
        private final DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        private final Map<String, CopyOnWriteArrayList<DdsSubscription>> subscribers = new ConcurrentHashMap<>();
        private final ScheduledExecutorService dispatchPool = Executors.newScheduledThreadPool(4);
        private final Random random = new Random(20260507);
        private final double dropRate;

        VirtualDDSBus(double dropRate) {
            this.dropRate = dropRate;
        }

        void subscribe(String topic, String subscriberId, Consumer<DslPacket> consumer) {
            subscribers.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<>())
                .add(new DdsSubscription(subscriberId, consumer));
            System.out.println("[" + LocalTime.now().format(tsFmt) + "] [DDS-SUB] topic=" + topic
                + ", subscriber=" + subscriberId);
        }

        void publish(String topic, DslPacket packet) {
            long currentSeq = seq.incrementAndGet();
            packet.topic = topic;
            packet.sequence = currentSeq;
            packet.publishedAtMillis = System.currentTimeMillis();
            publishedCount.incrementAndGet();
            System.out.println("[" + LocalTime.now().format(tsFmt) + "] [DDS-PUB] topic=" + topic
                + ", kind=" + packet.kind
                + ", qos=" + packet.qos.profile
                + ", from=" + packet.sourceId
                + ", to=" + packet.targetId
                + ", seq=" + currentSeq);
            logDslContractIfPresent(packet);
            List<DdsSubscription> subscriptions = subscribers.get(topic);
            if (subscriptions == null || subscriptions.isEmpty()) return;
            for (DdsSubscription subscription : subscriptions) {
                dispatchPool.schedule(() -> deliverWithQos(subscription, packet, false), 35, TimeUnit.MILLISECONDS);
            }
        }

        private void logDslContractIfPresent(DslPacket packet) {
            if (packet.dsl == null) return;
            boolean hasDataContract = packet.dsl.contains("dataExchangeContract");
            boolean hasCommandContract = packet.dsl.contains("commandExchangeContract");
            boolean hasIdlBinding = packet.dsl.contains("ddsIdlBinding");
            if (!hasDataContract && !hasCommandContract && !hasIdlBinding) return;
            System.out.println("[DSL-CONTRACT] kind=" + packet.kind
                + ", dataExchangeContract=" + hasDataContract
                + ", commandExchangeContract=" + hasCommandContract
                + ", ddsIdlBinding=" + hasIdlBinding
                + ", idlType=" + packet.metrics.getOrDefault("idlType", packet.metrics.getOrDefault("idl_type", "unknown")));
            if ("PASSENGER_INTENT".equals(packet.kind)) {
                System.out.println("[DSL-CONTRACT] passenger intent maps NL intent into DDS topic subscriptions and IDL payload fields.");
            }
        }

        private void deliverWithQos(DdsSubscription subscription, DslPacket packet, boolean retry) {
            if (System.currentTimeMillis() - packet.publishedAtMillis > packet.qos.ttlMillis) {
                droppedCount.incrementAndGet();
                System.out.println("[DDS-DROP] seq=" + packet.sequence + ", reason=ttl_expired");
                return;
            }
            boolean loss = random.nextDouble() < dropRate;
            if (loss && packet.qos.reliable && !retry) {
                retriedCount.incrementAndGet();
                dispatchPool.schedule(() -> deliverWithQos(subscription, packet, true), packet.qos.retryDelayMillis, TimeUnit.MILLISECONDS);
                return;
            }
            if (loss) {
                droppedCount.incrementAndGet();
                System.out.println("[DDS-DROP] seq=" + packet.sequence + ", reason=simulated_wireless_loss");
                return;
            }
            deliveredCount.incrementAndGet();
            System.out.println("[" + LocalTime.now().format(tsFmt) + "] [DDS-RECV] topic=" + packet.topic
                + ", kind=" + packet.kind
                + ", from=" + packet.sourceId
                + ", subscriber=" + subscription.subscriberId
                + ", seq=" + packet.sequence
                + (retry ? ", retry=true" : ""));
            subscription.consumer.accept(packet);
        }

        int getPublishedCount() { return publishedCount.get(); }
        int getDeliveredCount() { return deliveredCount.get(); }
        int getDroppedCount() { return droppedCount.get(); }
        int getRetriedCount() { return retriedCount.get(); }
        void shutdown() { dispatchPool.shutdownNow(); }
    }

    private static final class DdsSubscription {
        final String subscriberId;
        final Consumer<DslPacket> consumer;

        DdsSubscription(String subscriberId, Consumer<DslPacket> consumer) {
            this.subscriberId = subscriberId;
            this.consumer = consumer;
        }
    }

    private abstract static class SimNode {
        final String nodeId;
        final VirtualDDSBus bus;
        final VirtualWorld world;

        SimNode(String nodeId, VirtualDDSBus bus, VirtualWorld world) {
            this.nodeId = nodeId;
            this.bus = bus;
            this.world = world;
        }

        abstract void bindSubscriptions();
        void tick() {}
        void log(String message) { System.out.println("[" + nodeId + "] " + message); }
        void subscribe(String topic, Consumer<DslPacket> consumer) { bus.subscribe(topic, nodeId, consumer); }
        void publish(String topic, String target, String kind, String dsl, Map<String, Object> metrics, VirtualQos qos) {
            bus.publish(topic, new DslPacket(nodeId, target, kind, dsl, metrics, qos));
        }
    }

    private static final class PassengerCabinNode extends SimNode {
        private final String vehicleId;
        private final AtomicInteger round = new AtomicInteger();

        PassengerCabinNode(String nodeId, String vehicleId, VirtualDDSBus bus, VirtualWorld world) {
            super(nodeId, bus, world);
            this.vehicleId = vehicleId;
        }

        @Override
        void bindSubscriptions() {
            subscribe(TOPIC_FINAL_DECISION, packet -> {
                if (vehicleId.equals(packet.targetId) || "ALL".equals(packet.targetId)) {
                    log("passenger receives final explanation: " + packet.metrics.get("passengerExplanation"));
                }
            });
            subscribe(TOPIC_ROUTE_ADVICE, packet -> log("visual panel receives advice from=" + packet.sourceId
                + ", route=" + packet.metrics.get("routeCandidate")
                + ", risk=" + packet.metrics.get("riskScore")));
        }

        @Override
        void tick() {
            int current = round.incrementAndGet();
            String natural = choosePassengerSentence(current);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("schema_version", "3.0");
            fields.put("intent", "PASSENGER_TRAVEL_INTENT");
            fields.put("speaker", nodeId);
            fields.put("listener", vehicleId);
            fields.put("natural_language", natural);
            fields.put("destination", destinationOf(natural));
            fields.put("urgency", natural.contains("urgent") || natural.contains("rush") ? "HIGH" : "MEDIUM");
            fields.put("preference", natural.contains("safe") || natural.contains("stable") ? "SAFE_FIRST" : "TIME_FIRST");
            fields.put("expected_behavior", "vehicle_self_selects_dds_topics_and_collaborates");
            fields.put("requested_capabilities", "route_planning,uav_forward_perception,rsu_road_event_query,peer_vehicle_advice,compliant_backbone_context");
            fields.put("publish_topics", TOPIC_PASSENGER_INTENT);
            fields.put("subscribe_topics", TOPIC_FINAL_DECISION + "," + TOPIC_ROUTE_ADVICE);
            fields.put("dsl_contract", "dataExchangeContract,commandExchangeContract,ddsIdlBinding");
            fields.put("idl_type", "urban.MissionCommand");
            Map<String, Object> metrics = new LinkedHashMap<>(fields);
            metrics.put("messageKind", "PASSENGER_INTENT");
            metrics.put("targetId", vehicleId);
            publish(TOPIC_PASSENGER_INTENT, vehicleId, "PASSENGER_INTENT",
                buildPassengerIntentDsl("passenger_intent_" + current, fields), metrics, VirtualQos.reliable());
            log("passenger natural-language input: " + natural);
        }

        private String choosePassengerSentence(int current) {
            switch ((current - 1) % 3) {
                case 0:
                    return "I need to reach Southwest University Hospital urgently. If the main road is congested, ask the UAV to check ahead. Prefer safety but avoid a long detour.";
                case 1:
                    return "The road ahead looks crowded. Ask the peer vehicle, RSU, and UAV, then choose a more stable route.";
                default:
                    return "I need to rush to Jiangbei Airport T3. If the main road accident is serious, suggest a backup route, but keep safety first.";
            }
        }

        private String destinationOf(String natural) {
            return natural.contains("Airport") ? "Jiangbei Airport T3" : "Southwest University Hospital";
        }
    }

    private static final class AutonomousVehicleNode extends SimNode {
        private final boolean egoVehicle;
        private final Map<String, DslPacket> latestAdvice = new ConcurrentHashMap<>();
        private volatile DslPacket latestPassengerIntent;

        AutonomousVehicleNode(String nodeId, boolean egoVehicle, VirtualDDSBus bus, VirtualWorld world) {
            super(nodeId, bus, world);
            this.egoVehicle = egoVehicle;
        }

        @Override
        void bindSubscriptions() {
            subscribe(TOPIC_PASSENGER_INTENT, packet -> {
                if (egoVehicle && nodeId.equals(packet.targetId)) {
                    latestPassengerIntent = packet;
                    log("received passenger intent; self-selecting subscribe/publish topics: " + packet.metrics.get("natural_language"));
                    log("DSL contract parsed: subscribe=" + packet.metrics.get("subscribe_topics")
                        + ", publish=" + packet.metrics.get("publish_topics")
                        + ", idl=" + packet.metrics.get("idl_type"));
                    publishLocalRouteAdvice("ego_initial_plan", packet);
                } else if (!egoVehicle) {
                    publishPeerAdvice(packet);
                }
            });
            subscribe(TOPIC_ROUTE_ADVICE, packet -> {
                if (!packet.sourceId.equals(nodeId)) {
                    latestAdvice.put(packet.sourceId, packet);
                    if (egoVehicle && latestPassengerIntent != null) fuseAndPublishDecision("route_advice_fusion");
                }
            });
            subscribe(TOPIC_RSU_EVENT, packet -> {
                latestAdvice.put(packet.sourceId, packet);
                if (egoVehicle && latestPassengerIntent != null) fuseAndPublishDecision("rsu_event_update");
            });
            subscribe(TOPIC_UAV_PERCEPTION, packet -> {
                latestAdvice.put(packet.sourceId, packet);
                if (egoVehicle && latestPassengerIntent != null) fuseAndPublishDecision("uav_forward_perception");
            });
            subscribe(TOPIC_BACKBONE_CONTEXT, packet -> {
                latestAdvice.put(packet.sourceId, packet);
                if (egoVehicle && latestPassengerIntent != null) fuseAndPublishDecision("backbone_context_update");
            });
        }

        @Override
        void tick() {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("vehicleId", nodeId);
            metrics.put("role", egoVehicle ? "EGO_AUTONOMOUS_VEHICLE" : "PEER_AUTONOMOUS_VEHICLE");
            metrics.put("frame", world.frame);
            metrics.put("x", egoVehicle ? world.egoX : world.peerX);
            metrics.put("speedMps", egoVehicle ? 12.4 : 11.8);
            metrics.put("roadFriction", world.roadFriction);
            metrics.put("trafficSignal", world.trafficSignal);
            metrics.put("roadEvent", world.roadEvent);
            metrics.put("idlType", "urban.VehicleTelemetry");
            publish(TOPIC_VEHICLE_TELEMETRY, "ALL", "VEHICLE_TELEMETRY",
                buildDsl("telemetry_" + nodeId + "_" + world.frame, "vehicle_state_publish", metrics), metrics, VirtualQos.bestEffort());
        }

        private void publishPeerAdvice(DslPacket intentPacket) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("sourceType", "PEER_VEHICLE");
            metrics.put("destination", intentPacket.metrics.get("destination"));
            metrics.put("routeCandidate", world.roadEvent.contains("accident") ? "route_C_elevated_detour" : "route_A_main_road");
            metrics.put("riskScore", fmt(peerRisk()));
            metrics.put("confidence", fmt(0.76));
            metrics.put("advice", "peer_vehicle_suggests_gap_and_lane_availability");
            metrics.put("idlType", "urban.RouteAdvice");
            publish(TOPIC_ROUTE_ADVICE, "ALL", "ROUTE_ADVICE",
                buildDsl("peer_route_advice_" + world.frame, "peer_vehicle_route_advice", metrics), metrics, VirtualQos.reliable());
            publish(TOPIC_PEER_ADVICE, "CAR-CQ-001", "PEER_ADVICE",
                buildDsl("peer_dialogue_" + world.frame, "vehicle_to_vehicle_dialogue", metrics), metrics, VirtualQos.reliable());
            log("peer vehicle publishes advice: " + metrics.get("routeCandidate"));
        }

        private void publishLocalRouteAdvice(String reason, DslPacket intentPacket) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("sourceType", "EGO_VEHICLE");
            metrics.put("reason", reason);
            metrics.put("destination", intentPacket.metrics.get("destination"));
            metrics.put("routeCandidate", chooseLocalCandidate());
            metrics.put("riskScore", fmt(localRisk()));
            metrics.put("confidence", fmt(0.82));
            metrics.put("subscribeTopics", TOPIC_ROUTE_ADVICE + "," + TOPIC_UAV_PERCEPTION + "," + TOPIC_RSU_EVENT + "," + TOPIC_BACKBONE_CONTEXT);
            metrics.put("publishTopics", TOPIC_VEHICLE_TELEMETRY + "," + TOPIC_FINAL_DECISION);
            metrics.put("idlType", "urban.RouteAdvice");
            publish(TOPIC_ROUTE_ADVICE, "ALL", "ROUTE_ADVICE",
                buildDsl("ego_route_advice_" + world.frame, "vehicle_self_decision_advice", metrics), metrics, VirtualQos.reliable());
        }

        private void fuseAndPublishDecision(String trigger) {
            double risk = localRisk();
            String route = chooseLocalCandidate();
            for (DslPacket packet : latestAdvice.values()) {
                double packetRisk = toDouble(packet.metrics.get("riskScore"), 0.4);
                String candidate = String.valueOf(packet.metrics.getOrDefault("routeCandidate", route));
                if (packetRisk < risk || world.roadEvent.contains("accident")) {
                    route = candidate;
                    risk = Math.min(risk, packetRisk);
                }
            }
            if (world.roadEvent.contains("accident") && world.ambulanceLaneClearance > 0.8) {
                route = "route_D_emergency_green_wave";
                risk = Math.min(risk, 0.26);
            }
            if (world.roadFriction < 0.70) {
                route = route + "_low_speed_safe_profile";
                risk += 0.08;
            }
            world.finalRoute = route;
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("decisionOwner", nodeId);
            metrics.put("trigger", trigger);
            metrics.put("finalRoute", route);
            metrics.put("riskScore", fmt(Math.min(0.95, risk)));
            metrics.put("confidence", fmt(0.88));
            metrics.put("adviceSources", latestAdvice.keySet().toString());
            metrics.put("autonomyMode", "vehicle_self_decision_after_dds_consensus");
            metrics.put("passengerExplanation", "Fused UAV, peer vehicle, RSU, and compliant backbone context. Selected " + route + ", risk=" + fmt(risk));
            metrics.put("idlType", "urban.RouteDecision");
            publish(TOPIC_FINAL_DECISION, nodeId, "FINAL_ROUTE_DECISION",
                buildDsl("final_decision_" + world.frame, "route_decision_fusion", metrics), metrics, VirtualQos.reliable());
            log("fused multi-source advice and made autonomous route decision: route=" + route + ", trigger=" + trigger);
        }

        private String chooseLocalCandidate() {
            if (world.roadEvent.contains("accident")) return "route_C_elevated_detour";
            if (world.pedestrianCount > 0) return "route_B_school_zone_slow_safe";
            return "route_A_main_road";
        }

        private double localRisk() {
            double risk = 0.18;
            if (world.pedestrianCount > 0) risk += 0.18;
            if ("RED".equals(world.trafficSignal)) risk += 0.16;
            if (world.roadEvent.contains("accident")) risk += 0.32;
            if (world.roadFriction < 0.70) risk += 0.16;
            return clamp(risk, 0.05, 0.92);
        }

        private double peerRisk() {
            return clamp(0.22 + world.rsuLoad * 0.2 + ("RED".equals(world.trafficSignal) ? 0.12 : 0), 0.08, 0.88);
        }
    }

    private static final class AerialUavNode extends SimNode {
        AerialUavNode(String nodeId, VirtualDDSBus bus, VirtualWorld world) { super(nodeId, bus, world); }

        @Override
        void bindSubscriptions() {
            subscribe(TOPIC_PASSENGER_INTENT, packet -> publishAerialAdvice(packet, "intent_trigger"));
            subscribe(TOPIC_VEHICLE_TELEMETRY, packet -> {
                if ("CAR-CQ-001".equals(packet.sourceId) && world.frame % 3 == 0) publishAerialAdvice(packet, "vehicle_state_trigger");
            });
        }

        @Override
        void tick() {
            Map<String, Object> metrics = aerialMetrics("periodic_patrol");
            publish(TOPIC_UAV_PERCEPTION, "ALL", "UAV_PERCEPTION",
                buildDsl("uav_perception_" + world.frame, "aerial_perception_publish", metrics), metrics, VirtualQos.bestEffort());
        }

        private void publishAerialAdvice(DslPacket sourcePacket, String reason) {
            Map<String, Object> metrics = aerialMetrics(reason);
            metrics.put("destination", sourcePacket.metrics.getOrDefault("destination", "unknown"));
            metrics.put("routeCandidate", world.roadEvent.contains("accident") ? "route_C_elevated_detour" : "route_A_main_road");
            metrics.put("advice", "uav_forward_view_recommends_" + metrics.get("routeCandidate"));
            publish(TOPIC_ROUTE_ADVICE, "ALL", "ROUTE_ADVICE",
                buildDsl("uav_route_advice_" + world.frame, "aerial_route_advice", metrics), metrics, VirtualQos.reliable());
            log("UAV publishes aerial route advice: " + metrics.get("routeCandidate"));
        }

        private Map<String, Object> aerialMetrics(String reason) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("sourceType", "UAV");
            metrics.put("reason", reason);
            metrics.put("windSpeedMps", fmt(world.windSpeed));
            metrics.put("visibilityM", fmt(world.uavVisibility));
            metrics.put("roadEvent", world.roadEvent);
            metrics.put("riskScore", fmt(world.uavVisibility < 1800 ? 0.48 : 0.28));
            metrics.put("confidence", fmt(world.uavVisibility < 1800 ? 0.71 : 0.9));
            metrics.put("idlType", "urban.EnvironmentPerception");
            return metrics;
        }
    }

    private static final class RoadSideUnitNode extends SimNode {
        RoadSideUnitNode(String nodeId, VirtualDDSBus bus, VirtualWorld world) { super(nodeId, bus, world); }
        @Override
        void bindSubscriptions() { subscribe(TOPIC_PASSENGER_INTENT, packet -> publishRoadEvent("intent_related_query")); }
        @Override
        void tick() { publishRoadEvent("periodic_v2x_broadcast"); }
        private void publishRoadEvent(String reason) {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("sourceType", "RSU");
            metrics.put("reason", reason);
            metrics.put("roadEvent", world.roadEvent);
            metrics.put("trafficSignal", world.trafficSignal);
            metrics.put("pedestrianCount", world.pedestrianCount);
            metrics.put("rsuLoad", fmt(world.rsuLoad));
            metrics.put("routeCandidate", world.roadEvent.contains("accident") ? "route_D_emergency_green_wave" : "route_B_school_zone_slow_safe");
            metrics.put("riskScore", fmt(world.pedestrianCount > 0 ? 0.44 : 0.24));
            metrics.put("confidence", fmt(0.86));
            metrics.put("idlType", "urban.RsuRoadEvent");
            publish(TOPIC_RSU_EVENT, "ALL", "RSU_ROAD_EVENT",
                buildDsl("rsu_event_" + world.frame, "road_side_event_publish", metrics), metrics, VirtualQos.reliable());
        }
    }

    private static final class CompliantBackboneNode extends SimNode {
        CompliantBackboneNode(String nodeId, VirtualDDSBus bus, VirtualWorld world) { super(nodeId, bus, world); }
        @Override
        void bindSubscriptions() {
            subscribe(TOPIC_ROUTE_ADVICE, packet -> {
                if (!nodeId.equals(packet.sourceId)) log("wide-area context fusion includes from=" + packet.sourceId + ", type=" + packet.metrics.get("sourceType"));
            });
        }
        @Override
        void tick() {
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("sourceType", "COMPLIANT_BACKBONE");
            metrics.put("linkMode", "approved_satcom_or_5g_v2x_or_industry_private_network");
            metrics.put("private5gSignal", fmt(world.private5gSignal));
            metrics.put("rsuLoad", fmt(world.rsuLoad));
            metrics.put("linkHealth", fmt(clamp(world.private5gSignal * 0.7 + (1.0 - world.rsuLoad) * 0.3, 0.45, 0.99)));
            metrics.put("routeCandidate", world.rsuLoad > 0.72 ? "route_C_elevated_detour" : "route_A_main_road");
            metrics.put("riskScore", fmt(world.rsuLoad > 0.72 ? 0.36 : 0.2));
            metrics.put("confidence", fmt(0.8));
            metrics.put("idlType", "urban.CompliantNetworkContext");
            publish(TOPIC_BACKBONE_CONTEXT, "ALL", "COMPLIANT_CONTEXT",
                buildDsl("backbone_context_" + world.frame, "compliant_wide_area_context", metrics), metrics, VirtualQos.reliable());
        }
    }

    private static final class DslPacket {
        final String sourceId;
        final String targetId;
        final String kind;
        final String dsl;
        final Map<String, Object> metrics;
        final VirtualQos qos;
        volatile String topic;
        volatile long sequence;
        volatile long publishedAtMillis;
        DslPacket(String sourceId, String targetId, String kind, String dsl, Map<String, Object> metrics, VirtualQos qos) {
            this.sourceId = sourceId;
            this.targetId = targetId == null ? "ALL" : targetId;
            this.kind = kind;
            this.dsl = dsl;
            this.metrics = metrics;
            this.qos = qos;
        }
    }

    private static final class VirtualQos {
        final String profile;
        final boolean reliable;
        final long ttlMillis;
        final long retryDelayMillis;
        private VirtualQos(String profile, boolean reliable, long ttlMillis, long retryDelayMillis) {
            this.profile = profile;
            this.reliable = reliable;
            this.ttlMillis = ttlMillis;
            this.retryDelayMillis = retryDelayMillis;
        }
        static VirtualQos reliable() { return new VirtualQos("reliable_keep_last_10_deadline_300ms", true, 2500, 90); }
        static VirtualQos bestEffort() { return new VirtualQos("best_effort_sensor_stream", false, 900, 0); }
    }

    private static String buildDsl(String id, String type, Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("mission ").append(id).append(" {\n");
        sb.append("  type: ").append(type).append("\n");
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String buildPassengerIntentDsl(String id, Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("mission ").append(id).append(" {\n");
        sb.append("  type: natural_language_to_vehicle_collaboration_contract\n");
        sb.append("  natural_language: \"").append(escapeDsl(fields.get("natural_language"))).append("\"\n");
        sb.append("  destination: \"").append(escapeDsl(fields.get("destination"))).append("\"\n");
        sb.append("  urgency: ").append(fields.get("urgency")).append("\n");
        sb.append("  preference: ").append(fields.get("preference")).append("\n");
        sb.append("  expected_behavior: vehicle_self_selects_dds_topics_and_collaborates\n");
        sb.append("\n");
        sb.append("  dataExchangeContract {\n");
        sb.append("    publish: ").append(TOPIC_PASSENGER_INTENT).append(" -> urban.MissionCommand\n");
        sb.append("    subscribe: ").append(TOPIC_ROUTE_ADVICE).append(" -> urban.RouteAdvice\n");
        sb.append("    subscribe: ").append(TOPIC_FINAL_DECISION).append(" -> urban.RouteDecision\n");
        sb.append("    required_fields: natural_language,destination,urgency,preference,requested_capabilities\n");
        sb.append("  }\n");
        sb.append("\n");
        sb.append("  commandExchangeContract {\n");
        sb.append("    intent: request_autonomous_route_collaboration\n");
        sb.append("    allowed_commands: ask_peer_vehicle,ask_rsu,ask_uav,fuse_advice,publish_final_decision\n");
        sb.append("    forbidden_commands: remote_drive_control,central_dispatch_override\n");
        sb.append("    decision_owner: CAR-CQ-001\n");
        sb.append("  }\n");
        sb.append("\n");
        sb.append("  ddsIdlBinding {\n");
        sb.append("    topic ").append(TOPIC_PASSENGER_INTENT).append(" uses urban.MissionCommand reliable_keep_last_10_deadline_300ms\n");
        sb.append("    topic ").append(TOPIC_VEHICLE_TELEMETRY).append(" uses urban.VehicleTelemetry best_effort_sensor_stream\n");
        sb.append("    topic ").append(TOPIC_UAV_PERCEPTION).append(" uses urban.EnvironmentPerception best_effort_sensor_stream\n");
        sb.append("    topic ").append(TOPIC_RSU_EVENT).append(" uses urban.RsuRoadEvent reliable_keep_last_10_deadline_300ms\n");
        sb.append("    topic ").append(TOPIC_BACKBONE_CONTEXT).append(" uses urban.CompliantNetworkContext reliable_keep_last_10_deadline_300ms\n");
        sb.append("    topic ").append(TOPIC_ROUTE_ADVICE).append(" uses urban.RouteAdvice reliable_keep_last_10_deadline_300ms\n");
        sb.append("    topic ").append(TOPIC_FINAL_DECISION).append(" uses urban.RouteDecision reliable_keep_last_10_deadline_300ms\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeDsl(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static double toDouble(Object value, double fallback) {
        if (value == null) return fallback;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static String fmt(double value) { return String.format(Locale.ROOT, "%.2f", value); }
}

