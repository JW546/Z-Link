package planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlanningResult {
    public String routeId;
    public String algorithm;
    public double score;
    public boolean rejectedShortestRoute;
    public String explanation;
    public List<Map<String, Object>> candidateScores = new ArrayList<Map<String, Object>>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("routeId", routeId);
        map.put("algorithm", algorithm);
        map.put("score", score);
        map.put("rejectedShortestRoute", rejectedShortestRoute);
        map.put("explanation", explanation);
        map.put("candidateScores", candidateScores);
        return map;
    }
}
