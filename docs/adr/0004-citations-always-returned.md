# 4. Citations are always returned for a grounded answer

Date: 2026-10-08

## Status

Accepted

## Context

An answer the caller cannot verify is of limited value in a document-QA setting: the whole
point is that the answer is backed by the corpus. Both the live-LLM path and the offline
extractive path operate over the same retrieved passages, so the evidence for an answer is
always available at the moment it is produced.

## Decision

Make **citations a first-class, non-optional part of a grounded response**. For every
answer that passes the grounding floor, `AnswerService` attaches the retrieved passages as
`Citation`s (`passageId`, `documentId`, `documentTitle`, `snippet`, `score`), regardless of
whether the text was phrased by an LLM or extracted offline. The confidence reported is the
best passage's similarity. When the service escalates, it returns an empty citation list,
which unambiguously marks "no grounded answer".

## Consequences

- Every grounded answer is **traceable to specific passages and documents**; a client (or a
  human reviewer) can check each claim.
- The extractive fallback's answer text is a **verbatim span of a cited passage**, so it is
  grounded by construction — this is asserted in the tests.
- Citations do not *prove* the natural-language answer faithfully reflects the passages
  when a live LLM phrases it; they show the evidence that was available. Verifying
  faithfulness of the generated text is a separate concern (future eval harness).
- The citation always carries the passage `snippet`, so the response is self-contained and
  does not require a second lookup against the corpus.
