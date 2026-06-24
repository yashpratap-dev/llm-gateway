package dev.yashpratap.llmgateway.routing.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for updating a tenant's routing policy.
 *
 * @param strategy the desired routing strategy; must be COST, LATENCY, or PRIORITY
 */
public record UpdateRoutingPolicyRequest(
        @NotBlank String strategy
) {}
