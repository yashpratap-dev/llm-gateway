package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;

import java.util.List;

/**
 * Strategy interface for selecting a single {@link LLMProvider} from the available pool.
 *
 * <p>Each concrete implementation embodies one routing algorithm.
 * {@link RoutingService} uses {@link #strategy()} to build a lookup map and dispatches
 * to the appropriate implementation at request time.</p>
 */
public interface Router {

    /**
     * Returns the {@link RoutingStrategy} constant that identifies this router.
     *
     * @return the strategy enum constant for this implementation
     */
    RoutingStrategy strategy();

    /**
     * Selects the best provider for the given request from the supplied pool.
     *
     * <p>The caller guarantees that {@code providers} contains only healthy providers;
     * implementations must not re-filter by health.</p>
     *
     * @param request   the incoming chat request (may include model preference)
     * @param providers the list of healthy candidate providers
     * @return the selected {@link LLMProvider}; never {@code null}
     * @throws dev.yashpratap.llmgateway.provider.exception.ProviderException
     *         if no suitable provider is available
     */
    LLMProvider route(ChatRequest request, List<LLMProvider> providers);
}
