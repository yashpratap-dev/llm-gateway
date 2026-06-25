package dev.yashpratap.llmgateway.analytics;

import java.util.List;

/**
 * Aggregated overview statistics for the admin dashboard.
 *
 * <p>All monetary values use double for JSON serialisation simplicity.
 * Internal calculations use BigDecimal to avoid floating-point errors.</p>
 *
 * @param totalRequests      total number of requests processed by the gateway
 * @param totalCostUsd       total USD cost across all providers and tenants
 * @param cacheHitRatio      ratio of cache hits to total requests (0.0–1.0); 0.0 when no requests
 * @param activeTenantsCount number of tenants currently registered
 * @param providers          per-provider breakdown of request count and cost
 */
public record OverviewResponse(
    long totalRequests,
    double totalCostUsd,
    double cacheHitRatio,
    long activeTenantsCount,
    List<ProviderCostSummary> providers
) {}
