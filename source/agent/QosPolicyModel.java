package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DDS QoS 策略模型。
 *
 * 用于把可靠性、持久性、时限、延迟预算、历史深度、优先级和降级策略统一成结构化对象，
 * 供平台编排、通信模拟和前端展示复用。
 */
public class QosPolicyModel {
    public String topic;
    public String reliability;
    public String durability;
    public int deadlineMs;
    public int latencyBudgetMs;
    public int historyDepth;
    public int priority;
    public int retryLimit;
    public String fallbackPolicy;
    public String reason;

    public static QosPolicyModel create(String topic, boolean critical, String reason) {
        QosPolicyModel policy = new QosPolicyModel();
        policy.topic = topic == null || topic.isBlank() ? "CollaborationPlanTopic" : topic;
        policy.reliability = critical ? "RELIABLE" : "BEST_EFFORT";
        policy.durability = critical ? "TRANSIENT_LOCAL" : "VOLATILE";
        policy.deadlineMs = critical ? 300 : 1000;
        policy.latencyBudgetMs = critical ? 80 : 250;
        policy.historyDepth = critical ? 10 : 3;
        policy.priority = critical ? 9 : 5;
        policy.retryLimit = critical ? 2 : 1;
        policy.fallbackPolicy = critical
            ? "重试后切换广域链路摘要同步，并保留最近协同计划"
            : "等待下一周期样本，必要时提升为可靠 Topic";
        policy.reason = reason == null || reason.isBlank()
            ? "按任务紧急程度和 Topic 业务关键性动态选择 QoS。"
            : reason;
        return policy;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("topic", topic);
        map.put("reliability", reliability);
        map.put("durability", durability);
        map.put("deadlineMs", deadlineMs);
        map.put("latencyBudgetMs", latencyBudgetMs);
        map.put("historyDepth", historyDepth);
        map.put("priority", priority);
        map.put("retryLimit", retryLimit);
        map.put("fallbackPolicy", fallbackPolicy);
        map.put("reason", reason);
        return map;
    }
}
