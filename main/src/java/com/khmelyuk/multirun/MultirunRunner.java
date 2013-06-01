package com.khmelyuk.multirun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO - javadoc me
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunner extends GenericProgramRunner {

    @NotNull
    @Override
    public String getRunnerId() {
        return "multirun";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile runProfile) {
        return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && runProfile instanceof MultirunRunConfiguration;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(Project project, Executor executor, RunProfileState runProfileState, RunContentDescriptor runContentDescriptor, ExecutionEnvironment executionEnvironment) throws ExecutionException {
        throw new UnsupportedOperationException("implement me");
    }
}
