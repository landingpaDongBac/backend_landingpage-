package com.landingpage.backend.repository;

import com.landingpage.backend.domain.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID>, JpaSpecificationExecutor<MediaAsset> {
    Optional<MediaAsset> findByCloudinaryPublicId(String publicId);
}
