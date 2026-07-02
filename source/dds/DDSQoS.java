package dds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal QoS model used by the virtual DDS message bus.
 */
public class DDSQoS {
    public final String reliability;
    public final int latencyBudgetMs;
    public final int retryLimit;
    public final long deliveryDelayMs;
    public final long timeToLiveMs;
    public final double dropProbability;

    public DDSQoS(String reliability, int latencyBudgetMs, int retryLimit) {
        this(reliability, latencyBudgetMs, retryLimit, 0L, 5_000L, 0.0);
    }

    public DDSQoS(String reliability, int latencyBudgetMs, int retryLimit,
                  long deliveryDelayMs, long timeToLiveMs, double dropProbability) {
        this.reliability = reliability == null || reliability.isBlank() ? "RELIABLE" : reliability;
        this.latencyBudgetMs = latencyBudgetMs <= 0 ? 100 : latencyBudgetMs;
        this.retryLimit = Math.max(0, retryLimit);
        this.deliveryDelayMs = Math.max(0L, deliveryDelayMs);
        this.timeToLiveMs = timeToLiveMs <= 0L ? 5_000L : timeToLiveMs;
        this.dropProbability = Math.max(0.0, Math.min(1.0, dropProbability));
    }

    public static DDSQoS reliable() {
        return new DDSQoS("RELIABLE", 80, 2);
    }

    public static DDSQoS bestEffort() {
        return new DDSQoS("BEST_EFFORT", 250, 0);
    }

    public DDSQoS withDeliveryDelayMs(long delayMs) {
        return new DDSQoS(reliability, latencyBudgetMs, retryLimit, delayMs, timeToLiveMs, dropProbability);
    }

    public DDSQoS withTimeToLiveMs(long ttlMs) {
        return new DDSQoS(reliability, latencyBudgetMs, retryLimit, deliveryDelayMs, ttlMs, dropProbability);
    }

    public DDSQoS withDropProbability(double probability) {
        return new DDSQoS(reliability, latencyBudgetMs, retryLimit, deliveryDelayMs, timeToLiveMs, probability);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("reliability", reliability);
        map.put("latencyBudgetMs", latencyBudgetMs);
        map.put("retryLimit", retryLimit);
        map.put("deliveryDelayMs", deliveryDelayMs);
        map.put("timeToLiveMs", timeToLiveMs);
        map.put("dropProbability", dropProbability);
        return map;
    }
}
