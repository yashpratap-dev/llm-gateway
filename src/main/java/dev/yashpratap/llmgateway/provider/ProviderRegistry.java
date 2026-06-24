package dev.yashpratap.llmgateway.provider;

import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Central registry that holds all active {@link LLMProvider} beans.
 *
 * <p>Providers are injected by Spring's collection injection — only beans whose
 * {@code @ConditionalOnProperty} condition is satisfied (i.e. API key is set) are included.
 * At startup the registry validates that at least one provider is present, failing fast
 * if every provider key is missing from the environment.</p>
 */
@Component
public class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    private final List<LLMProvider> providers;

    /**
     * Constructs the registry; Spring auto-injects all registered {@link LLMProvider} beans.
     *
     * @param providers all {@link LLMProvider} implementations active in the application context
     */
    public ProviderRegistry(List<LLMProvider> providers) {
        this.providers = providers;
    }

    /**
     * Validates that at least one provider is configured, failing application startup otherwise.
     *
     * @throws IllegalStateException if the provider list is empty
     */
    @PostConstruct
    public void validate() {
        if (providers.isEmpty()) {
            throw new IllegalStateException(
                    "No LLMProvider beans registered. " +
                    "Check that at least one provider API key is configured " +
                    "(providers.groq.api-key or providers.openai.api-key).");
        }
        log.info("ProviderRegistry initialized with {} provider(s): {}",
                providers.size(),
                providers.stream().map(p -> p.name().name()).toList());
    }

    /**
     * Returns the provider matching the given name.
     *
     * @param name the provider identifier
     * @return the matching {@link LLMProvider}
     * @throws ProviderException if no provider is registered for {@code name}
     */
    public LLMProvider getProvider(ProviderName name) {
        return providers.stream()
                .filter(p -> p.name() == name)
                .findFirst()
                .orElseThrow(() -> new ProviderException("No provider found for: " + name));
    }

    /**
     * Returns all providers that are currently reporting healthy.
     *
     * @return immutable list of healthy providers; empty if all providers are down
     */
    public List<LLMProvider> getHealthyProviders() {
        return providers.stream()
                .filter(LLMProvider::isHealthy)
                .toList();
    }

    /**
     * Returns an unmodifiable view of every registered provider, regardless of health.
     *
     * @return all registered providers
     */
    public List<LLMProvider> getAllProviders() {
        return Collections.unmodifiableList(providers);
    }
}
