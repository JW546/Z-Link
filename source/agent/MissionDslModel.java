package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统一任务意图与结构化 DSL Schema。
 *
 * 该模型作为平台内部的 canonical mission schema，向下游 IDL/DDS、路径规划、
 * 调度和执行仿真提供一致字段；旧字符串 DSL 仍作为 legacy 产物保留。
 */
public class MissionDslModel {
    public String missionId;
    public String originalText;
    public String taskType;
    public String priority;
    public String source;
    public String target;
    public List<String> devices = new ArrayList<String>();
    public List<String> constraints = new ArrayList<String>();
    public String qosRequirement;
    public List<String> requiredTopics = new ArrayList<String>();
    public List<String> publishTopics = new ArrayList<String>();
    public List<String> subscribeTopics = new ArrayList<String>();
    public String idlType;
    public Map<String, Object> routeRequirement = new LinkedHashMap<String, Object>();
    public Map<String, Object> collaborationRequirement = new LinkedHashMap<String, Object>();
    public List<String> expectedOutput = new ArrayList<String>();
    public String fallbackPolicy;
    public List<String> missingInfoHints = new ArrayList<String>();
    public List<String> riskFactors = new ArrayList<String>();
    public int timeLimitMinutes;
    public String safetyPreference;

    public static MissionDslModel fromText(String text, String missionId) {
        String sourceText = text == null || text.trim().isEmpty()
            ? "紧急调度无人车将患者送往医院，并请求无人机和 RSU 协同"
            : text.trim();
        String lower = sourceText.toLowerCase(Locale.ROOT);

        MissionDslModel model = new MissionDslModel();
        model.missionId = missionId == null || missionId.isBlank() ? "MSN-" + System.currentTimeMillis() : missionId;
        model.originalText = sourceText;
        model.taskType = inferTaskType(sourceText, lower);
        model.priority = containsAny(sourceText, "紧急", "急", "马上", "救援", "患者", "120", "最快") ? "HIGH" : "MEDIUM";
        model.source = extractAfter(sourceText, "从", "出发", "观音桥");
        model.target = inferDestination(sourceText);
        model.devices = inferDevices(sourceText, model.taskType);
        model.safetyPreference = containsAny(sourceText, "安全", "稳定", "保守", "避开风险") ? "SAFETY_FIRST" : "BALANCED";
        model.qosRequirement = containsAny(sourceText, "可靠", "必须送达", "应急", "救援", "医疗") ? "RELIABLE" : "BEST_EFFORT_ACCEPTABLE";
        model.riskFactors = inferRiskFactors(sourceText);
        model.timeLimitMinutes = extractTimeLimit(sourceText, "HIGH".equals(model.priority) ? 20 : 45);
        model.constraints = Arrays.asList(
            "vehicle_self_decision",
            "dds_topic_idl_consistency",
            "fallback_when_link_degraded",
            model.safetyPreference
        );
        model.expectedOutput = Arrays.asList(
            "collaboration_plan",
            "route_plan",
            "device_assignments",
            "dds_qos_policy",
            "risk_warnings",
            "execution_feedback"
        );
        model.fallbackPolicy = "路径不可达、设备离线或 DDS 超时时启用备选设备和降级通信链路。";
        model.idlType = inferIdlType(model.taskType);
        model.requiredTopics = selectRequiredTopics(model);
        for (String topic : model.requiredTopics) {
            if (topic.contains("Status") || topic.contains("Risk") || topic.contains("RoadSide")) {
                model.subscribeTopics.add(topic);
            } else {
                model.publishTopics.add(topic);
            }
        }
        model.routeRequirement.put("source", model.source);
        model.routeRequirement.put("target", model.target);
        model.routeRequirement.put("priority", model.priority);
        model.routeRequirement.put("safetyPreference", model.safetyPreference);
        model.routeRequirement.put("riskFactors", model.riskFactors);
        model.routeRequirement.put("timeLimitMinutes", model.timeLimitMinutes);
        model.collaborationRequirement.put("devices", model.devices);
        model.collaborationRequirement.put("scheduler", "基于动态权重评分的贪心任务调度算法");
        model.collaborationRequirement.put("backupRequired", true);
        if (model.source == null || model.source.isBlank()) model.missingInfoHints.add("未给出明确起点，默认使用观音桥。");
        if (model.target == null || model.target.isBlank()) model.missingInfoHints.add("未给出明确终点，默认使用西南大学附属医院。");
        if (model.timeLimitMinutes <= 0) model.missingInfoHints.add("未给出时间限制，使用任务类型默认时限。");
        return model;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("schemaVersion", "mission-dsl/v1");
        map.put("missionId", missionId);
        map.put("originalText", originalText);
        map.put("taskType", taskType);
        map.put("priority", priority);
        map.put("source", source);
        map.put("target", target);
        map.put("devices", devices);
        map.put("constraints", constraints);
        map.put("qosRequirement", qosRequirement);
        map.put("requiredTopics", requiredTopics);
        map.put("publishTopics", publishTopics);
        map.put("subscribeTopics", subscribeTopics);
        map.put("idlType", idlType);
        map.put("routeRequirement", routeRequirement);
        map.put("collaborationRequirement", collaborationRequirement);
        map.put("expectedOutput", expectedOutput);
        map.put("fallbackPolicy", fallbackPolicy);
        map.put("missingInfoHints", missingInfoHints);
        map.put("riskFactors", riskFactors);
        Map<String, Object> legacy = new LinkedHashMap<String, Object>();
        legacy.put("task", taskType);
        legacy.put("qos", qosRequirement);
        legacy.put("priority", priority);
        map.put("legacy", legacy);
        return map;
    }

    public Map<String, Object> intentMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("taskType", taskType);
        map.put("origin", source);
        map.put("destination", target);
        map.put("urgency", priority);
        map.put("participatingDevices", devices);
        map.put("safetyPreference", safetyPreference);
        map.put("reliabilityRequirement", qosRequirement);
        map.put("timeLimitMinutes", timeLimitMinutes);
        map.put("riskFactors", riskFactors);
        map.put("defaultHints", missingInfoHints);
        return map;
    }

    public String toDslPreview() {
        return "mission " + missionId + " {\n"
            + "  taskType: \"" + escape(taskType) + "\";\n"
            + "  source: \"" + escape(source) + "\";\n"
            + "  target: \"" + escape(target) + "\";\n"
            + "  priority: " + priority + ";\n"
            + "  devices: " + devices + ";\n"
            + "  qosRequirement: " + qosRequirement + ";\n"
            + "  requiredTopics: " + requiredTopics + ";\n"
            + "}\n";
    }

    private static List<String> selectRequiredTopics(MissionDslModel model) {
        List<String> topics = new ArrayList<String>();
        topics.add("VehicleStatusTopic");
        topics.add("CollaborationPlanTopic");
        if (model.devices.contains("无人机")) topics.add("DroneMissionTopic");
        if (model.devices.contains("RSU")) topics.add("RoadSideUnitTopic");
        if (model.taskType.contains("应急") || model.taskType.contains("医疗")) topics.add("EmergencyDispatchTopic");
        topics.add("TrafficRiskTopic");
        return topics;
    }

    private static String inferTaskType(String text, String lower) {
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

    private static String inferIdlType(String taskType) {
        if (taskType.contains("医疗") || taskType.contains("应急")) return "urban.EmergencyDispatch";
        if (taskType.contains("巡检") || taskType.contains("事故")) return "urban.DroneMission";
        if (taskType.contains("疏导")) return "urban.RsuRoadEvent";
        return "urban.CollaborationPlan";
    }

    private static String inferDestination(String text) {
        String[] known = {"西南大学附属医院", "医院", "江北机场", "解放碑", "观音桥", "重庆北站", "事故路段"};
        for (String item : known) {
            if (text.contains(item)) return "医院".equals(item) ? "西南大学附属医院" : item;
        }
        return extractAfter(text, "到", "，", "西南大学附属医院");
    }

    private static List<String> inferDevices(String text, String taskType) {
        List<String> devices = new ArrayList<String>();
        if (containsAny(text, "无人车", "车辆", "患者", "配送", "转运")) devices.add("无人车");
        if (containsAny(text, "无人机", "巡检", "侦察", "事故")) devices.add("无人机");
        if (containsAny(text, "RSU", "路侧", "信号灯", "绿波")) devices.add("RSU");
        if (containsAny(text, "应急车", "救护", "消防", "120") || taskType.contains("应急") || taskType.contains("医疗")) devices.add("应急车辆");
        if (containsAny(text, "广域", "跨区域", "态势")) devices.add("广域链路");
        if (devices.isEmpty()) devices.addAll(Arrays.asList("无人车", "无人机", "RSU", "广域链路"));
        return devices;
    }

    private static List<String> inferRiskFactors(String text) {
        List<String> risks = new ArrayList<String>();
        if (containsAny(text, "拥堵", "堵", "排队")) risks.add("拥堵");
        if (containsAny(text, "事故", "碰撞")) risks.add("事故");
        if (containsAny(text, "雨", "雪", "湿滑")) risks.add("雨雪湿滑");
        if (containsAny(text, "通信差", "弱网", "断连")) risks.add("通信链路质量差");
        if (risks.isEmpty()) risks.add("常规道路风险");
        return risks;
    }

    private static int extractTimeLimit(String text, int fallback) {
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

    private static String extractAfter(String text, String start, String end, String fallback) {
        int idx = text.indexOf(start);
        if (idx < 0) return fallback;
        String tail = text.substring(idx + start.length()).trim();
        int endIdx = end == null ? -1 : tail.indexOf(end);
        if (endIdx >= 0) tail = tail.substring(0, endIdx);
        tail = tail.replaceAll("[，。,.].*$", "").trim();
        return tail.isEmpty() ? fallback : tail;
    }

    private static boolean containsAny(String text, String... terms) {
        if (text == null) return false;
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
