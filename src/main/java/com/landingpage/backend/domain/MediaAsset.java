package com.landingpage.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {

    @Id
    private UUID id;

    @Column(name = "cloudinary_public_id", nullable = false, unique = true, length = 500)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_asset_id")
    private String cloudinaryAssetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private MediaResourceType resourceType;

    @Column(length = 50)
    private String format;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "secure_url", nullable = false, columnDefinition = "text")
    private String secureUrl;

    @Column(name = "file_size")
    private Long fileSize;

    private Integer width;
    private Integer height;
    private Double duration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 320)
    private String createdBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
