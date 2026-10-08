package com.angelo.docintel.llm;

/**
 * A provider-agnostic client that turns a grounded request into answer text.
 *
 * <p>Implementations are either live HTTP providers (Anthropic, OpenAI, ...) that
 * phrase the grounded context, or the deterministic offline fallback that extracts
 * an answer from the passages. The {@link CascadeLlmClient} composes them.
 */
public interface LlmClient {

    /** Short, stable id for logging and for tagging results (e.g. {@code anthropic}). */
    String name();

    /**
     * Whether this client can be used right now. HTTP providers return {@code true}
     * only when their API key is configured, which is why they are never exercised
     * in tests. The offline fallback is always available.
     */
    boolean isAvailable();

    /**
     * Produce answer text grounded in {@code request.context()}.
     *
     * @throws LlmException if the client is available but fails to produce an answer
     */
    LlmResult answer(GroundedRequest request) throws LlmException;
}
