package com.angelo.docintel.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns text into a bag of lower-cased alphanumeric tokens. A small English
 * stop-word list is removed so that common function words do not dominate the
 * TF-IDF weights. The behaviour is fully deterministic.
 */
public final class Tokenizer {

    private static final Pattern NON_TOKEN = Pattern.compile("[^a-z0-9]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for",
            "from", "how", "i", "in", "is", "it", "its", "me", "my", "of", "on", "or",
            "our", "that", "the", "their", "them", "then", "there", "these", "they",
            "this", "to", "was", "we", "what", "when", "where", "which", "who", "why",
            "will", "with", "you", "your");

    private Tokenizer() {
    }

    /**
     * @return the content tokens of {@code text}, lower-cased, with stop-words and
     *         one-character tokens removed. Never {@code null}.
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String raw : NON_TOKEN.split(text.toLowerCase())) {
            if (raw.length() < 2) {
                continue;
            }
            if (STOP_WORDS.contains(raw)) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }
}
