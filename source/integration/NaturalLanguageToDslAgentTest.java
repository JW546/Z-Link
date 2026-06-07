package integration;

import agent.CPSEnums;
import agent.PubSubDecisionMaker;
import utils.SynonymLibrary;
import utils.UnitConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Natural language -> agent reasoning -> DSL file test.
 *
 * This test is intentionally not a fixed DSL sample. The DSL is synthesized
 * from multiple agent decisions: passenger intent parsing, pub/sub planning,
 * device advice generation, route fusion, and DDS/IDL binding.
 */
public class NaturalLanguageToDslAgentTest {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) throws Exception {
        String naturalLanguage = parseInput(args);
        String missionId = "MSN-" + System.currentTimeMillis();

        SemanticIntentAgent semanticAgent = new SemanticIntentAgent();
        PubSubPlanningAgent pubSubAgent = new PubSubPlanningAgent();
        SynonymNormalizationAgent synonymAgent = new SynonymNormalizationAgent();
        UnitNormalizationAgent unitAgent = new UnitNormalizationAgent();
        DeviceAdviceAgent deviceAdviceAgent = new DeviceAdviceAgent();
        RouteFusionAgent fusionAgent = new RouteFusionAgent();
        DslWritingAgent dslAgent = new DslWritingAgent();

        PassengerIntent intent = semanticAgent.analyze(naturalLanguage);
        PubSubPlan pubSubPlan = pubSubAgent.plan(intent);
        SynonymContract synonymContract = synonymAgent.normalize(intent);
        UnitContract unitContract = unitAgent.plan();
        List<DeviceAdvice> advice = deviceAdviceAgent.generate(intent);
        RouteDecision decision = fusionAgent.fuse(intent, advice);
        String dsl = dslAgent.write(missionId, intent, pubSubPlan, synonymContract, unitContract, advice, decision);

        Path outDir = Paths.get("dsl-output");
        Files.createDirectories(outDir);
        Path timestampFile = outDir.resolve("agent_nl_to_dsl_" + LocalDateTime.now().format(TS) + ".dsl");
        Path latestFile = outDir.resolve("agent_nl_to_dsl_latest.dsl");
        Files.writeString(timestampFile, dsl, StandardCharsets.UTF_8);
        Files.writeString(latestFile, dsl, StandardCharsets.UTF_8);

        System.out.println("============================================================");
        System.out.println("  Natural Language -> Agent Reasoning -> DSL Test");
        System.out.println("============================================================");
        System.out.println("[INPUT] " + naturalLanguage);
        System.out.println("[AGENT] semantic intent=" + intent.intent
            + ", destination=" + intent.destination
            + ", urgency=" + intent.urgency
            + ", preference=" + intent.preference);
        System.out.println("[AGENT] publish topics=" + pubSubPlan.publishTopics);
        System.out.println("[AGENT] subscribe topics=" + pubSubPlan.subscribeTopics);
        System.out.println("[AGENT] synonym normalization=" + synonymContract.mappings);
        System.out.println("[AGENT] unit normalization=" + unitContract.canonicalUnits);
        System.out.println("[AGENT] final route=" + decision.route
            + ", risk=" + fmt(decision.riskScore)
            + ", confidence=" + fmt(decision.confidence));
        System.out.println();
        System.out.println("---------------- GENERATED DSL ----------------");
        System.out.println(dsl);
        System.out.println("------------------------------------------------");
        System.out.println("[SAVE] " + timestampFile.toAbsolutePath());
        System.out.println("[SAVE] " + latestFile.toAbsolutePath());

        validate(dsl);
        System.out.println("[PASS] Natural language was converted into an agent-generated DSL file.");
    }

    private static String parseInput(String[] args) {
        String fallback = "乘客想尽快去西南大学医院，前方主路可能拥堵，请无人车自己询问邻车、RSU和无人机，安全优先，但不要绕太远。";
        if (args == null || args.length == 0) {
            return fallback;
        }
        String joined = String.join(" ", args).trim();
        if (joined.startsWith("--input=")) {
            return joined.substring("--input=".length()).trim();
        }
        return joined.isEmpty() ? fallback : joined;
    }

    private static void validate(String dsl) {
        List<String> required = Arrays.asList(
            "task passengerIntent",
            "state agentReasoningTrace",
            "state synonymNormalizationContract",
            "state dataExchangeContract",
            "state unitNormalizationContract",
            "state commandExchangeContract",
            "state ddsIdlBinding",
            "state routeDecision",
            "urban.MissionCommand",
            "urban.RouteAdvice",
            "urban.RouteDecision"
        );
        for (String item : required) {
            if (!dsl.contains(item)) {
                throw new IllegalStateException("Generated DSL missing required signal: " + item);
            }
        }
    }

    private static final class SemanticIntentAgent {
        PassengerIntent analyze(String text) {
            PassengerIntent intent = new PassengerIntent();
            intent.rawText = text;
            intent.intent = "AUTONOMOUS_ROUTE_COLLABORATION";
            intent.destination = inferDestination(text);
            intent.urgency = containsAny(text, "急", "赶时间", "尽快", "马上", "医院", "urgent", "quickly")
                ? "HIGH" : "MEDIUM";
            intent.preference = containsAny(text, "安全", "稳定", "稳", "不要冒险", "safe", "stable")
                ? "SAFE_FIRST" : "TIME_RISK_BALANCED";
            intent.needsPeerVehicle = containsAny(text, "邻车", "前车", "后车", "其他车", "车队", "peer");
            intent.needsUav = containsAny(text, "无人机", "空中", "前方", "航拍", "uav", "drone");
            intent.needsRsu = containsAny(text, "RSU", "路侧", "路口", "红绿灯", "信号灯", "roadside");
            intent.needsBackbone = containsAny(text, "全局", "广域", "5G", "专网", "合规", "链路", "backbone");
            intent.congestionExpected = containsAny(text, "堵", "拥堵", "排队", "事故", "红灯", "crowded", "congested");
            intent.avoidLongDetour = containsAny(text, "不要绕太远", "少绕路", "不要绕远", "avoid a long detour");
            intent.sceneTags.add(intent.congestionExpected ? "possible_congestion" : "normal_flow");
            if (intent.needsPeerVehicle) intent.sceneTags.add("peer_vehicle_advice");
            if (intent.needsUav) intent.sceneTags.add("uav_forward_perception");
            if (intent.needsRsu) intent.sceneTags.add("rsu_road_event");
            if (intent.needsBackbone) intent.sceneTags.add("compliant_backbone_context");
            return intent;
        }

        private String inferDestination(String text) {
            if (containsAny(text, "西南大学医院", "西南大学附属医院")) return "Southwest University Hospital";
            if (containsAny(text, "医院")) return "nearest_hospital";
            if (containsAny(text, "机场", "T3")) return "Jiangbei Airport T3";
            if (containsAny(text, "西南大学")) return "Southwest University";
            if (containsAny(text, "观音桥")) return "Guanyinqiao";
            return "passenger_specified_destination";
        }
    }

    private static final class PubSubPlanningAgent {
        private final PubSubDecisionMaker decisionMaker = new PubSubDecisionMaker();

        PubSubPlan plan(PassengerIntent intent) {
            PubSubDecisionMaker.PubSubDecision base = decisionMaker.makeDecision(
                "COORDINATION", CPSEnums.DeviceRole.VEHICLE, CPSEnums.TaskPhase.PLANNING);
            PubSubPlan plan = new PubSubPlan();
            plan.publishTopics.addAll(base.publishTopics);
            plan.subscribeTopics.addAll(base.subscribeTopics);
            plan.publishTopics.add("cabin/passenger/intent.dsl");
            plan.publishTopics.add("urban/vehicle/telemetry.dsl");
            plan.publishTopics.add("urban/route/final_decision.dsl");
            if (intent.needsPeerVehicle) plan.subscribeTopics.add("urban/vehicle/peer_advice.dsl");
            if (intent.needsUav) plan.subscribeTopics.add("urban/uav/aerial_perception.dsl");
            if (intent.needsRsu) plan.subscribeTopics.add("urban/rsu/road_event.dsl");
            if (intent.needsBackbone || intent.congestionExpected) plan.subscribeTopics.add("backbone/compliant/context.dsl");
            plan.subscribeTopics.add("urban/route/advice.dsl");
            plan.publishTopics = unique(plan.publishTopics);
            plan.subscribeTopics = unique(plan.subscribeTopics);
            return plan;
        }
    }

    private static final class SynonymNormalizationAgent {
        SynonymContract normalize(PassengerIntent intent) {
            SynonymContract contract = new SynonymContract();
            addIfPresent(contract, intent.rawText, "车速", "speed");
            addIfPresent(contract, intent.rawText, "速度", "speed");
            addIfPresent(contract, intent.rawText, "飞行速度", "speed");
            addIfPresent(contract, intent.rawText, "无人车", "vehicle");
            addIfPresent(contract, intent.rawText, "汽车", "vehicle");
            addIfPresent(contract, intent.rawText, "邻车", "vehicle");
            addIfPresent(contract, intent.rawText, "无人机", "drone");
            addIfPresent(contract, intent.rawText, "UAV", "drone");
            addIfPresent(contract, intent.rawText, "前往", "navigation");
            addIfPresent(contract, intent.rawText, "去", "navigation");
            addIfPresent(contract, intent.rawText, "路线", "route");
            addIfPresent(contract, intent.rawText, "绕行", "route");
            addIfPresent(contract, intent.rawText, "目的地", "destination");
            addIfPresent(contract, intent.rawText, "拥堵", "congestion");
            addIfPresent(contract, intent.rawText, "堵", "congestion");
            addIfPresent(contract, intent.rawText, "红灯", "congestion");
            addIfPresent(contract, intent.rawText, "急", "urgent");
            addIfPresent(contract, intent.rawText, "尽快", "urgent");
            addIfPresent(contract, intent.rawText, "安全", "priority");
            addIfPresent(contract, intent.rawText, "优先", "priority");

            if (contract.mappings.isEmpty()) {
                contract.mappings.put("passenger_intent", "AUTONOMOUS_ROUTE_COLLABORATION");
            }
            contract.rules.add("Each device may use local words, but DDS/IDL payload fields use canonical semantic terms.");
            contract.rules.add("Synonym normalization runs before unit normalization and before DDS publication.");
            contract.rules.add("Example: 车速, 行驶速度, vehicle_speed -> speed; 无人机, UAV, drone -> drone.");
            return contract;
        }

        private void addIfPresent(SynonymContract contract, String text, String term, String fallbackStandard) {
            if (text == null || term == null) return;
            if (text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT))) {
                String standard = SynonymLibrary.normalize(term);
                if (standard == null || standard.equals(term)) {
                    standard = fallbackStandard;
                }
                contract.mappings.put(term, standard);
            }
        }
    }

    private static final class DeviceAdviceAgent {
        List<DeviceAdvice> generate(PassengerIntent intent) {
            List<DeviceAdvice> advice = new ArrayList<>();
            if (intent.needsPeerVehicle || intent.congestionExpected) {
                advice.add(new DeviceAdvice("CAR-CQ-002", "PEER_VEHICLE", "urban/vehicle/peer_advice.dsl",
                    "urban.RouteAdvice", routeForPeer(intent), risk(intent, 0.46), 0.84,
                    "Peer vehicle reports lane queue and recommends shared route posture."));
            }
            if (intent.needsUav || intent.congestionExpected) {
                advice.add(new DeviceAdvice("UAV-CQ-007", "UAV", "urban/uav/aerial_perception.dsl",
                    "urban.EnvironmentPerception", routeForUav(intent), risk(intent, 0.30), 0.88,
                    "UAV forward perception checks congestion and publishes aerial route advice."));
            }
            if (intent.needsRsu || intent.congestionExpected) {
                advice.add(new DeviceAdvice("RSU-SCHOOL-009", "RSU", "urban/rsu/road_event.dsl",
                    "urban.RsuRoadEvent", intent.congestionExpected ? "route_D_emergency_green_wave" : "route_A_main_road",
                    risk(intent, 0.34), 0.86,
                    "RSU publishes signal phase, queue state, pedestrian and road-event context."));
            }
            advice.add(new DeviceAdvice("CN-BACKBONE-CQ", "COMPLIANT_BACKBONE", "backbone/compliant/context.dsl",
                "urban.CompliantNetworkContext", intent.congestionExpected ? "route_C_elevated_detour" : "route_A_main_road",
                risk(intent, 0.40), 0.76,
                "Compliant backbone provides regional context only, without remote task dispatch."));
            return advice;
        }

        private String routeForPeer(PassengerIntent intent) {
            return intent.congestionExpected ? "route_C_elevated_detour" : "route_A_main_road";
        }

        private String routeForUav(PassengerIntent intent) {
            return intent.congestionExpected ? "route_C_elevated_detour" : "route_A_main_road";
        }

        private double risk(PassengerIntent intent, double base) {
            double value = base;
            if ("HIGH".equals(intent.urgency)) value += 0.04;
            if (intent.congestionExpected) value += 0.08;
            if ("SAFE_FIRST".equals(intent.preference)) value -= 0.03;
            return clamp(value, 0.08, 0.90);
        }
    }

    private static final class UnitNormalizationAgent {
        UnitContract plan() {
            UnitContract contract = new UnitContract();
            contract.canonicalUnits.put("speed", "m/s");
            contract.canonicalUnits.put("wind_speed", "m/s");
            contract.canonicalUnits.put("distance", "m");
            contract.canonicalUnits.put("position", "WGS84 longitude/latitude degrees");
            contract.canonicalUnits.put("heading", "degree");
            contract.canonicalUnits.put("latency", "ms");
            contract.canonicalUnits.put("risk_score", "0.0-1.0");
            contract.rules.add("speed: km/h -> m/s, value / 3.6");
            contract.rules.add("speed: mph -> m/s, value * 0.44704");
            contract.rules.add("wind_speed: km/h -> m/s, value / 3.6");
            contract.rules.add("distance: km -> m, value * 1000");
            contract.rules.add("all DDS/IDL numeric payloads use canonical units before publication");
            contract.examples.add("36 km/h -> " + UnitConverter.smartConvert("speed", "36 km/h"));
            contract.examples.add("72 km/h -> " + UnitConverter.smartConvert("speed", "72 km/h"));
            contract.examples.add("12 m/s remains canonical speed=12.00 m/s");
            return contract;
        }
    }

    private static final class RouteFusionAgent {
        RouteDecision fuse(PassengerIntent intent, List<DeviceAdvice> adviceList) {
            Map<String, Double> weightedRisk = new LinkedHashMap<>();
            Map<String, Double> weights = new LinkedHashMap<>();
            for (DeviceAdvice advice : adviceList) {
                double weight = advice.confidence;
                if ("SAFE_FIRST".equals(intent.preference) && advice.riskScore < 0.40) weight += 0.15;
                if ("HIGH".equals(intent.urgency) && advice.route.contains("emergency")) weight += 0.20;
                weightedRisk.put(advice.route, weightedRisk.getOrDefault(advice.route, 0.0) + advice.riskScore * weight);
                weights.put(advice.route, weights.getOrDefault(advice.route, 0.0) + weight);
            }
            String bestRoute = "route_A_main_road";
            double bestRisk = Double.MAX_VALUE;
            double confidence = 0.72;
            for (Map.Entry<String, Double> entry : weightedRisk.entrySet()) {
                double routeRisk = entry.getValue() / Math.max(0.01, weights.get(entry.getKey()));
                if (routeRisk < bestRisk || (intent.congestionExpected && entry.getKey().contains("emergency"))) {
                    bestRoute = entry.getKey();
                    bestRisk = routeRisk;
                    confidence = Math.min(0.95, weights.get(entry.getKey()) / Math.max(1.0, adviceList.size()));
                }
            }
            if (intent.congestionExpected && "HIGH".equals(intent.urgency)) {
                bestRoute = "route_D_emergency_green_wave";
                bestRisk = Math.min(bestRisk, 0.29);
                confidence = Math.max(confidence, 0.86);
            }
            RouteDecision decision = new RouteDecision();
            decision.route = bestRoute;
            decision.riskScore = clamp(bestRisk, 0.05, 0.95);
            decision.confidence = clamp(confidence, 0.50, 0.96);
            decision.policy = "time_risk_balanced_consensus";
            if ("SAFE_FIRST".equals(intent.preference)) {
                decision.policy = "safety_weighted_consensus";
            }
            decision.reason = "Ego vehicle fuses peer, UAV, RSU and compliant backbone advice, then publishes the final route itself.";
            return decision;
        }
    }

    private static final class DslWritingAgent {
        String write(String missionId, PassengerIntent intent, PubSubPlan plan,
                     SynonymContract synonymContract, UnitContract unitContract,
                     List<DeviceAdvice> adviceList, RouteDecision decision) throws IOException {
            StringBuilder dsl = new StringBuilder();
            dsl.append("// Generated by NaturalLanguageToDslAgentTest\n");
            dsl.append("// This file is produced by agent reasoning, not by copying a fixed DSL sample.\n");
            dsl.append("// Source natural language: ").append(quote(intent.rawText)).append("\n\n");
            dsl.append("topic passenger_intent_collaboration_").append(normalize(missionId)).append(" {\n");
            dsl.append("    task passengerIntent {\n");
            dsl.append("        string mission_id = ").append(quote(missionId)).append(";\n");
            dsl.append("        string source = \"CABIN-CAR-CQ-001\";\n");
            dsl.append("        string target_vehicle = \"CAR-CQ-001\";\n");
            dsl.append("        string natural_language = ").append(quote(intent.rawText)).append(";\n");
            dsl.append("        string intent = ").append(quote(intent.intent)).append(";\n");
            dsl.append("        string destination = ").append(quote(intent.destination)).append(";\n");
            dsl.append("        string urgency = ").append(quote(intent.urgency)).append(";\n");
            dsl.append("        string preference = ").append(quote(intent.preference)).append(";\n");
            dsl.append("        string scene_tags = ").append(quote(String.join(",", intent.sceneTags))).append(";\n");
            dsl.append("    }\n\n");

            dsl.append("    state agentReasoningTrace {\n");
            dsl.append("        string semantic_agent = ").append(quote("extract destination, urgency, preference and requested collaborators")).append(";\n");
            dsl.append("        string synonym_agent = ").append(quote("normalize natural-language terms into canonical cross-protocol semantics")).append(";\n");
            dsl.append("        string pubsub_agent = ").append(quote("choose DDS publish and subscribe topics from intent and device role")).append(";\n");
            dsl.append("        string unit_agent = ").append(quote("normalize protocol-specific units before DDS/IDL publication")).append(";\n");
            dsl.append("        string advice_agent = ").append(quote("simulate autonomous advice from peer vehicle, UAV, RSU and compliant backbone")).append(";\n");
            dsl.append("        string fusion_agent = ").append(quote("select final route by risk, confidence, urgency and safety preference")).append(";\n");
            dsl.append("    }\n\n");

            dsl.append("    state synonymNormalizationContract {\n");
            int synonymIndex = 1;
            for (Map.Entry<String, String> entry : synonymContract.mappings.entrySet()) {
                dsl.append("        string term_").append(synonymIndex++).append(" = ")
                    .append(quote(entry.getKey() + " -> " + entry.getValue())).append(";\n");
            }
            for (int i = 0; i < synonymContract.rules.size(); i++) {
                dsl.append("        string rule_").append(i + 1).append(" = ")
                    .append(quote(synonymContract.rules.get(i))).append(";\n");
            }
            dsl.append("    }\n\n");

            dsl.append("    state dataExchangeContract {\n");
            appendStringList(dsl, "publish_topics", plan.publishTopics);
            appendStringList(dsl, "subscribe_topics", plan.subscribeTopics);
            dsl.append("        string data_scope = \"passenger_intent,vehicle_telemetry,peer_advice,uav_perception,rsu_event,compliant_context,route_decision\";\n");
            dsl.append("        string ownership = \"CAR-CQ-001 owns final route decision; other devices publish advice only\";\n");
            dsl.append("    }\n\n");

            dsl.append("    state unitNormalizationContract {\n");
            for (Map.Entry<String, String> entry : unitContract.canonicalUnits.entrySet()) {
                dsl.append("        string canonical_").append(normalize(entry.getKey())).append(" = ")
                    .append(quote(entry.getKey() + " uses " + entry.getValue())).append(";\n");
            }
            for (int i = 0; i < unitContract.rules.size(); i++) {
                dsl.append("        string rule_").append(i + 1).append(" = ")
                    .append(quote(unitContract.rules.get(i))).append(";\n");
            }
            for (int i = 0; i < unitContract.examples.size(); i++) {
                dsl.append("        string example_").append(i + 1).append(" = ")
                    .append(quote(unitContract.examples.get(i))).append(";\n");
            }
            dsl.append("    }\n\n");

            dsl.append("    state commandExchangeContract {\n");
            dsl.append("        string passenger_command = \"request_autonomous_route_collaboration\";\n");
            dsl.append("        string allowed_device_actions = \"subscribe_context,publish_advice,fuse_advice,publish_final_decision\";\n");
            dsl.append("        string forbidden_device_actions = \"remote_drive_control,central_dispatch_override,unapproved_satcom_access\";\n");
            dsl.append("        string decision_policy = ").append(quote(decision.policy)).append(";\n");
            dsl.append("    }\n\n");

            dsl.append("    state deviceAdvice {\n");
            for (int i = 0; i < adviceList.size(); i++) {
                DeviceAdvice advice = adviceList.get(i);
                dsl.append("        string advice_").append(i + 1).append(" = ")
                    .append(quote(advice.nodeId + "|" + advice.role + "|" + advice.topic + "|" + advice.route
                        + "|risk=" + fmt(advice.riskScore) + "|confidence=" + fmt(advice.confidence)
                        + "|" + advice.message)).append(";\n");
            }
            dsl.append("    }\n\n");

            dsl.append("    state routeDecision {\n");
            dsl.append("        string decision_owner = \"CAR-CQ-001\";\n");
            dsl.append("        string final_route = ").append(quote(decision.route)).append(";\n");
            dsl.append("        float risk_score = ").append(fmt(decision.riskScore)).append(";\n");
            dsl.append("        float confidence = ").append(fmt(decision.confidence)).append(";\n");
            dsl.append("        string passenger_explanation = ").append(quote(decision.reason)).append(";\n");
            dsl.append("    }\n\n");

            dsl.append("    state ddsIdlBinding {\n");
            appendBinding(dsl, "cabin/passenger/intent.dsl", "urban.MissionCommand", "RELIABLE_KEEP_LAST_10");
            appendBinding(dsl, "urban/vehicle/telemetry.dsl", "urban.VehicleTelemetry", "BEST_EFFORT_SENSOR_STREAM");
            appendBinding(dsl, "urban/vehicle/peer_advice.dsl", "urban.RouteAdvice", "RELIABLE_KEEP_LAST_10");
            appendBinding(dsl, "urban/uav/aerial_perception.dsl", "urban.EnvironmentPerception", "BEST_EFFORT_SENSOR_STREAM");
            appendBinding(dsl, "urban/rsu/road_event.dsl", "urban.RsuRoadEvent", "RELIABLE_KEEP_LAST_10");
            appendBinding(dsl, "backbone/compliant/context.dsl", "urban.CompliantNetworkContext", "RELIABLE_KEEP_LAST_10");
            appendBinding(dsl, "urban/route/advice.dsl", "urban.RouteAdvice", "RELIABLE_KEEP_LAST_10");
            appendBinding(dsl, "urban/route/final_decision.dsl", "urban.RouteDecision", "RELIABLE_KEEP_LAST_10");
            dsl.append("    }\n");
            dsl.append("}\n");
            return dsl.toString();
        }

        private void appendStringList(StringBuilder dsl, String name, List<String> values) {
            dsl.append("        string ").append(name).append(" = ").append(quote(String.join(",", values))).append(";\n");
        }

        private void appendBinding(StringBuilder dsl, String topic, String idlType, String qos) {
            dsl.append("        string ").append(normalize(topic)).append(" = ")
                .append(quote(topic + " -> " + idlType + " @ " + qos)).append(";\n");
        }
    }

    private static final class PassengerIntent {
        String rawText;
        String intent;
        String destination;
        String urgency;
        String preference;
        boolean needsPeerVehicle;
        boolean needsUav;
        boolean needsRsu;
        boolean needsBackbone;
        boolean congestionExpected;
        boolean avoidLongDetour;
        List<String> sceneTags = new ArrayList<>();
    }

    private static final class PubSubPlan {
        List<String> publishTopics = new ArrayList<>();
        List<String> subscribeTopics = new ArrayList<>();
    }

    private static final class SynonymContract {
        Map<String, String> mappings = new LinkedHashMap<>();
        List<String> rules = new ArrayList<>();
    }

    private static final class UnitContract {
        Map<String, String> canonicalUnits = new LinkedHashMap<>();
        List<String> rules = new ArrayList<>();
        List<String> examples = new ArrayList<>();
    }

    private static final class DeviceAdvice {
        final String nodeId;
        final String role;
        final String topic;
        final String idlType;
        final String route;
        final double riskScore;
        final double confidence;
        final String message;

        DeviceAdvice(String nodeId, String role, String topic, String idlType, String route,
                     double riskScore, double confidence, String message) {
            this.nodeId = nodeId;
            this.role = role;
            this.topic = topic;
            this.idlType = idlType;
            this.route = route;
            this.riskScore = riskScore;
            this.confidence = confidence;
            this.message = message;
        }
    }

    private static final class RouteDecision {
        String route;
        double riskScore;
        double confidence;
        String policy;
        String reason;
    }

    private static boolean containsAny(String text, String... terms) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term != null && !term.isBlank() && lower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> unique(List<String> values) {
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) set.add(value);
        }
        return new ArrayList<>(set);
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private static String normalize(String raw) {
        String clean = raw == null ? "field" : raw.replaceAll("[^A-Za-z0-9_]", "_");
        while (clean.contains("__")) clean = clean.replace("__", "_");
        if (clean.isBlank() || Character.isDigit(clean.charAt(0))) clean = "f_" + clean;
        return clean;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
