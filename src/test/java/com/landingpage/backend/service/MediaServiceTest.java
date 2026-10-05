package com.landingpage.backend.service;

import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.MediaAsset;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.domain.ContentRevision;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.ContentRevisionRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock Cloudinary cloudinary;
    @Mock MediaAssetRepository mediaRepository;
    @Mock ContentDocumentRepository documentRepository;
    @Mock ContentRevisionRepository revisionRepository;

    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(cloudinary, mediaRepository, documentRepository, revisionRepository);
        ReflectionTestUtils.setField(service, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiSecret", "test-secret");
        ReflectionTestUtils.setField(service, "folder", "agricultural-landing");
    }

    @Test
    void rejectsFileWhoseBytesDoNotMatchExtension() {
        var file = new MockMultipartFile("file", "product.png", "image/png", "not-a-png".getBytes());

        assertThatThrownBy(() -> service.upload(file, MediaResourceType.IMAGE, "editor@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void preventsDeletionWhenDraftReferencesMedia() throws Exception {
        UUID id = UUID.randomUUID();
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setCloudinaryPublicId("agricultural-landing/product");
        asset.setResourceType(MediaResourceType.IMAGE);
        ContentDocument document = new ContentDocument();
        document.setContentJson(new ObjectMapper().readTree("""
                {"type":"doc","content":[{"type":"image","attrs":{"mediaId":"%s"}}]}
                """.formatted(id)));
        when(mediaRepository.findById(id)).thenReturn(Optional.of(asset));
        when(documentRepository.findAll()).thenReturn(List.of(document));

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("referenced");
        verify(mediaRepository, never()).delete(asset);
    }

    @Test
    void signatureResponseRestrictsImageFormats() {
        when(cloudinary.apiSignRequest(org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq("test-secret"))).thenReturn("signature");

        var response = service.createUploadSignature(MediaResourceType.IMAGE);

        assertThat(response.signature()).isEqualTo("signature");
        assertThat(response.allowedFormats()).containsExactly("jpg", "jpeg", "png", "webp");
        assertThat(response.overwrite()).isFalse();
    }

    @Test
    void usagesReportDraftPublishedAndRevisionReferences() throws Exception {
        UUID id = UUID.randomUUID();
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        ContentDocument document = new ContentDocument();
        document.setId(UUID.randomUUID());
        document.setTitle("Product");
        document.setContentJson(new ObjectMapper().readTree(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"mediaImage\",\"attrs\":{\"mediaId\":\"" + id + "\"}}]}"));
        document.setPublishedContentJson(document.getContentJson());
        ContentRevision revision = new ContentRevision();
        revision.setId(UUID.randomUUID());
        revision.setDocument(document);
        revision.setVersionNumber(1);
        revision.setContentJson(document.getContentJson());
        when(mediaRepository.findById(id)).thenReturn(Optional.of(asset));
        when(documentRepository.findAll()).thenReturn(List.of(document));
        when(revisionRepository.findAll()).thenReturn(List.of(revision));

        var result = service.usages(id);

        assertThat(result.inUse()).isTrue();
        assertThat(result.usages()).extracting(usage -> usage.usageType())
                .containsExactly("DRAFT", "PUBLISHED", "REVISION");
    }
}
