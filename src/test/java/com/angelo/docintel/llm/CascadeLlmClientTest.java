package com.angelo.docintel.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.corpus.Passage;
import com.angelo.docintel.retrieval.ScoredPassage;
import com.angelo.docintel.support.FailingLlmClient;
import com.angelo.docintel.support.FakeLlmClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class CascadeLlmClientTest {

    private static final GroundedRequest REQUEST = new GroundedRequest(
            "how long are documents retained",
            List.of(new ScoredPassage(new Passage("retention#0", "doc-retention", "Data retention",
                    0, "Uploaded documents are retained for 90 days by default."), 0.42)));

    @Test
    void usesTheOfflineFallbackWhenNoLiveProviderIsConfigured() {
        CascadeLlmClient cascade = new CascadeLlmClient(List.of(new OfflineLlmClient()));

        LlmResult result = cascade.answer(REQUEST);

        assertThat(result.source()).isEqualTo(AnswerSource.EXTRACTIVE);
        assertThat(result.provider()).isEqualTo("offline");
        assertThat(result.text()).contains("90 days");
    }

    @Test
    void fallsBackToOfflineWhenAnAvailableProviderFails() {
        FailingLlmClient failing = new FailingLlmClient("anthropic");
        CascadeLlmClient cascade = new CascadeLlmClient(List.of(failing, new OfflineLlmClient()));

        LlmResult result = cascade.answer(REQUEST);

        assertThat(failing.calls()).isEqualTo(1);
        assertThat(result.source()).isEqualTo(AnswerSource.EXTRACTIVE);
        assertThat(result.provider()).isEqualTo("offline");
    }

    @Test
    void prefersTheFirstAvailableProviderAndStops() {
        FakeLlmClient fake = new FakeLlmClient("anthropic", "Documents are kept for 90 days. [1]", true);
        OfflineLlmClient offline = new OfflineLlmClient();
        CascadeLlmClient cascade = new CascadeLlmClient(List.of(fake, offline));

        LlmResult result = cascade.answer(REQUEST);

        assertThat(fake.calls()).isEqualTo(1);
        assertThat(result.source()).isEqualTo(AnswerSource.LLM);
        assertThat(result.provider()).isEqualTo("anthropic");
    }

    @Test
    void skipsUnavailableProvidersWithoutCallingThem() {
        FakeLlmClient unavailable = new FakeLlmClient("anthropic", "should never run", false);
        CascadeLlmClient cascade = new CascadeLlmClient(List.of(unavailable, new OfflineLlmClient()));

        LlmResult result = cascade.answer(REQUEST);

        assertThat(unavailable.calls()).isZero();
        assertThat(result.source()).isEqualTo(AnswerSource.EXTRACTIVE);
    }

    @Test
    void reportsItsProviderOrder() {
        CascadeLlmClient cascade = new CascadeLlmClient(
                List.of(new FakeLlmClient("anthropic", "x", true), new OfflineLlmClient()));

        assertThat(cascade.providerOrder()).containsExactly("anthropic", "offline");
    }
}
