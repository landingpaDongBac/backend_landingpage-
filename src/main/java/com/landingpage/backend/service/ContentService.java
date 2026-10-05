package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.api.dto.ContentDocumentRequest;
import com.landingpage.backend.api.dto.ContentDocumentResponse;
import com.landingpage.backend.api.dto.ContentRevisionResponse;
import com.landingpage.backend.api.dto.PublicContentResponse;
import com.landingpage.backend.api.dto.PublicMediaResponse;
import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.ContentRevision;
import com.landingpage.backend.domain.ContentStatus;
import com.landingpage.backend.domain.MediaAsset;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.domain.Section;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.exception.ResourceNotFoundException;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.ContentRevisionRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import com.landingpage.backend.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "publishedAt", "title", "slug", "displayOrder", "status");

    private final ContentDocumentRepository documentRepository;
    private final ContentRevisionRepository revisionRepository;
    private final SectionRepository sectionRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final RichTextDocumentValidator richTextValidator;
    private final SectionService sectionService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ContentDocumentResponse> listAdmin() {
        return documentRepository.findAllByOrderBySectionDisplayOrderAscDisplayOrderAsc().stream()
                .map(this::toAdminResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContentDocumentResponse getAdmin(UUID id) {
        return toAdminResponse(findDocument(id));
    }

    @Transactional(readOnly = true)
    public Page<ContentDocumentResponse> searchAdmin(String query, UUID sectionId, ContentStatus status,
                                                      int page, int size, String sortBy, Sort.Direction direction) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        String safeSort = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "updatedAt";
        Specification<ContentDocument> spec = (root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (sectionId != null) predicates.add(builder.equal(root.get("section").get("id"), sectionId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("slug")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return documentRepository.findAll(spec, PageRequest.of(page, size, Sort.by(direction, safeSort)))
                .map(this::toAdminResponse);
    }

    @Transactional
    public ContentDocumentResponse create(ContentDocumentRequest request, String actor) {
        if (documentRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Content slug already exists");
        }
        validateMediaReferences(request.contentJson());
        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        ContentDocument document = new ContentDocument();
        document.setSection(section);
        document.setTitle(request.title().trim());
        document.setSlug(request.slug().trim());
        document.setContentJson(request.contentJson().deepCopy());
        document.setStatus(ContentStatus.DRAFT);
        document.setDisplayOrder(request.displayOrder());
        document.setCreatedBy(actor);
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        saveRevision(saved, actor, "Document created");
        audit(actor, "CONTENT_CREATED", saved.getId(), "Content document created");
        return toAdminResponse(saved);
    }

    @Transactional
    public ContentDocumentResponse update(UUID id, ContentDocumentRequest request, String actor) {
        ContentDocument document = findDocument(id);
        if (document.getStatus() == ContentStatus.ARCHIVED) {
            throw new ConflictException("CONTENT_ARCHIVED", "Restore archived content before editing it");
        }
        if (request.version() == null) {
            throw new BadRequestException("The current document version is required for updates");
        }
        if (request.version() != document.getVersion()) {
            throw new ConflictException("CONTENT_VERSION_CONFLICT", "Content was changed by another user; reload before saving");
        }
        if (documentRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ConflictException("Content slug already exists");
        }
        validateMediaReferences(request.contentJson());
        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        document.setSection(section);
        document.setTitle(request.title().trim());
        document.setSlug(request.slug().trim());
        document.setContentJson(request.contentJson().deepCopy());
        document.setDisplayOrder(request.displayOrder());
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        saveRevision(saved, actor, "Draft updated");
        audit(actor, "CONTENT_DRAFT_UPDATED", saved.getId(), "Content draft updated");
        return toAdminResponse(saved);
    }

    @Transactional
    public ContentDocumentResponse publish(UUID id, String actor) {
        return publish(id, null, actor);
    }

    @Transactional
    public ContentDocumentResponse publish(UUID id, Long expectedVersion, String actor) {
        ContentDocument document = findDocument(id);
        if (document.getStatus() == ContentStatus.ARCHIVED) {
            throw new ConflictException("CONTENT_ARCHIVED", "Restore archived content before publishing it");
        }
        if (expectedVersion != null && expectedVersion != document.getVersion()) {
            throw new ConflictException("CONTENT_VERSION_CONFLICT", "Content was changed by another user; reload before publishing");
        }
        if (!document.getSection().isEnabled()) {
            throw new ConflictException("SECTION_DISABLED", "Content cannot be published to a disabled section");
        }
        if (document.getContentJson() == null || !document.getContentJson().path("content").isArray()
                || document.getContentJson().path("content").isEmpty()) {
            throw new ConflictException("CONTENT_VALIDATION_FAILED", "Content must contain at least one node before publishing");
        }
        validateMediaReferences(document.getContentJson());
        document.setPublishedContentJson(document.getContentJson().deepCopy());
        document.setStatus(ContentStatus.PUBLISHED);
        document.setPublishedAt(Instant.now());
        document.setPublishedBy(actor);
        document.setArchivedAt(null);
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        audit(actor, "CONTENT_PUBLISHED", saved.getId(), "Content published");
        return toAdminResponse(saved);
    }

    @Transactional
    public ContentDocumentResponse unpublish(UUID id, String actor) {
        ContentDocument document = findDocument(id);
        if (document.getStatus() != ContentStatus.PUBLISHED) {
            throw new ConflictException("CONTENT_NOT_PUBLISHED", "Only published content can be unpublished");
        }
        document.setStatus(ContentStatus.DRAFT);
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        audit(actor, "CONTENT_UNPUBLISHED", saved.getId(), "Content unpublished");
        return toAdminResponse(saved);
    }

    @Transactional
    public ContentDocumentResponse reorder(UUID id, int displayOrder, String actor) {
        ContentDocument document = findDocument(id);
        document.setDisplayOrder(displayOrder);
        document.setUpdatedBy(actor);
        return toAdminResponse(documentRepository.saveAndFlush(document));
    }

    @Transactional
    public ContentDocumentResponse archive(UUID id, String actor) {
        ContentDocument document = findDocument(id);
        document.setStatus(ContentStatus.ARCHIVED);
        document.setArchivedAt(Instant.now());
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        audit(actor, "CONTENT_ARCHIVED", saved.getId(), "Content archived");
        return toAdminResponse(saved);
    }

    @Transactional
    public ContentDocumentResponse restoreArchived(UUID id, String actor) {
        ContentDocument document = findDocument(id);
        if (document.getStatus() != ContentStatus.ARCHIVED) {
            throw new ConflictException("CONTENT_NOT_ARCHIVED", "Only archived content can be restored");
        }
        document.setStatus(ContentStatus.DRAFT);
        document.setArchivedAt(null);
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        audit(actor, "CONTENT_RESTORED", saved.getId(), "Archived content restored as draft");
        return toAdminResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContentRevisionResponse> revisions(UUID documentId) {
        findDocument(documentId);
        return revisionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(this::toRevisionResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContentRevisionResponse revision(UUID documentId, UUID revisionId) {
        findDocument(documentId);
        return revisionRepository.findByIdAndDocumentId(revisionId, documentId)
                .map(this::toRevisionResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Content revision not found"));
    }

    @Transactional(readOnly = true)
    public Page<ContentRevisionResponse> searchRevisions(String query, UUID documentId, String createdBy,
                                                          String changeReason, Instant createdFrom, Instant createdTo,
                                                          int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        Specification<ContentRevision> spec = (root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (documentId != null) predicates.add(builder.equal(root.get("document").get("id"), documentId));
            if (query != null && !query.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("document").get("title")),
                        "%" + query.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("createdBy")), createdBy.trim().toLowerCase(Locale.ROOT)));
            }
            if (changeReason != null && !changeReason.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("changeReason")),
                        "%" + changeReason.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (createdFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return revisionRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toRevisionResponse);
    }

    @Transactional
    public ContentDocumentResponse restoreRevision(UUID documentId, UUID revisionId, String actor) {
        ContentDocument document = findDocument(documentId);
        ContentRevision revision = revisionRepository.findByIdAndDocumentId(revisionId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content revision not found"));
        validateMediaReferences(revision.getContentJson());
        document.setContentJson(revision.getContentJson().deepCopy());
        document.setUpdatedBy(actor);
        ContentDocument saved = documentRepository.saveAndFlush(document);
        saveRevision(saved, actor, "Restored revision " + revision.getVersionNumber());
        audit(actor, "CONTENT_REVISION_RESTORED", saved.getId(), "Revision " + revision.getVersionNumber() + " restored");
        return toAdminResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PublicContentResponse> listPublished() {
        List<ContentDocument> documents = documentRepository
                .findByStatusAndSectionEnabledTrueOrderBySectionDisplayOrderAscDisplayOrderAsc(ContentStatus.PUBLISHED)
                .stream().filter(document -> document.getPublishedContentJson() != null).toList();

        Map<UUID, Map<UUID, MediaResourceType>> referencesByDocument = new HashMap<>();
        Set<UUID> mediaIds = new HashSet<>();
        for (ContentDocument document : documents) {
            Map<UUID, MediaResourceType> references = richTextValidator
                    .validateAndExtractMedia(document.getPublishedContentJson());
            referencesByDocument.put(document.getId(), references);
            mediaIds.addAll(references.keySet());
        }

        Map<UUID, MediaAsset> mediaById = loadMedia(mediaIds);
        return documents.stream()
                .map(document -> toPublicResponse(document, referencesByDocument.get(document.getId()), mediaById))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicContentResponse getPublished(String slug) {
        ContentDocument document = documentRepository
                .findBySlugAndStatusAndSectionEnabledTrue(slug, ContentStatus.PUBLISHED)
                .filter(value -> value.getPublishedContentJson() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Published content not found"));
        Map<UUID, MediaResourceType> references = richTextValidator
                .validateAndExtractMedia(document.getPublishedContentJson());
        return toPublicResponse(document, references, loadMedia(references.keySet()));
    }

    private void validateMediaReferences(JsonNode content) {
        Map<UUID, MediaResourceType> references = richTextValidator.validateAndExtractMedia(content);
        for (Map.Entry<UUID, MediaResourceType> reference : references.entrySet()) {
            MediaAsset asset = mediaAssetRepository.findById(reference.getKey())
                    .orElseThrow(() -> new ConflictException("MEDIA_NOT_READY", "Referenced media asset does not exist: " + reference.getKey()));
            if (asset.getResourceType() != reference.getValue()) {
                throw new ConflictException("MEDIA_NOT_READY", "Referenced media type does not match asset: " + reference.getKey());
            }
        }
    }

    private void saveRevision(ContentDocument document, String actor, String changeReason) {
        if (document.getContentJson() == null || document.getId() == null) {
            return;
        }
        int nextVersion = revisionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(revision -> revision.getVersionNumber() + 1).orElse(1);
        ContentRevision revision = new ContentRevision();
        revision.setDocument(document);
        revision.setVersionNumber(nextVersion);
        revision.setContentJson(document.getContentJson().deepCopy());
        revision.setCreatedBy(actor);
        revision.setChangeReason(changeReason);
        revisionRepository.save(revision);
    }

    private ContentDocument findDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content document not found"));
    }

    private ContentDocumentResponse toAdminResponse(ContentDocument document) {
        return new ContentDocumentResponse(document.getId(), sectionService.toResponse(document.getSection()),
                document.getTitle(), document.getSlug(), copy(document.getContentJson()),
                copy(document.getPublishedContentJson()), document.getStatus(), document.getDisplayOrder(),
                document.getVersion(), document.getCreatedAt(), document.getUpdatedAt(), document.getPublishedAt(),
                document.getArchivedAt(), document.getCreatedBy(), document.getUpdatedBy(), document.getPublishedBy());
    }

    private PublicContentResponse toPublicResponse(ContentDocument document,
                                                   Map<UUID, MediaResourceType> references,
                                                   Map<UUID, MediaAsset> mediaById) {
        Section section = document.getSection();
        List<PublicMediaResponse> media = references.keySet().stream()
                .map(mediaById::get)
                .filter(java.util.Objects::nonNull)
                .map(asset -> new PublicMediaResponse(asset.getId(), asset.getResourceType(), asset.getMimeType(),
                        asset.getSecureUrl(), asset.getWidth(), asset.getHeight(), asset.getDuration()))
                .toList();
        return new PublicContentResponse(document.getId(), section.getSectionKey(), section.getName(),
                section.getDisplayOrder(), document.getTitle(), document.getSlug(),
                copy(document.getPublishedContentJson()), media, document.getDisplayOrder(), document.getPublishedAt());
    }

    private Map<UUID, MediaAsset> loadMedia(Set<UUID> mediaIds) {
        if (mediaIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, MediaAsset> mediaById = new HashMap<>();
        mediaAssetRepository.findAllById(mediaIds)
                .forEach(asset -> mediaById.put(asset.getId(), asset));
        return mediaById;
    }

    private ContentRevisionResponse toRevisionResponse(ContentRevision revision) {
        return new ContentRevisionResponse(revision.getId(), revision.getDocument().getId(),
                revision.getDocument().getTitle(),
                revision.getVersionNumber(), copy(revision.getContentJson()), revision.getCreatedAt(),
                revision.getCreatedBy(), revision.getChangeReason());
    }

    private JsonNode copy(JsonNode value) {
        return value == null ? null : value.deepCopy();
    }

    private void audit(String actor, String action, UUID id, String description) {
        if (auditLogService != null) auditLogService.record(actor, action, "CONTENT", id, description);
    }
}
