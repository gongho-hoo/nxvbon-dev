package com.nxvibeon.backend.scm.git;

import com.nxvibeon.backend.scm.core.CliCommandExecutor;
import com.nxvibeon.backend.scm.core.CliCommandResult;
import com.nxvibeon.backend.scm.core.ScmType;
import com.nxvibeon.backend.scm.core.SourceControlAdapter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class GitCliAdapter implements SourceControlAdapter {
    private final CliCommandExecutor executor;

    public GitCliAdapter(CliCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public ScmType type() { return ScmType.GIT; }

    @Override
    public CliCommandResult testConnection(String repositoryUrl) {
        return executor.run(List.of("git", "ls-remote", repositoryUrl), null);
    }

    @Override
    public CliCommandResult checkout(String repositoryUrl, Path targetPath) {
        return executor.run(List.of("git", "clone", repositoryUrl, targetPath.toString()), null);
    }

    @Override
    public CliCommandResult update(Path workingCopyPath) {
        return executor.run(List.of("git", "pull", "--ff-only"), workingCopyPath);
    }

    @Override
    public CliCommandResult status(Path workingCopyPath) {
        return executor.run(List.of("git", "status", "--porcelain=v1"), workingCopyPath);
    }

    @Override
    public CliCommandResult log(Path workingCopyPath, int limit) {
        return executor.run(List.of("git", "log", "--oneline", "-n", String.valueOf(limit)), workingCopyPath);
    }

    @Override
    public CliCommandResult diff(Path workingCopyPath) {
        return executor.run(List.of("git", "diff", "--"), workingCopyPath);
    }
}
