package com.angelo.docintel.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.angelo.docintel.corpus.Passage;
import com.angelo.docintel.retrieval.ScoredPassage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfflineLlmClientTest {

    private final OfflineLlmClient client = new OfflineLlmClient();

    private static ScoredPassage scored(String text, double score) {
        return new ScoredPassage(new Passage("p", "doc", "Doc", 0, text), score);
    }

    @Test
    void composesAnExtractiveAnswerFromTheTopPassage() {
        String passageText = "Uploaded documents are retained for 90 days by default.";
        GroundedRequest request = new GroundedRequest(
                "how long are documents retained", List.of(scored(passageText, 0.4)));

        LlmResult result = client.answer(request);

        assertThat(result.source()).isEqualTo(AnswerSource.EXTRACTIVE);
        assertThat(result.provider()).isEqualTo("offline");
        // The answer is a verbatim span of the retrieved passage: it cannot drift off-corpus.
        assertThat(passageText).contains(result.text());
        assertThat(result.text()).contains("90 days");
    }

    @Test
    void picksTheSentenceThatOverlapsTheQuestion() {
        String passageText = "Invoices are issued on the first day of each month. "
                + "Annual plans receive a 20 percent discount compared with paying monthly.";
        GroundedRequest request = new GroundedRequest(
                "is there a discount for annual plans", List.of(scored(passageText, 0.3)));

        String extracted = OfflineLlmClient.extract(passageText, request.question());

        assertThat(extracted).isEqualTo(
                "Annual plans receive a 20 percent discount compared with paying monthly.");
    }

    @Test
    void failsWhenThereIsNoContext() {
        GroundedRequest request = new GroundedRequest("anything", List.of());

        assertThatThrownBy(() -> client.answer(request)).isInstanceOf(LlmException.class);
    }
}
