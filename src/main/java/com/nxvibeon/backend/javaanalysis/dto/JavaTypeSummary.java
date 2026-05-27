package com.nxvibeon.backend.javaanalysis.dto;

public record JavaTypeSummary(
    String packageName,
    String typeName,
    String kind,
    int methodCount,
    int fieldCount,
    String filePath
) {
}
