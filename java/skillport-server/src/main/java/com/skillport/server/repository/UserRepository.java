package com.skillport.server.repository;

import com.skillport.server.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmailNormalized(String emailNormalized);
    Optional<UserEntity> findByEmailNormalized(String emailNormalized);
    Optional<UserEntity> findByPublicId(String publicId);
    Optional<UserEntity> findByWeComCorpIdAndWeComUserId(String weComCorpId, String weComUserId);
}
