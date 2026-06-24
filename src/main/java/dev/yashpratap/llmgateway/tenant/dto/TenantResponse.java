package dev.yashpratap.llmgateway.tenant.dto;

import dev.yashpratap.llmgateway.domain.Tenant;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@link Tenant} returned by the admin API.
 *
 * @param id        surrogate key of the tenant
 * @param name      unique display name
 * @param plan      subscription plan identifier
 * @param createdAt timestamp when the tenant was first created
 */
public record TenantResponse(UUID id, String name, String plan, LocalDateTime createdAt) {

    /**
     * Maps a {@link Tenant} entity to this response record.
     *
     * @param tenant the entity to convert
     * @return the corresponding {@link TenantResponse}
     */
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getPlan(),
                tenant.getCreatedAt());
    }
}
