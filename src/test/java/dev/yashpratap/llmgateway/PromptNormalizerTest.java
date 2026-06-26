package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.cache.semantic.PromptNormalizer;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.Message;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptNormalizerTest {

    private static ChatRequest req(Message... messages) {
        return new ChatRequest("auto", List.of(messages), false, Map.of());
    }

    private static Message user(String content) {
        return new Message("user", content);
    }

    private static Message asst(String content) {
        return new Message("assistant", content);
    }

    private static Message sys(String content) {
        return new Message("system", content);
    }

    @Test
    void normalize_emptyMessages_returnsEmptyString() {
        ChatRequest request = new ChatRequest("auto", List.of(), false, Map.of());
        assertThat(PromptNormalizer.normalize(request)).isEmpty();
    }

    @Test
    void normalize_singleUserMessage_returnsNormalized() {
        ChatRequest request = req(user("What is TCP?"));
        assertThat(PromptNormalizer.normalize(request)).isEqualTo("what is tcp?");
    }

    @Test
    void normalize_withSystemMessage_includesSystem() {
        ChatRequest request = req(
                sys("You are helpful"),
                user("What is TCP?"));
        assertThat(PromptNormalizer.normalize(request)).isEqualTo("you are helpful what is tcp?");
    }

    @Test
    void normalize_withMoreThan3Pairs_takesOnlyLast3() {
        // 4 pairs + final user message
        ChatRequest request = req(
                user("q1"), asst("a1"),
                user("q2"), asst("a2"),
                user("q3"), asst("a3"),
                user("q4"), asst("a4"),
                user("q5"));

        String result = PromptNormalizer.normalize(request);

        // Only last 3 pairs (q2..a4) + final user (q5) should be included
        assertThat(result).doesNotContain("q1");
        assertThat(result).doesNotContain("a1");
        assertThat(result).contains("q2", "a2", "q3", "a3", "q4", "a4", "q5");
    }

    @Test
    void normalize_mixedCase_returnsLowercase() {
        ChatRequest request = req(
                sys("You Are HELPFUL"),
                user("WHAT IS TCP?"),
                asst("TCP IS A Protocol"),
                user("Compare"));

        String result = PromptNormalizer.normalize(request);

        assertThat(result).isEqualTo("you are helpful what is tcp? tcp is a protocol compare");
    }

    @Test
    void normalize_extraWhitespace_collapsed() {
        ChatRequest request = req(user("  hello   world  "));
        assertThat(PromptNormalizer.normalize(request)).isEqualTo("hello world");
    }

    @Test
    void normalize_noAssistantMessages_handlesGracefully() {
        // Multiple user messages but no assistant — no pairs formed, only final user included
        ChatRequest request = req(user("q1"), user("q2"), user("q3"));
        String result = PromptNormalizer.normalize(request);
        // Final user message is q3; q1, q2 are not part of any pair
        assertThat(result).isEqualTo("q3");
    }
}
