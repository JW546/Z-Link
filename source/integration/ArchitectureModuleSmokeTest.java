package integration;

import agent.AgentAdvice;
import agent.AgentRuntime;
import api.PlatformApiFacade;
import dds.DDSMessage;
import dds.DDSQoS;
import dds.VirtualDDSMessageBus;
import decision.ConflictResolver;
import evaluation.EvaluationService;
import intent.IntentContract;
import intent.IntentContractService;
import planner.RoutePlanningService;
import policy.AgentAction;
import policy.AgentPolicy;
import policy.AgentState;
import policy.PolicyEvaluator;
import policy.PolicyOptimizationService;
import policy.RewardFunction;
import simulation.SimulationScenarioService;
import worldmodel.SystemWorldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smoke test for the refactored architecture seams.
 */
public class ArchitectureModuleSmokeTest {
    public static void main(String[] args) {
        String text = "尽快去医院，优先安全，避开拥堵";

        IntentContract contract = new IntentContractService().createContract(text, "MSN-ARCH-SMOKE");
        require(contract.structuredDsl.containsKey("requiredTopics"), "intent module returns structured DSL");
        require(contract.dslText.contains("dataExchangeContract"), "intent module returns DSL data contract");

        VirtualDDSMessageBus bus = new VirtualDDSMessageBus();
        AtomicInteger delivered = new AtomicInteger();
        bus.subscribe("urban/route/advice.dsl", message -> delivered.incrementAndGet());
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("road_event", "main_road_accident_and_congestion");
        payload.put("risk_score", 0.36);
        payload.put("confidence", 0.86);
        DDSMessage message = bus.publish("urban/route/advice.dsl", "RSU-SCHOOL-009", payload, DDSQoS.reliable());
        require(delivered.get() == 1, "dds module delivers published message");

        List<DDSMessage> messages = new ArrayList<DDSMessage>();
        messages.add(message);
        SystemWorldModel world = SystemWorldModel.fromMessages(messages);
        require(world.congestionLevel > 0.70, "worldmodel module derives congestion from DDS message");

        AgentRuntime runtime = new AgentRuntime();
        List<AgentAdvice> advice = runtime.collectAdvice(text);
        require(advice.size() >= 3, "agent module returns multi-device advice");

        RoutePlanningService planner = new RoutePlanningService();
        require(planner.planDijkstraBaseline("医院").routeId != null, "planner module exposes Dijkstra baseline");
        require(planner.planAdaptive("医院", true, true, world.congestionLevel).routeId != null,
            "planner module returns adaptive route");

        Map<String, Object> decision = new ConflictResolver().resolve(advice, true, true);
        require(Boolean.TRUE.equals(decision.get("hasConflict")), "decision module detects route conflict");
        require(decision.get("finalRoute") != null, "decision module resolves final route");

        SimulationScenarioService simulation = new SimulationScenarioService();
        require(!simulation.listScenarios().isEmpty(), "simulation module exposes scenario library");
        require(simulation.shouldTriggerReplan(world.congestionLevel, true), "simulation module triggers replan");

        Map<String, Object> metrics = new EvaluationService().evaluate(10, 8, 18.5, 0.32, 2, 4, 5);
        require(((Number) metrics.get("successRate")).doubleValue() == 0.8, "evaluation module computes success rate");

        AgentPolicy safePolicy = state -> new AgentAction("REPLAN", "route_D_emergency_green_wave");
        RewardFunction reward = (state, action, outcomeRisk, arrivalMinutes) ->
            1.0 - outcomeRisk - Math.max(0.0, arrivalMinutes - 20.0) / 100.0;
        double policyScore = new PolicyEvaluator().evaluate(safePolicy, reward,
            Arrays.asList(new AgentState("CAR-CQ-001", 0.82, 0.88, 0.30)));
        require(policyScore > 0.0, "policy module evaluates a policy");
        require(new PolicyOptimizationService().chooseBest(Arrays.asList(safePolicy), reward,
            Arrays.asList(new AgentState("CAR-CQ-001", 0.82, 0.88, 0.30))) != null,
            "policy module selects a policy without claiming RL training");

        Map<String, Object> apiPlan = new PlatformApiFacade().generatePlan(text, "MSN-ARCH-API");
        require(apiPlan.containsKey("collaboration") && apiPlan.containsKey("platformPlan"),
            "api module returns existing planning payload");

        System.out.println("[PASS] Architecture module smoke test passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
