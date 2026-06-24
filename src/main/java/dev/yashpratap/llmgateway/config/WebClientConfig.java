package dev.yashpratap.llmgateway.config;

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

    /**
     * Constructs the config with both sets of provider properties.
     *
     * @param groqProperties   bound Groq configuration
     * @param openAIProperties bound OpenAI configuration
     */
    public WebClientConfig(GroqProperties groqProperties, OpenAIProperties openAIProperties) {
        this.groqProperties = groqProperties;
        this.openAIProperties = openAIProperties;
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
