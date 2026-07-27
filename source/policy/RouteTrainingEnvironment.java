package policy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Small simulation environment for route-decision policy training.
 */
public class RouteTrainingEnvironment implements PolicyEnvironment {
    private final DefaultRewardFunction rewardFunction = new DefaultRewardFunction();
    private final Random random;
    private final int maxSteps;
    private AgentState currentState;
    private int stepIndex;
    private int episodeIndex;

    public RouteTrainingEnvironment() {
        this(20260712L, 4);
    }

    public RouteTrainingEnvironment(long seed, int maxSteps) {
        this.random = new Random(seed);
        this.maxSteps = Math.max(1, maxSteps);
    }

    @Override
    public AgentState reset() {
        stepIndex = 0;
        episodeIndex++;
        currentState = scenarioState(episodeIndex);
        return currentState;
    }

    @Override
    public StepResult step(AgentAction action) {
        if (currentState == null) {
            reset();
        }
        AgentAction safeAction = action == null ? AgentAction.waitForMoreDDSMessages("missing action") : action;
        Map<String, Object> outcome = outcomeFor(currentState, safeAction);
        DefaultRewardFunction.RewardBreakdown reward = rewardFunction.evaluate(currentState, safeAction, outcome);
        stepIndex++;
        boolean done = stepIndex >= maxSteps || reward.hardSafetyViolation || bool(outcome, "arrived", false);
        AgentState next = transition(currentState, safeAction, outcome);

        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("outcome", outcome);
        info.put("rewardBreakdown", reward.toMap());
        info.put("stateKey", RouteTrainingStateEncoder.encode(currentState));
        info.put("actionKey", RouteTrainingActionSpace.key(safeAction));
        currentState = next;
        return new StepResult(next, reward.totalReward, done, info);
    }

    @Override
    public List<String> agentIds() {
        return Arrays.asList("CAR-CQ-001");
    }

    @Override
    public boolean isTerminal() {
        return stepIndex >= maxSteps;
    }

    @Override
    public Map<String, Object> metadata() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("environment", "RouteTrainingEnvironment");
        map.put("source", "synthetic DDS route-advice simulation");
        map.put("maxSteps", maxSteps);
        map.put("trainingScope", "single vehicle route decision prototype");
        return map;
    }

    private AgentState scenarioState(int index) {
        double congestion = 0.18 + random.nextDouble() * 0.78;
        double risk = 0.12 + congestion * 0.62 + random.nextDouble() * 0.20;
        double confidence = 0.35 + random.nextDouble() * 0.62;
        double freshness = 0.45 + random.nextDouble() * 0.52;
        double trust = 0.48 + random.nextDouble() * 0.48;
        boolean urgent = index % 3 == 0 || random.nextDouble() > 0.72;
        boolean safetyFirst = risk > 0.58 || random.nextDouble() > 0.66;
        boolean conflict = risk > 0.62 || (trust > 0.82 && confidence < 0.55);

        AgentState.Builder builder = AgentState.builder("CAR-CQ-001")
            .passengerIntent((urgent ? "urgent " : "") + (safetyFirst ? "safe " : "") + "route collaboration")
            .currentRoute("route_A_main_road")
            .candidateRoutes(Arrays.asList("route_A_main_road", "route_C_elevated_detour", "route_D_emergency_green_wave"))
            .congestionLevel(congestion)
            .safetyRisk(Math.min(0.98, risk))
            .messageConfidence(confidence)
            .messageFreshness(freshness)
            .sourceTrust(trust)
            .passengerUrgent(urgent)
            .passengerSafetyFirst(safetyFirst)
            .conflictStatus(conflict)
            .replanRequired(conflict || congestion > 0.74);
        builder.addDdsMessage(message("urban/uav/aerial_perception.dsl", "route_C_elevated_detour", confidence, trust, risk));
        builder.addDdsMessage(message("urban/rsu/road_event.dsl", "route_D_emergency_green_wave", Math.min(0.98, confidence + 0.08), Math.min(0.98, trust + 0.06), Math.max(0.08, risk - 0.12)));
        return builder.build();
    }

    private Map<String, Object> message(String topic, String route, double confidence, double trust, double risk) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("topic", topic);
        map.put("route", route);
        map.put("confidence", round(confidence));
        map.put("sourceTrust", round(trust));
        map.put("riskScore", round(risk));
        map.put("ddsDelayMs", 80 + random.nextInt(320));
        return map;
    }

    private Map<String, Object> outcomeFor(AgentState state, AgentAction action) {
        double arrival = 18.0 + state.congestionLevel * 8.0;
        double risk = state.safetyRisk;
        int replans = 0;
        boolean conflictHandled = !state.conflictStatus;

        switch (action.type) {
            case SWITCH_TO_SAFETY_PRIORITY:
                risk -= 0.18;
                arrival += 2.2;
                conflictHandled = true;
                break;
            case REQUEST_REPLANNING:
                risk -= 0.14;
                arrival += 1.4;
                replans = 1;
                conflictHandled = true;
                break;
            case SWITCH_TO_TIME_PRIORITY:
                arrival -= 2.4;
                risk += state.passengerSafetyFirst ? 0.18 : 0.08;
                break;
            case CHOOSE_ROUTE:
                if ("route_D_emergency_green_wave".equals(action.route)) {
                    arrival -= state.passengerUrgent ? 2.0 : 0.5;
                    risk -= 0.08;
                } else if ("route_C_elevated_detour".equals(action.route)) {
                    risk -= 0.13;
                    arrival += 0.8;
                } else {
                    risk += state.congestionLevel > 0.65 ? 0.16 : 0.02;
                }
                conflictHandled = state.conflictStatus ? !"route_A_main_road".equals(action.route) : true;
                break;
            default:
                arrival += 3.0;
                risk += 0.02;
                break;
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("arrived", risk < 0.88 && arrival <= 30.0);
        map.put("arrivalMinutes", round(Math.max(1.0, arrival)));
        map.put("safetyRisk", round(clamp(risk)));
        map.put("congestionLevel", state.congestionLevel);
        map.put("conflictHandled", conflictHandled);
        map.put("explanationComplete", action.reason != null && action.reason.length() >= 12);
        map.put("replanningCount", replans);
        map.put("trustedLowConfidenceMessage", state.messageConfidence < 0.45 && action.type == AgentAction.Type.CHOOSE_ROUTE);
        map.put("ignoredHighTrustRiskMessage", state.sourceTrust > 0.85 && state.safetyRisk > 0.65 && !conflictHandled);
        return map;
    }

    private AgentState transition(AgentState state, AgentAction action, Map<String, Object> outcome) {
        return AgentState.builder(state.agentId)
            .passengerIntent(state.passengerIntent)
            .currentRoute(action.route == null || action.route.isEmpty() ? state.currentRoute : action.route)
            .candidateRoutes(state.candidateRoutes)
            .congestionLevel(Math.max(0.05, state.congestionLevel - 0.08))
            .safetyRisk(number(outcome, "safetyRisk", state.safetyRisk))
            .messageConfidence(Math.min(1.0, state.messageConfidence + 0.03))
            .messageFreshness(Math.max(0.35, state.messageFreshness - 0.05))
            .sourceTrust(state.sourceTrust)
            .passengerUrgent(state.passengerUrgent)
            .passengerSafetyFirst(state.passengerSafetyFirst)
            .conflictStatus(state.conflictStatus && !bool(outcome, "conflictHandled", false))
            .replanRequired(false)
            .build();
    }

    private double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
