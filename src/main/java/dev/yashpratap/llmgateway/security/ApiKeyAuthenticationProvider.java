package dev.yashpratap.llmgateway.security;

import dev.yashpratap.llmgateway.common.KeyHashUtil;
import dev.yashpratap.llmgateway.domain.ApiKey;
import dev.yashpratap.llmgateway.domain.ApiKeyStatus;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.tenant.ApiKeyRepository;
import dev.yashpratap.llmgateway.tenant.ApiKeyUsedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Security {@link AuthenticationProvider} that validates API keys against
 * the tenant store.
 *
 * <p>Accepts {@link ApiKeyAuthenticationToken} instances only. Authentication proceeds
 * in three steps:</p>
 * <ol>
 *   <li>Hash the raw key with SHA-256 and look it up in the database.</li>
 *   <li>Verify the key status is {@link ApiKeyStatus#ACTIVE}.</li>
 *   <li>Return an authenticated token carrying the owning {@link Tenant} as principal.</li>
 * </ol>
 *
 * <p>The method is {@link Transactional} (read-only) so that the lazily-associated
 * {@link Tenant} can be loaded within the same JPA session opened by the repository call.</p>
 */
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the provider with its required dependencies.
     *
     * @param apiKeyRepository JPA repository used for hash-based key lookup
     * @param eventPublisher   Spring event bus for publishing async usage events
     */
    public ApiKeyAuthenticationProvider(ApiKeyRepository apiKeyRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.apiKeyRepository = apiKeyRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Authenticates the API key carried by the supplied token.
     *
     * @param authentication an unauthenticated {@link ApiKeyAuthenticationToken}
     * @return an authenticated {@link ApiKeyAuthenticationToken} with {@code ROLE_API_USER}
     * @throws BadCredentialsException if the key hash is not found or the key is revoked
     */
    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String rawKey = (String) token.getCredentials();
        String keyHash = KeyHashUtil.hash(rawKey);

        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid API key"));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new BadCredentialsException("API key has been revoked");
        }

        Tenant tenant = apiKey.getTenant();

        eventPublisher.publishEvent(new ApiKeyUsedEvent(apiKey.getId(), LocalDateTime.now()));

        return new ApiKeyAuthenticationToken(
                tenant,
                apiKey.getId(),
                List.of(new SimpleGrantedAuthority("ROLE_API_USER")));
    }

    /**
     * Indicates that this provider handles {@link ApiKeyAuthenticationToken} only.
     *
     * @param authentication the authentication class to test
     * @return {@code true} when {@code authentication} is or extends {@link ApiKeyAuthenticationToken}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
