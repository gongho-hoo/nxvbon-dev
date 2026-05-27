package com.nxvibeon.backend.scm.service;

import com.nxvibeon.backend.scm.core.CliCommandResult;
import com.nxvibeon.backend.scm.core.ScmType;
import com.nxvibeon.backend.scm.core.SourceControlAdapter;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SourceControlService {
    private final Map<ScmType, SourceControlAdapter> adapters = new EnumMap<>(ScmType.class);

    public SourceControlService(List<SourceControlAdapter> adapterList) {
        for (SourceControlAdapter adapter : adapterList) {
            adapters.put(adapter.type(), adapter);
        }
    }

    public CliCommandResult testConnection(ScmType type, String repositoryUrl) {
        return adapter(type).testConnection(repositoryUrl);
    }

    public CliCommandResult checkout(ScmType type, String repositoryUrl, Path targetPath) {
        return adapter(type).checkout(repositoryUrl, targetPath);
    }

    public CliCommandResult status(ScmType type, Path workingCopyPath) {
        return adapter(type).status(workingCopyPath);
    }

    public CliCommandResult update(ScmType type, Path workingCopyPath) {
        return adapter(type).update(workingCopyPath);
    }

    public CliCommandResult log(ScmType type, Path workingCopyPath, int limit) {
        return adapter(type).log(workingCopyPath, limit);
    }

    public CliCommandResult diff(ScmType type, Path workingCopyPath) {
        return adapter(type).diff(workingCopyPath);
    }

    private SourceControlAdapter adapter(ScmType type) {
        SourceControlAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported SCM type: " + type);
        }
        return adapter;
    }
}
