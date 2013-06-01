package com.khmelyuk.multirun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizable;
import com.khmelyuk.multirun.ui.MultirunRunConfigurationEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO - javadoc me
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunConfiguration extends RunConfigurationBase {

    private List<RunConfiguration> runConfigurations = new ArrayList<RunConfiguration>();

    public MultirunRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);

        System.out.println("run configuration " + project.getComponent(RunConfigurationExtension.class));
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MultirunRunConfigurationEditor(getProject());
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
        return new MultirunRunnerState();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (runConfigurations.isEmpty()) {
            throw new RuntimeConfigurationError("No run configuration chosen");
        }
    }
}
