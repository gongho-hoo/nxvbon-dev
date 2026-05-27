package com.nxvibeon.backend.codechange.dto;

import com.nxvibeon.backend.codechange.domain.CodeChangeProposalEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record CodeChangeProposalResponse(
    String proposalId,
    String projectId,
    String sessionId,
    String targetFilePath,
    String instruction,
    String originalCode,
    String proposedCode,
    String diffText,
    String status,
    LocalDateTime createdAt
) {
    /**
     * 기존 CodeChangeService에서 사용하는 JPA Entity -> Response 변환 메서드입니다.
     * Workspace 기반 기능변경 패치에서 DTO가 단순 record로 바뀌면서 누락되었던 메서드를 복구합니다.
     */
    public static CodeChangeProposalResponse from(CodeChangeProposalEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CodeChangeProposalResponse(
            entity.getId(),
            entity.getProjectId(),
            entity.getSessionId(),
            entity.getTargetFilePath(),
            entity.getInstruction(),
            entity.getOriginalCode(),
            entity.getProposedCode(),
            entity.getDiffText(),
            entity.getStatus(),
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
