package com.skillport.server.repository;

import com.skillport.server.domain.DeviceLocalSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceLocalSkillRepository extends JpaRepository<DeviceLocalSkillEntity, Long> {
    List<DeviceLocalSkillEntity> findAllByOwnerIdAndDevicePublicIdOrderByToolAscNameAsc(
            String ownerId, String devicePublicId);

    Optional<DeviceLocalSkillEntity> findByOwnerIdAndDevicePublicIdAndToolAndSlug(
            String ownerId, String devicePublicId, String tool, String slug);

    @Modifying
    @Query("delete from DeviceLocalSkillEntity localSkill where localSkill.devicePublicId = :devicePublicId")
    void deleteInventory(@Param("devicePublicId") String devicePublicId);
}
