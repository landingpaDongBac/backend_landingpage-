package com.landingpage.backend.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
public class SystemSettings {
    public static final UUID SINGLETON_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;
    @Column(name = "website_name") private String websiteName;
    @Column(name = "default_language", nullable = false, length = 20) private String defaultLanguage;
    @Column(name = "public_information", columnDefinition = "text") private String publicInformation;
    @Column(name = "support_phone", length = 50) private String supportPhone;
    @Column(name = "contact_email", length = 320) private String contactEmail;
    @Column(length = 1000) private String address;
    @Enumerated(EnumType.STRING) @Column(name = "default_lead_status", nullable = false, length = 50)
    private LeadStatus defaultLeadStatus;
    @Enumerated(EnumType.STRING) @Column(name = "default_lead_source", nullable = false, length = 50)
    private LeadSource defaultLeadSource;
    @Column(name = "publication_approval_required", nullable = false) private boolean publicationApprovalRequired;
    @Column(name = "preview_width", nullable = false) private int previewWidth;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "display_configuration", nullable = false, columnDefinition = "jsonb")
    private JsonNode displayConfiguration;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by", length = 320) private String updatedBy;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = SINGLETON_ID;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
