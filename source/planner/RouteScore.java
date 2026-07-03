package planner;

import java.util.LinkedHashMap;
import java.util.Map;

public class RouteScore {
    public final CandidateRoute route;
    public final double score;
    public final boolean rejected;
    public final String rejectionReason;

    public RouteScore(CandidateRoute route, double score, boolean rejected, String rejectionReason) {
        this.route = route;
        this.score = score;
        this.rejected = rejected;
        this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>(route.toMap());
        map.put("score", score);
        map.put("rejected", rejected);
        map.put("rejectionReason", rejectionReason);
        return map;
    }
}
