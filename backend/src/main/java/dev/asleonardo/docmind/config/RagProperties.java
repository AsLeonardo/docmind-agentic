package dev.asleonardo.docmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do RAG. Os valores padrão vieram do conjunto de avaliação em eval/ (ver README).
 *
 * @param chunkSize           tamanho de cada trecho, em tokens
 * @param topK                quantos trechos entram no prompt
 * @param similarityThreshold similaridade mínima para um trecho ser considerado
 */
@ConfigurationProperties(prefix = "docmind.rag")
public record RagProperties(int chunkSize, int topK, double similarityThreshold) {
}
