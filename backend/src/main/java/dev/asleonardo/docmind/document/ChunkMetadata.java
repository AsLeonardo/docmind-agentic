package dev.asleonardo.docmind.document;

/** Chaves de metadado gravadas em cada trecho no vector store. */
public final class ChunkMetadata {

    public static final String OWNER_ID = "ownerId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String FILENAME = "filename";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String PAGE = "page_number"; // definido pelo leitor de PDF do Spring AI

    private ChunkMetadata() {
    }
}
