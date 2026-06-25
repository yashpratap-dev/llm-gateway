package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.analytics.AnalyticsOverviewService;
import dev.yashpratap.llmgateway.analytics.OverviewResponse;
import dev.yashpratap.llmgateway.billing.UsageLogRepository;
import dev.yashpratap.llmgateway.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalyticsOverviewService}.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsOverviewServiceTest {

    @Mock
    UsageLogRepository usageLogRepository;

    @Mock
    TenantRepository tenantRepository;

    AnalyticsOverviewService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsOverviewService(usageLogRepository, tenantRepository);
    }

    @Test
    void testGetOverview_noRequests_returnsZeroRatio() {
        when(usageLogRepository.count()).thenReturn(0L);
        when(usageLogRepository.findTotalCostAllTenants()).thenReturn(0.0);
        when(usageLogRepository.findCostByProvider()).thenReturn(List.of());
        when(tenantRepository.count()).thenReturn(2L);

        OverviewResponse result = service.getOverview();

        assertThat(result.totalRequests()).isEqualTo(0L);
        assertThat(result.cacheHitRatio()).isEqualTo(0.0);
        assertThat(result.activeTenantsCount()).isEqualTo(2L);
        assertThat(result.providers()).isEmpty();
    }

    @Test
    void testGetOverview_withRequests_computesCacheHitRatio() {
        when(usageLogRepository.count()).thenReturn(100L);
        when(usageLogRepository.findTotalCostAllTenants()).thenReturn(0.005);
        when(usageLogRepository.countCacheHits()).thenReturn(35L);
        when(usageLogRepository.findCostByProvider()).thenReturn(List.of());
        when(tenantRepository.count()).thenReturn(3L);

        OverviewResponse result = service.getOverview();

        assertThat(result.totalRequests()).isEqualTo(100L);
        assertThat(result.cacheHitRatio()).isEqualTo(0.35);
        assertThat(result.totalCostUsd()).isEqualTo(0.005);
    }

    @Test
    void testGetOverview_withProviders_buildsProviderSummaries() {
        when(usageLogRepository.count()).thenReturn(50L);
        when(usageLogRepository.findTotalCostAllTenants()).thenReturn(0.001);
        when(usageLogRepository.countCacheHits()).thenReturn(10L);
        when(tenantRepository.count()).thenReturn(1L);

        Object[] groqRow = new Object[]{"GROQ", new BigDecimal("0.0008"), 40L};
        Object[] openaiRow = new Object[]{"OPENAI", new BigDecimal("0.0002"), 10L};
        when(usageLogRepository.findCostByProvider()).thenReturn(List.of(groqRow, openaiRow));

        OverviewResponse result = service.getOverview();

        assertThat(result.providers()).hasSize(2);
        assertThat(result.providers().get(0).provider()).isEqualTo("GROQ");
        assertThat(result.providers().get(1).provider()).isEqualTo("OPENAI");
    }

    @Test
    void testGetOverview_repositoryThrows_returnsPartialData() {
        when(usageLogRepository.count()).thenReturn(10L);
        when(usageLogRepository.findTotalCostAllTenants()).thenThrow(new RuntimeException("DB error"));
        when(usageLogRepository.countCacheHits()).thenReturn(5L);
        when(usageLogRepository.findCostByProvider()).thenReturn(List.of());
        when(tenantRepository.count()).thenReturn(1L);

        OverviewResponse result = service.getOverview();

        assertThat(result.totalCostUsd()).isEqualTo(0.0);
        assertThat(result.totalRequests()).isEqualTo(10L);
    }
}
