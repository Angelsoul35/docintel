package com.angelo.docintel.support;

import com.angelo.docintel.llm.AnswerSource;
import com.angelo.docintel.llm.GroundedRequest;
import com.angelo.docintel.llm.LlmClient;
import com.angelo.docintel.llm.LlmResult;

/**
 * A fake live LLM for tests. It never touches the network: it returns a canned answer
 * tagged as {@link AnswerSource#LLM}, so the cascade and answerer can be exercised as
 * if a real provider were present. Availability is controllable.
 */
public class FakeLlmClient implements LlmClient {

    private final String name;
    private final String cannedAnswer;
    private final boolean available;
    private int calls = 0;

    public FakeLlmClient(String name, String cannedAnswer, boolean available) {
        this.name = name;
        this.cannedAnswer = cannedAnswer;
        this.available = available;
    }

    public FakeLlmClient(String cannedAnswer) {
        this("fake", cannedAnswer, true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public LlmResult answer(GroundedRequest request) {
        calls++;
        return new LlmResult(cannedAnswer, AnswerSource.LLM, name);
    }

    public int calls() {
        return calls;
    }
}
