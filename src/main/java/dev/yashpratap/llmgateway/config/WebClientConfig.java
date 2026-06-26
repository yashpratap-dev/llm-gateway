package dev.yashpratap.llmgateway.config;

import dev.yashpratap.llmgateway.provider.anthropic.AnthropicProperties;
import dev.yashpratap.llmgateway.provider.groq.GroqProperties;
import dev.yashpratap.llmgateway.provider.openai.OpenAIProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient bean configuration for each downstream LLM provider.
 *
 * <p>Each provider gets its own pre-configured {@link WebClient} with a reactive
 * {@link HttpClient} timeout, the correct base URL, and the {@code Authorization} header
 * pre-populated. Provider beans are conditional on the corresponding API key being set,
 * so omitting a key simply excludes that provider from the routing pool.</p>
 */
@Configuration
public class WebClientConfig {

    private final GroqProperties groqProperties;
    private final OpenAIProperties openAIProperties;
    private final AnthropicProperties anthropicProperties;

    /**
     * Constructs the config with all provider properties.
     *
     * @param groqProperties      bound Groq configuration
     * @param openAIProperties    bound OpenAI configuration
     * @param anthropicProperties bound Anthropic configuration
     */
    public WebClientConfig(GroqProperties groqProperties,
                           OpenAIProperties openAIProperties,
                           AnthropicProperties anthropicProperties) {
        this.groqProperties = groqProperties;
        this.openAIProperties = openAIProperties;
        this.anthropicProperties = anthropicProperties;
    }

    /**
     * Creates the Groq-specific {@link WebClient}, only when
     * {@code providers.groq.api-key} is set.
     *
     * @return a {@link WebClient} pre-configured for the Groq REST API
     */
    @Bean
    @Qualifier("groqWebClient")
    @ConditionalOnProperty(prefix = "providers.groq", name = "api-key")
    public WebClient groqWebClient() {
        return buildWebClient(
                groqProperties.baseUrl(),
                groqProperties.apiKey(),
                groqProperties.timeoutSeconds());
    }

    /**
     * Creates the OpenAI-specific {@link WebClient}, only when
     * {@code providers.openai.api-key} is set.
     *
     * @return a {@link WebClient} pre-configured for the OpenAI REST API
     */
    @Bean
    @Qualifier("openaiWebClient")
    @ConditionalOnProperty(prefix = "providers.openai", name = "api-key")
    public WebClient openaiWebClient() {
        return buildWebClient(
                openAIProperties.baseUrl(),
                openAIProperties.apiKey(),
                openAIProperties.timeoutSeconds());
    }

    /**
     * Creates a WebClient for the OpenAI embeddings endpoint, using the OpenAI API key.
     * No connection-level timeout — callers apply per-request timeouts via Mono.timeout().
     */
    @Bean
    @Qualifier("embeddingWebClient")
    @ConditionalOnProperty(prefix = "providers.openai", name = "api-key")
    public WebClient embeddingWebClient() {
        return WebClient.builder()
                .baseUrl(openAIProperties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Creates the Anthropic-specific {@link WebClient}, only when
     * {@code providers.anthropic.api-key} is set.
     *
     * <p>Anthropic uses {@code x-api-key} (not {@code Authorization: Bearer}),
     * so this bean cannot reuse {@link #buildWebClient(String, String, int)}
     * and is constructed inline with the same timeout pattern.</p>
     *
     * @return a {@link WebClient} pre-configured for the Anthropic Messages API
     */
    @Bean
    @Qualifier("anthropicWebClient")
    @ConditionalOnProperty(prefix = "providers.anthropic", name = "api-key")
    public WebClient anthropicWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(anthropicProperties.timeoutSeconds()));
        return WebClient.builder()
                .baseUrl(anthropicProperties.baseUrl())
                .defaultHeader("x-api-key", anthropicProperties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private WebClient buildWebClient(String baseUrl, String apiKey, int timeoutSeconds) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
