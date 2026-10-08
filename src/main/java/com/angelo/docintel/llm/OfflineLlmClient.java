package com.angelo.docintel.llm;

import com.angelo.docintel.retrieval.ScoredPassage;
import com.angelo.docintel.retrieval.Tokenizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The deterministic offline fallback. It never calls the network: it composes an
 * extractive answer straight from the retrieved passages by selecting, from the
 * best passage, the sentence that overlaps the question most. The returned text is
 * therefore always a verbatim span of a retrieved passage, so the answer cannot
 * drift away from the corpus.
 *
 * <p>This client is always {@link #isAvailable() available}, which is what lets the
 * {@link CascadeLlmClient} guarantee an answer with no API key and no network.
 */
public class OfflineLlmClient implements LlmClient {

    public static final String NAME = "offline";

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResult answer(GroundedRequest request) {
        List<ScoredPassage> context = request.context();
        if (context.isEmpty()) {
            // The caller is responsible for the grounding floor; this is a defensive guard.
            throw new LlmException("offline fallback requires at least one passage");
        }
        String best = extract(context.get(0).passage().text(), request.question());
        return new LlmResult(best, AnswerSource.EXTRACTIVE, NAME);
    }

    /**
     * Pick the sentence of {@code passageText} that shares the most distinct content
     * terms with {@code question}. Falls back to the whole passage when nothing overlaps.
     */
    static String extract(String passageText, String question) {
        Set<String> queryTerms = Set.copyOf(Tokenizer.tokenize(question));
        String[] sentences = SENTENCE_BOUNDARY.split(passageText.trim());

        String best = passageText.trim();
        int bestOverlap = -1;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int overlap = 0;
            for (String term : Set.copyOf(Tokenizer.tokenize(trimmed))) {
                if (queryTerms.contains(term)) {
                    overlap++;
                }
            }
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = trimmed;
            }
        }
        // bestOverlap == 0 means no sentence matched; return the whole passage instead
        // of an arbitrary first sentence.
        return bestOverlap <= 0 ? passageText.trim() : best;
    }
}
