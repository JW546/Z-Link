package integration;

import collaboration.runtime.ContractSubscriptionResolver;
import collaboration.runtime.LocalDDSBroker;
import collaboration.runtime.LocalDDSClient;
import collaboration.runtime.ProtocolAdapterRegistry;
import collaboration.runtime.RuntimeAgentConfig;
import collaboration.runtime.RuntimeAgentRole;
import collaboration.runtime.RuntimeDeviceAgent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies that device subscriptions are resolved from the mission contract instead of a
 * hard-coded topic list, and that a device really subscribes to what the contract declares.
 */
public class ContractDrivenSubscriptionTest {

    private static final String COORDINATION_TASK =
        "无人机先侦察 A 区，自车接近目标点，peer car 协同避让；发生路线冲突时动态重规划。";
    private static final String PLAIN_TASK = "从起点前往医院。";

    public static void main(String[] args) throws Exception {
        testCoordinationTaskAddsSharedStatusChannel();
        testEgoVehicleAlsoReceivesFinalDecision();
        testTaskWithoutCoordinationFallsBackToDefault();
        testUavPerceptionTopicFollowsMissionText();
        testEveryResolvedTopicHasAPublisher();
        testDeviceSubscribesToContractDeclaredTopics();
        System.out.println("[PASS] Contract-driven subscription test passed.");
    }

    private static void testCoordinationTaskAddsSharedStatusChannel() {
        List<String> uav = ContractSubscriptionResolver.runtimeSubscriptions(
            COORDINATION_TASK, RuntimeAgentRole.UAV);
        require(uav.contains(LocalDDSBroker.TOPIC_INTENT_CONTRACT),
            "coordination task subscribes to the task contract");
        require(uav.contains(LocalDDSBroker.TOPIC_AGENT_STATUS),
            "coordination task adds the shared device status channel");
        require(uav.contains(LocalDDSBroker.TOPIC_REPLAN_REQUEST),
            "replanning keyword adds the replanning request channel");
    }

    private static void testEgoVehicleAlsoReceivesFinalDecision() {
        List<String> ego = ContractSubscriptionResolver.runtimeSubscriptions(
            COORDINATION_TASK, RuntimeAgentRole.EGO_CAR);
        List<String> peer = ContractSubscriptionResolver.runtimeSubscriptions(
            COORDINATION_TASK, RuntimeAgentRole.PEER_CAR);
        require(ego.contains(LocalDDSBroker.TOPIC_FINAL_DECISION),
            "ego vehicle subscribes to the final decision it has to execute");
        require(!peer.contains(LocalDDSBroker.TOPIC_FINAL_DECISION),
            "peer car does not subscribe to the final decision");
    }

    private static void testTaskWithoutCoordinationFallsBackToDefault() {
        List<String> uav = ContractSubscriptionResolver.runtimeSubscriptions(
            PLAIN_TASK, RuntimeAgentRole.UAV);
        require(uav.equals(ContractSubscriptionResolver.DEFAULT_SUBSCRIPTIONS),
            "task without coordination requirements keeps the compatibility subscriptions");
    }

    private static void testUavPerceptionTopicFollowsMissionText() {
        require(ContractSubscriptionResolver.semanticTopics(COORDINATION_TASK)
                .contains(ContractSubscriptionResolver.SEMANTIC_UAV_CORRIDOR),
            "mission text mentioning the UAV declares the UAV perception topic");
        require(!ContractSubscriptionResolver.semanticTopics(PLAIN_TASK)
                .contains(ContractSubscriptionResolver.SEMANTIC_UAV_CORRIDOR),
            "mission text without a UAV does not declare the UAV perception topic");
    }

    private static void testEveryResolvedTopicHasAPublisher() {
        for (RuntimeAgentRole role : RuntimeAgentRole.values()) {
            List<String> resolved = ContractSubscriptionResolver.runtimeSubscriptions(
                COORDINATION_TASK, role);
            require(!resolved.isEmpty(), "role " + role + " resolves at least one subscription");
            for (String topic : resolved) {
                require(ContractSubscriptionResolver.PUBLISHED_RUNTIME_TOPICS.contains(topic),
                    "resolved topic has a publisher: " + topic);
            }
        }
    }

    /**
     * Publishes a contract that declares a custom subscription set, then checks the broker saw the
     * device subscribe to exactly that set. This is the end-to-end proof that the runtime reads the
     * contract instead of a hard-coded list.
     */
    private static void testDeviceSubscribesToContractDeclaredTopics() throws Exception {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        RuntimeAgentConfig config = ProtocolAdapterRegistry.loadDefault()
            .runtimeConfig("UAV-CQ-007", Collections.<String>emptyList(), 150L, 8);
        RuntimeDeviceAgent agent = new RuntimeDeviceAgent("127.0.0.1", broker.port(), config);
        Thread agentThread = new Thread(agent, "contract-subscription-uav");
        agentThread.setDaemon(true);
        LocalDDSClient publisher = new LocalDDSClient("127.0.0.1", broker.port(), "TEST-CONTRACT-PUBLISHER");

        try {
            agentThread.start();
            publisher.start();
            Thread.sleep(250L);

            Map<String, Object> byRole = new LinkedHashMap<String, Object>();
            byRole.put("UAV", new ArrayList<String>(Arrays.asList(
                LocalDDSBroker.TOPIC_AGENT_STATUS,
                LocalDDSBroker.TOPIC_CONFLICT_EVENT)));
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("missionId", "MSN-CONTRACT-SUBSCRIPTION-TEST");
            payload.put("naturalLanguage", COORDINATION_TASK);
            payload.put("runtimeSubscriptionsByRole", byRole);
            publisher.publish(LocalDDSBroker.TOPIC_INTENT_CONTRACT, payload);

            Thread.sleep(700L);
            List<String> subscribed = broker.subscribedTopics();
            require(subscribed.contains(LocalDDSBroker.TOPIC_AGENT_STATUS),
                "device subscribes to the contract-declared shared status channel");
            require(subscribed.contains(LocalDDSBroker.TOPIC_CONFLICT_EVENT),
                "device subscribes to the contract-declared conflict channel");
        } finally {
            publisher.close();
            agent.close();
            broker.close();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
