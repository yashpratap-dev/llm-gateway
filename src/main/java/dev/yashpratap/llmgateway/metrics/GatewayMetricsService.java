package dev.yashpratap.llmgateway.metrics;

import dev.yashpratap.llmgateway.provider.ProviderName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GatewayMetricsService {

    private static final Logger log = LoggerFactory.getLogger(GatewayMetricsService.class);

    private final MeterRegistry registry;
    private final AtomicInteger activeRequestsCounter = new AtomicInteger(0);

    public GatewayMetricsService(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("llm.active.requests", activeRequestsCounter, AtomicInteger::get)
                .description("Number of LLM requests currently in flight")
                .register(registry);
        log.info("[metrics] GatewayMetricsService initialized");
    }

    public void incrementActiveRequests() {
        try {
            activeRequestsCounter.incrementAndGet();
        } catch (Exception e) {
            log.warn("[metrics] incrementActiveRequests failed: {}", e.getMessage());
        }
    }

    public void decrementActiveRequests() {
        try {
            activeRequestsCounter.updateAndGet(current -> Math.max(0, current - 1));
        } catch (Exception e) {
            log.warn("[metrics] decrementActiveRequests failed: {}", e.getMessage());
        }
    }

    public void recordRequest(String provider, long latencyMs, boolean cacheHit, double costUsd) {
        try {
            String p = safeProvider(provider);
            String mf = modelFamily(provider);

            registry.counter("llm.requests.total",
                    "provider", p, "model_family", mf,
                    "cache_hit", String.valueOf(cacheHit), "status", "success"
            ).increment();

            registry.timer("llm.request.latency",
                    "provider", p, "model_family", mf
            ).record(latencyMs, TimeUnit.MILLISECONDS);

            if (cacheHit) {
                registry.counter("llm.cache.hits.total", "provider", p).increment();
            }

            if (costUsd > 0.0) {
                registry.counter("llm.cost.usd.total", "provider", p).increment(costUsd);
            }
        } catch (Exception e) {
            log.warn("[metrics] recordRequest failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordError(String provider, String errorType) {
        try {
            String p = safeProvider(provider);
            registry.counter("llm.errors.total",
                    "provider", p, "error_type", safeTag(errorType, "Unknown")
            ).increment();
            registry.counter("llm.requests.total",
                    "provider", p, "model_family", modelFamily(provider),
                    "cache_hit", "false", "status", "error"
            ).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordError failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordStreamingRequest(String provider) {
        try {
            registry.counter("llm.streaming.requests.total",
                    "provider", safeProvider(provider)
            ).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordStreamingRequest failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordFallback(String fromProvider, String toProvider) {
        try {
            registry.counter("llm.fallback.total",
                    "from_provider", safeProvider(fromProvider),
                    "to_provider", safeProvider(toProvider)
            ).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordFallback failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordCircuitBreakerEvent(String provider, String state) {
        try {
            registry.counter("llm.circuit.breaker.events.total",
                    "provider", safeProvider(provider),
                    "state", safeTag(state, "UNKNOWN")
            ).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordCircuitBreakerEvent failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordProviderSelection(String provider, String strategy) {
        try {
            registry.counter("llm.provider.selections.total",
                    "provider", safeProvider(provider),
                    "strategy", safeTag(strategy, "UNKNOWN")
            ).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordProviderSelection failed (best-effort): {}", e.getMessage());
        }
    }

    public void recordSemanticCacheHit(String provider) {
        try {
            registry.counter("llm.semantic.cache.hits", "provider", safeProvider(provider)).increment();
        } catch (Exception e) {
            log.warn("[metrics] recordSemanticCacheHit failed: {}", e.getMessage());
        }
    }

    public void recordSemanticCacheMiss() {
        try {
            registry.counter("llm.semantic.cache.misses").increment();
        } catch (Exception e) {
            log.warn("[metrics] recordSemanticCacheMiss failed: {}", e.getMessage());
        }
    }

    public void recordSemanticSimilarityScore(double score) {
        try {
            registry.summary("llm.semantic.similarity.score").record(score);
        } catch (Exception e) {
            log.warn("[metrics] recordSemanticSimilarityScore failed: {}", e.getMessage());
        }
    }

    public void recordEmbeddingCacheHit() {
        try {
            registry.counter("llm.embedding.cache.hits").increment();
        } catch (Exception e) {
            log.warn("[metrics] recordEmbeddingCacheHit failed: {}", e.getMessage());
        }
    }

    public void recordEmbeddingCacheMiss() {
        try {
            registry.counter("llm.embedding.cache.misses").increment();
        } catch (Exception e) {
            log.warn("[metrics] recordEmbeddingCacheMiss failed: {}", e.getMessage());
        }
    }

    private String safeProvider(String provider) {
        return (provider != null && !provider.isBlank()) ? provider.toUpperCase() : "UNKNOWN";
    }

    private String modelFamily(String provider) {
        if (provider == null || provider.isBlank()) return "unknown";
        try {
            return switch (ProviderName.valueOf(provider.toUpperCase())) {
                case GROQ   -> "groq-llama";
                case OPENAI -> "openai-gpt";
                case CLAUDE -> "anthropic-claude";
            };
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
    }

    private String safeTag(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
