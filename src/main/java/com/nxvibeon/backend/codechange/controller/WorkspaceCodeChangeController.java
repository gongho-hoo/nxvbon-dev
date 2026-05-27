package com.nxvibeon.backend.codechange.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nxvibeon.backend.codechange.dto.ApplyCodeChangeResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeHistoryResponse;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalRequest;
import com.nxvibeon.backend.codechange.dto.CodeChangeProposalResponse;
import com.nxvibeon.backend.codechange.service.WorkspaceCodeChangeService;

@RestController
@RequestMapping("/api/v1/code-changes")
public class WorkspaceCodeChangeController {

    private final WorkspaceCodeChangeService workspaceCodeChangeService;

    public WorkspaceCodeChangeController(WorkspaceCodeChangeService workspaceCodeChangeService) {
        this.workspaceCodeChangeService = workspaceCodeChangeService;
    }

    @PostMapping(
        value = "/proposals",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CodeChangeProposalResponse createProposal(@RequestBody CodeChangeProposalRequest request) {
        return workspaceCodeChangeService.createProposal(request);
    }

    @GetMapping(value = "/proposals/{proposalId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CodeChangeProposalResponse getProposal(@PathVariable String proposalId) {
        return workspaceCodeChangeService.getProposal(proposalId);
    }

    @PostMapping(value = "/proposals/{proposalId}/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplyCodeChangeResponse applyProposal(@PathVariable String proposalId) {
        return workspaceCodeChangeService.applyProposal(proposalId);
    }

    @GetMapping(value = "/histories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CodeChangeHistoryResponse> getHistories(
        @RequestParam String projectId,
        @RequestParam(required = false) String targetFilePath
    ) {
        return workspaceCodeChangeService.getHistories(projectId, targetFilePath);
    }

    @PostMapping(value = "/histories/{historyId}/rollback", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplyCodeChangeResponse rollback(@PathVariable String historyId) {
        return workspaceCodeChangeService.rollback(historyId);
    }
}
