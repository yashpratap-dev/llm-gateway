package dev.yashpratap.llmgateway.analytics;

import dev.yashpratap.llmgateway.billing.UsageLogRepository;
import dev.yashpratap.llmgateway.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Computes aggregated overview statistics for the admin dashboard.
 *
 * <p>Business logic lives here, not in the controller. The controller
 * delegates entirely to this service.</p>
 */
@Service
public class AnalyticsOverviewService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsOverviewService.class);

    private final UsageLogRepository usageLogRepository;
    private final TenantRepository tenantRepository;

    public AnalyticsOverviewService(UsageLogRepository usageLogRepository,
                                    TenantRepository tenantRepository) {
        this.usageLogRepository = usageLogRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Returns aggregated gateway statistics for the overview dashboard.
     *
     * <p>DB calls made: count(), findTotalCostAllTenants(), countCacheHits(),
     * tenantRepository.count(), findCostByProvider() — 5 total.</p>
     *
     * @return populated {@link OverviewResponse}
     */
    public OverviewResponse getOverview() {
        long totalRequests = usageLogRepository.count();
        log.debug("[overview] totalRequests={}", totalRequests);

        double totalCostUsd = 0.0;
        try {
            Double raw = usageLogRepository.findTotalCostAllTenants();
            if (raw != null) {
                totalCostUsd = BigDecimal.valueOf(raw)
                    .setScale(8, RoundingMode.HALF_UP)
                    .doubleValue();
            }
        } catch (Exception e) {
            log.warn("[overview] failed to fetch total cost: {}", e.getMessage());
        }

        double cacheHitRatio = 0.0;
        if (totalRequests > 0) {
            long cacheHits = usageLogRepository.countCacheHits();
            cacheHitRatio = BigDecimal.valueOf(cacheHits)
                .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP)
                .doubleValue();
        }

        long activeTenantsCount = tenantRepository.count();

        List<ProviderCostSummary> providers = buildProviderSummaries();

        return new OverviewResponse(
            totalRequests,
            totalCostUsd,
            cacheHitRatio,
            activeTenantsCount,
            providers
        );
    }

    /**
     * Parses the raw Object[] rows from findCostByProvider() into typed records.
     * Column order: [String provider, BigDecimal totalCost, Long requestCount]
     */
    private List<ProviderCostSummary> buildProviderSummaries() {
        try {
            List<Object[]> raw = usageLogRepository.findCostByProvider();
            return raw.stream()
                .map(row -> {
                    String provider = (String) row[0];
                    BigDecimal cost = row[1] instanceof BigDecimal
                        ? (BigDecimal) row[1]
                        : BigDecimal.valueOf(((Number) row[1]).doubleValue());
                    long count = ((Number) row[2]).longValue();
                    return new ProviderCostSummary(provider, cost, count);
                })
                .toList();
        } catch (Exception e) {
            log.warn("[overview] failed to fetch provider summaries: {}", e.getMessage());
            return List.of();
        }
    }
}
