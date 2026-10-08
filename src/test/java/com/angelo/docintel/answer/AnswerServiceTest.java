package com.angelo.docintel.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.config.RetrievalProperties;
import com.angelo.docintel.corpus.Document;
import com.angelo.docintel.corpus.DocumentStore;
import com.angelo.docintel.llm.AnswerSource;
import com.angelo.docintel.llm.CascadeLlmClient;
import com.angelo.docintel.llm.LlmClient;
import com.angelo.docintel.llm.OfflineLlmClient;
import com.angelo.docintel.retrieval.TfIdfRetriever;
import com.angelo.docintel.support.FakeLlmClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnswerServiceTest {

    private DocumentStore store;
    private TfIdfRetriever retriever;
    private final RetrievalProperties retrieval = new RetrievalProperties(3, 0.05);

    @BeforeEach
    void setUp() {
        store = new DocumentStore();
        store.add(new Document("doc-retention", "Data retention",
                "Uploaded documents are retained for 90 days by default. "
                        + "Enterprise workspaces can configure retention between 30 and 365 days."));
        store.add(new Document("doc-ratelimit", "API rate limits",
                "The REST API allows 60 requests per minute for each API key by default. "
                        + "Exceeding the limit returns HTTP 429 with a Retry-After header."));
        store.add(new Document("doc-billing", "Billing and invoices",
                "Invoices are issued on the first day of each month and charged to the card on file."));
        retriever = new TfIdfRetriever(store);
    }

    private AnswerService serviceWith(LlmClient llm) {
        return new AnswerService(retriever, llm, retrieval);
    }

    private AnswerService offlineService() {
        return serviceWith(new CascadeLlmClient(List.of(new OfflineLlmClient())));
    }

    @Test
    void offlineAnswerIsGroundedInRetrievedPassagesAndAlwaysCites() {
        Answer answer = offlineService().ask("how long are uploaded documents retained");

        assertThat(answer.escalated()).isFalse();
        assertThat(answer.source()).isEqualTo(AnswerSource.EXTRACTIVE);
        assertThat(answer.citations()).isNotEmpty();
        // The top citation is the retention document.
        assertThat(answer.citations().get(0).documentId()).isEqualTo("doc-retention");
        // The answer text is drawn verbatim from a cited passage: it is grounded.
        assertThat(answer.citations())
                .anyMatch(c -> c.snippet().contains(answer.answer()));
        assertThat(answer.answer()).contains("90 days");
        assertThat(answer.confidence()).isGreaterThan(0.0);
    }

    @Test
    void usesTheLiveLlmWhenOneIsAvailableButStillCitesRetrievedPassages() {
        FakeLlmClient fake = new FakeLlmClient("anthropic",
                "Uploaded documents are kept for 90 days by default. [1]", true);
        AnswerService service = serviceWith(new CascadeLlmClient(List.of(fake, new OfflineLlmClient())));

        Answer answer = service.ask("how long are uploaded documents retained");

        assertThat(fake.calls()).isEqualTo(1);
        assertThat(answer.source()).isEqualTo(AnswerSource.LLM);
        assertThat(answer.citations()).isNotEmpty();
        assertThat(answer.citations().get(0).documentId()).isEqualTo("doc-retention");
    }

    @Test
    void outOfCorpusQuestionHitsTheGroundingFloorAndEscalates() {
        Answer answer = offlineService().ask("how do I bake sourdough bread at home");

        assertThat(answer.escalated()).isTrue();
        assertThat(answer.source()).isEqualTo(AnswerSource.NONE);
        assertThat(answer.citations()).isEmpty();
        assertThat(answer.confidence()).isEqualTo(0.0);
        assertThat(answer.answer()).contains("don't have that information");
    }

    @Test
    void blankQuestionEscalates() {
        Answer answer = offlineService().ask("   ");

        assertThat(answer.escalated()).isTrue();
        assertThat(answer.source()).isEqualTo(AnswerSource.NONE);
    }

    @Test
    void confidenceReflectsTheTopPassageSimilarity() {
        Answer answer = offlineService().ask("how many requests per minute does the API allow");

        assertThat(answer.citations().get(0).documentId()).isEqualTo("doc-ratelimit");
        assertThat(answer.confidence()).isEqualTo(answer.citations().get(0).score());
    }
}
