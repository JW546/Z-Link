package collaboration.runtime;

public class RuntimeAgentConfig {
    public final String agentId;
    public final RuntimeAgentRole role;
    public final DeviceProtocolAdapter adapter;
    public final String initialRoute;
    public final long intervalMs;
    public final int ticks;

    public RuntimeAgentConfig(String agentId, RuntimeAgentRole role, DeviceProtocolAdapter adapter,
                              String initialRoute, long intervalMs, int ticks) {
        this.agentId = agentId;
        this.role = role;
        this.adapter = adapter;
        this.initialRoute = initialRoute;
        this.intervalMs = intervalMs <= 0L ? 500L : intervalMs;
        this.ticks = ticks <= 0 ? 6 : ticks;
    }
}
