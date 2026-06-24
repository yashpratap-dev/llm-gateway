package dev.yashpratap.llmgateway.provider;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic representation of a chat completion request.
 *
 * <p>Each {@link LLMProvider} implementation translates this record into its own
 * wire format before sending it to the upstream API.</p>
 *
 * @param model      the model identifier requested by the client
 * @param messages   the ordered list of conversation turns
 * @param stream     {@code true} to request a Server-Sent Events stream
 * @param parameters additional provider-specific parameters (e.g. temperature, max_tokens)
 */
public record ChatRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Map<String, Object> parameters) {
}
