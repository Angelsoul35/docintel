package com.angelo.docintel.eval;

import com.angelo.docintel.corpus.DocumentStore;
import com.angelo.docintel.corpus.Passage;
import com.angelo.docintel.retrieval.Retriever;
import com.angelo.docintel.retrieval.ScoredPassage;
import java.util.ArrayList;
import java.util.List;

/**
 * A deliberately naive baseline {@link Retriever}: it ignores the query entirely and
 * always returns the first {@code k} passages of the corpus, in store order. It has
 * no retrieval signal, so its recall is the chance level for the corpus. The
 * benchmark compares DocIntel's TF-IDF retriever against this floor to show the
 * TF-IDF ranking is doing real work rather than exploiting a corpus artefact.
 */
public final class FirstKRetriever implements Retriever {

    private final DocumentStore store;

    public FirstKRetriever(DocumentStore store) {
        this.store = store;
    }

    @Override
    public List<ScoredPassage> retrieve(String query, int k) {
        List<Passage> passages = store.passages();
        int limit = Math.min(k, passages.size());
        List<ScoredPassage> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            // Descending synthetic scores just to keep a stable best-first ordering.
            out.add(new ScoredPassage(passages.get(i), (double) (limit - i)));
        }
        return List.copyOf(out);
    }
}
