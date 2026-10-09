package com.angelo.docintel.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The official SQuAD v1.1 answer metrics, reimplemented so the benchmark can score
 * DocIntel's answers against the gold spans.
 *
 * <p>Both metrics compare normalised text: lower-cased, with punctuation removed,
 * the articles {@code a}/{@code an}/{@code the} dropped, and whitespace collapsed —
 * exactly the normalisation used by the reference SQuAD evaluation script. When a
 * question has several gold answers, the score is the maximum over them.
 *
 * <ul>
 *   <li><b>Exact match</b> — 1 when the normalised prediction equals a normalised gold.</li>
 *   <li><b>Token F1</b> — {@code 2·precision·recall / (precision + recall)} over the
 *       multiset of shared tokens, where precision is against the prediction's tokens
 *       and recall against the gold's.</li>
 *   <li><b>Containment</b> — 1 when a normalised gold answer is a substring of the
 *       normalised prediction. This is the metric that matches a sentence-extractive
 *       answerer: it asks whether the surfaced text actually carries the answer.</li>
 * </ul>
 */
public final class SquadMetrics {

    private SquadMetrics() {
    }

    /** The 32 ASCII punctuation characters removed by the reference SQuAD script. */
    private static final String ASCII_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    /**
     * SQuAD normalisation, matching the reference evaluation script: lower-case,
     * delete ASCII punctuation (so {@code Levi's} becomes {@code levis}), drop the
     * articles {@code a}/{@code an}/{@code the}, and collapse whitespace.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        StringBuilder noPunct = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            // Punctuation is deleted (not replaced), joining letters across it; every
            // non-ASCII-punctuation character (letters, digits, unicode dashes) is kept.
            if (ASCII_PUNCTUATION.indexOf(c) < 0) {
                noPunct.append(c);
            }
        }
        StringBuilder out = new StringBuilder(noPunct.length());
        for (String token : noPunct.toString().split("\\s+")) {
            if (token.isEmpty() || token.equals("a") || token.equals("an") || token.equals("the")) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(token);
        }
        return out.toString();
    }

    /** 1.0 when the prediction exactly matches any gold answer after normalisation. */
    public static double exactMatch(String prediction, List<String> golds) {
        String p = normalize(prediction);
        for (String gold : golds) {
            if (p.equals(normalize(gold))) {
                return 1.0;
            }
        }
        return 0.0;
    }

    /** The best token-F1 of the prediction against any gold answer, in {@code [0, 1]}. */
    public static double tokenF1(String prediction, List<String> golds) {
        String[] predTokens = tokens(prediction);
        double best = 0.0;
        for (String gold : golds) {
            best = Math.max(best, f1(predTokens, tokens(gold)));
        }
        return best;
    }

    /** 1.0 when any normalised gold answer is a substring of the normalised prediction. */
    public static double contains(String prediction, List<String> golds) {
        String p = normalize(prediction);
        for (String gold : golds) {
            String g = normalize(gold);
            if (!g.isEmpty() && p.contains(g)) {
                return 1.0;
            }
        }
        return 0.0;
    }

    private static double f1(String[] predTokens, String[] goldTokens) {
        if (predTokens.length == 0 && goldTokens.length == 0) {
            return 1.0;
        }
        if (predTokens.length == 0 || goldTokens.length == 0) {
            return 0.0;
        }
        Map<String, Integer> gold = new HashMap<>();
        for (String t : goldTokens) {
            gold.merge(t, 1, Integer::sum);
        }
        int shared = 0;
        Map<String, Integer> remaining = new HashMap<>(gold);
        for (String t : predTokens) {
            Integer left = remaining.get(t);
            if (left != null && left > 0) {
                shared++;
                remaining.put(t, left - 1);
            }
        }
        if (shared == 0) {
            return 0.0;
        }
        double precision = (double) shared / predTokens.length;
        double recall = (double) shared / goldTokens.length;
        return 2.0 * precision * recall / (precision + recall);
    }

    private static String[] tokens(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return normalized.split(" ");
    }
}
