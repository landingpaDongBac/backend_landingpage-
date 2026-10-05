package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.landingpage.backend.api.dto.PublicSiteSettingsResponse;
import com.landingpage.backend.api.dto.SystemSettingsRequest;
import com.landingpage.backend.api.dto.SystemSettingsResponse;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.domain.SystemSettings;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {
    private final SystemSettingsRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Value("${app.cloudinary.cloud-name:}") private String cloudName;
    @Value("${app.cloudinary.api-key:}") private String cloudinaryApiKey;
    @Value("${app.cloudinary.api-secret:}") private String cloudinaryApiSecret;

    @Transactional(readOnly = true)
    public SystemSettingsResponse get() {
        return toResponse(repository.findById(SystemSettings.SINGLETON_ID).orElseGet(this::defaults));
    }

    @Transactional(readOnly = true)
    public PublicSiteSettingsResponse getPublic() {
        SystemSettings settings = repository.findById(SystemSettings.SINGLETON_ID).orElseGet(this::defaults);
        return new PublicSiteSettingsResponse(settings.getWebsiteName(), settings.getPublicInformation(),
                settings.getSupportPhone(), settings.getContactEmail(), settings.getAddress(),
                publicDisplayConfiguration(settings.getDisplayConfiguration()));
    }

    @Transactional
    public SystemSettingsResponse update(SystemSettingsRequest request, String actor) {
        SystemSettings settings = repository.findById(SystemSettings.SINGLETON_ID).orElseGet(this::defaults);
        if (request.websiteName() != null) settings.setWebsiteName(clean(request.websiteName()));
        if (request.defaultLanguage() != null) settings.setDefaultLanguage(request.defaultLanguage());
        if (request.publicInformation() != null) settings.setPublicInformation(clean(request.publicInformation()));
        if (request.supportPhone() != null) settings.setSupportPhone(clean(request.supportPhone()));
        if (request.contactEmail() != null) settings.setContactEmail(clean(request.contactEmail()));
        if (request.address() != null) settings.setAddress(clean(request.address()));
        if (request.defaultLeadStatus() != null) settings.setDefaultLeadStatus(request.defaultLeadStatus());
        if (request.defaultLeadSource() != null) settings.setDefaultLeadSource(request.defaultLeadSource());
        if (request.publicationApprovalRequired() != null) {
            settings.setPublicationApprovalRequired(request.publicationApprovalRequired());
        }
        if (request.previewWidth() != null) settings.setPreviewWidth(request.previewWidth());
        if (request.displayConfiguration() != null) {
            if (!request.displayConfiguration().isObject() || request.displayConfiguration().toString().length() > 20_000) {
                throw new BadRequestException("Display configuration must be a JSON object of at most 20000 characters");
            }
            settings.setDisplayConfiguration(request.displayConfiguration().deepCopy());
        }
        settings.setUpdatedBy(actor);
        SystemSettings saved = repository.save(settings);
        auditLogService.record(actor, "SETTINGS_UPDATED", "SYSTEM_SETTINGS", saved.getId(), "System settings updated");
        return toResponse(saved);
    }

    private SystemSettings defaults() {
        SystemSettings settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setDefaultLanguage("vi");
        settings.setDefaultLeadStatus(LeadStatus.NEW);
        settings.setDefaultLeadSource(LeadSource.OTHER);
        settings.setPublicationApprovalRequired(true);
        settings.setPreviewWidth(1200);
        settings.setDisplayConfiguration(JsonNodeFactory.instance.objectNode());
        return settings;
    }

    private SystemSettingsResponse toResponse(SystemSettings settings) {
        boolean cloudinaryConfigured = present(cloudName) && present(cloudinaryApiKey) && present(cloudinaryApiSecret);
        var infrastructure = new SystemSettingsResponse.InfrastructureStatus(
                databaseConnected(), cloudinaryConfigured, cloudinaryConfigured);
        return new SystemSettingsResponse(settings.getWebsiteName(), settings.getDefaultLanguage(),
                settings.getPublicInformation(), settings.getSupportPhone(), settings.getContactEmail(),
                settings.getAddress(), settings.getDefaultLeadStatus(), settings.getDefaultLeadSource(),
                settings.isPublicationApprovalRequired(), settings.getPreviewWidth(),
                settings.getDisplayConfiguration() == null ? JsonNodeFactory.instance.objectNode()
                        : settings.getDisplayConfiguration().deepCopy(), infrastructure,
                settings.getUpdatedAt(), settings.getUpdatedBy());
    }

    private boolean databaseConnected() {
        try { return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class)); }
        catch (RuntimeException exception) { return false; }
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private ObjectNode publicDisplayConfiguration(com.fasterxml.jackson.databind.JsonNode source) {
        ObjectNode safe = JsonNodeFactory.instance.objectNode();
        if (source == null || !source.isObject()) return safe;
        for (String key : new String[] { "zalo", "contact", "socials", "links", "privacyUrl", "termsUrl", "seo", "campaign", "logoAlt" }) {
            if (source.has(key)) safe.set(key, source.get(key).deepCopy());
        }
        return safe;
    }
}
