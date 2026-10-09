package com.angelo.docintel.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.angelo.docintel.answer.Answer;
import com.angelo.docintel.answer.AnswerService;
import com.angelo.docintel.config.RetrievalProperties;
import com.angelo.docintel.corpus.DocumentStore;
import com.angelo.docintel.llm.CascadeLlmClient;
import com.angelo.docintel.llm.OfflineLlmClient;
import com.angelo.docintel.retrieval.Retriever;
import com.angelo.docintel.retrieval.ScoredPassage;
import com.angelo.docintel.retrieval.TfIdfRetriever;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Real-inputs benchmark. It ingests the vendored SQuAD v1.1 snapshot through
 * DocIntel's own pipeline — {@link DocumentStore} ingest and chunking, the
 * {@link TfIdfRetriever} index, the {@link AnswerService} grounding guard and the
 * {@link OfflineLlmClient} extractive answerer — and scores it against SQuAD's gold
 * labels with standard information-retrieval and SQuAD answer metrics.
 *
 * <p>The suite proves three things, and fails loudly if any regresses:
 * <ol>
 *   <li><b>The engine works.</b> TF-IDF retrieval finds the gold source paragraph far
 *       above a query-independent baseline, and the extractive answer carries the gold
 *       answer far above a retrieval-ablated baseline.</li>
 *   <li><b>The numbers are reproducible.</b> Each metric is asserted against a reference
 *       value computed independently by a second implementation (the Python reference in
 *       the study guide), within a tight tolerance.</li>
 *   <li><b>The signal is real.</b> A negative control that scrambles the
 *       question&harr;document pairing collapses retrieval back to the chance baseline,
 *       so the high scores can only come from genuine query/document grounding.</li>
 * </ol>
 *
 * <p>Reference values were computed independently over the same snapshot by the Python
 * reimplementation documented in {@code docs/estudio/06-benchmark-inputs-reales.md}.
 */
class SquadBenchmarkTest {

    // Retrieval depth for MRR and the deepest recall@k reported.
    private static final int DEPTH = 10;
    private static final double FLOOR = 0.05;

    // --- Independently-computed reference values (Python reimplementation, same snapshot) ---
    private static final double REF_RECALL_AT_1 = 0.651;
    private static final double REF_RECALL_AT_3 = 0.822;
    private static final double REF_RECALL_AT_5 = 0.869;
    private static final double REF_MRR = 0.747;
    private static final double REF_BASELINE_RECALL_AT_1 = 0.025;
    private static final double REF_ANSWER_CONTAINMENT = 0.608;
    private static final double REF_ANSWER_CONTAINMENT_ABLATED = 0.021;
    private static final double REF_ANSWER_TOKEN_F1 = 0.110;
    // Tolerance for the regression lock. The two implementations agree to four decimals,
    // so half a percentage point is a crisp pin that still absorbs reference rounding.
    private static final double TOL = 0.005;

    @Test
    void docIntelBeatsBaselinesOnRealSquadAndTheSignalIsReal() {
        SquadDataset data = SquadDataset.load();
        List<GoldQuestion> questions = data.questions();
        DocumentStore store = data.toStore();

        Retriever engine = new TfIdfRetriever(store);
        Retriever firstK = new FirstKRetriever(store);
        AnswerService engineAnswers = answerService(engine);
        AnswerService ablatedAnswers = answerService(firstK);

        // One retrieval pass with the engine; every retrieval metric reuses these rankings.
        List<List<String>> engineRankings = new ArrayList<>(questions.size());
        for (GoldQuestion q : questions) {
            List<ScoredPassage> hits = engine.retrieve(q.question(), DEPTH);
            List<String> docIds = new ArrayList<>(hits.size());
            for (ScoredPassage sp : hits) {
                docIds.add(sp.passage().documentId());
            }
            engineRankings.add(docIds);
        }

        // Baseline ranking is query-independent: the same first-K passages for every query.
        List<String> baselineRanking = new ArrayList<>();
        for (ScoredPassage sp : firstK.retrieve("", DEPTH)) {
            baselineRanking.add(sp.passage().documentId());
        }

        // Negative control: scramble which document each question is scored against.
        List<String> scrambledGold = scramble(questions);

        Retrieval engineR = new Retrieval();
        Retrieval baselineR = new Retrieval();
        Retrieval negControlR = new Retrieval();
        for (int i = 0; i < questions.size(); i++) {
            String gold = questions.get(i).docId();
            engineR.add(engineRankings.get(i), gold);
            baselineR.add(baselineRanking, gold);
            negControlR.add(engineRankings.get(i), scrambledGold.get(i));
        }

        // Answer quality: full engine pipeline vs a retrieval-ablated pipeline (same answerer).
        Answers engineA = new Answers();
        Answers ablatedA = new Answers();
        for (GoldQuestion q : questions) {
            engineA.add(engineAnswers.ask(q.question()), q.answers());
            ablatedA.add(ablatedAnswers.ask(q.question()), q.answers());
        }

        report(data, engineR, baselineR, negControlR, engineA, ablatedA);

        // 1) The engine reproduces the independently-computed reference values.
        assertThat(engineR.recallAt(1)).isCloseTo(REF_RECALL_AT_1, within(TOL));
        assertThat(engineR.recallAt(3)).isCloseTo(REF_RECALL_AT_3, within(TOL));
        assertThat(engineR.recallAt(5)).isCloseTo(REF_RECALL_AT_5, within(TOL));
        assertThat(engineR.mrr()).isCloseTo(REF_MRR, within(TOL));
        assertThat(baselineR.recallAt(1)).isCloseTo(REF_BASELINE_RECALL_AT_1, within(TOL));
        assertThat(engineA.containment()).isCloseTo(REF_ANSWER_CONTAINMENT, within(TOL));
        assertThat(ablatedA.containment()).isCloseTo(REF_ANSWER_CONTAINMENT_ABLATED, within(TOL));
        assertThat(engineA.tokenF1()).isCloseTo(REF_ANSWER_TOKEN_F1, within(TOL));

        // 2) The engine beats the naive baselines by a wide, unambiguous margin.
        assertThat(engineR.recallAt(1)).isGreaterThan(0.60);
        assertThat(engineR.recallAt(1)).isGreaterThan(10.0 * baselineR.recallAt(1));
        assertThat(engineR.mrr()).isGreaterThan(10.0 * baselineR.mrr());
        assertThat(engineA.containment()).isGreaterThan(10.0 * ablatedA.containment());
        assertThat(engineA.tokenF1()).isGreaterThan(5.0 * ablatedA.tokenF1());

        // 3) The negative control collapses retrieval back to chance: the score is not an artefact.
        assertThat(negControlR.recallAt(1)).isLessThan(0.05);
        assertThat(negControlR.mrr()).isLessThan(0.05);
        assertThat(engineR.recallAt(1)).isGreaterThan(10.0 * negControlR.recallAt(1));
        // Scrambling must destroy the signal, landing near the query-independent baseline.
        assertThat(negControlR.recallAt(1)).isCloseTo(baselineR.recallAt(1), within(0.03));
    }

    private AnswerService answerService(Retriever retriever) {
        return new AnswerService(retriever,
                new CascadeLlmClient(List.of(new OfflineLlmClient())),
                new RetrievalProperties(3, FLOOR));
    }

    /** Scramble the gold document assignment across questions with a fixed seed. */
    private static List<String> scramble(List<GoldQuestion> questions) {
        List<String> gold = new ArrayList<>();
        for (GoldQuestion q : questions) {
            gold.add(q.docId());
        }
        Collections.shuffle(gold, new Random(1234));
        return gold;
    }

    private static void report(SquadDataset data, Retrieval engine, Retrieval baseline,
                               Retrieval negControl, Answers engineA, Answers ablatedA) {
        String line = "+----------------------+-----------+-----------+-----------+";
        System.out.println();
        System.out.println("=== DocIntel real-inputs benchmark: SQuAD v1.1 (first 1200 validation QA pairs) ===");
        System.out.printf("corpus: %d documents, %d passages, %d questions%n",
                data.documents().size(), data.toStore().passages().size(), data.questions().size());
        System.out.println();
        System.out.println("RETRIEVAL  (gold = SQuAD source paragraph; higher is better)");
        System.out.println(line);
        System.out.printf("| %-20s | %9s | %9s | %9s |%n", "metric", "engine", "baseline", "scrambled");
        System.out.println(line);
        System.out.printf("| %-20s | %9.4f | %9.4f | %9.4f |%n", "recall@1",
                engine.recallAt(1), baseline.recallAt(1), negControl.recallAt(1));
        System.out.printf("| %-20s | %9.4f | %9.4f | %9.4f |%n", "recall@3",
                engine.recallAt(3), baseline.recallAt(3), negControl.recallAt(3));
        System.out.printf("| %-20s | %9.4f | %9.4f | %9.4f |%n", "recall@5",
                engine.recallAt(5), baseline.recallAt(5), negControl.recallAt(5));
        System.out.printf("| %-20s | %9.4f | %9.4f | %9.4f |%n", "MRR@" + DEPTH,
                engine.mrr(), baseline.mrr(), negControl.mrr());
        System.out.println(line);
        System.out.println();
        System.out.println("ANSWER  (offline extractive answerer; engine retrieval vs first-passage ablation)");
        System.out.println(line);
        System.out.printf("| %-20s | %9s | %9s | %9s |%n", "metric", "engine", "ablated", "");
        System.out.println(line);
        System.out.printf("| %-20s | %9.4f | %9.4f | %9s |%n", "answer-containment",
                engineA.containment(), ablatedA.containment(), "");
        System.out.printf("| %-20s | %9.4f | %9.4f | %9s |%n", "token-F1",
                engineA.tokenF1(), ablatedA.tokenF1(), "");
        System.out.printf("| %-20s | %9.4f | %9.4f | %9s |%n", "exact-match",
                engineA.exactMatch(), ablatedA.exactMatch(), "");
        System.out.println(line);
        System.out.printf("answered (cleared grounding floor %.2f): engine %d / %d%n",
                FLOOR, engineA.count(), data.questions().size());
        System.out.println();
        System.out.printf("VERDICT: TF-IDF retrieval recalls the gold paragraph @1 in %.1f%% of %d questions "
                        + "(%.0fx the %.1f%% baseline); the extractive answer carries the gold answer in "
                        + "%.1f%% of questions (%.0fx the %.1f%% retrieval-ablated floor). Scrambling the "
                        + "question/document pairing collapses recall@1 to %.1f%%, proving the engine answers "
                        + "from real grounding.%n",
                engine.recallAt(1) * 100, data.questions().size(),
                engine.recallAt(1) / Math.max(baseline.recallAt(1), 1e-9), baseline.recallAt(1) * 100,
                engineA.containment() * 100,
                engineA.containment() / Math.max(ablatedA.containment(), 1e-9), ablatedA.containment() * 100,
                negControl.recallAt(1) * 100);
        System.out.println();
    }

    /** Accumulates document-level retrieval metrics over a question set. */
    private static final class Retrieval {
        private int n = 0;
        private int hit1 = 0;
        private int hit3 = 0;
        private int hit5 = 0;
        private double mrrSum = 0.0;

        void add(List<String> ranking, String gold) {
            n++;
            hit1 += (int) RetrievalMetrics.recallAt(ranking, gold, 1);
            hit3 += (int) RetrievalMetrics.recallAt(ranking, gold, 3);
            hit5 += (int) RetrievalMetrics.recallAt(ranking, gold, 5);
            mrrSum += RetrievalMetrics.reciprocalRank(ranking, gold);
        }

        double recallAt(int k) {
            return switch (k) {
                case 1 -> (double) hit1 / n;
                case 3 -> (double) hit3 / n;
                case 5 -> (double) hit5 / n;
                default -> throw new IllegalArgumentException("unsupported k " + k);
            };
        }

        double mrr() {
            return mrrSum / n;
        }
    }

    /** Accumulates SQuAD answer metrics over a question set (denominator is all questions). */
    private static final class Answers {
        private int n = 0;
        private int answered = 0;
        private double containSum = 0.0;
        private double f1Sum = 0.0;
        private double emSum = 0.0;

        void add(Answer answer, List<String> golds) {
            n++;
            if (!answer.escalated()) {
                answered++;
            }
            String text = answer.escalated() ? "" : answer.answer();
            containSum += SquadMetrics.contains(text, golds);
            f1Sum += SquadMetrics.tokenF1(text, golds);
            emSum += SquadMetrics.exactMatch(text, golds);
        }

        int count() {
            return answered;
        }

        double containment() {
            return containSum / n;
        }

        double tokenF1() {
            return f1Sum / n;
        }

        double exactMatch() {
            return emSum / n;
        }
    }
}
