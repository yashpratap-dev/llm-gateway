package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.cache.semantic.EmbeddingProvider;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheEntry;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheProperties;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheRepository;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheService;
import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.Choice;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock EmbeddingProvider embeddingProvider;
    @Mock SemanticCacheRepository semanticCacheRepository;
    @Mock SemanticCacheProperties properties;
    @Mock GatewayMetricsService metricsService;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    ObjectMapper objectMapper = new ObjectMapper();
    SemanticCacheService service;

    static final float[] EMBEDDING = {0.1f, 0.2f, 0.3f};
    static final String MODEL_VERSION = "text-embedding-3-small";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new SemanticCacheService(
                embeddingProvider, semanticCacheRepository, properties,
                metricsService, redisTemplate, objectMapper, Duration.ofHours(24));
    }

    private ChatRequest request() {
        return new ChatRequest("gpt-4", List.of(new Message("user", "What is TCP?")), false, Map.of());
    }

    private ChatResponse response() {
        return new ChatResponse(
                "resp-1", "gpt-4", "OPENAI",
                List.of(new Choice(0, new Message("assistant", "TCP is a protocol"), "stop")),
                new Usage(10, 20, 0.001),
                new GatewayMeta("OPENAI", false, 100L));
    }

    // ── findSemanticMatch ────────────────────────────────────────────────────

    @Test
    void findSemanticMatch_disabled_returnsEmpty() {
        when(properties.enabled()).thenReturn(false);

        Optional<ChatResponse> result = service.findSemanticMatch(UUID.randomUUID(), request());

        assertThat(result).isEmpty();
        verifyNoInteractions(embeddingProvider, semanticCacheRepository);
    }

    @Test
    void findSemanticMatch_embeddingProviderFails_returnsEmpty() {
        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.empty());

        Optional<ChatResponse> result = service.findSemanticMatch(UUID.randomUUID(), request());

        assertThat(result).isEmpty();
        verify(metricsService).recordEmbeddingCacheMiss();
        verifyNoInteractions(semanticCacheRepository);
    }

    @Test
    void findSemanticMatch_noRepositoryMatch_returnsEmpty() {
        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(properties.similarityThreshold()).thenReturn(0.92);
        when(properties.modelIsolation()).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.of(EMBEDDING));
        when(semanticCacheRepository.findBestMatch(any(), any(), any(), any(), anyDouble()))
                .thenReturn(Optional.empty());

        Optional<ChatResponse> result = service.findSemanticMatch(UUID.randomUUID(), request());

        assertThat(result).isEmpty();
        verify(metricsService).recordSemanticCacheMiss();
        verify(metricsService, never()).recordSemanticCacheHit(anyString());
    }

    @Test
    void findSemanticMatch_hit_returnsResponseAndRecordsHit() throws Exception {
        UUID entryId = UUID.randomUUID();
        String responseJson = objectMapper.writeValueAsString(response());
        SemanticCacheEntry entry = new SemanticCacheEntry(entryId, responseJson, 0.95);

        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(properties.similarityThreshold()).thenReturn(0.92);
        when(properties.modelIsolation()).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.of(EMBEDDING));
        when(semanticCacheRepository.findBestMatch(any(), any(), any(), any(), anyDouble()))
                .thenReturn(Optional.of(entry));

        Optional<ChatResponse> result = service.findSemanticMatch(UUID.randomUUID(), request());

        assertThat(result).isPresent();
        assertThat(result.get().model()).isEqualTo("gpt-4");
        verify(semanticCacheRepository).incrementHitCount(entryId);
        verify(metricsService).recordSemanticCacheHit("CACHED");
        verify(metricsService).recordSemanticSimilarityScore(0.95);
    }

    @Test
    void findSemanticMatch_repositoryThrows_failsOpen() {
        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(properties.similarityThreshold()).thenReturn(0.92);
        when(properties.modelIsolation()).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.of(EMBEDDING));
        when(semanticCacheRepository.findBestMatch(any(), any(), any(), any(), anyDouble()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatNoException().isThrownBy(
                () -> service.findSemanticMatch(UUID.randomUUID(), request()));
    }

    // ── storeSemanticEntry ───────────────────────────────────────────────────

    @Test
    void storeSemanticEntry_disabled_doesNothing() {
        when(properties.enabled()).thenReturn(false);

        service.storeSemanticEntry(UUID.randomUUID(), request(), response());

        verifyNoInteractions(embeddingProvider, semanticCacheRepository);
    }

    @Test
    void storeSemanticEntry_embeddingFails_doesNotStore() {
        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.empty());

        service.storeSemanticEntry(UUID.randomUUID(), request(), response());

        verifyNoInteractions(semanticCacheRepository);
    }

    @Test
    void storeSemanticEntry_happyPath_storesEntry() {
        when(properties.enabled()).thenReturn(true);
        when(properties.embeddingModelVersion()).thenReturn(MODEL_VERSION);
        when(properties.modelIsolation()).thenReturn(true);
        when(properties.ttlHours()).thenReturn(24L);
        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.of(EMBEDDING));

        service.storeSemanticEntry(UUID.randomUUID(), request(), response());

        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
        verify(semanticCacheRepository).store(
                any(), any(), any(), any(), any(), any(), any(), any());
    }
}
