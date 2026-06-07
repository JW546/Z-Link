package agent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 协同策略配置（HCP/TCL/广域链路）
 * 读取 satcom.properties / sl.example（可选），不存在时使用中国竞赛友好的默认值。
 */
public class CoordinationPolicy {
    private final boolean globalBackboneEnabled;
    private final boolean tclSemanticEnabled;
    private final boolean hcpModeEnabled;
    private final List<String> coordinationKeywords;

    public CoordinationPolicy() {
        Properties p = loadProps();
        boolean legacyCOMPLIANT_BACKBONEEnabled = Boolean.parseBoolean(p.getProperty("hcp.COMPLIANT_BACKBONE.enabled", "false"));
        String cnPolicy = p.getProperty("COMPLIANT_BACKBONE.policy.cn", "disabled");
        this.globalBackboneEnabled = Boolean.parseBoolean(p.getProperty("hcp.global.backbone.enabled", "true"))
            && (!legacyCOMPLIANT_BACKBONEEnabled || !"disabled".equalsIgnoreCase(cnPolicy));
        this.tclSemanticEnabled = Boolean.parseBoolean(p.getProperty("hcp.tcl.semantic.enabled", "true"));
        this.hcpModeEnabled = Boolean.parseBoolean(p.getProperty("hcp.mode.enabled", "true"));

        String keywords = p.getProperty(
            "hcp.intent.coordination.keywords",
            "协同,联动,邻车,前车,后车,无人机建议,路线建议,合规广域链路,卫星移动通信,低轨卫星,北斗短报文,5G专网"
        );
        this.coordinationKeywords = Arrays.stream(keywords.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private Properties loadProps() {
        Properties p = new Properties();
        try {
            try (InputStream in = new FileInputStream("satcom.properties")) {
                p.load(in);
                return p;
            }
        } catch (Exception ignore) {
            // fallback to legacy / example
        }

        for (String candidate : new String[] {"satcom.properties.example", "sl.example"}) {
            try (InputStream in = new FileInputStream(candidate)) {
                p.load(in);
                break;
            } catch (Exception ignore) {
                // try next candidate
            }
        }
        return p;
    }

    public boolean isCOMPLIANT_BACKBONEEnabled() {
        return globalBackboneEnabled;
    }

    public boolean isGlobalBackboneEnabled() {
        return globalBackboneEnabled;
    }

    public boolean isTclSemanticEnabled() {
        return tclSemanticEnabled;
    }

    public boolean isHcpModeEnabled() {
        return hcpModeEnabled;
    }

    public List<String> getCoordinationKeywords() {
        return coordinationKeywords;
    }
}
