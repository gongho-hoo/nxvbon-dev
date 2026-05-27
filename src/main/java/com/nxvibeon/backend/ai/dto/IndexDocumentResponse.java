package com.nxvibeon.backend.ai.dto;

import java.util.Map;

public record IndexDocumentResponse(
    String documentId,
    int chunkCount,
    Map<String, Object> detail
) {
}
