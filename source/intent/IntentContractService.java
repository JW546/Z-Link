package intent;

import agent.MissionDslModel;
import nlu.DSLGenerator;
import nlu.NLUProcessor;
import nlu.NLUResult;

/**
 * Owns the seam from natural language to a DSL mission contract.
 */
public class IntentContractService {
    private final NLUProcessor nluProcessor;
    private final DSLGenerator dslGenerator;

    public IntentContractService() {
        this(new NLUProcessor(), new DSLGenerator());
    }

    public IntentContractService(NLUProcessor nluProcessor, DSLGenerator dslGenerator) {
        this.nluProcessor = nluProcessor;
        this.dslGenerator = dslGenerator;
    }

    public IntentContract createContract(String naturalLanguage, String missionId) {
        String text = naturalLanguage == null || naturalLanguage.trim().isEmpty()
            ? "尽快去医院，优先安全，避开拥堵"
            : naturalLanguage.trim();
        NLUResult result = nluProcessor.processToResult(text);
        MissionDslModel model = MissionDslModel.fromText(text, missionId);

        IntentContract contract = new IntentContract();
        contract.missionId = model.missionId;
        contract.sourceText = text;
        contract.primaryIntent = result.getPrimaryIntent();
        contract.destination = result.getDestination() == null || result.getDestination().isBlank()
            ? model.target
            : result.getDestination();
        contract.confidence = result.getConfidence();
        contract.dslText = dslGenerator.generateDSL(result);
        contract.structuredDsl = model.toMap();
        return contract;
    }
}
