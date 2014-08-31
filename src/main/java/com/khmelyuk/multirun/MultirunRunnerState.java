package com.khmelyuk.multirun;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunnableState;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** @author Ruslan Khmelyuk */
public class MultirunRunnerState implements RunnableState {

    private String name;
    private boolean separateTabs;
    private boolean startOneByOne;
    private boolean markFailedProcess;
    private boolean hideSuccessProcess = true;
    private List<RunConfiguration> runConfigurations;
    private StopRunningMultirunConfigurationsAction stopRunningMultirunConfiguration;

    public MultirunRunnerState(String name,
                               List<RunConfiguration> runConfigurations,
                               boolean startOneByOne, boolean separateTabs,
                               boolean markFailedProcess, boolean hideSuccessProcess) {

        this.name = name;
        this.separateTabs = separateTabs;
        this.startOneByOne = startOneByOne;
        this.runConfigurations = runConfigurations;
        this.markFailedProcess = markFailedProcess;
        this.hideSuccessProcess = hideSuccessProcess;

        ActionManager actionManager = ActionManagerImpl.getInstance();
        stopRunningMultirunConfiguration = (StopRunningMultirunConfigurationsAction) actionManager.getAction("stopRunningMultirunConfiguration");
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        stopRunningMultirunConfiguration.beginStaringConfigurations();
        runConfigurations(executor, runConfigurations, 0);
        return null;
    }

    private void runConfigurations(final Executor executor, final List<RunConfiguration> runConfigurations, final int index) {
        if (index >= runConfigurations.size()) {
            stopRunningMultirunConfiguration.doneStaringConfigurations();
            return;
        }
        if (!stopRunningMultirunConfiguration.canContinueStartingConfigurations()) {
            stopRunningMultirunConfiguration.doneStaringConfigurations();
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
            ExecutionEnvironment executionEnvironment = new ExecutionEnvironment(
                    executor, runner, DefaultExecutionTarget.INSTANCE,
                    configuration, project);

            runner.execute(executionEnvironment, new ProgramRunner.Callback() {
                @SuppressWarnings("ConstantConditions")
                @Override
                public void processStarted(final RunContentDescriptor descriptor) {
                    if (descriptor == null) {
                        if (startOneByOne) {
                            // start next configuration..
                            runConfigurations(executor, runConfigurations, index + 1);
                        }
                        return;
                    }

                    final ProcessHandler processHandler = descriptor.getProcessHandler();
                    if (processHandler != null) {
                        processHandler.addProcessListener(new ProcessAdapter() {
                            @SuppressWarnings("ConstantConditions")
                            @Override
                            public void startNotified(ProcessEvent processEvent) {
                                Content content = descriptor.getAttachedContent();
                                if (content != null) {
                                    content.setIcon(descriptor.getIcon());
                                    if (!stopRunningMultirunConfiguration.canContinueStartingConfigurations()) {
                                        // Multirun was stopped - destroy processes that are still starting up
                                        processHandler.destroyProcess();

                                        if (!content.isPinned() && !startOneByOne) {
                                            // checks if not pinned, to avoid destroying already existed tab
                                            // checks if start one by one - no need to close the console tab, as it's won't be shown
                                            // as other checks disallow starting it

                                            // content.getManager() can be null, if content is removed already as part of destroy above
                                            if (content.getManager() != null) {
                                                content.getManager().removeContent(content, false);
                                            }
                                        }
                                    } else {
                                        // mark all current console tab as pinned
                                        content.setPinned(true);

                                        // mark running process tab with *
                                        content.setDisplayName(descriptor.getDisplayName() + "*");
                                    }
                                }
                            }

                            @Override public void processTerminated(final ProcessEvent processEvent) {
                                onTermination(processEvent, true);
                            }

                            @Override public void processWillTerminate(ProcessEvent processEvent, boolean willBeDestroyed) {
                                onTermination(processEvent, false);
                            }

                            private void onTermination(final ProcessEvent processEvent, final boolean terminated) {
                                if (descriptor.getAttachedContent() == null) {
                                    return;
                                }

                                LaterInvocator.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Content content = descriptor.getAttachedContent();
                                        if (content == null) return;

                                        // exit code is 0 if the process completed successfully
                                        final boolean completedSuccessfully = (terminated && processEvent.getExitCode() == 0);

                                        if (hideSuccessProcess && completedSuccessfully) {
                                            // close the tab for the success process and exit - nothing else could be done
                                            if (content.getManager() != null) {
                                                content.getManager().removeContent(content, false);
                                                return;
                                            }
                                        }

                                        if (!separateTabs && completedSuccessfully) {
                                            // un-pin the console tab if re-use is allowed and process completed successfully,
                                            // so the tab could be re-used for other processes
                                            content.setPinned(false);
                                        }

                                        // remove the * used to identify running process
                                        content.setDisplayName(descriptor.getDisplayName());

                                        // add the alert icon in case if process existed with non-0 status
                                        if (markFailedProcess && processEvent.getExitCode() != 0) {
                                            LaterInvocator.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    content.setIcon(LayeredIcon.create(content.getIcon(), AllIcons.Nodes.TabAlert));
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    }
                    stopRunningMultirunConfiguration.addProcess(project, processHandler);

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
}
