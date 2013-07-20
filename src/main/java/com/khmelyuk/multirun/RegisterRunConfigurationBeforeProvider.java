package com.khmelyuk.multirun;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public class RegisterRunConfigurationBeforeProvider implements ProjectComponent {
    private Project project;
    private BeforeRunTaskProvider beforeRunProvider;

    public RegisterRunConfigurationBeforeProvider(Project project) {
        this.project = project;
        this.beforeRunProvider = new RunConfigurationBeforeRunProvider(project);
    }

    public void initComponent() {
        for (BeforeRunTaskProvider extension: Extensions.getExtensions(BeforeRunTaskProvider.EXTENSION_POINT_NAME, project)) {
            if (extension instanceof RunConfigurationBeforeRunProvider) {
                // no need to register provider
                return;
            }
        }

        // register the provider
        Extensions.getArea(project).getExtensionPoint(BeforeRunTaskProvider.EXTENSION_POINT_NAME).registerExtension(beforeRunProvider);
    }

    public void disposeComponent() {
        // unregister the provider
        Extensions.getArea(project).getExtensionPoint(BeforeRunTaskProvider.EXTENSION_POINT_NAME).unregisterExtension(beforeRunProvider);
    }

    @NotNull
    public String getComponentName() {
        return "RegisterRunConfigurationBeforeProvider";
    }

    public void projectOpened() {
        // called when project is opened
    }

    public void projectClosed() {
        // called when project is being closed
    }
}
