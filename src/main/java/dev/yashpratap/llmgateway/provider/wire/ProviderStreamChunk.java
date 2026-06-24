package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single Server-Sent Events data chunk deserialised from a streaming provider response.
 *
 * @param id      unique chunk identifier (matches the parent response id)
 * @param choices list of delta choices; each carries the incremental text fragment
 */
public record ProviderStreamChunk(
        @JsonProperty("id") String id,
        @JsonProperty("choices") List<ProviderChoice> choices
) {}
