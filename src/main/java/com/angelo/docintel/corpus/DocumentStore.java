package com.angelo.docintel.corpus;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * An in-memory corpus. Documents are kept in insertion order and flattened into a
 * passage list that the retriever indexes. A monotonically increasing {@link #version()}
 * lets the retriever know when it must rebuild its index.
 *
 * <p>This is deliberately a toy store. Production would use a real document store
 * plus a vector database (see the README scope note).
 */
@Component
public class DocumentStore {

    private final Map<String, Document> documents = new LinkedHashMap<>();
    private final List<Passage> passages = new ArrayList<>();
    private final AtomicLong version = new AtomicLong(0);

    @PostConstruct
    void seed() {
        for (Document document : SeedCorpus.documents()) {
            add(document);
        }
    }

    /**
     * Add (or replace) a document and split it into passages.
     *
     * @return the number of passages produced for the document
     */
    public synchronized int add(Document document) {
        if (document.id() == null || document.id().isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
        // Drop any passages belonging to a document with the same id (idempotent replace).
        passages.removeIf(p -> p.documentId().equals(document.id()));
        documents.put(document.id(), document);
        List<Passage> produced = PassageSplitter.split(document);
        passages.addAll(produced);
        version.incrementAndGet();
        return produced.size();
    }

    public synchronized List<Document> documents() {
        return List.copyOf(documents.values());
    }

    /** A stable snapshot of all passages across all documents. */
    public synchronized List<Passage> passages() {
        return List.copyOf(passages);
    }

    public synchronized int passageCount(String documentId) {
        return (int) passages.stream().filter(p -> p.documentId().equals(documentId)).count();
    }

    /** Changes whenever the corpus is mutated; used by the retriever to invalidate its index. */
    public long version() {
        return version.get();
    }

    /**
     * The seed corpus: a small product-support knowledge base. Kept in its own type so
     * it is easy to see what the service ships with out of the box.
     */
    static final class SeedCorpus {
        private SeedCorpus() {
        }

        static List<Document> documents() {
            List<Document> docs = new ArrayList<>();
            docs.add(new Document("doc-password", "Resetting your password",
                    "To reset your password, open the sign-in page and choose 'Forgot password'. "
                            + "We email a reset link that stays valid for 30 minutes. "
                            + "If the link expires, request a new one from the same page. "
                            + "For security, resetting a password signs out all active sessions on every device."));
            docs.add(new Document("doc-billing", "Billing and invoices",
                    "Invoices are issued on the first day of each month and charged to the card on file. "
                            + "You can download past invoices as PDF from the Billing page in your account. "
                            + "Annual plans receive a 20 percent discount compared with paying monthly."));
            docs.add(new Document("doc-retention", "Data retention",
                    "Uploaded documents are retained for 90 days by default. "
                            + "Enterprise workspaces can configure retention anywhere between 30 and 365 days. "
                            + "When a document is deleted it is purged from encrypted backups within 35 days."));
            docs.add(new Document("doc-ratelimit", "API rate limits",
                    "The REST API allows 60 requests per minute for each API key by default. "
                            + "Exceeding the limit returns HTTP 429 with a Retry-After header telling you when to try again. "
                            + "Contact support to request a higher quota for production workloads."));
            docs.add(new Document("doc-filetypes", "Supported file types",
                    "You can upload PDF, DOCX, TXT and Markdown files up to 25 MB each. "
                            + "Scanned PDFs are processed with OCR so that their text becomes searchable. "
                            + "Password-protected files must be unlocked before you upload them."));
            return Collections.unmodifiableList(docs);
        }
    }
}
