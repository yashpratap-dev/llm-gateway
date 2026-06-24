package dev.yashpratap.llmgateway.provider;

/**
 * Gateway-level metadata appended to every chat response.
 *
 * <p>These fields are not part of the provider's original response; they are
 * added by the gateway to aid observability and billing.</p>
 *
 * @param provider   the {@link ProviderName} that served the request
 * @param cacheHit   {@code true} when the response was served from Redis cache
 * @param latencyMs  total end-to-end gateway latency in milliseconds
 */
public record GatewayMeta(String provider, boolean cacheHit, long latencyMs) {
}
