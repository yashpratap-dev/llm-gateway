package dev.yashpratap.llmgateway.provider.exception;

/**
 * Thrown when a provider returns a 5xx response or is otherwise unreachable.
 *
 * <p>Mapped to {@code 502 Bad Gateway} by
 * {@link dev.yashpratap.llmgateway.common.GlobalExceptionHandler}.
 * The M6 circuit breaker tracks these failures to open the circuit
 * when the failure rate crosses the configured threshold.</p>
 */
public class ProviderUnavailableException extends ProviderException {

    /**
     * Constructs an unavailable exception for the named provider.
     *
     * @param providerName the display name of the provider that is unavailable
     */
    public ProviderUnavailableException(String providerName) {
        super("Provider " + providerName + " is unavailable");
    }

    /**
     * Constructs an unavailable exception for the named provider with a root cause.
     *
     * @param providerName the display name of the provider that is unavailable
     * @param cause        the upstream response or connection error
     */
    public ProviderUnavailableException(String providerName, Throwable cause) {
        super("Provider " + providerName + " is unavailable", cause);
    }
}
