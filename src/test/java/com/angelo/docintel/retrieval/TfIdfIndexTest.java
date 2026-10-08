package com.angelo.docintel.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.corpus.Passage;
import java.util.List;
import org.junit.jupiter.api.Test;

class TfIdfIndexTest {

    private static Passage passage(String id, String title, String text) {
        return new Passage(id, id, title, 0, text);
    }

    private final List<Passage> corpus = List.of(
            passage("retention", "Data retention",
                    "Uploaded documents are retained for 90 days by default."),
            passage("ratelimit", "API rate limits",
                    "The REST API allows 60 requests per minute for each API key by default."),
            passage("billing", "Billing and invoices",
                    "Invoices are issued on the first day of each month and charged to the card on file."),
            passage("password", "Resetting your password",
                    "We email a password reset link that stays valid for 30 minutes."));

    @Test
    void returnsTheRightPassageForAQuery() {
        TfIdfIndex index = TfIdfIndex.build(corpus);

        List<ScoredPassage> hits = index.query("how long are uploaded documents retained", 3);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).passage().id()).isEqualTo("retention");
        assertThat(hits.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void ranksTheMostRelevantPassageFirst() {
        TfIdfIndex index = TfIdfIndex.build(corpus);

        List<ScoredPassage> hits = index.query("how many API requests per minute are allowed", 4);

        assertThat(hits.get(0).passage().id()).isEqualTo("ratelimit");
        // Scores are sorted strictly non-increasing.
        for (int i = 1; i < hits.size(); i++) {
            assertThat(hits.get(i - 1).score()).isGreaterThanOrEqualTo(hits.get(i).score());
        }
    }

    @Test
    void rareTermsOutweighCommonOnesViaIdf() {
        TfIdfIndex index = TfIdfIndex.build(corpus);

        // "default" appears in two passages, "invoices" in one; the invoices passage must win.
        List<ScoredPassage> hits = index.query("invoices default", 4);

        assertThat(hits.get(0).passage().id()).isEqualTo("billing");
    }

    @Test
    void returnsNothingForAnOutOfVocabularyQuery() {
        TfIdfIndex index = TfIdfIndex.build(corpus);

        List<ScoredPassage> hits = index.query("sourdough bread baking", 3);

        assertThat(hits).isEmpty();
    }

    @Test
    void respectsTheTopKLimit() {
        TfIdfIndex index = TfIdfIndex.build(corpus);

        List<ScoredPassage> hits = index.query("documents api invoices password", 2);

        assertThat(hits).hasSizeLessThanOrEqualTo(2);
    }
}
