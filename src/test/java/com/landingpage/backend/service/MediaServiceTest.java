package com.landingpage.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Api;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landingpage.backend.api.dto.MediaConfirmRequest;
import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.MediaAsset;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.domain.ContentRevision;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.exception.MultimediaUploadException;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock Cloudinary cloudinary;
    @Mock Api api;
    @Mock ApiResponse apiResponse;
    @Mock Uploader uploader;
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
    void wrapsCloudinaryRuntimeFailureInSafeUploadException() throws Exception {
        var file = new MockMultipartFile("file", "product.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IllegalStateException("provider failure"));

        assertThatThrownBy(() -> service.upload(file, MediaResourceType.IMAGE, "editor@example.com"))
                .isInstanceOf(MultimediaUploadException.class)
                .hasMessageContaining("Cloudinary upload failed");
    }

    @Test
    void streamsValidImageAndPersistsTrustedCloudinaryMetadata() throws Exception {
        var file = new MockMultipartFile("file", "product.png", "application/octet-stream",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "public_id", "agricultural-landing/product",
                "secure_url", "https://res.cloudinary.com/test/image/upload/product.png",
                "resource_type", "image",
                "format", "png",
                "bytes", 12));
        when(mediaRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        var response = service.upload(file, MediaResourceType.IMAGE, "editor@example.com");

        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(response.secureUrl()).startsWith("https://");
        verify(mediaRepository).saveAndFlush(any(MediaAsset.class));
    }

    @Test
    void confirmsDirectUploadOnlyAfterFetchingTrustedCloudinaryMetadata() throws Exception {
        String publicId = "agricultural-landing/direct-product";
        when(mediaRepository.findByCloudinaryPublicId(publicId)).thenReturn(Optional.empty());
        when(cloudinary.api()).thenReturn(api);
        when(api.resource(org.mockito.ArgumentMatchers.eq(publicId), anyMap())).thenReturn(apiResponse);
        Map<String, Object> metadata = Map.of(
                "public_id", publicId,
                "secure_url", "https://res.cloudinary.com/test/image/upload/direct-product.png",
                "resource_type", "image",
                "format", "png",
                "bytes", 128L);
        when(apiResponse.get(any())).thenAnswer(invocation -> metadata.get(invocation.getArgument(0)));
        when(mediaRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        var response = service.confirmDirectUpload(
                new MediaConfirmRequest(publicId, MediaResourceType.IMAGE, "direct-product.png"),
                "editor@example.com");

        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.mimeType()).isEqualTo("image/png");
        verify(api).resource(org.mockito.ArgumentMatchers.eq(publicId), anyMap());
        verify(mediaRepository).saveAndFlush(any(MediaAsset.class));
    }

    @Test
    void repeatedDirectUploadConfirmationReturnsExistingRegistryEntry() {
        String publicId = "agricultural-landing/existing";
        MediaAsset existing = new MediaAsset();
        existing.setId(UUID.randomUUID());
        existing.setCloudinaryPublicId(publicId);
        existing.setResourceType(MediaResourceType.IMAGE);
        existing.setSecureUrl("https://res.cloudinary.com/test/image/upload/existing.png");
        when(mediaRepository.findByCloudinaryPublicId(publicId)).thenReturn(Optional.of(existing));

        var response = service.confirmDirectUpload(
                new MediaConfirmRequest(publicId, MediaResourceType.IMAGE, "existing.png"),
                "editor@example.com");

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(cloudinary, never()).api();
        verify(mediaRepository, never()).saveAndFlush(any());
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
