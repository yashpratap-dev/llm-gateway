package dev.yashpratap.llmgateway.common;

/**
 * Application-wide string constants shared across multiple layers.
 *
 * <p>Using an interface rather than a class prevents instantiation and
 * allows constants to be statically imported without a class qualifier.</p>
 */
public interface Constants {

    /** The base path prefix for all versioned API endpoints. */
    String API_VERSION = "/api/v1";

    /** HTTP {@code Authorization} header prefix for bearer tokens. */
    String BEARER_PREFIX = "Bearer ";
}
