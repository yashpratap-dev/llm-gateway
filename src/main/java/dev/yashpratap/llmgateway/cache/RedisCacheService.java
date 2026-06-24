package dev.yashpratap.llmgateway.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for reading and writing exact-match chat responses in Redis.
 *
 * <p>Cache keys include the tenant UUID to guarantee tenant isolation: two tenants
 * making the same request will never share cached responses. Key format:
 * {@code llm_cache:{tenantId}:{sha256-hash-of-request}}.</p>
 *
 * <p>Responses are serialised to JSON via {@link ObjectMapper} and stored as plain strings
 * using {@link StringRedisTemplate}. All errors are swallowed so that a Redis failure
 * never breaks the request path.</p>
 */
@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String PREFIX = "llm_cache:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the cache service with its required dependencies.
     *
     * @param redisTemplate    the {@link StringRedisTemplate} for string-based Redis operations
     * @param cacheKeyGenerator utility for deriving a SHA-256 key suffix from a request
     * @param objectMapper     Jackson mapper for serialising and deserialising responses
     */
    public RedisCacheService(StringRedisTemplate redisTemplate,
                             CacheKeyGenerator cacheKeyGenerator,
                             ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Attempts to retrieve a cached response for the given tenant and request.
     *
     * @param tenantId the UUID of the requesting tenant (used for key isolation)
     * @param request  the incoming chat request used as the cache lookup key
     * @return an {@link Optional} containing the deserialised response, or empty on a miss
     */
    public Optional<ChatResponse> get(UUID tenantId, ChatRequest request) {
        String key = PREFIX + tenantId + ":" + cacheKeyGenerator.generate(request);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, ChatResponse.class));
        } catch (Exception e) {
            log.debug("Cache read error for tenant {}: {}", tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores a chat response in Redis under a tenant-scoped key with a 1-hour TTL.
     *
     * @param tenantId the UUID of the requesting tenant (used for key isolation)
     * @param request  the original request (used to derive the cache key)
     * @param response the response to cache
     */
    public void put(UUID tenantId, ChatRequest request, ChatResponse response) {
        String key = PREFIX + tenantId + ":" + cacheKeyGenerator.generate(request);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), TTL);
        } catch (Exception e) {
            log.warn("Cache write error for tenant {}: {}", tenantId, e.getMessage());
        }
    }
}
