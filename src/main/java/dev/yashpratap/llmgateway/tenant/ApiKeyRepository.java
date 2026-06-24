package dev.yashpratap.llmgateway.tenant;

import dev.yashpratap.llmgateway.domain.ApiKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ApiKey} entities.
 *
 * <p>The {@code findByKeyHash} method uses an entity graph to eagerly load the
 * associated {@link dev.yashpratap.llmgateway.domain.Tenant}, preventing a
 * {@code LazyInitializationException} when authentication runs outside a
 * long-lived JPA session.</p>
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Finds an API key by its SHA-256 hash, eagerly loading the owning tenant.
     *
     * @param keyHash the SHA-256 hash of the raw API key
     * @return the matching {@link ApiKey} with its tenant pre-loaded, or empty if not found
     */
    @EntityGraph(attributePaths = {"tenant"})
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Returns all API keys belonging to the specified tenant.
     *
     * <p>Spring Data resolves {@code TenantId} as the path {@code tenant.id} via
     * property traversal, so no explicit {@code @Query} is required.</p>
     *
     * @param tenantId the UUID of the owning tenant
     * @return a list of API keys; empty if the tenant has none
     */
    List<ApiKey> findByTenantId(UUID tenantId);
}
