package com.nxvibeon.backend.scm.core;

import java.nio.file.Path;

public interface SourceControlAdapter {
    ScmType type();
    CliCommandResult testConnection(String repositoryUrl);
    CliCommandResult checkout(String repositoryUrl, Path targetPath);
    CliCommandResult update(Path workingCopyPath);
    CliCommandResult status(Path workingCopyPath);
    CliCommandResult log(Path workingCopyPath, int limit);
    CliCommandResult diff(Path workingCopyPath);
}
