package collaboration.ros2;

import collaboration.runtime.LocalDDSClient;
import collaboration.runtime.RuntimeMessage;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ros2LiveBridge implements Closeable {
    private final String host;
    private final int port;
    private final Ros2DdsAdapter adapter;
    private final Ros2CommandExecutor executor;
    private final List<Map<String, Object>> forwarded = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
    private volatile LocalDDSClient client;
    private volatile boolean running;

    public Ros2LiveBridge(String host, int port, Ros2DdsAdapter adapter) {
        this(host, port, adapter, new ProcessRos2CommandExecutor());
    }

    public Ros2LiveBridge(String host, int port, Ros2DdsAdapter adapter, Ros2CommandExecutor executor) {
        this.host = host;
        this.port = port;
        this.adapter = adapter == null ? new Ros2DdsAdapter() : adapter;
        this.executor = executor == null ? new ProcessRos2CommandExecutor() : executor;
    }

    public void start() throws IOException {
        client = new LocalDDSClient(host, port, "ROS2-LIVE-BRIDGE");
        client.onMessage(this::forward);
        client.start();
        for (Ros2TopicBinding binding : adapter.topicBindings()) {
            client.subscribe(binding.semanticTopic);
        }
        running = true;
    }

    public int forwardedCount() {
        return forwarded.size();
    }

    public List<Map<String, Object>> forwardedCommands() {
        synchronized (forwarded) {
            return new ArrayList<Map<String, Object>>(forwarded);
        }
    }

    public boolean awaitForwarded(int expectedCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(1L, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (forwardedCount() >= expectedCount) {
                return true;
            }
            Thread.sleep(25L);
        }
        return forwardedCount() >= expectedCount;
    }

    public void close() {
        running = false;
        LocalDDSClient openClient = client;
        if (openClient != null) {
            openClient.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void forward(RuntimeMessage message) {
        if (!running || message == null || !"publish".equalsIgnoreCase(message.type)) {
            return;
        }
        Map<String, Object> command = adapter.toPublishCommand(message.toMap());
        Object raw = command.get("command");
        if (!(raw instanceof List<?>)) {
            return;
        }
        List<String> commandLine = new ArrayList<String>();
        for (Object part : (List<Object>) raw) {
            commandLine.add(String.valueOf(part));
        }
        Thread publisher = new Thread(() -> executeAndRecord(command, commandLine),
            "ros2-live-publish-" + message.topic.replace("/", "-"));
        publisher.setDaemon(true);
        publisher.start();
    }

    private void executeAndRecord(Map<String, Object> command, List<String> commandLine) {
        try {
            int exitCode = executor.execute(commandLine);
            Map<String, Object> record = new LinkedHashMap<String, Object>(command);
            record.put("exitCode", exitCode);
            record.put("timestamp", System.currentTimeMillis());
            forwarded.add(record);
        } catch (Exception e) {
            Map<String, Object> record = new LinkedHashMap<String, Object>(command);
            record.put("exitCode", -1);
            record.put("error", e.getMessage());
            record.put("timestamp", System.currentTimeMillis());
            forwarded.add(record);
        }
    }
}
