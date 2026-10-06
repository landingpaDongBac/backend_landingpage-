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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

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
                settings.getSupportPhone(), settings.getContactEmail(), settings.getAddress(), settings.getZaloUrl(),
                publicDisplayConfiguration(settings.getDisplayConfiguration(), settings.getZaloUrl()));
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
        updateZalo(settings, request);
        if (request.defaultLeadStatus() != null) settings.setDefaultLeadStatus(request.defaultLeadStatus());
        if (request.defaultLeadSource() != null) settings.setDefaultLeadSource(request.defaultLeadSource());
        if (request.publicationApprovalRequired() != null) {
            settings.setPublicationApprovalRequired(request.publicationApprovalRequired());
        }
        if (request.previewWidth() != null) settings.setPreviewWidth(request.previewWidth());
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
                settings.getAddress(), settings.getZaloUrl(), settings.getDefaultLeadStatus(), settings.getDefaultLeadSource(),
                settings.isPublicationApprovalRequired(), settings.getPreviewWidth(),
                configurationWithZalo(settings.getDisplayConfiguration(), settings.getZaloUrl()), infrastructure,
                settings.getUpdatedAt(), settings.getUpdatedBy());
    }

    private boolean databaseConnected() {
        try { return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class)); }
        catch (RuntimeException exception) { return false; }
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void updateZalo(SystemSettings settings, SystemSettingsRequest request) {
        boolean configurationProvided = request.displayConfiguration() != null;
        ObjectNode configuration = configurationProvided
                ? validatedConfiguration(request.displayConfiguration())
                : configurationWithZalo(settings.getDisplayConfiguration(), settings.getZaloUrl());

        if (request.zaloUrl() != null) {
            settings.setZaloUrl(normalizeZaloUrl(request.zaloUrl()));
        } else if (configurationProvided) {
            settings.setZaloUrl(normalizeZaloUrl(extractLegacyZaloUrl(configuration)));
        }

        if (configurationProvided || request.zaloUrl() != null) {
            settings.setDisplayConfiguration(configurationWithZalo(configuration, settings.getZaloUrl()));
        }
    }

    private ObjectNode validatedConfiguration(com.fasterxml.jackson.databind.JsonNode source) {
        if (!source.isObject() || source.toString().length() > 20_000) {
            throw new BadRequestException("Display configuration must be a JSON object of at most 20000 characters");
        }
        return (ObjectNode) source.deepCopy();
    }

    private String normalizeZaloUrl(String value) {
        String normalized = clean(value);
        if (normalized == null) return null;
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();
            boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean supportedHost = host != null && ("zalo.me".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".zalo.me"));
            if (!supportedScheme || !supportedHost || uri.getUserInfo() != null
                    || path == null || path.isBlank() || "/".equals(path)) {
                throw new BadRequestException("zaloUrl must be a valid http(s) URL on zalo.me");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new BadRequestException("zaloUrl must be a valid http(s) URL on zalo.me", exception);
        }
    }

    private String extractLegacyZaloUrl(com.fasterxml.jackson.databind.JsonNode source) {
        if (source == null || !source.isObject()) return null;
        com.fasterxml.jackson.databind.JsonNode zalo = source.get("zalo");
        if (zalo != null && zalo.isTextual()) return zalo.asText();
        if (zalo != null && zalo.isObject() && zalo.path("url").isTextual()) return zalo.path("url").asText();
        com.fasterxml.jackson.databind.JsonNode contactZalo = source.path("contact").path("zalo");
        return contactZalo.isTextual() ? contactZalo.asText() : null;
    }

    private ObjectNode configurationWithZalo(com.fasterxml.jackson.databind.JsonNode source, String zaloUrl) {
        ObjectNode configuration = source != null && source.isObject()
                ? (ObjectNode) source.deepCopy() : JsonNodeFactory.instance.objectNode();
        if (zaloUrl == null) {
            configuration.remove("zalo");
            if (configuration.path("contact").isObject()) {
                ((ObjectNode) configuration.path("contact")).remove("zalo");
            }
        } else {
            configuration.put("zalo", zaloUrl);
            if (configuration.path("contact").isObject()) {
                ((ObjectNode) configuration.path("contact")).put("zalo", zaloUrl);
            }
        }
        return configuration;
    }

    private ObjectNode publicDisplayConfiguration(com.fasterxml.jackson.databind.JsonNode source, String zaloUrl) {
        ObjectNode safe = JsonNodeFactory.instance.objectNode();
        if (source != null && source.isObject()) {
            for (String key : new String[] { "zalo", "contact", "socials", "links", "privacyUrl", "termsUrl", "seo", "campaign", "logoAlt" }) {
                if (source.has(key)) safe.set(key, source.get(key).deepCopy());
            }
        }
        return configurationWithZalo(safe, zaloUrl);
    }
}
