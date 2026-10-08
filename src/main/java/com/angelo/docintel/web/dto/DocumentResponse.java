package com.angelo.docintel.web.dto;

import com.angelo.docintel.corpus.Document;

/** API view of a stored {@link Document}, with how many passages it was split into. */
public record DocumentResponse(String id, String title, int passageCount) {

    public static DocumentResponse of(Document document, int passageCount) {
        return new DocumentResponse(document.id(), document.title(), passageCount);
    }
}
