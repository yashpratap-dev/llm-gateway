package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage statistics returned by the provider in a chat completion response.
 *
 * @param promptTokens     tokens consumed by the input messages
 * @param completionTokens tokens generated in the response
 * @param totalTokens      sum of prompt and completion tokens
 */
public record ProviderUsage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens
) {}
