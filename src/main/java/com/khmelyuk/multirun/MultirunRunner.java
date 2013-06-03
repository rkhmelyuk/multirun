package com.khmelyuk.multirun;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import org.jetbrains.annotations.NotNull;

/**
 * Runner for Multirun configurations.
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunner extends DefaultProgramRunner {

    @NotNull
    @Override
    public String getRunnerId() {
        return "multirun";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile runProfile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && runProfile instanceof MultirunRunConfiguration;
    }
}
