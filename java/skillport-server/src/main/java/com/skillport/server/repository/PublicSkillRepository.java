package com.skillport.server.repository;

import com.skillport.server.domain.PublicSkillEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicSkillRepository extends JpaRepository<PublicSkillEntity, Long> {
    Optional<PublicSkillEntity> findByPublicId(String publicId);
    Optional<PublicSkillEntity> findBySourceSkillPublicId(String sourceSkillPublicId);
    List<PublicSkillEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);
    List<PublicSkillEntity> findAllByPublisherOwnerId(String publisherOwnerId);
}
