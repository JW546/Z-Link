package nlu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import utils.AmapRealAPI;

/**
 * 多意图 TCL 代码生成器。
 *
 * 设计目标：
 * 1. 不再依赖单一导航模板。
 * 2. 不使用 @ 注解语法，采用统一 task/path/state 契约。
 * 3. 支持 NAVIGATION、DELIVERY、COORDINATION、EMERGENCY、QUERY_STATUS、CONTROL 等意图。
 */
public class DSLGenerator {

    private static final String TOPIC_NAME_PREFIX = "MissionContract";
    private static final String TASK_NAME = "intentContract";
    private static final String STATE_NAME = "runtimeContext";
    private static final String FLOW_NAME = "collaborationFlow";
    private static final String PATH_NAME = "routePlan";
    private static final String DSL_SCHEMA_VERSION = "2.0";

    private static AmapRealAPI amapService;

    private static String DEFAULT_ORIGIN = "重庆市江北区观音桥";
    private static String DEFAULT_ORIGIN_LOCATION = "106.5516,29.5630";

    private static AmapRealAPI getAmapService() {
        if (amapService == null) {
            String apiKey = System.getenv("AMAP_API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                amapService = new AmapRealAPI(apiKey);
            } else {
                amapService = new AmapRealAPI();
            }
        }
        return amapService;
    }

    public static void setDefaultOrigin(String origin) {
        DEFAULT_ORIGIN = origin;
    }

    public static void setDefaultOriginCoords(String coords) {
        DEFAULT_ORIGIN_LOCATION = coords;
    }

    public static String getDefaultOrigin() {
        return DEFAULT_ORIGIN;
    }

    public String generateDSL(NLUResult result) {
        if (result == null) {
            return "// [提示] NLU结果为空，无法生成TCL\n";
        }

        Intent intent = result.getPrimaryIntent() != null ? result.getPrimaryIntent() : Intent.QUERY_STATUS;
        String destination = safe(result.getDestination(), "none");
        String originalText = safe(result.getOriginalText(), "");
        String topicName = generateTopicName(intent, result.getDestination());

        StringBuilder dsl = new StringBuilder();
        dsl.append("// ==================================================================\n");
        dsl.append("// Traffic Communication Language Contract\n");
        dsl.append("// 原始输入: \"").append(escape(originalText)).append("\"\n");
        dsl.append("// 生成时间: ").append(getCurrentTime()).append("\n");
        dsl.append("// 主意图: ").append(intent).append("\n");
        dsl.append("// 置信度: ").append(String.format(Locale.ROOT, "%.1f%%", result.getConfidence() * 100)).append("\n");
        dsl.append("// 目标: ").append(destination).append("\n");
        dsl.append("// ==================================================================\n\n");

        dsl.append("topic ").append(topicName).append(" {\n");
        dsl.append(generateIntentContractTask(result, intent));
        if (needsPath(intent, result)) {
            dsl.append(generateRoutePath(result));
        }
        dsl.append(generateRuntimeState(result, intent));
        dsl.append(generateFlowState(result, intent));
        dsl.append(generateDataExchangeContract(result, intent));
        dsl.append(generateCommandExchangeContract(result, intent));
        dsl.append(generateDdsIdlBinding(result, intent));
        dsl.append("}\n");

        return dsl.toString();
    }

    public boolean canGenerateTask(NLUResult result) {
        return result != null && result.getPrimaryIntent() != null;
    }

    private String generateTopicName(Intent intent, String destination) {
        if (destination != null && !destination.isEmpty()) {
            String cleanName = destination.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
            return normalizeIdentifier(intent.name().toLowerCase(Locale.ROOT) + "_" + cleanName);
        }
        return normalizeIdentifier(TOPIC_NAME_PREFIX + "_" + intent.name().toLowerCase(Locale.ROOT) + "_" + (System.currentTimeMillis() % 10000));
    }

    private String generateIntentContractTask(NLUResult result, Intent intent) {
        StringBuilder task = new StringBuilder();
        task.append("    task ").append(generateTaskName(result, intent)).append(" {\n");
        task.append("        string schema_version = \"").append(DSL_SCHEMA_VERSION).append("\";\n");
        task.append("        string intent = \"").append(intent.name()).append("\";\n");
        task.append("        string actor_role = \"").append(inferActorRole(intent, result.getOriginalText())).append("\";\n");
        task.append("        string goal = \"").append(escape(buildGoal(intent, result))).append("\";\n");
        task.append("        string destination = \"").append(escape(safe(result.getDestination(), "none"))).append("\";\n");
        task.append("        string start_location = \"").append(escape(safe(result.getStartLocation(), DEFAULT_ORIGIN))).append("\";\n");
        task.append("        string urgency = \"").append(result.getUrgencyLevel().name()).append("\";\n");
        task.append("        string cost_preference = \"").append(result.getCostPreference().name()).append("\";\n");
        task.append("        string route_preference = \"").append(result.getRoutePreference().name()).append("\";\n");
        task.append("        string vehicle_id = \"").append(escape(safe(result.getVehicleId(), "VEH_AUTO"))).append("\";\n");
        task.append("        int passenger_count = ").append(Math.max(1, result.getEstimatedPassengers())).append(";\n");
        task.append("        string required_capability = \"").append(escape(requiredCapability(intent))).append("\";\n");
        task.append("        string execution_mode = \"").append(escape(executionMode(intent))).append("\";\n");
        appendConversationFields(task, result, intent);
        appendIntentSpecificFields(task, result, intent);
        task.append("        int confidence_percent = ").append((int) Math.round(Math.max(0.0, Math.min(1.0, result.getConfidence())) * 100)).append(";\n");
        task.append("        string created_at = \"").append(getCurrentTime()).append("\";\n");
        task.append("    }\n\n");
        return task.toString();
    }

    private void appendConversationFields(StringBuilder task, NLUResult result, Intent intent) {
        List<String> directives = collectConversationDirectives(result);
        if (directives.isEmpty()) {
            return;
        }

        task.append("        string conversation_mode = \"").append(escape(buildConversationMode(result, intent))).append("\";\n");
        task.append("        string peer_directives = \"").append(escape(String.join("|", directives))).append("\";\n");
        task.append("        string peer_target_hint = \"").append(escape(inferPeerTarget(result))).append("\";\n");
        task.append("        string peer_intent = \"").append(escape(inferPeerIntent(directives))).append("\";\n");
    }

    private void appendIntentSpecificFields(StringBuilder task, NLUResult result, Intent intent) {
        switch (intent) {
            case NAVIGATION:
                task.append("        string navigation_mode = \"")
                    .append(escape(safe(result.getNavigationMode(), "balanced_route"))).append("\";\n");
                task.append("        string route_goal = \"").append(escape(buildRouteGoal(result))).append("\";\n");
                break;
            case DELIVERY:
                task.append("        string payload_type = \"").append(escape(extractPayloadType(result))).append("\";\n");
                task.append("        string delivery_priority = \"").append(escape(buildDeliveryPriority(result))).append("\";\n");
                task.append("        string navigation_mode = \"")
                    .append(escape(safe(result.getNavigationMode(), "balanced_route"))).append("\";\n");
                break;
            case COORDINATION:
                task.append("        string coordination_scope = \"").append(escape(buildCoordinationScope(result))).append("\";\n");
                task.append("        string consensus_policy = \"").append(escape(buildConsensusPolicy(result))).append("\";\n");
                break;
            case EMERGENCY:
                task.append("        string payload_type = \"").append(escape(extractPayloadType(result))).append("\";\n");
                task.append("        string escalation_level = \"").append(escape(buildEscalationLevel(result))).append("\";\n");
                task.append("        string response_policy = \"").append(escape(buildEmergencyResponsePolicy(result))).append("\";\n");
                break;
            case QUERY_STATUS:
                task.append("        string query_scope = \"").append(escape(buildQueryScope(result))).append("\";\n");
                task.append("        string response_style = \"").append(escape(buildResponseStyle(result))).append("\";\n");
                break;
            case CONTROL:
                task.append("        string control_action = \"").append(escape(extractControlAction(result))).append("\";\n");
                task.append("        string safety_guard = \"").append(escape(buildSafetyGuard(result))).append("\";\n");
                break;
            default:
                task.append("        string task_profile = \"").append(escape(buildGenericTaskProfile(result, intent))).append("\";\n");
                break;
        }
    }

    private String generateTaskName(NLUResult result, Intent intent) {
        String intentName = intent == null ? "generic" : intent.name().toLowerCase(Locale.ROOT);
        String destination = safe(result == null ? null : result.getDestination(), "");
        String suffix = destination.isBlank()
            ? String.valueOf(System.currentTimeMillis() % 10000)
            : normalizeIdentifier(destination.toLowerCase(Locale.ROOT));
        return normalizeIdentifier(intentName + "_task_" + suffix);
    }

    private String buildRouteGoal(NLUResult result) {
        String destination = safe(result.getDestination(), "目标");
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "优先抵达" + destination;
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH) {
            return "尽快抵达" + destination;
        }
        return "安全抵达" + destination;
    }

    private String buildDeliveryPriority(NLUResult result) {
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "emergency_delivery";
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH) {
            return "priority_delivery";
        }
        if (result.getEstimatedPassengers() > 2) {
            return "group_delivery";
        }
        return "routine_delivery";
    }

    private String buildCoordinationScope(NLUResult result) {
        if (result.getEstimatedPassengers() > 4) {
            return "multi_agent_cluster";
        }
        if (result.getRoutePreference() == NLUResult.RoutePreference.TIME_FIRST) {
            return "time_sensitive_sync";
        }
        if (result.getRoutePreference() == NLUResult.RoutePreference.COST_FIRST) {
            return "resource_optimized_sync";
        }
        return "pairwise_sync";
    }

    private String buildConsensusPolicy(NLUResult result) {
        if (result.getConfidence() >= 0.8) {
            return "fast_consensus_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "priority_override_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        return "distributed_sync_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
    }

    private String buildEscalationLevel(NLUResult result) {
        switch (result.getUrgencyLevel()) {
            case CRITICAL:
                return "level_3";
            case HIGH:
                return "level_2";
            case LOW:
                return "level_0";
            default:
                return "level_1";
        }
    }

    private String buildEmergencyResponsePolicy(NLUResult result) {
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "dispatch_mode_critical";
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH) {
            return "dispatch_mode_rapid";
        }
        return "dispatch_mode_observe";
    }

    private String buildQueryScope(NLUResult result) {
        java.util.List<String> scope = new java.util.ArrayList<>();
        if (result.getDestination() != null && !result.getDestination().isBlank()) {
            scope.add("route");
        }
        if (result.getVehicleId() != null && !result.getVehicleId().isBlank()) {
            scope.add("vehicle");
        }
        if (result.getNavigationMode() != null && !result.getNavigationMode().isBlank()) {
            scope.add("navigation");
        }
        if (result.getEstimatedPassengers() > 0) {
            scope.add("passenger_count");
        }
        scope.add("battery");
        scope.add("drone");
        return String.join(",", scope);
    }

    private String buildResponseStyle(NLUResult result) {
        if (result.getConfidence() >= 0.8) {
            return "concise_metrics_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "alarm_first_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        return "summary_and_metrics_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
    }

    private String buildSafetyGuard(NLUResult result) {
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "double_confirm_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        if (result.getRoutePreference() == NLUResult.RoutePreference.COST_FIRST) {
            return "cost_safety_balanced_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
        }
        return "enabled_" + result.getPrimaryIntent().name().toLowerCase(Locale.ROOT);
    }

    private String buildGenericTaskProfile(NLUResult result, Intent intent) {
        StringBuilder profile = new StringBuilder();
        profile.append(intent == null ? "unknown" : intent.name().toLowerCase(Locale.ROOT));
        if (result.getDestination() != null && !result.getDestination().isBlank()) {
            profile.append("_").append(normalizeIdentifier(result.getDestination()));
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH || result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            profile.append("_priority");
        }
        return profile.toString();
    }

    private String generateRoutePath(NLUResult result) {
        StringBuilder path = new StringBuilder();

        String dest = result.getDestination();
        int distanceKm = 0;
        int timeMin = 0;
        double tolls = 0;
        int strategy = getDrivingStrategy(result);
        String strategyName = strategyToName(strategy);

        try {
            AmapRealAPI amap = getAmapService();
            AmapRealAPI.AmapGeoResult destGeo = amap.geocode(dest);

            if (destGeo.isSuccess()) {
                AmapRealAPI.AmapRouteResult route = amap.drivingWithStrategy(
                    DEFAULT_ORIGIN_LOCATION, destGeo.getLocationString(), strategy);

                if (route.isSuccess()) {
                    distanceKm = Math.max(1, (int) route.getDistanceKm());
                    timeMin = Math.max(1, (int) route.getDurationMinutes());
                    tolls = route.getTolls();
                    if (route.getStrategyName() != null && !route.getStrategyName().trim().isEmpty()) {
                        strategyName = route.getStrategyName();
                    }
                }
            }
        } catch (Exception e) {
            distanceKm = getEstimatedDistance(result);
            timeMin = getEstimatedArrival(result);
        }

        if (distanceKm <= 0) {
            distanceKm = getEstimatedDistance(result);
        }
        if (timeMin <= 0) {
            timeMin = getEstimatedArrival(result);
        }

        String routeType = getRouteType(result.getRoutePreference());
        String waypoints = getWaypoints(result.getRoutePreference());

        path.append("    path ").append(PATH_NAME).append(" {\n");
        path.append("        string route_type = \"").append(routeType).append("\";\n");
        path.append("        string strategy = \"").append(strategyName).append("\";\n");
        path.append("        string start = \"").append(escape(DEFAULT_ORIGIN)).append("\";\n");
        path.append("        string end = \"").append(escape(result.getDestination())).append("\";\n");
        if (!waypoints.isEmpty()) {
            path.append("        string waypoints = \"").append(escape(waypoints)).append("\";\n");
        }
        path.append("        int estimated_distance_km = ").append(distanceKm).append(";\n");
        path.append("        string estimated_distance_unit = \"km\";\n");
        path.append("        int estimated_time_min = ").append(timeMin).append(";\n");
        path.append("        string estimated_time_unit = \"min\";\n");

        if (tolls > 0) {
            path.append("        float toll_cost_yuan = ").append(String.format(Locale.ROOT, "%.1f", tolls)).append(";\n");
        }

        if (result.getCostPreference() == NLUResult.CostPreference.ANY) {
            path.append("        string toll_cost = \"unlimited\";\n");
        } else if (result.getRoutePreference() == NLUResult.RoutePreference.TIME_FIRST) {
            path.append("        string toll_cost = \"estimated\";\n");
        } else {
            path.append("        string toll_cost = \"avoid_toll\";\n");
        }

        path.append("    }\n\n");
        return path.toString();
    }

    private String generateRuntimeState(NLUResult result, Intent intent) {
        StringBuilder state = new StringBuilder();

        state.append("    state ").append(STATE_NAME).append(" {\n");
        state.append("        string status = \"").append(defaultStatus(intent)).append("\";\n");
        state.append("        string intent_stage = \"planning\";\n");
        state.append("        string urgency = \"").append(result.getUrgencyLevel().name()).append("\";\n");
        state.append("        string cost_constraint = \"").append(result.getCostPreference().name()).append("\";\n");
        state.append("        string route_optimization = \"").append(result.getRoutePreference().name()).append("\";\n");
        state.append("        string vehicle_mode = \"").append(escape(safe(result.getNavigationMode(), "adaptive"))).append("\";\n");
        state.append("        int monitoring_interval_sec = ").append(getMonitoringInterval(result.getUrgencyLevel())).append(";\n");
        state.append("        string monitoring_interval_unit = \"sec\";\n");
        state.append("    }\n");

        return state.toString();
    }

    private String generateFlowState(NLUResult result, Intent intent) {
        StringBuilder flow = new StringBuilder();
        Set<String> publishTopics = buildPublishTopics(result, intent);
        Set<String> subscribeTopics = buildSubscribeTopics(result, intent);
        flow.append("\n    state ").append(FLOW_NAME).append(" {\n");
        flow.append("        string publish_topics = \"").append(escape(String.join(",", publishTopics))).append("\";\n");
        flow.append("        string subscribe_topics = \"").append(escape(String.join(",", subscribeTopics))).append("\";\n");
        flow.append("        string exchange_contract = \"").append(escape(buildExchangeContract(result))).append("\";\n");
        flow.append("        string dds_qos = \"").append(defaultQos(intent)).append("\";\n");
        flow.append("        string fallback_policy = \"").append(escape(buildFallbackPolicy(result, intent))).append("\";\n");
        flow.append("    }\n");
        return flow.toString();
    }

    private String generateDataExchangeContract(NLUResult result, Intent intent) {
        StringBuilder state = new StringBuilder();
        state.append("\n    state dataExchangeContract {\n");
        state.append("        string purpose = \"define_vehicle_to_vehicle_and_infrastructure_data\";\n");
        state.append("        string data_1 = \"topic=urban/vehicle/telemetry.dsl; idl=urban.VehicleTelemetry; publisher=CAR-CQ-001|CAR-CQ-002; subscriber=UAV-CQ-007; fields=vehicle_id,position_x_m,position_y_m,speed_mps,heading_deg,road_event,traffic_signal; qos=BEST_EFFORT_sensor_stream; purpose=share_vehicle_motion_and_road_state\";\n");
        state.append("        string data_2 = \"topic=urban/uav/aerial_perception.dsl; idl=urban.EnvironmentPerception; publisher=UAV-CQ-007; subscriber=CAR-CQ-001|CAR-CQ-002; fields=event_type,confidence,risk_score,visibility_m,wind_speed_mps,evidence_summary; qos=BEST_EFFORT_sensor_stream; purpose=share_forward_corridor_perception\";\n");
        state.append("        string data_3 = \"topic=urban/rsu/road_event.dsl; idl=urban.RsuRoadEvent; publisher=RSU-SCHOOL-009; subscriber=CAR-CQ-001|CAR-CQ-002; fields=road_event,traffic_signal,pedestrian_count,rsu_load,recommended_route,risk_score; qos=RELIABLE_keep_last_10; purpose=share_roadside_event\";\n");
        state.append("        string data_4 = \"topic=backbone/compliant/context.dsl; idl=urban.CompliantNetworkContext; publisher=CN-BACKBONE-CQ; subscriber=CAR-CQ-001|CAR-CQ-002; fields=link_mode,link_health,private_5g_signal,rsu_load,expected_latency_ms,recommended_route; qos=RELIABLE_keep_last_10; purpose=context_only_no_remote_dispatch\";\n");
        state.append("    }\n");
        return state.toString();
    }

    private String generateCommandExchangeContract(NLUResult result, Intent intent) {
        StringBuilder state = new StringBuilder();
        state.append("\n    state commandExchangeContract {\n");
        state.append("        string purpose = \"define_intent_advice_and_final_decision_commands\";\n");
        state.append("        string command_1 = \"topic=cabin/passenger/intent.dsl; idl=urban.MissionCommand; publisher=CABIN-CAR-CQ-001; subscriber=CAR-CQ-001|CAR-CQ-002|UAV-CQ-007|RSU-SCHOOL-009; kind=PASSENGER_INTENT; fields=natural_language,destination,urgency,preference,requested_capabilities,publish_topics,subscribe_topics; qos=RELIABLE_keep_last_10; effect=trigger_device_self_subscription\";\n");
        state.append("        string command_2 = \"topic=urban/vehicle/peer_advice.dsl; idl=urban.RouteAdvice; publisher=CAR-CQ-002; subscriber=CAR-CQ-001; kind=PEER_ADVICE; fields=source_type,destination,route_candidate,risk_score,confidence,estimated_time_min,advice_reason; qos=RELIABLE_keep_last_10; effect=provide_peer_route_suggestion\";\n");
        state.append("        string command_3 = \"topic=urban/route/advice.dsl; idl=urban.RouteAdvice; publisher=CAR-CQ-001|CAR-CQ-002|UAV-CQ-007; subscriber=CABIN-CAR-CQ-001|CAR-CQ-001|CN-BACKBONE-CQ; kind=ROUTE_ADVICE; fields=route_candidate,risk_score,confidence,advice_reason,required_topics; qos=RELIABLE_keep_last_10; effect=feed_route_fusion\";\n");
        state.append("        string command_4 = \"topic=urban/route/final_decision.dsl; idl=urban.RouteDecision; publisher=CAR-CQ-001; subscriber=CABIN-CAR-CQ-001; kind=FINAL_ROUTE_DECISION; fields=decision_owner,autonomy_mode,final_route,final_risk_score,confidence,advice_sources,passenger_explanation,fusion_reason; qos=RELIABLE_keep_last_10; effect=explain_vehicle_self_decision_to_passenger\";\n");
        state.append("        string safety_rule = \"wide_area_backbone_may_publish_context_but_must_not_publish_dispatch_command\";\n");
        state.append("    }\n");
        return state.toString();
    }

    private String generateDdsIdlBinding(NLUResult result, Intent intent) {
        StringBuilder state = new StringBuilder();
        state.append("\n    state ddsIdlBinding {\n");
        state.append("        string idl_file = \"idl/urban_intent_collaboration.idl\";\n");
        state.append("        string binding_1 = \"cabin/passenger/intent.dsl -> urban.MissionCommand\";\n");
        state.append("        string binding_2 = \"urban/vehicle/telemetry.dsl -> urban.VehicleTelemetry\";\n");
        state.append("        string binding_3 = \"urban/uav/aerial_perception.dsl -> urban.EnvironmentPerception\";\n");
        state.append("        string binding_4 = \"urban/rsu/road_event.dsl -> urban.RsuRoadEvent\";\n");
        state.append("        string binding_5 = \"backbone/compliant/context.dsl -> urban.CompliantNetworkContext\";\n");
        state.append("        string binding_6 = \"urban/route/advice.dsl -> urban.RouteAdvice\";\n");
        state.append("        string binding_7 = \"urban/route/final_decision.dsl -> urban.RouteDecision\";\n");
        state.append("    }\n");
        return state.toString();
    }

    private Set<String> buildPublishTopics(NLUResult result, Intent intent) {
        Set<String> topics = new LinkedHashSet<>();
        for (String topic : getPublishTopics(intent)) {
            topics.add(topic);
        }

        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "建议", "让路", "提醒", "通知", "协同", "其他车", "前车", "后车")) {
            topics.add("Vehicle/Peer/Advice");
            topics.add("Vehicle/Peer/Negotiation");
        }
        if (containsAny(text, "让路", "借道", "避让")) {
            topics.add("Vehicle/Yield/Request");
        }
        if (containsAny(text, "无人机", "空域")) {
            topics.add("UAV/Perception/ForwardCorridor");
        }
        if (containsAny(text, "合规广域链路", "卫星", "卫星互联网", "低轨", "北斗", "5g专网")) {
            topics.add("CompliantBackbone/GlobalPerception");
        }
        return topics;
    }

    private Set<String> buildSubscribeTopics(NLUResult result, Intent intent) {
        Set<String> topics = new LinkedHashSet<>();
        for (String topic : getSubscribeTopics(intent)) {
            topics.add(topic);
        }

        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "建议", "让路", "提醒", "通知", "协同", "其他车", "前车", "后车")) {
            topics.add("Vehicle/Peer/Advice");
            topics.add("Vehicle/Peer/Negotiation");
            topics.add("Vehicle/Yield/Ack");
        }
        if (containsAny(text, "路口", "红绿灯", "拥堵")) {
            topics.add("Traffic/Intersection/Forward");
        }
        if (containsAny(text, "无人机", "空域")) {
            topics.add("UAV/Perception/ForwardCorridor");
        }
        if (containsAny(text, "合规广域链路", "卫星", "卫星互联网", "低轨", "北斗", "5g专网")) {
            topics.add("CompliantBackbone/GlobalPerception");
        }
        return topics;
    }

    private String buildExchangeContract(NLUResult result) {
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "建议", "让路", "协同", "谈判")) {
            return "data_and_advice";
        }
        if (containsAny(text, "查询", "状态", "进度")) {
            return "query_and_response";
        }
        return "command_and_data";
    }

    private String buildFallbackPolicy(NLUResult result, Intent intent) {
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            return "priority_path_with_local_override";
        }
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "建议", "协同", "让路")) {
            return "peer_consensus_then_local";
        }
        return "local_rule_first";
    }

    private boolean needsPath(Intent intent, NLUResult result) {
        if (result == null || result.getDestination() == null || result.getDestination().isBlank()) {
            return false;
        }
        return intent == Intent.NAVIGATION
            || intent == Intent.DELIVERY
            || intent == Intent.EMERGENCY;
    }

    private String inferActorRole(Intent intent, String originalText) {
        String text = originalText == null ? "" : originalText;
        if (text.contains("无人机") || text.contains("飞") || intent == Intent.DELIVERY) {
            return "DRONE";
        }
        if (text.contains("控制中心") || text.contains("后台") || text.contains("指挥中心")) {
            return "CONTROL_CENTER";
        }
        if (text.contains("无人车") || text.contains("乘客") || text.contains("路线") || text.contains("协同") || intent == Intent.COORDINATION) {
            return "VEHICLE";
        }
        return "VEHICLE";
    }

    private String buildGoal(Intent intent, NLUResult result) {
        String destination = safe(result.getDestination(), "目标未指定");
        String start = safe(result.getStartLocation(), DEFAULT_ORIGIN);
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        List<String> goalParts = new ArrayList<>();

        if (intent == Intent.NAVIGATION) {
            goalParts.add("从" + start + "前往" + destination);
        } else if (intent == Intent.DELIVERY) {
            goalParts.add("从" + start + "向" + destination + "执行配送");
        } else if (intent == Intent.COORDINATION) {
            goalParts.add("围绕" + destination + "进行多设备协同");
        } else if (intent == Intent.EMERGENCY) {
            goalParts.add("对" + destination + "启动应急响应");
        } else if (intent == Intent.QUERY_STATUS) {
            goalParts.add("对" + destination + "进行状态查询");
        } else if (intent == Intent.CONTROL) {
            goalParts.add("在" + destination + "执行控制动作");
        } else {
            goalParts.add("执行交通任务");
        }

        if (containsAny(text, "建议", "让路", "提醒", "通知")) {
            goalParts.add("同步生成车间建议并广播");
        }
        if (result.getUrgencyLevel() == NLUResult.UrgencyLevel.HIGH || result.getUrgencyLevel() == NLUResult.UrgencyLevel.CRITICAL) {
            goalParts.add("优先保障时效");
        }
        if (result.getCostPreference() == NLUResult.CostPreference.ECONOMICAL) {
            goalParts.add("控制通行成本");
        }

        return String.join("，", goalParts);
    }

    private String extractControlAction(NLUResult result) {
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (text.contains("停车") || text.contains("停下")) return "STOP";
        if (text.contains("减速")) return "SLOW_DOWN";
        if (text.contains("加速")) return "SPEED_UP";
        if (text.contains("充电")) return "CHARGE";
        if (text.contains("空调")) return "AC_CONTROL";
        return "NONE";
    }

    private String extractPayloadType(NLUResult result) {
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (text.contains("医疗") || text.contains("药") || text.contains("急救")) return "MEDICAL";
        if (text.contains("文件") || text.contains("证件")) return "DOCUMENT";
        if (text.contains("包裹") || text.contains("快递")) return "PARCEL";
        return "GENERAL";
    }

    private String requiredCapability(Intent intent) {
        switch (intent) {
            case NAVIGATION:
                return "route_planning";
            case DELIVERY:
                return "air_ground_delivery";
            case COORDINATION:
                return "multi_agent_collaboration";
            case EMERGENCY:
                return "emergency_response";
            case QUERY_STATUS:
                return "status_observability";
            case CONTROL:
                return "vehicle_control";
            default:
                return "basic_execution";
        }
    }

    private String executionMode(Intent intent) {
        if (intent == Intent.COORDINATION || intent == Intent.EMERGENCY) {
            return "distributed";
        }
        return "local_with_dds_sync";
    }

    private String defaultStatus(Intent intent) {
        if (intent == Intent.QUERY_STATUS) {
            return "observing";
        }
        if (intent == Intent.CONTROL) {
            return "controlling";
        }
        return "active";
    }

    private String defaultQos(Intent intent) {
        if (intent == Intent.EMERGENCY) {
            return "reliable_low_latency";
        }
        if (intent == Intent.QUERY_STATUS) {
            return "best_effort";
        }
        return "reliable";
    }

    private String[] getPublishTopics(Intent intent) {
        switch (intent) {
            case NAVIGATION:
                return new String[]{"cabin/passenger/intent.dsl", "urban/vehicle/telemetry.dsl", "urban/route/advice.dsl", "urban/route/final_decision.dsl"};
            case DELIVERY:
                return new String[]{"DroneStatus", "DeliveryStatus", "MissionAssignment", "SemanticTaskDSL"};
            case COORDINATION:
                return new String[]{"cabin/passenger/intent.dsl", "urban/route/advice.dsl", "Vehicle/Peer/Advice", "SemanticTaskDSL", "CompliantBackboneSync"};
            case EMERGENCY:
                return new String[]{"EmergencyAlert", "MissionAssignment", "Route/Replan/Trigger"};
            case QUERY_STATUS:
                return new String[]{"SubscriptionIntent"};
            case CONTROL:
                return new String[]{"ControlCommand"};
            default:
                return new String[]{"MissionRequest"};
        }
    }

    private String[] getSubscribeTopics(Intent intent) {
        switch (intent) {
            case NAVIGATION:
                return new String[]{"urban/vehicle/peer_advice.dsl", "urban/uav/aerial_perception.dsl", "urban/rsu/road_event.dsl", "backbone/compliant/context.dsl", "urban/route/advice.dsl"};
            case DELIVERY:
                return new String[]{"WeatherData", "NoFlyZone", "UAV/Perception/ForwardCorridor"};
            case COORDINATION:
                return new String[]{"VehicleStatus", "DroneStatus", "UAV/Perception/ForwardCorridor", "Traffic/Intersection/Forward", "urban/route/advice.dsl", "CompliantBackbone/GlobalPerception"};
            case EMERGENCY:
                return new String[]{"EmergencyStatus", "TrafficFlow", "GlobalPerception"};
            case QUERY_STATUS:
                return new String[]{"VehicleData", "DroneStatus", "BatteryStatus"};
            case CONTROL:
                return new String[]{"VehicleData", "MissionStatus"};
            default:
                return new String[]{"GlobalPerception"};
        }
    }

    private String joinTopics(String[] topics) {
        if (topics == null || topics.length == 0) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topics.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(topics[i]);
        }
        return sb.toString();
    }

    private String getRouteType(NLUResult.RoutePreference pref) {
        switch (pref) {
            case TIME_FIRST:
                return "highway_toll_road";
            case COST_FIRST:
                return "local_economy_route";
            case SCENIC:
                return "scenic_route";
            default:
                return "balanced_route";
        }
    }

    private String getWaypoints(NLUResult.RoutePreference pref) {
        switch (pref) {
            case TIME_FIRST:
                return buildWaypointHint("time", DEFAULT_ORIGIN);
            case COST_FIRST:
                return buildWaypointHint("cost", DEFAULT_ORIGIN);
            case SCENIC:
                return buildWaypointHint("scenic", DEFAULT_ORIGIN);
            default:
                return buildWaypointHint("balanced", DEFAULT_ORIGIN);
        }
    }

    private String buildWaypointHint(String mode, String origin) {
        String originTag = normalizeIdentifier(safe(origin, "origin"));
        return mode + "_via_" + originTag + "," + mode + "_handoff," + mode + "_arrival";
    }

    private int getEstimatedDistance(NLUResult result) {
        String dest = result.getDestination();
        if (dest == null) return 20;

        try {
            AmapRealAPI amap = getAmapService();
            AmapRealAPI.AmapGeoResult destGeo = amap.geocode(dest);

            if (destGeo.isSuccess()) {
                int strategy = getDrivingStrategy(result);
                AmapRealAPI.AmapRouteResult route = amap.drivingWithStrategy(
                    DEFAULT_ORIGIN_LOCATION, destGeo.getLocationString(), strategy);

                if (route.isSuccess()) {
                    return Math.max(1, (int) route.getDistanceKm());
                }
            }
        } catch (Exception e) {
            // fallback
        }

        if (dest.contains("大学")) return 15;
        if (dest.contains("机场")) return 35;
        if (dest.contains("医院")) return 10;
        if (dest.contains("火车站")) return 25;
        if (dest.contains("景区") || dest.contains("景点")) return 20;

        return 15;
    }

    private int getEstimatedArrival(NLUResult result) {
        String dest = result.getDestination();
        if (dest == null) return 30;

        try {
            AmapRealAPI amap = getAmapService();
            AmapRealAPI.AmapGeoResult destGeo = amap.geocode(dest);

            if (destGeo.isSuccess()) {
                int strategy = getDrivingStrategy(result);
                AmapRealAPI.AmapRouteResult route = amap.drivingWithStrategy(
                    DEFAULT_ORIGIN_LOCATION, destGeo.getLocationString(), strategy);

                if (route.isSuccess()) {
                    return Math.max(1, (int) route.getDurationMinutes());
                }
            }
        } catch (Exception e) {
            // fallback
        }

        int baseDistance = getEstimatedDistance(result);
        String mode = safe(result.getNavigationMode(), "balanced");
        if (mode.contains("highway")) {
            return Math.max(1, (int) (baseDistance / 1.2));
        } else if (mode.contains("economy")) {
            return Math.max(1, (int) (baseDistance / 0.5));
        }
        return Math.max(1, (int) (baseDistance / 0.6));
    }

    private int getDrivingStrategy(NLUResult result) {
        NLUResult.UrgencyLevel urgency = result.getUrgencyLevel();
        NLUResult.RoutePreference routePref = result.getRoutePreference();
        NLUResult.CostPreference costPref = result.getCostPreference();
        double costAwareScore = getIntentScore(result, Intent.COST_AWARE);
        double preferEconomyScore = getIntentScore(result, Intent.PREFER_ECONOMY);
        double emergencyScore = getIntentScore(result, Intent.EMERGENCY);

        boolean intentCostFirst = (costAwareScore >= 0.8 || preferEconomyScore >= 0.8)
            && emergencyScore < 0.9
            && urgency != NLUResult.UrgencyLevel.CRITICAL
            && urgency != NLUResult.UrgencyLevel.HIGH;

        if (intentCostFirst) {
            return 1;
        }
        if (urgency == NLUResult.UrgencyLevel.CRITICAL || urgency == NLUResult.UrgencyLevel.HIGH) {
            return 0;
        }
        if (costPref == NLUResult.CostPreference.ECONOMICAL || routePref == NLUResult.RoutePreference.COST_FIRST) {
            return 1;
        }
        if (routePref == NLUResult.RoutePreference.SCENIC) {
            return 2;
        }
        return 0;
    }

    private double getIntentScore(NLUResult result, Intent intent) {
        if (result == null || result.getIntentScores() == null || intent == null) {
            return 0.0;
        }
        Map<Intent, Double> scores = result.getIntentScores();
        Double score = scores.get(intent);
        return score == null ? 0.0 : score;
    }

    private String strategyToName(int strategy) {
        switch (strategy) {
            case 1:
                return "费用优先";
            case 2:
                return "距离优先";
            case 4:
                return "躲避拥堵";
            case 3:
                return "多策略平衡";
            default:
                return "速度优先";
        }
    }

    private int getMonitoringInterval(NLUResult.UrgencyLevel level) {
        switch (level) {
            case CRITICAL:
                return 10;
            case HIGH:
                return 15;
            case NORMAL:
                return 30;
            case LOW:
                return 60;
            default:
                return 30;
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String normalizeIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return "mission_contract";
        }
        String normalized = raw.replaceAll("[^\\p{L}\\p{N}_]", "_");
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "m_" + normalized;
        }
        return normalized;
    }

    private List<String> collectConversationDirectives(NLUResult result) {
        List<String> directives = new ArrayList<>();
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "建议", "提醒", "通知")) {
            directives.add("peer_advice");
        }
        if (containsAny(text, "让路", "借道", "避让")) {
            directives.add("yield_negotiation");
        }
        if (containsAny(text, "协同", "联动", "配合")) {
            directives.add("coordination_sync");
        }
        if (containsAny(text, "其他车", "前车", "后车", "车队")) {
            directives.add("vehicle_peer_exchange");
        }
        return directives;
    }

    private String buildConversationMode(NLUResult result, Intent intent) {
        List<String> directives = collectConversationDirectives(result);
        if (directives.isEmpty()) {
            return "none";
        }
        return intent.name().toLowerCase(Locale.ROOT) + "_" + String.join("_", directives);
    }

    private String inferPeerTarget(NLUResult result) {
        String text = safe(result.getOriginalText(), "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "前车")) {
            return "front_vehicle";
        }
        if (containsAny(text, "后车")) {
            return "rear_vehicle";
        }
        if (containsAny(text, "其他车", "车队")) {
            return "nearby_vehicles";
        }
        if (containsAny(text, "无人机")) {
            return "drone";
        }
        return "peer_auto";
    }

    private String inferPeerIntent(List<String> directives) {
        if (directives.contains("yield_negotiation")) {
            return "yield_request";
        }
        if (directives.contains("peer_advice")) {
            return "advice_broadcast";
        }
        if (directives.contains("coordination_sync")) {
            return "sync_coordination";
        }
        return "context_share";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
