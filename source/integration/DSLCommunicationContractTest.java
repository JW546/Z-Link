package integration;

import generated.TailorLexer;
import generated.TailorParser;
import model.DSLVisitor;
import model.Mission;
import model.State;
import nlu.NLUProcessor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Verifies that the project DSL explicitly defines unmanned-traffic
 * data/command exchanges instead of hiding them in opaque string fields.
 */
public class DSLCommunicationContractTest {
    public static void main(String[] args) {
        String input = "\u4e58\u5ba2\u8bf4\u524d\u65b9\u62e5\u5835\uff0c\u8bf7\u65e0\u4eba\u8f66\u8054\u52a8\u540e\u8f66\u3001\u65e0\u4eba\u673a\u548c\u8def\u4fa7\u7cfb\u7edf\u4ea4\u6362\u6570\u636e\uff0c\u7ed9\u51fa\u8def\u7ebf\u5efa\u8bae\u5e76\u89e3\u91ca\u6700\u7ec8\u51b3\u7b56";
        String dsl = new NLUProcessor().process(input);

        require(dsl.contains("dataExchangeContract {"), "DSL has first-class dataExchangeContract");
        require(dsl.contains("dataFlow vehicleTelemetry"), "DSL defines vehicle telemetry data flow");
        require(dsl.contains("dataFlow uavForwardPerception"), "DSL defines UAV perception data flow");
        require(dsl.contains("dataFlow rsuRoadEvent"), "DSL defines RSU road event data flow");
        require(dsl.contains("commandExchangeContract {"), "DSL has first-class commandExchangeContract");
        require(dsl.contains("commandFlow routeAdvice"), "DSL defines route advice command flow");
        require(dsl.contains("commandFlow finalDecision"), "DSL defines final decision command flow");
        require(dsl.contains("ddsIdlBinding {"), "DSL has first-class DDS/IDL binding block");
        require(dsl.contains("binding routeAdvice"), "DSL binds route advice topic to IDL");

        TailorLexer lexer = new TailorLexer(CharStreams.fromString(dsl));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TailorParser parser = new TailorParser(tokens);
        TailorParser.TailorProgramContext tree = parser.tailorProgram();
        require(parser.getNumberOfSyntaxErrors() == 0, "generated DSL parses without syntax errors");

        DSLVisitor visitor = new DSLVisitor();
        Mission mission = (Mission) visitor.visitTailorProgram(tree);
        require(mission != null, "DSL visitor returns mission model");

        State dataContract = findState(mission, "dataExchangeContract");
        State commandContract = findState(mission, "commandExchangeContract");
        State bindingContract = findState(mission, "ddsIdlBinding");

        require(dataContract != null, "model contains data exchange contract");
        require(commandContract != null, "model contains command exchange contract");
        require(bindingContract != null, "model contains DDS/IDL binding contract");

        require("urban/vehicle/telemetry.dsl".equals(dataContract.getFields().get("vehicleTelemetry.topic")),
            "vehicle telemetry topic is machine-readable");
        require("urban.VehicleTelemetry".equals(dataContract.getFields().get("vehicleTelemetry.idl")),
            "vehicle telemetry IDL type is machine-readable");
        require(dataContract.getFields().get("vehicleTelemetry.fields").contains("speed_mps"),
            "vehicle telemetry fields are machine-readable");
        require("RELIABLE_KEEP_LAST_10".equals(commandContract.getFields().get("routeAdvice.qos")),
            "route advice QoS is machine-readable");
        require("urban.RouteDecision".equals(bindingContract.getFields().get("finalDecision.idl_type")),
            "final decision IDL binding is machine-readable");

        System.out.println("[PASS] DSL communication contract test passed.");
    }

    private static State findState(Mission mission, String name) {
        for (State state : mission.getStates()) {
            if (name.equals(state.getName())) {
                return state;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[FAIL] " + message);
        }
        System.out.println("[PASS] " + message);
    }
}
