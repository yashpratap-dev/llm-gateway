package dev.yashpratap.llmgateway.common;

/**
 * Generic wrapper for all HTTP responses returned by the gateway's REST API.
 *
 * <p>Provides a consistent envelope so clients always receive the same top-level
 * structure regardless of whether the request succeeded or failed.</p>
 *
 * @param <T>       the type of the {@code data} payload
 * @param success   {@code true} for successful responses, {@code false} for errors
 * @param data      the response payload; {@code null} on error responses
 * @param message   a human-readable description; {@code null} on success responses
 * @param errorCode a machine-readable error code; {@code null} on success responses
 */
public record ApiResponse<T>(boolean success, T data, String message, String errorCode) {

    /**
     * Creates a successful response wrapping the given data payload.
     *
     * @param <T>  the payload type
     * @param data the response payload
     * @return an {@link ApiResponse} with {@code success = true}
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /**
     * Creates an error response with a human-readable message and error code.
     *
     * @param <T>       the payload type (always {@code Void} for error responses)
     * @param message   a human-readable description of the error
     * @param errorCode a machine-readable error code (e.g. {@code RATE_LIMIT_EXCEEDED})
     * @return an {@link ApiResponse} with {@code success = false}
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
}
