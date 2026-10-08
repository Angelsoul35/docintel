package com.angelo.docintel.web;

import com.angelo.docintel.answer.Answer;
import com.angelo.docintel.answer.AnswerService;
import com.angelo.docintel.exception.BadRequestException;
import com.angelo.docintel.web.dto.AskRequest;
import com.angelo.docintel.web.dto.AskResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Answers questions grounded in the corpus. */
@RestController
public class AskController {

    private final AnswerService answerService;

    public AskController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@RequestBody(required = false) AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new BadRequestException("field 'question' is required");
        }
        Answer answer = answerService.ask(request.question());
        return AskResponse.from(answer);
    }
}
