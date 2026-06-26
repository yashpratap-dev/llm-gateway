package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Choice;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.Usage;
import dev.yashpratap.llmgateway.provider.exception.ProviderTimeoutException;
import dev.yashpratap.llmgateway.provider.exception.ProviderUnavailableException;
import dev.yashpratap.llmgateway.resilience.ResilienceService;
import dev.yashpratap.llmgateway.routing.RoutingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResilienceService}.
 *
 * Uses real {@link CircuitBreakerRegistry} and {@link RetryRegistry} instances
 * for accurate state transition testing.
 */
@ExtendWith(MockitoExtension.class)
class ResilienceServiceTest {

    @Mock
    RoutingService routingService;

    @Mock
    LLMProvider primaryProvider;

    @Mock
    LLMProvider fallbackProvider;

    private static final ChatRequest REQUEST =
            new ChatRequest("auto", List.of(new Message("user", "hi")), false, Map.of());

    private static final ChatResponse EXPECTED_RESPONSE = new ChatResponse(
            "resp-1", "llama-3", "GROQ",
            List.of(new Choice(0, new Message("assistant", "hello"), "stop")),
            new Usage(10, 20, 0.0),
            new GatewayMeta("GROQ", false, 100L));

    @BeforeEach
    void setUpProviderNames() {
        when(primaryProvider.name()).thenReturn(ProviderName.GROQ);
    }

    private ResilienceService buildService(CircuitBreakerRegistry cbRegistry, RetryRegistry retryRegistry) {
        return new ResilienceService(cbRegistry, retryRegistry, routingService,
                new GatewayMetricsService(new SimpleMeterRegistry()));
    }

    private CircuitBreakerRegistry defaultCbRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    private RetryRegistry defaultRetryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(ProviderTimeoutException.class, ProviderUnavailableException.class)
                .build();
        return RetryRegistry.of(config);
    }

    @Test
    void testExecuteWithResilience_success_returnsResponse() {
        when(primaryProvider.generate(any())).thenReturn(EXPECTED_RESPONSE);

        ResilienceService service = buildService(defaultCbRegistry(), defaultRetryRegistry());
        ChatResponse result = service.executeWithResilience(primaryProvider, REQUEST);

        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        verify(routingService, times(0)).getNextProvider(any(), any());
    }

    @Test
    void testExecuteWithResilience_circuitOpen_attemptsFallback() {
        when(fallbackProvider.name()).thenReturn(ProviderName.OPENAI);
        when(fallbackProvider.generate(any())).thenReturn(EXPECTED_RESPONSE);
        when(routingService.getNextProvider(ProviderName.GROQ, REQUEST))
                .thenReturn(Optional.of(fallbackProvider));

        CircuitBreakerRegistry cbRegistry = defaultCbRegistry();
        // Force primary circuit breaker to OPEN state
        CircuitBreaker cb = cbRegistry.circuitBreaker("groq-provider");
        cb.transitionToOpenState();

        ResilienceService service = buildService(cbRegistry, defaultRetryRegistry());
        ChatResponse result = service.executeWithResilience(primaryProvider, REQUEST);

        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        verify(routingService, times(1)).getNextProvider(ProviderName.GROQ, REQUEST);
        verify(fallbackProvider, times(1)).generate(REQUEST);
    }

    @Test
    void testExecuteWithResilience_allProvidersFail_throwsProviderUnavailable() {
        when(fallbackProvider.name()).thenReturn(ProviderName.OPENAI);
        when(primaryProvider.generate(any()))
                .thenThrow(new ProviderTimeoutException("GROQ"));
        when(fallbackProvider.generate(any()))
                .thenThrow(new ProviderUnavailableException("OPENAI"));
        when(routingService.getNextProvider(ProviderName.GROQ, REQUEST))
                .thenReturn(Optional.of(fallbackProvider));

        // Use maxAttempts=1 so the primary fails once, hits fallback, fallback fails → exception
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(ProviderTimeoutException.class, ProviderUnavailableException.class)
                .build();

        ResilienceService service = buildService(defaultCbRegistry(), RetryRegistry.of(retryConfig));

        assertThatThrownBy(() -> service.executeWithResilience(primaryProvider, REQUEST))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void testExecuteWithResilience_retrySucceedsOnSecondAttempt() {
        when(primaryProvider.generate(any()))
                .thenThrow(new ProviderTimeoutException("GROQ"))
                .thenReturn(EXPECTED_RESPONSE);

        ResilienceService service = buildService(defaultCbRegistry(), defaultRetryRegistry());
        ChatResponse result = service.executeWithResilience(primaryProvider, REQUEST);

        assertThat(result).isEqualTo(EXPECTED_RESPONSE);
        verify(primaryProvider, times(2)).generate(REQUEST);
        verify(routingService, times(0)).getNextProvider(any(), any());
    }

    @Test
    void testExecuteWithResilience_circuitTransitionsToOpen() {
        when(primaryProvider.generate(any()))
                .thenThrow(new ProviderTimeoutException("GROQ"));
        when(routingService.getNextProvider(ProviderName.GROQ, REQUEST))
                .thenReturn(Optional.empty());

        // CB: slidingWindowSize=4, minimumNumberOfCalls=4 — trips after 4 failures
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);

        // maxAttempts=1 so each executeWithResilience call = exactly 1 CB failure
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(ProviderTimeoutException.class)
                .build();

        ResilienceService service = buildService(cbRegistry, RetryRegistry.of(retryConfig));

        // 4 calls — each records 1 CB failure; after 4th, failure rate = 100% > 50% → OPEN
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.executeWithResilience(primaryProvider, REQUEST))
                    .isInstanceOf(ProviderUnavailableException.class);
        }

        CircuitBreaker cb = cbRegistry.circuitBreaker("groq-provider");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
