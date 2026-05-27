package com.nxvibeon.backend.project.controller;

import com.nxvibeon.backend.common.ApiResponse;
import com.nxvibeon.backend.project.dto.CreateProjectRequest;
import com.nxvibeon.backend.project.dto.ProjectResponse;
import com.nxvibeon.backend.project.dto.UpdateProjectRequest;
import com.nxvibeon.backend.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@PathVariable String projectId) {
        return ApiResponse.ok(service.get(projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
        @PathVariable String projectId,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ApiResponse.ok(service.update(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Map<String, Long>> delete(@PathVariable String projectId) {
        long deletedChatHistoryCount = service.deleteWithHistories(projectId);
        return ApiResponse.ok(Map.of("deletedChatHistoryCount", deletedChatHistoryCount));
    }
}
