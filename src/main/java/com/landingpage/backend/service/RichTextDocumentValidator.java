package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RichTextDocumentValidator {

    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "doc", "paragraph", "text", "heading", "bulletList", "orderedList", "listItem",
            "blockquote", "codeBlock", "hardBreak", "horizontalRule", "image", "video", "mediaImage", "mediaVideo",
            "table", "tableRow", "tableHeader", "tableCell", "figure", "caption", "callout", "customBlock");
    private static final Set<String> ALLOWED_MARK_TYPES = Set.of(
            "bold", "italic", "underline", "strike", "textStyle", "highlight", "link",
            "subscript", "superscript", "code");
    private static final Set<String> URL_ATTRIBUTES = Set.of("href", "src", "url", "poster");

    private final int maxBytes;
    private final int maxDepth;

    public RichTextDocumentValidator(
            @Value("${app.rich-text.max-bytes}") int maxBytes,
            @Value("${app.rich-text.max-depth}") int maxDepth) {
        this.maxBytes = maxBytes;
        this.maxDepth = maxDepth;
    }

    public Map<UUID, MediaResourceType> validateAndExtractMedia(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new BadRequestException("Rich-text content must be a JSON object");
        }
        if (document.toString().getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new BadRequestException("Rich-text content exceeds the configured size limit");
        }
        if (!"doc".equals(document.path("type").asText())) {
            throw new BadRequestException("Rich-text root node must have type 'doc'");
        }
        Map<UUID, MediaResourceType> references = new HashMap<>();
        validateNode(document, 0, references);
        return Map.copyOf(references);
    }

    private void validateNode(JsonNode node, int depth, Map<UUID, MediaResourceType> references) {
        if (depth > maxDepth) {
            throw new BadRequestException("Rich-text content is nested too deeply");
        }
        if (!node.isObject()) {
            throw new BadRequestException("Rich-text nodes must be JSON objects");
        }
        String type = node.path("type").asText("");
        if (!ALLOWED_NODE_TYPES.contains(type)) {
            throw new BadRequestException("Unsupported rich-text node type: " + type);
        }
        validateObjectSafety(node, depth);

        if ("heading".equals(type)) {
            int level = node.path("attrs").path("level").asInt(0);
            if (level < 1 || level > 6) {
                throw new BadRequestException("Heading level must be between 1 and 6");
            }
        }

        JsonNode marks = node.get("marks");
        if (marks != null) {
            if (!marks.isArray()) {
                throw new BadRequestException("Rich-text marks must be an array");
            }
            for (JsonNode mark : marks) {
                String markType = mark.path("type").asText("");
                if (!ALLOWED_MARK_TYPES.contains(markType)) {
                    throw new BadRequestException("Unsupported rich-text mark type: " + markType);
                }
                validateObjectSafety(mark, depth + 1);
            }
        }

        if (isImage(type) || isVideo(type)) {
            String mediaId = node.path("attrs").path("mediaId").asText("");
            try {
                references.put(UUID.fromString(mediaId),
                        isImage(type) ? MediaResourceType.IMAGE : MediaResourceType.VIDEO);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException(type + " nodes must reference a valid mediaId");
            }
        }

        JsonNode content = node.get("content");
        if (content != null) {
            if (!content.isArray()) {
                throw new BadRequestException("Rich-text node content must be an array");
            }
            for (JsonNode child : content) {
                validateNode(child, depth + 1, references);
            }
        }
    }

    private boolean isImage(String type) {
        return "image".equals(type) || "mediaImage".equals(type);
    }

    private boolean isVideo(String type) {
        return "video".equals(type) || "mediaVideo".equals(type);
    }

    private void validateObjectSafety(JsonNode object, int depth) {
        if (depth > maxDepth) {
            throw new BadRequestException("Rich-text content is nested too deeply");
        }
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();
            if ("content".equals(key) || "marks".equals(key)) {
                continue;
            }
            if (key.toLowerCase().startsWith("on")) {
                throw new BadRequestException("Event-handler attributes are not allowed in rich text");
            }
            if (value.isTextual()) {
                String text = value.asText().trim().toLowerCase();
                if (text.contains("<script") || text.contains("</script") || text.startsWith("javascript:")
                        || text.startsWith("data:text/html")) {
                    throw new BadRequestException("Unsafe rich-text value detected");
                }
                if (URL_ATTRIBUTES.contains(key) && !(text.startsWith("https://") || text.startsWith("http://")
                        || text.startsWith("/") || text.startsWith("#") || text.startsWith("mailto:")
                        || text.startsWith("tel:") || text.isBlank())) {
                    throw new BadRequestException("Unsupported URL scheme in rich text");
                }
            } else if (value.isObject()) {
                validateObjectSafety(value, depth + 1);
            } else if (value.isArray()) {
                for (JsonNode child : value) {
                    if (child.isObject()) {
                        validateObjectSafety(child, depth + 1);
                    }
                }
            }
        }
    }
}
