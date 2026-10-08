package com.angelo.docintel.llm;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries a list of {@link LlmClient}s in order and returns the first answer. A client
 * is skipped when it is not {@link LlmClient#isAvailable() available} (e.g. no API
 * key) and the cascade moves on to the next client when an available one throws.
 *
 * <p>The list is expected to end with the always-available offline fallback, so the
 * cascade is guaranteed to return an answer with no key and no network.
 */
public class CascadeLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(CascadeLlmClient.class);

    private final List<LlmClient> clients;

    public CascadeLlmClient(List<LlmClient> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("cascade requires at least one client");
        }
        this.clients = List.copyOf(clients);
    }

    @Override
    public String name() {
        return "cascade";
    }

    @Override
    public boolean isAvailable() {
        return clients.stream().anyMatch(LlmClient::isAvailable);
    }

    @Override
    public LlmResult answer(GroundedRequest request) {
        LlmException last = null;
        for (LlmClient client : clients) {
            if (!client.isAvailable()) {
                log.debug("skipping unavailable LLM client '{}'", client.name());
                continue;
            }
            try {
                LlmResult result = client.answer(request);
                log.debug("LLM client '{}' produced the answer", client.name());
                return result;
            } catch (LlmException e) {
                log.warn("LLM client '{}' failed, falling back: {}", client.name(), e.getMessage());
                last = e;
            }
        }
        throw new LlmException("no LLM client could produce an answer", last);
    }

    /** The ordered client ids, for diagnostics. */
    public List<String> providerOrder() {
        return clients.stream().map(LlmClient::name).toList();
    }
}
