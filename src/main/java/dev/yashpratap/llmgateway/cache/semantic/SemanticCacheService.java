package dev.yashpratap.llmgateway.cache.semantic;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.metrics.GatewayMetricsService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the semantic cache pipeline: embedding retrieval (with Redis caching),
 * vector similarity search, and async cache storage.
 *
 * Must only be called AFTER the exact Redis cache miss; never calls the embedding
 * provider when semantic cache is disabled.
 */
@Service
@ConditionalOnBean(EmbeddingProvider.class)
public class SemanticCacheService {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final String EMB_KEY_PREFIX = "emb:v1:";

    private final EmbeddingProvider embeddingProvider;
    private final SemanticCacheRepository semanticCacheRepository;
    private final SemanticCacheProperties properties;
    private final GatewayMetricsService metricsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration embeddingCacheTtl;

    public SemanticCacheService(
            EmbeddingProvider embeddingProvider,
            SemanticCacheRepository semanticCacheRepository,
            SemanticCacheProperties properties,
            GatewayMetricsService metricsService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${cache.semantic.embedding-cache.ttl}") Duration embeddingCacheTtl) {
        this.embeddingProvider = embeddingProvider;
        this.semanticCacheRepository = semanticCacheRepository;
        this.properties = properties;
        this.metricsService = metricsService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.embeddingCacheTtl = embeddingCacheTtl;
    }

    /**
     * Attempts to find a semantically similar cached response.
     * Returns Optional.empty() on any failure (fail open).
     * Must only be called AFTER the exact Redis cache check has missed.
     */
    public Optional<ChatResponse> findSemanticMatch(UUID tenantId, ChatRequest request) {
        if (!properties.enabled()) return Optional.empty();
        try {
            String normalized = PromptNormalizer.normalize(request);
            if (normalized.isBlank()) return Optional.empty();

            float[] embedding = getOrComputeEmbedding(normalized);
            if (embedding == null) return Optional.empty();

            String embeddingModel = properties.embeddingModelVersion();
            String llmModel = properties.modelIsolation() ? request.model() : "any";

            Optional<SemanticCacheEntry> entry = semanticCacheRepository.findBestMatch(
                    tenantId, llmModel, embeddingModel, embedding, properties.similarityThreshold());

            if (entry.isPresent()) {
                semanticCacheRepository.incrementHitCount(entry.get().id());
                metricsService.recordSemanticCacheHit("CACHED");
                metricsService.recordSemanticSimilarityScore(entry.get().similarity());
                ChatResponse response = deserializeResponse(entry.get().responseJson());
                return Optional.ofNullable(response);
            } else {
                metricsService.recordSemanticCacheMiss();
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("[semantic-cache] findSemanticMatch failed (fail open): {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores a new semantic cache entry asynchronously.
     * Never throws. Logs WARN on any failure.
     */
    @Async
    public void storeSemanticEntry(UUID tenantId, ChatRequest request, ChatResponse response) {
        if (!properties.enabled()) return;
        try {
            String normalized = PromptNormalizer.normalize(request);
            if (normalized.isBlank()) return;

            float[] embedding = getOrComputeEmbedding(normalized);
            if (embedding == null) return;

            String promptHash = sha256Hex(normalized);
            String embeddingModel = properties.embeddingModelVersion();
            String llmModel = properties.modelIsolation() ? request.model() : "any";
            Instant expiresAt = Instant.now().plus(properties.ttlHours(), ChronoUnit.HOURS);
            String responseJson = objectMapper.writeValueAsString(response);

            semanticCacheRepository.store(
                    tenantId, llmModel, embeddingModel,
                    normalized, promptHash, embedding, responseJson, expiresAt);
        } catch (Exception e) {
            log.warn("[semantic-cache] storeSemanticEntry failed: {}", e.getMessage());
        }
    }

    /**
     * Gets embedding from Redis cache; computes via provider on miss.
     * Returns null if embedding is unavailable (fail open).
     */
    private float[] getOrComputeEmbedding(String normalized) {
        String cacheKey = EMB_KEY_PREFIX + properties.embeddingModelVersion()
                + ":" + sha256Hex(normalized);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                metricsService.recordEmbeddingCacheHit();
                return objectMapper.readValue(cached, float[].class);
            }
        } catch (Exception e) {
            log.warn("[semantic-cache] embedding cache read failed: {}", e.getMessage());
        }

        metricsService.recordEmbeddingCacheMiss();
        Optional<float[]> computed = embeddingProvider.embed(normalized);
        if (computed.isEmpty()) return null;

        float[] embedding = computed.get();
        try {
            String json = objectMapper.writeValueAsString(embedding);
            redisTemplate.opsForValue().set(cacheKey, json, embeddingCacheTtl);
        } catch (Exception e) {
            log.warn("[semantic-cache] embedding cache write failed: {}", e.getMessage());
        }
        return embedding;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private ChatResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, ChatResponse.class);
        } catch (Exception e) {
            log.warn("[semantic-cache] failed to deserialize cached response: {}", e.getMessage());
            return null;
        }
    }
}
