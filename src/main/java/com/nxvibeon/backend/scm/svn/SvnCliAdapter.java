package com.nxvibeon.backend.scm.svn;

import com.nxvibeon.backend.scm.core.CliCommandExecutor;
import com.nxvibeon.backend.scm.core.CliCommandResult;
import com.nxvibeon.backend.scm.core.LockableSourceControlAdapter;
import com.nxvibeon.backend.scm.core.ScmType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class SvnCliAdapter implements LockableSourceControlAdapter {
    private final CliCommandExecutor executor;

    public SvnCliAdapter(CliCommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public ScmType type() { return ScmType.SVN; }

    @Override
    public CliCommandResult testConnection(String repositoryUrl) {
        return executor.run(List.of("svn", "info", repositoryUrl, "--non-interactive"), null);
    }

    @Override
    public CliCommandResult checkout(String repositoryUrl, Path targetPath) {
        return executor.run(List.of("svn", "checkout", repositoryUrl, targetPath.toString(), "--non-interactive"), null);
    }

    @Override
    public CliCommandResult update(Path workingCopyPath) {
        return executor.run(List.of("svn", "update", "--non-interactive"), workingCopyPath);
    }

    @Override
    public CliCommandResult status(Path workingCopyPath) {
        return executor.run(List.of("svn", "status", "--xml", "--non-interactive"), workingCopyPath);
    }

    @Override
    public CliCommandResult log(Path workingCopyPath, int limit) {
        return executor.run(List.of("svn", "log", "--xml", "--limit", String.valueOf(limit), "--non-interactive"), workingCopyPath);
    }

    @Override
    public CliCommandResult diff(Path workingCopyPath) {
        return executor.run(List.of("svn", "diff", "--non-interactive"), workingCopyPath);
    }

    @Override
    public CliCommandResult lock(Path workingCopyPath, List<String> paths, String message) {
        List<String> command = new ArrayList<>();
        command.addAll(List.of("svn", "lock", "--non-interactive", "-m", message == null ? "NxVibeOn lock" : message));
        command.addAll(paths);
        return executor.run(command, workingCopyPath);
    }

    @Override
    public CliCommandResult unlock(Path workingCopyPath, List<String> paths) {
        List<String> command = new ArrayList<>();
        command.addAll(List.of("svn", "unlock", "--non-interactive"));
        command.addAll(paths);
        return executor.run(command, workingCopyPath);
    }
}
