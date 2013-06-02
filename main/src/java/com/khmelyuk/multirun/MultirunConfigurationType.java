package com.khmelyuk.multirun;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;

public class MultirunConfigurationType extends ConfigurationTypeBase {
    public MultirunConfigurationType() {
        super("Multirun", "Multirun", "Run multiple configurations", AllIcons.Actions.Rerun);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new MultirunRunConfiguration(project, this, "Multirun");
            }
        });
    }
}
