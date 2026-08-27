package com.skillport.server.repository;

import com.skillport.server.domain.FeedbackMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessageEntity, Long> {
}
