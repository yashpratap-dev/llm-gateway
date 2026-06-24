package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import dev.yashpratap.llmgateway.provider.exception.ProviderUnavailableException;
import dev.yashpratap.llmgateway.provider.groq.GroqProperties;
import dev.yashpratap.llmgateway.provider.groq.GroqProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GroqProvider} using WireMock to stub the Groq REST API.
 */
@WireMockTest
class GroqProviderTest {

    private GroqProvider groqProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        GroqProperties props = new GroqProperties(
                "test-key",
                wmRuntimeInfo.getHttpBaseUrl(),
                "llama-3.3-70b-versatile",
                30);
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .defaultHeader("Authorization", "Bearer test-key")
                .build();
        groqProvider = new GroqProvider(webClient, props, objectMapper);
    }

    @Test
    void testGenerate_successResponse_returnsChatResponse() {
        stubFor(post("/chat/completions").willReturn(okJson("""
                {
                  "id": "test-id",
                  "model": "llama-3.3-70b-versatile",
                  "choices": [{"index": 0, "message": {"role": "assistant", "content": "Hello!"}, "finish_reason": "stop"}],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
                }
                """)));

        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hi")), false, Map.of());
        ChatResponse response = groqProvider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.choices()).hasSize(1);
        assertThat(response.choices().get(0).message().content()).isEqualTo("Hello!");
    }

    @Test
    void testGenerate_serverError_throwsProviderUnavailableException() {
        stubFor(post("/chat/completions").willReturn(serverError()));

        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> groqProvider.generate(request))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void testGenerate_emptyChoices_throwsProviderException() {
        stubFor(post("/chat/completions").willReturn(okJson("""
                {
                  "id": "test-id",
                  "model": "llama-3.3-70b-versatile",
                  "choices": [],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 0, "total_tokens": 10}
                }
                """)));

        ChatRequest request = new ChatRequest("auto",
                List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> groqProvider.generate(request))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("empty choices");
    }
}
