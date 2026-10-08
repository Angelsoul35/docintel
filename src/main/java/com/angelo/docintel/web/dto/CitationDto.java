package com.angelo.docintel.web.dto;

import com.angelo.docintel.answer.Citation;

/** API view of a {@link Citation}. */
public record CitationDto(String passageId, String documentId, String documentTitle,
                          String snippet, double score) {

    public static CitationDto from(Citation citation) {
        return new CitationDto(citation.passageId(), citation.documentId(),
                citation.documentTitle(), citation.snippet(), citation.score());
    }
}
