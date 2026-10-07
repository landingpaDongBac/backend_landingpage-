package com.landingpage.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landingpage.backend.api.dto.SystemSettingsRequest;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.domain.SystemSettings;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.repository.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    @Mock SystemSettingsRepository repository;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SystemSettingsService service;
    private SystemSettings settings;

    @BeforeEach
    void setUp() {
        service = new SystemSettingsService(repository, jdbcTemplate, auditLogService);
        settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setDefaultLanguage("vi");
        settings.setDefaultLeadStatus(LeadStatus.NEW);
        settings.setDefaultLeadSource(LeadSource.OTHER);
        settings.setPublicationApprovalRequired(true);
        settings.setPreviewWidth(1200);
        settings.setDisplayConfiguration(objectMapper.createObjectNode());
        when(repository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
    }

    @Test
    void storesAndReturnsTypedZaloUrlWhileKeepingLegacyConfigurationCompatible() {
        var response = service.update(request("https://zalo.me/0901234567", null), "admin@example.com");

        assertThat(settings.getZaloUrl()).isEqualTo("https://zalo.me/0901234567");
        assertThat(response.zaloUrl()).isEqualTo("https://zalo.me/0901234567");
        assertThat(response.displayConfiguration().path("zalo").asText())
                .isEqualTo("https://zalo.me/0901234567");
        assertThat(service.getPublic().zaloUrl()).isEqualTo("https://zalo.me/0901234567");
    }

    @Test
    void acceptsLegacyDisplayConfigurationAndCopiesZaloIntoTypedField() throws Exception {
        var configuration = objectMapper.readTree("{\"zalo\":{\"url\":\"https://zalo.me/oa/example\"},\"seo\":{}}");

        service.update(request(null, configuration), "admin@example.com");

        assertThat(settings.getZaloUrl()).isEqualTo("https://zalo.me/oa/example");
        assertThat(settings.getDisplayConfiguration().path("zalo").asText())
                .isEqualTo("https://zalo.me/oa/example");
    }

    @Test
    void rejectsNonZaloHost() {
        assertThatThrownBy(() -> service.update(request("https://example.com/profile", null), "admin@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("zalo.me");
    }

    @Test
    void blankZaloUrlClearsTypedAndLegacyValues() {
        settings.setZaloUrl("https://zalo.me/0901234567");

        var response = service.update(request(" ", null), "admin@example.com");

        assertThat(response.zaloUrl()).isNull();
        assertThat(response.displayConfiguration().has("zalo")).isFalse();
    }

    @Test
    void persistsAndReturnsAllPublicContactFieldsWithoutMixingFacebookAndZalo() {
        settings.setSupportPhone("0901234567");
        settings.setAddress("Dak Lak");
        settings.setZaloUrl("https://zalo.me/0901234567");

        var response = service.update(new SystemSettingsRequest(
                null, null, null, null, "support@example.com", null,
                "Thứ Hai - Thứ Bảy, 08:00 - 17:00", null,
                "https://www.facebook.com/agricultural-landing",
                null, null, null, null, null), "admin@example.com");

        assertThat(response.supportPhone()).isEqualTo("0901234567");
        assertThat(response.contactEmail()).isEqualTo("support@example.com");
        assertThat(response.address()).isEqualTo("Dak Lak");
        assertThat(response.supportHours()).isEqualTo("Thứ Hai - Thứ Bảy, 08:00 - 17:00");
        assertThat(response.zaloUrl()).isEqualTo("https://zalo.me/0901234567");
        assertThat(response.facebookUrl()).isEqualTo("https://www.facebook.com/agricultural-landing");
        assertThat(response.displayConfiguration().path("contact").path("supportHours").asText())
                .isEqualTo("Thứ Hai - Thứ Bảy, 08:00 - 17:00");
        assertThat(response.displayConfiguration().path("socials").path("facebook").asText())
                .isEqualTo("https://www.facebook.com/agricultural-landing");

        var publicResponse = service.getPublic();
        assertThat(publicResponse.contactEmail()).isEqualTo("support@example.com");
        assertThat(publicResponse.supportHours()).isEqualTo("Thứ Hai - Thứ Bảy, 08:00 - 17:00");
        assertThat(publicResponse.facebookUrl()).isNotEqualTo(publicResponse.zaloUrl());
    }

    @Test
    void readsLegacyNestedContactValuesAsTopLevelFallbacks() throws Exception {
        settings.setDisplayConfiguration(objectMapper.readTree("""
                {
                  "contact": {"supportHours": "Thứ 2 - Chủ Nhật"},
                  "socials": {"facebook": "https://www.facebook.com/cho.giong.2025"}
                }
                """));

        var adminResponse = service.get();
        var publicResponse = service.getPublic();

        assertThat(adminResponse.supportHours()).isEqualTo("Thứ 2 - Chủ Nhật");
        assertThat(adminResponse.facebookUrl()).isEqualTo("https://www.facebook.com/cho.giong.2025");
        assertThat(publicResponse.supportHours()).isEqualTo("Thứ 2 - Chủ Nhật");
        assertThat(publicResponse.facebookUrl()).isEqualTo("https://www.facebook.com/cho.giong.2025");
    }

    @Test
    void legacyNestedPatchPersistsFirstClassFieldsAndKeepsConfigurationSynchronized() throws Exception {
        settings.setZaloUrl("https://zalo.me/0901234567");
        var configuration = objectMapper.readTree("""
                {
                  "contact": {"supportHours": "Thứ 2 - Chủ Nhật"},
                  "socials": {"facebook": "https://www.facebook.com/cho.giong.2025"}
                }
                """);

        var response = service.update(request(null, configuration), "admin@example.com");

        assertThat(settings.getSupportHours()).isEqualTo("Thứ 2 - Chủ Nhật");
        assertThat(settings.getFacebookUrl()).isEqualTo("https://www.facebook.com/cho.giong.2025");
        assertThat(response.supportHours()).isEqualTo("Thứ 2 - Chủ Nhật");
        assertThat(response.facebookUrl()).isEqualTo("https://www.facebook.com/cho.giong.2025");
        assertThat(response.zaloUrl()).isEqualTo("https://zalo.me/0901234567");
        assertThat(settings.getDisplayConfiguration().path("contact").path("supportHours").asText())
                .isEqualTo(settings.getSupportHours());
        assertThat(settings.getDisplayConfiguration().path("socials").path("facebook").asText())
                .isEqualTo(settings.getFacebookUrl());
    }

    @Test
    void firstClassValuesOverrideLegacyNestedValues() throws Exception {
        settings.setSupportHours("First-class hours");
        settings.setFacebookUrl("https://www.facebook.com/first-class");
        var configuration = objectMapper.readTree("""
                {
                  "contact": {"supportHours": "Legacy hours"},
                  "socials": {"facebook": "https://www.facebook.com/legacy"}
                }
                """);

        var response = service.update(request(null, configuration), "admin@example.com");

        assertThat(response.supportHours()).isEqualTo("First-class hours");
        assertThat(response.facebookUrl()).isEqualTo("https://www.facebook.com/first-class");
        assertThat(response.displayConfiguration().path("contact").path("supportHours").asText())
                .isEqualTo("First-class hours");
        assertThat(response.displayConfiguration().path("socials").path("facebook").asText())
                .isEqualTo("https://www.facebook.com/first-class");
    }

    @Test
    void rejectsNonFacebookUrl() {
        var request = new SystemSettingsRequest(
                null, null, null, null, null, null, null, null,
                "https://zalo.me/0901234567", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(request, "admin@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Facebook");
    }

    private SystemSettingsRequest request(String zaloUrl, com.fasterxml.jackson.databind.JsonNode configuration) {
        return new SystemSettingsRequest(null, null, null, null, null, null, null, zaloUrl,
                null, null, null, null, null, configuration);
    }
}
