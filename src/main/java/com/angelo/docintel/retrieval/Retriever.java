package com.angelo.docintel.retrieval;

import java.util.List;

/** Returns the passages most relevant to a query, best first. */
public interface Retriever {

    /**
     * Retrieve up to {@code k} passages for {@code query}, ordered by descending
     * similarity. Passages with a zero score are omitted.
     *
     * @return a possibly empty, immutable list sorted best-first
     */
    List<ScoredPassage> retrieve(String query, int k);
}
