package com.nxvibeon.backend.codechange.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_change_histories")
public class CodeChangeHistoryEntity {
    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 80)
    private String projectId;

    @Column(length = 80)
    private String proposalId;

    @Column(nullable = false, columnDefinition = "text")
    private String targetFilePath;

    @Column(columnDefinition = "text")
    private String instruction;

    @Column(nullable = false, columnDefinition = "text")
    private String beforeCode;

    @Column(nullable = false, columnDefinition = "text")
    private String afterCode;

    @Column(nullable = false, length = 128)
    private String beforeHash;

    @Column(nullable = false, length = 128)
    private String afterHash;

    @Column(nullable = false, length = 30)
    private String actionType;

    @Column(nullable = false)
    private Instant createdAt;

    protected CodeChangeHistoryEntity() {
    }

    public CodeChangeHistoryEntity(
        String projectId,
        String proposalId,
        String targetFilePath,
        String instruction,
        String beforeCode,
        String afterCode,
        String beforeHash,
        String afterHash,
        String actionType
    ) {
        this.id = "history-" + UUID.randomUUID();
        this.projectId = projectId;
        this.proposalId = proposalId;
        this.targetFilePath = targetFilePath;
        this.instruction = instruction;
        this.beforeCode = beforeCode;
        this.afterCode = afterCode;
        this.beforeHash = beforeHash;
        this.afterHash = afterHash;
        this.actionType = actionType;
    }

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "history-" + UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getProposalId() { return proposalId; }
    public String getTargetFilePath() { return targetFilePath; }
    public String getInstruction() { return instruction; }
    public String getBeforeCode() { return beforeCode; }
    public String getAfterCode() { return afterCode; }
    public String getBeforeHash() { return beforeHash; }
    public String getAfterHash() { return afterHash; }
    public String getActionType() { return actionType; }
    public Instant getCreatedAt() { return createdAt; }
}
