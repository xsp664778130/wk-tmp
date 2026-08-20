package com.skillport.server.repository;

import com.skillport.server.domain.DownloadTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadTicketRepository extends JpaRepository<DownloadTicketEntity, String> {
}
