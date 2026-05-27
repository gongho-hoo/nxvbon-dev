package com.nxvibeon.backend.buildtool.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildToolDetectionRequest(@NotBlank String projectPath) {
}
