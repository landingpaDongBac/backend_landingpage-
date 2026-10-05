package com.landingpage.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.landingpage.backend.api.dto.MediaAssetResponse;
import com.landingpage.backend.api.dto.MediaConfirmRequest;
import com.landingpage.backend.api.dto.UploadSignatureResponse;
import com.landingpage.backend.domain.ContentDocument;
import com.landingpage.backend.domain.ContentRevision;
import com.landingpage.backend.domain.MediaAsset;
import com.landingpage.backend.domain.MediaResourceType;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.exception.ResourceNotFoundException;
import com.landingpage.backend.exception.ServiceUnavailableException;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.ContentRevisionRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm");

    private final Cloudinary cloudinary;
    private final MediaAssetRepository mediaRepository;
    private final ContentDocumentRepository documentRepository;
    private final ContentRevisionRepository revisionRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Value("${app.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.cloudinary.api-key}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret}")
    private String apiSecret;

    @Value("${app.cloudinary.folder}")
    private String folder;

    @Transactional
    public MediaAssetResponse upload(MultipartFile file, MediaResourceType resourceType, String actor) {
        ensureConfigured();
        byte[] bytes = readAndValidate(file, resourceType);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", resourceType.cloudinaryValue(),
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false));
            MediaAsset saved = mediaRepository.save(fromCloudinary(result, resourceType,
                    safeOriginalFilename(file.getOriginalFilename()), file.getContentType(), actor));
            audit(actor, "MEDIA_UPLOADED", saved.getId(), "Media uploaded to Cloudinary");
            return toResponse(saved);
        } catch (IOException exception) {
            throw new ServiceUnavailableException("Cloudinary upload failed", exception);
        }
    }

    public UploadSignatureResponse createUploadSignature(MediaResourceType resourceType) {
        ensureConfigured();
        long timestamp = Instant.now().getEpochSecond();
        List<String> allowedFormats = resourceType == MediaResourceType.IMAGE
                ? List.of("jpg", "jpeg", "png", "webp") : List.of("mp4", "mov", "webm");
        Map<String, Object> parameters = ObjectUtils.asMap(
                "timestamp", timestamp,
                "folder", folder,
                "allowed_formats", allowedFormats,
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false);
        String signature = cloudinary.apiSignRequest(parameters, apiSecret);
        return new UploadSignatureResponse(timestamp, signature, apiKey, cloudName, folder,
                resourceType.cloudinaryValue(), allowedFormats, true, true, false);
    }

    @Transactional
    public MediaAssetResponse confirmDirectUpload(MediaConfirmRequest request, String actor) {
        ensureConfigured();
        if (!(request.publicId().equals(folder) || request.publicId().startsWith(folder + "/"))) {
            throw new BadRequestException("Uploaded asset must belong to the configured Cloudinary folder");
        }
        return mediaRepository.findByCloudinaryPublicId(request.publicId())
                .map(this::toResponse)
                .orElseGet(() -> fetchAndSave(request, actor));
    }

    @Transactional(readOnly = true)
    public Page<MediaAssetResponse> list(String search, MediaResourceType resourceType, int page, int size,
                                          String sortBy, Sort.Direction direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be at least 0 and size must be between 1 and 100");
        }
        Set<String> allowedSorts = Set.of("createdAt", "originalFileName", "fileSize", "resourceType");
        String safeSort = allowedSorts.contains(sortBy) ? sortBy : "createdAt";
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, safeSort));
        return mediaRepository.findAll((root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (resourceType != null) {
                predicates.add(builder.equal(root.get("resourceType"), resourceType));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("originalFileName")), pattern),
                        builder.like(builder.lower(root.get("cloudinaryPublicId")), pattern),
                        builder.like(builder.lower(root.get("format")), pattern)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MediaAssetResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public com.landingpage.backend.api.dto.MediaUsageResponse usages(UUID id) {
        find(id);
        var usages = new java.util.ArrayList<com.landingpage.backend.api.dto.MediaUsageResponse.Usage>();
        for (ContentDocument document : documentRepository.findAll()) {
            if (containsMedia(document.getContentJson(), id)) {
                usages.add(new com.landingpage.backend.api.dto.MediaUsageResponse.Usage(
                        "DRAFT", document.getId(), document.getTitle(), null, null, document.getUpdatedAt()));
            }
            if (containsMedia(document.getPublishedContentJson(), id)) {
                usages.add(new com.landingpage.backend.api.dto.MediaUsageResponse.Usage(
                        "PUBLISHED", document.getId(), document.getTitle(), null, null, document.getPublishedAt()));
            }
        }
        for (ContentRevision revision : revisionRepository.findAll()) {
            if (containsMedia(revision.getContentJson(), id)) {
                usages.add(new com.landingpage.backend.api.dto.MediaUsageResponse.Usage(
                        "REVISION", revision.getDocument().getId(), revision.getDocument().getTitle(),
                        revision.getId(), revision.getVersionNumber(), revision.getCreatedAt()));
            }
        }
        return new com.landingpage.backend.api.dto.MediaUsageResponse(id, !usages.isEmpty(), List.copyOf(usages));
    }

    @Transactional
    public void delete(UUID id) {
        ensureConfigured();
        MediaAsset asset = find(id);
        if (isReferenced(id)) {
            throw new ConflictException("Media asset is referenced by content and cannot be deleted");
        }
        try {
            cloudinary.uploader().destroy(asset.getCloudinaryPublicId(), ObjectUtils.asMap(
                    "resource_type", asset.getResourceType().cloudinaryValue(), "invalidate", true));
            mediaRepository.delete(asset);
            audit(null, "MEDIA_DELETED", id, "Media deleted from Cloudinary and registry");
        } catch (IOException exception) {
            throw new ServiceUnavailableException("Cloudinary deletion failed", exception);
        }
    }

    private MediaAssetResponse fetchAndSave(MediaConfirmRequest request, String actor) {
        try {
            Map<?, ?> result = cloudinary.api().resource(request.publicId(), ObjectUtils.asMap(
                    "resource_type", request.resourceType().cloudinaryValue(), "type", "upload"));
            MediaAsset asset = fromCloudinary(result, request.resourceType(),
                    safeOriginalFilename(request.originalFileName()), null, actor);
            if (!asset.getCloudinaryPublicId().equals(request.publicId())) {
                throw new BadRequestException("Cloudinary asset identity mismatch");
            }
            MediaAsset saved = mediaRepository.save(asset);
            audit(actor, "MEDIA_CONFIRMED", saved.getId(), "Direct Cloudinary upload confirmed");
            return toResponse(saved);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServiceUnavailableException("Unable to verify Cloudinary upload", exception);
        }
    }

    private byte[] readAndValidate(MultipartFile file, MediaResourceType resourceType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A non-empty file is required");
        }
        String filename = safeOriginalFilename(file.getOriginalFilename());
        String extension = extension(filename);
        Set<String> allowed = resourceType == MediaResourceType.IMAGE ? IMAGE_EXTENSIONS : VIDEO_EXTENSIONS;
        if (!allowed.contains(extension)) {
            throw new BadRequestException("Unsupported " + resourceType.name().toLowerCase(Locale.ROOT) + " file extension");
        }
        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(bytes, extension)) {
                throw new BadRequestException("File content does not match its extension");
            }
            return bytes;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read uploaded file", exception);
        }
    }

    private boolean matchesSignature(byte[] bytes, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "webp" -> bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
            case "mp4", "mov" -> bytes.length >= 12 && ascii(bytes, 4, "ftyp");
            case "webm" -> startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3);
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        byte[] expected = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return bytes.length >= offset + expected.length
                && Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + expected.length), expected);
    }

    private MediaAsset fromCloudinary(Map<?, ?> result, MediaResourceType resourceType,
                                      String originalFilename, String suppliedMimeType, String actor) {
        Object publicId = result.get("public_id");
        Object secureUrl = result.get("secure_url");
        if (publicId == null || secureUrl == null) {
            throw new ServiceUnavailableException("Cloudinary response is missing required asset metadata");
        }
        MediaAsset asset = new MediaAsset();
        asset.setCloudinaryPublicId(publicId.toString());
        asset.setCloudinaryAssetId(stringValue(result.get("asset_id")));
        asset.setResourceType(resourceType);
        asset.setFormat(stringValue(result.get("format")));
        asset.setMimeType(suppliedMimeType == null ? mimeType(resourceType, asset.getFormat()) : suppliedMimeType);
        asset.setOriginalFileName(originalFilename);
        asset.setSecureUrl(secureUrl.toString());
        asset.setFileSize(longValue(result.get("bytes")));
        asset.setWidth(integerValue(result.get("width")));
        asset.setHeight(integerValue(result.get("height")));
        asset.setDuration(doubleValue(result.get("duration")));
        asset.setCreatedBy(actor);
        return asset;
    }

    private boolean isReferenced(UUID mediaId) {
        for (ContentDocument document : documentRepository.findAll()) {
            if (containsMedia(document.getContentJson(), mediaId)
                    || containsMedia(document.getPublishedContentJson(), mediaId)) {
                return true;
            }
        }
        for (ContentRevision revision : revisionRepository.findAll()) {
            if (containsMedia(revision.getContentJson(), mediaId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMedia(JsonNode node, UUID mediaId) {
        if (node == null) {
            return false;
        }
        if (node.isObject() && mediaId.toString().equals(node.path("attrs").path("mediaId").asText())) {
            return true;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                if (containsMedia(child, mediaId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MediaAsset find(UUID id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found"));
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(asset.getId(), asset.getCloudinaryPublicId(), asset.getCloudinaryPublicId(),
                asset.getCloudinaryAssetId(), asset.getResourceType(), asset.getFormat(), asset.getMimeType(),
                asset.getOriginalFileName(), asset.getSecureUrl(), asset.getFileSize(),
                asset.getWidth(), asset.getHeight(), asset.getDuration(), asset.getCreatedAt(), asset.getCreatedBy());
    }

    private void ensureConfigured() {
        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new ServiceUnavailableException("Cloudinary is not configured");
        }
    }

    private String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String safeOriginalFilename(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return filename.length() > 500 ? filename.substring(filename.length() - 500) : filename;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private String mimeType(MediaResourceType type, String format) {
        if (format == null) return null;
        String normalized = "jpg".equalsIgnoreCase(format) ? "jpeg" : format.toLowerCase(Locale.ROOT);
        return type.cloudinaryValue() + "/" + normalized;
    }

    private void audit(String actor, String action, UUID id, String description) {
        if (auditLogService != null) {
            if (actor == null) auditLogService.recordCurrent(action, "MEDIA", id, description);
            else auditLogService.record(actor, action, "MEDIA", id, description);
        }
    }
}
