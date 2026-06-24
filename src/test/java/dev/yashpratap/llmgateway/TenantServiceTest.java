package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.domain.ApiKey;
import dev.yashpratap.llmgateway.domain.ApiKeyStatus;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.tenant.ApiKeyRepository;
import dev.yashpratap.llmgateway.tenant.TenantRepository;
import dev.yashpratap.llmgateway.tenant.TenantService;
import dev.yashpratap.llmgateway.tenant.dto.ApiKeyResponse;
import dev.yashpratap.llmgateway.tenant.dto.GeneratedApiKeyResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantService}.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, apiKeyRepository, eventPublisher);
    }

    @Test
    void testGenerateApiKey_validTenant_returnsKeyWithPrefix() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();

        ApiKey savedKey = new ApiKey();
        savedKey.setTenant(tenant);
        savedKey.setKeyPrefix("lgw_a1b2");
        savedKey.setName("ci-key");
        savedKey.setStatus(ApiKeyStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            // simulate @PrePersist
            try {
                var f = ApiKey.class.getDeclaredField("createdAt");
                f.setAccessible(true);
                f.set(k, LocalDateTime.now());
                var idF = ApiKey.class.getDeclaredField("id");
                idF.setAccessible(true);
                idF.set(k, UUID.randomUUID());
            } catch (Exception ignored) { }
            return k;
        });

        GeneratedApiKeyResponse response = tenantService.generateApiKey(tenantId, "ci-key");

        assertThat(response.rawKey()).isNotBlank();
        assertThat(response.keyPrefix()).startsWith("lgw_");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void testGenerateApiKey_keyStartsWithLgw() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            try {
                var f = ApiKey.class.getDeclaredField("createdAt");
                f.setAccessible(true);
                f.set(k, LocalDateTime.now());
                var idF = ApiKey.class.getDeclaredField("id");
                idF.setAccessible(true);
                idF.set(k, UUID.randomUUID());
            } catch (Exception ignored) { }
            return k;
        });

        GeneratedApiKeyResponse response = tenantService.generateApiKey(tenantId, "my-key");

        assertThat(response.rawKey()).startsWith("lgw_");
    }

    @Test
    void testGenerateApiKey_invalidTenant_throwsEntityNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(tenantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.generateApiKey(unknownId, "key"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void testRevokeApiKey_setsStatusRevoked() {
        UUID keyId = UUID.randomUUID();

        ApiKey existing = new ApiKey();
        existing.setStatus(ApiKeyStatus.ACTIVE);
        existing.setName("prod-key");
        existing.setKeyPrefix("lgw_ab12");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(captor.capture())).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            try {
                var f = ApiKey.class.getDeclaredField("createdAt");
                f.setAccessible(true);
                f.set(k, LocalDateTime.now());
                var idF = ApiKey.class.getDeclaredField("id");
                idF.setAccessible(true);
                idF.set(k, keyId);
            } catch (Exception ignored) { }
            return k;
        });

        ApiKeyResponse response = tenantService.revokeApiKey(keyId);

        assertThat(captor.getValue().getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(response.status()).isEqualTo("REVOKED");
    }
}
