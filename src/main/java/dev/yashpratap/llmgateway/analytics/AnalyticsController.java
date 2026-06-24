package dev.yashpratap.llmgateway.analytics;

import dev.yashpratap.llmgateway.billing.BudgetRepository;
import dev.yashpratap.llmgateway.billing.UsageLogRepository;
import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.domain.Budget;
import dev.yashpratap.llmgateway.domain.UsageLog;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing admin-level analytics and budget endpoints.
 *
 * <p>All endpoints require a valid {@code Authorization: Bearer} API key enforced by
 * {@link dev.yashpratap.llmgateway.security.ApiKeyFilter}.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AnalyticsController {

    private final UsageLogRepository usageLogRepository;
    private final BudgetRepository budgetRepository;

    /**
     * Constructs the controller with its repository dependencies.
     *
     * @param usageLogRepository repository for querying usage log entries
     * @param budgetRepository   repository for querying tenant budgets
     */
    public AnalyticsController(UsageLogRepository usageLogRepository,
                               BudgetRepository budgetRepository) {
        this.usageLogRepository = usageLogRepository;
        this.budgetRepository = budgetRepository;
    }

    /**
     * Returns all usage log entries for a tenant within a specified date range.
     *
     * @param tenantId the UUID of the tenant to query
     * @param from     the inclusive start of the date range (ISO-8601 format)
     * @param to       the inclusive end of the date range (ISO-8601 format)
     * @return {@code 200 OK} with the list of matching {@link UsageLog} entries
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<List<UsageLog>>> getUsage(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<UsageLog> logs = usageLogRepository.findByTenantIdAndDateRange(tenantId, from, to);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Returns aggregated cost and request count grouped by provider.
     *
     * @return {@code 200 OK} with a list of {@link ProviderCostSummary} entries, one per provider
     */
    @GetMapping("/by-provider")
    public ResponseEntity<ApiResponse<List<ProviderCostSummary>>> getByProvider() {
        List<Object[]> raw = usageLogRepository.findCostByProvider();
        List<ProviderCostSummary> summaries = raw.stream()
                .map(row -> new ProviderCostSummary(
                        (String) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    /**
     * Returns the budget status for the specified tenant.
     *
     * @param tenantId the UUID of the tenant whose budget to retrieve
     * @return {@code 200 OK} with the tenant's {@link BudgetSummary}
     * @throws EntityNotFoundException if no budget record exists for the tenant
     */
    @GetMapping("/budget/{tenantId}")
    public ResponseEntity<ApiResponse<BudgetSummary>> getBudget(@PathVariable UUID tenantId) {
        Budget budget = budgetRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Budget not found for tenant: " + tenantId));

        BigDecimal remaining = budget.getLimitUsd().subtract(budget.getSpentUsd())
                .max(BigDecimal.ZERO);

        return ResponseEntity.ok(ApiResponse.success(
                new BudgetSummary(budget.getLimitUsd(), budget.getSpentUsd(), remaining)));
    }
}
