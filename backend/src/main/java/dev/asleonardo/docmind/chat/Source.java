package dev.asleonardo.docmind.chat;

/** Um trecho recuperado, numerado como aparece no prompt e na resposta ([1], [2]...). */
public record Source(int n, long documentId, String filename, Integer page, int chunkIndex, double score, String text) {
}
