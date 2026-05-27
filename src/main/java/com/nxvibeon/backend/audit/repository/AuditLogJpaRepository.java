package com.nxvibeon.backend.audit.repository;

import com.nxvibeon.backend.audit.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {
}
