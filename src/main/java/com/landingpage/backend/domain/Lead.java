package com.landingpage.backend.domain;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
public class Lead {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "crop_type", nullable = false)
    private String cropType;

    @Column(name = "tree_count")
    private Integer treeCount;

    @Column(name = "garden_area")
    private String gardenArea;

    @Column(name = "consultation_message", length = 2000)
    private String consultationMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadStatus status;

    @Column(name = "admin_notes", columnDefinition = "text")
    private String adminNotes;

    @Column(name = "consent_accepted", nullable = false)
    private boolean consentAccepted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "contacted_at")
    private Instant contactedAt;

    @Column(name = "follow_up_at")
    private Instant followUpAt;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = LeadStatus.NEW;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
