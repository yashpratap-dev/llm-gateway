package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.cache.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RateLimiterService}.
 */
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    @Test
    void testIsAllowed_underLimit_returnsTrue() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        boolean result = rateLimiterService.isAllowed(UUID.randomUUID(), "FREE");

        assertThat(result).isTrue();
    }

    @Test
    void testIsAllowed_overLimit_returnsFalse() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L);

        boolean result = rateLimiterService.isAllowed(UUID.randomUUID(), "FREE");

        assertThat(result).isFalse();
    }

    @Test
    void testGetRemainingRequests_noKey_returnsFullLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        int remaining = rateLimiterService.getRemainingRequests(UUID.randomUUID(), "FREE");

        assertThat(remaining).isEqualTo(10);
    }
}
