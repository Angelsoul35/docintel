package com.angelo.docintel.retrieval;

import com.angelo.docintel.corpus.Passage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable TF-IDF index over a fixed set of passages, with cosine-similarity
 * ranking. This is the deterministic heart of the service and carries no randomness
 * and no external calls.
 *
 * <p>Weighting scheme:
 * <ul>
 *   <li>term frequency {@code tf(t, p)} is the raw count of {@code t} in passage {@code p};</li>
 *   <li>inverse document frequency uses the smoothed form
 *       {@code idf(t) = ln((N + 1) / (df(t) + 1)) + 1}, which keeps weights positive
 *       and avoids division by zero for unseen terms;</li>
 *   <li>a passage vector is {@code tf * idf} per term;</li>
 *   <li>relevance is the cosine of the angle between the query and passage vectors.</li>
 * </ul>
 */
public final class TfIdfIndex {

    private final List<Passage> passages;
    private final Map<String, Double> idf;
    private final List<Map<String, Double>> vectors;
    private final double[] norms;

    private TfIdfIndex(List<Passage> passages, Map<String, Double> idf,
                       List<Map<String, Double>> vectors, double[] norms) {
        this.passages = passages;
        this.idf = idf;
        this.vectors = vectors;
        this.norms = norms;
    }

    /** Build an index over {@code passages}. */
    public static TfIdfIndex build(List<Passage> passages) {
        List<Passage> snapshot = List.copyOf(passages);
        int n = snapshot.size();

        // Document frequency: in how many passages does each term appear at least once.
        Map<String, Integer> documentFrequency = new HashMap<>();
        List<Map<String, Integer>> termCounts = new ArrayList<>(n);
        for (Passage passage : snapshot) {
            Map<String, Integer> counts = countTerms(Tokenizer.tokenize(passage.text()));
            termCounts.add(counts);
            for (String term : counts.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        // Smoothed inverse document frequency.
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            double value = Math.log((double) (n + 1) / (entry.getValue() + 1)) + 1.0;
            idf.put(entry.getKey(), value);
        }

        // Per-passage TF-IDF vectors and their L2 norms.
        List<Map<String, Double>> vectors = new ArrayList<>(n);
        double[] norms = new double[n];
        for (int i = 0; i < n; i++) {
            Map<String, Double> vector = weight(termCounts.get(i), idf);
            vectors.add(vector);
            norms[i] = norm(vector);
        }

        return new TfIdfIndex(snapshot, idf, vectors, norms);
    }

    /**
     * Rank passages against {@code query} and return up to {@code k}, best first.
     * Passages that share no terms with the query (score {@code 0}) are dropped.
     */
    public List<ScoredPassage> query(String query, int k) {
        Map<String, Double> queryVector = weight(countTerms(Tokenizer.tokenize(query)), idf);
        double queryNorm = norm(queryVector);

        List<ScoredPassage> scored = new ArrayList<>();
        if (queryNorm > 0.0) {
            for (int i = 0; i < passages.size(); i++) {
                double score = cosine(queryVector, queryNorm, vectors.get(i), norms[i]);
                if (score > 0.0) {
                    scored.add(new ScoredPassage(passages.get(i), score));
                }
            }
        }
        // Highest score first; ties broken by passage id for deterministic output.
        scored.sort(Comparator.comparingDouble(ScoredPassage::score).reversed()
                .thenComparing(sp -> sp.passage().id()));
        return List.copyOf(scored.subList(0, Math.min(k, scored.size())));
    }

    public int size() {
        return passages.size();
    }

    private static Map<String, Integer> countTerms(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }

    private static Map<String, Double> weight(Map<String, Integer> counts, Map<String, Double> idf) {
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            // Unseen query terms get the maximum smoothed idf so they never silently vanish.
            double termIdf = idf.getOrDefault(entry.getKey(), defaultIdf(idf));
            vector.put(entry.getKey(), entry.getValue() * termIdf);
        }
        return vector;
    }

    private static double defaultIdf(Map<String, Double> idf) {
        // For a term never seen in the corpus, use a weight at least as large as any known
        // idf. It still contributes 0 to the dot product (no passage contains it), but it
        // correctly lowers the cosine of a query that is mostly out-of-vocabulary.
        double max = 1.0;
        for (double value : idf.values()) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static double norm(Map<String, Double> vector) {
        double sum = 0.0;
        for (double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private static double cosine(Map<String, Double> a, double normA, Map<String, Double> b, double normB) {
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        // Iterate over the smaller vector for the dot product.
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : small.entrySet()) {
            Double other = large.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        return dot / (normA * normB);
    }
}
