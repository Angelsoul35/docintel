package com.angelo.docintel.support;

import com.angelo.docintel.llm.GroundedRequest;
import com.angelo.docintel.llm.LlmClient;
import com.angelo.docintel.llm.LlmException;
import com.angelo.docintel.llm.LlmResult;

/**
 * An available LLM that always fails, used to prove the cascade falls through to the
 * next client. It never touches the network.
 */
public class FailingLlmClient implements LlmClient {

    private final String name;
    private int calls = 0;

    public FailingLlmClient(String name) {
        this.name = name;
    }

    public FailingLlmClient() {
        this("failing");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResult answer(GroundedRequest request) {
        calls++;
        throw new LlmException(name + " is deliberately failing");
    }

    public int calls() {
        return calls;
    }
}
