package com.nxvibeon.backend.scm.controller;

import com.nxvibeon.backend.common.ApiResponse;
import com.nxvibeon.backend.scm.core.CliCommandResult;
import com.nxvibeon.backend.scm.core.ScmType;
import com.nxvibeon.backend.scm.dto.ScmCommandRequest;
import com.nxvibeon.backend.scm.service.SourceControlService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/scm")
public class SourceControlController {
    private final SourceControlService service;

    public SourceControlController(SourceControlService service) {
        this.service = service;
    }

    @PostMapping("/{type}/test")
    public ApiResponse<CliCommandResult> test(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        return ApiResponse.ok(service.testConnection(type, request.repositoryUrl()));
    }

    @PostMapping("/{type}/checkout")
    public ApiResponse<CliCommandResult> checkout(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        return ApiResponse.ok(service.checkout(type, request.repositoryUrl(), Path.of(request.targetPath())));
    }

    @PostMapping("/{type}/status")
    public ApiResponse<CliCommandResult> status(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        return ApiResponse.ok(service.status(type, Path.of(request.workingCopyPath())));
    }

    @PostMapping("/{type}/update")
    public ApiResponse<CliCommandResult> update(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        return ApiResponse.ok(service.update(type, Path.of(request.workingCopyPath())));
    }

    @PostMapping("/{type}/log")
    public ApiResponse<CliCommandResult> log(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        int limit = request.limit() == null ? 20 : request.limit();
        return ApiResponse.ok(service.log(type, Path.of(request.workingCopyPath()), limit));
    }

    @PostMapping("/{type}/diff")
    public ApiResponse<CliCommandResult> diff(@PathVariable ScmType type, @RequestBody ScmCommandRequest request) {
        return ApiResponse.ok(service.diff(type, Path.of(request.workingCopyPath())));
    }
}
