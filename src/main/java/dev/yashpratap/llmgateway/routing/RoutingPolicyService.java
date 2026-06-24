package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.domain.RoutingPolicy;
import dev.yashpratap.llmgateway.routing.dto.RoutingPolicyResponse;
import dev.yashpratap.llmgateway.tenant.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resolves and manages the {@link RoutingStrategy} configured for each tenant.
 *
 * <p>Falls back to {@link RoutingStrategy#PRIORITY} when no policy is configured
 * or the stored strategy string does not match a known enum constant.</p>
 */
@Service
public class RoutingPolicyService {

    private static final Logger log = LoggerFactory.getLogger(RoutingPolicyService.class);

    private final RoutingPolicyRepository routingPolicyRepository;
    private final TenantRepository tenantRepository;

    /**
     * Constructs the routing policy service with its dependencies.
     *
     * @param routingPolicyRepository repository for tenant routing policy records
     * @param tenantRepository        repository for tenant entity lookups and proxy references
     */
    public RoutingPolicyService(RoutingPolicyRepository routingPolicyRepository,
                                TenantRepository tenantRepository) {
        this.routingPolicyRepository = routingPolicyRepository;
        this.tenantRepository = tenantRepository;
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

    /**
     * Returns the routing policy DTO for a tenant.
     * Returns PRIORITY as the default strategy if no policy is configured.
     *
     * @param tenantId the UUID of the tenant whose policy to retrieve
     * @return the current {@link RoutingPolicyResponse}, with a PRIORITY default if unconfigured
     */
    public RoutingPolicyResponse getPolicyForTenant(UUID tenantId) {
        return routingPolicyRepository.findByTenantId(tenantId)
                .map(policy -> new RoutingPolicyResponse(
                        tenantId,
                        policy.getStrategy(),
                        policy.getUpdatedAt()))
                .orElse(new RoutingPolicyResponse(tenantId, "PRIORITY", null));
    }

    /**
     * Updates or creates the routing policy for a tenant.
     *
     * <p>Validates that {@code strategy} is a known {@link RoutingStrategy} value before
     * persisting. Upserts: creates a new record if none exists, updates the existing one otherwise.</p>
     *
     * @param tenantId the UUID of the tenant whose policy to update
     * @param strategy the desired routing strategy string (case-insensitive)
     * @return the persisted {@link RoutingPolicyResponse}
     * @throws IllegalArgumentException if {@code strategy} is not a valid {@link RoutingStrategy}
     * @throws EntityNotFoundException  if no tenant exists with the given {@code tenantId}
     */
    @Transactional
    public RoutingPolicyResponse updatePolicy(UUID tenantId, String strategy) {
        try {
            RoutingStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid routing strategy: " + strategy +
                            ". Valid values: COST, LATENCY, PRIORITY");
        }

        if (!tenantRepository.existsById(tenantId)) {
            throw new EntityNotFoundException("Tenant not found: " + tenantId);
        }

        RoutingPolicy policy = routingPolicyRepository.findByTenantId(tenantId)
                .orElse(new RoutingPolicy());
        policy.setTenant(tenantRepository.getReferenceById(tenantId));
        policy.setStrategy(strategy.toUpperCase());
        policy.setUpdatedAt(LocalDateTime.now());
        RoutingPolicy saved = routingPolicyRepository.save(policy);

        return new RoutingPolicyResponse(tenantId, saved.getStrategy(), saved.getUpdatedAt());
    }
}
