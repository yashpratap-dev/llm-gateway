package dev.yashpratap.llmgateway.provider.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties bound from the {@code providers.anthropic.*} namespace.
 *
 * <p>Registered as a Spring bean via
 * {@link dev.yashpratap.llmgateway.config.PropertiesConfig}.</p>
 *
 * @param apiKey          Anthropic API key sourced from the {@code ANTHROPIC_API_KEY} env var
 * @param baseUrl         base URL of the Anthropic REST API
 * @param defaultModel    default model to use when the client does not specify one
 * @param timeoutSeconds  maximum seconds to wait for a response before timing out
 * @param maxOutputTokens maximum number of tokens to generate (required by Anthropic API)
 */
@ConfigurationProperties(prefix = "providers.anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String defaultModel,
        int timeoutSeconds,
        int maxOutputTokens
) {}
