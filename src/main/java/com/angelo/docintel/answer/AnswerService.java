package com.angelo.docintel.answer;

import com.angelo.docintel.config.RetrievalProperties;
import com.angelo.docintel.llm.GroundedRequest;
import com.angelo.docintel.llm.LlmClient;
import com.angelo.docintel.llm.LlmResult;
import com.angelo.docintel.retrieval.Retriever;
import com.angelo.docintel.retrieval.ScoredPassage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates ground-then-answer:
 * <ol>
 *   <li>retrieve the top-k passages with TF-IDF;</li>
 *   <li>apply the grounding floor — if the best passage is below the configured
 *       similarity floor, refuse and escalate rather than answer ungrounded;</li>
 *   <li>otherwise ask the LLM cascade to phrase an answer from exactly those
 *       passages (falling back to the extractive offline answerer), and</li>
 *   <li>always attach the retrieved passages as citations.</li>
 * </ol>
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    static final String ESCALATION_MESSAGE =
            "I don't have that information in the current document corpus. "
                    + "Escalating to a human instead of answering without grounding.";

    private final Retriever retriever;
    private final LlmClient llmClient;
    private final RetrievalProperties retrieval;

    public AnswerService(Retriever retriever, LlmClient llmClient, RetrievalProperties retrieval) {
        this.retriever = retriever;
        this.llmClient = llmClient;
        this.retrieval = retrieval;
    }

    public Answer ask(String question) {
        if (question == null || question.isBlank()) {
            return Answer.escalated(ESCALATION_MESSAGE);
        }

        List<ScoredPassage> context = retriever.retrieve(question, retrieval.topK());
        double topScore = context.isEmpty() ? 0.0 : context.get(0).score();

        // Grounding guard: nothing is confident enough, so do not answer ungrounded.
        if (context.isEmpty() || topScore < retrieval.similarityFloor()) {
            log.info("question '{}' fell below the grounding floor ({} < {}); escalating",
                    question, round(topScore), retrieval.similarityFloor());
            return Answer.escalated(ESCALATION_MESSAGE);
        }

        LlmResult result = llmClient.answer(new GroundedRequest(question, context));
        List<Citation> citations = context.stream().map(Citation::from).toList();
        log.info("answered '{}' via {} with {} citation(s), confidence {}",
                question, result.source().jsonValue(), citations.size(), round(topScore));
        return new Answer(result.text(), result.source(), citations, round(topScore), false);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
