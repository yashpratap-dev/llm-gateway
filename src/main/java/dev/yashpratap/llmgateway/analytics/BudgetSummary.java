package dev.yashpratap.llmgateway.analytics;

import java.math.BigDecimal;

/**
 * Budget status summary for a single tenant.
 *
 * @param limit     the maximum USD spend allowed in the current billing period
 * @param spent     the accumulated USD spend so far
 * @param remaining the remaining budget ({@code limit - spent}), floored at zero
 */
public record BudgetSummary(BigDecimal limit, BigDecimal spent, BigDecimal remaining) {
}
