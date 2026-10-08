package com.angelo.docintel.retrieval;

import com.angelo.docintel.corpus.DocumentStore;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * A {@link Retriever} backed by an in-process {@link TfIdfIndex} over the
 * {@link DocumentStore}. The index is rebuilt lazily whenever the store changes,
 * detected through its {@link DocumentStore#version() version}, so answers always
 * reflect the current corpus (including documents added at runtime).
 */
@Component
public class TfIdfRetriever implements Retriever {

    private final DocumentStore store;

    private long indexedVersion = -1;
    private TfIdfIndex index;

    public TfIdfRetriever(DocumentStore store) {
        this.store = store;
    }

    @Override
    public synchronized List<ScoredPassage> retrieve(String query, int k) {
        long current = store.version();
        if (index == null || current != indexedVersion) {
            index = TfIdfIndex.build(store.passages());
            indexedVersion = current;
        }
        return index.query(query, k);
    }
}
