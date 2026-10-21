package dev.asleonardo.docmind;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Substitui o LLM e o pgvector nos testes: embeddings determinísticos por "saco de palavras" com hash
 * (textos com palavras em comum ficam próximos) e um chat que devolve uma resposta fixa em pedaços.
 */
@TestConfiguration
public class FakeAiConfig {

    public static final AtomicReference<Prompt> LAST_PROMPT = new AtomicReference<>();

    @Bean
    EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> out = new ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    out.add(new Embedding(bagOfWords(request.getInstructions().get(i)), i));
                }
                return new EmbeddingResponse(out);
            }

            @Override
            public float[] embed(Document document) {
                return bagOfWords(document.getText());
            }
        };
    }

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    ChatModel chatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                LAST_PROMPT.set(prompt);
                return new ChatResponse(List.of(new Generation(new AssistantMessage("Resposta [1]"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                LAST_PROMPT.set(prompt);
                return Flux.just("Segundo ", "o manual, ", "reinicie a VPN [1].")
                        .map(t -> new ChatResponse(List.of(new Generation(new AssistantMessage(t)))));
            }
        };
    }

    static float[] bagOfWords(String text) {
        float[] v = new float[256];
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        for (String word : normalized.split("[^a-z0-9]+")) {
            if (word.length() > 3) {
                v[Math.floorMod(word.hashCode(), v.length)] += 1;
            }
        }
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) {
            v[0] = 1;
            return v;
        }
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
        return v;
    }
}
