package dev.yashpratap.llmgateway.provider.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties bound from the {@code providers.gemini.*} namespace.
 *
 * <p>Registered as a Spring bean via
 * {@link dev.yashpratap.llmgateway.config.PropertiesConfig}.</p>
 *
 * @param apiKey          Google Gemini API key sourced from the {@code GEMINI_API_KEY} env var
 * @param baseUrl         base URL of the Gemini Generative Language API
 * @param apiVersion      API version string (e.g. "v1beta") — configurable for future upgrades
 * @param defaultModel    default model to use when the client does not specify one
 * @param timeoutSeconds  maximum seconds to wait for a response before timing out
 * @param maxOutputTokens maximum number of tokens to generate per request
 */
@ConfigurationProperties(prefix = "providers.gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String apiVersion,
        String defaultModel,
        int timeoutSeconds,
        int maxOutputTokens
) {}
