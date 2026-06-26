package dev.yashpratap.llmgateway.provider.openai;

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
import dev.yashpratap.llmgateway.provider.wire.ProviderChatRequest;
import dev.yashpratap.llmgateway.provider.wire.ProviderChatResponse;
import dev.yashpratap.llmgateway.provider.wire.ProviderChoice;
import dev.yashpratap.llmgateway.provider.wire.ProviderMessage;
import dev.yashpratap.llmgateway.provider.wire.ProviderStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * {@link LLMProvider} implementation for the OpenAI platform.
 *
 * <p>Supports GPT-4o and GPT-4o-mini models via the OpenAI chat completions endpoint.
 * This provider uses a reactive {@link WebClient} for both synchronous (blocking) and
 * streaming completion requests.</p>
 *
 * <p>The bean is only registered when {@code providers.openai.api-key} is present in the
 * environment, so the application starts cleanly if only Groq is configured.</p>
 *
 * <p><b>Note on {@code .block()}</b>: {@link #generate} blocks the reactive chain because
 * the gateway controller is currently servlet-based. The WebClient is retained so that
 * streaming and future non-blocking migration require no provider-layer changes.</p>
 *
 * <p><b>Note on {@code isHealthy()}</b>: returns {@code true} unconditionally in M3.
 * The M6 resilience layer replaces this with a Resilience4j circuit breaker state check.</p>
 *
 * <p><b>Note on {@code costUsd}</b>: set to {@code 0.0} in M3.
 * M8 calculates the real cost from the {@code model_pricing} table.</p>
 */
@Component
@ConditionalOnExpression("'${providers.openai.api-key:}'.length() > 0")
public class OpenAIProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);

    private final WebClient webClient;
    private final OpenAIProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the provider with all required collaborators.
     *
     * @param webClient    pre-configured OpenAI {@link WebClient} (base URL + auth header)
     * @param properties   OpenAI-specific configuration (model, timeout)
     * @param objectMapper Jackson mapper for deserialising stream chunks
     */
    public OpenAIProvider(@Qualifier("openaiWebClient") WebClient webClient,
                          OpenAIProperties properties,
                          ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public ProviderName name() {
        return ProviderName.OPENAI;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHealthy() {
        return true;
    }

    /**
     * Sends a synchronous chat completion request to the OpenAI API.
     *
     * @param request the provider-agnostic chat request
     * @return the completed {@link ChatResponse}
     * @throws ProviderUnavailableException if OpenAI returns a 5xx response
     * @throws ProviderTimeoutException     if no response arrives within the configured timeout
     * @throws ProviderException            if OpenAI returns a 4xx error or an empty choices list
     */
    @Override
    public ChatResponse generate(ChatRequest request) {
        ProviderChatRequest providerRequest = buildProviderRequest(request, false);

        ProviderChatResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(providerRequest)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new ProviderException("OpenAI client error: " + body)))
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new ProviderUnavailableException("OPENAI", new RuntimeException(body))))
                .bodyToMono(ProviderChatResponse.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()),
                        Mono.error(new ProviderTimeoutException("OPENAI")))
                .block();

        if (response == null) {
            throw new ProviderException("Provider returned null response");
        }
        return mapToChatResponse(response);
    }

    /**
     * Opens a Server-Sent Events stream to the OpenAI API and returns a reactive stream of chunks.
     *
     * @param request the provider-agnostic streaming chat request
     * @return a cold {@link Flux} emitting {@link ChatChunk} items until the stream is done
     */
    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        ProviderChatRequest providerRequest = buildProviderRequest(request, true);
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(providerRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank()
                        && !line.equals("[DONE]")
                        && !line.equals("data: [DONE]"))
                .map(line -> line.startsWith("data: ") ? line.substring(6) : line)
                .flatMap(line -> {
                    try {
                        ProviderStreamChunk chunk = objectMapper.readValue(line, ProviderStreamChunk.class);
                        if (chunk.choices() == null || chunk.choices().isEmpty()) {
                            return Flux.empty();
                        }
                        ProviderChoice choice = chunk.choices().get(0);
                        String delta = choice.delta() != null ? choice.delta().content() : null;
                        if (delta == null || delta.isBlank()) {
                            return Flux.empty();
                        }
                        boolean done = "stop".equals(choice.finishReason());
                        return Flux.just(new ChatChunk(chunk.id(), ProviderName.OPENAI.name(), delta, done));
                    } catch (Exception e) {
                        log.debug("Skipping malformed stream chunk from OpenAI: {}", e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private ProviderChatRequest buildProviderRequest(ChatRequest request, boolean stream) {
        String model = "auto".equals(request.model()) ? properties.model() : request.model();
        List<ProviderMessage> messages = request.messages().stream()
                .map(m -> new ProviderMessage(m.role(), m.content()))
                .toList();
        return new ProviderChatRequest(model, messages, stream, null, null);
    }

    private ChatResponse mapToChatResponse(ProviderChatResponse response) {
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new ProviderException("OpenAI returned empty choices in response");
        }
        String content = response.choices().get(0).message().content();
        List<Choice> choices = List.of(new Choice(0, new Message("assistant", content), "stop"));
        Usage usage = new Usage(
                response.usage() != null ? response.usage().promptTokens() : 0,
                response.usage() != null ? response.usage().completionTokens() : 0,
                0.0);
        GatewayMeta meta = new GatewayMeta(ProviderName.OPENAI.name(), false, 0L);
        return new ChatResponse(response.id(), response.model(), ProviderName.OPENAI.name(), choices, usage, meta);
    }
}
