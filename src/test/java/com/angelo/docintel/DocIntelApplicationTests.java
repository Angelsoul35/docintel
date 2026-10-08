package com.angelo.docintel;

import static org.assertj.core.api.Assertions.assertThat;

import com.angelo.docintel.llm.CascadeLlmClient;
import com.angelo.docintel.llm.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocIntelApplicationTests {

    @Autowired
    private LlmClient llmClient;

    @Test
    void contextLoads() {
        assertThat(llmClient).isNotNull();
    }

    @Test
    void withNoApiKeyTheCascadeIsOfflineOnly() {
        // The test profile configures no providers, so the only client is the offline
        // fallback: proof that the service runs with no key and no network.
        assertThat(llmClient).isInstanceOf(CascadeLlmClient.class);
        assertThat(((CascadeLlmClient) llmClient).providerOrder()).containsExactly("offline");
    }
}
