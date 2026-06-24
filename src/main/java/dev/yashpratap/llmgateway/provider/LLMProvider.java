package dev.yashpratap.llmgateway.provider;

import reactor.core.publisher.Flux;

/**
 * Abstraction over a single LLM provider (e.g. Groq, OpenAI).
 *
 * <p>Each implementation handles authentication, request translation,
 * circuit-breaking, and retry logic for its specific upstream API.
 * The gateway's routing layer selects a concrete implementation at
 * request time based on the tenant's {@link dev.yashpratap.llmgateway.routing.RoutingStrategy}.</p>
 */
public interface LLMProvider {

    /**
     * Returns the identifier of this provider.
     *
     * @return the {@link ProviderName} enum constant for this implementation
     */
    ProviderName name();

    /**
     * Performs a lightweight health check against the provider's API.
     *
     * @return {@code true} if the provider is reachable and accepting requests
     */
    boolean isHealthy();

    /**
     * Sends a synchronous (non-streaming) chat completion request.
     *
     * @param request the provider-agnostic chat request
     * @return the completed chat response
     * @throws ProviderException if the provider returns an error or times out
     */
    ChatResponse generate(ChatRequest request);

    /**
     * Sends a streaming chat completion request and returns a reactive stream of chunks.
     *
     * @param request the provider-agnostic chat request (must have {@code stream = true})
     * @return a cold {@link Flux} that emits {@link ChatChunk} items until {@code done = true}
     * @throws ProviderException if the provider connection cannot be established
     */
    Flux<ChatChunk> stream(ChatRequest request);
}
