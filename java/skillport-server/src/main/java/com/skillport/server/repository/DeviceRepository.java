package com.skillport.server.repository;

import com.skillport.server.domain.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    List<DeviceEntity> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Optional<DeviceEntity> findByPublicIdAndOwnerId(String publicId, String ownerId);
    Optional<DeviceEntity> findByPublicId(String publicId);
    Optional<DeviceEntity> findByOwnerIdAndClientInstanceId(String ownerId, String clientInstanceId);
    Optional<DeviceEntity> findFirstByOwnerIdAndClientInstanceIdIsNullAndNameAndOsAndArchOrderByCreatedAtDesc(
            String ownerId, String name, String os, String arch);
}
