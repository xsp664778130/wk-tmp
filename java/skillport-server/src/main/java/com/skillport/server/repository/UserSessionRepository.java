package com.skillport.server.repository;

import com.skillport.server.domain.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {
    Optional<UserSessionEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("update UserSessionEntity session set session.revokedAt = :now "
            + "where session.ownerId = :ownerId and session.revokedAt is null")
    int revokeAllByOwnerId(@Param("ownerId") String ownerId, @Param("now") Instant now);
}
