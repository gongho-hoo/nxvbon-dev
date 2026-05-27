package com.nxvibeon.backend.codechange.dto;

import com.nxvibeon.backend.codechange.domain.CodeChangeHistoryEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record CodeChangeHistoryResponse(
    String historyId,
    String projectId,
    String proposalId,
    String targetFilePath,
    String instruction,
    String beforeCode,
    String afterCode,
    String actionType,
    LocalDateTime createdAt
) {
    /**
     * 기존 CodeChangeService에서 사용하는 JPA Entity -> Response 변환 메서드입니다.
     * Workspace 기반 기능변경 패치에서 DTO가 단순 record로 바뀌면서 누락되었던 메서드를 복구합니다.
     */
    public static CodeChangeHistoryResponse from(CodeChangeHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CodeChangeHistoryResponse(
            entity.getId(),
            entity.getProjectId(),
            entity.getProposalId(),
            entity.getTargetFilePath(),
            entity.getInstruction(),
            entity.getBeforeCode(),
            entity.getAfterCode(),
            entity.getActionType(),
            toLocalDateTime(entity.getCreatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
