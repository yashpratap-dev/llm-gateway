package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.yashpratap.llmgateway.provider.ChatChunk;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.anthropic.AnthropicProperties;
import dev.yashpratap.llmgateway.provider.anthropic.AnthropicProvider;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AnthropicProvider} using WireMock to stub the Anthropic Messages API.
 */
@WireMockTest
class AnthropicProviderTest {

    private AnthropicProvider provider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        AnthropicProperties props = new AnthropicProperties(
                "test-key",
                wm.getHttpBaseUrl(),
                "claude-3-5-sonnet-20241022",
                30,
                1024);
        WebClient webClient = WebClient.builder()
                .baseUrl(wm.getHttpBaseUrl())
                .defaultHeader("x-api-key", "test-key")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        provider = new AnthropicProvider(webClient, props, objectMapper);
    }

    @Test
    void testGenerate_success_returnsChatResponse() {
        stubFor(post("/v1/messages").willReturn(okJson("""
                {
                  "id": "msg_test123",
                  "model": "claude-3-5-sonnet-20241022",
                  "content": [{"type": "text", "text": "Hello from Claude!"}],
                  "usage": {"input_tokens": 10, "output_tokens": 8}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());
        ChatResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.provider()).isEqualTo("CLAUDE");
        assertThat(response.choices()).hasSize(1);
        assertThat(response.choices().get(0).message().content()).isEqualTo("Hello from Claude!");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(8);
    }

    @Test
    void testGenerate_http500_throwsProviderException() {
        stubFor(post("/v1/messages").willReturn(serverError()));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class);
    }

    @Test
    void testGenerate_withSystemMessage_extractsSystemField() {
        stubFor(post("/v1/messages").willReturn(okJson("""
                {
                  "id": "msg_sys",
                  "model": "claude-3-5-sonnet-20241022",
                  "content": [{"type": "text", "text": "Sure!"}],
                  "usage": {"input_tokens": 15, "output_tokens": 3}
                }
                """)));

        ChatRequest request = new ChatRequest("auto",
                List.of(
                        new Message("system", "You are a helpful assistant."),
                        new Message("user", "Help me.")),
                false, Map.of());
        provider.generate(request);

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system",
                        WireMock.equalTo("You are a helpful assistant.")))
                .withRequestBody(matchingJsonPath("$.messages[0].role",
                        WireMock.equalTo("user"))));
    }

    @Test
    void testGenerate_requestModelOverridesDefault() {
        stubFor(post("/v1/messages").willReturn(okJson("""
                {
                  "id": "msg_haiku",
                  "model": "claude-3-haiku-20240307",
                  "content": [{"type": "text", "text": "Hi!"}],
                  "usage": {"input_tokens": 5, "output_tokens": 2}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "claude-3-haiku-20240307",
                List.of(new Message("user", "Hi")), false, Map.of());
        provider.generate(request);

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.model",
                        WireMock.equalTo("claude-3-haiku-20240307"))));
    }

    @Test
    void testGenerate_malformedJsonResponse_throwsProviderException() {
        stubFor(post("/v1/messages").willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not-valid-json")));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class);
    }

    @Test
    void testGenerate_emptyContentArray_throwsProviderException() {
        stubFor(post("/v1/messages").willReturn(okJson("""
                {
                  "id": "msg_empty",
                  "model": "claude-3-5-sonnet-20241022",
                  "content": [],
                  "usage": {"input_tokens": 1, "output_tokens": 0}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("empty or malformed content");
    }

    @Test
    void testStream_success_emitsChunks() {
        String sseBody = """
                event: content_block_delta\r
                data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hello"}}\r
                \r
                event: content_block_delta\r
                data: {"type":"content_block_delta","delta":{"type":"text_delta","text":" world"}}\r
                \r
                event: message_stop\r
                data: {"type":"message_stop"}\r
                \r
                """;

        stubFor(post("/v1/messages").willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), true, Map.of());
        List<ChatChunk> chunks = provider.stream(request).collectList().block();

        assertThat(chunks).isNotNull().hasSize(2);
        assertThat(chunks.get(0).delta()).isEqualTo("Hello");
        assertThat(chunks.get(1).delta()).isEqualTo(" world");
    }

    @Test
    void testStream_unknownEventType_ignoredSilently() {
        String sseBody = """
                event: ping\r
                data: {}\r
                \r
                event: content_block_delta\r
                data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"hi"}}\r
                \r
                event: message_stop\r
                data: {"type":"message_stop"}\r
                \r
                """;

        stubFor(post("/v1/messages").willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hello")), true, Map.of());
        List<ChatChunk> chunks = provider.stream(request).collectList().block();

        assertThat(chunks).isNotNull().hasSize(1);
        assertThat(chunks.get(0).delta()).isEqualTo("hi");
    }

    @Test
    void testStream_malformedSseLine_skippedSilently() {
        String sseBody = """
                data: this-is-not-json\r
                \r
                event: content_block_delta\r
                data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"ok"}}\r
                \r
                event: message_stop\r
                data: {"type":"message_stop"}\r
                \r
                """;

        stubFor(post("/v1/messages").willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hello")), true, Map.of());
        List<ChatChunk> chunks = provider.stream(request).collectList().block();

        assertThat(chunks).isNotNull().hasSize(1);
        assertThat(chunks.get(0).delta()).isEqualTo("ok");
    }
}
