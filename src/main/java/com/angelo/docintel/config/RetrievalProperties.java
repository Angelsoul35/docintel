package com.angelo.docintel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the retrieval stage.
 *
 * @param topK           how many passages to retrieve for a query
 * @param similarityFloor the minimum cosine similarity the best passage must reach;
 *                        below it the service refuses to answer and escalates instead
 *                        of answering ungrounded
 */
@ConfigurationProperties(prefix = "docintel.retrieval")
public record RetrievalProperties(int topK, double similarityFloor) {

    public RetrievalProperties {
        if (topK <= 0) {
            topK = 3;
        }
        if (similarityFloor < 0) {
            similarityFloor = 0.0;
        }
    }
}
