package integration;

import agent.CollaborationDecisionEngine;
import agent.RouteDecisionAssistant;

import java.util.Arrays;
import java.util.Map;

/**
 * Verifies that the LLM is used as an explanation/analysis assistant, not as
 * the route authority.
 */
public class AgentQwenRouteDecisionAssistTest {
    public static void main(String[] args) {
        CollaborationDecisionEngine engine = new CollaborationDecisionEngine(new FakeAssistant());
        Map<String, Object> result = engine.evaluate("urgent congestion but keep safety first", "MSN-LLM-001");
        Map<String, Object> decision = map(result.get("finalDecision"));

        require(Boolean.TRUE.equals(decision.get("llmAssisted")), "LLM assistant is attached to final decision");
        require("fake-qwen-compatible".equals(decision.get("llmProvider")), "LLM provider metadata is exposed");
        require("explanation_intent_completion_auxiliary_analysis_only".equals(decision.get("llmRole")),
            "LLM role is explicitly bounded");
        require("vehicle_agent_rule_score_safety_constraint".equals(decision.get("decisionAuthority")),
            "vehicle Agent remains the decision authority");
        require(Arrays.asList("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave")
                .contains(String.valueOf(decision.get("route"))),
            "route remains selected from deterministic candidate routes");
        require(String.valueOf(decision.get("passengerExplanation")).contains("DDS evidence"),
            "passenger explanation uses LLM-assisted analysis");
        require(String.valueOf(decision.get("ruleBasedReason")).length() > 0,
            "rule-based reason is preserved");

        System.out.println("[PASS] Agent Qwen route decision assist test passed.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }

    private static final class FakeAssistant implements RouteDecisionAssistant {
        @Override
        public RouteDecisionSupport analyze(RouteDecisionRequest request) {
            require(!request.selectedRoute.isEmpty(), "assistant receives selected route");
            require(!request.deviceAdvice.isEmpty(), "assistant receives DDS device advice");
            return new RouteDecisionSupport(
                true,
                "fake-qwen-compatible",
                "qwen-test",
                "DDS evidence supports the already selected vehicle route.",
                "DDS evidence supports the already selected vehicle route.",
                0.91,
                12
            );
        }
    }
}
