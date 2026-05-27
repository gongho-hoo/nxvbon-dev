package com.nxvibeon.backend.scm.core;

public record CliCommandResult(
    int exitCode,
    String output,
    boolean timedOut
) {
    public boolean success() {
        return exitCode == 0 && !timedOut;
    }
}
