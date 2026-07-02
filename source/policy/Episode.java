package policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Episode {
    public final String episodeId;
    public final String scenarioId;
    public final List<ReplayRecord> records = new ArrayList<ReplayRecord>();
    public final Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    public Episode(String episodeId, String scenarioId) {
        this.episodeId = episodeId == null || episodeId.isBlank() ? "EP-" + System.currentTimeMillis() : episodeId;
        this.scenarioId = scenarioId == null ? "" : scenarioId;
        metadata.put("trainingStatus", "interface_only_no_rl_training_data");
    }

    public void add(ReplayRecord record) {
        if (record != null) records.add(record);
    }

    public double totalReward() {
        double total = 0.0;
        for (ReplayRecord record : records) total += record.reward;
        return total;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> replay = new ArrayList<Map<String, Object>>();
        for (ReplayRecord record : records) replay.add(record.toMap());
        map.put("episodeId", episodeId);
        map.put("scenarioId", scenarioId);
        map.put("records", replay);
        map.put("totalReward", totalReward());
        map.put("metadata", metadata);
        return map;
    }
}
