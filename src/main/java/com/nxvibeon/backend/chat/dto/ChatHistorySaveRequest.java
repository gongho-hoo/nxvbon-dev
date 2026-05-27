package com.nxvibeon.backend.chat.dto;

import com.nxvibeon.backend.chat.domain.ChatRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatHistorySaveRequest(
    String projectId,
    @NotBlank String sessionId,
    @NotNull ChatRole role,
    @NotBlank String content,
    String metadataJson
) {
}
