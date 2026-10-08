package com.angelo.docintel.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.corpus.Document;
import com.angelo.docintel.corpus.DocumentStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class TfIdfRetrieverTest {

    @Test
    void retrievesOverTheStoreAndRebuildsWhenTheCorpusChanges() {
        DocumentStore store = new DocumentStore();
        store.add(new Document("a", "Retention",
                "Uploaded documents are retained for 90 days by default."));
        store.add(new Document("b", "Rate limits",
                "The REST API allows 60 requests per minute for each API key."));
        TfIdfRetriever retriever = new TfIdfRetriever(store);

        List<ScoredPassage> before = retriever.retrieve("how long are documents retained", 3);
        assertThat(before.get(0).passage().documentId()).isEqualTo("a");

        // Add a new, highly specific document and confirm the index picks it up.
        store.add(new Document("c", "Webhooks",
                "Webhook deliveries are retried with exponential backoff for up to 24 hours."));
        List<ScoredPassage> after = retriever.retrieve("how are webhook deliveries retried", 3);
        assertThat(after.get(0).passage().documentId()).isEqualTo("c");
    }
}
