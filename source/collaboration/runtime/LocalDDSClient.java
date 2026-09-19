package collaboration.runtime;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public class LocalDDSClient implements Closeable {
    private static final Gson GSON = new Gson();

    private final String nodeId;
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private volatile Consumer<RuntimeMessage> listener;
    private volatile boolean running;
    private Thread readerThread;

    public LocalDDSClient(String host, int port, String nodeId) throws IOException {
        this.nodeId = nodeId;
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public void onMessage(Consumer<RuntimeMessage> listener) {
        this.listener = listener;
    }

    public void start() {
        running = true;
        readerThread = new Thread(this::readLoop, nodeId + "-dds-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void subscribe(String topic) {
        send(RuntimeMessage.subscribe(topic, nodeId));
    }

    public void publish(String topic, Map<String, Object> payload) {
        send(RuntimeMessage.publish(topic, nodeId, payload));
    }

    public void publishFrom(String topic, String sourceId, Map<String, Object> payload) {
        send(RuntimeMessage.publish(topic, sourceId, payload));
    }

    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                RuntimeMessage message = GSON.fromJson(line, RuntimeMessage.class);
                Consumer<RuntimeMessage> handler = listener;
                if (message != null && handler != null) {
                    handler.accept(message);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private synchronized void send(RuntimeMessage message) {
        try {
            writer.write(GSON.toJson(message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send local DDS message from " + nodeId, e);
        }
    }
}
