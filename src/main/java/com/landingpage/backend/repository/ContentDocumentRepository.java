package com.landingpage.backend.repository;

import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.ContentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentDocumentRepository extends JpaRepository<ContentDocument, UUID>, JpaSpecificationExecutor<ContentDocument> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    @EntityGraph(attributePaths = "section")
    Optional<ContentDocument> findBySlugAndStatusAndSectionEnabledTrue(String slug, ContentStatus status);

    @EntityGraph(attributePaths = "section")
    List<ContentDocument> findByStatusAndSectionEnabledTrueOrderBySectionDisplayOrderAscDisplayOrderAsc(ContentStatus status);

    @EntityGraph(attributePaths = "section")
    List<ContentDocument> findAllByOrderBySectionDisplayOrderAscDisplayOrderAsc();
    long countByStatus(ContentStatus status);
    long countBySectionId(UUID sectionId);

    @EntityGraph(attributePaths = "section")
    List<ContentDocument> findTop5ByOrderByUpdatedAtDesc();
}
