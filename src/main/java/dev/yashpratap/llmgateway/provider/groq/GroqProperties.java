package dev.yashpratap.llmgateway.provider.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties bound from the {@code providers.groq.*} namespace.
 *
 * <p>Registered as a Spring bean via
 * {@link dev.yashpratap.llmgateway.config.PropertiesConfig}.</p>
 *
 * @param apiKey         Groq API key sourced from the {@code GROQ_API_KEY} env var
 * @param baseUrl        base URL of the Groq REST API
 * @param model          default model to use when the client does not specify one
 * @param timeoutSeconds maximum seconds to wait for a response before timing out
 */
@ConfigurationProperties(prefix = "providers.groq")
public record GroqProperties(
        String apiKey,
        String baseUrl,
        String model,
        int timeoutSeconds) {
}
