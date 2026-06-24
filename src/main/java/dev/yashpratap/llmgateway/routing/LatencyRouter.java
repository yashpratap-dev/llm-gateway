package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link Router} implementation that selects the provider with the lowest rolling
 * average observed latency.
 *
 * <p>Latency estimates are seeded with conservative defaults at startup.
 * Each successful provider response should call {@link #updateLatency} to refine
 * the rolling average. In M6 the resilience layer will call this automatically
 * after every measured request.</p>
 */
@Component
public class LatencyRouter implements Router {

    private static final Map<ProviderName, Long> DEFAULT_LATENCY = Map.of(
            ProviderName.GROQ, 300L,
            ProviderName.OPENAI, 800L
    );

    private final ConcurrentHashMap<ProviderName, Long> avgLatencyMs =
            new ConcurrentHashMap<>(DEFAULT_LATENCY);

    /** {@inheritDoc} */
    @Override
    public RoutingStrategy strategy() {
        return RoutingStrategy.LATENCY;
    }

    /**
     * Selects the provider with the lowest rolling average latency from the supplied pool.
     *
     * @param request   the incoming chat request
     * @param providers healthy providers to choose from
     * @return the lowest-latency provider
     * @throws ProviderException if the provider list is empty
     */
    @Override
    public LLMProvider route(ChatRequest request, List<LLMProvider> providers) {
        return providers.stream()
                .min(Comparator.comparingLong(p -> avgLatencyMs.getOrDefault(p.name(), Long.MAX_VALUE)))
                .orElseThrow(() -> new ProviderException("No providers available for latency routing"));
    }

    /**
     * Updates the rolling average latency for a provider using a simple EWMA.
     *
     * <p>Called by the M6 resilience layer after each successful provider response.</p>
     *
     * @param provider  the provider whose latency was measured
     * @param latencyMs the observed end-to-end latency in milliseconds
     */
    public void updateLatency(ProviderName provider, long latencyMs) {
        avgLatencyMs.merge(provider, latencyMs, (oldVal, newVal) -> (oldVal + newVal) / 2);
    }
}
