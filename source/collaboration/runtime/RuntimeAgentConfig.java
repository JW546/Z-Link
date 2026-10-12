package collaboration.runtime;

import java.util.ArrayList;
import java.util.List;

public class RuntimeAgentConfig {
    public final String agentId;
    public final RuntimeAgentRole role;
    public final DeviceProtocolAdapter adapter;
    public final String initialRoute;
    /**
     * Runtime topics this device subscribes to, resolved from the mission contract.
     * An empty list means "use the compatibility defaults" (see RuntimeDeviceAgent).
     */
    public final List<String> subscriptions;
    public final long intervalMs;
    public final int ticks;

    public RuntimeAgentConfig(String agentId, RuntimeAgentRole role, DeviceProtocolAdapter adapter,
                              String initialRoute, long intervalMs, int ticks) {
        this(agentId, role, adapter, initialRoute, new ArrayList<String>(), intervalMs, ticks);
    }

    public RuntimeAgentConfig(String agentId, RuntimeAgentRole role, DeviceProtocolAdapter adapter,
                              String initialRoute, List<String> subscriptions, long intervalMs, int ticks) {
        this.agentId = agentId;
        this.role = role;
        this.adapter = adapter;
        this.initialRoute = initialRoute;
        this.subscriptions = subscriptions == null
            ? new ArrayList<String>() : new ArrayList<String>(subscriptions);
        this.intervalMs = intervalMs <= 0L ? 500L : intervalMs;
        this.ticks = ticks <= 0 ? 6 : ticks;
    }
}
