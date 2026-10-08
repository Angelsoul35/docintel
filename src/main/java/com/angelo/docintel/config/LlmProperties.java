package com.angelo.docintel.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Declarative LLM provider configuration.
 *
 * <p>{@code providers} is the order in which configured providers are tried. A
 * provider is only activated when its API key is present; the deterministic
 * offline fallback is always appended last so an answer is always produced.
 *
 * @param providers ordered provider ids to try (e.g. {@code [anthropic, openai]})
 * @param timeoutMs HTTP timeout for provider calls, in milliseconds
 * @param anthropic Anthropic provider settings
 * @param openai    OpenAI provider settings
 */
@ConfigurationProperties(prefix = "docintel.llm")
public record LlmProperties(
        List<String> providers,
        int timeoutMs,
        Provider anthropic,
        Provider openai) {

    public LlmProperties {
        if (providers == null) {
            providers = List.of();
        }
        if (timeoutMs <= 0) {
            timeoutMs = 20_000;
        }
    }

    /**
     * Settings for a single HTTP provider.
     *
     * @param apiKey  credential; when blank the provider stays inactive and is skipped
     * @param model   model identifier sent to the provider
     * @param baseUrl provider base URL
     */
    public record Provider(String apiKey, String model, String baseUrl) {

        /** A provider is usable only when an API key has actually been supplied. */
        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
