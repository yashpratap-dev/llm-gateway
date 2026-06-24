package dev.yashpratap.llmgateway.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces per-tenant rate limits using a fixed-window counter algorithm backed by Redis.
 *
 * <p>Algorithm: a counter key is incremented on each request. The key is given an expiry of
 * {@value #WINDOW_SECONDS} seconds on its first increment, resetting the window. Requests are
 * allowed while the counter is at or below the plan limit; rejected once it exceeds the limit.</p>
 *
 * <p>The Lua script ensures the increment and the limit check are executed atomically, preventing
 * race conditions under concurrent traffic. The script is executed via {@code EVAL} on the Redis
 * server, so it runs as a single atomic command.</p>
 */
@Service
public class RateLimiterService {

    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "FREE", 10,
            "PRO", 60,
            "ENTERPRISE", 300
    );

    private static final String PREFIX = "rate_limit:";
    private static final int WINDOW_SECONDS = 60;

    private static final String SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            if current > limit then
                return 0
            end
            return 1
            """;

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs the rate limiter service with its Redis dependency.
     *
     * @param redisTemplate the {@link StringRedisTemplate} used for atomic Lua script execution
     */
    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks whether the tenant is within their rate limit for the current window.
     *
     * <p>Atomically increments the request counter and returns {@code true} if the counter
     * is within the plan limit. Returns {@code false} if the limit is exceeded.</p>
     *
     * @param tenantId the UUID of the tenant making the request
     * @param plan     the tenant's subscription plan (FREE, PRO, or ENTERPRISE)
     * @return {@code true} if the request is allowed; {@code false} if rate-limited
     */
    public boolean isAllowed(UUID tenantId, String plan) {
        String key = PREFIX + tenantId;
        int limit = PLAN_LIMITS.getOrDefault(plan.toUpperCase(), 10);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(key),
                String.valueOf(limit), String.valueOf(WINDOW_SECONDS));
        return result != null && result == 1L;
    }

    /**
     * Returns the number of requests remaining in the current rate-limit window.
     *
     * <p>Returns the full plan limit when no requests have been made in the current window
     * (key does not exist in Redis). Never returns a negative value.</p>
     *
     * @param tenantId the UUID of the tenant
     * @param plan     the tenant's subscription plan
     * @return the number of requests remaining in the current window
     */
    public int getRemainingRequests(UUID tenantId, String plan) {
        String key = PREFIX + tenantId;
        int limit = PLAN_LIMITS.getOrDefault(plan.toUpperCase(), 10);
        String current = redisTemplate.opsForValue().get(key);
        if (current == null) {
            return limit;
        }
        return Math.max(0, limit - Integer.parseInt(current));
    }
}
