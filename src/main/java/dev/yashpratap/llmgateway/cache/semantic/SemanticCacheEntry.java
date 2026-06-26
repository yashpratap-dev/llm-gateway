package dev.yashpratap.llmgateway.cache.semantic;

import java.util.UUID;

public record SemanticCacheEntry(UUID id, String responseJson, double similarity) {
}
