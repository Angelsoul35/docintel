package com.angelo.docintel.retrieval;

import com.angelo.docintel.corpus.Passage;

/**
 * A passage paired with its cosine similarity to a query, in {@code [0, 1]}.
 *
 * @param passage the retrieved passage
 * @param score   cosine similarity between the query and the passage (higher is better)
 */
public record ScoredPassage(Passage passage, double score) {
}
