# 3. Ground-then-answer with a similarity floor

Date: 2026-10-08

## Status

Accepted

## Context

The most damaging failure mode of a QA service is a confident answer to a question the
corpus does not actually cover. If retrieval returns only weak, irrelevant passages and we
still hand them to an LLM, the model will often "answer" anyway by filling gaps from its
parametric knowledge — producing an ungrounded response that looks authoritative. The
service needs a defined behaviour for "I don't know".

## Decision

Enforce a **grounding floor before answering**. `AnswerService`:

1. retrieves the top-k passages;
2. if the corpus is empty or the **best passage's cosine similarity is below a configurable
   floor** (`docintel.retrieval.similarity-floor`), it returns a fixed
   "I don't have that information" message with `source=none`, `escalated=true`, no
   citations and `confidence=0` — **without consulting any LLM**;
3. otherwise it proceeds to the answerer with exactly those passages.

The floor is a configuration value so it can be tuned per deployment.

## Consequences

- The service **refuses rather than confabulates** when nothing relevant is retrieved, and
  signals this explicitly so a caller can route the question to a human.
- The check runs **before** the LLM, so it applies identically to the live and offline
  paths and costs nothing when it triggers.
- The floor is a single global threshold on a lexical score; it is a blunt instrument. A
  production system would complement it with answer-level faithfulness checks and
  per-domain calibration.
- Choosing the floor is a precision/recall trade-off: too high and valid questions are
  escalated, too low and thin context slips through. The default (0.05) cleanly separates
  "no shared terms" (score 0) from real matches for the seeded corpus.
