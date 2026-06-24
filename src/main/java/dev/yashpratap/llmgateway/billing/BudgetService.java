package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.common.BudgetExceededException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Enforces per-tenant spending budgets.
 *
 * <p>{@link #checkBudget} is called synchronously before routing each request
 * and throws {@link BudgetExceededException} when the tenant has exhausted their limit.</p>
 *
 * <p>Race condition note: {@link #deductAsync} uses an atomic SQL {@code UPDATE}
 * ({@code spent_usd = spent_usd + cost}) via {@link BudgetRepository#incrementSpent}
 * to prevent concurrent overcounting without application-level locking.</p>
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    /**
     * Constructs the budget service with its repository dependency.
     *
     * @param budgetRepository JPA repository used for budget lookups and atomic spend updates
     */
    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    /**
     * Verifies that the tenant has not yet exhausted their spending budget.
     *
     * <p>Throws {@link BudgetExceededException} when {@code spentUsd >= limitUsd}.
     * If no budget record exists for the tenant, the check is skipped.</p>
     *
     * @param tenantId the UUID of the tenant making the request
     * @throws BudgetExceededException if the tenant's accumulated spend meets or exceeds the limit
     */
    public void checkBudget(UUID tenantId) {
        budgetRepository.findByTenantId(tenantId).ifPresent(budget -> {
            if (budget.getSpentUsd().compareTo(budget.getLimitUsd()) >= 0) {
                throw new BudgetExceededException(
                        "Budget exhausted for tenant: " + tenantId +
                                " (spent=" + budget.getSpentUsd() + ", limit=" + budget.getLimitUsd() + ")");
            }
        });
    }

    /**
     * Asynchronously deducts the actual request cost from the tenant's budget.
     *
     * <p>Runs on the {@code gateway-async-} thread pool defined in
     * {@link dev.yashpratap.llmgateway.config.AsyncConfig}. The update is atomic at the
     * database level — no application-level synchronisation is required.</p>
     *
     * @param tenantId the UUID of the tenant to charge
     * @param costUsd  the USD cost to deduct from the tenant's budget
     */
    @Async
    @Transactional
    public void deductAsync(UUID tenantId, double costUsd) {
        budgetRepository.incrementSpent(tenantId, BigDecimal.valueOf(costUsd));
    }
}
