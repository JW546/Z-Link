package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 同义词库 - 实现语义对齐
 * 用于处理跨厂商、跨设备的术语不一致问题
 * 场景：无人交通领域的异构设备协同
 */
public class SynonymLibrary {
    // 同义词映射表
    private static final Map<String, SynonymGroup> synonymGroups = new HashMap<>();
    
    // 术语到标准术语的映射
    private static final Map<String, String> termToStandard = new HashMap<>();
    
    static {
        // 初始化同义词库 - 民用无人交通场景
        // 基于Word文档中的低空经济与智能网联汽车融合场景

        // ==================== 速度术语组 ====================
        // 不同厂商设备使用不同术语
        addSynonymGroup("speed", "速度", "速率", "velocity", "行驶速度", "车流速度", "飞行速度", "车速",
            "ground_speed", "air_speed", "vehicle_speed", "drone_speed");

        // ==================== 高度术语组 ====================
        addSynonymGroup("altitude", "高度", "飞行高度", "altitude", "elevation",
            "flight_alt", "drone_alt", "above_ground_level", "agl");

        // ==================== 区域/位置术语组 ====================
        addSynonymGroup("area", "区域", "地区", "zone", "sector", "配送区域", "侦察区域",
            "delivery_zone", "service_area", "patrol_area", "monitoring_zone");

        // ==================== 任务类型术语组 ====================
        // 配送任务相关
        addSynonymGroup("task", "任务", "mission", "operation", "配送任务", "侦察任务",
            "delivery_task", "surveillance_task", "transport_task");
        addSynonymGroup("patrol", "巡逻", "巡视", "配送", "delivery", "logistics", "distribution");
        addSynonymGroup("surveillance", "监视", "监控", "侦察", "观测", "observation", "monitoring");

        // ==================== 设备类型术语组 ====================
        addSynonymGroup("vehicle", "车辆", "车", "vehicle", "car", "automobile",
            "配送车", "物流车", "delivery_vehicle", "ground_vehicle");
        addSynonymGroup("drone", "无人机", "drone", "uav", "quadcopter",
            "配送无人机", "侦察无人机", "commercial_drone", "emergency_drone");

        // ==================== 状态术语组 ====================
        addSynonymGroup("state", "状态", "状况", "condition", "status",
            "device_state", "operational_status", "system_state");
        addSynonymGroup("congestion", "拥堵", "交通拥堵", "traffic_congestion", "congestion_level",
            "heavy_traffic", "traffic_status");

        // ==================== 坐标术语组 ====================
        addSynonymGroup("coordinate", "坐标", "位置", "position", "location", "定位",
            "gps_coordinate", "geo_position", "spatial_coordinate", "waypoint");

        // ==================== 通信协议术语组 ====================
        addSynonymGroup("protocol", "协议", "通信协议", "传输协议", "DDS协议",
            "dds_standard", "communication_protocol", "data_protocol");
        addSynonymGroup("idl", "IDL", "接口定义语言", "interface_definition_language",
            "idl_format", "dds_idl");

        // ==================== 预警类型术语组 ====================
        addSynonymGroup("alert", "预警", "警报", "warning", "alert", "道路预警", "灾害预警",
            "road_alert", "hazard_warning", "emergency_alert", "safety_warning");
        addSynonymGroup("disaster", "灾害", "灾难", "accident", "hazard",
            "road_disaster", "natural_disaster", "traffic_accident");

        // ==================== 交通数据术语组 ====================
        addSynonymGroup("traffic", "交通", "traffic_flow", "transportation",
            "road_traffic", "traffic_condition", "traffic_status");
        addSynonymGroup("weather", "天气", "气象", "weather_condition", "meteorology",
            "climate", "atmospheric_condition");

        // ==================== 电池/电量术语组 ====================
        addSynonymGroup("battery", "电池", "电量", "power", "energy", "battery_level",
            "charge_level", "remaining_power", "drone_battery", "vehicle_battery");

        // ==================== 负载/载重术语组 ====================
        addSynonymGroup("payload", "负载", "载重", "cargo", "load", "freight",
            "delivery_weight", "cargo_weight", "payload_capacity");
        
        // ==================== 导航相关术语组 ====================
        addSynonymGroup("navigation", "导航", "前往", "去", "开到", "开去",
            "navigate", "go_to", "drive_to", "head_to", "route_guidance");
        addSynonymGroup("destination", "目的地", "终点", "目标", "终点站",
            "destination", "end_point", "target_location", "arrival");
        addSynonymGroup("route", "路线", "路径", "航路", "道路",
            "route", "path", "way", "itinerary", "journey");
        addSynonymGroup("highway", "高速", "高速公路", "收费道路", "快速路",
            "highway", "expressway", "toll_road", "freeway", "free_road");
        
        // ==================== 紧急程度术语组 ====================
        addSynonymGroup("urgent", "紧急", "着急", "尽快", "速去", "赶紧",
            "urgent", "emergency", "hurry", "asap", "immediately");
        addSynonymGroup("priority", "优先", "最重要", "首要",
            "priority", "first", "most_important", "primary");
        
        // ==================== 费用/成本术语组 ====================
        addSynonymGroup("cost", "费用", "成本", "开销", "花钱", "收费",
            "expense", "toll", "payment", "price", "fee");
        addSynonymGroup("toll", "过路费", "高速费", "通行费",
            "toll_fee", "road_charge", "passing_fee");
        
        // ==================== 车辆控制术语组 ====================
        addSynonymGroup("start", "启动", "出发", "开始", "开车",
            "start", "begin", "go", "depart", "launch");
        addSynonymGroup("stop", "停车", "停下", "停止", "刹停",
            "stop", "halt", "park", "cease", "brake");
        addSynonymGroup("accelerate", "加速", "加快", "提速",
            "accelerate", "speed_up", "increase_speed");
        addSynonymGroup("decelerate", "减速", "慢点", "降速",
            "decelerate", "slow_down", "decrease_speed");
        
        // ==================== 时间估计术语组 ====================
        addSynonymGroup("eta", "预计到达", "多久到", "还要多久",
            "estimated_arrival", "how_long", "arrival_time");
        addSynonymGroup("distance", "距离", "多远", "路程",
            "distance", "how_far", "range", "journey_length");
    }
    
    /**
     * 添加同义词组
     */
    private static void addSynonymGroup(String standardTerm, String... synonyms) {
        SynonymGroup group = new SynonymGroup(standardTerm, synonyms);
        synonymGroups.put(standardTerm, group);
        for (String synonym : synonyms) {
            termToStandard.put(synonym.toLowerCase(), standardTerm);
        }
        termToStandard.put(standardTerm.toLowerCase(), standardTerm);
    }
    
    /**
     * 将输入术语转换为标准术语
     * 例如：比亚迪车机使用"车速"，大疆无人机使用"飞行速度"，都映射到"speed"
     */
    public static String normalize(String inputTerm) {
        if (inputTerm == null) return null;
        String lowerTerm = inputTerm.toLowerCase();
        return termToStandard.getOrDefault(lowerTerm, inputTerm);
    }
    
    /**
     * 检查两个术语是否语义相同
     */
    public static boolean isSynonym(String term1, String term2) {
        if (term1 == null || term2 == null) return false;
        String normalized1 = normalize(term1);
        String normalized2 = normalize(term2);
        return normalized1.equals(normalized2);
    }
    
    /**
     * 获取指定术语的所有同义词
     */
    public static List<String> getSynonyms(String term) {
        String normalized = normalize(term);
        SynonymGroup group = synonymGroups.get(normalized);
        if (group != null) {
            return group.getAllTerms();
        }
        List<String> result = new ArrayList<>();
        result.add(term);
        return result;
    }
    
    /**
     * 同义词组内部类
     */
    private static class SynonymGroup {
        private String standardTerm;
        private List<String> allTerms;
        
        public SynonymGroup(String standardTerm, String... synonyms) {
            this.standardTerm = standardTerm;
            this.allTerms = new ArrayList<>();
            allTerms.add(standardTerm);
            for (String synonym : synonyms) {
                allTerms.add(synonym);
            }
        }
        
        public List<String> getAllTerms() {
            return allTerms;
        }
        
        public String getStandardTerm() {
            return standardTerm;
        }
    }
}
