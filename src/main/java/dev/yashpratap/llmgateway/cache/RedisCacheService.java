package dev.yashpratap.llmgateway.cache;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service responsible for reading and writing exact-match chat responses in Redis.
 *
 * <p>A cache hit avoids a downstream provider call, reducing latency and cost.
 * Full TTL strategy and serialisation logic are implemented in M5.</p>
 */
@Service
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * Constructs the cache service with its required dependencies.
     *
     * @param redisTemplate    the configured Redis template
     * @param cacheKeyGenerator utility for deriving cache keys from requests
     */
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate,
                             CacheKeyGenerator cacheKeyGenerator) {
        this.redisTemplate = redisTemplate;
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    /**
     * Attempts to retrieve a cached response for the given request.
     *
     * @param request the incoming chat request used as a cache lookup key
     * @return an {@link Optional} containing the cached response, or empty on a miss
     */
    public Optional<ChatResponse> get(ChatRequest request) {
        return Optional.empty();
    }

    /**
     * Stores a chat response in Redis with the specified TTL.
     *
     * @param request  the original request (used to derive the cache key)
     * @param response the response to cache
     * @param ttl      how long the entry should live before automatic eviction
     */
    public void put(ChatRequest request, ChatResponse response, Duration ttl) {
        // Full implementation in M5
    }
}
