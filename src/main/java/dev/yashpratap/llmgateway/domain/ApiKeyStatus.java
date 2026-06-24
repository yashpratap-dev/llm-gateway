package dev.yashpratap.llmgateway.domain;

/**
 * Lifecycle status of an {@link ApiKey}.
 *
 * <p>Revoked keys are rejected immediately at the authentication filter level
 * without generating any usage log entry.</p>
 */
public enum ApiKeyStatus {

    /** Key is valid and will be accepted by the authentication filter. */
    ACTIVE,

    /** Key has been revoked and will be rejected at authentication time. */
    REVOKED
}
