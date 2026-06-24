package dev.yashpratap.llmgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis infrastructure configuration.
 *
 * <p>Configures a {@link RedisTemplate} with a {@link StringRedisSerializer} for keys
 * and a JSON serialiser for values, ensuring human-readable keys in the cache namespace.</p>
 */
@Configuration
public class RedisConfig {

    /**
     * Creates the primary {@link RedisTemplate} used by {@link dev.yashpratap.llmgateway.cache.RedisCacheService}.
     *
     * @param connectionFactory the auto-configured Redis connection factory
     * @return a fully configured {@link RedisTemplate} with string keys and JSON values
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
