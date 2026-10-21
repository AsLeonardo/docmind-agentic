package dev.asleonardo.docmind.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Fluxo SSE de uma resposta: primeiro o evento "sources" (para a interface já mostrar as fontes),
 * depois os pedaços do texto em eventos "token" e, no fim, "done". Cada pedaço vai como JSON ({"text": ...})
 * porque SSE descarta o espaço inicial de uma linha "data:", e os tokens costumam começar com espaço.
 */
@Service
public class ChatService {

    static final String NOTHING_FOUND = "Não encontrei nada sobre isso nos seus documentos. Envie um arquivo que trate do assunto ou reformule a pergunta.";
    private static final int MAX_HISTORY = 6;

    private final RetrievalService retrieval;
    private final ChatClient chatClient;

    public ChatService(RetrievalService retrieval, ChatClient.Builder builder) {
        this.retrieval = retrieval;
        this.chatClient = builder.defaultSystem(PromptFactory.SYSTEM).build();
    }

    public Flux<ServerSentEvent<Object>> answer(long ownerId, String question, List<Turn> history) {
        List<Source> sources = retrieval.search(ownerId, question);
        ServerSentEvent<Object> sourcesEvent = event("sources", sources);

        // sem contexto não chamamos o modelo: economiza tokens e elimina a chance de alucinação
        if (sources.isEmpty()) {
            return Flux.just(sourcesEvent, token(NOTHING_FOUND), event("done", Map.of()));
        }

        Flux<ServerSentEvent<Object>> tokens = chatClient.prompt()
                .messages(toMessages(history))
                .user(PromptFactory.user(question, sources))
                .stream()
                .content()
                .map(ChatService::token)
                .onErrorResume(e -> Flux.just(event("error", Map.of("message", "O modelo não respondeu. Tente de novo em instantes."))));

        return Flux.concat(Flux.just(sourcesEvent), tokens, Flux.just(event("done", Map.of())));
    }

    private static List<Message> toMessages(List<Turn> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .skip(Math.max(0, history.size() - MAX_HISTORY))
                .<Message>map(t -> "assistant".equals(t.role()) ? new AssistantMessage(t.content()) : new UserMessage(t.content()))
                .toList();
    }

    private static ServerSentEvent<Object> token(String text) {
        return event("token", Map.of("text", text));
    }

    private static ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder(data).event(name).build();
    }

    public record Turn(String role, String content) {
    }
}
