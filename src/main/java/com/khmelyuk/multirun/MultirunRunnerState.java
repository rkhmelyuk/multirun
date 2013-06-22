package com.khmelyuk.multirun;

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.runners.*;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** @author Ruslan Khmelyuk */
public class MultirunRunnerState implements RunnableState {

    private boolean separateTabs;
    private boolean startOneByOne;
    private List<RunConfiguration> runConfigurations;

    public MultirunRunnerState(List<RunConfiguration> runConfigurations, boolean startOneByOne, boolean separateTabs) {
        this.startOneByOne = startOneByOne;
        this.separateTabs = separateTabs;
        this.runConfigurations = runConfigurations;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        runConfigurations(executor, runConfigurations, 0);
        return null;
    }

    private void runConfigurations(final Executor executor, final List<RunConfiguration> runConfigurations, final int index) {
        if (runConfigurations.size() <= index) {
            return;
        }
        final RunConfiguration runConfiguration = runConfigurations.get(index);
        final Project project = runConfiguration.getProject();
        final RunnerAndConfigurationSettings configuration = new RunnerAndConfigurationSettingsImpl(
                RunManagerImpl.getInstanceImpl(project), runConfiguration, false);

        boolean started = false;
        try {
            ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), runConfiguration);
            if (runner == null) {
                return;
            }

            if (!checkRunConfiguration(executor, project, configuration)) {
                return;
            }

            runTriggers(executor, configuration);

            RunContentDescriptor runContentDescriptor = null;
            if (separateTabs) {
                runContentDescriptor = getRunContentDescriptor(runConfiguration, project);
                if (runContentDescriptor == null && runner instanceof GenericProgramRunner) {
                    // use custom runner that will start each configuration in separate task
                    runner = new MyGenericProgramRunner((GenericProgramRunner) runner);
                }
            }

            ExecutionEnvironment executionEnvironment = new ExecutionEnvironmentBuilder()
                    .setRunnerAndSettings(runner, configuration)
                    .setRunProfile(runConfiguration)
                    .setContentToReuse(runContentDescriptor)
                    .setProject(project).build();

            runner.execute(executor, executionEnvironment, new ProgramRunner.Callback() {
                @Override
                public void processStarted(final RunContentDescriptor descriptor) {
                    if (startOneByOne) {
                        runConfigurations(executor, runConfigurations, index + 1);
                    }
                }
            });
            started = true;
        } catch (ExecutionException e) {
            ExecutionUtil.handleExecutionError(project, executor.getToolWindowId(), configuration.getConfiguration(), e);
        } finally {
            // start the next one
            if (!startOneByOne) {
                runConfigurations(executor, runConfigurations, index + 1);
            } else if (!started) {
                // failed to start current, means the chain is broken
                runConfigurations(executor, runConfigurations, index + 1);
            }
        }
    }

    private void runTriggers(Executor executor, RunnerAndConfigurationSettings configuration) {
        final ConfigurationType configurationType = configuration.getType();
        if (configurationType != null) {
            UsageTrigger.trigger("execute." + ConvertUsagesUtil.ensureProperKey(configurationType.getId()) + "." + executor.getId());
        }
    }

    private boolean checkRunConfiguration(Executor executor, Project project, RunnerAndConfigurationSettings configuration) {
        ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);

        if (!ExecutionTargetManager.canRun(configuration, target)) {
            ExecutionUtil.handleExecutionError(
                    project, executor.getToolWindowId(), configuration.getConfiguration(),
                    new ExecutionException(StringUtil.escapeXml("Cannot run '" + configuration.getName() + "' on '" + target.getDisplayName() + "'")));
            return false;
        }

        if (!RunManagerImpl.canRunConfiguration(configuration, executor) || configuration.isEditBeforeRun()) {
            if (!RunDialog.editConfiguration(project, configuration, "Edit configuration", executor)) {
                return false;
            }

            while (!RunManagerImpl.canRunConfiguration(configuration, executor)) {
                if (0 == Messages.showYesNoDialog(project, "Configuration is still incorrect. Do you want to edit it again?",
                                                  "Change Configuration Settings",
                                                  "Edit", "Continue Anyway", Messages.getErrorIcon())) {
                    if (!RunDialog.editConfiguration(project, configuration, "Edit configuration", executor)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return true;
    }

    private RunContentDescriptor getRunContentDescriptor(final RunConfiguration runConfiguration, Project project) {
        List<RunContentDescriptor> runContentDescriptors = ExecutionHelper.collectConsolesByDisplayName(
                project,
                new NotNullFunction<String, Boolean>() {
                    @NotNull
                    @Override
                    public Boolean fun(String dom) {
                        return runConfiguration.getName().equals(dom);
                    }
                });

        return !runContentDescriptors.isEmpty() ? runContentDescriptors.get(0) : null;
    }

    @Override
    public RunnerSettings getRunnerSettings() {
        return null;
    }

    @Override
    public ConfigurationPerRunnerSettings getConfigurationSettings() {
        return null;
    }
}
