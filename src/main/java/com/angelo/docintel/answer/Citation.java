package com.angelo.docintel.answer;

import com.angelo.docintel.retrieval.ScoredPassage;

/**
 * A pointer to a passage that supported an answer. Citations are always returned
 * when an answer is grounded, so a caller can verify every claim against the corpus.
 *
 * @param passageId     id of the supporting passage
 * @param documentId    id of the document it came from
 * @param documentTitle title of that document
 * @param snippet       the passage text
 * @param score         the passage's similarity to the question, in {@code [0, 1]}
 */
public record Citation(String passageId, String documentId, String documentTitle, String snippet, double score) {

    public static Citation from(ScoredPassage scored) {
        return new Citation(
                scored.passage().id(),
                scored.passage().documentId(),
                scored.passage().documentTitle(),
                scored.passage().text(),
                round(scored.score()));
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
