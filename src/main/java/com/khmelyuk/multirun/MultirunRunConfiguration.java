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
    public static final String PROP_HIDE_SUCCESS_PROCESS = "hideSuccessProcess";

    private boolean separateTabs = true;
    private boolean startOneByOne = true;
    private boolean markFailedProcess = true;
    private boolean hideSuccessProcess = false;
    private List<RunConfigurationInternal> runConfigurations = new ArrayList<RunConfigurationInternal>();

    public MultirunRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public List<RunConfiguration> getRunConfigurations() {
        final List<RunConfiguration> result = new ArrayList<RunConfiguration>();
        final RunConfiguration[] allConfigurations = RunManager.getInstance(getProject()).getAllConfigurations();
        for (RunConfigurationInternal runConfiguration: runConfigurations) {
            for (RunConfiguration configuration : allConfigurations) {
                if (configuration.getName().equals(runConfiguration.name) &&
                        configuration.getType().getDisplayName().equals(runConfiguration.type)) {
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
                    result.add(configuration);
                    break;
                }
            }
        }
        return result;
    }

    public void setRunConfigurations(List<RunConfiguration> runConfigurations) {
        this.runConfigurations = new ArrayList<RunConfigurationInternal>();
        if (runConfigurations == null) {
            return;
        }

        for(RunConfiguration configuration: runConfigurations) {
            this.runConfigurations.add(new RunConfigurationInternal(configuration.getName(), configuration.getType().getDisplayName()));
        }
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

    public boolean isHideSuccessProcess() {
        return hideSuccessProcess;
    }

    public void setHideSuccessProcess(boolean hideSuccessProcess) {
        this.hideSuccessProcess = hideSuccessProcess;
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MultirunRunConfigurationEditor(getProject());
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        if (element.getAttributeValue(PROP_SEPARATE_TABS) != null) {
            separateTabs = Boolean.parseBoolean(element.getAttributeValue(PROP_SEPARATE_TABS));
        }
        if (element.getAttributeValue(PROP_START_ONE_BY_ONE) != null) {
            startOneByOne = Boolean.parseBoolean(element.getAttributeValue(PROP_START_ONE_BY_ONE));
        }
        if (element.getAttributeValue(PROP_MARK_FAILED_PROCESS) != null) {
            markFailedProcess = Boolean.parseBoolean(element.getAttributeValue(PROP_MARK_FAILED_PROCESS));
        }
        if (element.getAttributeValue(PROP_HIDE_SUCCESS_PROCESS) != null) {
            hideSuccessProcess = Boolean.parseBoolean(element.getAttributeValue(PROP_HIDE_SUCCESS_PROCESS));
        }

        for (Object each : element.getContent()) {
            if (!(each instanceof Element)) {
                continue;
            }
            final Element eachElement = (Element) each;
            if (!eachElement.getName().equals("runConfiguration")) {
                continue;
            }
            runConfigurations.add(new RunConfigurationInternal(eachElement.getAttributeValue("name"),
                                                               eachElement.getAttributeValue("type")));
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        element.setAttribute(PROP_SEPARATE_TABS, String.valueOf(separateTabs));
        element.setAttribute(PROP_START_ONE_BY_ONE, String.valueOf(startOneByOne));
        element.setAttribute(PROP_MARK_FAILED_PROCESS, String.valueOf(markFailedProcess));
        element.setAttribute(PROP_HIDE_SUCCESS_PROCESS, String.valueOf(hideSuccessProcess));

        final List<Element> configurations = new ArrayList<Element>();
        for (RunConfigurationInternal each : runConfigurations) {
            Element runConfiguration = new Element("runConfiguration");
            runConfiguration.setAttribute("name", each.name);
            runConfiguration.setAttribute("type", each.type);
            configurations.add(runConfiguration);
        }
        element.setContent(configurations);
    }

    @Nullable
    @Override
    public ConfigurationPerRunnerSettings createRunnerSettings( ConfigurationInfoProvider configurationInfoProvider ) {
        return null;
    }

    @Nullable
    @Override
    public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor( ProgramRunner programRunner ) {
        return null;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new MultirunRunnerState(getName(), getRunConfigurations(), startOneByOne, separateTabs, markFailedProcess, hideSuccessProcess);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (runConfigurations.isEmpty()) {
            throw new RuntimeConfigurationError("No run configuration chosen");
        }
    }

    private static class RunConfigurationInternal {
        String name;
        String type;

        RunConfigurationInternal() {
        }

        RunConfigurationInternal(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
