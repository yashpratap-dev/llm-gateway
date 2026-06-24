package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.UsageLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Persists {@link UsageLog} records asynchronously after each request.
 *
 * <p>Logging is fire-and-forget: it runs on the {@code gateway-async-} thread pool
 * defined in {@link dev.yashpratap.llmgateway.config.AsyncConfig} and never propagates
 * exceptions to the caller. Called for both {@code SUCCESS} and {@code FAILED} requests.</p>
 */
@Service
public class UsageLogger {

    private static final Logger log = LoggerFactory.getLogger(UsageLogger.class);

    private final UsageLogRepository usageLogRepository;

    /**
     * Constructs the usage logger with its repository dependency.
     *
     * @param usageLogRepository JPA repository for persisting usage records
     */
    public UsageLogger(UsageLogRepository usageLogRepository) {
        this.usageLogRepository = usageLogRepository;
    }

    /**
     * Persists a usage log entry asynchronously.
     *
     * <p>Any exception during persistence is caught and logged at error level so that a
     * logging failure never affects the API response path.</p>
     *
     * @param tenantId         the UUID of the tenant that issued the request
     * @param apiKeyId         the UUID of the API key used to authenticate the request
     * @param provider         the provider that served the request (e.g. {@code GROQ})
     * @param model            the model identifier used for the request
     * @param promptTokens     the number of input tokens billed
     * @param completionTokens the number of output tokens billed
     * @param costUsd          the calculated USD cost of the request
     * @param latencyMs        the end-to-end gateway latency in milliseconds
     * @param cacheHit         {@code true} when the response was served from Redis cache
     * @param status           the request outcome ({@code SUCCESS} or {@code FAILED})
     */
    @Async
    public void log(UUID tenantId, UUID apiKeyId, String provider, String model,
                    int promptTokens, int completionTokens, double costUsd,
                    long latencyMs, boolean cacheHit, String status) {
        try {
            UsageLog entry = new UsageLog();
            entry.setTenantId(tenantId);
            entry.setApiKeyId(apiKeyId);
            entry.setProvider(provider);
            entry.setModel(model);
            entry.setPromptTokens(promptTokens);
            entry.setCompletionTokens(completionTokens);
            entry.setCostUsd(BigDecimal.valueOf(costUsd));
            entry.setLatencyMs(latencyMs);
            entry.setCacheHit(cacheHit);
            entry.setStatus(status);
            usageLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist usage log for tenant {}: {}", tenantId, e.getMessage());
        }
    }
}
