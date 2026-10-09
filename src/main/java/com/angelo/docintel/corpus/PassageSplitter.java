package com.angelo.docintel.corpus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a document into sentence-level passages. Very short fragments are merged
 * forward so that every passage carries enough signal to be retrieved on its own.
 *
 * <p>Chunking is at sentence granularity with a minimum length. The same {@link Passage}
 * model supports richer strategies — overlapping windows, token budgets, structure-aware
 * splitting — behind this boundary; see the "Extending the engine" section of the README.
 */
public final class PassageSplitter {

    /** Split on a sentence terminator followed by whitespace, keeping the terminator. */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    /** Passages shorter than this many characters are merged into the next one. */
    private static final int MIN_PASSAGE_CHARS = 40;

    private PassageSplitter() {
    }

    /**
     * Split {@code document.text()} into passages.
     *
     * @return an ordered, non-empty list of passages (a blank document yields no passages)
     */
    public static List<Passage> split(Document document) {
        List<String> chunks = sentences(document.text());
        List<Passage> passages = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        int ordinal = 0;

        for (String chunk : chunks) {
            if (pending.length() > 0) {
                pending.append(' ');
            }
            pending.append(chunk);
            if (pending.length() >= MIN_PASSAGE_CHARS) {
                passages.add(passage(document, ordinal++, pending.toString()));
                pending.setLength(0);
            }
        }
        if (pending.length() > 0) {
            if (passages.isEmpty()) {
                passages.add(passage(document, ordinal, pending.toString()));
            } else {
                // Append the trailing remainder to the last full passage.
                Passage last = passages.remove(passages.size() - 1);
                passages.add(new Passage(last.id(), last.documentId(), last.documentTitle(),
                        last.ordinal(), last.text() + " " + pending.toString().trim()));
            }
        }
        return passages;
    }

    private static Passage passage(Document document, int ordinal, String text) {
        return new Passage(
                document.id() + "#" + ordinal,
                document.id(),
                document.title(),
                ordinal,
                text.trim());
    }

    private static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text.trim());
        int start = 0;
        while (matcher.find()) {
            String sentence = text.trim().substring(start, matcher.start()).trim();
            if (!sentence.isEmpty()) {
                out.add(sentence);
            }
            start = matcher.end();
        }
        String tail = text.trim().substring(start).trim();
        if (!tail.isEmpty()) {
            out.add(tail);
        }
        return out;
    }
}
