package com.khmelyuk.multirun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.khmelyuk.multirun.ui.MultirunRunConfigurationEditor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MultirunRunConfiguration extends RunConfigurationBase {

    public static final String PROP_SEPARATE_TABS = "separateTabs";
    public static final String PROP_START_ONE_BY_ONE = "startOneByOne";
    public static final String PROP_MARK_FAILED_PROCESS = "markFailedProcess";

    private boolean separateTabs = true;
    private boolean startOneByOne = true;
    private boolean markFailedProcess = true;
    private List<RunConfiguration> runConfigurations = new ArrayList<RunConfiguration>();

    public MultirunRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public List<RunConfiguration> getRunConfigurations() {
        return runConfigurations;
    }

    public void setRunConfigurations(List<RunConfiguration> runConfigurations) {
        this.runConfigurations = runConfigurations;
    }

    public boolean isSeparateTabs() {
        return separateTabs;
    }

    public void setSeparateTabs(boolean separateTabs) {
        this.separateTabs = separateTabs;
    }

    public boolean isStartOneByOne() {
        return startOneByOne;
    }

    public void setStartOneByOne(boolean startOneByOne) {
        this.startOneByOne = startOneByOne;
    }

    public boolean isMarkFailedProcess() {
        return markFailedProcess;
    }

    public void setMarkFailedProcess(boolean markFailedProcess) {
        this.markFailedProcess = markFailedProcess;
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MultirunRunConfigurationEditor(getProject());
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        if (element.getAttributeValue(PROP_START_ONE_BY_ONE) != null) {
            startOneByOne = Boolean.parseBoolean(element.getAttributeValue(PROP_START_ONE_BY_ONE));
        }
        if (element.getAttributeValue(PROP_SEPARATE_TABS) != null) {
            separateTabs = Boolean.parseBoolean(element.getAttributeValue(PROP_SEPARATE_TABS));
        }
        if (element.getAttributeValue(PROP_MARK_FAILED_PROCESS) != null) {
            markFailedProcess = Boolean.parseBoolean(element.getAttributeValue(PROP_MARK_FAILED_PROCESS));
        }

        final RunConfiguration[] allConfigurations = RunManager.getInstance(getProject()).getAllConfigurations();
        for (Object each : element.getContent()) {
            if (!(each instanceof Element)) {
                continue;
            }
            final Element eachElement = (Element) each;
            if (!eachElement.getName().equals("runConfiguration")) {
                continue;
            }
            for (RunConfiguration configuration : allConfigurations) {
                if (configuration.getName().equals(eachElement.getAttributeValue("name")) &&
                        configuration.getType().getDisplayName().equals(eachElement.getAttributeValue("type"))) {
                    if (configuration instanceof MultirunRunConfiguration) {
                        if (configuration.equals(this)) {
                            // exclude itself
                            break;
                        }
                        if (RunConfigurationHelper.containsLoopies((MultirunRunConfiguration) configuration, this)) {
                            // disallow adding multirun configuration that causes looping
                            break;
                        }
                    }
                    runConfigurations.add(configuration);
                    break;
                }
            }

        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        element.setAttribute(PROP_START_ONE_BY_ONE, String.valueOf(startOneByOne));
        element.setAttribute(PROP_SEPARATE_TABS, String.valueOf(separateTabs));
        element.setAttribute(PROP_MARK_FAILED_PROCESS, String.valueOf(markFailedProcess));

        final List<Element> configurations = new ArrayList<Element>();
        for (RunConfiguration each : runConfigurations) {
            Element runConfiguration = new Element("runConfiguration");
            runConfiguration.setAttribute("name", each.getName());
            runConfiguration.setAttribute("type", each.getType().getDisplayName());
            configurations.add(runConfiguration);
        }
        element.setContent(configurations);
    }

    @Nullable
    @Override
    public JDOMExternalizable createRunnerSettings(ConfigurationInfoProvider configurationInfoProvider) {
        return null;
    }

    @Nullable
    @Override
    public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(ProgramRunner programRunner) {
        return null;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new MultirunRunnerState(runConfigurations, startOneByOne, separateTabs, markFailedProcess);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (runConfigurations.isEmpty()) {
            throw new RuntimeConfigurationError("No run configuration chosen");
        }
    }
}
