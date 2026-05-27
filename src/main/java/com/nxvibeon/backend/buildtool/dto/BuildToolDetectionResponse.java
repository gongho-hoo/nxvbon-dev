package com.nxvibeon.backend.buildtool.dto;

import java.util.List;

public record BuildToolDetectionResponse(
    String projectPath,
    List<String> buildTools,
    List<String> indicators
) {
}
