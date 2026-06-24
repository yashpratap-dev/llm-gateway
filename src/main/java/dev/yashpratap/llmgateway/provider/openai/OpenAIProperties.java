package dev.yashpratap.llmgateway.provider.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties bound from the {@code providers.openai.*} namespace.
 *
 * <p>Registered as a Spring bean via
 * {@link dev.yashpratap.llmgateway.config.PropertiesConfig}.</p>
 *
 * @param apiKey         OpenAI API key sourced from the {@code OPENAI_API_KEY} env var
 * @param baseUrl        base URL of the OpenAI REST API
 * @param model          default model to use when the client does not specify one
 * @param timeoutSeconds maximum seconds to wait for a response before timing out
 */
@ConfigurationProperties(prefix = "providers.openai")
public record OpenAIProperties(
        String apiKey,
        String baseUrl,
        String model,
        int timeoutSeconds) {
}
