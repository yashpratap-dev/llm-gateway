package dev.yashpratap.llmgateway.billing;

import org.springframework.stereotype.Service;

/**
 * Calculates the USD cost of a completed LLM request using the {@code model_pricing} table.
 *
 * <p>Returns {@code 0.0} when no pricing record is found for the provider/model combination,
 * so the gateway never rejects a request solely due to missing pricing data.</p>
 */
@Service
public class CostCalculator {

    private final ModelPricingRepository modelPricingRepository;

    /**
     * Constructs the cost calculator with its pricing repository dependency.
     *
     * @param modelPricingRepository repository for per-model pricing data
     */
    public CostCalculator(ModelPricingRepository modelPricingRepository) {
        this.modelPricingRepository = modelPricingRepository;
    }

    /**
     * Calculates the total USD cost for a request given token counts and the model used.
     *
     * @param provider         the provider identifier (e.g. {@code GROQ}, {@code OPENAI})
     * @param modelName        the model identifier as reported in the provider response
     * @param promptTokens     the number of input tokens billed
     * @param completionTokens the number of output tokens billed
     * @return the total USD cost, or {@code 0.0} if pricing is not configured for this model
     */
    public double calculate(String provider, String modelName, int promptTokens, int completionTokens) {
        return modelPricingRepository.findByProviderAndModelName(provider, modelName)
                .map(pricing -> {
                    double input = (promptTokens / 1000.0) * pricing.getInputCostPer1k().doubleValue();
                    double output = (completionTokens / 1000.0) * pricing.getOutputCostPer1k().doubleValue();
                    return input + output;
                })
                .orElse(0.0);
    }
}
