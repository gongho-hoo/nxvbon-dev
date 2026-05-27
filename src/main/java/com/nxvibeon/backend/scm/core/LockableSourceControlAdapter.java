package com.nxvibeon.backend.scm.core;

import java.nio.file.Path;
import java.util.List;

public interface LockableSourceControlAdapter extends SourceControlAdapter {
    CliCommandResult lock(Path workingCopyPath, List<String> paths, String message);
    CliCommandResult unlock(Path workingCopyPath, List<String> paths);
}
