package dev.yashpratap.llmgateway.tenant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One-time response returned immediately after a new API key is generated.
 *
 * <p><strong>Security note:</strong> {@code rawKey} is the only time the full API key
 * value is visible. It is never stored and cannot be retrieved again. Clients must
 * persist it securely at creation time.</p>
 *
 * @param id        surrogate key of the API key record
 * @param keyPrefix the short prefix of the raw key shown in the UI
 * @param name      human-readable label set by the tenant
 * @param rawKey    the full API key — presented exactly once, never stored in plain text
 * @param status    lifecycle status at creation time (always {@code ACTIVE})
 * @param createdAt timestamp when the key was generated
 */
public record GeneratedApiKeyResponse(
        UUID id,
        String keyPrefix,
        String name,
        String rawKey,
        String status,
        LocalDateTime createdAt) {
}
