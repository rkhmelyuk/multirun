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

    private boolean separateTabs = false;
    private boolean startOneByOne = true;
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

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MultirunRunConfigurationEditor(getProject());
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        startOneByOne = Boolean.parseBoolean(element.getAttributeValue("startOneByOne"));
        separateTabs = Boolean.parseBoolean(element.getAttributeValue("separateTabs"));

        RunConfiguration[] allConfigurations = RunManager.getInstance(getProject()).getAllConfigurations();
        for (Object each : element.getContent()) {
            if (!(each instanceof Element)) {
                continue;
            }
            Element eachElement = (Element) each;
            if (!eachElement.getName().equals("runConfiguration")) {
                continue;
            }
            for (RunConfiguration configuration : allConfigurations) {
                if (configuration.getName().equals(eachElement.getAttributeValue("name")) &&
                        configuration.getType().getDisplayName().equals(eachElement.getAttributeValue("type"))) {
                    runConfigurations.add(configuration);
                    break;
                }
            }

        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute("startOneByOne", String.valueOf(startOneByOne));
        element.setAttribute("separateTabs", String.valueOf(separateTabs));
        List<Element> configurations = new ArrayList<Element>();
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
        return new MultirunRunnerState(runConfigurations, startOneByOne, separateTabs);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (runConfigurations.isEmpty()) {
            throw new RuntimeConfigurationError("No run configuration chosen");
        }
    }
}
