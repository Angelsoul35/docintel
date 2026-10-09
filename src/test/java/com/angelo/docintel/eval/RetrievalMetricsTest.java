package com.angelo.docintel.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The retrieval metrics on tiny, hand-checked rankings. Each case pins a value worked
 * out by hand so a broken implementation fails.
 */
class RetrievalMetricsTest {

    // Gold document is "d0". It sits at rank 2 (1-based) in this ranking.
    private final List<String> ranking = List.of("d2", "d0", "d1", "d0");

    @Test
    void recallAtKIsOneOnlyOnceTheGoldDocumentIsWithinTheTopK() {
        assertThat(RetrievalMetrics.recallAt(ranking, "d0", 1)).isEqualTo(0.0); // d2 at rank 1
        assertThat(RetrievalMetrics.recallAt(ranking, "d0", 2)).isEqualTo(1.0); // d0 enters at rank 2
        assertThat(RetrievalMetrics.recallAt(ranking, "d0", 3)).isEqualTo(1.0);
        assertThat(RetrievalMetrics.recallAt(ranking, "d2", 1)).isEqualTo(1.0); // d2 at rank 1
        assertThat(RetrievalMetrics.recallAt(ranking, "d9", 4)).isEqualTo(0.0); // absent
    }

    @Test
    void recallAtKIsCappedByTheRankingLength() {
        assertThat(RetrievalMetrics.recallAt(List.of("d1"), "d1", 5)).isEqualTo(1.0);
        assertThat(RetrievalMetrics.recallAt(List.of("d1"), "d0", 5)).isEqualTo(0.0);
    }

    @Test
    void reciprocalRankUsesTheFirstOccurrenceOfTheGoldDocument() {
        // d0 first appears at rank 2 -> 1/2, even though it appears again at rank 4.
        assertThat(RetrievalMetrics.reciprocalRank(ranking, "d0")).isCloseTo(0.5, within(1e-9));
        assertThat(RetrievalMetrics.reciprocalRank(ranking, "d2")).isCloseTo(1.0, within(1e-9));
        assertThat(RetrievalMetrics.reciprocalRank(ranking, "d1")).isCloseTo(1.0 / 3.0, within(1e-9));
        // Absent from the ranking -> 0 (not an infinite reciprocal).
        assertThat(RetrievalMetrics.reciprocalRank(ranking, "d9")).isEqualTo(0.0);
    }
}
