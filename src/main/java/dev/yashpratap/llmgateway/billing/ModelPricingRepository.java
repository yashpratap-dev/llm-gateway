package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.ModelPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ModelPricing} entities.
 *
 * <p>Used by {@link CostCalculator} to retrieve per-token pricing for a given
 * provider and model combination.</p>
 */
public interface ModelPricingRepository extends JpaRepository<ModelPricing, UUID> {

    /**
     * Finds the pricing record for the given provider and model name combination.
     *
     * @param provider  the provider identifier (e.g. {@code GROQ}, {@code OPENAI})
     * @param modelName the model identifier as used by the provider API
     * @return an {@link Optional} containing the pricing entry, or empty if not seeded
     */
    Optional<ModelPricing> findByProviderAndModelName(String provider, String modelName);
}
