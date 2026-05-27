package com.nxvibeon.backend.audit.service;

import com.nxvibeon.backend.audit.domain.AuditLogEntity;
import com.nxvibeon.backend.audit.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogJpaRepository repository;

    public AuditService(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    public void record(String actor, String action, String targetType, String targetId, String detail) {
        repository.save(new AuditLogEntity(actor, action, targetType, targetId, detail));
    }
}
