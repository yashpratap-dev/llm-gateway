package dev.yashpratap.llmgateway.controller;

import dev.yashpratap.llmgateway.billing.BudgetService;
import dev.yashpratap.llmgateway.billing.CostCalculator;
import dev.yashpratap.llmgateway.billing.UsageLogger;
import dev.yashpratap.llmgateway.cache.RateLimiterService;
import dev.yashpratap.llmgateway.cache.RedisCacheService;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheService;
import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.common.RateLimitException;
import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Usage;
import dev.yashpratap.llmgateway.resilience.ResilienceService;
import dev.yashpratap.llmgateway.routing.LatencyRouter;
import dev.yashpratap.llmgateway.routing.RoutingPolicyService;
import dev.yashpratap.llmgateway.routing.RoutingService;
import dev.yashpratap.llmgateway.routing.RoutingStrategy;
import dev.yashpratap.llmgateway.streaming.StreamingChatService;
import dev.yashpratap.llmgateway.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller that exposes the primary chat completions endpoint.
 *
 * <p>Accepts an OpenAI-compatible {@code POST /api/v1/chat/completions} request and
 * executes the full gateway pipeline:</p>
 * <ol>
 *   <li>Rate limit enforcement (fixed-window counter via Redis)</li>
 *   <li>Budget check (synchronous, throws {@code 402} if exhausted)</li>
 *   <li>Cache lookup (tenant-isolated, returns early on hit)</li>
 *   <li>Routing strategy resolution from DB</li>
 *   <li>Provider selection and request forwarding</li>
 *   <li>Cost calculation and usage logging (both async)</li>
 *   <li>Response caching and budget deduction (both async)</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final RoutingService routingService;
    private final RoutingPolicyService routingPolicyService;
    private final RateLimiterService rateLimiterService;
    private final BudgetService budgetService;
    private final RedisCacheService redisCacheService;
    private final CostCalculator costCalculator;
    private final UsageLogger usageLogger;
    private final TenantContext tenantContext;
    private final LatencyRouter latencyRouter;
    private final StreamingChatService streamingChatService;
    private final ResilienceService resilienceService;
    private final GatewayMetricsService metricsService;
    private final SemanticCacheService semanticCacheService;

    /**
     * Constructs the controller with all required pipeline dependencies.
     *
     * @param routingService       selects the downstream LLM provider
     * @param routingPolicyService resolves the per-tenant routing strategy from the DB
     * @param rateLimiterService   enforces per-plan request rate limits via Redis
     * @param budgetService        enforces per-tenant USD spending limits
     * @param redisCacheService    tenant-isolated exact-match response cache
     * @param costCalculator       calculates USD cost from token counts and model pricing
     * @param usageLogger          persists usage log entries asynchronously
     * @param tenantContext        request-scoped holder for the authenticated tenant
     * @param latencyRouter        updates rolling average latency after each successful provider call
     * @param streamingChatService handles SSE streaming completions
     * @param resilienceService    wraps provider calls with Circuit Breaker and Retry
     */
    public ChatController(RoutingService routingService,
                          RoutingPolicyService routingPolicyService,
                          RateLimiterService rateLimiterService,
                          BudgetService budgetService,
                          RedisCacheService redisCacheService,
                          CostCalculator costCalculator,
                          UsageLogger usageLogger,
                          TenantContext tenantContext,
                          LatencyRouter latencyRouter,
                          StreamingChatService streamingChatService,
                          ResilienceService resilienceService,
                          GatewayMetricsService metricsService,
                          @org.springframework.lang.Nullable SemanticCacheService semanticCacheService) {
        this.routingService = routingService;
        this.routingPolicyService = routingPolicyService;
        this.rateLimiterService = rateLimiterService;
        this.budgetService = budgetService;
        this.redisCacheService = redisCacheService;
        this.costCalculator = costCalculator;
        this.usageLogger = usageLogger;
        this.tenantContext = tenantContext;
        this.latencyRouter = latencyRouter;
        this.streamingChatService = streamingChatService;
        this.resilienceService = resilienceService;
        this.metricsService = metricsService;
        this.semanticCacheService = semanticCacheService;
    }

    /**
     * Handles synchronous chat completion requests through the full gateway pipeline.
     *
     * @param request the provider-agnostic chat completion request body
     * @return {@code 200 OK} with the completion response; includes {@code X-RateLimit-Remaining} header
     * @throws RateLimitException       if the tenant has exceeded their plan's rate limit
     * @throws dev.yashpratap.llmgateway.common.BudgetExceededException if the tenant's budget is exhausted
     * @throws dev.yashpratap.llmgateway.provider.exception.ProviderException if all providers fail
     */
    @PostMapping("/completions")
    public ResponseEntity<ApiResponse<ChatResponse>> complete(
            @Valid @RequestBody ChatRequest request) {

        UUID tenantId = tenantContext.getTenantId();
        UUID apiKeyId = tenantContext.getApiKeyId();
        String plan = tenantContext.getTenant().getPlan();

        LLMProvider provider = null;
        metricsService.incrementActiveRequests();
        try {
            // 1. Rate limit
            if (!rateLimiterService.isAllowed(tenantId, plan)) {
                throw new RateLimitException("Rate limit exceeded for plan " + plan);
            }

            // 2. Budget
            budgetService.checkBudget(tenantId);

            // 3. Cache (tenant-isolated)
            Optional<ChatResponse> cached = redisCacheService.get(tenantId, request);
            if (cached.isPresent()) {
                ChatResponse c = cached.get();
                usageLogger.log(tenantId, apiKeyId, c.provider(), c.model(),
                        0, 0, 0.0, 0L, true, "SUCCESS");
                ChatResponse hit = new ChatResponse(c.id(), c.model(), c.provider(),
                        c.choices(), c.usage(), new GatewayMeta(c.provider(), true, 0L));
                int rem = rateLimiterService.getRemainingRequests(tenantId, plan);
                return ResponseEntity.ok()
                        .header("X-RateLimit-Remaining", String.valueOf(rem))
                        .body(ApiResponse.success(hit));
            }

            // 3b. Semantic cache (only when exact cache missed)
            if (semanticCacheService != null) {
                Optional<ChatResponse> semanticHit = semanticCacheService.findSemanticMatch(tenantId, request);
                if (semanticHit.isPresent()) {
                    ChatResponse c = semanticHit.get();
                    usageLogger.log(tenantId, apiKeyId, c.provider(), c.model(),
                            0, 0, 0.0, 0L, true, "SUCCESS");
                    ChatResponse hit = new ChatResponse(c.id(), c.model(), c.provider(),
                            c.choices(), c.usage(), new GatewayMeta(c.provider(), true, 0L));
                    int rem = rateLimiterService.getRemainingRequests(tenantId, plan);
                    return ResponseEntity.ok()
                            .header("X-RateLimit-Remaining", String.valueOf(rem))
                            .body(ApiResponse.success(hit));
                }
            }

            // 4. Routing strategy from DB
            RoutingStrategy strategy = routingPolicyService.getStrategyForTenant(tenantId);

            // 5. Route
            provider = routingService.route(request, strategy);

            // 6. Call provider — log FAILED and rethrow on exception
            long startMs = System.currentTimeMillis();
            ChatResponse response;
            try {
                response = resilienceService.executeWithResilience(provider, request);
            } catch (Exception e) {
                long latencyMs = System.currentTimeMillis() - startMs;
                usageLogger.log(tenantId, apiKeyId, provider.name().name(), request.model(),
                        0, 0, 0.0, latencyMs, false, "FAILED");
                throw e;
            }
            long latencyMs = System.currentTimeMillis() - startMs;
            latencyRouter.updateLatency(provider.name(), latencyMs);

            // 7. Cost
            double costUsd = costCalculator.calculate(provider.name().name(), response.model(),
                    response.usage().promptTokens(), response.usage().completionTokens());

            // 8. Final response
            ChatResponse finalResponse = new ChatResponse(
                    response.id(), response.model(), provider.name().name(),
                    response.choices(),
                    new Usage(response.usage().promptTokens(), response.usage().completionTokens(), costUsd),
                    new GatewayMeta(provider.name().name(), false, latencyMs));

            // 9. Cache
            redisCacheService.put(tenantId, request, finalResponse);

            // 9b. Semantic cache store (async, fire-and-forget)
            if (semanticCacheService != null) {
                semanticCacheService.storeSemanticEntry(tenantId, request, finalResponse);
            }

            // 10. Log async
            usageLogger.log(tenantId, apiKeyId, provider.name().name(), response.model(),
                    response.usage().promptTokens(), response.usage().completionTokens(),
                    costUsd, latencyMs, false, "SUCCESS");

            // 11. Budget deduct async
            budgetService.deductAsync(tenantId, costUsd);

            // 12. Metrics
            metricsService.recordRequest(
                    finalResponse.provider(),
                    latencyMs,
                    finalResponse.gatewayMeta().cacheHit(),
                    finalResponse.usage().costUsd());

            // 13. Return
            int remaining = rateLimiterService.getRemainingRequests(tenantId, plan);
            return ResponseEntity.ok()
                    .header("X-RateLimit-Remaining", String.valueOf(remaining))
                    .body(ApiResponse.success(finalResponse));
        } catch (Exception e) {
            metricsService.recordError(
                    provider != null ? provider.name().name() : "UNKNOWN",
                    e.getClass().getSimpleName());
            throw e;
        } finally {
            metricsService.decrementActiveRequests();
        }
    }

    /**
     * SSE streaming chat completions endpoint (text-only).
     *
     * Known limitation: token usage and cost are not tracked for streamed
     * requests in this version. Deferred to a later module.
     *
     * Context (tenantId, apiKeyId, plan) is captured on the request thread
     * before the Flux is returned — @RequestScope TenantContext and
     * SecurityContext are unavailable on reactive threads.
     */
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamCompletions(
            @Valid @RequestBody ChatRequest request) {

        UUID tenantId = tenantContext.getTenantId();
        UUID apiKeyId = tenantContext.getApiKeyId();
        String plan = tenantContext.getTenant().getPlan();

        if (!rateLimiterService.isAllowed(tenantId, plan)) {
            throw new RateLimitException("Rate limit exceeded for plan " + plan);
        }
        budgetService.checkBudget(tenantId);

        return streamingChatService.stream(tenantId, apiKeyId, plan, request);
    }
}
