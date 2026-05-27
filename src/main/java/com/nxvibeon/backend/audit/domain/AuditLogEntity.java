package com.nxvibeon.backend.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    private String id;
    private String actor;
    @Column(nullable = false)
    private String action;
    private String targetType;
    private String targetId;
    @Column(columnDefinition = "text")
    private String detail;
    @Column(nullable = false)
    private Instant createdAt;

    protected AuditLogEntity() {}

    public AuditLogEntity(String actor, String action, String targetType, String targetId, String detail) {
        this.id = UUID.randomUUID().toString();
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = Instant.now();
    }
}
