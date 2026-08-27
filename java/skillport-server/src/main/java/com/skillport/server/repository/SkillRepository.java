package com.skillport.server.repository;

import com.skillport.server.domain.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
    long countByOwnerId(String ownerId);
    List<SkillEntity> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Optional<SkillEntity> findByPublicIdAndOwnerId(String publicId, String ownerId);
    Optional<SkillEntity> findByPublicId(String publicId);
    Optional<SkillEntity> findByOwnerIdAndSourcePublicSkillId(String ownerId, String sourcePublicSkillId);
    List<SkillEntity> findAllByPublicIdIn(Collection<String> publicIds);

    @Query("select skill.sourcePublicSkillId from SkillEntity skill " +
            "where skill.ownerId = :ownerId and skill.sourcePublicSkillId is not null")
    List<String> findImportedPublicSkillIds(@Param("ownerId") String ownerId);
}
