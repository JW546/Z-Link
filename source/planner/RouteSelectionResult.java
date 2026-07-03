package planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteSelectionResult {
    public final CandidateRoute selectedRoute;
    public final List<RouteScore> routeScores;
    public final boolean replanningTriggered;
    public final String explanation;
    public final List<String> influencingMessages;

    public RouteSelectionResult(CandidateRoute selectedRoute, List<RouteScore> routeScores,
                                boolean replanningTriggered, String explanation,
                                List<String> influencingMessages) {
        this.selectedRoute = selectedRoute;
        this.routeScores = routeScores == null ? new ArrayList<RouteScore>() : new ArrayList<RouteScore>(routeScores);
        this.replanningTriggered = replanningTriggered;
        this.explanation = explanation == null ? "" : explanation;
        this.influencingMessages = influencingMessages == null ? new ArrayList<String>() : new ArrayList<String>(influencingMessages);
    }

    public String routeId() {
        return selectedRoute == null ? "" : selectedRoute.routeId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("selectedRoute", selectedRoute == null ? new LinkedHashMap<String, Object>() : selectedRoute.toMap());
        List<Map<String, Object>> scores = new ArrayList<Map<String, Object>>();
        for (RouteScore score : routeScores) scores.add(score.toMap());
        map.put("routeScores", scores);
        map.put("replanningTriggered", replanningTriggered);
        map.put("explanation", explanation);
        map.put("influencingMessages", influencingMessages);
        return map;
    }
}
