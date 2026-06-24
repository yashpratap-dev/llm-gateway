package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI-compatible chat completion response body deserialised from the provider's JSON.
 *
 * @param id      unique response identifier assigned by the provider
 * @param model   the model that generated the response
 * @param choices list of completion choices (typically one element)
 * @param usage   token usage statistics (may be null when the provider omits it)
 */
public record ProviderChatResponse(
        @JsonProperty("id") String id,
        @JsonProperty("model") String model,
        @JsonProperty("choices") List<ProviderChoice> choices,
        @JsonProperty("usage") ProviderUsage usage
) {}
