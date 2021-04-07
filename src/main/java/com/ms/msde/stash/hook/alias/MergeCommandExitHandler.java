package com.ms.msde.stash.hook.alias;

import com.atlassian.bitbucket.scm.CommandExitHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MergeCommandExitHandler implements CommandExitHandler {
    boolean wasSuccessful = false;

    @Override
    public void onCancel(@Nonnull String command, int exitCode, @Nullable String stdErr, @Nullable Throwable thrown) {
    }

    @Override
    public void onExit(@Nonnull String command, int exitCode, @Nullable String stdErr, @Nullable Throwable thrown) {
        wasSuccessful = (exitCode == 0);
    }
}
