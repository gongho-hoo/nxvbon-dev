package com.nxvibeon.backend.ai.controller;

import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.ai.dto.IndexDocumentRequest;
import com.nxvibeon.backend.ai.dto.IndexDocumentResponse;
import com.nxvibeon.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIndexController {
    private final FastApiAiClient aiClient;

    public DocumentIndexController(FastApiAiClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping("/index")
    public ApiResponse<IndexDocumentResponse> index(@Valid @RequestBody IndexDocumentRequest request) {
        return ApiResponse.ok(aiClient.indexDocument(request));
    }
}
