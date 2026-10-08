package com.angelo.docintel.llm;

import com.angelo.docintel.retrieval.ScoredPassage;

/**
 * Builds the grounded prompt sent to a live LLM. The prompt pins the model to the
 * retrieved context and asks it to cite passages by their bracketed index, so a live
 * answer stays as grounded as the extractive fallback.
 */
public final class PromptBuilder {

    private static final String SYSTEM = """
            You are a careful assistant for a document question-answering service.
            Answer the user's question using ONLY the numbered context passages below.
            If the passages do not contain the answer, say you do not have that information.
            Do not use outside knowledge. Cite the passages you use by their number, e.g. [1].""";

    private PromptBuilder() {
    }

    /** The system instruction that constrains the model to the provided context. */
    public static String system() {
        return SYSTEM;
    }

    /** Render the question and numbered context into a single user prompt. */
    public static String user(GroundedRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Context:\n");
        int i = 1;
        for (ScoredPassage scored : request.context()) {
            sb.append('[').append(i++).append("] ")
              .append(scored.passage().documentTitle()).append(" — ")
              .append(scored.passage().text()).append('\n');
        }
        sb.append("\nQuestion: ").append(request.question()).append('\n');
        sb.append("Answer (grounded in the context above, with citations):");
        return sb.toString();
    }
}
