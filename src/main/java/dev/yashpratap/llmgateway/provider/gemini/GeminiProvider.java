package dev.yashpratap.llmgateway.provider.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.provider.ChatChunk;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Choice;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.Usage;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import dev.yashpratap.llmgateway.provider.exception.ProviderTimeoutException;
import dev.yashpratap.llmgateway.provider.exception.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link LLMProvider} implementation for the Google Gemini API.
 *
 * <p>Uses the Generative Language API ({@code /v1beta/models/{model}:generateContent})
 * with the API key passed as a {@code ?key=} query parameter on every request.
 * System messages are mapped to user-role entries because Gemini has no separate
 * system-message field.</p>
 *
 * <p>The bean is only registered when {@code providers.gemini.api-key} is present
 * and non-empty in the environment.</p>
 */
@Service
@ConditionalOnExpression("'${providers.gemini.api-key:}'.length() > 0")
public class GeminiProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiProvider(@Qualifier("geminiWebClient") WebClient webClient,
                          GeminiProperties properties,
                          ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderName name() {
        return ProviderName.GEMINI;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        String model = resolveModel(request.model());
        List<Map<String, Object>> contents = buildContents(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        body.put("generationConfig", Map.of("maxOutputTokens", properties.maxOutputTokens()));

        String rawResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiVersion}/models/{model}:generateContent")
                        .queryParam("key", properties.apiKey())
                        .build(properties.apiVersion(), model))
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> new ProviderException("Gemini client error: " + b)))
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> new ProviderUnavailableException("GEMINI", new RuntimeException(b))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()),
                        Mono.error(new ProviderTimeoutException("GEMINI")))
                .block();

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProviderException("Gemini returned null or empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return mapToChatResponse(root, model);
        } catch (ProviderException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ProviderException("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        String model = resolveModel(request.model());
        List<Map<String, Object>> contents = buildContents(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        body.put("generationConfig", Map.of("maxOutputTokens", properties.maxOutputTokens()));

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiVersion}/models/{model}:streamGenerateContent")
                        .queryParam("key", properties.apiKey())
                        .queryParam("alt", "sse")
                        .build(properties.apiVersion(), model))
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .flatMap(line -> {
                    try {
                        // Spring's SSE decoder strips "data: " prefix for text/event-stream.
                        // Handle defensively for both decoded and raw-line cases.
                        String json = line.startsWith("data: ") ? line.substring(6) : line;
                        JsonNode root = objectMapper.readTree(json);
                        JsonNode candidates = root.path("candidates");
                        if (!candidates.isArray() || candidates.isEmpty()) {
                            log.warn("[gemini-stream] empty candidates in chunk — skipping");
                            return Flux.empty();
                        }
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (!parts.isArray() || parts.isEmpty()) {
                            log.warn("[gemini-stream] empty parts in chunk — skipping");
                            return Flux.empty();
                        }
                        String text = parts.get(0).path("text").asText("");
                        if (text.isEmpty()) {
                            return Flux.empty();
                        }
                        return Flux.just(new ChatChunk("", ProviderName.GEMINI.name(), text, false));
                    } catch (Exception e) {
                        log.warn("[gemini-stream] skipping malformed SSE line: {}", e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private String resolveModel(String requestModel) {
        String model = (requestModel != null
                && !requestModel.trim().isBlank()
                && !"auto".equals(requestModel.trim()))
                ? requestModel.trim()
                : properties.defaultModel();
        if (!model.startsWith("gemini-")) {
            log.warn("[gemini] unexpected model '{}' — using default '{}'", model, properties.defaultModel());
            model = properties.defaultModel();
        }
        return model;
    }

    private List<Map<String, Object>> buildContents(ChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Message msg : request.messages()) {
            String geminiRole = switch (msg.role().toLowerCase().trim()) {
                case "assistant" -> "model";
                case "system"    -> "user";
                default          -> "user";
            };
            contents.add(Map.of(
                    "role", geminiRole,
                    "parts", List.of(Map.of("text", msg.content()))
            ));
        }
        return contents;
    }

    private ChatResponse mapToChatResponse(JsonNode root, String model) {
        JsonNode candidates = root.path("candidates");
        String text = "";
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                text = parts.get(0).path("text").asText("");
            }
        }
        if (text.isEmpty()) {
            throw new ProviderException("Gemini returned empty or malformed content");
        }

        int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
        int completionTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

        String id = UUID.randomUUID().toString();
        List<Choice> choices = List.of(new Choice(0, new Message("assistant", text), "stop"));
        Usage usage = new Usage(promptTokens, completionTokens, 0.0);
        GatewayMeta meta = new GatewayMeta(ProviderName.GEMINI.name(), false, 0L);
        return new ChatResponse(id, model, ProviderName.GEMINI.name(), choices, usage, meta);
    }
}
