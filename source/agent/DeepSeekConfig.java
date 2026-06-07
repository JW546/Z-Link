package agent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * DeepSeek 配置加载器。
 *
 * 优先级：
 * 1. 环境变量
 * 2. deepseek.properties
 * 3. deepseek.properties.example
 * 4. 默认值
 */
public class DeepSeekConfig {

    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final int timeoutMillis;
    private final double adoptConfidenceThreshold;
    private final String systemPrompt;

    public DeepSeekConfig() {
        Properties properties = loadProperties();

        this.apiKey = firstNonBlank(
            System.getenv("QWEN_API_KEY"),
            System.getenv("DASHSCOPE_API_KEY"),
            System.getenv("DEEPSEEK_API_KEY"),
            properties.getProperty("qwen.apiKey"),
            properties.getProperty("dashscope.apiKey"),
            properties.getProperty("deepseek.apiKey")
        );
        this.baseUrl = firstNonBlank(
            System.getenv("QWEN_BASE_URL"),
            System.getenv("DASHSCOPE_BASE_URL"),
            System.getenv("DEEPSEEK_BASE_URL"),
            properties.getProperty("qwen.baseUrl"),
            properties.getProperty("dashscope.baseUrl"),
            properties.getProperty("deepseek.baseUrl"),
            "https://dashscope.aliyuncs.com/compatible-mode/v1"
        );
        this.model = firstNonBlank(
            System.getenv("QWEN_MODEL"),
            System.getenv("DASHSCOPE_MODEL"),
            System.getenv("DEEPSEEK_MODEL"),
            properties.getProperty("qwen.model"),
            properties.getProperty("dashscope.model"),
            properties.getProperty("deepseek.model"),
            "qwen-plus"
        );
        this.temperature = parseDouble(firstNonBlank(System.getenv("DEEPSEEK_TEMPERATURE"), properties.getProperty("deepseek.temperature")), 0.2);
        this.maxTokens = parseInt(firstNonBlank(System.getenv("DEEPSEEK_MAX_TOKENS"), properties.getProperty("deepseek.maxTokens")), 512);
        this.timeoutMillis = parseInt(firstNonBlank(System.getenv("DEEPSEEK_TIMEOUT_MS"), properties.getProperty("deepseek.timeoutMillis")), 12000);
        this.adoptConfidenceThreshold = parseDouble(firstNonBlank(System.getenv("DEEPSEEK_ADOPT_THRESHOLD"), properties.getProperty("deepseek.adoptConfidenceThreshold")), 0.55);
        this.systemPrompt = firstNonBlank(
            System.getenv("DEEPSEEK_SYSTEM_PROMPT"),
            properties.getProperty("deepseek.systemPrompt"),
            "你是车内乘客自然语言意图、无人车、无人机与合规广域链路协同系统的决策增强模型。"
        );
        this.enabled = Boolean.parseBoolean(firstNonBlank(
                System.getenv("QWEN_ENABLED"),
                System.getenv("DASHSCOPE_ENABLED"),
                System.getenv("DEEPSEEK_ENABLED"),
                properties.getProperty("qwen.enabled"),
                properties.getProperty("dashscope.enabled"),
                properties.getProperty("deepseek.enabled"),
                "false"))
            && apiKey != null;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        String[] candidates = {"deepseek.properties", "deepseek.properties.example"};
        for (String candidate : candidates) {
            try (InputStream in = new FileInputStream(candidate)) {
                properties.load(in);
                return properties;
            } catch (Exception ignore) {
                // try next
            }
        }
        return properties;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public double getAdoptConfidenceThreshold() {
        return adoptConfidenceThreshold;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}
