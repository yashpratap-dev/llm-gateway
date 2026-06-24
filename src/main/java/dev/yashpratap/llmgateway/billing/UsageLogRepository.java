package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UsageLog} entities.
 *
 * <p>Provides both individual record persistence (via inherited {@code save}) and
 * analytical aggregation queries used by the admin analytics endpoints.</p>
 */
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

    /**
     * Returns all usage log entries for a tenant within the specified time range.
     *
     * @param tenantId the UUID of the tenant whose logs to retrieve
     * @param from     the inclusive start of the time range
     * @param to       the inclusive end of the time range
     * @return a list of matching usage log entries, ordered by persistence order
     */
    @Query("SELECT u FROM UsageLog u WHERE u.tenantId = :tenantId AND u.createdAt BETWEEN :from AND :to")
    List<UsageLog> findByTenantIdAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Returns aggregated cost and request count grouped by provider.
     *
     * <p>Each element in the result list is an {@code Object[]} with three elements:
     * {@code [String provider, BigDecimal totalCost, Long requestCount]}.</p>
     *
     * @return one row per provider with the total spend and request volume
     */
    @Query("SELECT u.provider, SUM(u.costUsd), COUNT(u) FROM UsageLog u GROUP BY u.provider")
    List<Object[]> findCostByProvider();

    /**
     * Returns the total USD cost accumulated across all requests for a tenant.
     *
     * <p>Returns {@code 0.0} (via {@code COALESCE}) when the tenant has no usage records.</p>
     *
     * @param tenantId the UUID of the tenant to aggregate
     * @return the total cost in USD, or {@code 0.0} if no records exist
     */
    @Query("SELECT COALESCE(SUM(u.costUsd), 0) FROM UsageLog u WHERE u.tenantId = :tenantId")
    Double findTotalCostByTenant(@Param("tenantId") UUID tenantId);
}
