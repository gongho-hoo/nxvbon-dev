package com.nxvibeon.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record IndexDocumentRequest(
    @NotBlank String projectId,
    @NotBlank String documentId,
    @NotBlank String title,
    @NotBlank String content,
    Map<String, Object> metadata
) {
}
