package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landingpage.backend.api.dto.ContentDocumentRequest;
import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.ContentRevision;
import com.landingpage.backend.domain.ContentStatus;
import com.landingpage.backend.domain.MediaAsset;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.domain.Section;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.ContentRevisionRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import com.landingpage.backend.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock ContentDocumentRepository documentRepository;
    @Mock ContentRevisionRepository revisionRepository;
    @Mock SectionRepository sectionRepository;
    @Mock MediaAssetRepository mediaAssetRepository;
    @Mock RichTextDocumentValidator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ContentService service;
    private Section section;

    @BeforeEach
    void setUp() {
        section = new Section();
        section.setId(UUID.randomUUID());
        section.setSectionKey("05");
        section.setName("Product Benefits");
        section.setDisplayOrder(5);
        section.setEnabled(true);
        section.setCreatedAt(Instant.now());
        section.setUpdatedAt(Instant.now());
        service = new ContentService(documentRepository, revisionRepository, sectionRepository,
                mediaAssetRepository, validator, new SectionService(sectionRepository));
    }

    @Test
    void createsDraftAndInitialRevision() throws Exception {
        var content = objectMapper.readTree("{\"type\":\"doc\",\"content\":[]}");
        var request = new ContentDocumentRequest(section.getId(), "Benefits", "benefits", content, 1, null);
        when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(validator.validateAndExtractMedia(content)).thenReturn(Map.of());
        when(revisionRepository.findTopByDocumentIdOrderByVersionNumberDesc(any())).thenReturn(Optional.empty());
        when(documentRepository.saveAndFlush(any())).thenAnswer(invocation -> persist(invocation.getArgument(0)));

        var response = service.create(request, "editor@example.com");

        assertThat(response.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.contentJson()).isEqualTo(content);
        verify(revisionRepository).save(any(ContentRevision.class));
    }

    @Test
    void rejectsStaleEditorVersion() throws Exception {
        ContentDocument document = document("{\"type\":\"doc\",\"content\":[]}");
        document.setVersion(4);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        var request = new ContentDocumentRequest(section.getId(), "Benefits", "benefits",
                document.getContentJson(), 1, 3L);

        assertThatThrownBy(() -> service.update(document.getId(), request, "editor@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("another user");
    }

    @Test
    void publishingCopiesDraftIntoPublicSnapshot() throws Exception {
        ContentDocument document = document("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}");
        document.setPublishedContentJson(objectMapper.readTree("{\"type\":\"doc\",\"content\":[]}"));
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(validator.validateAndExtractMedia(document.getContentJson())).thenReturn(Map.of());
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        var response = service.publish(document.getId(), "editor@example.com");

        assertThat(response.status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.publishedContentJson()).isEqualTo(response.contentJson());
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    void publishingRejectsDisabledSection() throws Exception {
        ContentDocument document = document("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}");
        section.setEnabled(false);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.publish(document.getId(), "admin@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("disabled section");
    }

    @Test
    void loadsMediaForPublishedContentInOneBatch() throws Exception {
        ContentDocument first = document("{\"type\":\"doc\",\"content\":[{\"type\":\"image\"}]}");
        ContentDocument second = document("{\"type\":\"doc\",\"content\":[{\"type\":\"video\"}]}");
        first.setPublishedContentJson(first.getContentJson());
        second.setPublishedContentJson(second.getContentJson());
        first.setStatus(ContentStatus.PUBLISHED);
        second.setStatus(ContentStatus.PUBLISHED);
        UUID imageId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        MediaAsset image = media(imageId, MediaResourceType.IMAGE, "https://cdn.example/image.jpg");
        MediaAsset video = media(videoId, MediaResourceType.VIDEO, "https://cdn.example/video.mp4");

        when(documentRepository.findByStatusAndSectionEnabledTrueOrderBySectionDisplayOrderAscDisplayOrderAsc(
                ContentStatus.PUBLISHED)).thenReturn(List.of(first, second));
        when(validator.validateAndExtractMedia(first.getPublishedContentJson()))
                .thenReturn(Map.of(imageId, MediaResourceType.IMAGE));
        when(validator.validateAndExtractMedia(second.getPublishedContentJson()))
                .thenReturn(Map.of(videoId, MediaResourceType.VIDEO));
        when(mediaAssetRepository.findAllById(any())).thenReturn(List.of(image, video));

        var response = service.listPublished();

        assertThat(response).hasSize(2);
        assertThat(response).flatExtracting(item -> item.media()).hasSize(2);
        verify(mediaAssetRepository).findAllById(any());
    }

    private ContentDocument document(String json) throws Exception {
        ContentDocument document = new ContentDocument();
        document.setId(UUID.randomUUID());
        document.setSection(section);
        document.setTitle("Benefits");
        document.setSlug("benefits");
        document.setContentJson(objectMapper.readTree(json));
        document.setStatus(ContentStatus.DRAFT);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        return document;
    }

    private ContentDocument persist(ContentDocument document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID());
        }
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        return document;
    }

    private MediaAsset media(UUID id, MediaResourceType type, String url) {
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setResourceType(type);
        asset.setSecureUrl(url);
        return asset;
    }
}
