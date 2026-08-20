package com.skillport.server.repository;

import com.skillport.server.domain.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
    List<SkillEntity> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Optional<SkillEntity> findByPublicIdAndOwnerId(String publicId, String ownerId);
    Optional<SkillEntity> findByPublicId(String publicId);
}
