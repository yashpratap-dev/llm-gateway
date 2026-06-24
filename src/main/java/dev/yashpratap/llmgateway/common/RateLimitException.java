package dev.yashpratap.llmgateway.common;

/**
 * Thrown when a tenant exceeds their configured request rate limit.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to a
 * {@code 429 Too Many Requests} HTTP response.</p>
 */
public class RateLimitException extends RuntimeException {

    /**
     * Constructs a {@code RateLimitException} with a detail message.
     *
     * @param message description of the rate limit that was exceeded
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code RateLimitException} with a detail message and root cause.
     *
     * @param message description of the rate limit that was exceeded
     * @param cause   the underlying exception (e.g. from Resilience4j)
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
