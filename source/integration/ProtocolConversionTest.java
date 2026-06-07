package integration;

import utils.RealProtocolAdapter;
import protocol.DroneCANAdapter;
import protocol.CompliantBackboneAdapter;
import protocol.CompliantBackboneProtocolConverter;
import protocol.V2XAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * 协议转换测试：验证 DDS / COMPLIANT_BACKBONE / V2X / MAVLink / CAN 的核心转换路径。
 */
public class ProtocolConversionTest {

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void printHeader() {
        System.out.println("============================================================");
        System.out.println("  Protocol Conversion Test");
        System.out.println("============================================================");
    }

    public static void main(String[] args) {
        try {
            printHeader();
            RealProtocolAdapter.initializeProtocolRegistry();

            testDdsToV2xAndBack();
            testDdsToCan();
            testCOMPLIANT_BACKBONEConversion();

            System.out.println("[PASS] ProtocolConversionTest passed.");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("[FAIL] ProtocolConversionTest failed.");
            System.exit(1);
        }
    }

    private static void testDdsToV2xAndBack() {
        System.out.println("[CASE 1] DDS <-> V2X");

        String ddsData = "DDS:topic=urban/vehicle/position,type=VehicleData,id=VEH-001,lat=29.564709,lon=106.550781,speed=36.5,heading=45.0";
        V2XAdapter.BSMMessage bsm = RealProtocolAdapter.convertDDSToV2X(ddsData);

        require(bsm != null, "DDS -> V2X 结果不能为空");
        require(Math.abs(bsm.getLatitude() - 29.564709) < 0.000001, "DDS -> V2X 纬度不正确");
        require(Math.abs(bsm.getLongitude() - 106.550781) < 0.000001, "DDS -> V2X 经度不正确");
        require(Math.abs(bsm.getSpeed() - 36.5) < 0.000001, "DDS -> V2X 速度不正确");

        String roundTripDds = RealProtocolAdapter.convertV2XToDDS(bsm);
        require(roundTripDds != null && roundTripDds.contains("topic=urban/vehicle/position"), "V2X -> DDS 结果不正确");
        require(roundTripDds.contains("lat=29.564709"), "V2X -> DDS 缺少纬度");
        require(roundTripDds.contains("lon=106.550781"), "V2X -> DDS 缺少经度");

        System.out.println("  DDS -> V2X -> DDS OK");
    }

    private static void testDdsToCan() {
        System.out.println("[CASE 2] DDS -> CAN");

        String ddsData = "DDS:topic=vehicle/can/frame,type=VehicleData,drone_id=DRONE-88,alt=128.5";
        try {
            Object converted = RealProtocolAdapter.convertExtendedProtocol("DDS", "CANBUS", ddsData);
            require(converted instanceof protocol.CANBusAdapter.CANFrame, "DDS -> CAN 应返回 CANFrame");
            protocol.CANBusAdapter.CANFrame frame = (protocol.CANBusAdapter.CANFrame) converted;
            require(frame.getCanId() > 0, "CAN ID 应大于0");
            require(frame.getDlc() >= 0, "CAN DLC 应有效");
        } catch (Throwable t) {
            throw new AssertionError("DDS -> CAN 转换失败: " + t.getMessage(), t);
        }

        DroneCANAdapter.DroneCANMessage droneMsg = RealProtocolAdapter.convertMAVLinkToDroneCAN(
            new RealProtocolAdapter.MAVLinkFrame(
                RealProtocolAdapter.createMAVLinkFrame(1, 1, 33, new byte[]{0x01, 0x02, 0x03, 0x04})
            )
        );
        require(droneMsg != null, "MAVLink -> DroneCAN 结果不能为空");
        require(droneMsg.getDataTypeId() == 33, "MAVLink -> DroneCAN 数据类型应保持一致");
        require(droneMsg.getPayload() != null && droneMsg.getPayload().length == 4, "MAVLink -> DroneCAN 载荷长度应为4");

        System.out.println("  DDS -> CAN OK");
        System.out.println("  MAVLink -> DroneCAN OK");
    }

    private static void testCOMPLIANT_BACKBONEConversion() {
        System.out.println("[CASE 3] COMPLIANT_BACKBONE <-> DDS / MAVLink / V2X");

        CompliantBackboneAdapter COMPLIANT_BACKBONE = RealProtocolAdapter.getCompliantBackboneAdapter("protocol-test-001");
        require(COMPLIANT_BACKBONE.connect(), "COMPLIANT_BACKBONE 模拟连接应成功");

        CompliantBackboneAdapter.GlobalPerceptionData data = new CompliantBackboneAdapter.GlobalPerceptionData();
        data.setLatitude(29.5600);
        data.setLongitude(106.5400);
        data.setAltitude(500.0);
        data.setDataSource("satellite");
        data.setPerceptionType("weather");
        data.setContent("{\"visibility\":450,\"fog_density\":0.35}");
        data.setConfidence(92);
        data.setCoverageRadius(50.0);

        String dds = COMPLIANT_BACKBONE.toDDSFormat(data);
        require(dds.contains("perception_type=weather"), "COMPLIANT_BACKBONE -> DDS 应包含 perception_type");
        require(dds.contains("coverage_radius=50.0"), "COMPLIANT_BACKBONE -> DDS 应包含 coverage_radius");

        CompliantBackboneAdapter.GlobalPerceptionData parsed = COMPLIANT_BACKBONE.fromDDSFormat(dds);
        require(Math.abs(parsed.getLatitude() - 29.5600) < 0.000001, "DDS -> COMPLIANT_BACKBONE 纬度不正确");
        require(Math.abs(parsed.getLongitude() - 106.5400) < 0.000001, "DDS -> COMPLIANT_BACKBONE 经度不正确");
        require("weather".equals(parsed.getPerceptionType()), "DDS -> COMPLIANT_BACKBONE 感知类型不正确");

        CompliantBackboneProtocolConverter converter = new CompliantBackboneProtocolConverter(COMPLIANT_BACKBONE);
        CompliantBackboneProtocolConverter.ConversionResult ddsResult = converter.convert("COMPLIANT_BACKBONE", "DDS", data);
        require(ddsResult.isSuccess(), "COMPLIANT_BACKBONE -> DDS 转换应成功");
        require(String.valueOf(ddsResult.getData()).contains("perception_type=weather"), "COMPLIANT_BACKBONE -> DDS 结果应包含 perception_type");

        Map<String, Object> satelliteData = new HashMap<>();
        satelliteData.put("latitude", 29.5600);
        satelliteData.put("longitude", 106.5400);
        satelliteData.put("altitude", 300.0);
        satelliteData.put("source", "satellite");
        satelliteData.put("perception_type", "weather");

        CompliantBackboneProtocolConverter.ConversionResult mavResult = converter.convert("COMPLIANT_BACKBONE", "MAVLink", satelliteData);
        require(mavResult.isSuccess(), "COMPLIANT_BACKBONE -> MAVLink 转换应成功");
        require(String.valueOf(mavResult.getData()).contains("msg_id=33"), "COMPLIANT_BACKBONE -> MAVLink 结果应包含 msg_id");

        CompliantBackboneProtocolConverter.ConversionResult v2xResult = converter.convert("COMPLIANT_BACKBONE", "V2X", data);
        require(v2xResult.isSuccess(), "COMPLIANT_BACKBONE -> V2X 转换应成功");
        require(String.valueOf(v2xResult.getData()).contains("BSM:"), "COMPLIANT_BACKBONE -> V2X 结果应包含 BSM");

        COMPLIANT_BACKBONE.disconnect();
        System.out.println("  COMPLIANT_BACKBONE -> DDS / MAVLink / V2X OK");
    }
}