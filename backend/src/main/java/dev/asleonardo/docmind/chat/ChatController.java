package dev.asleonardo.docmind.chat;

import dev.asleonardo.docmind.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class ChatController {

    private final ChatService chat;
    private final RetrievalService retrieval;

    public ChatController(ChatService chat, RetrievalService retrieval) {
        this.chat = chat;
        this.retrieval = retrieval;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request) {
        return chat.answer(CurrentUser.id(jwt), request.question(), request.history());
    }

    /** Só a recuperação, sem LLM. Usado pelo script de avaliação e para depurar o RAG. */
    @GetMapping("/search")
    public List<Source> search(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam @NotBlank String q,
                               @RequestParam(defaultValue = "5") @Min(1) @Max(20) int k) {
        return retrieval.search(CurrentUser.id(jwt), q, k);
    }

    public record ChatRequest(@NotBlank @Size(max = 2000) String question, @Size(max = 20) List<ChatService.Turn> history) {
    }
}
