package com.nxvibeon.backend.codechange.controller;

import com.nxvibeon.backend.codechange.dto.CodeChangeApplyResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeHistoryResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalCreateRequest;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeRollbackResponse;
import com.nxvibeon.backend.codechange.service.CodeChangeService;
import com.nxvibeon.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/code-changes")
public class CodeChangeController {
    private final CodeChangeService service;

    public CodeChangeController(CodeChangeService service) {
        this.service = service;
    }

    @PostMapping("/proposals")
    public ApiResponse<CodeChangeProposalResponse> createProposal(@Valid @RequestBody CodeChangeProposalCreateRequest request) {
        return ApiResponse.ok(service.createProposal(request));
    }

    @GetMapping("/proposals/{proposalId}")
    public ApiResponse<CodeChangeProposalResponse> getProposal(@PathVariable String proposalId) {
        return ApiResponse.ok(service.getProposal(proposalId));
    }

    @PostMapping("/proposals/{proposalId}/apply")
    public ApiResponse<CodeChangeApplyResponse> applyProposal(@PathVariable String proposalId) {
        return ApiResponse.ok(service.applyProposal(proposalId));
    }

    @GetMapping("/histories")
    public ApiResponse<List<CodeChangeHistoryResponse>> listHistories(
        @RequestParam String projectId,
        @RequestParam String targetFilePath
    ) {
        return ApiResponse.ok(service.listHistories(projectId, targetFilePath));
    }

    @PostMapping("/histories/{historyId}/rollback")
    public ApiResponse<CodeChangeRollbackResponse> rollback(@PathVariable String historyId) {
        return ApiResponse.ok(service.rollback(historyId));
    }
}
