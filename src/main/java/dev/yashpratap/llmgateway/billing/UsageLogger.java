package dev.yashpratap.llmgateway.billing;

import dev.yashpratap.llmgateway.domain.UsageLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service that persists {@link UsageLog} records asynchronously after each request.
 *
 * <p>Logging is fire-and-forget so that it does not add latency to the response path.
 * The {@link Async} annotation routes calls to the {@code gateway-async-} thread pool
 * defined in {@link dev.yashpratap.llmgateway.config.AsyncConfig}.</p>
 */
@Service
public class UsageLogger {

    private final UsageLogRepository usageLogRepository;

    /**
     * Constructs the logger with its repository dependency.
     *
     * @param usageLogRepository JPA repository for persisting usage records
     */
    public UsageLogger(UsageLogRepository usageLogRepository) {
        this.usageLogRepository = usageLogRepository;
    }

    /**
     * Persists the given usage log record on a background thread.
     *
     * @param usageLog the fully populated usage log to persist
     */
    @Async
    public void log(UsageLog usageLog) {
        usageLogRepository.save(usageLog);
    }
}
