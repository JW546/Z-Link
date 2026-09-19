package collaboration.runtime;

import java.util.ArrayList;
import java.util.List;

public class LocalRuntimeScenario {
    public RuntimeScenarioResult run(long intervalMs, int ticks) throws InterruptedException, java.io.IOException {
        LocalDDSBroker broker = new LocalDDSBroker(0);
        broker.start();
        CoordinatorAgent coordinator = new CoordinatorAgent("127.0.0.1", broker.port(), 3, Math.max(4_000L, intervalMs * ticks + 2_000L));

        List<Thread> threads = new ArrayList<Thread>();
        threads.add(thread("runtime-coordinator", coordinator));
        threads.add(thread("runtime-uav", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.UAV, intervalMs, ticks))));
        threads.add(thread("runtime-ego-car", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.EGO_CAR, intervalMs, ticks))));
        threads.add(thread("runtime-peer-car", new RuntimeDeviceAgent("127.0.0.1", broker.port(),
            RuntimeDeviceAgent.defaultConfig(RuntimeAgentRole.PEER_CAR, intervalMs, ticks))));

        try {
            for (Thread thread : threads) thread.start();
            for (Thread thread : threads) thread.join(Math.max(5_000L, intervalMs * ticks + 3_000L));
            return new RuntimeScenarioResult(broker.history(), coordinator.conflictEvent(), coordinator.finalDecision());
        } finally {
            coordinator.close();
            broker.close();
        }
    }

    private Thread thread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
