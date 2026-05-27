package com.nxvibeon.backend.javaanalysis.dto;

import jakarta.validation.constraints.NotBlank;

public record JavaAnalysisRequest(
    @NotBlank String projectPath,
    Integer maxFiles
) {
}
