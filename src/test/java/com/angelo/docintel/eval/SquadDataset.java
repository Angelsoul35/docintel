package com.angelo.docintel.eval;

import com.angelo.docintel.corpus.Document;
import com.angelo.docintel.corpus.DocumentStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the vendored SQuAD v1.1 evaluation snapshot from the test classpath
 * ({@code /squad/corpus.jsonl} and {@code /squad/questions.jsonl}) into DocIntel's
 * own {@link Document} corpus plus a gold {@link GoldQuestion} set.
 *
 * <p>The snapshot's provenance, license and regeneration are documented in
 * {@code src/test/resources/squad/README.md}.
 */
public final class SquadDataset {

    private static final String CORPUS = "/squad/corpus.jsonl";
    private static final String QUESTIONS = "/squad/questions.jsonl";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Document> documents;
    private final List<GoldQuestion> questions;

    private SquadDataset(List<Document> documents, List<GoldQuestion> questions) {
        this.documents = List.copyOf(documents);
        this.questions = List.copyOf(questions);
    }

    /** Read the vendored snapshot from the classpath. */
    public static SquadDataset load() {
        List<Document> docs = new ArrayList<>();
        for (JsonNode node : readLines(CORPUS)) {
            docs.add(new Document(
                    node.get("id").asText(),
                    node.get("title").asText(),
                    node.get("text").asText()));
        }
        List<GoldQuestion> questions = new ArrayList<>();
        for (JsonNode node : readLines(QUESTIONS)) {
            List<String> answers = new ArrayList<>();
            node.get("answers").forEach(a -> answers.add(a.asText()));
            questions.add(new GoldQuestion(
                    node.get("id").asText(),
                    node.get("question").asText(),
                    node.get("docId").asText(),
                    answers));
        }
        return new SquadDataset(docs, questions);
    }

    public List<Document> documents() {
        return documents;
    }

    public List<GoldQuestion> questions() {
        return questions;
    }

    /**
     * Build a {@link DocumentStore} holding exactly this snapshot's documents.
     * A directly-constructed store does not run its seed corpus, so the store
     * contains only the SQuAD paragraphs the benchmark indexes.
     */
    public DocumentStore toStore() {
        DocumentStore store = new DocumentStore();
        for (Document document : documents) {
            store.add(document);
        }
        return store;
    }

    private static List<JsonNode> readLines(String resource) {
        List<JsonNode> out = new ArrayList<>();
        try (InputStream in = SquadDataset.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing classpath resource " + resource
                        + " (is the SQuAD snapshot vendored under src/test/resources/squad?)");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        out.add(MAPPER.readTree(line));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + resource, e);
        }
        return out;
    }
}
