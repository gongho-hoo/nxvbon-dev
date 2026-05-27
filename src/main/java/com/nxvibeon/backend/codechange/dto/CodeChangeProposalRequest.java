package com.nxvibeon.backend.codechange.dto;

/**
 * Workspace 기능변경 요청 DTO.
 *
 * 중요:
 * - targetFilePath는 표시/이력/파일명 용도입니다.
 * - Backend는 targetFilePath로 서버 파일 시스템에서 원본 파일을 찾지 않습니다.
 * - originalCode는 Web UI Workspace 창에 현재 열린 소스코드 내용입니다.
 */
public record CodeChangeProposalRequest(
    String projectId,
    String sessionId,
    String targetFilePath,
    String originalCode,
    String instruction
) {
}
