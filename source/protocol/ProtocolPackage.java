package protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Protocol Package - Protocol Adapters Collection
 * 
 * Contains:
 * - CANBusAdapter: CAN-Bus Controller Area Network protocol
 * - V2XAdapter: V2X Vehicle-to-Everything communication
 * - ISO_TPAdapter: ISO-TP transport layer protocol
 * - DroneCANAdapter: DroneCAN UAV CAN bus protocol
 * - ProtocolRegistry: Protocol registry center
 */
public class ProtocolPackage {
    
    public static void demoAll() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("       Protocol Adapters - Full Demo");
        System.out.println("=".repeat(50));
        
        System.out.println("\n>>> CAN-Bus Protocol Demo");
        System.out.println("-".repeat(30));
        CANBusAdapter.demo();
        
        System.out.println("\n>>> V2X Protocol Demo");
        System.out.println("-".repeat(30));
        V2XAdapter.demo();
        
        System.out.println("\n>>> ISO-TP Protocol Demo");
        System.out.println("-".repeat(30));
        ISO_TPAdapter.demo();
        
        System.out.println("\n>>> DroneCAN Protocol Demo");
        System.out.println("-".repeat(30));
        DroneCANAdapter.demo();
        
        System.out.println("\n>>> Protocol Registry Demo");
        System.out.println("-".repeat(30));
        demoRegistry();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("       All Demos Complete");
        System.out.println("=".repeat(50) + "\n");
    }
    
    private static void demoRegistry() {
        ProtocolRegistry registry = ProtocolRegistry.getInstance();
        registry.reset();
        registry.registerDefaultAdapters();
        registry.listProtocols();
    }
    
    public static void demoScenario() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  Scenario: Connected Vehicle & Drone Coordination");
        System.out.println("=".repeat(50));
        
        System.out.println("\n[1] Vehicle V2X BSM Broadcast");
        V2XAdapter v2x = new V2XAdapter();
        v2x.updateVehicleState(29.564709, 106.550781, 60.0, 180.0);
        v2x.broadcastBSM();
        
        System.out.println("\n[2] Vehicle Internal CAN Bus");
        CANBusAdapter can = new CANBusAdapter();
        can.sendFrame(can.createStandardFrame(0x100, new byte[]{(byte)0x01, (byte)0x64, 0x00, 0x00}));
        
        System.out.println("\n[3] ISO-TP Diagnostic Upload");
        ISO_TPAdapter iso = new ISO_TPAdapter();
        byte[] diagData = new byte[20];
        for (int i = 0; i < 20; i++) diagData[i] = (byte) i;
        iso.sendMessage(0x7E0, diagData);
        
        System.out.println("\n[4] Drone DroneCAN Communication");
        DroneCANAdapter drone = new DroneCANAdapter(10, "drone-001");
        drone.sendHeartbeat();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  Scenario Complete");
        System.out.println("=".repeat(50) + "\n");
    }
    
    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("scenario")) demoScenario();
            else if (args[0].equals("registry")) demoRegistry();
        } else {
            demoAll();
        }
    }
}
