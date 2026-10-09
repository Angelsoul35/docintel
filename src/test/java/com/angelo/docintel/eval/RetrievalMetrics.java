package com.angelo.docintel.eval;

import java.util.List;

/**
 * Document-level retrieval metrics for a ranked list of retrieved passages.
 *
 * <p>A ranking is the ordered list of <em>document ids</em> of the retrieved
 * passages, best first (passages from the same document keep their positions).
 * The gold label is the single document a question's answer was written against.
 *
 * <ul>
 *   <li><b>recall@k (hit@k)</b> — 1 when the gold document appears among the first
 *       {@code k} entries of the ranking.</li>
 *   <li><b>reciprocal rank</b> — {@code 1 / rank} of the first entry equal to the
 *       gold document (1-based), or 0 when the gold document is absent from the
 *       ranking. Averaged over questions this is MRR.</li>
 * </ul>
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /** 1.0 when {@code goldDocId} is among the first {@code k} ranked documents. */
    public static double recallAt(List<String> rankedDocIds, String goldDocId, int k) {
        int limit = Math.min(k, rankedDocIds.size());
        for (int i = 0; i < limit; i++) {
            if (rankedDocIds.get(i).equals(goldDocId)) {
                return 1.0;
            }
        }
        return 0.0;
    }

    /** Reciprocal rank of the first ranked passage from {@code goldDocId}; 0 when absent. */
    public static double reciprocalRank(List<String> rankedDocIds, String goldDocId) {
        for (int i = 0; i < rankedDocIds.size(); i++) {
            if (rankedDocIds.get(i).equals(goldDocId)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }
}
