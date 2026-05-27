package com.nxvibeon.backend.codechange.dto;

public record CodeChangeApplyResponse(
    String proposalId,
    String historyId,
    String projectId,
    String targetFilePath,
    String status,
    String message
) {
}
