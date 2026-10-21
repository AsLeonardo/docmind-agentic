package dev.asleonardo.docmind.document;

import dev.asleonardo.docmind.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lê o arquivo, quebra em trechos, gera embeddings e grava no vector store com o dono como metadado.
 * O metadado ownerId é o que garante, na busca, que um usuário nunca recebe trechos de outro.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final Map<String, String> SUPPORTED = Map.of(
            "pdf", "application/pdf",
            "md", "text/markdown",
            "txt", "text/plain");

    private final SourceDocumentRepository repository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public IngestionService(SourceDocumentRepository repository, VectorStore vectorStore, RagProperties rag) {
        this.repository = repository;
        this.vectorStore = vectorStore;
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(rag.chunkSize())
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(20)
                .withKeepSeparator(true)
                .build();
    }

    public SourceDocument ingest(long ownerId, String filename, byte[] content) {
        String extension = extension(filename);
        String contentType = SUPPORTED.get(extension);
        if (contentType == null) {
            throw new DocumentException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formatos aceitos: PDF, Markdown e TXT");
        }

        List<Document> pages = read(extension, filename, content);
        List<Document> chunks = splitter.apply(pages);
        if (chunks.isEmpty()) {
            throw new DocumentException(HttpStatus.UNPROCESSABLE_ENTITY, "Não encontrei texto no arquivo (PDF escaneado?)");
        }

        SourceDocument doc = repository.save(new SourceDocument(ownerId, filename, contentType, content.length));
        List<Document> enriched = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put(ChunkMetadata.OWNER_ID, String.valueOf(ownerId));
            metadata.put(ChunkMetadata.DOCUMENT_ID, String.valueOf(doc.getId()));
            metadata.put(ChunkMetadata.FILENAME, filename);
            metadata.put(ChunkMetadata.CHUNK_INDEX, i);
            enriched.add(new Document(UUID.randomUUID().toString(), chunk.getText(), metadata));
        }

        try {
            vectorStore.add(enriched);
        } catch (RuntimeException e) {
            // compensação: sem os embeddings o documento não serve para nada, então não deixamos registro órfão
            repository.delete(doc);
            log.error("Falha ao gerar embeddings de {}", filename, e);
            throw new DocumentException(HttpStatus.BAD_GATEWAY, "Falha ao gerar embeddings; tente de novo");
        }

        doc.attachChunks(enriched.stream().map(Document::getId).toList());
        log.info("Documento {} ({}) indexado em {} trechos", doc.getId(), filename, enriched.size());
        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<SourceDocument> list(long ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public void delete(long ownerId, long documentId) {
        SourceDocument doc = repository.findByIdAndOwnerId(documentId, ownerId)
                .orElseThrow(() -> new DocumentException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
        if (!doc.getChunkIds().isEmpty()) {
            vectorStore.delete(doc.getChunkIds());
        }
        repository.delete(doc);
    }

    private static List<Document> read(String extension, String filename, byte[] content) {
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        if (extension.equals("pdf")) {
            return new PagePdfDocumentReader(resource).get();
        }
        return new TextReader(resource).get();
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
