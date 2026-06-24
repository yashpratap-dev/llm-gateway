package dev.yashpratap.llmgateway.provider.exception;

/**
 * Base exception for all downstream LLM provider failures.
 *
 * <p>Caught by {@link dev.yashpratap.llmgateway.common.GlobalExceptionHandler}
 * and mapped to {@code 502 Bad Gateway}. Subclasses carry more specific failure
 * semantics (timeout, unavailable) and are mapped to different HTTP status codes.</p>
 */
public class ProviderException extends RuntimeException {

    /**
     * Constructs a {@code ProviderException} with a detail message.
     *
     * @param message human-readable description of the provider error
     */
    public ProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ProviderException} with a detail message and root cause.
     *
     * @param message human-readable description of the provider error
     * @param cause   the upstream exception that triggered this error
     */
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
