package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class GatewayMetricsServiceTest {

    private SimpleMeterRegistry registry;
    private GatewayMetricsService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new GatewayMetricsService(registry);
    }

    @Test
    void recordRequest_success_incrementsRequestCounter() {
        service.recordRequest("GROQ", 100L, false, 0.000005);

        Counter counter = registry.find("llm.requests.total")
                .tags("provider", "GROQ", "status", "success")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordRequest_cacheHit_incrementsCacheCounter() {
        service.recordRequest("GROQ", 50L, true, 0.0);

        Counter counter = registry.find("llm.cache.hits.total").tags("provider", "GROQ").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordRequest_cacheHit_doesNotIncrementCacheCounter_whenFalse() {
        service.recordRequest("GROQ", 100L, false, 0.000005);

        Counter counter = registry.find("llm.cache.hits.total").tags("provider", "GROQ").counter();
        assertThat(counter == null || counter.count() == 0.0).isTrue();
    }

    @Test
    void recordRequest_recordsLatencyTimer() {
        service.recordRequest("GROQ", 500L, false, 0.001);

        Timer timer = registry.find("llm.request.latency").tags("provider", "GROQ").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void recordRequest_zeroCost_doesNotIncrementCostCounter() {
        service.recordRequest("GROQ", 100L, false, 0.0);

        Counter counter = registry.find("llm.cost.usd.total").tags("provider", "GROQ").counter();
        assertThat(counter == null || counter.count() == 0.0).isTrue();
    }

    @Test
    void recordRequest_nullProvider_doesNotThrow() {
        assertThatCode(() -> service.recordRequest(null, 100L, false, 0.001))
                .doesNotThrowAnyException();
    }

    @Test
    void recordRequest_emptyProvider_doesNotThrow() {
        assertThatCode(() -> service.recordRequest("", 100L, false, 0.001))
                .doesNotThrowAnyException();
    }

    @Test
    void recordError_incrementsErrorCounter() {
        service.recordError("OPENAI", "ProviderTimeoutException");

        Counter counter = registry.find("llm.errors.total")
                .tags("provider", "OPENAI", "error_type", "ProviderTimeoutException")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordFallback_incrementsFallbackCounter() {
        service.recordFallback("GROQ", "OPENAI");

        Counter counter = registry.find("llm.fallback.total")
                .tags("from_provider", "GROQ", "to_provider", "OPENAI")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordCircuitBreakerEvent_incrementsWithState() {
        service.recordCircuitBreakerEvent("GROQ", "OPEN");

        Counter counter = registry.find("llm.circuit.breaker.events.total")
                .tags("provider", "GROQ", "state", "OPEN")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementAndDecrement_gaugeReturnsCorrectValue() {
        service.incrementActiveRequests();
        service.incrementActiveRequests();
        service.incrementActiveRequests();
        service.decrementActiveRequests();
        service.decrementActiveRequests();

        double gaugeValue = registry.find("llm.active.requests").gauge().value();
        assertThat(gaugeValue).isEqualTo(1.0);
    }

    @Test
    void decrementBelowZero_gaugeNeverNegative() {
        service.decrementActiveRequests();

        double gaugeValue = registry.find("llm.active.requests").gauge().value();
        assertThat(gaugeValue).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void concurrentIncrementDecrement_gaugeNeverNegative() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                service.incrementActiveRequests();
                service.decrementActiveRequests();
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        double gaugeValue = registry.find("llm.active.requests").gauge().value();
        assertThat(gaugeValue).isGreaterThanOrEqualTo(0.0);
        assertThat(gaugeValue).isEqualTo(0.0);
    }
}
