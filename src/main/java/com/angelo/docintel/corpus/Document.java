package com.angelo.docintel.corpus;

/**
 * A source document before it is split into passages.
 *
 * @param id    stable identifier
 * @param title human-readable title, used in citations
 * @param text  raw document body
 */
public record Document(String id, String title, String text) {
}
