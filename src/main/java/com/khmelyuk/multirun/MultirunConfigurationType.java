package com.khmelyuk.multirun;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

public class MultirunConfigurationType extends SimpleConfigurationType implements ConfigurationType {

    public MultirunConfigurationType() {
        super("Multirun", "Multirun", "Run multiple configurations",
              NotNullLazyValue.createValue(() -> AllIcons.Actions.Rerun));
    }

    @Override
    public @NotNull
    RunConfiguration createTemplateConfiguration(@NotNull final Project project) {
        return new MultirunRunConfiguration(project, this, "Multirun");
    }
}
