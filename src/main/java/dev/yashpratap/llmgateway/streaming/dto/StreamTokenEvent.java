package dev.yashpratap.llmgateway.streaming.dto;

/** A single streamed text fragment sent as an SSE "token" event. */
public record StreamTokenEvent(String delta) {}
