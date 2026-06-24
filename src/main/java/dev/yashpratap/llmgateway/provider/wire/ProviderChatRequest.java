package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI-compatible chat completion request body serialised to JSON for provider APIs.
 *
 * @param model       the model identifier to use for this request
 * @param messages    ordered list of conversation turns
 * @param stream      {@code true} to request a Server-Sent Events stream
 * @param temperature optional sampling temperature (null uses provider default)
 * @param maxTokens   optional maximum tokens to generate (null uses provider default)
 */
public record ProviderChatRequest(
        @JsonProperty("model") String model,
        @JsonProperty("messages") List<ProviderMessage> messages,
        @JsonProperty("stream") boolean stream,
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {}
