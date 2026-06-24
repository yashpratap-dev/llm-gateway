package dev.yashpratap.llmgateway.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility that generates a deterministic SHA-256 cache key from a {@link ChatRequest}.
 *
 * <p>The request is serialised to JSON, hashed, and prefixed with {@code llm:cache:}
 * to avoid key collisions with other Redis namespaces.</p>
 */
@Component
public class CacheKeyGenerator {

    private final ObjectMapper objectMapper;

    /**
     * Constructs the generator with the shared Jackson mapper.
     *
     * @param objectMapper the configured Jackson {@link ObjectMapper}
     */
    public CacheKeyGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a SHA-256-based Redis cache key for the given chat request.
     *
     * <p>Two requests with identical model, messages, and parameters produce
     * the same key, enabling exact-match caching.</p>
     *
     * @param request the chat request to hash
     * @return a Redis key in the format {@code llm:cache:<hex-hash>}
     * @throws IllegalStateException if JSON serialisation or SHA-256 is unavailable
     */
    public String generate(ChatRequest request) {
        try {
            String serialized = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return "llm:cache:" + HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate cache key", e);
        }
    }
}
