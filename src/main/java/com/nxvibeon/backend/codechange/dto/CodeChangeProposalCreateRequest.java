package com.nxvibeon.backend.codechange.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeChangeProposalCreateRequest(
    @NotBlank String projectId,
    String sessionId,
    @NotBlank String targetFilePath,
    @NotBlank String originalCode,
    @NotBlank String instruction
) {
}
