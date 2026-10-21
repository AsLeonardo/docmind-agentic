package dev.asleonardo.docmind.document;

import dev.asleonardo.docmind.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestion;

    public DocumentController(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SourceDocument upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) throws IOException {
        return ingestion.ingest(CurrentUser.id(jwt), file.getOriginalFilename(), file.getBytes());
    }

    @GetMapping
    public List<SourceDocument> list(@AuthenticationPrincipal Jwt jwt) {
        return ingestion.list(CurrentUser.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        ingestion.delete(CurrentUser.id(jwt), id);
    }
}
