package collaboration.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeviceProfile {
    public final String agentId;
    public final RuntimeAgentRole role;
    public final String protocol;
    public final String initialRoute;
    public final String vendor;

    public DeviceProfile(String agentId, RuntimeAgentRole role, String protocol, String initialRoute, String vendor) {
        this.agentId = agentId;
        this.role = role;
        this.protocol = protocol;
        this.initialRoute = initialRoute == null || initialRoute.isBlank() ? "route_A_main_road" : initialRoute;
        this.vendor = vendor == null ? "" : vendor;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("agentId", agentId);
        map.put("role", role.name());
        map.put("protocol", protocol);
        map.put("initialRoute", initialRoute);
        map.put("vendor", vendor);
        return map;
    }
}
