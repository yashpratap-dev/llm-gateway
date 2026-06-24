package dev.yashpratap.llmgateway.common;

import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import dev.yashpratap.llmgateway.provider.exception.ProviderTimeoutException;
import dev.yashpratap.llmgateway.provider.exception.ProviderUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handler that translates domain exceptions into
 * consistent {@link ApiResponse} HTTP responses.
 *
 * <p>Handlers are ordered most-specific first so Spring always dispatches to the
 * narrowest match. {@link ProviderUnavailableException} and {@link ProviderTimeoutException}
 * are declared before the base {@link ProviderException} handler to ensure they get
 * their own HTTP status codes.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@code @Valid} bean-validation failures on request bodies.
     *
     * @param ex the binding exception produced by Spring MVC
     * @return {@code 400 Bad Request} with all field error messages concatenated
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, "VALIDATION_ERROR"));
    }

    /**
     * Handles explicit validation exceptions thrown from service code.
     *
     * @param ex the validation exception
     * @return {@code 400 Bad Request} with error code {@code VALIDATION_ERROR}
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR"));
    }

    /**
     * Handles domain constraint violations such as duplicate tenant names.
     *
     * @param ex the illegal argument exception
     * @return {@code 409 Conflict} with error code {@code CONFLICT}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "CONFLICT"));
    }

    /**
     * Handles requests for resources that do not exist.
     *
     * @param ex the not-found exception
     * @return {@code 404 Not Found} with error code {@code NOT_FOUND}
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }

    /**
     * Handles requests that exceed the tenant's rate limit.
     *
     * @param ex the rate limit exception
     * @return {@code 429 Too Many Requests} with error code {@code RATE_LIMIT_EXCEEDED}
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitException(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ex.getMessage(), "RATE_LIMIT_EXCEEDED"));
    }

    /**
     * Handles requests rejected because the tenant's budget is exhausted.
     *
     * @param ex the budget exceeded exception
     * @return {@code 402 Payment Required} with error code {@code BUDGET_EXCEEDED}
     */
    @ExceptionHandler(BudgetExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleBudgetExceededException(BudgetExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error(ex.getMessage(), "BUDGET_EXCEEDED"));
    }

    /**
     * Handles the case where a downstream provider is unreachable or returns 5xx.
     *
     * @param ex the unavailable exception
     * @return {@code 502 Bad Gateway} with error code {@code PROVIDER_UNAVAILABLE}
     */
    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderUnavailable(ProviderUnavailableException ex) {
        log.error("Provider unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getMessage(), "PROVIDER_UNAVAILABLE"));
    }

    /**
     * Handles the case where a downstream provider exceeds its configured timeout.
     *
     * @param ex the timeout exception
     * @return {@code 504 Gateway Timeout} with error code {@code PROVIDER_TIMEOUT}
     */
    @ExceptionHandler(ProviderTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderTimeout(ProviderTimeoutException ex) {
        log.error("Provider timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error(ex.getMessage(), "PROVIDER_TIMEOUT"));
    }

    /**
     * Handles general provider errors not covered by more specific subclass handlers.
     *
     * @param ex the provider exception
     * @return {@code 502 Bad Gateway} with error code {@code PROVIDER_ERROR}
     */
    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderException(ProviderException ex) {
        log.error("Provider error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getMessage(), "PROVIDER_ERROR"));
    }

    /**
     * Catch-all handler for any unhandled exception.
     *
     * @param ex the unexpected exception
     * @return {@code 500 Internal Server Error} with error code {@code INTERNAL_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_ERROR"));
    }
}
