package com.angelo.docintel.answer;

import com.angelo.docintel.llm.AnswerSource;
import java.util.List;

/**
 * The result of answering a question.
 *
 * @param answer     the answer text (or an escalation message when nothing was grounded)
 * @param source     how it was produced: {@code llm}, {@code extractive} or {@code none}
 * @param citations  the passages the answer was grounded in (empty when escalated)
 * @param confidence the best passage's similarity, in {@code [0, 1]} ({@code 0} when escalated)
 * @param escalated  true when retrieval fell below the grounding floor and no answer was attempted
 */
public record Answer(String answer, AnswerSource source, List<Citation> citations,
                     double confidence, boolean escalated) {

    public Answer {
        citations = List.copyOf(citations);
    }

    /** Build the "no grounded information" response that escalates instead of guessing. */
    public static Answer escalated(String message) {
        return new Answer(message, AnswerSource.NONE, List.of(), 0.0, true);
    }
}
