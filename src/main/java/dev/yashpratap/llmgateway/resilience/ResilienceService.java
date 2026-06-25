package dev.yashpratap.llmgateway.resilience;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.exception.ProviderUnavailableException;
import dev.yashpratap.llmgateway.routing.RoutingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Wraps LLM provider generate() calls with Resilience4j Circuit Breaker and Retry.
 *
 * <p>Design: ResilienceService wraps calls to an ALREADY-SELECTED provider.
 * Provider selection is the responsibility of RoutingService, not this class.
 * Fallback provider is also obtained via RoutingService to respect routing policy.</p>
 *
 * <p>Retry + CircuitBreaker order: Retry(CircuitBreaker(call)) — each retry
 * attempt is recorded by the circuit breaker, contributing to failure rate.</p>
 *
 * <p>Known limitation: TimeLimiter is not applied because provider.generate()
 * is synchronous. Async timeout support is deferred to a future module.</p>
 *
 * <p>Fallback: if primary provider fails after retries or circuit is OPEN,
 * RoutingService.getNextProvider() is called to respect routing policy.
 * Each fallback is attempted at most once — no infinite loop.</p>
 */
@Service
public class ResilienceService {

    private static final Logger log = LoggerFactory.getLogger(ResilienceService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RoutingService routingService;

    public ResilienceService(CircuitBreakerRegistry circuitBreakerRegistry,
                             RetryRegistry retryRegistry,
                             RoutingService routingService) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.routingService = routingService;
        registerCircuitBreakerEventListeners();
    }

    /**
     * Registers state-transition event listeners for all configured circuit breakers.
     * Logs CLOSED → OPEN → HALF_OPEN transitions with structured fields.
     */
    private void registerCircuitBreakerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::attachListeners);
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> attachListeners(event.getAddedEntry()));
    }

    private void attachListeners(CircuitBreaker cb) {
        cb.getEventPublisher()
                .onStateTransition(e -> log.info(
                        "[circuit-breaker] name={} transition={}→{}",
                        cb.getName(),
                        e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()))
                .onFailureRateExceeded(e -> log.warn(
                        "[circuit-breaker] name={} failureRate={}",
                        cb.getName(), e.getFailureRate()))
                .onSuccess(e -> log.debug(
                        "[circuit-breaker] name={} success latency={}ms",
                        cb.getName(), e.getElapsedDuration().toMillis()));
    }

    /**
     * Executes provider.generate() with Circuit Breaker + Retry protection.
     *
     * @param provider the already-selected primary provider
     * @param request  the chat request
     * @return ChatResponse from primary or fallback provider
     * @throws ProviderUnavailableException if all providers fail
     */
    public ChatResponse executeWithResilience(LLMProvider provider, ChatRequest request) {
        String instanceName = instanceName(provider);
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(instanceName);
        Retry retry = retryRegistry.retry(instanceName);

        if (cb.getState() == CircuitBreaker.State.OPEN) {
            log.warn("[resilience] provider={} circuit=OPEN — skipping to fallback",
                    provider.name());
            return executeFallback(provider, request);
        }

        try {
            ChatResponse response = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(cb,
                            () -> provider.generate(request))).get();
            log.debug("[resilience] provider={} generate success", provider.name());
            return response;
        } catch (Exception e) {
            log.warn("[resilience] provider={} failed after retries: {} — attempting fallback",
                    provider.name(), e.getMessage(), e);
            return executeFallback(provider, request);
        }
    }

    /**
     * Attempts the request on the next provider via RoutingService.
     * Routing policy is respected — fallback comes from RoutingService, not raw registry.
     */
    private ChatResponse executeFallback(LLMProvider failedProvider, ChatRequest request) {
        Optional<LLMProvider> fallbackOpt = routingService.getNextProvider(
                failedProvider.name(), request);

        if (fallbackOpt.isEmpty()) {
            log.warn("[resilience] provider={} no fallback available", failedProvider.name());
            throw new ProviderUnavailableException(failedProvider.name().name());
        }

        LLMProvider fallback = fallbackOpt.get();
        String fbInstance = instanceName(fallback);
        CircuitBreaker fbCb = circuitBreakerRegistry.circuitBreaker(fbInstance);

        if (fbCb.getState() == CircuitBreaker.State.OPEN) {
            log.warn("[resilience] fallback provider={} circuit=OPEN", fallback.name());
            throw new ProviderUnavailableException(fallback.name().name());
        }

        try {
            log.info("[resilience] fallback provider={} attempting", fallback.name());
            ChatResponse response = CircuitBreaker.decorateSupplier(fbCb,
                    () -> fallback.generate(request)).get();
            log.info("[resilience] fallback provider={} success", fallback.name());
            return response;
        } catch (Exception e) {
            log.warn("[resilience] fallback provider={} also failed: {}",
                    fallback.name(), e.getMessage(), e);
            throw new ProviderUnavailableException(fallback.name().name());
        }
    }

    private String instanceName(LLMProvider provider) {
        return provider.name().name().toLowerCase() + "-provider";
    }
}
