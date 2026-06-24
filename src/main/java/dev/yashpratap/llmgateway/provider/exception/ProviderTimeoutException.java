package dev.yashpratap.llmgateway.provider.exception;

/**
 * Thrown when a provider exceeds its configured response timeout.
 *
 * <p>Mapped to {@code 504 Gateway Timeout} by
 * {@link dev.yashpratap.llmgateway.common.GlobalExceptionHandler}.
 * The M6 circuit breaker will track these events to open the circuit
 * after repeated timeouts.</p>
 */
public class ProviderTimeoutException extends ProviderException {

    /**
     * Constructs a timeout exception for the named provider.
     *
     * @param providerName the display name of the provider that timed out
     */
    public ProviderTimeoutException(String providerName) {
        super("Provider " + providerName + " timed out");
    }

    /**
     * Constructs a timeout exception for the named provider with a root cause.
     *
     * @param providerName the display name of the provider that timed out
     * @param cause        the underlying exception from the reactive timeout signal
     */
    public ProviderTimeoutException(String providerName, Throwable cause) {
        super("Provider " + providerName + " timed out", cause);
    }
}
