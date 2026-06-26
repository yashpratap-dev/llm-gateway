package dev.yashpratap.llmgateway.cache.semantic;

import java.util.Optional;

public interface EmbeddingProvider {
    /**
     * Returns an embedding vector for the given normalized text.
     * An empty Optional means the embedding failed; callers must skip semantic cache entirely (fail open).
     */
    Optional<float[]> embed(String normalizedText);
}
