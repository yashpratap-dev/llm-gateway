package dev.yashpratap.llmgateway.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the {@link RoutingStrategy} for a tenant from the database.
 *
 * <p>Falls back to {@link RoutingStrategy#PRIORITY} when no policy is configured
 * or the stored strategy string does not match a known enum constant.</p>
 */
@Service
public class RoutingPolicyService {

    private static final Logger log = LoggerFactory.getLogger(RoutingPolicyService.class);

    private final RoutingPolicyRepository routingPolicyRepository;

    /**
     * Constructs the routing policy service with its repository dependency.
     *
     * @param routingPolicyRepository repository for tenant routing policy records
     */
    public RoutingPolicyService(RoutingPolicyRepository routingPolicyRepository) {
        this.routingPolicyRepository = routingPolicyRepository;
    }

    /**
     * Returns the active {@link RoutingStrategy} for the given tenant.
     *
     * <p>If the tenant has no configured policy, or if the stored strategy string cannot be
     * parsed as a {@link RoutingStrategy} enum constant, {@link RoutingStrategy#PRIORITY}
     * is returned as a safe default.</p>
     *
     * @param tenantId the UUID of the tenant whose strategy to resolve
     * @return the resolved {@link RoutingStrategy}, defaulting to {@link RoutingStrategy#PRIORITY}
     */
    public RoutingStrategy getStrategyForTenant(UUID tenantId) {
        return routingPolicyRepository.findByTenantId(tenantId)
                .map(policy -> {
                    try {
                        return RoutingStrategy.valueOf(policy.getStrategy().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown routing strategy '{}' for tenant {}, falling back to PRIORITY",
                                policy.getStrategy(), tenantId);
                        return RoutingStrategy.PRIORITY;
                    }
                })
                .orElse(RoutingStrategy.PRIORITY);
    }
}
