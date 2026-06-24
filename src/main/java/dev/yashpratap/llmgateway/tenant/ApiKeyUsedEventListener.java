package dev.yashpratap.llmgateway.tenant;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous listener that keeps the {@code last_used_at} timestamp of an API key
 * up to date without blocking the request authentication path.
 *
 * <p>Runs in the {@code gateway-async-} thread pool defined in
 * {@link dev.yashpratap.llmgateway.config.AsyncConfig}. Each invocation opens its own
 * short-lived write transaction.</p>
 */
@Component
public class ApiKeyUsedEventListener {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Constructs the listener with its required repository.
     *
     * @param apiKeyRepository JPA repository for updating API key records
     */
    public ApiKeyUsedEventListener(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Updates the {@code lastUsedAt} field of the API key that raised this event.
     *
     * <p>If the key is not found (e.g. deleted between auth and this invocation) the
     * event is silently ignored — the update is best-effort.</p>
     *
     * @param event the event carrying the API key ID and the usage timestamp
     */
    @Async
    @EventListener
    @Transactional
    public void handleApiKeyUsed(ApiKeyUsedEvent event) {
        apiKeyRepository.findById(event.apiKeyId()).ifPresent(apiKey -> {
            apiKey.setLastUsedAt(event.usedAt());
            apiKeyRepository.save(apiKey);
        });
    }
}
