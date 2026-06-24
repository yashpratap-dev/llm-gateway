package dev.yashpratap.llmgateway.tenant.dto;

import dev.yashpratap.llmgateway.domain.ApiKey;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only view of an {@link ApiKey} returned by the admin API.
 *
 * <p>The raw key value is never included in this response — use
 * {@link GeneratedApiKeyResponse} which is returned only once at creation time.</p>
 *
 * @param id        surrogate key of the API key record
 * @param keyPrefix the short prefix of the raw key (e.g. {@code lgw_a1b2})
 * @param name      human-readable label set by the tenant
 * @param status    current lifecycle status: {@code ACTIVE} or {@code REVOKED}
 * @param createdAt timestamp when the key was generated
 */
public record ApiKeyResponse(UUID id, String keyPrefix, String name, String status, LocalDateTime createdAt) {

    /**
     * Maps an {@link ApiKey} entity to this response record.
     *
     * @param apiKey the entity to convert
     * @return the corresponding {@link ApiKeyResponse}
     */
    public static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getKeyPrefix(),
                apiKey.getName(),
                apiKey.getStatus().name(),
                apiKey.getCreatedAt());
    }
}
