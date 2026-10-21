CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE documents (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    chunk_count  INTEGER      NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_documents_owner ON documents (owner_id);

-- ids dos trechos no vector store, para apagar os embeddings junto com o documento
CREATE TABLE document_chunks (
    document_id BIGINT      NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_id    VARCHAR(36) NOT NULL
);

CREATE INDEX idx_document_chunks_document ON document_chunks (document_id);
