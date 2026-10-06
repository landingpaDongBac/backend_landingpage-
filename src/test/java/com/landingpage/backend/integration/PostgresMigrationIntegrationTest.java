package com.landingpage.backend.integration;

import com.landingpage.backend.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("landing_page")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.initial-admin.email", () -> "");
        registry.add("app.initial-admin.password", () -> "");
    }

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesSchemaAndSeedsStableSectionVisibility() {
        assertThat(sectionRepository.count()).isEqualTo(9);
        assertThat(sectionRepository.findBySectionKey("04")).isPresent().get()
                .extracting(section -> section.isEnabled()).isEqualTo(false);
        assertThat(sectionRepository.findBySectionKey("09")).isPresent().get()
                .extracting(section -> section.isEnabled()).isEqualTo(false);
        assertThat(sectionRepository.findBySectionKey("10")).isPresent().get()
                .extracting(section -> section.isEnabled()).isEqualTo(true);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public' and table_name in ('refresh_tokens','system_settings','audit_logs')",
                Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema='public' and table_name='leads' and column_name='deleted_at'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema='public' and table_name='system_settings' and column_name='zalo_url'",
                Integer.class)).isEqualTo(1);
    }
}
