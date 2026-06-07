package integration;

import nlu.NLUProcessor;

/**
 * 验证协同指令类自然语言是否触发动态协同DSL字段。
 */
public class NLUCollaborationDirectiveTest {

    public static void main(String[] args) {
        String text = "\u6211\u8981\u53bb\u5189\u5bb6\u575d\uff0c\u6211\u7740\u6025\uff0c\u8d39\u7528\u4e0d\u662frontend\u95ee\u9898\uff0c\u4f60\u8981\u7ed9\u5176\u4ed6\u8f66\u5efa\u8bae\uff0c\u8ba9\u4ed6\u4eec\u8ba9\u8def";

        NLUProcessor processor = new NLUProcessor();
        String dsl = processor.process(text);

        boolean hasConversationMode = dsl.contains("conversation_mode");
        boolean hasPeerDirectives = dsl.contains("peer_directives");
        boolean hasPeerTarget = dsl.contains("peer_target_hint");
        boolean hasPeerIntent = dsl.contains("peer_intent");
        boolean hasPeerAdviceTopic = dsl.contains("Vehicle/Peer/Advice");
        boolean hasPeerNegotiationTopic = dsl.contains("Vehicle/Peer/Negotiation");
        boolean hasYieldRequestTopic = dsl.contains("Vehicle/Yield/Request");
        boolean hasYieldAckTopic = dsl.contains("Vehicle/Yield/Ack");

        System.out.println("CHECK conversation_mode=" + hasConversationMode);
        System.out.println("CHECK peer_directives=" + hasPeerDirectives);
        System.out.println("CHECK peer_target_hint=" + hasPeerTarget);
        System.out.println("CHECK peer_intent=" + hasPeerIntent);
        System.out.println("CHECK peer_advice_topic=" + hasPeerAdviceTopic);
        System.out.println("CHECK peer_negotiation_topic=" + hasPeerNegotiationTopic);
        System.out.println("CHECK yield_request_topic=" + hasYieldRequestTopic);
        System.out.println("CHECK yield_ack_topic=" + hasYieldAckTopic);

        if (!(hasConversationMode
            && hasPeerDirectives
            && hasPeerTarget
            && hasPeerIntent
            && hasPeerAdviceTopic
            && hasPeerNegotiationTopic
            && hasYieldRequestTopic
            && hasYieldAckTopic)) {
            System.err.println("[FAIL] 协同指令未完整反映到DSL动态字段/主题。");
            System.exit(1);
        }

        System.out.println("[PASS] 协同指令已映射为动态DSL字段与DDS协同主题。");
    }
}
