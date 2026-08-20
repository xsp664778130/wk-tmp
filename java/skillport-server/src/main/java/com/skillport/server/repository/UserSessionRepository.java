package com.skillport.server.repository;

import com.skillport.server.domain.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {
    Optional<UserSessionEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
