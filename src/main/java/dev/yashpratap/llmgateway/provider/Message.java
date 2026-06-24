package dev.yashpratap.llmgateway.provider;

/**
 * A single chat message within a conversation turn.
 *
 * @param role    the speaker: {@code system}, {@code user}, or {@code assistant}
 * @param content the text content of the message
 */
public record Message(String role, String content) {
}
