package nlu;

/**
 * 意图类型枚举
 * 定义车辆能理解的所有用户意图
 */
public enum Intent {
    // 导航相关
    NAVIGATION("导航", "前往", "去", "开去", "开到", "导航到"),
    DELIVERY("配送", "送达", "送货", "运送", "投递", "医疗物资"),
    COORDINATION("协同", "联动", "邻车", "后车", "前车", "无人机建议", "路线建议", "合规广域链路", "5G专网", "北斗短报文"),
    EMERGENCY("紧急", "着急", "尽快", "快点", "速去", "快"),
    COST_AWARE("费用", "成本", "钱", "贵", "便宜", "高速", "不走高速"),
    QUERY_STATUS("状态", "电量", "到哪里", "位置", "还有多久"),
    CONTROL("停车", "减速", "加速", "充电", "空调", "窗户"),
    
    // 紧急程度
    URGENCY_HIGH("紧急", "着急", "尽快", "快点", "速"),
    URGENCY_MEDIUM("不急", "慢慢", "普通"),
    URGENCY_LOW("随便", "无所谓"),
    
    // 路线偏好
    PREFER_HIGHWAY("高速", "收费", "快", "最快"),
    PREFER_ECONOMY("普通", "省", "不收费", "经济"),
    PREFER_SCENIC("风景", "景色", "绕", "好看"),
    
    // 目的地类型
    DESTINATION_SPECIFIC("西南大学", "机场", "医院", "火车站", "商场"),
    DESTINATION_VAGUE("家", "公司", "学校", "附近"),
    
    // 车辆控制
    VEHICLE_START("启动", "出发", "开始", "开车"),
    VEHICLE_STOP("停车", "停下", "结束", "到达"),
    VEHICLE_MODE("模式", "自动驾驶", "手动", "辅助");

    private final String[] keywords;

    Intent(String... keywords) {
        this.keywords = keywords;
    }

    public String[] getKeywords() {
        return keywords;
    }

    /**
     * 检查文本是否包含此意图的关键词
     */
    public boolean matches(String text) {
        if (text == null) return false;
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
