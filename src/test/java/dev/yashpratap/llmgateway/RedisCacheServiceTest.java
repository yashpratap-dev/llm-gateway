package dev.yashpratap.llmgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.yashpratap.llmgateway.cache.CacheKeyGenerator;
import dev.yashpratap.llmgateway.cache.RedisCacheService;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisCacheService}.
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private CacheKeyGenerator cacheKeyGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private RedisCacheService redisCacheService;

    private static final String CACHE_KEY_SUFFIX = "llm:cache:abc123hash";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        redisCacheService = new RedisCacheService(redisTemplate, cacheKeyGenerator, objectMapper);
    }

    @Test
    void testGet_cacheHit_returnsCachedResponse() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ChatRequest request = new ChatRequest("gpt-4", List.of(), false, Map.of());
        ChatResponse expected = new ChatResponse(
                "resp-1", "gpt-4", "OPENAI", List.of(),
                new Usage(10, 20, 0.01), new GatewayMeta("OPENAI", false, 100L));

        when(cacheKeyGenerator.generate(request)).thenReturn(CACHE_KEY_SUFFIX);
        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(expected));

        Optional<ChatResponse> result = redisCacheService.get(tenantId, request);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("resp-1");
        assertThat(result.get().model()).isEqualTo("gpt-4");
    }

    @Test
    void testGet_cacheMiss_returnsEmpty() {
        UUID tenantId = UUID.randomUUID();
        ChatRequest request = new ChatRequest("gpt-4", List.of(), false, Map.of());

        when(cacheKeyGenerator.generate(request)).thenReturn(CACHE_KEY_SUFFIX);
        when(valueOps.get(anyString())).thenReturn(null);

        Optional<ChatResponse> result = redisCacheService.get(tenantId, request);

        assertThat(result).isEmpty();
    }

    @Test
    void testPut_storesInRedisWithTenantIsolation() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ChatRequest request = new ChatRequest("gpt-4", List.of(), false, Map.of());
        ChatResponse response = new ChatResponse(
                "resp-1", "gpt-4", "OPENAI", List.of(),
                new Usage(10, 20, 0.01), new GatewayMeta("OPENAI", false, 100L));

        when(cacheKeyGenerator.generate(request)).thenReturn(CACHE_KEY_SUFFIX);

        redisCacheService.put(tenantId, request, response);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), any(String.class), eq(Duration.ofHours(1)));

        String capturedKey = keyCaptor.getValue();
        assertThat(capturedKey).startsWith("llm_cache:" + tenantId + ":");
        assertThat(capturedKey).contains(CACHE_KEY_SUFFIX);
    }
}
