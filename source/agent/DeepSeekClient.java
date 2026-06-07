package agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DeepSeek OpenAI-compatible client.
 *
 * This client is deliberately isolated from the rest of the agent so the project
 * can keep its deterministic rule/NLU pipeline and use the LLM only as an
 * optional enhancer.
 */
public class DeepSeekClient {

    private final DeepSeekConfig config;
    private final HttpClient httpClient;
    private final Gson gson;

    public DeepSeekClient() {
        this(new DeepSeekConfig());
    }

    public DeepSeekClient(DeepSeekConfig config) {
        this.config = config;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
            .build();

        if (config.isEnabled()) {
            System.out.println("[DeepSeek] 已启用: model=" + config.getModel() + ", baseUrl=" + config.getBaseUrl());
        } else {
            System.out.println("[DeepSeek] 未启用，当前保持规则+NLU模式运行");
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public double getAdoptConfidenceThreshold() {
        return config.getAdoptConfidenceThreshold();
    }

    public DeepSeekSuggestion suggest(String input, String currentIntent, CPSEnums.DeviceRole role, String scenarioScope) {
        if (!config.isEnabled()) {
            return null;
        }

        String prompt = buildPrompt(input, currentIntent, role, scenarioScope);
        ChatCompletionRequest requestPayload = ChatCompletionRequest.create(
            config.getModel(),
            config.getSystemPrompt(),
            prompt,
            config.getTemperature(),
            config.getMaxTokens()
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestPayload), StandardCharsets.UTF_8))
                .build();

            Instant start = Instant.now();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            int httpStatus = response.statusCode();

            if (httpStatus < 200 || httpStatus >= 300) {
                System.out.println("[DeepSeek] HTTP " + httpStatus + "，回退本地规则模式");
                return null;
            }

            ChatCompletionResponse parsed = parseResponse(response.body());
            if (parsed == null) {
                System.out.println("[DeepSeek] 响应结构解析失败，回退本地规则模式");
                return null;
            }

            String content = parsed.extractContent();
            if (content == null || content.isBlank()) {
                System.out.println("[DeepSeek] 内容为空，回退本地规则模式");
                return null;
            }

            DeepSeekSuggestion suggestion = parseSuggestion(content);
            if (suggestion == null) {
                System.out.println("[DeepSeek] 内容格式不符合协议，回退本地规则模式");
                return null;
            }

            suggestion.latencyMs = latencyMs;
            suggestion.model = parsed.model != null ? parsed.model : config.getModel();
            suggestion.provider = detectProvider(config.getBaseUrl());
            suggestion.httpStatus = httpStatus;
            suggestion.responseId = parsed.id;
            suggestion.rawContent = content;
            suggestion.rawResponse = response.body();
            System.out.println("[DeepSeek] HTTP " + httpStatus
                + " 成功, provider=" + suggestion.provider
                + ", model=" + suggestion.model
                + ", latency=" + latencyMs + "ms");
            return suggestion;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[DeepSeek] 调用中断，回退本地规则模式: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("[DeepSeek] 调用失败，回退本地规则模式: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("[DeepSeek] 解析失败，回退本地规则模式: " + e.getMessage());
            return null;
        }
    }

    public HealthCheckResult healthCheck() {
        HealthCheckResult result = new HealthCheckResult();
        result.enabled = config.isEnabled();
        result.provider = detectProvider(config.getBaseUrl());
        result.model = config.getModel();

        if (!config.isEnabled()) {
            result.success = false;
            result.message = "DEEPSEEK_ENABLED=false 或 API_KEY 未配置";
            return result;
        }

        ChatCompletionRequest requestPayload = ChatCompletionRequest.create(
            config.getModel(),
            config.getSystemPrompt(),
            "健康检查，请只回复 OK",
            config.getTemperature(),
            Math.min(config.getMaxTokens(), 64)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(config.getTimeoutMillis()))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestPayload), StandardCharsets.UTF_8))
                .build();

            Instant start = Instant.now();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            int status = response.statusCode();

            result.httpStatus = status;
            result.latencyMs = latencyMs;

            if (status < 200 || status >= 300) {
                result.success = false;
                result.message = "HTTP " + status + "，请检查 Key、余额或模型权限";
                return result;
            }

            ChatCompletionResponse parsed = parseResponse(response.body());
            String content = parsed == null ? null : trimToNull(parsed.extractContent());
            result.responseId = parsed == null ? null : parsed.id;
            result.model = (parsed != null && parsed.model != null) ? parsed.model : config.getModel();
            result.success = content != null;
            result.message = result.success ? "健康检查通过" : "HTTP 2xx 但响应内容为空";
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.success = false;
            result.message = "健康检查被中断: " + e.getMessage();
            return result;
        } catch (IOException e) {
            result.success = false;
            result.message = "健康检查调用失败: " + e.getMessage();
            return result;
        } catch (Exception e) {
            result.success = false;
            result.message = "健康检查异常: " + e.getMessage();
            return result;
        }
    }

    private String detectProvider(String baseUrl) {
        if (baseUrl == null) {
            return "deepseek";
        }
        String normalized = baseUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("dashscope")) {
            return "qwen-compatible";
        }
        return "deepseek";
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildPrompt(String input, String currentIntent, CPSEnums.DeviceRole role, String scenarioScope) {
        String topicGuide = buildTopicGuide(role);
        return "你是车内乘客自然语言意图、无人车、无人机和合规广域链路协同系统的决策增强器。\n"
            + "请严格输出 JSON 兼容的 5 行键值对，禁止输出额外解释：\n"
            + "INTENT=...\n"
            + "PUBLISH=topic1,topic2\n"
            + "SUBSCRIBE=topic1,topic2\n"
            + "SUMMARY=...\n"
            + "CONFIDENCE=0.00\n"
            + "要求：\n"
            + "1. INTENT 只能从 NAVIGATION, DELIVERY, COORDINATION, EMERGENCY, CHARGE, QUERY_BATTERY, QUERY_STATUS, STOP, PATROL, SURVEILLANCE, UNKNOWN 中选择。\n"
            + "2. PUBLISH/SUBSCRIBE 必须使用项目内已有主题名。\n"
            + "3. 如果某字段没有建议，用 none。\n"
            + "4. SUMMARY 只写一句中文。\n"
            + "5. CONFIDENCE 取 0 到 1 之间小数。\n"
            + "6. 不要输出 Markdown，不要输出代码块。\n"
            + "允许使用的主题: " + topicGuide + "\n"
            + "上下文信息：\n"
            + "- 场景范围: " + safeText(scenarioScope) + "\n"
            + "- 当前角色: " + role + "\n"
            + "- 当前规则意图: " + safeText(currentIntent) + "\n"
            + "- 用户输入: " + safeText(input) + "\n";
    }

    private String buildTopicGuide(CPSEnums.DeviceRole role) {
        if (role == CPSEnums.DeviceRole.DRONE) {
            return "DroneStatus,AerialView,TrafficFlow,VehicleData,WeatherData,NoFlyZone,DeliveryStatus,EmergencyAlert,MissionAssignment,CompliantBackboneSync,GlobalPerception,CompliantBackbone/GlobalPerception,Route/Replan/Trigger,SubscriptionIntent,SemanticTaskDSL,Traffic/Intersection/Forward,UAV/Perception/ForwardCorridor";
        }
        if (role == CPSEnums.DeviceRole.CONTROL_CENTER) {
            return "ControlCommand,MissionAssignment,VehicleStatus,DroneStatus,TrafficFlow,WeatherData,CompliantBackboneSync,GlobalPerception,CompliantBackbone/GlobalPerception,Route/Replan/Trigger";
        }
        return "VehicleData,MissionRequest,TrafficFlow,IntersectionPerception,DroneStatus,ControlCommand,MissionAssignment,CompliantBackboneSync,GlobalPerception,CompliantBackbone/GlobalPerception,Route/Replan/Trigger,SubscriptionIntent,SemanticTaskDSL,EmergencyAlert,BatteryStatus,Traffic/Intersection/Forward,UAV/Perception/ForwardCorridor";
    }

    private ChatCompletionResponse parseResponse(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.id = getAsString(root, "id");
        response.model = getAsString(root, "model");
        response.created = getAsLong(root, "created");

        if (root.has("choices") && root.get("choices").isJsonArray()) {
            JsonArray choices = root.getAsJsonArray("choices");
            if (!choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                response.finishReason = getNestedString(firstChoice, "finish_reason");
                if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    response.content = getAsString(message, "content");
                    response.role = getAsString(message, "role");
                }
            }
        }
        return response;
    }

    private DeepSeekSuggestion parseSuggestion(String content) {
        String normalizedContent = sanitizeContent(content);
        if (normalizedContent == null || normalizedContent.isBlank()) {
            return null;
        }

        DeepSeekSuggestion suggestion = new DeepSeekSuggestion();
        if (normalizedContent.startsWith("{")) {
            try {
                JsonObject json = JsonParser.parseString(normalizedContent).getAsJsonObject();
                suggestion.intent = normalizeToken(getJsonString(json, "intent"));
                suggestion.publishTopics = parseTopics(getJsonString(json, "publish"));
                suggestion.subscribeTopics = parseTopics(getJsonString(json, "subscribe"));
                suggestion.summary = stripWrappingQuotes(getJsonString(json, "summary"));
                suggestion.confidence = parseDouble(getJsonString(json, "confidence"), 0.0);
            } catch (Exception ignore) {
                // fall through to line-based parsing
            }
        }

        if (suggestion.intent == null && suggestion.summary == null && suggestion.publishTopics.isEmpty() && suggestion.subscribeTopics.isEmpty()) {
            String[] lines = normalizedContent.split("\\R");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim().toUpperCase(Locale.ROOT);
                String value = line.substring(idx + 1).trim();
                switch (key) {
                    case "INTENT":
                        suggestion.intent = normalizeToken(value);
                        break;
                    case "PUBLISH":
                        suggestion.publishTopics = parseTopics(value);
                        break;
                    case "SUBSCRIBE":
                        suggestion.subscribeTopics = parseTopics(value);
                        break;
                    case "SUMMARY":
                        suggestion.summary = stripWrappingQuotes(value);
                        break;
                    case "CONFIDENCE":
                        suggestion.confidence = parseDouble(value, 0.0);
                        break;
                    default:
                        break;
                }
            }
        }

        if ((suggestion.intent == null || suggestion.intent.isBlank())
            && (suggestion.summary == null || suggestion.summary.isBlank())) {
            return null;
        }
        return suggestion;
    }

    private String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.startsWith("```")) {
            int firstBreak = normalized.indexOf('\n');
            if (firstBreak >= 0) {
                normalized = normalized.substring(firstBreak + 1).trim();
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }

    private String stripWrappingQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'" ) && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private List<String> parseTopics(String value) {
        List<String> topics = new ArrayList<>();
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value.trim())) {
            return topics;
        }
        String normalized = value.replace('，', ',');
        for (String part : normalized.split(",")) {
            String topic = part.trim();
            if (!topic.isEmpty() && !"none".equalsIgnoreCase(topic)) {
                topics.add(topic);
            }
        }
        return topics;
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        String token = value.trim().toUpperCase(Locale.ROOT);
        return token.isEmpty() || "NONE".equals(token) ? null : token;
    }

    private String safeText(String value) {
        return value == null ? "" : value.replace("\n", " ").replace("\r", " ");
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        JsonElement element = object.get(key);
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }

    private String getJsonString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    private Long getAsLong(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private String getNestedString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static class ChatCompletionRequest {
        String model;
        List<Message> messages;
        Double temperature;
        Integer max_tokens;
        Boolean stream;

        static ChatCompletionRequest create(String model, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.model = model;
            request.messages = new ArrayList<>();
            request.messages.add(new Message("system", systemPrompt));
            request.messages.add(new Message("user", userPrompt));
            request.temperature = temperature;
            request.max_tokens = maxTokens;
            request.stream = Boolean.FALSE;
            return request;
        }
    }

    public static class Message {
        String role;
        String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ChatCompletionResponse {
        String id;
        String model;
        Long created;
        String role;
        String content;
        String finishReason;

        String extractContent() {
            return content;
        }
    }

    public static class DeepSeekSuggestion {
        public String provider;
        public String model;
        public int httpStatus;
        public String responseId;
        public String intent;
        public List<String> publishTopics = new ArrayList<>();
        public List<String> subscribeTopics = new ArrayList<>();
        public String summary;
        public double confidence;
        public long latencyMs;
        public String rawContent;
        public String rawResponse;
    }

    public static class HealthCheckResult {
        public boolean enabled;
        public boolean success;
        public String provider;
        public String model;
        public int httpStatus;
        public long latencyMs;
        public String responseId;
        public String message;
    }
}
