package com.nxvibeon.backend.health;

import com.nxvibeon.backend.ai.client.FastApiAiClient;
import com.nxvibeon.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final FastApiAiClient fastApiAiClient;

    public HealthController(FastApiAiClient fastApiAiClient) {
        this.fastApiAiClient = fastApiAiClient;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
            "status", "ok",
            "service", "nxvibeon-spring-api",
            "role", "main-backend"
        ));
    }

    @GetMapping("/ai-worker")
    public ApiResponse<Map<String, Object>> aiWorkerHealth() {
        return ApiResponse.ok(fastApiAiClient.health());
    }
}
