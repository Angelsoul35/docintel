package com.angelo.docintel.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The SQuAD answer metrics on tiny, hand-checked inputs. Every assertion pins a value
 * computed by hand, so a wrong metric implementation fails rather than passes.
 */
class SquadMetricsTest {

    @Test
    void normalizeLowercasesDropsPunctuationAndArticles() {
        // "The" is an article; "!" is punctuation.
        assertThat(SquadMetrics.normalize("The Denver Broncos!")).isEqualTo("denver broncos");
        // Punctuation is deleted (not spaced), so the apostrophe joins the letters.
        assertThat(SquadMetrics.normalize("Levi's Stadium")).isEqualTo("levis stadium");
        // A string of only articles and punctuation normalises to empty.
        assertThat(SquadMetrics.normalize("A, an. The!")).isEmpty();
        assertThat(SquadMetrics.normalize(null)).isEmpty();
    }

    @Test
    void exactMatchIgnoresArticlesAndPunctuationButNotContent() {
        assertThat(SquadMetrics.exactMatch("the Denver Broncos", List.of("Denver Broncos"))).isEqualTo(1.0);
        assertThat(SquadMetrics.exactMatch("Denver Broncos.", List.of("denver broncos"))).isEqualTo(1.0);
        // A missing content word is not an exact match.
        assertThat(SquadMetrics.exactMatch("Broncos", List.of("Denver Broncos"))).isEqualTo(0.0);
        // Max over several golds: matching the second gold still scores 1.
        assertThat(SquadMetrics.exactMatch("Panthers", List.of("Broncos", "the Panthers"))).isEqualTo(1.0);
    }

    @Test
    void tokenF1IsTheHarmonicMeanOfTokenPrecisionAndRecall() {
        // pred -> {brown, fox} (article "the" dropped); gold -> {quick, brown, fox}.
        // shared = 2, precision = 2/2 = 1.0, recall = 2/3, F1 = 2*1*(2/3)/(1+2/3) = 0.8.
        assertThat(SquadMetrics.tokenF1("the brown fox", List.of("quick brown fox")))
                .isCloseTo(0.8, within(1e-9));
        // No shared tokens -> F1 = 0.
        assertThat(SquadMetrics.tokenF1("red cat", List.of("quick brown fox")))
                .isCloseTo(0.0, within(1e-9));
        // Identical after normalisation -> F1 = 1.
        assertThat(SquadMetrics.tokenF1("Denver Broncos", List.of("the denver broncos")))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void tokenF1CountsSharedTokensAsAMultisetNotASet() {
        // pred tokens: [na, na, na]; gold tokens: [na]. shared is capped at 1 (multiset),
        // precision = 1/3, recall = 1/1, F1 = 2*(1/3)*1/((1/3)+1) = 0.5. A set-based (buggy)
        // implementation would wrongly score precision = 1 and F1 = 1.
        assertThat(SquadMetrics.tokenF1("na na na", List.of("na")))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void containmentDetectsTheGoldAnswerAsASubstring() {
        // A full sentence that contains the gold span -> contained.
        assertThat(SquadMetrics.contains(
                "The AFC champion Denver Broncos won the game.", List.of("Denver Broncos")))
                .isEqualTo(1.0);
        // Gold present but split across non-adjacent words -> not contained.
        assertThat(SquadMetrics.contains(
                "Denver beat the Carolina Panthers, not the Broncos alone.", List.of("Denver Broncos")))
                .isEqualTo(0.0);
        // Partial token overlap without the full span -> not contained.
        assertThat(SquadMetrics.contains("the Broncos", List.of("Denver Broncos"))).isEqualTo(0.0);
    }
}
