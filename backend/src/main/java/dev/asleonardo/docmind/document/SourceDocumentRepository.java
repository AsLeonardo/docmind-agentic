package dev.asleonardo.docmind.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {
    List<SourceDocument> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<SourceDocument> findByIdAndOwnerId(Long id, Long ownerId);
}
