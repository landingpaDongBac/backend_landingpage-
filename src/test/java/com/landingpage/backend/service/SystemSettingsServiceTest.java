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

    private SystemSettingsRequest request(String zaloUrl, com.fasterxml.jackson.databind.JsonNode configuration) {
        return new SystemSettingsRequest(null, null, null, null, null, null, zaloUrl,
                null, null, null, null, configuration);
    }
}
