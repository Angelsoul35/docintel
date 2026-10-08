package com.angelo.docintel.llm;

import com.angelo.docintel.retrieval.ScoredPassage;
import java.util.List;

/**
 * Everything an {@link LlmClient} needs to produce a grounded answer: the user's
 * question and the retrieved passages it must be answered from. Clients must not
 * answer from anything outside this context.
 *
 * @param question the user question
 * @param context  the retrieved passages, best first
 */
public record GroundedRequest(String question, List<ScoredPassage> context) {

    public GroundedRequest {
        context = List.copyOf(context);
    }
}
