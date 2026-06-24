package dev.yashpratap.llmgateway.security;

import dev.yashpratap.llmgateway.domain.Tenant;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security authentication token that carries an API key credential.
 *
 * <p>Two distinct states exist:</p>
 * <ul>
 *   <li><strong>Unauthenticated</strong> — created by {@link ApiKeyFilter} from the raw key
 *       extracted from the {@code Authorization} header. No authorities; {@code authenticated = false}.</li>
 *   <li><strong>Authenticated</strong> — created by {@link ApiKeyAuthenticationProvider} after a
 *       successful database lookup. Carries the resolved {@link Tenant} as principal.</li>
 * </ul>
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String rawKey;
    private final Tenant tenant;
    private final UUID apiKeyId;

    /**
     * Creates an unauthenticated token from the raw API key string.
     *
     * @param rawKey the raw API key extracted from the {@code Authorization: Bearer} header
     */
    public ApiKeyAuthenticationToken(String rawKey) {
        super(Collections.emptyList());
        this.rawKey = rawKey;
        this.tenant = null;
        this.apiKeyId = null;
        setAuthenticated(false);
    }

    /**
     * Creates an authenticated token after the API key has been validated.
     *
     * @param tenant      the tenant resolved from the database for the validated key
     * @param apiKeyId    the UUID of the API key record that was matched
     * @param authorities the granted authorities for this principal
     */
    public ApiKeyAuthenticationToken(Tenant tenant, UUID apiKeyId,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.rawKey = null;
        this.tenant = tenant;
        this.apiKeyId = apiKeyId;
        setAuthenticated(true);
    }

    /**
     * Returns the raw API key used as the credential.
     *
     * @return the raw key string when unauthenticated; {@code null} after authentication
     */
    @Override
    public Object getCredentials() {
        return rawKey;
    }

    /**
     * Returns the principal — the resolved {@link Tenant} on an authenticated token.
     *
     * @return the {@link Tenant} entity, or {@code null} before authentication
     */
    @Override
    public Object getPrincipal() {
        return tenant;
    }

    /**
     * Returns the resolved tenant entity.
     *
     * @return the authenticated tenant, or {@code null} on an unauthenticated token
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Returns the UUID of the API key record that was matched during authentication.
     *
     * @return the API key UUID, or {@code null} on an unauthenticated token
     */
    public UUID getApiKeyId() {
        return apiKeyId;
    }
}
