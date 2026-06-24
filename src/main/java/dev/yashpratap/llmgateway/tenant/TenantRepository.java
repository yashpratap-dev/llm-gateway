package dev.yashpratap.llmgateway.tenant;

import dev.yashpratap.llmgateway.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Tenant} entities.
 *
 * <p>Additional query methods (lookup by name, search by plan) are added
 * as needed in M2.</p>
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Looks up a tenant by its unique name.
     *
     * @param name the tenant name to search for
     * @return an {@link Optional} containing the matching tenant, or empty if not found
     */
    Optional<Tenant> findByName(String name);
}
