package dev.yashpratap.llmgateway.streaming.dto;

/** Final SSE "done" event metadata. No cost/tokens in this version (known limitation). */
public record StreamDoneEvent(String provider, long latencyMs, boolean cacheHit) {}
