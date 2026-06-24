package dev.yashpratap.llmgateway.controller;

import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.tenant.TenantService;
import dev.yashpratap.llmgateway.tenant.dto.ApiKeyResponse;
import dev.yashpratap.llmgateway.tenant.dto.CreateApiKeyRequest;
import dev.yashpratap.llmgateway.tenant.dto.CreateTenantRequest;
import dev.yashpratap.llmgateway.tenant.dto.GeneratedApiKeyResponse;
import dev.yashpratap.llmgateway.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for administrative operations on tenants and API keys.
 *
 * <p>Most endpoints require {@code ROLE_API_USER} authentication enforced by
 * {@link dev.yashpratap.llmgateway.security.SecurityConfig}. The sole exception is
 * {@code POST /api/v1/admin/tenants} which is intentionally open for development
 * bootstrap — see the security config Javadoc for the production hardening steps.</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final TenantService tenantService;

    /**
     * Constructs the controller with its service dependency.
     *
     * @param tenantService the tenant and API key management service
     */
    public AdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Creates a new tenant.
     *
     * @param request validated request body containing tenant name and plan
     * @return {@code 201 Created} with the created tenant
     */
    @PostMapping("/tenants")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = TenantResponse.from(tenantService.createTenant(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Returns all tenants registered in the system.
     *
     * @return {@code 200 OK} with the list of all tenants
     */
    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
        List<TenantResponse> tenants = tenantService.getAllTenants()
                .stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    /**
     * Generates a new API key for the specified tenant.
     *
     * <p>The {@code rawKey} field in the response is presented exactly once.
     * It is not stored and cannot be retrieved again.</p>
     *
     * @param tenantId the UUID of the tenant to generate the key for
     * @param request  validated request body containing the key name
     * @return {@code 201 Created} with the generated key including the raw value
     */
    @PostMapping("/tenants/{tenantId}/keys")
    public ResponseEntity<ApiResponse<GeneratedApiKeyResponse>> generateApiKey(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        GeneratedApiKeyResponse response = tenantService.generateApiKey(tenantId, request.keyName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Returns all API keys belonging to the specified tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return {@code 200 OK} with the list of API key summaries (no raw values)
     */
    @GetMapping("/tenants/{tenantId}/keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getApiKeys(
            @PathVariable UUID tenantId) {
        List<ApiKeyResponse> keys = tenantService.getApiKeysByTenant(tenantId)
                .stream()
                .map(ApiKeyResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    /**
     * Revokes an API key, preventing all future use.
     *
     * @param keyId the UUID of the API key to revoke
     * @return {@code 200 OK} with the updated key record showing REVOKED status
     */
    @DeleteMapping("/keys/{keyId}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revokeApiKey(@PathVariable UUID keyId) {
        ApiKeyResponse response = tenantService.revokeApiKey(keyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
