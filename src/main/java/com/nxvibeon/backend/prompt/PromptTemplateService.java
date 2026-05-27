package com.nxvibeon.backend.prompt;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PromptTemplateService {
    private final ResourceLoader resourceLoader;
    private final PromptProperties promptProperties;

    public PromptTemplateService(ResourceLoader resourceLoader, PromptProperties promptProperties) {
        this.resourceLoader = resourceLoader;
        this.promptProperties = promptProperties;
    }

    public String resolveChatPrompt(String projectId) {
        if (StringUtils.hasText(projectId)) {
            return loadClasspathPrompt(promptProperties.getProjectChat());
        }
        return loadClasspathPrompt(promptProperties.getGeneralChat());
    }

    public String loadClasspathPrompt(String classpathLocation) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + classpathLocation);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("프롬프트 파일을 읽을 수 없습니다: " + classpathLocation, e);
        }
    }
}
