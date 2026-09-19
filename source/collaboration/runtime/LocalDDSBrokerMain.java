package collaboration.runtime;

public class LocalDDSBrokerMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 19090;
        LocalDDSBroker broker = new LocalDDSBroker(port);
        broker.start();
        System.out.println("[LocalDDSBroker] listening on 127.0.0.1:" + broker.port());
        Runtime.getRuntime().addShutdownHook(new Thread(broker::close));
        Thread.currentThread().join();
    }
}
