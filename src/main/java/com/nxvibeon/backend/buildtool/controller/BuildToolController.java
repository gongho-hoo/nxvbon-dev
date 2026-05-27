package com.nxvibeon.backend.buildtool.controller;

import com.nxvibeon.backend.buildtool.dto.BuildToolDetectionRequest;
import com.nxvibeon.backend.buildtool.dto.BuildToolDetectionResponse;
import com.nxvibeon.backend.buildtool.service.BuildToolService;
import com.nxvibeon.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/build-tools")
public class BuildToolController {
    private final BuildToolService service;

    public BuildToolController(BuildToolService service) {
        this.service = service;
    }

    @PostMapping("/detect")
    public ApiResponse<BuildToolDetectionResponse> detect(@Valid @RequestBody BuildToolDetectionRequest request) {
        return ApiResponse.ok(service.detect(Path.of(request.projectPath())));
    }
}
