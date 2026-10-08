package com.angelo.docintel.web;

import com.angelo.docintel.corpus.Document;
import com.angelo.docintel.corpus.DocumentStore;
import com.angelo.docintel.exception.BadRequestException;
import com.angelo.docintel.web.dto.DocumentRequest;
import com.angelo.docintel.web.dto.DocumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lists the corpus and adds documents to it. */
@RestController
public class DocumentController {

    private final DocumentStore store;

    public DocumentController(DocumentStore store) {
        this.store = store;
    }

    @GetMapping("/documents")
    public List<DocumentResponse> list() {
        return store.documents().stream()
                .map(doc -> DocumentResponse.of(doc, store.passageCount(doc.id())))
                .toList();
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse add(@RequestBody(required = false) DocumentRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("field 'title' is required");
        }
        if (request.text() == null || request.text().isBlank()) {
            throw new BadRequestException("field 'text' is required");
        }
        String id = (request.id() == null || request.id().isBlank())
                ? "doc-" + UUID.randomUUID()
                : request.id();
        Document document = new Document(id, request.title(), request.text());
        int passageCount = store.add(document);
        return DocumentResponse.of(document, passageCount);
    }
}
