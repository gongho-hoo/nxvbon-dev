package com.nxvibeon.backend.codechange.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_change_proposals")
public class CodeChangeProposalEntity {
    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 80)
    private String projectId;

    @Column(length = 120)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "text")
    private String targetFilePath;

    @Column(nullable = false, columnDefinition = "text")
    private String instruction;

    @Column(nullable = false, columnDefinition = "text")
    private String originalCode;

    @Column(nullable = false, columnDefinition = "text")
    private String proposedCode;

    @Column(columnDefinition = "text")
    private String diffText;

    @Column(nullable = false, length = 128)
    private String originalHash;

    @Column(nullable = false, length = 128)
    private String proposedHash;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant appliedAt;

    protected CodeChangeProposalEntity() {
    }

    public CodeChangeProposalEntity(
        String projectId,
        String sessionId,
        String targetFilePath,
        String instruction,
        String originalCode,
        String proposedCode,
        String diffText,
        String originalHash,
        String proposedHash
    ) {
        this.id = "proposal-" + UUID.randomUUID();
        this.projectId = projectId;
        this.sessionId = sessionId;
        this.targetFilePath = targetFilePath;
        this.instruction = instruction;
        this.originalCode = originalCode;
        this.proposedCode = proposedCode;
        this.diffText = diffText;
        this.originalHash = originalHash;
        this.proposedHash = proposedHash;
        this.status = "PENDING";
    }

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "proposal-" + UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    public void markApplied() {
        this.status = "APPLIED";
        this.appliedAt = Instant.now();
    }

    public void markConflict() {
        this.status = "CONFLICT";
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getSessionId() { return sessionId; }
    public String getTargetFilePath() { return targetFilePath; }
    public String getInstruction() { return instruction; }
    public String getOriginalCode() { return originalCode; }
    public String getProposedCode() { return proposedCode; }
    public String getDiffText() { return diffText; }
    public String getOriginalHash() { return originalHash; }
    public String getProposedHash() { return proposedHash; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAppliedAt() { return appliedAt; }
}
