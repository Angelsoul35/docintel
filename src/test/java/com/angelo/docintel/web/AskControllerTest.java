package com.angelo.docintel.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test of {@code POST /ask} over the real Spring context: real TF-IDF
 * retriever, real seed corpus, and the offline extractive answerer (no LLM key is
 * configured in the test profile, so no network is touched).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inCorpusQuestionReturnsAGroundedExtractiveAnswerWithCitations() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"how long are uploaded documents retained\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("extractive"))
                .andExpect(jsonPath("$.escalated").value(false))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("90 days")))
                .andExpect(jsonPath("$.confidence").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.citations").isNotEmpty())
                .andExpect(jsonPath("$.citations[0].documentId").value("doc-retention"))
                .andExpect(jsonPath("$.citations[0].documentTitle").value("Data retention"));
    }

    @Test
    void outOfCorpusQuestionEscalatesInsteadOfAnswering() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"how do I bake sourdough bread at home\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("none"))
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.confidence").value(0.0))
                .andExpect(jsonPath("$.citations").isEmpty())
                .andExpect(jsonPath("$.answer").value(
                        org.hamcrest.Matchers.containsString("don't have that information")));
    }

    @Test
    void missingQuestionIsRejected() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
