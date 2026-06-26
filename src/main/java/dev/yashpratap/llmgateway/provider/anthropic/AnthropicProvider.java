package dev.yashpratap.llmgateway.provider.anthropic;

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

/**
 * {@link LLMProvider} implementation for the Anthropic Claude API.
 *
 * <p>Uses the Anthropic Messages API ({@code /v1/messages}) with {@code x-api-key}
 * authentication. System messages are extracted from the conversation and placed in
 * the top-level {@code system} field as required by the Anthropic API format.</p>
 *
 * <p>The bean is only registered when {@code providers.anthropic.api-key} is present
 * in the environment, so the application starts cleanly if Anthropic is not configured.</p>
 */
@Service
@ConditionalOnExpression("'${providers.anthropic.api-key:}'.length() > 0")
public class AnthropicProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);

    private final WebClient webClient;
    private final AnthropicProperties properties;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(@Qualifier("anthropicWebClient") WebClient webClient,
                             AnthropicProperties properties,
                             ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderName name() {
        return ProviderName.CLAUDE;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        String model = resolveModel(request.model());
        Map<String, Object> body = buildRequestBody(request, model, false);

        String rawResponse = webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> new ProviderException("Anthropic client error: " + b)))
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> new ProviderUnavailableException("CLAUDE", new RuntimeException(b))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()),
                        Mono.error(new ProviderTimeoutException("CLAUDE")))
                .block();

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProviderException("Anthropic returned null or empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return mapToChatResponse(root);
        } catch (ProviderException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ProviderException("Failed to parse Anthropic response: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        String model = resolveModel(request.model());
        Map<String, Object> body = buildRequestBody(request, model, true);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .flatMap(line -> {
                    try {
                        // Spring's SSE decoder strips the "data: " prefix for text/event-stream.
                        // For raw lines (non-SSE content-type), strip it defensively.
                        String json = line.startsWith("data: ") ? line.substring(6) : line;
                        JsonNode root = objectMapper.readTree(json);
                        String type = root.path("type").asText("");
                        if (!"content_block_delta".equals(type)) {
                            return Flux.empty();
                        }
                        JsonNode delta = root.path("delta");
                        if (!"text_delta".equals(delta.path("type").asText(""))) {
                            return Flux.empty();
                        }
                        String text = delta.path("text").asText("");
                        if (text.isEmpty()) {
                            return Flux.empty();
                        }
                        return Flux.just(new ChatChunk("", ProviderName.CLAUDE.name(), text, false));
                    } catch (Exception e) {
                        log.warn("Skipping malformed Anthropic SSE line: {}", e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private String resolveModel(String requestModel) {
        if (requestModel != null && !requestModel.isBlank() && !"auto".equals(requestModel)) {
            if (!requestModel.startsWith("claude-")) {
                log.warn("[anthropic] model '{}' does not start with 'claude-'; using default '{}'",
                        requestModel, properties.defaultModel());
                return properties.defaultModel();
            }
            return requestModel;
        }
        return properties.defaultModel();
    }

    private Map<String, Object> buildRequestBody(ChatRequest request, String model, boolean stream) {
        String systemContent = null;
        List<Map<String, String>> messages = new ArrayList<>();
        for (Message msg : request.messages()) {
            if ("system".equalsIgnoreCase(msg.role())) {
                systemContent = msg.content();
            } else {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", properties.maxOutputTokens());
        if (systemContent != null) {
            body.put("system", systemContent);
        }
        body.put("messages", messages);
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private ChatResponse mapToChatResponse(JsonNode root) {
        String id = root.path("id").asText("unknown");
        String responseModel = root.path("model").asText(properties.defaultModel());

        JsonNode contentArr = root.path("content");
        String text = "";
        if (contentArr.isArray() && !contentArr.isEmpty()) {
            JsonNode first = contentArr.get(0);
            if (first.hasNonNull("text")) {
                text = first.get("text").asText("");
            }
        }
        if (text.isEmpty()) {
            throw new ProviderException("Anthropic returned empty or malformed content");
        }

        JsonNode usageNode = root.path("usage");
        int inputTokens = usageNode.path("input_tokens").asInt(0);
        int outputTokens = usageNode.path("output_tokens").asInt(0);

        List<Choice> choices = List.of(new Choice(0, new Message("assistant", text), "stop"));
        Usage usage = new Usage(inputTokens, outputTokens, 0.0);
        GatewayMeta meta = new GatewayMeta(ProviderName.CLAUDE.name(), false, 0L);
        return new ChatResponse(id, responseModel, ProviderName.CLAUDE.name(), choices, usage, meta);
    }
}
