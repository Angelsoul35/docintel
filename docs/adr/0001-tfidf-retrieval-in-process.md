# 1. TF-IDF retrieval implemented in-process

Date: 2026-10-08

## Status

Accepted

## Context

A document-QA service needs a retrieval stage that selects the passages most relevant to a
question. The realistic production choice is embeddings plus a vector database, but that
adds a heavy dependency (an embedding model or API, and an external store) and introduces
network and non-determinism into the part of the system that most needs to be correct and
testable. For a focused portfolio service whose goal is to demonstrate the *shape* of a
grounded RAG pipeline, the retrieval algorithm itself should be inspectable and run
anywhere with no setup.

## Decision

Implement retrieval as **TF-IDF with cosine similarity in plain Java**, in-process, over
an in-memory passage list:

- a tokenizer that lower-cases, splits on non-alphanumeric characters and removes a small
  stop-word list;
- smoothed inverse document frequency, `idf(t) = ln((N + 1) / (df(t) + 1)) + 1`, which
  keeps weights positive and handles unseen query terms;
- per-passage `tf * idf` vectors and cosine similarity for ranking;
- top-k selection with a deterministic tie-break on passage id.

The index is rebuilt lazily when the corpus changes.

## Consequences

- Retrieval is **deterministic and fully unit-testable** with no external services; it is
  where the test suite concentrates.
- The service runs with **no model download, no vector store, no network**.
- The trade-off is that retrieval is **lexical, not semantic**: paraphrases with no shared
  vocabulary will not match. This is documented as a scope limitation; a production system
  would add embeddings and a real vector database (see the README scope note).
- Rebuilding the index on change is fine for a small corpus but would not scale; a real
  deployment would use an incremental index or an external store.
