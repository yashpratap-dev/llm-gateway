package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * {@link Router} implementation that selects the provider with the lowest per-token cost.
 *
 * <p>Costs are currently hardcoded as USD per 1 000 tokens (input). M8 will replace
 * this with real-time lookups from the {@code model_pricing} table.</p>
 */
@Component
public class CostRouter implements Router {

    private static final Map<ProviderName, Double> COST_MAP = Map.of(
            ProviderName.GROQ, 0.000059,
            ProviderName.OPENAI, 0.000150
    );

    /** {@inheritDoc} */
    @Override
    public RoutingStrategy strategy() {
        return RoutingStrategy.COST;
    }

    /**
     * Selects the provider with the lowest cost-per-token from the supplied pool.
     *
     * @param request   the incoming chat request
     * @param providers healthy providers to choose from
     * @return the cheapest provider
     * @throws ProviderException if the provider list is empty
     */
    @Override
    public LLMProvider route(ChatRequest request, List<LLMProvider> providers) {
        return providers.stream()
                .min(Comparator.comparingDouble(p -> COST_MAP.getOrDefault(p.name(), Double.MAX_VALUE)))
                .orElseThrow(() -> new ProviderException("No providers available for cost routing"));
    }
}
