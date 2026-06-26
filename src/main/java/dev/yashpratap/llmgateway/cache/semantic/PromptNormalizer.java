package dev.yashpratap.llmgateway.cache.semantic;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility that produces a deterministic, normalized string from a ChatRequest
 * for use as the semantic cache lookup key.
 *
 * Normalization algorithm (exact, per locked architecture):
 *   1. Include system message content if messages[0].role == "system"
 *   2. Collect user+assistant pairs from remaining messages; take the last 3
 *   3. Append the final user message if it was not already the user half of the last pair
 *   4. Concatenate parts with single space
 *   5. trim → toLowerCase → collapse whitespace
 */
public final class PromptNormalizer {

    private PromptNormalizer() {}

    public static String normalize(ChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return "";
        }

        List<Message> messages = request.messages();

        // Step 1: Extract system message
        String systemContent = null;
        int startIndex = 0;
        if ("system".equalsIgnoreCase(messages.get(0).role())) {
            systemContent = messages.get(0).content();
            startIndex = 1;
        }

        List<Message> remaining = messages.subList(startIndex, messages.size());

        // Step 2: Collect consecutive user+assistant pairs
        // A pair = user message immediately followed by assistant message
        List<Message[]> pairs = new ArrayList<>();
        for (int i = 0; i < remaining.size() - 1; i++) {
            Message cur = remaining.get(i);
            Message nxt = remaining.get(i + 1);
            if ("user".equalsIgnoreCase(cur.role()) && "assistant".equalsIgnoreCase(nxt.role())) {
                pairs.add(new Message[]{cur, nxt});
            }
        }

        // Take the last 3 pairs (chronological order preserved)
        int fromIndex = Math.max(0, pairs.size() - 3);
        List<Message[]> selected = pairs.subList(fromIndex, pairs.size());

        // Step 3: Find the current (final) user message
        Message finalUserMessage = null;
        for (int i = remaining.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(remaining.get(i).role())) {
                finalUserMessage = remaining.get(i);
                break;
            }
        }

        // Do not duplicate if the final user message is the same object as the last pair's user
        boolean finalUserAlreadyCovered = false;
        if (!selected.isEmpty() && finalUserMessage != null) {
            finalUserAlreadyCovered = (selected.get(selected.size() - 1)[0] == finalUserMessage);
        }

        // Step 4: Concatenate
        StringBuilder sb = new StringBuilder();

        if (systemContent != null && !systemContent.isBlank()) {
            sb.append(systemContent);
        }

        for (Message[] pair : selected) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(pair[0].content());
            sb.append(' ');
            sb.append(pair[1].content());
        }

        if (!finalUserAlreadyCovered && finalUserMessage != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(finalUserMessage.content());
        }

        // Step 5: trim → toLowerCase → collapse whitespace
        return sb.toString().trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
