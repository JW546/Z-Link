package collaboration.ros2;

public class Ros2InboundBridgeMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 19090;
        Ros2InboundBridge bridge = new Ros2InboundBridge(host, port, new Ros2DdsAdapter());
        Runtime.getRuntime().addShutdownHook(new Thread(bridge::close));
        bridge.start();
        System.out.println("[ROS2-INBOUND] listening ROS2 topics and publishing into local DDS broker at " + host + ":" + port);
        Thread.currentThread().join();
    }
}
