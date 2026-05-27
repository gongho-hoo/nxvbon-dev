package com.nxvibeon.backend.scm.dto;

public record ScmCommandRequest(
    String repositoryUrl,
    String workingCopyPath,
    String targetPath,
    Integer limit
) {
}
