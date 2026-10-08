package com.angelo.docintel.llm;

import com.fasterxml.jackson.annotation.JsonValue;

/** How an answer was produced. Serialised in lower case in API responses. */
public enum AnswerSource {

    /** Phrased by a live LLM provider from grounded context. */
    LLM,

    /** Composed deterministically from the retrieved passages (offline fallback). */
    EXTRACTIVE,

    /** No grounded answer was produced; the request was escalated. */
    NONE;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }
}
