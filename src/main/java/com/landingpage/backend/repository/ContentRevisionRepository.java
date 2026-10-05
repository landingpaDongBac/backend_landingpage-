package com.landingpage.backend.repository;

import com.landingpage.backend.domain.ContentRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRevisionRepository extends JpaRepository<ContentRevision, UUID>, JpaSpecificationExecutor<ContentRevision> {
    List<ContentRevision> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);
    Optional<ContentRevision> findByIdAndDocumentId(UUID id, UUID documentId);
    Optional<ContentRevision> findTopByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    @Override
    @EntityGraph(attributePaths = "document")
    org.springframework.data.domain.Page<ContentRevision> findAll(org.springframework.data.jpa.domain.Specification<ContentRevision> spec,
                                                                  org.springframework.data.domain.Pageable pageable);
}
