package com.angelo.docintel.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheSeedCorpusWithPassageCounts() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].passageCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void addsADocumentAndCanThenAnswerFromIt() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"doc-sla\",\"title\":\"Service levels\","
                                + "\"text\":\"The platform targets 99.9 percent uptime measured monthly. "
                                + "Planned maintenance windows are announced at least seven days in advance.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("doc-sla"))
                .andExpect(jsonPath("$.passageCount").value(org.hamcrest.Matchers.greaterThan(0)));

        // The newly added document is now retrievable.
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"what uptime does the platform target\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(false))
                .andExpect(jsonPath("$.citations[0].documentId").value("doc-sla"));
    }

    @Test
    void rejectsADocumentWithoutText() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No body\"}"))
                .andExpect(status().isBadRequest());
    }
}
