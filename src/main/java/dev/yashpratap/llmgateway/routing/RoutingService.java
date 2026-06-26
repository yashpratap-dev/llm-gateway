package dev.yashpratap.llmgateway.routing;

import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.ProviderRegistry;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Entry point for all provider-selection decisions.
 *
 * <p>At construction time the service builds a {@link RoutingStrategy} → {@link Router} lookup
 * map from every {@link Router} bean in the context. At request time it retrieves healthy
 * providers from the {@link ProviderRegistry} and delegates selection to the appropriate router.</p>
 */
@Service
public class RoutingService {

    private final ProviderRegistry providerRegistry;
    private final Map<RoutingStrategy, Router> routerMap;
    private final GatewayMetricsService metricsService;

    public RoutingService(ProviderRegistry providerRegistry, List<Router> routers,
                          GatewayMetricsService metricsService) {
        this.providerRegistry = providerRegistry;
        this.routerMap = routers.stream()
                .collect(Collectors.toMap(Router::strategy, r -> r));
        this.metricsService = metricsService;
    }

    /**
     * Selects a provider for the given request using the specified routing strategy.
     *
     * <p>Falls back to {@link RoutingStrategy#PRIORITY} if the requested strategy has no
     * registered router bean.</p>
     *
     * @param request  the incoming chat request
     * @param strategy the desired routing strategy
     * @return the selected {@link LLMProvider}
     * @throws ProviderException if no healthy providers are available
     */
    public LLMProvider route(ChatRequest request, RoutingStrategy strategy) {
        List<LLMProvider> healthyProviders = providerRegistry.getHealthyProviders();
        if (healthyProviders.isEmpty()) {
            throw new ProviderException("No healthy providers available");
        }
        Router router = routerMap.getOrDefault(strategy, routerMap.get(RoutingStrategy.PRIORITY));
        LLMProvider selected = router.route(request, healthyProviders);
        metricsService.recordProviderSelection(selected.name().name(), strategy.name());
        return selected;
    }

    /**
     * Returns the next available healthy provider excluding the specified one.
     * Used by ResilienceService for fallback — routing policy is still respected
     * because this method uses the same provider list as route().
     *
     * @param excluding the provider name to exclude (the failed primary)
     * @param request   the original chat request (used for routing decisions)
     * @return the next healthy provider, or empty if none available
     */
    public Optional<LLMProvider> getNextProvider(ProviderName excluding, ChatRequest request) {
        return providerRegistry.getHealthyProviders().stream()
                .filter(p -> p.name() != excluding)
                .findFirst();
    }
}
