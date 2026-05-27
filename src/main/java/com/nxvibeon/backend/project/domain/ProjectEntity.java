package com.nxvibeon.backend.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String rootPath;

    @Column(columnDefinition = "text")
    private String backendPath;

    @Column(columnDefinition = "text")
    private String fastapiPath;

    @Column(columnDefinition = "text")
    private String webUiPath;

    @Column(length = 20)
    private String vcsType;

    @Column(columnDefinition = "text")
    private String vcsUrl;

    @Column(length = 100)
    private String defaultBranch;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProjectEntity() {
    }

    public ProjectEntity(
        String name,
        String description,
        String rootPath,
        String backendPath,
        String fastapiPath,
        String webUiPath,
        String vcsType,
        String vcsUrl,
        String defaultBranch
    ) {
        this.id = "project-" + UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.rootPath = rootPath;
        this.backendPath = backendPath;
        this.fastapiPath = fastapiPath;
        this.webUiPath = webUiPath;
        this.vcsType = vcsType;
        this.vcsUrl = vcsUrl;
        this.defaultBranch = defaultBranch;
        this.enabled = true;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null || id.isBlank()) {
            id = "project-" + UUID.randomUUID();
        }
        if (enabled == null) {
            enabled = true;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRootPath() { return rootPath; }
    public String getBackendPath() { return backendPath; }
    public String getFastapiPath() { return fastapiPath; }
    public String getWebUiPath() { return webUiPath; }
    public String getVcsType() { return vcsType; }
    public String getVcsUrl() { return vcsUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public Boolean getEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setRootPath(String rootPath) { this.rootPath = rootPath; }
    public void setBackendPath(String backendPath) { this.backendPath = backendPath; }
    public void setFastapiPath(String fastapiPath) { this.fastapiPath = fastapiPath; }
    public void setWebUiPath(String webUiPath) { this.webUiPath = webUiPath; }
    public void setVcsType(String vcsType) { this.vcsType = vcsType; }
    public void setVcsUrl(String vcsUrl) { this.vcsUrl = vcsUrl; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
