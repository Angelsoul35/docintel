package com.angelo.docintel.llm;

import com.angelo.docintel.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Shared plumbing for thin HTTP LLM providers. A provider is only
 * {@link #isAvailable() available} when its API key is configured, so these clients
 * are never constructed or exercised by the offline test suite.
 *
 * <p>Subclasses describe the request body, the endpoint and how to read the answer
 * out of the response; everything else (timeouts, status handling, mapping failures
 * to {@link LlmException}) lives here.
 */
public abstract class HttpLlmClient implements LlmClient {

    private final String name;
    private final LlmProperties.Provider config;
    private final HttpClient http;
    private final Duration timeout;
    protected final ObjectMapper mapper = new ObjectMapper();

    protected HttpLlmClient(String name, LlmProperties.Provider config, int timeoutMs) {
        this.name = name;
        this.config = config;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAvailable() {
        return config.hasKey();
    }

    protected LlmProperties.Provider config() {
        return config;
    }

    @Override
    public LlmResult answer(GroundedRequest request) {
        String system = PromptBuilder.system();
        String user = PromptBuilder.user(request);
        try {
            HttpRequest httpRequest = buildRequest(system, user);
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new LlmException(name + " returned HTTP " + response.statusCode());
            }
            String text = extractText(mapper.readTree(response.body()));
            if (text == null || text.isBlank()) {
                throw new LlmException(name + " returned an empty answer");
            }
            return new LlmResult(text.trim(), AnswerSource.LLM, name);
        } catch (IOException e) {
            throw new LlmException(name + " request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException(name + " request was interrupted", e);
        }
    }

    /** Build the fully-formed HTTP request, including auth headers and JSON body. */
    protected abstract HttpRequest buildRequest(String system, String user) throws IOException;

    /** Extract the answer text from the parsed provider response. */
    protected abstract String extractText(JsonNode body);

    /** Convenience for subclasses building requests. */
    protected HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + path))
                .timeout(timeout)
                .header("content-type", "application/json");
    }

    protected String json(Object value) throws IOException {
        return mapper.writeValueAsString(value);
    }
}
