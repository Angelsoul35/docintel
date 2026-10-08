package com.angelo.docintel.corpus;

/**
 * A retrievable unit of text. Documents are split into passages; retrieval,
 * grounding and citations all operate at passage granularity.
 *
 * @param id            unique passage id (e.g. {@code doc-retention#1})
 * @param documentId    id of the document this passage came from
 * @param documentTitle title of that document, carried for citations
 * @param ordinal       position of the passage within its document (0-based)
 * @param text          passage text
 */
public record Passage(String id, String documentId, String documentTitle, int ordinal, String text) {
}
