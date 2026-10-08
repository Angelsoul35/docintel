package com.angelo.docintel.config;

import com.angelo.docintel.llm.AnthropicLlmClient;
import com.angelo.docintel.llm.CascadeLlmClient;
import com.angelo.docintel.llm.LlmClient;
import com.angelo.docintel.llm.OfflineLlmClient;
import com.angelo.docintel.llm.OpenAiLlmClient;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assembles the {@link CascadeLlmClient} from configuration. For each provider id in
 * {@code docintel.llm.providers}, the matching HTTP adapter is added only when its
 * API key is present. The deterministic offline fallback is always appended last, so
 * with no keys configured the cascade contains exactly the offline client and the
 * service answers extractively with no network.
 */
@Configuration
public class LlmClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmClientConfig.class);

    @Bean
    LlmClient llmClient(LlmProperties properties) {
        List<LlmClient> clients = new ArrayList<>();
        for (String provider : properties.providers()) {
            LlmClient client = buildProvider(provider.trim().toLowerCase(), properties);
            if (client != null) {
                clients.add(client);
                log.info("LLM provider '{}' is configured and will be tried first-to-last", client.name());
            }
        }
        // The offline fallback is always present and always last.
        clients.add(new OfflineLlmClient());

        CascadeLlmClient cascade = new CascadeLlmClient(clients);
        log.info("LLM cascade order: {}", cascade.providerOrder());
        return cascade;
    }

    private LlmClient buildProvider(String provider, LlmProperties properties) {
        return switch (provider) {
            case AnthropicLlmClient.NAME -> activate(properties.anthropic(), provider,
                    () -> new AnthropicLlmClient(properties.anthropic(), properties.timeoutMs()));
            case OpenAiLlmClient.NAME -> activate(properties.openai(), provider,
                    () -> new OpenAiLlmClient(properties.openai(), properties.timeoutMs()));
            default -> {
                log.warn("ignoring unknown LLM provider id '{}'", provider);
                yield null;
            }
        };
    }

    private LlmClient activate(LlmProperties.Provider config, String provider, java.util.function.Supplier<LlmClient> factory) {
        if (config == null || !config.hasKey()) {
            log.info("LLM provider '{}' has no API key; it stays inactive and the service "
                    + "will fall back to the offline extractive answerer", provider);
            return null;
        }
        return factory.get();
    }
}
