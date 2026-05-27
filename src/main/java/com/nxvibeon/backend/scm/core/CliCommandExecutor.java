package com.nxvibeon.backend.scm.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class CliCommandExecutor {
    private final long timeoutSeconds;

    public CliCommandExecutor(@Value("${nxvibeon.scm.command-timeout-seconds:120}") long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public CliCommandResult run(List<String> command, Path workingDirectory) {
        return run(command, workingDirectory, Duration.ofSeconds(timeoutSeconds));
    }

    public CliCommandResult run(List<String> command, Path workingDirectory, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CliCommandResult(-1, "Command timed out: " + String.join(" ", command), true);
            }
            String output = outputFuture.get(3, TimeUnit.SECONDS);
            return new CliCommandResult(process.exitValue(), output, false);
        } catch (Exception ex) {
            return new CliCommandResult(-1, ex.getMessage(), false);
        }
    }

    private String readOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
}
