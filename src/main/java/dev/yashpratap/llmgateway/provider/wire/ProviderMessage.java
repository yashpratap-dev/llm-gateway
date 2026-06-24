package dev.yashpratap.llmgateway.provider.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single message in the wire-format request or response sent to an LLM provider.
 *
 * @param role    the speaker role: {@code system}, {@code user}, or {@code assistant}
 * @param content the text content of the message
 */
public record ProviderMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
) {}
