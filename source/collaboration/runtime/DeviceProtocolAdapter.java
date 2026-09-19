package collaboration.runtime;

import java.util.Map;

public interface DeviceProtocolAdapter {
    String protocolName();

    Map<String, Object> toNativeStatus(Map<String, Object> unifiedStatus);

    Map<String, Object> toNativeCommand(Map<String, Object> unifiedCommand);

    Map<String, Object> fromNativeStatus(Map<String, Object> nativeStatus);

    Map<String, Object> fromNativeCommand(Map<String, Object> nativeCommand);
}
