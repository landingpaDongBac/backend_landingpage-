package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RichTextDocumentValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RichTextDocumentValidator validator = new RichTextDocumentValidator(32_768, 12);

    @Test
    void acceptsTiptapDocumentAndExtractsMediaReferences() throws Exception {
        UUID imageId = UUID.randomUUID();
        var document = objectMapper.readTree("""
                {
                  "type": "doc",
                  "content": [
                    {"type": "heading", "attrs": {"level": 2}, "content": [{"type": "text", "text": "Hello"}]},
                    {"type": "image", "attrs": {"mediaId": "%s", "alt": "Product"}}
                  ]
                }
                """.formatted(imageId));

        Map<UUID, MediaResourceType> references = validator.validateAndExtractMedia(document);

        assertThat(references).containsEntry(imageId, MediaResourceType.IMAGE);
    }

    @Test
    void rejectsScriptPayloads() throws Exception {
        var document = objectMapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"title":"<script>alert(1)</script>"}}]}
                """);

        assertThatThrownBy(() -> validator.validateAndExtractMedia(document))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsafe");
    }

    @Test
    void rejectsExternalMediaWithoutManagedId() throws Exception {
        var document = objectMapper.readTree("""
                {"type":"doc","content":[{"type":"image","attrs":{"src":"https://example.com/image.jpg"}}]}
                """);

        assertThatThrownBy(() -> validator.validateAndExtractMedia(document))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mediaId");
    }

    @Test
    void acceptsFrontendMediaImageAndMediaVideoNodes() throws Exception {
        UUID imageId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        var document = objectMapper.readTree("""
                {"type":"doc","content":[
                  {"type":"mediaImage","attrs":{"mediaId":"%s","width":640}},
                  {"type":"paragraph","content":[{"type":"text","text":"Between media"}]},
                  {"type":"mediaVideo","attrs":{"mediaId":"%s","width":960}}
                ]}
                """.formatted(imageId, videoId));

        Map<UUID, MediaResourceType> references = validator.validateAndExtractMedia(document);

        assertThat(references).containsEntry(imageId, MediaResourceType.IMAGE)
                .containsEntry(videoId, MediaResourceType.VIDEO);
    }
}
