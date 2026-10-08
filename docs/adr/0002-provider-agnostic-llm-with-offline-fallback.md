# 2. Provider-agnostic LLM client with a deterministic offline fallback

Date: 2026-10-08

## Status

Accepted

## Context

The answering stage benefits from an LLM to phrase a fluent answer from retrieved context.
But binding directly to one vendor's SDK couples the service to that provider, pulls in a
large dependency tree, and — most importantly — makes the service unusable (and untestable)
whenever there is no API key, the provider is down, or there is no network. A portfolio
service should keep working and keep being testable under all of those conditions.

## Decision

Define a small **`LlmClient` interface** (`name`, `isAvailable`, `answer`) and compose
implementations rather than depending on any vendor SDK:

- thin HTTP **provider adapters** (Anthropic, OpenAI) that build a grounded prompt and are
  `isAvailable()` only when their API key is configured;
- a **`CascadeLlmClient`** that tries providers in configured order, skips unavailable ones
  and falls through to the next when an available one fails;
- a deterministic **`OfflineLlmClient`** that is always available and composes an
  **extractive** answer from the top passage.

`LlmClientConfig` assembles the cascade from `docintel.llm.providers`, appending the offline
client last. No Spring AI starter / BOM is used, so the build resolves and runs offline.

## Consequences

- The LLM is **optional and swappable**; adding a provider is a new adapter plus a config
  entry.
- With no key the cascade is **offline-only**, which is exactly the configuration the test
  suite runs under — so **no test touches the network or needs a key**.
- Each result records whether it came from a live LLM (`source=llm`) or the fallback
  (`source=extractive`), so callers can tell how an answer was produced.
- The adapters are intentionally minimal (no streaming, retries or token accounting); those
  would be added for production use.
