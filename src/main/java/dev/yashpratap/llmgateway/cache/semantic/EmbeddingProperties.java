package dev.yashpratap.llmgateway.cache.semantic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache.semantic.embedding")
public record EmbeddingProperties(
        String model,
        int dimensions,
        long timeoutMs) {
}
