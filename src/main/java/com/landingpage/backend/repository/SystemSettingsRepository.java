package com.landingpage.backend.repository;

import com.landingpage.backend.domain.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {
}
