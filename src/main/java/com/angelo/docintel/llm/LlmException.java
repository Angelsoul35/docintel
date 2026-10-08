package com.angelo.docintel.llm;

/**
 * Raised when an {@link LlmClient} cannot produce an answer (network error, bad
 * status, timeout, malformed response). The {@link CascadeLlmClient} treats it as a
 * signal to try the next provider.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
