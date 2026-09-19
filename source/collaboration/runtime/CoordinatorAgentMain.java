package collaboration.runtime;

public class CoordinatorAgentMain {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 19090;
        long timeoutMs = args.length > 2 ? Long.parseLong(args[2]) : 60_000L;
        new CoordinatorAgent(host, port, 3, timeoutMs).run();
    }
}
