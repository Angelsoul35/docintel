package com.angelo.docintel.llm;

import com.angelo.docintel.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

/**
 * Thin adapter for the Anthropic Messages API. Activated only when an API key is set.
 */
public class AnthropicLlmClient extends HttpLlmClient {

    public static final String NAME = "anthropic";

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    public AnthropicLlmClient(LlmProperties.Provider config, int timeoutMs) {
        super(NAME, config, timeoutMs);
    }

    @Override
    protected HttpRequest buildRequest(String system, String user) throws IOException {
        Map<String, Object> body = Map.of(
                "model", config().model(),
                "max_tokens", MAX_TOKENS,
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", user)));
        return request("/v1/messages")
                .header("x-api-key", config().apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(json(body)))
                .build();
    }

    @Override
    protected String extractText(JsonNode body) {
        JsonNode content = body.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText(null);
        }
        return null;
    }
}
