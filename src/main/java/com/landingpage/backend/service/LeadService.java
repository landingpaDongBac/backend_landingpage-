package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.LeadAdminRequest;
import com.landingpage.backend.api.dto.LeadResponse;
import com.landingpage.backend.api.dto.LeadStatusUpdateRequest;
import com.landingpage.backend.api.dto.PublicLeadRequest;
import com.landingpage.backend.api.dto.PublicLeadResponse;
import com.landingpage.backend.domain.Lead;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ResourceNotFoundException;
import com.landingpage.backend.repository.LeadRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "fullName", "phoneNumber", "cropType", "status", "source", "followUpAt");

    private final LeadRepository leadRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Transactional
    public PublicLeadResponse createPublic(PublicLeadRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            throw new BadRequestException("Invalid submission");
        }
        Lead lead = new Lead();
        lead.setFullName(clean(request.fullName()));
        lead.setPhoneNumber(clean(request.phoneNumber()));
        lead.setCropType(clean(request.cropType()));
        lead.setTreeCount(request.treeCount());
        lead.setGardenArea(cleanNullable(request.gardenArea()));
        lead.setConsultationMessage(cleanNullable(request.consultationMessage()));
        lead.setSource(request.source());
        lead.setStatus(LeadStatus.NEW);
        lead.setConsentAccepted(request.consentAccepted());
        Lead saved = leadRepository.save(lead);
        audit("LEAD_PUBLIC_CREATED", saved.getId(), "Public consultation request created");
        return new PublicLeadResponse(saved.getId(), "Consultation request received", saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> search(String query, String crop, LeadStatus status, LeadSource source, Boolean deleted,
                                     Instant createdFrom, Instant createdTo, int page, int size,
                                     String sortBy, Sort.Direction direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be at least 0 and size must be between 1 and 100");
        }
        String safeSort = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Specification<Lead> specification = (root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get("deleted"), deleted != null && deleted));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (source != null) {
                predicates.add(builder.equal(root.get("source"), source));
            }
            if (crop != null && !crop.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("cropType")),
                        "%" + crop.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (createdFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(builder.lower(root.get("phoneNumber")), pattern),
                        builder.like(builder.lower(root.get("cropType")), pattern),
                        builder.like(builder.lower(root.get("gardenArea")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return leadRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by(direction, safeSort)))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public LeadResponse createAdmin(LeadAdminRequest request) {
        Lead lead = new Lead();
        apply(lead, request);
        Lead saved = leadRepository.save(lead);
        audit("LEAD_CREATED", saved.getId(), "Lead created by administrator");
        return toResponse(saved);
    }

    @Transactional
    public LeadResponse update(UUID id, LeadAdminRequest request) {
        Lead lead = find(id);
        apply(lead, request);
        Lead saved = leadRepository.save(lead);
        audit("LEAD_UPDATED", saved.getId(), "Lead details updated");
        return toResponse(saved);
    }

    @Transactional
    public LeadResponse updateStatus(UUID id, LeadStatusUpdateRequest request) {
        Lead lead = find(id);
        lead.setStatus(request.status());
        lead.setAdminNotes(cleanNullable(request.adminNotes()));
        if (request.contactedAt() != null) {
            lead.setContactedAt(request.contactedAt());
        }
        lead.setFollowUpAt(request.followUpAt());
        if (request.status() == LeadStatus.CONTACTED && lead.getContactedAt() == null) {
            lead.setContactedAt(Instant.now());
        }
        Lead saved = leadRepository.save(lead);
        audit("LEAD_STATUS_UPDATED", saved.getId(), "Lead status updated to " + saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        Lead lead = find(id);
        lead.setDeleted(true);
        if (lead.getDeletedAt() == null) lead.setDeletedAt(Instant.now());
        leadRepository.save(lead);
        audit("LEAD_DELETED", lead.getId(), "Lead soft-deleted");
    }

    @Transactional
    public LeadResponse restore(UUID id) {
        Lead lead = find(id);
        lead.setDeleted(false);
        lead.setDeletedAt(null);
        Lead saved = leadRepository.save(lead);
        audit("LEAD_RESTORED", saved.getId(), "Lead restored");
        return toResponse(saved);
    }

    private Lead find(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
    }

    private void apply(Lead lead, LeadAdminRequest request) {
        lead.setFullName(clean(request.fullName()));
        lead.setPhoneNumber(clean(request.phoneNumber()));
        lead.setCropType(clean(request.cropType()));
        lead.setTreeCount(request.treeCount());
        lead.setGardenArea(cleanNullable(request.gardenArea()));
        lead.setConsultationMessage(cleanNullable(request.consultationMessage()));
        lead.setSource(request.source());
        lead.setStatus(request.status());
        lead.setAdminNotes(cleanNullable(request.adminNotes()));
        lead.setConsentAccepted(request.consentAccepted());
        if (request.contactedAt() != null) {
            lead.setContactedAt(request.contactedAt());
        }
        lead.setFollowUpAt(request.followUpAt());
        if (request.status() == LeadStatus.CONTACTED && lead.getContactedAt() == null) {
            lead.setContactedAt(Instant.now());
        }
    }

    private LeadResponse toResponse(Lead lead) {
        return new LeadResponse(lead.getId(), lead.getFullName(), lead.getPhoneNumber(), lead.getCropType(),
                lead.getTreeCount(), lead.getGardenArea(), lead.getConsultationMessage(), lead.getSource(),
                lead.getStatus(), lead.getAdminNotes(), lead.isConsentAccepted(), lead.getCreatedAt(),
                lead.getUpdatedAt(), lead.getContactedAt(), lead.getFollowUpAt(), lead.isDeleted(), lead.getDeletedAt());
    }

    private String clean(String value) {
        return value.trim();
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(String action, UUID id, String description) {
        if (auditLogService != null) auditLogService.recordCurrent(action, "LEAD", id, description);
    }
}
