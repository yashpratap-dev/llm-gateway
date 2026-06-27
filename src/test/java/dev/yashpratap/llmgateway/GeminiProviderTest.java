package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.yashpratap.llmgateway.provider.ChatChunk;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.gemini.GeminiProperties;
import dev.yashpratap.llmgateway.provider.gemini.GeminiProvider;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GeminiProvider} using WireMock to stub the Gemini Generative Language API.
 */
@WireMockTest
class GeminiProviderTest {

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String API_VERSION   = "v1beta";
    private static final String GENERATE_PATH = "/" + API_VERSION + "/models/" + DEFAULT_MODEL + ":generateContent";
    private static final String STREAM_PATH   = "/" + API_VERSION + "/models/" + DEFAULT_MODEL + ":streamGenerateContent";
    private static final String PRO_PATH      = "/" + API_VERSION + "/models/gemini-1.5-pro:generateContent";

    private GeminiProvider provider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        GeminiProperties props = new GeminiProperties(
                "test-key",
                wm.getHttpBaseUrl(),
                API_VERSION,
                DEFAULT_MODEL,
                30,
                1024);
        WebClient webClient = WebClient.builder()
                .baseUrl(wm.getHttpBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        provider = new GeminiProvider(webClient, props, objectMapper);
    }

    // ─────────────────────────────── generate() tests ────────────────────────────────

    @Test
    void testGenerate_success_returnsChatResponse() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(okJson("""
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "Hello from Gemini!"}], "role": "model"}, "finishReason": "STOP"}
                  ],
                  "usageMetadata": {"promptTokenCount": 10, "candidatesTokenCount": 8}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());
        ChatResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.provider()).isEqualTo("GEMINI");
        assertThat(response.choices()).hasSize(1);
        assertThat(response.choices().get(0).message().content()).isEqualTo("Hello from Gemini!");
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(8);
    }

    @Test
    void testGenerate_http500_throwsProviderException() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(serverError()));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class);
    }

    @Test
    void testGenerate_http400_throwsProviderException() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(
                aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": {"code": 400, "message": "API key not valid."}}
                                """)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Gemini client error");
    }

    @Test
    void testGenerate_roleMapping_systemMessageBecomesUser() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(okJson("""
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "Sure!"}], "role": "model"}}
                  ],
                  "usageMetadata": {"promptTokenCount": 15, "candidatesTokenCount": 3}
                }
                """)));

        ChatRequest request = new ChatRequest("auto",
                List.of(
                        new Message("system", "You are a helpful assistant."),
                        new Message("user", "Help me.")),
                false, Map.of());
        provider.generate(request);

        // Verify the system message was mapped to role=user in the Gemini request
        verify(postRequestedFor(urlPathEqualTo(GENERATE_PATH))
                .withRequestBody(matchingJsonPath("$.contents[0].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text",
                        equalTo("You are a helpful assistant."))));
    }

    @Test
    void testGenerate_modelOverride_usesGeminiPro() {
        stubFor(post(urlPathEqualTo(PRO_PATH)).willReturn(okJson("""
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "Pro response"}], "role": "model"}}
                  ],
                  "usageMetadata": {"promptTokenCount": 5, "candidatesTokenCount": 2}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "gemini-1.5-pro",
                List.of(new Message("user", "Hi")), false, Map.of());
        ChatResponse response = provider.generate(request);

        assertThat(response.model()).isEqualTo("gemini-1.5-pro");
        verify(postRequestedFor(urlPathEqualTo(PRO_PATH)));
    }

    @Test
    void testGenerate_nonGeminiModel_fallsBackToDefault() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(okJson("""
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "Fallback response"}], "role": "model"}}
                  ],
                  "usageMetadata": {"promptTokenCount": 5, "candidatesTokenCount": 4}
                }
                """)));

        // "gpt-4o" does not start with "gemini-" → provider falls back to gemini-1.5-flash
        ChatRequest request = new ChatRequest(
                "gpt-4o",
                List.of(new Message("user", "Hi")), false, Map.of());
        ChatResponse response = provider.generate(request);

        assertThat(response.model()).isEqualTo(DEFAULT_MODEL);
        verify(postRequestedFor(urlPathEqualTo(GENERATE_PATH)));
    }

    @Test
    void testGenerate_malformedJsonResponse_throwsProviderException() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not-valid-json")));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class);
    }

    @Test
    void testGenerate_emptyCandidates_throwsProviderException() {
        stubFor(post(urlPathEqualTo(GENERATE_PATH)).willReturn(okJson("""
                {
                  "candidates": [],
                  "usageMetadata": {"promptTokenCount": 1, "candidatesTokenCount": 0}
                }
                """)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), false, Map.of());

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("empty or malformed content");
    }

    // ─────────────────────────────── stream() tests ──────────────────────────────────

    @Test
    void testStream_success_emitsChunks() {
        String sseBody = """
                data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}]}\r
                \r
                data: {"candidates":[{"content":{"parts":[{"text":" world"}],"role":"model"}}]}\r
                \r
                """;

        stubFor(post(urlPathEqualTo(STREAM_PATH)).willReturn(
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
    void testStream_emptyCandidatesChunk_skippedSilently() {
        String sseBody = """
                data: {"candidates":[]}\r
                \r
                data: {"candidates":[{"content":{"parts":[{"text":"ok"}],"role":"model"}}]}\r
                \r
                """;

        stubFor(post(urlPathEqualTo(STREAM_PATH)).willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), true, Map.of());
        List<ChatChunk> chunks = provider.stream(request).collectList().block();

        assertThat(chunks).isNotNull().hasSize(1);
        assertThat(chunks.get(0).delta()).isEqualTo("ok");
    }

    @Test
    void testStream_malformedSseLine_skippedSilently() {
        String sseBody = """
                data: this-is-not-json\r
                \r
                data: {"candidates":[{"content":{"parts":[{"text":"ok"}],"role":"model"}}]}\r
                \r
                """;

        stubFor(post(urlPathEqualTo(STREAM_PATH)).willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody)));

        ChatRequest request = new ChatRequest(
                "auto", List.of(new Message("user", "Hi")), true, Map.of());
        List<ChatChunk> chunks = provider.stream(request).collectList().block();

        assertThat(chunks).isNotNull().hasSize(1);
        assertThat(chunks.get(0).delta()).isEqualTo("ok");
    }
}
