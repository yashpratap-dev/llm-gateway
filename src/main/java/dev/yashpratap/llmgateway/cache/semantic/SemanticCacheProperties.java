package dev.yashpratap.llmgateway.cache.semantic;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "cache.semantic")
@Validated
public record SemanticCacheProperties(
        boolean enabled,
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        long ttlHours,
        boolean modelIsolation,
        String embeddingModelVersion) {
}
