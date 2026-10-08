package com.angelo.docintel.llm;

/**
 * The text produced by an {@link LlmClient}, tagged with how it was produced.
 *
 * @param text     the answer text
 * @param source   whether a live LLM or the extractive fallback produced it
 * @param provider the id of the client that produced it (e.g. {@code anthropic}, {@code offline})
 */
public record LlmResult(String text, AnswerSource source, String provider) {
}
