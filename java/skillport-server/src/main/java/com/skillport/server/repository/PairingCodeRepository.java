package com.skillport.server.repository;

import com.skillport.server.domain.PairingCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingCodeRepository extends JpaRepository<PairingCodeEntity, String> {
}
