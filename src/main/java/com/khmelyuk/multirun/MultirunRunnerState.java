package com.khmelyuk.multirun;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
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
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** @author Ruslan Khmelyuk */
public class MultirunRunnerState implements RunProfileState {

    private final double delayTime;
    private final boolean separateTabs;
    private final boolean startOneByOne;
    private final boolean markFailedProcess;
    private final boolean hideSuccessProcess;
    private final List<RunConfiguration> runConfigurations;
    private final StopRunningMultirunConfigurationsAction stopRunningMultirunConfiguration;

    public MultirunRunnerState(List<RunConfiguration> runConfigurations,
                               boolean startOneByOne, double delayTime, boolean separateTabs,
                               boolean markFailedProcess, boolean hideSuccessProcess) {

        this.delayTime = delayTime;
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
        stopRunningMultirunConfiguration.beginStartingConfigurations();
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
            final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), runConfiguration);
            if (runner == null) return;
            if (!checkRunConfiguration(executor, project, configuration)) return;

            ExecutionEnvironment executionEnvironment = new ExecutionEnvironment(executor, runner, configuration, project);

            executionEnvironment.setCallback(new ProgramRunner.Callback() {
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
                                        // ensure tab is not pinned
                                        content.setPinned(false);

                                        // mark running process tab with *
                                        content.setDisplayName(descriptor.getDisplayName() + "*");
                                    }
                                }
                            }

                            @Override public void processTerminated(final ProcessEvent processEvent) {
                                onTermination(processEvent, true);
                            }

                            @Override public void processWillTerminate(ProcessEvent processEvent, boolean willBeDestroyed) {}

                            private void onTermination(final ProcessEvent processEvent, final boolean terminated) {
                                if (descriptor.getAttachedContent() == null) {
                                    return;
                                }

                                ApplicationManager.getApplication().invokeLater(new Runnable() {
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

                                        if (separateTabs || !completedSuccessfully) {
                                            // attempt to pin tab if not completed successfully or asked not to reuse tabs
                                            if (!stopRunningMultirunConfiguration.isStopMultirunTriggered()) {
                                                // ... do not pin if multirun stopped by "Stop Multirun" action.
                                                content.setPinned(true);
                                            }
                                        }

                                        // remove the * used to identify running process
                                        content.setDisplayName(descriptor.getDisplayName());

                                        // add the alert icon in case if process existed with non-0 status
                                        if (markFailedProcess && processEvent.getExitCode() != 0) {
                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
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

                    final boolean moreConfigurationsToRun = index + 1 < runConfigurations.size();
                    if (startOneByOne && moreConfigurationsToRun) {
                        // start next configuration..

                        if (delayTime > 0) {
                            final long start = System.currentTimeMillis();
                            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Waiting for delay") {
                                @Override
                                public void run(@NotNull ProgressIndicator progressIndicator) {
                                    try {
                                        while (System.currentTimeMillis() - start < delayTime * 1000) {
                                            if (progressIndicator.isCanceled()) {
                                                return;
                                            }
                                            final double passed = (double) (System.currentTimeMillis() - start) / 1000;
                                            final String seconds = (delayTime - passed == 1)  ? "second" : "seconds";
                                            progressIndicator.setFraction(passed / delayTime);
                                            final String waitingPeriod = String.format("%.1f", delayTime - passed);
                                            progressIndicator.setText("waiting " + waitingPeriod + " " + seconds);
                                            Thread.sleep(100);
                                        }
                                    } catch (InterruptedException ignored) {
                                        return;
                                    }
                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                                        public void run() {
                                            runConfigurations(executor, runConfigurations, index + 1);
                                        }
                                    });
                                }
                            });
                        } else {
                            runConfigurations(executor, runConfigurations, index + 1);
                        }
                    }
                }
            });

            runner.execute(executionEnvironment);
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

    private boolean checkRunConfiguration(Executor executor, Project project, RunnerAndConfigurationSettings configuration) {
        ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);

        if (!ExecutionTargetManager.canRun(configuration, target)) {
            ExecutionUtil.handleExecutionError(
                    project, executor.getToolWindowId(), configuration.getConfiguration(),
                    new ExecutionException(StringUtil.escapeXml("Cannot run '" + configuration.getName() + "' on '" + target.getDisplayName() + "'")));
            return false;
        }

        if (!RunManagerImpl.canRunConfiguration(configuration, executor) || configuration.isEditBeforeRun()) {
            if (!RunDialog.editConfiguration(project, configuration, "Edit Configuration", executor)) {
                return false;
            }

            while (!RunManagerImpl.canRunConfiguration(configuration, executor)) {
                if (0 == Messages.showYesNoDialog(project, "Configuration is still incorrect. Do you want to edit it again?",
                                                  "Change Configuration Settings",
                                                  "Edit", "Continue Anyway", Messages.getErrorIcon())) {
                    if (!RunDialog.editConfiguration(project, configuration, "Edit Configuration", executor)) {
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
