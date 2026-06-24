package dev.yashpratap.llmgateway.provider;

/**
 * Enumeration of the LLM providers supported by the gateway.
 *
 * <p>New providers are added here first and then a corresponding
 * {@link LLMProvider} implementation is created in a sub-package.</p>
 */
public enum ProviderName {

    /** Groq cloud inference — ultra-low latency via custom LPU hardware. */
    GROQ,

    /** OpenAI — GPT-4o family models. */
    OPENAI
}
