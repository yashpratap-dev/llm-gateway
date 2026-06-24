package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link Router} implementation that selects providers using a fixed priority order:
 * Groq first, OpenAI second.
 *
 * <p>This is the default routing strategy. Because {@link RoutingService} pre-filters
 * the provider list to healthy providers only, this router achieves automatic failover:
 * if Groq is down it is absent from the supplied list, and OpenAI becomes the first match.</p>
 */
@Component
public class PriorityRouter implements Router {

    private static final List<ProviderName> PRIORITY_ORDER =
            List.of(ProviderName.GROQ, ProviderName.OPENAI);

    /** {@inheritDoc} */
    @Override
    public RoutingStrategy strategy() {
        return RoutingStrategy.PRIORITY;
    }

    /**
     * Selects the highest-priority provider from the supplied pool.
     *
     * <p>Falls back to the first available provider if none match the priority list,
     * ensuring forward compatibility when new providers are added.</p>
     *
     * @param request   the incoming chat request
     * @param providers healthy providers to choose from (pre-filtered by the caller)
     * @return the highest-priority provider present in the pool
     * @throws ProviderException if the provider list is empty
     */
    @Override
    public LLMProvider route(ChatRequest request, List<LLMProvider> providers) {
        if (providers.isEmpty()) {
            throw new ProviderException("No providers available for priority routing");
        }
        return PRIORITY_ORDER.stream()
                .flatMap(name -> providers.stream().filter(p -> p.name() == name))
                .findFirst()
                .orElse(providers.get(0));
    }
}
