package com.angelo.docintel;

import com.angelo.docintel.config.LlmProperties;
import com.angelo.docintel.config.RetrievalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the DocIntel service: a small, provider-agnostic document-QA
 * microservice that answers questions grounded in a document corpus and returns
 * citations. Retrieval (TF-IDF + cosine) is the deterministic core; the LLM only
 * phrases the grounded context, and a deterministic offline fallback keeps the
 * service working with no API key and no network.
 */
@SpringBootApplication
@EnableConfigurationProperties({LlmProperties.class, RetrievalProperties.class})
public class DocIntelApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocIntelApplication.class, args);
    }
}
