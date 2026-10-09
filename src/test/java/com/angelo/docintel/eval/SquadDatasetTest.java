package com.angelo.docintel.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.corpus.Document;
import com.angelo.docintel.corpus.DocumentStore;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Confirms the vendored SQuAD snapshot loads with the expected shape and that every
 * question references a document that is actually in the corpus. If the snapshot is
 * truncated, re-numbered or mis-linked, these assertions fail.
 */
class SquadDatasetTest {

    @Test
    void loadsTheExpectedCorpusAndGoldShape() {
        SquadDataset data = SquadDataset.load();

        assertThat(data.documents()).hasSize(152);
        assertThat(data.questions()).hasSize(1200);

        // The first document and the first question are known fixed points of the slice.
        Document first = data.documents().get(0);
        assertThat(first.id()).isEqualTo("d000");
        assertThat(first.title()).isEqualTo("Super_Bowl_50");
        assertThat(first.text()).contains("Super Bowl 50 was an American football game");

        GoldQuestion firstQuestion = data.questions().get(0);
        assertThat(firstQuestion.id()).isEqualTo("56be4db0acb8001400a502ec");
        assertThat(firstQuestion.docId()).isEqualTo("d000");
        assertThat(firstQuestion.answers()).contains("Denver Broncos");
    }

    @Test
    void everyQuestionPointsAtARealDocumentAndHasAtLeastOneGoldAnswer() {
        SquadDataset data = SquadDataset.load();
        Set<String> docIds = new HashSet<>();
        for (Document d : data.documents()) {
            docIds.add(d.id());
            assertThat(d.text()).isNotBlank();
        }

        for (GoldQuestion q : data.questions()) {
            assertThat(docIds).contains(q.docId());
            assertThat(q.question()).isNotBlank();
            assertThat(q.answers()).isNotEmpty();
            assertThat(q.answers()).allSatisfy(a -> assertThat(a).isNotBlank());
        }
    }

    @Test
    void toStoreContainsOnlyTheSnapshotDocumentsSplitIntoPassages() {
        SquadDataset data = SquadDataset.load();
        DocumentStore store = data.toStore();

        // A directly-constructed store does not seed the built-in support corpus.
        assertThat(store.documents()).hasSize(152);
        // Chunking produced strictly more passages than documents (paragraphs split up).
        assertThat(store.passages().size()).isGreaterThan(152);
        assertThat(store.passageCount("d000")).isGreaterThanOrEqualTo(1);
    }
}
