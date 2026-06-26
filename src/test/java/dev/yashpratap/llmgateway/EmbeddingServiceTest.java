package dev.yashpratap.llmgateway;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.yashpratap.llmgateway.cache.semantic.EmbeddingProperties;
import dev.yashpratap.llmgateway.cache.semantic.OpenAIEmbeddingProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmbeddingServiceTest {

    // Shared across tests to avoid Netty cold-start overhead on subsequent calls.
    private static WebClient sharedWebClient;
    private OpenAIEmbeddingProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        if (sharedWebClient == null) {
            sharedWebClient = WebClient.builder()
                    .baseUrl(wm.getHttpBaseUrl())
                    .defaultHeader("Authorization", "Bearer test-key")
                    .build();
        }
        // Generous timeout so Netty cold-start on the first test does not fire it.
        EmbeddingProperties props = new EmbeddingProperties("text-embedding-3-small", 1536, 5000);
        provider = new OpenAIEmbeddingProvider(sharedWebClient, props, new SimpleMeterRegistry());
    }

    @Test
    @Order(1)
    void testEmbed_success_returnsFloatArrayWithCorrectDimensions() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1536; i++) {
            if (i > 0) sb.append(",");
            sb.append("0.").append(i % 9 + 1);
        }
        sb.append("]");

        stubFor(post("/embeddings").willReturn(okJson("""
                {
                  "data": [
                    {
                      "embedding": %s
                    }
                  ]
                }
                """.formatted(sb))));

        Optional<float[]> result = provider.embed("Hello world");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1536);
    }

    @Test
    @Order(2)
    void testEmbed_timeout_returnsEmpty() {
        // Reuse sharedWebClient (connection already warm after Order-1 test).
        // Short timeout (150ms) < WireMock delay (500ms) → TimeoutException → Optional.empty().
        EmbeddingProperties shortProps = new EmbeddingProperties("text-embedding-3-small", 1536, 150);
        OpenAIEmbeddingProvider shortProvider = new OpenAIEmbeddingProvider(
                sharedWebClient, shortProps, new SimpleMeterRegistry());

        stubFor(post("/embeddings").willReturn(
                aResponse().withFixedDelay(500).withStatus(200).withBody("{}")));

        Optional<float[]> result = shortProvider.embed("Hello world");

        assertThat(result).isEmpty();
    }

    @Test
    @Order(3)
    void testEmbed_http500_returnsEmpty() {
        stubFor(post("/embeddings").willReturn(serverError()));

        Optional<float[]> result = provider.embed("Hello world");

        assertThat(result).isEmpty();
    }
}
