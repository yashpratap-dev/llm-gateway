package dev.yashpratap.llmgateway.analytics;

import java.math.BigDecimal;

/**
 * Aggregated cost and request volume for a single LLM provider.
 *
 * @param provider     the provider identifier (e.g. {@code GROQ}, {@code OPENAI})
 * @param totalCost    the total USD spend across all requests to this provider
 * @param requestCount the total number of requests routed to this provider
 */
public record ProviderCostSummary(String provider, BigDecimal totalCost, long requestCount) {
}
