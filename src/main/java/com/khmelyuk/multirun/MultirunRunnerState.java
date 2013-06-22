package com.khmelyuk.multirun;

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
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
    private StopRunningMultirunConfigurationsAction stopRunningMultirunConfiguration;

    public MultirunRunnerState(List<RunConfiguration> runConfigurations, boolean startOneByOne, boolean separateTabs) {
        this.startOneByOne = startOneByOne;
        this.separateTabs = separateTabs;
        this.runConfigurations = runConfigurations;

        ActionManager actionManager = ActionManagerImpl.getInstance();
        stopRunningMultirunConfiguration = (StopRunningMultirunConfigurationsAction) actionManager.getAction("stopRunningMultirunConfiguration");
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        stopRunningMultirunConfiguration.beginStaringConfigurations();
        runConfigurations(executor, runConfigurations, 0);
        stopRunningMultirunConfiguration.doneStaringConfigurations();
        return null;
    }

    private void runConfigurations(final Executor executor, final List<RunConfiguration> runConfigurations, final int index) {
        if (runConfigurations.size() <= index) {
            return;
        }
        if (!stopRunningMultirunConfiguration.canContinueStartingConfigurations()) {
            // don't start more configurations if user stopped the plugin work.
            return;
        }

        final RunConfiguration runConfiguration = runConfigurations.get(index);
        final Project project = runConfiguration.getProject();
        final RunnerAndConfigurationSettings configuration = new RunnerAndConfigurationSettingsImpl(
                RunManagerImpl.getInstanceImpl(project), runConfiguration, false);

        boolean started = false;
        try {
            ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), runConfiguration);
            if (runner == null) return;
            if (!checkRunConfiguration(executor, project, configuration)) return;

            runTriggers(executor, configuration);
            RunContentDescriptor runContentDescriptor = getRunContentDescriptor(runConfiguration, project);
            ExecutionEnvironment executionEnvironment = new ExecutionEnvironmentBuilder()
                    .setRunnerAndSettings(runner, configuration)
                    .setRunProfile(runConfiguration)
                    .setContentToReuse(runContentDescriptor)
                    .setProject(project).build();

            runner.execute(executor, executionEnvironment, new ProgramRunner.Callback() {
                @SuppressWarnings("ConstantConditions")
                @Override
                public void processStarted(final RunContentDescriptor descriptor) {
                    if (descriptor.getProcessHandler() != null) {
                        descriptor.getProcessHandler().addProcessListener(new ProcessListener() {
                            @SuppressWarnings("ConstantConditions")
                            @Override
                            public void startNotified(ProcessEvent processEvent) {
                                if (descriptor.getAttachedContent() != null) {
                                    if (!stopRunningMultirunConfiguration.canContinueStartingConfigurations()) {
                                        descriptor.getProcessHandler().destroyProcess();
                                        if (!descriptor.getAttachedContent().isPinned() && !startOneByOne) {
                                            // check if not pinned, to avoid destroying already existed tab
                                            // and if start one by one - no need to close the console tab, as it's won't be shown
                                            // as other checks disallow starting it
                                            descriptor.getAttachedContent().getManager().removeContent(descriptor.getAttachedContent(), false);
                                        }
                                    } else {
                                        // mark all current console tab as pinned
                                        descriptor.getAttachedContent().setPinned(true);
                                    }
                                }
                            }

                            @Override public void processTerminated(ProcessEvent processEvent) {
                                if (descriptor.getAttachedContent() != null) {
                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        @Override public void run() {
                                            if (!separateTabs) {
                                                // un-pin the console tab if re-use is allowed, so the tab could be re-used soon
                                                descriptor.getAttachedContent().setPinned(false);
                                            }
                                        }
                                    });
                                }
                            }

                            @Override public void processWillTerminate(ProcessEvent processEvent, boolean b) { }

                            @Override public void onTextAvailable(ProcessEvent processEvent, Key key) { }
                        });
                    }
                    stopRunningMultirunConfiguration.addProcess(project, descriptor.getProcessHandler());

                    if (startOneByOne) {
                        // start next configuration..
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
                    public Boolean fun(String name) {
                        return runConfiguration.getName().equals(name);
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
