package dev.yashpratap.llmgateway.routing;

/**
 * Enumeration of the provider-selection strategies available to tenants.
 *
 * <p>The active strategy for a tenant is stored in
 * {@link dev.yashpratap.llmgateway.domain.RoutingPolicy} and selected at
 * request time by {@link RoutingService}.</p>
 */
public enum RoutingStrategy {

    /** Route to the provider with the lowest per-token cost for the requested model. */
    COST,

    /** Route to the provider with the lowest rolling average latency. */
    LATENCY,

    /** Route to providers in a fixed priority order: Groq first, OpenAI second. */
    PRIORITY
}
