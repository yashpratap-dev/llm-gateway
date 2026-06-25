package dev.yashpratap.llmgateway.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.billing.UsageLogger;
import dev.yashpratap.llmgateway.cache.RedisCacheService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Choice;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.Usage;
import dev.yashpratap.llmgateway.routing.LatencyRouter;
import dev.yashpratap.llmgateway.routing.RoutingPolicyService;
import dev.yashpratap.llmgateway.routing.RoutingService;
import dev.yashpratap.llmgateway.routing.RoutingStrategy;
import dev.yashpratap.llmgateway.streaming.dto.StreamDoneEvent;
import dev.yashpratap.llmgateway.streaming.dto.StreamErrorEvent;
import dev.yashpratap.llmgateway.streaming.dto.StreamTokenEvent;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles SSE streaming chat completions through the gateway pipeline.
 *
 * <p>tenantId, apiKeyId, and plan are passed as method parameters — @RequestScope
 * TenantContext is unavailable on the reactive threads where this Flux executes.</p>
 *
 * <p>Known limitation: token usage and cost are not tracked for streamed requests.
 * Usage is logged with zero tokens/cost. Planned for a later module.</p>
 */
@Service
public class StreamingChatService {

    private final RoutingService routingService;
    private final RoutingPolicyService routingPolicyService;
    private final LatencyRouter latencyRouter;
    private final RedisCacheService redisCacheService;
    private final UsageLogger usageLogger;
    private final ObjectMapper objectMapper;

    public StreamingChatService(RoutingService routingService,
                                RoutingPolicyService routingPolicyService,
                                LatencyRouter latencyRouter,
                                RedisCacheService redisCacheService,
                                UsageLogger usageLogger,
                                ObjectMapper objectMapper) {
        this.routingService = routingService;
        this.routingPolicyService = routingPolicyService;
        this.latencyRouter = latencyRouter;
        this.redisCacheService = redisCacheService;
        this.usageLogger = usageLogger;
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> stream(UUID tenantId, UUID apiKeyId,
                                                String plan, ChatRequest request) {
        Optional<ChatResponse> cached = redisCacheService.get(tenantId, request);
        if (cached.isPresent()) {
            ChatResponse c = cached.get();
            String content = c.choices().get(0).message().content();
            String cachedProvider = c.provider();
            usageLogger.log(tenantId, apiKeyId, cachedProvider, c.model(),
                    0, 0, 0.0, 0L, true, "SUCCESS");
            return Flux.concat(
                    Flux.just(sse("token", new StreamTokenEvent(content))),
                    Flux.just(sse("done", new StreamDoneEvent(cachedProvider, 0L, true)))
            );
        }

        RoutingStrategy strategy = routingPolicyService.getStrategyForTenant(tenantId);
        LLMProvider provider = routingService.route(request, strategy);
        String providerName = provider.name().name();
        long startMs = System.currentTimeMillis();
        StringBuilder accumulated = new StringBuilder();

        Flux<ServerSentEvent<String>> tokens = provider.stream(request)
                .doOnNext(chunk -> {
                    if (chunk.delta() != null) accumulated.append(chunk.delta());
                })
                .map(chunk -> {
                    String delta = chunk.delta();
                    return sse("token", new StreamTokenEvent(delta != null ? delta : ""));
                });

        Mono<ServerSentEvent<String>> done = Mono.fromCallable(() -> {
            long latencyMs = System.currentTimeMillis() - startMs;
            latencyRouter.updateLatency(provider.name(), latencyMs);
            ChatResponse full = new ChatResponse(
                    "stream-" + UUID.randomUUID(),
                    request.model(),
                    providerName,
                    List.of(new Choice(0, new Message("assistant", accumulated.toString()), "stop")),
                    new Usage(0, 0, 0.0),
                    new GatewayMeta(providerName, false, latencyMs));
            redisCacheService.put(tenantId, request, full);
            usageLogger.log(tenantId, apiKeyId, providerName, request.model(),
                    0, 0, 0.0, latencyMs, false, "SUCCESS");
            return sse("done", new StreamDoneEvent(providerName, latencyMs, false));
        });

        return tokens.concatWith(done)
                .onErrorResume(e -> {
                    long latencyMs = System.currentTimeMillis() - startMs;
                    usageLogger.log(tenantId, apiKeyId, providerName, request.model(),
                            0, 0, 0.0, latencyMs, false, "FAILED");
                    return Flux.just(sse("error",
                            new StreamErrorEvent(e.getMessage(), "PROVIDER_ERROR")));
                });
    }

    private ServerSentEvent<String> sse(String event, Object payload) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(event)
                    .data(objectMapper.writeValueAsString(payload))
                    .build();
        } catch (Exception e) {
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"message\":\"serialization failed\",\"errorCode\":\"INTERNAL\"}")
                    .build();
        }
    }
}
