package com.nxvibeon.backend.javaanalysis.dto;

import java.util.List;

public record JavaAnalysisResponse(
    String projectPath,
    int parsedFileCount,
    int failedFileCount,
    List<JavaTypeSummary> types
) {
}
