package dev.yashpratap.llmgateway.tenant;

import dev.yashpratap.llmgateway.domain.ApiKey;
import dev.yashpratap.llmgateway.domain.Tenant;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Request-scoped holder for the authenticated tenant and API key associated with
 * the current HTTP request.
 *
 * <p>Backed by Spring's {@link RequestScope}, which uses
 * {@link org.springframework.web.context.request.RequestContextHolder} internally,
 * providing per-request isolation without direct {@link ThreadLocal} usage in
 * application code.</p>
 *
 * <p>Populated by {@link dev.yashpratap.llmgateway.security.ApiKeyFilter} immediately
 * after successful authentication. All downstream services (budget check, usage logger)
 * read from this context rather than re-fetching the tenant from the database.</p>
 */
@Component
@RequestScope
public class TenantContext {

    private UUID tenantId;
    private UUID apiKeyId;
    private Tenant tenant;
    private ApiKey apiKey;

    /**
     * Returns the UUID of the authenticated tenant for the current request.
     *
     * @return the tenant UUID, or {@code null} if the request has not been authenticated
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Sets the UUID of the authenticated tenant.
     *
     * @param tenantId the tenant UUID resolved during authentication
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Returns the UUID of the API key used to authenticate this request.
     *
     * @return the API key UUID, or {@code null} before authentication
     */
    public UUID getApiKeyId() {
        return apiKeyId;
    }

    /**
     * Sets the UUID of the API key that authenticated this request.
     *
     * @param apiKeyId the authenticated API key UUID
     */
    public void setApiKeyId(UUID apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    /**
     * Returns the authenticated tenant entity for the current request.
     *
     * @return the {@link Tenant}, or {@code null} before authentication
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Sets the authenticated tenant entity and derives the tenant ID from it.
     *
     * @param tenant the authenticated {@link Tenant}
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
        this.tenantId = tenant != null ? tenant.getId() : null;
    }

    /**
     * Returns the {@link ApiKey} entity used to authenticate the current request.
     *
     * @return the authenticated {@link ApiKey}, or {@code null} before authentication
     */
    public ApiKey getApiKey() {
        return apiKey;
    }

    /**
     * Sets the {@link ApiKey} that authenticated this request.
     *
     * @param apiKey the authenticated API key entity
     */
    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Returns {@code true} when this context has been populated by the authentication filter.
     *
     * @return {@code true} if a tenant ID is present
     */
    public boolean isAuthenticated() {
        return tenantId != null;
    }
}
