package agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 典型任务场景库。
 *
 * 用于实验演示时快速选择无人系统协同任务，并复用同一套 plan/simulate 主流程。
 */
public class ScenarioLibrary {
    private static final ScenarioLibrary INSTANCE = new ScenarioLibrary();
    private final List<Map<String, Object>> scenarios = new ArrayList<Map<String, Object>>();

    private ScenarioLibrary() {
        add("medical-transfer", "医疗应急转运", "无人车转运患者，RSU 协同绿波，应急车辆备援。",
            "紧急调度无人车将患者从观音桥送往西南大学附属医院，请 RSU 开启绿波并联动应急车辆。",
            "医疗转运", Arrays.asList("无人车", "RSU", "应急车辆"), Arrays.asList("VehicleStatusTopic", "EmergencyDispatchTopic", "RoadSideUnitTopic"), "RELIABLE_HIGH",
            Arrays.asList("拥堵", "应急车道占用", "通信链路抖动"));
        add("uav-accident-inspection", "无人机事故巡检", "无人机快速侦察事故路段，RSU 汇聚路侧风险。",
            "派遣无人机巡检事故路段，订阅 RSU 路侧事件并同步 TrafficRiskTopic。",
            "事故检测", Arrays.asList("无人机", "RSU"), Arrays.asList("DroneMissionTopic", "RoadSideUnitTopic", "TrafficRiskTopic"), "SENSOR_BALANCED",
            Arrays.asList("事故", "拥堵", "视距遮挡"));
        add("joint-patrol", "多设备联合巡逻", "无人车和无人机联合巡逻，广域链路同步态势。",
            "组织无人车和无人机进行多设备联合巡逻，并通过广域链路同步跨区域态势。",
            "多设备联合巡逻", Arrays.asList("无人车", "无人机", "广域链路"), Arrays.asList("VehicleStatusTopic", "DroneMissionTopic", "CollaborationPlanTopic"), "RELIABLE_MEDIUM",
            Arrays.asList("常规道路风险", "链路质量变化"));
        add("supply-delivery", "物资配送", "无人车配送物资，广域链路提供调度态势，RSU 提供道路感知。",
            "调度无人车配送应急物资，通过广域链路同步状态并订阅路侧感知信息。",
            "物资配送", Arrays.asList("无人车", "广域链路", "RSU"), Arrays.asList("VehicleStatusTopic", "CollaborationPlanTopic", "RoadSideUnitTopic"), "RELIABLE_MEDIUM",
            Arrays.asList("路侧施工", "弱网", "配送时限"));
        add("extreme-weather-risk", "极端天气道路风险监测", "无人机和 RSU 监测雨雪湿滑道路风险。",
            "极端天气下派无人机和 RSU 监测道路风险，生成风险评估并提醒车辆绕行。",
            "广域态势感知", Arrays.asList("无人机", "RSU"), Arrays.asList("DroneMissionTopic", "RoadSideUnitTopic", "TrafficRiskTopic"), "SENSOR_RELIABLE",
            Arrays.asList("雨雪湿滑", "能见度低", "传感器不可用"));
        add("cross-region-sync", "跨区域交通态势同步", "广域链路汇聚多 Topic，面向远程指挥同步态势。",
            "通过广域链路同步跨区域交通态势，汇聚车辆、无人机、RSU 和 TrafficRiskTopic。",
            "广域态势感知", Arrays.asList("广域链路", "无人车", "无人机", "RSU"), Arrays.asList("VehicleStatusTopic", "DroneMissionTopic", "RoadSideUnitTopic", "TrafficRiskTopic", "CollaborationPlanTopic"), "RELIABLE_HIGH",
            Arrays.asList("跨区域延迟", "Topic 汇聚压力", "链路抖动"));
    }

    public static ScenarioLibrary getInstance() {
        return INSTANCE;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> scenario : scenarios) copy.add(new LinkedHashMap<String, Object>(scenario));
        return copy;
    }

    public Map<String, Object> get(String id) {
        for (Map<String, Object> scenario : scenarios) {
            if (String.valueOf(scenario.get("scenarioId")).equals(id)) return new LinkedHashMap<String, Object>(scenario);
        }
        return scenarios.isEmpty() ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(scenarios.get(0));
    }

    private void add(String id, String name, String description, String text, String taskType,
                     List<String> devices, List<String> topics, String qos, List<String> risks) {
        Map<String, Object> scenario = new LinkedHashMap<String, Object>();
        scenario.put("scenarioId", id);
        scenario.put("name", name);
        scenario.put("description", description);
        scenario.put("defaultIntentText", text);
        scenario.put("taskType", taskType);
        scenario.put("recommendedDevices", devices);
        scenario.put("requiredTopics", topics);
        scenario.put("qosLevel", qos);
        scenario.put("riskFactors", risks);
        scenario.put("expectedOutput", Arrays.asList("structuredDsl", "routePlan", "taskSchedule", "ddsTrace", "riskAssessment", "executionTimeline"));
        scenarios.add(scenario);
    }
}
