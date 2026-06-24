package dev.yashpratap.llmgateway.billing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service responsible for enforcing per-tenant spending budgets.
 *
 * <p>The gateway calls {@link #checkBudget} before routing a request and
 * {@link #deductBudget} after a successful response. Full implementation
 * with optimistic-lock protection is delivered in M6.</p>
 */
@Service
public class BudgetService {

    /**
     * Verifies that the tenant has sufficient remaining budget for the estimated cost.
     *
     * @param tenantId      the UUID of the tenant making the request
     * @param estimatedCost an estimate of the USD cost before tokens are known
     * @throws dev.yashpratap.llmgateway.common.BudgetExceededException
     *         if {@code spentUsd + estimatedCost >= limitUsd}
     */
    public void checkBudget(UUID tenantId, BigDecimal estimatedCost) {
        // Full implementation in M6
    }

    /**
     * Atomically deducts the actual request cost from the tenant's budget.
     *
     * @param tenantId the UUID of the tenant to deduct from
     * @param cost     the actual USD cost calculated from token usage
     */
    public void deductBudget(UUID tenantId, BigDecimal cost) {
        // Full implementation in M6
    }
}
