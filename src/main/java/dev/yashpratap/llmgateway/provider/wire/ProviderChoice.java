package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single completion choice within a provider's chat response.
 *
 * <p>For streaming responses {@code delta} carries the incremental text fragment;
 * for non-streaming responses {@code message} carries the full assistant message.</p>
 *
 * @param index        zero-based index of this choice in the response
 * @param message      full assistant message (non-streaming only; may be null in stream chunks)
 * @param finishReason reason the model stopped generating (e.g. {@code stop}, {@code length})
 * @param delta        incremental text fragment (streaming only; null in non-streaming responses)
 */
public record ProviderChoice(
        @JsonProperty("index") int index,
        @JsonProperty("message") ProviderMessage message,
        @JsonProperty("finish_reason") String finishReason,
        @JsonProperty("delta") ProviderMessage delta
) {}
