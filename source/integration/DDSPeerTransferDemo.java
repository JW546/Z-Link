package integration;

import dds.DDSManager;
import urban.VehicleData;

import java.util.Locale;

/**
 * DDS host/peer transfer demo.
 *
 * 用法：
 * - 接收端：java integration.DDSPeerTransferDemo receive
 * - 发送端：java integration.DDSPeerTransferDemo send
 *
 * 目标：在两台设备上验证同一 DDS 域内的跨设备收发。
 */
public class DDSPeerTransferDemo {

    private static final int DEFAULT_DOMAIN_ID = 0;
    private static final String TOPIC_NAME = "urban/vehicle/data";

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "send";
        int domainId = args.length > 1 ? parseInt(args[1], DEFAULT_DOMAIN_ID) : DEFAULT_DOMAIN_ID;

        if ("receive".equals(mode) || "sub".equals(mode) || "listener".equals(mode)) {
            runReceiver(domainId);
        } else {
            runSender(domainId);
        }
    }

    private static void runReceiver(int domainId) {
        System.out.println("============================================================");
        System.out.println("  DDS Peer Receiver");
        System.out.println("  Topic: " + TOPIC_NAME);
        System.out.println("  Domain: " + domainId);
        System.out.println("============================================================");

        DDSManager dds = DDSManager.getInstance(domainId);
        if (!dds.connect()) {
            System.err.println("[FAIL] DDS connection failed, receiver cannot start");
            System.exit(1);
        }

        System.out.println("[INFO ] Receiver started, waiting for remote VehicleData");
        dds.subscribeVehicleData(DDSPeerTransferDemo::printVehicleData);

        System.out.println("[READY] Subscribed to " + TOPIC_NAME + ", waiting for remote sender...");
        System.out.println("[INFO ] Press Ctrl+C to stop the receiver");

        keepAlive();
    }

    private static void runSender(int domainId) {
        System.out.println("============================================================");
        System.out.println("  DDS Peer Sender");
        System.out.println("  Topic: " + TOPIC_NAME);
        System.out.println("  Domain: " + domainId);
        System.out.println("============================================================");

        DDSManager dds = DDSManager.getInstance(domainId);
        if (!dds.connect()) {
            System.err.println("[FAIL] DDS connection failed, sender cannot start");
            System.exit(1);
        }

        System.out.println("[READY] Sender will publish VehicleData to " + TOPIC_NAME);
        System.out.println("[INFO ] Start receive mode on the other device first");

        for (int i = 1; i <= 10; i++) {
            VehicleData data = createSampleVehicleData(i);
            long seq = dds.publishVehicleData(data);
            System.out.println("[SEND] seq=" + seq + " vehicle_id=" + data.vehicle_id +
                " speed=" + String.format(Locale.ROOT, "%.1f", data.speed) +
                " heading=" + String.format(Locale.ROOT, "%.1f", data.heading));

            sleepSilently(1000);
        }

        dds.disconnect();
        System.out.println("[PASS] Sender published 10 VehicleData samples");
    }

    private static VehicleData createSampleVehicleData(int index) {
        VehicleData data = new VehicleData();
        data.vehicle_id = String.format(Locale.ROOT, "DEMO-VEHICLE-%03d", index);
        data.latitude = 29.5582 + index * 0.0001;
        data.longitude = 106.5549 + index * 0.0001;
        data.altitude = 300.0 + index;
        data.speed = 12.5 + index * 0.6;
        data.heading = 45.0 + index * 3.0;
        data.acceleration = 0.2 + index * 0.05;
        data.battery_level = Math.max(20, 85 - index);
        data.drive_mode = "DDS_TRANSFER_DEMO";
        data.timestamp = (int) (System.currentTimeMillis() / 1000);
        return data;
    }

    private static void printVehicleData(VehicleData data) {
        System.out.println("[RECV ✓] Received data from remote device");
        System.out.println("[RECV] vehicle_id=" + data.vehicle_id
            + " lat=" + String.format(Locale.ROOT, "%.6f", data.latitude)
            + " lon=" + String.format(Locale.ROOT, "%.6f", data.longitude)
            + " speed=" + String.format(Locale.ROOT, "%.1f", data.speed)
            + " heading=" + String.format(Locale.ROOT, "%.1f", data.heading)
            + " battery=" + data.battery_level
            + " mode=" + data.drive_mode);
    }

    private static void keepAlive() {
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}