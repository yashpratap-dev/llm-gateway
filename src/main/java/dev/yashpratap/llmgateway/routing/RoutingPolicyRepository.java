package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.domain.RoutingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RoutingPolicy} entities.
 *
 * <p>Used by {@link RoutingPolicyService} to resolve the routing strategy configured
 * for a tenant. At most one policy record exists per tenant.</p>
 */
public interface RoutingPolicyRepository extends JpaRepository<RoutingPolicy, UUID> {

    /**
     * Finds the routing policy configured for the given tenant.
     *
     * <p>Spring Data JPA resolves {@code tenantId} via property traversal on the
     * {@code tenant.id} association.</p>
     *
     * @param tenantId the UUID of the tenant whose routing policy to retrieve
     * @return an {@link Optional} containing the routing policy, or empty if none is configured
     */
    Optional<RoutingPolicy> findByTenantId(UUID tenantId);
}
