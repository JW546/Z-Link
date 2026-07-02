package intent;

import java.util.LinkedHashMap;
import java.util.Map;

import nlu.Intent;

/**
 * Natural-language intent plus its DSL task contract.
 */
public class IntentContract {
    public String missionId;
    public String sourceText;
    public Intent primaryIntent;
    public String destination;
    public double confidence;
    public String dslText;
    public Map<String, Object> structuredDsl = new LinkedHashMap<String, Object>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("missionId", missionId);
        map.put("sourceText", sourceText);
        map.put("primaryIntent", primaryIntent == null ? "UNKNOWN" : primaryIntent.name());
        map.put("destination", destination);
        map.put("confidence", confidence);
        map.put("dslText", dslText);
        map.put("structuredDsl", structuredDsl);
        return map;
    }
}
