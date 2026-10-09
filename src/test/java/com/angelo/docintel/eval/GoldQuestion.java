package com.angelo.docintel.eval;

import java.util.List;

/**
 * One gold question from the SQuAD evaluation snapshot.
 *
 * @param id       SQuAD question id
 * @param question the natural-language question
 * @param docId    id of the corpus document the answer was written against
 *                 (SQuAD's ground truth for which paragraph should be retrieved)
 * @param answers  de-duplicated gold answer strings, copied verbatim from the document
 */
public record GoldQuestion(String id, String question, String docId, List<String> answers) {

    public GoldQuestion {
        answers = List.copyOf(answers);
    }
}
