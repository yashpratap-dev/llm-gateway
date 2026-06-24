package dev.yashpratap.llmgateway.controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response payload for the provider health endpoint.
 *
 * @param providers map of provider name to health status string ({@code "UP"} or {@code "DOWN"})
 * @param timestamp the moment at which health was sampled
 */
public record ProviderHealthResponse(
        Map<String, String> providers,
        LocalDateTime timestamp
) {}
