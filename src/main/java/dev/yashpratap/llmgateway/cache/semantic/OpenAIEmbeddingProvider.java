package dev.yashpratap.llmgateway.cache.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnExpression("'${providers.openai.api-key:}'.length() > 0")
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);

    private final WebClient webClient;
    private final EmbeddingProperties embeddingProperties;
    private final Timer embeddingLatencyTimer;

    public OpenAIEmbeddingProvider(
            @Qualifier("embeddingWebClient") WebClient webClient,
            EmbeddingProperties embeddingProperties,
            MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.embeddingProperties = embeddingProperties;
        this.embeddingLatencyTimer = meterRegistry.timer("llm.embedding.latency");
    }

    @Override
    public Optional<float[]> embed(String normalizedText) {
        Timer.Sample sample = Timer.start();
        try {
            JsonNode body = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(Map.of("model", embeddingProperties.model(), "input", normalizedText))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofMillis(embeddingProperties.timeoutMs()))
                    .block();

            if (body == null || !body.hasNonNull("data") || body.get("data").isEmpty()) {
                log.warn("Empty or null embedding response from OpenAI");
                return Optional.empty();
            }

            JsonNode embeddingNode = body.get("data").get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                log.warn("Missing embedding array in OpenAI response");
                return Optional.empty();
            }

            float[] result = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                result[i] = (float) embeddingNode.get(i).asDouble();
            }
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Failed to generate embedding: {}", e.getMessage());
            return Optional.empty();
        } finally {
            sample.stop(embeddingLatencyTimer);
        }
    }
}
