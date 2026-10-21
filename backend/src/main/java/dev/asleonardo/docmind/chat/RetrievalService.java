package dev.asleonardo.docmind.chat;

import dev.asleonardo.docmind.config.RagProperties;
import dev.asleonardo.docmind.document.ChunkMetadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final RagProperties rag;

    public RetrievalService(VectorStore vectorStore, RagProperties rag) {
        this.vectorStore = vectorStore;
        this.rag = rag;
    }

    public List<Source> search(long ownerId, String query) {
        return search(ownerId, query, rag.topK());
    }

    public List<Source> search(long ownerId, String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(rag.similarityThreshold())
                // filtro aplicado dentro da consulta vetorial, não depois: o usuário só enxerga os próprios trechos
                .filterExpression(new FilterExpressionBuilder().eq(ChunkMetadata.OWNER_ID, String.valueOf(ownerId)).build())
                .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        List<Source> sources = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            Map<String, Object> m = d.getMetadata();
            sources.add(new Source(
                    i + 1,
                    Long.parseLong(String.valueOf(m.get(ChunkMetadata.DOCUMENT_ID))),
                    String.valueOf(m.get(ChunkMetadata.FILENAME)),
                    m.get(ChunkMetadata.PAGE) instanceof Number page ? page.intValue() : null,
                    ((Number) m.getOrDefault(ChunkMetadata.CHUNK_INDEX, 0)).intValue(),
                    d.getScore() == null ? 0 : d.getScore(),
                    d.getText()));
        }
        return sources;
    }
}
