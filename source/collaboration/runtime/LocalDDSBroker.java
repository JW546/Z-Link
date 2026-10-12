package collaboration.runtime;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local TCP publish/subscribe broker. It is intentionally small so it can be
 * replaced by ROS 2/DDS without changing Agent logic.
 */
public class LocalDDSBroker implements Closeable {
    public static final String TOPIC_INTENT_CONTRACT = "runtime/intent/contract";
    public static final String TOPIC_AGENT_STATUS = "runtime/agent/status";
    public static final String TOPIC_PROTOCOL_FRAME = "runtime/protocol/native_frame";
    public static final String TOPIC_CONFLICT_EVENT = "runtime/conflict/event";
    public static final String TOPIC_REPLAN_REQUEST = "runtime/replanning/request";
    public static final String TOPIC_FINAL_DECISION = "runtime/decision/final";
    public static final String TOPIC_POLICY_EVALUATION = "runtime/policy/evaluation";

    private static final Gson GSON = new Gson();

    private final int requestedPort;
    private final Map<String, Set<ClientSession>> subscribers = new ConcurrentHashMap<String, Set<ClientSession>>();
    private final List<Map<String, Object>> history = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
    private final List<ClientSession> sessions = new CopyOnWriteArrayList<ClientSession>();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public LocalDDSBroker(int port) {
        this.requestedPort = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "local-dds-broker-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int port() {
        return serverSocket == null ? requestedPort : serverSocket.getLocalPort();
    }

    public List<Map<String, Object>> history() {
        synchronized (history) {
            return new ArrayList<Map<String, Object>>(history);
        }
    }

    /**
     * Runtime topic names that currently have at least one subscriber. Used to verify that a
     * contract-resolved subscription actually reached the broker instead of being dropped.
     */
    public List<String> subscribedTopics() {
        List<String> topics = new ArrayList<String>();
        for (Map.Entry<String, Set<ClientSession>> entry : subscribers.entrySet()) {
            Set<ClientSession> handlers = entry.getValue();
            synchronized (handlers) {
                if (!handlers.isEmpty()) {
                    topics.add(entry.getKey());
                }
            }
        }
        Collections.sort(topics);
        return topics;
    }

    public void close() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        for (ClientSession session : sessions) {
            session.close();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                ClientSession session = new ClientSession(serverSocket.accept());
                sessions.add(session);
                Thread thread = new Thread(session::readLoop, "local-dds-client-" + sessions.size());
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (running) {
                    throw new IllegalStateException("Local DDS broker accept failed.", e);
                }
            }
        }
    }

    private void handle(ClientSession session, RuntimeMessage message) {
        if ("subscribe".equalsIgnoreCase(message.type)) {
            subscribers.computeIfAbsent(message.topic, ignored -> Collections.synchronizedSet(new LinkedHashSet<ClientSession>()))
                .add(session);
            return;
        }
        if (!"publish".equalsIgnoreCase(message.type)) {
            return;
        }
        history.add(message.toMap());
        Set<ClientSession> handlers = subscribers.get(message.topic);
        if (handlers == null) {
            return;
        }
        List<ClientSession> copy;
        synchronized (handlers) {
            copy = new ArrayList<ClientSession>(handlers);
        }
        for (ClientSession handler : copy) {
            handler.send(message);
        }
    }

    private class ClientSession implements Closeable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void readLoop() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    RuntimeMessage message = GSON.fromJson(line, RuntimeMessage.class);
                    if (message != null) {
                        handle(this, message);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
        }

        synchronized void send(RuntimeMessage message) {
            try {
                writer.write(GSON.toJson(message));
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {
                close();
            }
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
