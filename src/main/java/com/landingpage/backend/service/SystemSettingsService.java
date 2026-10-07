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
        String supportHours = resolvedSupportHours(settings);
        String facebookUrl = resolvedFacebookUrl(settings);
        return new PublicSiteSettingsResponse(settings.getWebsiteName(), settings.getPublicInformation(),
                settings.getSupportPhone(), settings.getContactEmail(), settings.getAddress(), supportHours,
                settings.getZaloUrl(), facebookUrl,
                publicDisplayConfiguration(settings.getDisplayConfiguration(), settings.getZaloUrl(),
                        supportHours, facebookUrl));
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
        updatePublicContactFields(settings, request);
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
        String supportHours = resolvedSupportHours(settings);
        String facebookUrl = resolvedFacebookUrl(settings);
        return new SystemSettingsResponse(settings.getWebsiteName(), settings.getDefaultLanguage(),
                settings.getPublicInformation(), settings.getSupportPhone(), settings.getContactEmail(),
                settings.getAddress(), supportHours, settings.getZaloUrl(), facebookUrl,
                settings.getDefaultLeadStatus(), settings.getDefaultLeadSource(),
                settings.isPublicationApprovalRequired(), settings.getPreviewWidth(),
                configurationWithPublicContacts(settings.getDisplayConfiguration(), settings.getZaloUrl(),
                        supportHours, facebookUrl), infrastructure,
                settings.getUpdatedAt(), settings.getUpdatedBy());
    }

    private boolean databaseConnected() {
        try { return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class)); }
        catch (RuntimeException exception) { return false; }
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void updatePublicContactFields(SystemSettings settings, SystemSettingsRequest request) {
        ObjectNode existingConfiguration = configurationObject(settings.getDisplayConfiguration());
        boolean configurationProvided = request.displayConfiguration() != null;
        ObjectNode configuration = configurationProvided
                ? validatedConfiguration(request.displayConfiguration())
                : existingConfiguration;

        if (request.supportHours() != null) {
            settings.setSupportHours(clean(request.supportHours()));
        } else if (settings.getSupportHours() == null) {
            com.fasterxml.jackson.databind.JsonNode fallbackSource =
                    configurationProvided && hasLegacySupportHours(configuration)
                            ? configuration : existingConfiguration;
            settings.setSupportHours(clean(extractLegacySupportHours(fallbackSource)));
        }

        if (request.facebookUrl() != null) {
            settings.setFacebookUrl(normalizeFacebookUrl(request.facebookUrl()));
        } else if (settings.getFacebookUrl() == null) {
            com.fasterxml.jackson.databind.JsonNode fallbackSource =
                    configurationProvided && hasLegacyFacebookUrl(configuration)
                            ? configuration : existingConfiguration;
            settings.setFacebookUrl(normalizeLegacyFacebookUrl(extractLegacyFacebookUrl(fallbackSource)));
        }

        if (request.zaloUrl() != null) {
            settings.setZaloUrl(normalizeZaloUrl(request.zaloUrl()));
        } else if (configurationProvided && hasLegacyZaloUrl(configuration)) {
            settings.setZaloUrl(normalizeZaloUrl(extractLegacyZaloUrl(configuration)));
        }

        settings.setDisplayConfiguration(configurationWithPublicContacts(configuration, settings.getZaloUrl(),
                settings.getSupportHours(), settings.getFacebookUrl()));
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

    private String normalizeFacebookUrl(String value) {
        String normalized = clean(value);
        if (normalized == null) return null;
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean supportedHost = host != null && ("facebook.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".facebook.com")
                    || "fb.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".fb.com"));
            if (!supportedScheme || !supportedHost || uri.getUserInfo() != null) {
                throw new BadRequestException("facebookUrl must be a valid http(s) Facebook URL");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new BadRequestException("facebookUrl must be a valid http(s) Facebook URL", exception);
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

    private boolean hasLegacyZaloUrl(com.fasterxml.jackson.databind.JsonNode source) {
        if (source == null || !source.isObject()) return false;
        com.fasterxml.jackson.databind.JsonNode zalo = source.get("zalo");
        if (zalo != null && (zalo.isTextual()
                || zalo.isObject() && zalo.path("url").isTextual())) return true;
        return source.path("contact").path("zalo").isTextual();
    }

    private String extractLegacySupportHours(com.fasterxml.jackson.databind.JsonNode source) {
        com.fasterxml.jackson.databind.JsonNode value = source == null
                ? null : source.path("contact").get("supportHours");
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private boolean hasLegacySupportHours(com.fasterxml.jackson.databind.JsonNode source) {
        com.fasterxml.jackson.databind.JsonNode value = source == null
                ? null : source.path("contact").get("supportHours");
        return value != null && value.isTextual();
    }

    private String extractLegacyFacebookUrl(com.fasterxml.jackson.databind.JsonNode source) {
        com.fasterxml.jackson.databind.JsonNode value = source == null
                ? null : source.path("socials").get("facebook");
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private boolean hasLegacyFacebookUrl(com.fasterxml.jackson.databind.JsonNode source) {
        com.fasterxml.jackson.databind.JsonNode value = source == null
                ? null : source.path("socials").get("facebook");
        return value != null && value.isTextual();
    }

    private String resolvedSupportHours(SystemSettings settings) {
        return settings.getSupportHours() != null
                ? settings.getSupportHours()
                : clean(extractLegacySupportHours(settings.getDisplayConfiguration()));
    }

    private String resolvedFacebookUrl(SystemSettings settings) {
        return settings.getFacebookUrl() != null
                ? settings.getFacebookUrl()
                : normalizeLegacyFacebookUrl(extractLegacyFacebookUrl(settings.getDisplayConfiguration()));
    }

    private String normalizeLegacyFacebookUrl(String value) {
        try {
            return normalizeFacebookUrl(value);
        } catch (BadRequestException exception) {
            return null;
        }
    }

    private ObjectNode configurationObject(com.fasterxml.jackson.databind.JsonNode source) {
        return source != null && source.isObject()
                ? (ObjectNode) source.deepCopy() : JsonNodeFactory.instance.objectNode();
    }

    private ObjectNode configurationWithPublicContacts(com.fasterxml.jackson.databind.JsonNode source, String zaloUrl,
                                                       String supportHours, String facebookUrl) {
        ObjectNode configuration = configurationObject(source);
        if (zaloUrl == null) {
            configuration.remove("zalo");
        } else {
            configuration.put("zalo", zaloUrl);
        }

        ObjectNode contact = childObject(configuration, "contact",
                zaloUrl != null || supportHours != null);
        synchronizeText(contact, "zalo", zaloUrl);
        synchronizeText(contact, "supportHours", supportHours);

        ObjectNode socials = childObject(configuration, "socials", facebookUrl != null);
        synchronizeText(socials, "facebook", facebookUrl);
        return configuration;
    }

    private ObjectNode childObject(ObjectNode parent, String field, boolean create) {
        if (parent.path(field).isObject()) return (ObjectNode) parent.path(field);
        if (!create) return null;
        ObjectNode child = JsonNodeFactory.instance.objectNode();
        parent.set(field, child);
        return child;
    }

    private void synchronizeText(ObjectNode object, String field, String value) {
        if (object == null) return;
        if (value == null) object.remove(field);
        else object.put(field, value);
    }

    private ObjectNode publicDisplayConfiguration(com.fasterxml.jackson.databind.JsonNode source, String zaloUrl,
                                                  String supportHours, String facebookUrl) {
        ObjectNode safe = JsonNodeFactory.instance.objectNode();
        if (source != null && source.isObject()) {
            for (String key : new String[] { "zalo", "contact", "socials", "links", "privacyUrl", "termsUrl", "seo", "campaign", "logoAlt" }) {
                if (source.has(key)) safe.set(key, source.get(key).deepCopy());
            }
        }
        return configurationWithPublicContacts(safe, zaloUrl, supportHours, facebookUrl);
    }
}
