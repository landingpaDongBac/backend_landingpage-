package com.landingpage.backend.repository;

import com.landingpage.backend.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    Optional<Section> findBySectionKey(String sectionKey);
    List<Section> findAllByOrderByDisplayOrderAsc();
    List<Section> findByEnabledTrueOrderByDisplayOrderAsc();
}
