package com.khmelyuk.multirun;

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

public class MultirunRunConfiguration extends RunConfigurationBase implements RunnerSettings {

    public static final String PROP_SEPARATE_TABS = "separateTabs";
    public static final String PROP_REUSE_TABS_WITH_FAILURE = "reuseTabsWithFailures";
    public static final String PROP_START_ONE_BY_ONE = "startOneByOne";
    public static final String PROP_MARK_FAILED_PROCESS = "markFailedProcess";
    public static final String PROP_HIDE_SUCCESS_PROCESS = "hideSuccessProcess";
    public static final String PROP_DELAY_TIME = "delayTime";

    private double delayTime = 0;
    private boolean reuseTabs = true;
    private boolean reuseTabsWithFailure = false;
    private boolean startOneByOne = true;
    private boolean markFailedProcess = true;
    private boolean hideSuccessProcess = false;
    private List<RunConfigurationInternal> runConfigurations = new ArrayList<>();

    public MultirunRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public List<RunConfiguration> getRunConfigurations() {
        final List<RunConfiguration> result = new ArrayList<>();
        final List<RunConfiguration> allConfigurations = RunManager.getInstance(getProject()).getAllConfigurationsList();
        for (RunConfigurationInternal runConfiguration : runConfigurations) {
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
        this.runConfigurations = new ArrayList<>();
        if (runConfigurations == null) {
            return;
        }

        for (RunConfiguration configuration : runConfigurations) {
            this.runConfigurations.add(new RunConfigurationInternal(configuration.getName(), configuration.getType().getDisplayName()));
        }
    }

    public boolean isReuseTabs() {
        return reuseTabs;
    }

    public void setReuseTabs(boolean reuseTabs) {
        this.reuseTabs = reuseTabs;
    }

    public boolean isReuseTabsWithFailure() {
        return reuseTabsWithFailure;
    }

    public void setReuseTabsWithFailure(boolean reuseTabs) {
        this.reuseTabsWithFailure = reuseTabs;
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

    public double getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(double delayTime) {
        this.delayTime = delayTime;
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MultirunRunConfigurationEditor(getProject());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        if (element.getAttributeValue(PROP_SEPARATE_TABS) != null) {
            reuseTabs = !Boolean.parseBoolean(element.getAttributeValue(PROP_SEPARATE_TABS));
        }
        if (element.getAttributeValue(PROP_REUSE_TABS_WITH_FAILURE) != null) {
            reuseTabsWithFailure = Boolean.parseBoolean(element.getAttributeValue(PROP_REUSE_TABS_WITH_FAILURE));
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
        if (element.getAttributeValue(PROP_DELAY_TIME) != null) {
            delayTime = Double.parseDouble(element.getAttributeValue(PROP_DELAY_TIME));
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
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);

        element.setAttribute(PROP_SEPARATE_TABS, String.valueOf(!reuseTabs));
        element.setAttribute(PROP_REUSE_TABS_WITH_FAILURE, String.valueOf(reuseTabsWithFailure));
        element.setAttribute(PROP_START_ONE_BY_ONE, String.valueOf(startOneByOne));
        element.setAttribute(PROP_MARK_FAILED_PROCESS, String.valueOf(markFailedProcess));
        element.setAttribute(PROP_HIDE_SUCCESS_PROCESS, String.valueOf(hideSuccessProcess));
        element.setAttribute(PROP_DELAY_TIME, String.valueOf(delayTime));

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
    public ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider configurationInfoProvider) {
        return null;
    }

    @Nullable
    @Override
    public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner programRunner) {
        return null;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new MultirunRunnerState(getRunConfigurations(), startOneByOne, delayTime,
                                       reuseTabs, reuseTabsWithFailure,
                                       markFailedProcess, hideSuccessProcess);
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
