package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Budget} entities.
 *
 * <p>Provides tenant-scoped budget lookups and an atomic increment operation
 * used by {@link BudgetService} to deduct costs after each successful request.</p>
 */
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * Finds the budget record for the given tenant.
     *
     * <p>Spring Data JPA resolves {@code tenantId} via property traversal on the
     * {@code tenant.id} association.</p>
     *
     * @param tenantId the UUID of the tenant whose budget to retrieve
     * @return an {@link Optional} containing the tenant's budget, or empty if none exists
     */
    Optional<Budget> findByTenantId(UUID tenantId);

    /**
     * Atomically adds {@code cost} to the tenant's accumulated spend.
     *
     * <p>Uses a SQL {@code UPDATE} with an arithmetic expression ({@code spent_usd + :cost})
     * to prevent concurrent overcounting without requiring application-level locking.</p>
     *
     * @param tenantId the UUID of the tenant to charge
     * @param cost     the USD amount to add to {@code spent_usd}
     */
    @Modifying
    @Transactional
    @Query("UPDATE Budget b SET b.spentUsd = b.spentUsd + :cost WHERE b.tenant.id = :tenantId")
    void incrementSpent(@Param("tenantId") UUID tenantId, @Param("cost") BigDecimal cost);
}
