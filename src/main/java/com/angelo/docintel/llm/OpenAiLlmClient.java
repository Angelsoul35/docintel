package com.angelo.docintel.llm;

import com.angelo.docintel.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

/**
 * Thin adapter for the OpenAI Chat Completions API. Activated only when an API key is set.
 */
public class OpenAiLlmClient extends HttpLlmClient {

    public static final String NAME = "openai";

    public OpenAiLlmClient(LlmProperties.Provider config, int timeoutMs) {
        super(NAME, config, timeoutMs);
    }

    @Override
    protected HttpRequest buildRequest(String system, String user) throws IOException {
        Map<String, Object> body = Map.of(
                "model", config().model(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
        return request("/v1/chat/completions")
                .header("authorization", "Bearer " + config().apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json(body)))
                .build();
    }

    @Override
    protected String extractText(JsonNode body) {
        JsonNode choices = body.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText(null);
        }
        return null;
    }
}
