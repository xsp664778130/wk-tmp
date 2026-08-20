package com.skillport.server.repository;

import com.skillport.server.domain.InstallTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstallTaskRepository extends JpaRepository<InstallTaskEntity, Long> {
    Optional<InstallTaskEntity> findByPublicId(String publicId);
    Optional<InstallTaskEntity> findByPublicIdAndDevicePublicId(String publicId, String devicePublicId);
    Optional<InstallTaskEntity> findByPublicIdAndOwnerId(String publicId, String ownerId);
    List<InstallTaskEntity> findTop50ByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
