package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 无人系统意图协同平台编排器。
 *
 * 该类把“自然语言意图 -> 结构化 DSL -> IDL/DDS Topic/QoS -> 路径规划 ->
 * 多设备调度 -> DDS 发布订阅模拟 -> 风险与降级建议”串成完整闭环。
 */
public class PlatformOrchestrator {
    private final AdaptiveRoutePlanner routePlanner = new AdaptiveRoutePlanner();
    private final DynamicTaskScheduler scheduler = new DynamicTaskScheduler();
    private final DeviceRegistry deviceRegistry = DeviceRegistry.getInstance();
    private final DdsCommunicationService ddsCommunication = DdsCommunicationService.getInstance();
    private final MissionExecutionService executionService = MissionExecutionService.getInstance();
    private final FaultInjectionService faultService = FaultInjectionService.getInstance();
    private final RiskAssessmentEngine riskEngine = new RiskAssessmentEngine();
    private final TopicTopologyService topologyService = new TopicTopologyService();
    private final MissionHistoryService historyService = MissionHistoryService.getInstance();

    public Map<String, Object> buildFullPlan(String text, String missionId) {
        String source = safeText(text, "紧急调度无人车将患者送往医院，并请求无人机和 RSU 协同");
        MissionDslModel mission = MissionDslModel.fromText(source, missionId);
        IntentProfile intent = parseIntent(source);
        Map<String, Object> structuredDsl = mission.toMap();
        List<Map<String, Object>> devices = deviceCatalog();
        List<Map<String, Object>> topics = selectTopics(intent);
        List<QosPolicyModel> qosModels = selectQosModels(topics, intent);
        List<Map<String, Object>> qosPolicies = qosToMaps(qosModels);
        AdaptiveRoutePlanner.RoutePlan routePlan = planRoute(intent);
        DynamicTaskScheduler.ScheduleResult schedule = scheduleTasks(intent);
        List<Map<String, Object>> assignments = enrichAssignments(schedule, missionId);
        List<Map<String, Object>> ddsTrace = ddsCommunication.simulateExchange(mission, topics, qosModels, schedule);
        List<String> riskWarnings = riskWarnings(intent, devices, routePlan, schedule, ddsTrace);
        Map<String, Object> riskAssessment = riskEngine.assess(mission, devices, routePlan.toMap(), ddsTrace, faultService.list());
        List<String> fallbackStrategies = fallbackStrategies(intent, riskWarnings);
        if (Boolean.TRUE.equals(riskAssessment.get("whetherFallbackRequired"))) {
            fallbackStrategies.add("风险等级达到 " + riskAssessment.get("riskLevel") + "，自动启用备选设备和可靠 DDS 重试链路。");
        }
        Map<String, Object> topology = topologyService.build(topics, devices, ddsTrace, ddsCommunication.getRuntimeMode());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("missionId", missionId);
        result.put("input", source);
        result.put("intent", intent.toMap());
        result.put("structuredDsl", structuredDsl);
        result.put("idlBindings", buildIdlBindings(topics, intent));
        result.put("topics", topics);
        result.put("qosPolicies", qosPolicies);
        result.put("routePlan", routePlan.toMap());
        result.put("routeAnalysis", routeAnalysis(routePlan, intent));
        result.put("deviceCatalog", devices);
        result.put("taskSchedule", schedule.toMap());
        @SuppressWarnings("unchecked")
        Map<String, Object> scheduleMap = (Map<String, Object>) result.get("taskSchedule");
        scheduleMap.put("assignments", assignments);
        result.put("deviceAssignments", assignments);
        result.put("ddsRuntimeMode", ddsCommunication.getRuntimeMode());
        result.put("ddsSimulation", ddsTrace);
        result.put("communicationLogs", latestLogs(50));
        result.put("riskWarnings", riskWarnings);
        result.put("riskAssessment", riskAssessment);
        result.put("fallbackStrategies", fallbackStrategies);
        result.put("topicTopology", topology);
        result.put("activeFaults", faultService.list());
        result.put("executionStatus", executionStatus(ddsTrace, riskWarnings));
        result.put("decisionSummary", decisionSummary(intent, routePlan, schedule));
        historyService.recordPlan(result);
        return result;
    }

    public Map<String, Object> simulate(String text, String missionId) {
        Map<String, Object> plan = buildFullPlan(text, missionId);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("missionId", missionId);
        result.put("accepted", true);
        result.put("status", "SIMULATED_EXECUTION_COMPLETED");
        result.put("ddsSimulation", plan.get("ddsSimulation"));
        result.put("communicationLogs", plan.get("communicationLogs"));
        result.put("riskWarnings", plan.get("riskWarnings"));
        result.put("fallbackStrategies", plan.get("fallbackStrategies"));
        result.put("riskAssessment", plan.get("riskAssessment"));
        Map<String, Object> execution = executionService.createExecution(plan);
        result.put("runId", execution.get("runId"));
        result.put("timeline", execution.get("timeline"));
        result.put("executionTimeline", execution.get("executionTimeline"));
        result.put("timelineSummary", execution.get("timelineSummary"));
        result.put("ddsRuntimeMode", plan.get("ddsRuntimeMode"));
        result.put("activeFaults", plan.get("activeFaults"));
        result.put("message", "已完成发布、订阅、重试和降级流程模拟。");
        return result;
    }

    public List<Map<String, Object>> deviceCatalog() {
        return deviceRegistry.listDevices();
    }

    public List<Map<String, Object>> topicCatalog() {
        List<Map<String, Object>> topics = Arrays.asList(
            topic("VehicleStatusTopic", "urban/vehicle/status", "urban.VehicleStatus", "车辆状态、位置、速度、电量", "sub/pub"),
            topic("DroneMissionTopic", "urban/drone/mission", "urban.DroneMission", "无人机巡检、侦察和航线任务", "pub"),
            topic("EmergencyDispatchTopic", "urban/emergency/dispatch", "urban.EmergencyDispatch", "应急车辆、绿波和救援协同", "pub"),
            topic("RoadSideUnitTopic", "urban/rsu/event", "urban.RsuRoadEvent", "路侧事件、信号灯和局部中继", "sub"),
            topic("TrafficRiskTopic", "urban/traffic/risk", "urban.TrafficRisk", "拥堵、事故和道路风险态势", "sub/pub"),
            topic("CollaborationPlanTopic", "urban/collaboration/plan", "urban.CollaborationPlan", "多设备协同计划和执行反馈", "pub")
        );
        for (Map<String, Object> topic : topics) {
            topic.put("publishers", publishersFor(String.valueOf(topic.get("name"))));
            topic.put("subscribers", subscribersFor(String.valueOf(topic.get("name"))));
            topic.put("defaultQos", QosPolicyModel.create(String.valueOf(topic.get("name")), true, "Topic 目录默认可靠策略，实际计划中会动态调整。").toMap());
        }
        return topics;
    }

    public List<Map<String, Object>> latestLogs(int limit) {
        return ddsCommunication.getCommunicationLogs(null, null, limit);
    }

    public List<Map<String, Object>> latestLogs(String missionId, String runId, int limit) {
        return ddsCommunication.getCommunicationLogs(missionId, runId, limit);
    }

    public Map<String, Object> topicTopology() {
        return topologyService.build(topicCatalog(), deviceCatalog(), latestLogs(100), ddsCommunication.getRuntimeMode());
    }

    public MissionHistoryService history() {
        return historyService;
    }

    public FaultInjectionService faults() {
        return faultService;
    }

    public DdsCommunicationService dds() {
        return ddsCommunication;
    }

    public Map<String, Object> executePlan(Map<String, Object> platformPlan) {
        return executionService.createExecution(platformPlan);
    }

    public List<Map<String, Object>> executionLogs(String missionId, String runId) {
        return executionService.logs(missionId, runId);
    }

    public DeviceRegistry registry() {
        return deviceRegistry;
    }

    public String ddsRuntimeMode() {
        return ddsCommunication.getRuntimeMode();
    }

    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<String, Object>();
        health.put("ok", true);
        health.put("service", "unmanned-intent-collaboration-platform");
        health.put("timestamp", System.currentTimeMillis());
        health.put("modules", Arrays.asList(
            module("intentParser", "UP", "支持 8 类任务和默认值兜底"),
            module("dslBuilder", "UP", "输出结构化 DSL 字段"),
            module("ddsGateway", "UP", "统一 DDS 通信入口，runtimeMode=" + ddsCommunication.getRuntimeMode()),
            module("deviceRegistry", "UP", "内存级注册、心跳、故障和恢复"),
            module("executionTimeline", "UP", "execute/simulate 生成可回放 runId 时间线"),
            module("routePlanner", "UP", "A* 动态边权路径规划"),
            module("taskScheduler", "UP", "动态权重贪心任务调度")
        ));
        health.put("recentLogCount", ddsCommunication.getCommunicationLogs().size());
        health.put("deviceSummary", deviceRegistry.summary());
        health.put("ddsRuntimeMode", ddsCommunication.getRuntimeMode());
        return health;
    }

    private IntentProfile parseIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        IntentProfile intent = new IntentProfile();
        intent.rawText = text;
        intent.taskType = inferTaskType(text, lower);
        intent.origin = extractAfter(text, "从", "出发", "观音桥");
        intent.destination = inferDestination(text);
        intent.urgency = containsAny(text, "紧急", "急", "马上", "救援", "患者", "120", "最快") ? "HIGH" : "MEDIUM";
        intent.safetyPreference = containsAny(text, "安全", "稳定", "保守", "避开风险") ? "SAFETY_FIRST" : "BALANCED";
        intent.reliabilityRequirement = containsAny(text, "可靠", "必须送达", "应急", "救援", "医疗") ? "RELIABLE" : "BEST_EFFORT_ACCEPTABLE";
        intent.timeLimitMinutes = extractTimeLimit(text, intent.urgency.equals("HIGH") ? 20 : 45);
        intent.riskFactors = inferRiskFactors(text);
        intent.participatingDevices = inferDevices(text, intent.taskType);
        intent.defaultHints = inferDefaultHints(intent);
        return intent;
    }

    private Map<String, Object> buildStructuredDsl(IntentProfile intent, String missionId) {
        Map<String, Object> dsl = new LinkedHashMap<String, Object>();
        dsl.put("dslVersion", "3.1-platform");
        dsl.put("missionId", missionId);
        dsl.put("task", intent.taskType);
        dsl.put("source", intent.origin);
        dsl.put("target", intent.destination);
        dsl.put("devices", intent.participatingDevices);
        dsl.put("priority", intent.urgency);
        dsl.put("qos", intent.reliabilityRequirement);
        dsl.put("constraints", Arrays.asList(
            "vehicle_self_decision",
            "dds_topic_idl_consistency",
            "fallback_when_link_degraded",
            intent.safetyPreference
        ));
        dsl.put("expectedOutput", Arrays.asList(
            "collaboration_plan",
            "route_plan",
            "device_assignments",
            "dds_qos_policy",
            "risk_warnings",
            "execution_feedback"
        ));
        dsl.put("missingInfoHints", intent.defaultHints);
        return dsl;
    }

    private List<Map<String, Object>> selectTopics(IntentProfile intent) {
        List<Map<String, Object>> selected = new ArrayList<Map<String, Object>>();
        selected.add(topic("VehicleStatusTopic", "urban/vehicle/status", "urban.VehicleStatus", "车辆状态反馈", "sub"));
        selected.add(topic("CollaborationPlanTopic", "urban/collaboration/plan", "urban.CollaborationPlan", "协同计划发布", "pub"));
        if (intent.participatingDevices.contains("无人机")) {
            selected.add(topic("DroneMissionTopic", "urban/drone/mission", "urban.DroneMission", "无人机任务发布", "pub"));
        }
        if (intent.participatingDevices.contains("RSU")) {
            selected.add(topic("RoadSideUnitTopic", "urban/rsu/event", "urban.RsuRoadEvent", "路侧事件订阅", "sub"));
        }
        if (intent.taskType.contains("应急") || intent.taskType.contains("医疗")) {
            selected.add(topic("EmergencyDispatchTopic", "urban/emergency/dispatch", "urban.EmergencyDispatch", "应急协同发布", "pub"));
        }
        selected.add(topic("TrafficRiskTopic", "urban/traffic/risk", "urban.TrafficRisk", "交通风险态势", "sub"));
        return selected;
    }

    private List<QosPolicyModel> selectQosModels(List<Map<String, Object>> topics, IntentProfile intent) {
        List<QosPolicyModel> policies = new ArrayList<QosPolicyModel>();
        for (Map<String, Object> topic : topics) {
            String name = String.valueOf(topic.get("name"));
            boolean critical = intent.urgency.equals("HIGH") || name.contains("Emergency") || name.contains("Collaboration");
            policies.add(QosPolicyModel.create(name, critical, critical
                ? "高紧急或关键协同 Topic，需要可靠送达、历史保留和低延迟。"
                : "传感器态势类 Topic 允许短时丢包，优先降低带宽和延迟压力。"));
        }
        return policies;
    }

    private List<Map<String, Object>> qosToMaps(List<QosPolicyModel> policies) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (QosPolicyModel policy : policies) {
            result.add(policy.toMap());
        }
        return result;
    }

    private AdaptiveRoutePlanner.RoutePlan planRoute(IntentProfile intent) {
        AdaptiveRoutePlanner.RouteContext ctx = new AdaptiveRoutePlanner.RouteContext();
        ctx.destination = intent.destination;
        ctx.urgent = "HIGH".equals(intent.urgency);
        ctx.safetyFirst = "SAFETY_FIRST".equals(intent.safetyPreference);
        ctx.congestionLevel = faultService.hasType("HIGH_CONGESTION") ? 0.95 : intent.riskFactors.contains("拥堵") ? 0.82 : 0.36;
        ctx.roadFriction = intent.riskFactors.contains("雨雪湿滑") ? 0.66 : 0.82;
        ctx.ambulanceLaneClearance = faultService.hasType("ROUTE_BLOCKED") ? 0.20 : intent.taskType.contains("应急") || intent.taskType.contains("医疗") ? 0.88 : 0.62;
        try {
            return routePlanner.plan(ctx);
        } catch (RuntimeException ex) {
            AdaptiveRoutePlanner.RoutePlan fallback = new AdaptiveRoutePlanner.RoutePlan();
            fallback.routeId = "route_A_main_road";
            fallback.algorithm = "fallback_route_policy";
            fallback.path = Arrays.asList("START", "MAIN_ROAD", "HOSPITAL");
            fallback.estimatedMinutes = 30;
            fallback.riskScore = 0.35;
            fallback.reason = "路径规划异常，采用主路兜底: " + ex.getMessage();
            return fallback;
        }
    }

    private DynamicTaskScheduler.ScheduleResult scheduleTasks(IntentProfile intent) {
        List<DynamicTaskScheduler.TaskRequest> tasks = new ArrayList<DynamicTaskScheduler.TaskRequest>();
        tasks.add(DynamicTaskScheduler.task("vehicle-execution", "无人车路径执行与状态反馈", 9, 100, 30, "路径执行", "VehicleStatusTopic"));
        tasks.add(DynamicTaskScheduler.task("risk-perception", "无人机/RSU 风险感知", 8, 120, 20, "事故检测"));
        tasks.add(DynamicTaskScheduler.task("rsu-signal", "RSU 信号灯与局部中继", 8, 80, 0, "信号灯协同"));
        tasks.add(DynamicTaskScheduler.task("wide-area-sync", "广域态势同步", 6, 200, 0, "跨区域态势共享"));
        if (intent.taskType.contains("医疗") || intent.taskType.contains("应急")) {
            tasks.add(DynamicTaskScheduler.task("emergency-support", "应急车辆联动与绿波保障", 10, 80, 20, "应急救援"));
        }

        return scheduler.schedule(tasks, deviceRegistry.schedulerDevices());
    }

    private List<Map<String, Object>> enrichAssignments(DynamicTaskScheduler.ScheduleResult schedule, String missionId) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        if (schedule == null) return items;
        for (DynamicTaskScheduler.Assignment assignment : schedule.assignments) {
            Map<String, Object> map = assignment.toMap();
            String deviceId = String.valueOf(map.get("deviceId"));
            List<String> capabilities = new ArrayList<String>(assignment.task.requiredCapabilities);
            String backup = deviceRegistry.backupDeviceFor(capabilities, deviceId);
            map.put("requiredCapabilities", capabilities);
            map.put("estimatedExecutionMinutes", Math.max(3, 18 - assignment.task.priority));
            map.put("riskWarning", deviceRegistry.riskFor(deviceId));
            map.put("backupDeviceId", backup.isBlank() ? "NO_BACKUP_AVAILABLE" : backup);
            map.put("assignmentPolicy", "基于动态权重评分的贪心任务调度算法");
            deviceRegistry.assignMission(deviceId, missionId);
            items.add(map);
        }
        return items;
    }

    private List<Map<String, Object>> buildIdlBindings(List<Map<String, Object>> topics, IntentProfile intent) {
        List<Map<String, Object>> bindings = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> topic : topics) {
            Map<String, Object> binding = new LinkedHashMap<String, Object>();
            binding.put("topic", topic.get("name"));
            binding.put("ddsTopic", topic.get("ddsTopic"));
            binding.put("idlType", topic.get("idlType"));
            binding.put("fields", Arrays.asList("mission_id", "device_id", "timestamp", "payload", "priority", "risk_score"));
            binding.put("source", intent.origin);
            binding.put("target", intent.destination);
            bindings.add(binding);
        }
        return bindings;
    }

    private Map<String, Object> routeAnalysis(AdaptiveRoutePlanner.RoutePlan routePlan, IntentProfile intent) {
        Map<String, Object> analysis = new LinkedHashMap<String, Object>();
        analysis.put("recommendedRoute", routePlan.routeId);
        analysis.put("recommendedReason", routePlan.reason);
        analysis.put("riskExplanation", routePlan.riskScore > 0.45 ? "路径存在较高风险，需要 RSU 与无人机持续感知。" : "路径风险可控，保持常规状态反馈。");
        analysis.put("candidateRoutes", routePlan.toMap().get("candidateRoutes"));
        analysis.put("notRecommendedReasons", Arrays.asList(
            "主路在拥堵或事故场景下时间代价偏高",
            "通信覆盖不足的路段不适合高可靠应急任务",
            "湿滑路面下不推荐高速通过学校慢行区"
        ));
        analysis.put("taskUrgency", intent.urgency);
        return analysis;
    }

    private List<String> riskWarnings(IntentProfile intent, List<Map<String, Object>> devices,
                                      AdaptiveRoutePlanner.RoutePlan routePlan,
                                      DynamicTaskScheduler.ScheduleResult schedule,
                                      List<Map<String, Object>> ddsTrace) {
        List<String> risks = new ArrayList<String>();
        if (intent.defaultHints.size() > 0) risks.addAll(intent.defaultHints);
        if (routePlan.riskScore > 0.45) risks.add("推荐路径风险偏高，需持续订阅 TrafficRiskTopic。");
        if (!schedule.unassignedTasks.isEmpty()) risks.add("存在未分配任务: " + schedule.unassignedTasks);
        for (Map<String, Object> device : devices) {
            int battery = ((Number) device.get("batteryLevel")).intValue();
            double linkQuality = ((Number) device.get("linkQuality")).doubleValue();
            if (battery < 30) risks.add(device.get("deviceId") + " 电量不足，建议只承担低负载任务。");
            if (linkQuality < 0.70) risks.add(device.get("deviceId") + " 链路质量偏低，建议启用备用链路。");
        }
        for (Map<String, Object> event : ddsTrace) {
            if ("DROPPED".equals(event.get("status")) || "DEGRADED".equals(event.get("status"))) {
                risks.add("DDS " + event.get("topic") + " 出现 " + event.get("status") + ": " + event.get("reason"));
            }
        }
        if (risks.isEmpty()) risks.add("当前计划风险可控，维持可靠 QoS 和周期性状态反馈。");
        return risks;
    }

    private List<String> fallbackStrategies(IntentProfile intent, List<String> risks) {
        List<String> fallback = new ArrayList<String>();
        fallback.add("路径不可达时回退 route_A_main_road 并降低速度。");
        fallback.add("无人机不可用时由 RSU 接管局部感知，车辆提高安全距离。");
        fallback.add("可靠 DDS Topic 超时后执行 1 次重试，仍失败则降级为广域链路摘要同步。");
        if (intent.taskType.contains("应急") || intent.taskType.contains("医疗")) {
            fallback.add("应急任务中 RSU 优先请求绿波通道，应急车辆作为备选执行主体。");
        }
        if (risks.size() > 2) {
            fallback.add("风险项过多时进入保守策略：安全优先、低速、扩大感知订阅范围。");
        }
        return fallback;
    }

    private Map<String, Object> executionStatus(List<Map<String, Object>> ddsTrace, List<String> risks) {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        long delivered = ddsTrace.stream().filter(e -> "DELIVERED".equals(e.get("status"))).count();
        long dropped = ddsTrace.stream().filter(e -> "DROPPED".equals(e.get("status"))).count();
        status.put("state", dropped > 0 ? "DEGRADED_READY" : "READY");
        status.put("deliveredMessages", delivered);
        status.put("droppedMessages", dropped);
        status.put("riskCount", risks.size());
        status.put("explain", dropped > 0 ? "存在 best-effort 丢包，计划仍可通过重试或下一周期样本继续执行。" : "通信模拟完成，计划可进入执行态。");
        return status;
    }

    private String decisionSummary(IntentProfile intent, AdaptiveRoutePlanner.RoutePlan routePlan,
                                   DynamicTaskScheduler.ScheduleResult schedule) {
        return "任务类型=" + intent.taskType
            + "，推荐路线=" + routePlan.routeId
            + "，调度任务数=" + schedule.assignments.size()
            + "，QoS=" + intent.reliabilityRequirement
            + "，策略=" + intent.safetyPreference + "。";
    }

    private Map<String, Object> topic(String name, String ddsTopic, String idlType, String description, String direction) {
        Map<String, Object> topic = new LinkedHashMap<String, Object>();
        topic.put("name", name);
        topic.put("ddsTopic", ddsTopic);
        topic.put("idlModule", "urban");
        topic.put("idlType", idlType);
        topic.put("direction", direction);
        topic.put("description", description);
        return topic;
    }

    private List<String> publishersFor(String topicName) {
        if ("DroneMissionTopic".equals(topicName)) return Arrays.asList("PlatformOrchestrator", "CAR-CQ-001");
        if ("RoadSideUnitTopic".equals(topicName)) return Arrays.asList("RSU-SCHOOL-009");
        if ("TrafficRiskTopic".equals(topicName)) return Arrays.asList("UAV-CQ-007", "RSU-SCHOOL-009");
        if ("VehicleStatusTopic".equals(topicName)) return Arrays.asList("CAR-CQ-001", "EMG-CQ-120");
        return Arrays.asList("PlatformOrchestrator");
    }

    private List<String> subscribersFor(String topicName) {
        if ("DroneMissionTopic".equals(topicName)) return Arrays.asList("UAV-CQ-007");
        if ("RoadSideUnitTopic".equals(topicName)) return Arrays.asList("CAR-CQ-001", "CN-BACKBONE-CQ");
        if ("TrafficRiskTopic".equals(topicName)) return Arrays.asList("CAR-CQ-001", "EMG-CQ-120", "CN-BACKBONE-CQ");
        if ("VehicleStatusTopic".equals(topicName)) return Arrays.asList("PlatformOrchestrator", "RSU-SCHOOL-009");
        return Arrays.asList("CAR-CQ-001", "CN-BACKBONE-CQ");
    }

    private Map<String, Object> module(String name, String status, String detail) {
        Map<String, Object> module = new LinkedHashMap<String, Object>();
        module.put("name", name);
        module.put("status", status);
        module.put("detail", detail);
        return module;
    }

    private String inferTaskType(String text, String lower) {
        if (containsAny(text, "医疗", "患者", "医院", "转运")) return "医疗转运";
        if (containsAny(text, "应急", "救援", "消防", "120")) return "应急救援";
        if (containsAny(text, "疏导", "绿波", "信号灯")) return "交通疏导";
        if (containsAny(text, "巡检", "巡查")) return "无人机巡检";
        if (containsAny(text, "事故", "检测", "识别")) return "事故检测";
        if (containsAny(text, "配送", "物资", "药品", "送达")) return "物资配送";
        if (containsAny(text, "广域", "态势", "跨区域")) return "广域态势感知";
        if (containsAny(text, "联合", "巡逻", "多设备")) return "多设备联合巡逻";
        return lower.contains("drone") ? "无人机巡检" : "多设备联合巡逻";
    }

    private String inferDestination(String text) {
        String[] known = {"西南大学附属医院", "医院", "江北机场", "解放碑", "观音桥", "重庆北站", "事故路段"};
        for (String item : known) {
            if (text.contains(item)) return "医院".equals(item) ? "西南大学附属医院" : item;
        }
        return extractAfter(text, "到", "，", "西南大学附属医院");
    }

    private List<String> inferDevices(String text, String taskType) {
        List<String> devices = new ArrayList<String>();
        if (containsAny(text, "无人车", "车辆", "患者", "配送", "转运")) devices.add("无人车");
        if (containsAny(text, "无人机", "巡检", "侦察", "事故")) devices.add("无人机");
        if (containsAny(text, "RSU", "路侧", "信号灯", "绿波")) devices.add("RSU");
        if (containsAny(text, "应急车", "救护", "消防", "120") || taskType.contains("应急") || taskType.contains("医疗")) devices.add("应急车辆");
        if (containsAny(text, "广域", "跨区域", "态势")) devices.add("广域链路");
        if (devices.isEmpty()) devices.addAll(Arrays.asList("无人车", "无人机", "RSU", "广域链路"));
        return devices;
    }

    private List<String> inferRiskFactors(String text) {
        List<String> risks = new ArrayList<String>();
        if (containsAny(text, "拥堵", "堵", "排队")) risks.add("拥堵");
        if (containsAny(text, "事故", "碰撞")) risks.add("事故");
        if (containsAny(text, "雨", "雪", "湿滑")) risks.add("雨雪湿滑");
        if (containsAny(text, "通信差", "弱网", "断连")) risks.add("通信链路质量差");
        if (risks.isEmpty()) risks.add("常规道路风险");
        return risks;
    }

    private List<String> inferDefaultHints(IntentProfile intent) {
        List<String> hints = new ArrayList<String>();
        if (intent.origin == null || intent.origin.isBlank()) hints.add("未给出明确起点，默认使用观音桥。");
        if (intent.destination == null || intent.destination.isBlank()) hints.add("未给出明确终点，默认使用西南大学附属医院。");
        if (intent.timeLimitMinutes <= 0) hints.add("未给出任务时间限制，使用任务类型默认时限。");
        return hints;
    }

    private int extractTimeLimit(String text, int fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s*分钟").matcher(text);
        if (matcher.find()) {
            try {
                return Math.max(1, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String extractAfter(String text, String start, String end, String fallback) {
        int idx = text.indexOf(start);
        if (idx < 0) return fallback;
        String tail = text.substring(idx + start.length()).trim();
        int endIdx = end == null ? -1 : tail.indexOf(end);
        if (endIdx >= 0) tail = tail.substring(0, endIdx);
        tail = tail.replaceAll("[，。,.].*$", "").trim();
        return tail.isEmpty() ? fallback : tail;
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null) return false;
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String safeText(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text.trim();
    }

    private static class IntentProfile {
        String rawText;
        String taskType;
        String origin;
        String destination;
        String urgency;
        String safetyPreference;
        String reliabilityRequirement;
        int timeLimitMinutes;
        List<String> participatingDevices = new ArrayList<String>();
        List<String> riskFactors = new ArrayList<String>();
        List<String> defaultHints = new ArrayList<String>();

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("taskType", taskType);
            map.put("origin", origin);
            map.put("destination", destination);
            map.put("urgency", urgency);
            map.put("participatingDevices", participatingDevices);
            map.put("safetyPreference", safetyPreference);
            map.put("reliabilityRequirement", reliabilityRequirement);
            map.put("timeLimitMinutes", timeLimitMinutes);
            map.put("riskFactors", riskFactors);
            map.put("defaultHints", defaultHints);
            return map;
        }
    }
}
