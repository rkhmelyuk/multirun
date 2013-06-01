package com.khmelyuk.multirun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO - javadoc me
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunnerState implements RunProfileState {
    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    public RunnerSettings getRunnerSettings() {
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    public ConfigurationPerRunnerSettings getConfigurationSettings() {
        throw new UnsupportedOperationException("implement me");
    }
}
