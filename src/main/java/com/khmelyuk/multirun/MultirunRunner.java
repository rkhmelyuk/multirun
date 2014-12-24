package com.khmelyuk.multirun;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Runner for Multirun configurations.
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunner extends DefaultProgramRunner {

    public static final String JREBEL_EXECUTOR_ID = "JRebel Executor";
    public static final String JREBEL_DEBUG_ID = "JRebel Debug";

    @NotNull
    @Override
    public String getRunnerId() {
        return "multirun";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile runProfile) {
        return runProfile instanceof MultirunRunConfiguration &&
                (DefaultRunExecutor.EXECUTOR_ID.equals(executorId)
                        || DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)
                        || JREBEL_EXECUTOR_ID.equalsIgnoreCase(executorId)
                        || JREBEL_DEBUG_ID.equalsIgnoreCase(executorId));
    }
}
