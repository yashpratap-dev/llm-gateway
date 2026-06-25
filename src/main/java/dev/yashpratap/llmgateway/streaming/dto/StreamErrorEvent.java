package dev.yashpratap.llmgateway.streaming.dto;

/** SSE "error" event payload sent if the provider stream fails. */
public record StreamErrorEvent(String message, String errorCode) {}
