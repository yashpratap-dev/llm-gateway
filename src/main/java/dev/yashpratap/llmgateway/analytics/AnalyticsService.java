package dev.yashpratap.llmgateway.analytics;

import org.springframework.stereotype.Service;

/**
 * Service that aggregates usage data for tenant-facing analytics dashboards.
 *
 * <p>Provides summaries such as total tokens consumed, cost over time,
 * cache hit rates, and provider distribution. Full implementation in M8.</p>
 */
@Service
public class AnalyticsService {

    /**
     * Constructs the analytics service.
     * Dependencies (e.g., UsageLogRepository) are injected in M8.
     */
    public AnalyticsService() {
        // Dependencies are wired in M8
    }
}
