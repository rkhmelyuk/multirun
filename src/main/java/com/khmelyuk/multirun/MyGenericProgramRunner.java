package com.khmelyuk.multirun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * This program runner is a through-the-ass way to star each configuration in separate tab.
 *
 * @author Ruslan Khmelyuk
 */
public class MyGenericProgramRunner extends GenericProgramRunner {

    private GenericProgramRunner runner;

    public MyGenericProgramRunner(GenericProgramRunner runner) {
        this.runner = runner;
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return runner.getRunnerId();
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return runner.canRun(executorId, profile);
    }

    @Override
    protected RunContentDescriptor doExecute(Project project, Executor executor, RunProfileState state,
                                             RunContentDescriptor contentToReuse, ExecutionEnvironment env) throws ExecutionException {
        try {
            Method doExecute = runner.getClass().getDeclaredMethod("doExecute", Project.class, Executor.class, RunProfileState.class,
                                                                   RunContentDescriptor.class, ExecutionEnvironment.class);
            doExecute.setAccessible(true);
            RunContentDescriptor descriptor = (RunContentDescriptor) doExecute.invoke(runner, project, executor, state, null, env);
            return new MyRunContentDescriptor(descriptor, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
