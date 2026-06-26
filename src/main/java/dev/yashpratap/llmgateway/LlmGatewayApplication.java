package dev.yashpratap.llmgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the LLM Gateway application.
 *
 * <p>The gateway provides a unified API for multiple LLM providers (Groq, OpenAI) with
 * API-key authentication, per-tenant budget enforcement, semantic caching, and
 * circuit-breaker resilience.</p>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class
LlmGatewayApplication {

    /**
     * Bootstraps the Spring application context.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(LlmGatewayApplication.class, args);
    }
}
