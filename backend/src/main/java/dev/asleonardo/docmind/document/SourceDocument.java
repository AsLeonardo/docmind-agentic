package dev.asleonardo.docmind.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Um arquivo enviado pelo usuário. Os trechos (chunks) com embeddings ficam no vector store. */
@Entity
@Table(name = "documents")
public class SourceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    private long sizeBytes;

    private int chunkCount;

    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "document_chunks", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "chunk_id", nullable = false)
    private List<String> chunkIds = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected SourceDocument() {
    }

    public SourceDocument(Long ownerId, String filename, String contentType, long sizeBytes) {
        this.ownerId = ownerId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    void attachChunks(List<String> ids) {
        this.chunkIds = new ArrayList<>(ids);
        this.chunkCount = ids.size();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public int getChunkCount() { return chunkCount; }
    public List<String> getChunkIds() { return chunkIds; }
    public Instant getCreatedAt() { return createdAt; }
}
