package com.khmelyuk.multirun.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.khmelyuk.multirun.MultirunRunConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * TODO - javadoc me
 *
 * @author Ruslan Khmelyuk
 */
public class MultirunRunConfigurationEditor extends SettingsEditor<MultirunRunConfiguration> {
    private JPanel myMainPanel;
    private JList configurations;
    private JButton addConfiguration;
    private JButton removeConfiguration;

    public MultirunRunConfigurationEditor(Project project) {
    }

    @Override
    protected void resetEditorFrom(MultirunRunConfiguration multirunRunConfiguration) {

    }

    @Override
    protected void applyEditorTo(MultirunRunConfiguration multirunRunConfiguration) throws ConfigurationException {

    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myMainPanel;
    }

    @Override
    protected void disposeEditor() {
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
