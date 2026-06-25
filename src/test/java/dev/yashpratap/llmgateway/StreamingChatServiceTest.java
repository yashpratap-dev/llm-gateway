package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.billing.UsageLogger;
import dev.yashpratap.llmgateway.cache.RedisCacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import dev.yashpratap.llmgateway.provider.ChatChunk;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Choice;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.Usage;
import dev.yashpratap.llmgateway.routing.LatencyRouter;
import dev.yashpratap.llmgateway.routing.RoutingPolicyService;
import dev.yashpratap.llmgateway.routing.RoutingService;
import dev.yashpratap.llmgateway.routing.RoutingStrategy;
import dev.yashpratap.llmgateway.streaming.StreamingChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingChatServiceTest {

    @Mock RoutingService routingService;
    @Mock RoutingPolicyService routingPolicyService;
    @Mock LatencyRouter latencyRouter;
    @Mock RedisCacheService redisCacheService;
    @Mock UsageLogger usageLogger;
    @Mock LLMProvider provider;

    private StreamingChatService service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

    private final UUID tenantId = UUID.randomUUID();
    private final UUID apiKeyId = UUID.randomUUID();
    private final String plan = "FREE";

    @BeforeEach
    void setUp() {
        service = new StreamingChatService(routingService, routingPolicyService, latencyRouter,
                redisCacheService, usageLogger, objectMapper, circuitBreakerRegistry);
    }

    @Test
    void testStream_cacheHit_emitsContentThenDone() {
        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hello")), true, null);
        ChatResponse cached = new ChatResponse(
                "id-1", "llama", "GROQ",
                List.of(new Choice(0, new Message("assistant", "Cached content"), "stop")),
                new Usage(5, 10, 0.0),
                new GatewayMeta("GROQ", true, 0L));

        when(redisCacheService.get(tenantId, request)).thenReturn(Optional.of(cached));

        Flux<ServerSentEvent<String>> flux = service.stream(tenantId, apiKeyId, plan, request);

        StepVerifier.create(flux)
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("token");
                    assertThat(sse.data()).contains("Cached content");
                })
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("done");
                    assertThat(sse.data()).contains("\"cacheHit\":true");
                })
                .verifyComplete();

        verify(provider, never()).stream(any());
        verify(usageLogger).log(eq(tenantId), eq(apiKeyId), eq("GROQ"), eq("llama"),
                eq(0), eq(0), eq(0.0), eq(0L), eq(true), eq("SUCCESS"));
    }

    @Test
    void testStream_cacheMiss_streamsProviderTokensThenDone() {
        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hello")), true, null);

        when(redisCacheService.get(tenantId, request)).thenReturn(Optional.empty());
        when(routingPolicyService.getStrategyForTenant(tenantId)).thenReturn(RoutingStrategy.PRIORITY);
        when(routingService.route(request, RoutingStrategy.PRIORITY)).thenReturn(provider);
        when(provider.name()).thenReturn(ProviderName.GROQ);
        when(provider.stream(request)).thenReturn(Flux.just(
                new ChatChunk("id-1", "GROQ", "Hello", false),
                new ChatChunk("id-1", "GROQ", " World", false),
                new ChatChunk("id-1", "GROQ", "!", true)
        ));

        Flux<ServerSentEvent<String>> flux = service.stream(tenantId, apiKeyId, plan, request);

        StepVerifier.create(flux)
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("token");
                    assertThat(sse.data()).contains("Hello");
                })
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("token");
                    assertThat(sse.data()).contains("World");
                })
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("token");
                    assertThat(sse.data()).contains("!");
                })
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("done");
                    assertThat(sse.data()).contains("\"cacheHit\":false");
                })
                .verifyComplete();

        verify(redisCacheService).put(eq(tenantId), eq(request), any(ChatResponse.class));
        verify(usageLogger).log(eq(tenantId), eq(apiKeyId), eq("GROQ"), eq("auto"),
                eq(0), eq(0), eq(0.0), anyLong(), eq(false), eq("SUCCESS"));
    }

    @Test
    void testStream_providerError_emitsErrorEvent() {
        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hello")), true, null);

        when(redisCacheService.get(tenantId, request)).thenReturn(Optional.empty());
        when(routingPolicyService.getStrategyForTenant(tenantId)).thenReturn(RoutingStrategy.PRIORITY);
        when(routingService.route(request, RoutingStrategy.PRIORITY)).thenReturn(provider);
        when(provider.name()).thenReturn(ProviderName.GROQ);
        when(provider.stream(request)).thenReturn(Flux.error(new RuntimeException("Provider failed")));

        Flux<ServerSentEvent<String>> flux = service.stream(tenantId, apiKeyId, plan, request);

        StepVerifier.create(flux)
                .assertNext(sse -> {
                    assertThat(sse.event()).isEqualTo("error");
                    assertThat(sse.data()).contains("PROVIDER_ERROR");
                })
                .verifyComplete();

        verify(usageLogger).log(eq(tenantId), eq(apiKeyId), eq("GROQ"), eq("auto"),
                eq(0), eq(0), eq(0.0), anyLong(), eq(false), eq("FAILED"));
    }
}
