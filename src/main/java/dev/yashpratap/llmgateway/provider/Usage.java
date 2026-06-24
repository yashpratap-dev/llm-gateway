package dev.yashpratap.llmgateway.provider;

/**
 * Token usage and cost summary for a completed chat request.
 *
 * @param promptTokens     number of tokens consumed by the input messages
 * @param completionTokens number of tokens generated in the response
 * @param costUsd          total USD cost calculated from model pricing
 */
public record Usage(int promptTokens, int completionTokens, double costUsd) {
}
