package dev.yashpratap.llmgateway.common;

/**
 * Thrown when a tenant's spending budget for the current period is exhausted.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to a
 * {@code 402 Payment Required} HTTP response.</p>
 */
public class BudgetExceededException extends RuntimeException {

    /**
     * Constructs a {@code BudgetExceededException} with a detail message.
     *
     * @param message description identifying the tenant and the exceeded budget
     */
    public BudgetExceededException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code BudgetExceededException} with a detail message and root cause.
     *
     * @param message description identifying the tenant and the exceeded budget
     * @param cause   the underlying exception
     */
    public BudgetExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
