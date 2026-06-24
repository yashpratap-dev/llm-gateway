package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.common.KeyHashUtil;
import dev.yashpratap.llmgateway.domain.ApiKey;
import dev.yashpratap.llmgateway.domain.ApiKeyStatus;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.security.ApiKeyAuthenticationProvider;
import dev.yashpratap.llmgateway.security.ApiKeyAuthenticationToken;
import dev.yashpratap.llmgateway.tenant.ApiKeyRepository;
import dev.yashpratap.llmgateway.tenant.ApiKeyUsedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiKeyAuthenticationProvider}.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationProviderTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ApiKeyAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ApiKeyAuthenticationProvider(apiKeyRepository, eventPublisher);
    }

    @Test
    void testAuthenticate_validKey_returnsAuthenticatedToken() {
        String rawKey = "lgw_validkey123";
        String keyHash = KeyHashUtil.hash(rawKey);

        Tenant tenant = new Tenant();
        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(tenant);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(apiKey));

        Authentication result = provider.authenticate(new ApiKeyAuthenticationToken(rawKey));

        assertThat(result).isInstanceOf(ApiKeyAuthenticationToken.class);
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(tenant);
        verify(eventPublisher).publishEvent(any(ApiKeyUsedEvent.class));
    }

    @Test
    void testAuthenticate_keyNotFound_throwsBadCredentials() {
        String rawKey = "lgw_unknownkey";
        when(apiKeyRepository.findByKeyHash(KeyHashUtil.hash(rawKey))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(new ApiKeyAuthenticationToken(rawKey)))
                .isInstanceOf(BadCredentialsException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testAuthenticate_revokedKey_throwsBadCredentials() {
        String rawKey = "lgw_revokedkey";
        String keyHash = KeyHashUtil.hash(rawKey);

        ApiKey revokedKey = new ApiKey();
        revokedKey.setStatus(ApiKeyStatus.REVOKED);

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(revokedKey));

        assertThatThrownBy(() -> provider.authenticate(new ApiKeyAuthenticationToken(rawKey)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testSupports_correctClass_returnsTrue() {
        assertThat(provider.supports(ApiKeyAuthenticationToken.class)).isTrue();
    }
}
