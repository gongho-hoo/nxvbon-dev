package com.nxvibeon.backend.codechange.dto;

public record CodeChangeRollbackResponse(
    String rollbackHistoryId,
    String sourceHistoryId,
    String projectId,
    String targetFilePath,
    String status,
    String message
) {
}
