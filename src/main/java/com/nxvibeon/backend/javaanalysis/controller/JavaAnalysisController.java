package com.nxvibeon.backend.javaanalysis.controller;

import com.nxvibeon.backend.common.ApiResponse;
import com.nxvibeon.backend.javaanalysis.dto.JavaAnalysisRequest;
import com.nxvibeon.backend.javaanalysis.dto.JavaAnalysisResponse;
import com.nxvibeon.backend.javaanalysis.service.JavaProjectAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/java-analysis")
public class JavaAnalysisController {
    private final JavaProjectAnalysisService service;

    public JavaAnalysisController(JavaProjectAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public ApiResponse<JavaAnalysisResponse> analyze(@Valid @RequestBody JavaAnalysisRequest request) {
        int maxFiles = request.maxFiles() == null ? 300 : request.maxFiles();
        return ApiResponse.ok(service.analyze(Path.of(request.projectPath()), maxFiles));
    }
}
