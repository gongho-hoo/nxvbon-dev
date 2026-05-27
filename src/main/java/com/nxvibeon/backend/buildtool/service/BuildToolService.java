package com.nxvibeon.backend.buildtool.service;

import com.nxvibeon.backend.buildtool.dto.BuildToolDetectionResponse;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class BuildToolService {
    public BuildToolDetectionResponse detect(Path projectPath) {
        List<String> tools = new ArrayList<>();
        List<String> indicators = new ArrayList<>();

        if (Files.exists(projectPath.resolve("pom.xml"))) {
            tools.add("MAVEN");
            indicators.add("pom.xml");
        }
        if (Files.exists(projectPath.resolve("build.gradle"))) {
            tools.add("GRADLE");
            indicators.add("build.gradle");
        }
        if (Files.exists(projectPath.resolve("build.gradle.kts"))) {
            tools.add("GRADLE");
            indicators.add("build.gradle.kts");
        }
        if (Files.exists(projectPath.resolve("settings.gradle"))) {
            indicators.add("settings.gradle");
        }
        if (Files.exists(projectPath.resolve("settings.gradle.kts"))) {
            indicators.add("settings.gradle.kts");
        }

        return new BuildToolDetectionResponse(projectPath.toString(), tools.stream().distinct().toList(), indicators);
    }
}
