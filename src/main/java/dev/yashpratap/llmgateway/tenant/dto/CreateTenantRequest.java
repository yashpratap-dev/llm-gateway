package dev.yashpratap.llmgateway.tenant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a new tenant.
 *
 * @param name the unique display name for the tenant; must not be blank
 * @param plan the subscription plan (e.g. FREE, PRO, ENTERPRISE); must not be blank
 */
public record CreateTenantRequest(
        @NotBlank(message = "Tenant name must not be blank") String name,
        @NotBlank(message = "Plan must not be blank") String plan) {
}
