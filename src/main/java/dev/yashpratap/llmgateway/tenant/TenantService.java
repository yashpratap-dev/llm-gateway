package dev.yashpratap.llmgateway.tenant;

import dev.yashpratap.llmgateway.domain.ApiKey;
import dev.yashpratap.llmgateway.domain.ApiKeyStatus;
import dev.yashpratap.llmgateway.domain.Tenant;
import dev.yashpratap.llmgateway.tenant.dto.GeneratedApiKeyResponse;
import dev.yashpratap.llmgateway.tenant.dto.ApiKeyResponse;
import dev.yashpratap.llmgateway.tenant.dto.CreateTenantRequest;
import dev.yashpratap.llmgateway.common.KeyHashUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for tenant and API key lifecycle management.
 *
 * <p>Centralises all business rules around tenant creation, key generation, and
 * key revocation. Raw API key values are generated here and immediately discarded
 * after returning {@link GeneratedApiKeyResponse} — they are never stored.</p>
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Constructs the service with all required dependencies.
     *
     * @param tenantRepository  JPA repository for tenant persistence
     * @param apiKeyRepository  JPA repository for API key persistence
     * @param eventPublisher    Spring event bus used for async notifications
     */
    public TenantService(TenantRepository tenantRepository,
                         ApiKeyRepository apiKeyRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new tenant with a unique name.
     *
     * @param request the tenant creation request containing name and plan
     * @return the newly persisted {@link Tenant} entity
     * @throws IllegalArgumentException if a tenant with the same name already exists
     */
    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        if (tenantRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException(
                    "Tenant with name '" + request.name() + "' already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setPlan(request.plan());
        return tenantRepository.save(tenant);
    }

    /**
     * Returns all tenants registered in the system.
     *
     * @return list of all {@link Tenant} records
     */
    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Finds a tenant by its surrogate UUID.
     *
     * @param id the tenant UUID
     * @return an {@link Optional} containing the tenant, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> getTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    /**
     * Generates a new API key for the specified tenant.
     *
     * <p>The raw key format is {@code lgw_<Base64URL(32 random bytes)>}. Only the
     * SHA-256 hash and the first 8 characters (prefix) are stored; the raw value
     * is returned exactly once in the response and never retrievable afterwards.</p>
     *
     * @param tenantId the UUID of the tenant to generate the key for
     * @param keyName  a human-readable label for the new key
     * @return a {@link GeneratedApiKeyResponse} containing the raw key — save it now
     * @throws EntityNotFoundException if no tenant exists with the given ID
     */
    @Transactional
    public GeneratedApiKeyResponse generateApiKey(UUID tenantId, String keyName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        String rawKey;
        String keyHash;
        int attempts = 0;
        do {
            if (attempts++ > 5) {
                throw new IllegalStateException("Failed to generate unique API key after 5 attempts");
            }
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            rawKey = "lgw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            keyHash = KeyHashUtil.hash(rawKey);
        } while (apiKeyRepository.findByKeyHash(keyHash).isPresent());

        String keyPrefix = rawKey.substring(0, 8);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(tenant);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(keyName);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        ApiKey saved = apiKeyRepository.save(apiKey);

        return new GeneratedApiKeyResponse(
                saved.getId(),
                saved.getKeyPrefix(),
                saved.getName(),
                rawKey,
                saved.getStatus().name(),
                saved.getCreatedAt());
    }

    /**
     * Revokes an API key, preventing it from being used for future authentication.
     *
     * @param apiKeyId the UUID of the API key to revoke
     * @return an {@link ApiKeyResponse} reflecting the updated REVOKED status
     * @throws EntityNotFoundException if no API key exists with the given ID
     */
    @Transactional
    public ApiKeyResponse revokeApiKey(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new EntityNotFoundException("API key not found: " + apiKeyId));
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        ApiKey saved = apiKeyRepository.save(apiKey);
        return ApiKeyResponse.from(saved);
    }

    /**
     * Returns all API keys belonging to the specified tenant.
     *
     * @param tenantId the UUID of the tenant
     * @return list of {@link ApiKey} entities; empty if the tenant has no keys
     */
    @Transactional(readOnly = true)
    public List<ApiKey> getApiKeysByTenant(UUID tenantId) {
        return apiKeyRepository.findByTenantId(tenantId);
    }
}
