package dev.yashpratap.llmgateway.tenant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for generating a new API key for a tenant.
 *
 * @param keyName a human-readable label for the key (e.g. "production", "ci-pipeline")
 */
public record CreateApiKeyRequest(
        @NotBlank(message = "Key name must not be blank") String keyName) {
}
