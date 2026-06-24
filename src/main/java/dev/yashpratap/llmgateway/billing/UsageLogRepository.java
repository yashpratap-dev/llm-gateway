package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UsageLog} entities.
 *
 * <p>Additional query methods for analytics aggregation (sum by tenant,
 * time-series grouping) are added in M8.</p>
 */
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {
}
