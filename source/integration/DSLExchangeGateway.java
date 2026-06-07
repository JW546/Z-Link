package integration;

import dds.DDSManager;
import dds.IDLDataConverter;
import generated.TailorLexer;
import generated.TailorParser;
import model.DSLVisitor;
import model.Mission;
import nlu.DSLGenerator;
import nlu.NLUProcessor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * DSL 统一交换网关。
 *
 * 负责把外界信息转换为 DSL，再把 DSL 转回项目内部领域模型，
 * 并可进一步映射为 IDL 数据后通过 DDS 传输。
 *
 * 这层类的作用是把分散在 NLU、ANTLR、领域模型、IDL、DDS 的能力收口成统一入口。
 */
public class DSLExchangeGateway {

    private final NLUProcessor nluProcessor;
    private final DSLGenerator dslGenerator;
    private final DSLIDLMapper dslidlMapper;
    private final IDLDataConverter idlDataConverter;

    public DSLExchangeGateway() {
        this.nluProcessor = new NLUProcessor();
        this.dslGenerator = new DSLGenerator();
        this.dslidlMapper = DSLIDLMapper.getInstance();
        this.idlDataConverter = IDLDataConverter.getInstance();
    }

    /**
     * 将外部自然语言信息转换为 DSL。
     */
    public String externalInfoToDsl(String externalText) {
        return nluProcessor.process(externalText);
    }

    /**
     * 先识别自然语言，再返回完整结果对象。
     */
    public String externalInfoToDslByResult(String externalText) {
        return dslGenerator.generateDSL(nluProcessor.processToResult(externalText));
    }

    /**
     * 将 DSL 解析回项目内部语言，也就是 Mission / Task / PathPlan / State 这套领域模型。
     */
    public Mission dslToMission(String dslCode) {
        try {
            TailorLexer lexer = new TailorLexer(CharStreams.fromString(dslCode));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TailorParser parser = new TailorParser(tokens);
            DSLVisitor visitor = new DSLVisitor();
            return (Mission) visitor.visitTailorProgram(parser.tailorProgram());
        } catch (Exception e) {
            throw new IllegalStateException("DSL 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 DSL 转成 DSLVisitor 生成的 Java 代码。
     */
    public String dslToNativeLanguage(String dslCode) {
        try {
            TailorLexer lexer = new TailorLexer(CharStreams.fromString(dslCode));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TailorParser parser = new TailorParser(tokens);
            DSLVisitor visitor = new DSLVisitor();
            visitor.visitTailorProgram(parser.tailorProgram());
            return visitor.getGeneratedJavaCode();
        } catch (Exception e) {
            throw new IllegalStateException("DSL 转本地语言失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 DSL 映射成可通过 DDS 传输的 IDL 数据。
     * 当前返回的是统一领域模型对应的 IDL 结果，具体发布由 DDSManager 完成。
     */
    public DdsPayload toDdsPayload(String dslCode) {
        Mission mission = dslToMission(dslCode);
        DdsPayload payload = new DdsPayload();
        payload.mission = mission;
        payload.missionCommands = dslidlMapper.mapMissionToCommands(mission);
        payload.swarmMission = dslidlMapper.mapMissionToSwarmMission(mission);
        payload.vehicleData = dslidlMapper.mapMissionToVehicleData(mission);
        payload.emergencyAlerts = dslidlMapper.mapMissionToAlerts(mission);
        return payload;
    }

    /**
     * 将 DSL 直接发布到 DDS。
     * 这里采用“先转领域模型，再转 IDL，再发布”的方式。
     */
    public boolean publishToDds(String dslCode, int domainId) {
        DdsPayload payload = toDdsPayload(dslCode);
        DDSManager dds = DDSManager.getInstance(domainId);
        if (!dds.connect()) {
            return false;
        }

        boolean published = false;

        if (payload.swarmMission != null) {
            dds.publishSwarmMission(idlDataConverter.toIDLSwarmMission(payload.swarmMission));
            published = true;
        }

        for (DSLIDLMapper.IDLMissionCommand cmd : payload.missionCommands) {
            dds.publishMissionCommand(idlDataConverter.toIDLMissionCommand(cmd));
            published = true;
        }

        for (DSLIDLMapper.IDLVehicleData vehicleData : payload.vehicleData) {
            dds.publishVehicleData(idlDataConverter.toIDLVehicleData(vehicleData));
            published = true;
        }

        for (DSLIDLMapper.IDLEmergencyAlert alert : payload.emergencyAlerts) {
            dds.publishEmergencyAlert(idlDataConverter.toIDLEmergencyAlert(alert));
            published = true;
        }

        return published;
    }

    /**
     * DSL 传输负载。
     */
    public static class DdsPayload {
        public Mission mission;
        public java.util.List<DSLIDLMapper.IDLMissionCommand> missionCommands;
        public DSLIDLMapper.IDLSwarmMission swarmMission;
        public java.util.List<DSLIDLMapper.IDLVehicleData> vehicleData;
        public java.util.List<DSLIDLMapper.IDLEmergencyAlert> emergencyAlerts;
    }
}