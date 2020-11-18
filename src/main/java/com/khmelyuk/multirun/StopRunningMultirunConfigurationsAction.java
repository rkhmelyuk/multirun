package com.khmelyuk.multirun;

import com.intellij.execution.KillableProcess;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The action to stop the running multirun configurations.
 *
 * @author Ruslan Khmelyuk
 */
public class StopRunningMultirunConfigurationsAction extends AnAction {

    private ConcurrentHashMap<Project, List<ProcessHandler>> processes = new ConcurrentHashMap<Project, List<ProcessHandler>>();
    private AtomicBoolean stopStartingConfigurations = new AtomicBoolean(false);
    private AtomicInteger startingCounter = new AtomicInteger(0);

    @Override public void update(AnActionEvent e) {
        super.update(e);
        if (e.getProject() == null) return;

        final Presentation presentation = e.getPresentation();
        final List<ProcessHandler> processes = this.processes.get(e.getProject());
        presentation.setEnabled(startingCounter.get() > 0 || hasNonTerminatedProcesses(processes));
    }

    private boolean hasNonTerminatedProcesses(List<ProcessHandler> processes) {
        if (processes != null && !processes.isEmpty()) {
            for(ProcessHandler each : processes) {
                if (!each.isProcessTerminated()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() == null) return;

        stopStartingConfigurations.set(true);
        System.out.println("Asked to stop running multirun configurations.");
        List<ProcessHandler> processesToStop = processes.get(e.getProject());
        if (processesToStop == null || processesToStop.isEmpty()) {
            System.out.println("Nothing to stop");
            return;
        }
        List<ProcessHandler> stoppedProcesses = new ArrayList<ProcessHandler>();
        for (ProcessHandler process : processesToStop) {

            stop(process);
            stoppedProcesses.add(process);
        }
        processesToStop.removeAll(stoppedProcesses);

        System.out.println("Stopped " + stoppedProcesses.size() + " processes");
    }

    public void addProcess(Project project, ProcessHandler process) {
        if (process == null) return;

        if (stopStartingConfigurations.get()) {
            stop(process);
            return;
        }
        this.processes.putIfAbsent(project, new CopyOnWriteArrayList<ProcessHandler>());
        this.processes.get(project).add(process);
    }

    private void stop(ProcessHandler processHandler) {
        if (processHandler instanceof KillableProcess && processHandler.isProcessTerminating()) {
            ((KillableProcess) processHandler).killProcess();
            return;
        }

        if (processHandler.detachIsDefault()) {
            processHandler.detachProcess();
        } else {
            processHandler.destroyProcess();
        }
    }

    public boolean canContinueStartingConfigurations() {
        return !stopStartingConfigurations.get();
    }

    public boolean isStopMultirunTriggered() {
        return stopStartingConfigurations.get();
    }

    // TODO - move to some component

    public void beginStartingConfigurations() {
        if (startingCounter.incrementAndGet() == 1) {
            stopStartingConfigurations.set(false);
        }
    }

    public void doneStaringConfigurations() {
        startingCounter.decrementAndGet();
    }
}
