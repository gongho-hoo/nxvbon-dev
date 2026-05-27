package com.nxvibeon.backend.codechange.dto;

import java.time.LocalDateTime;

/**
 * 소스 반영 응답.
 *
 * 이번 정책에서는 Backend가 서버 파일을 직접 덮어쓰지 않습니다.
 * Web UI가 appliedCode를 Workspace 코드 영역에 반영하고,
 * 실제 파일 저장은 Workspace 저장 버튼 또는 별도 저장 기능에서 처리합니다.
 */
public record ApplyCodeChangeResponse(
    String proposalId,
    String historyId,
    String projectId,
    String targetFilePath,
    String appliedCode,
    String status,
    LocalDateTime appliedAt
) {
}
