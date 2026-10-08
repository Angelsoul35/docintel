package com.angelo.docintel.web.dto;

import com.angelo.docintel.answer.Answer;
import com.angelo.docintel.llm.AnswerSource;
import java.util.List;

/** Response body for {@code POST /ask}. */
public record AskResponse(String answer, AnswerSource source, List<CitationDto> citations,
                          double confidence, boolean escalated) {

    public static AskResponse from(Answer answer) {
        List<CitationDto> citations = answer.citations().stream().map(CitationDto::from).toList();
        return new AskResponse(answer.answer(), answer.source(), citations,
                answer.confidence(), answer.escalated());
    }
}
