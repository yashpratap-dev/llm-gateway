package dev.yashpratap.llmgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration for gateway background tasks.
 *
 * <p>Provides a dedicated thread pool for {@link org.springframework.scheduling.annotation.Async}
 * methods such as {@link dev.yashpratap.llmgateway.billing.UsageLogger#log}. Using a named
 * executor prevents async tasks from consuming threads from the HTTP worker pool.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates the thread pool executor used for all {@code @Async} gateway tasks.
     *
     * <p>Configuration: 4 core threads, 8 max threads, queue capacity of 100.
     * Threads are named {@code gateway-async-N} for easy identification in thread dumps.</p>
     *
     * @return the configured {@link Executor}
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("gateway-async-");
        executor.initialize();
        return executor;
    }
}
