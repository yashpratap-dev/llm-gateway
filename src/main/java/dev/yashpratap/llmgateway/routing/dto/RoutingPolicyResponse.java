package dev.yashpratap.llmgateway.routing.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for routing policy queries.
 *
 * @param tenantId  the UUID of the tenant
 * @param strategy  the active routing strategy (COST, LATENCY, or PRIORITY)
 * @param updatedAt when the policy was last changed, or {@code null} if using the default
 */
public record RoutingPolicyResponse(
        UUID tenantId,
        String strategy,
        LocalDateTime updatedAt
) {}
