package agent;

import java.util.List;
import java.util.Map;

/**
 * Qwen-compatible assistant for explaining an already selected route.
 */
public class QwenRouteDecisionAssistant implements RouteDecisionAssistant {
    private final DeepSeekClient client;

    public QwenRouteDecisionAssistant() {
        this(new DeepSeekClient());
    }

    public QwenRouteDecisionAssistant(DeepSeekClient client) {
        this.client = client;
    }

    @Override
    public RouteDecisionSupport analyze(RouteDecisionRequest request) {
        if (client == null || !client.isEnabled()) {
            return RouteDecisionSupport.unavailable("LLM disabled; deterministic agent explanation used.");
        }

        String prompt = buildPrompt(request);
        DeepSeekClient.DeepSeekSuggestion suggestion = client.suggest(
            prompt,
            "COORDINATION",
            CPSEnums.DeviceRole.VEHICLE,
            "route_decision_explanation_only"
        );
        if (suggestion == null || isBlank(suggestion.summary)) {
            return RouteDecisionSupport.unavailable("LLM unavailable; deterministic agent explanation used.");
        }

        String explanation = suggestion.summary.trim();
        return new RouteDecisionSupport(
            true,
            suggestion.provider,
            suggestion.model,
            explanation,
            explanation,
            clamp(suggestion.confidence),
            suggestion.latencyMs
        );
    }

    private String buildPrompt(RouteDecisionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是无人车路线决策解释助手。最终路线已经由车端 Agent 的规则评分、DDS 证据融合和安全约束确定。");
        prompt.append("你只能解释和辅助分析，不能改变最终路线。\n");
        prompt.append("乘客意图: ").append(request.passengerIntent).append('\n');
        prompt.append("目的地: ").append(request.destination).append('\n');
        prompt.append("已选择路线: ").append(request.selectedRoute).append('\n');
        prompt.append("规则决策理由: ").append(request.ruleReason).append('\n');
        prompt.append("冲突消解: ").append(String.valueOf(request.conflictResolution)).append('\n');
        prompt.append("路线评分: ").append(String.valueOf(request.routeSelection)).append('\n');
        prompt.append("DDS 订阅到的多设备建议:\n");
        appendAdvice(prompt, request.deviceAdvice);
        prompt.append("请用一中文句话解释为什么选择该路线，强调本车最终自主决策、DDS 证据来源和安全约束。");
        return prompt.toString();
    }

    private void appendAdvice(StringBuilder prompt, List<Map<String, Object>> adviceList) {
        if (adviceList == null || adviceList.isEmpty()) {
            prompt.append("- none\n");
            return;
        }
        for (Map<String, Object> item : adviceList) {
            prompt.append("- source=").append(item.get("nodeId"))
                .append(", role=").append(item.get("agentRole"))
                .append(", topic=").append(item.get("topic"))
                .append(", route=").append(item.get("route"))
                .append(", risk=").append(item.get("riskScore"))
                .append(", confidence=").append(item.get("confidence"))
                .append('\n');
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
