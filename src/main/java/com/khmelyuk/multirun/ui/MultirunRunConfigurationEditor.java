package com.khmelyuk.multirun.ui;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.khmelyuk.multirun.MultirunRunConfiguration;
import com.khmelyuk.multirun.RunConfigurationHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * For to edit multirun run configuration.
 *
 * @author Ruslan Khmelyuk
 */
@SuppressWarnings("unchecked")
public class MultirunRunConfigurationEditor extends SettingsEditor<MultirunRunConfiguration> {

    private Project project;
    private JPanel myMainPanel;
    private JBList<RunConfiguration> configurations;
    private JPanel collectionsPanel;
    private JCheckBox reuseTabs;
    private JCheckBox reuseTabsWithFailure;
    private JCheckBox startOneByOne;
    private JCheckBox markFailedProcess;
    private JCheckBox hideSuccessProcess;
    private JCheckBox configurationsListChanged;
    private JTextField delayTime;
    private MultirunRunConfiguration configuration;

    public MultirunRunConfigurationEditor(final Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@Nullable MultirunRunConfiguration multirunRunConfiguration) {
        if (multirunRunConfiguration != null) {
            this.configuration = multirunRunConfiguration;
        }

        final DefaultListModel<RunConfiguration> listModel = new DefaultListModel<>();
        if (this.configuration != null) {
            for (RunConfiguration each : this.configuration.getRunConfigurations()) {
                listModel.addElement(each);
            }
        }
        configurations.setModel(listModel);
        configurations.getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                RunConfiguration[] buffer = new RunConfiguration[configurations.getModel().getSize()];
                ((DefaultListModel<RunConfiguration>) configurations.getModel()).copyInto(buffer);
                if (MultirunRunConfigurationEditor.this.configuration != null) {
                    configuration.setRunConfigurations(Arrays.asList(buffer));
                }
                fireEditorStateChanged();
            }
        });
        configurations.setCellRenderer(new RunConfigurationListCellRenderer());

        if (this.configuration != null) {
            delayTime.setText(String.format("%.1f", this.configuration.getDelayTime()));
            reuseTabs.setSelected(this.configuration.isReuseTabs());
            reuseTabsWithFailure.setSelected(this.configuration.isReuseTabsWithFailure());
            startOneByOne.setSelected(this.configuration.isStartOneByOne());
            markFailedProcess.setSelected(this.configuration.isMarkFailedProcess());
            hideSuccessProcess.setSelected(this.configuration.isHideSuccessProcess());
            delayTime.setEnabled(startOneByOne.isSelected());
        }
    }

    @Override
    protected void applyEditorTo(@Nullable MultirunRunConfiguration multirunRunConfiguration) {
        if (multirunRunConfiguration == null) {
            return;
        }

        multirunRunConfiguration.setReuseTabs(reuseTabs.isSelected());
        multirunRunConfiguration.setReuseTabsWithFailure(reuseTabsWithFailure.isSelected());
        multirunRunConfiguration.setStartOneByOne(startOneByOne.isSelected());
        multirunRunConfiguration.setMarkFailedProcess(markFailedProcess.isSelected());
        multirunRunConfiguration.setHideSuccessProcess(hideSuccessProcess.isSelected());
        double delayTimeSeconds = 0;
        if (delayTime.getText() != null && !delayTime.getText().isEmpty()) {
            try {
                delayTimeSeconds = Double.parseDouble(delayTime.getText());
            } catch (Exception e) {
                // well ignore if the value is not a number
            }
        }
        multirunRunConfiguration.setDelayTime(delayTimeSeconds);

        RunConfiguration[] buffer = new RunConfiguration[configurations.getModel().getSize()];
        ((DefaultListModel<RunConfiguration>) configurations.getModel()).copyInto(buffer);
        MultirunRunConfigurationEditor.this.configuration.setRunConfigurations(Arrays.asList(buffer));
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        configurations = new JBList<>();
        configurations.getEmptyText().setText("Add run configurations to this list");
        final ToolbarDecorator myDecorator = ToolbarDecorator.createDecorator(configurations);
        myDecorator.initPosition();

        myDecorator.setRemoveAction(anActionButton -> {
            ListUtil.removeSelectedItems(configurations);
            markConfigurationsChanged();
        });
        myDecorator.setAddAction(button -> {
            final JBList<RunConfiguration> list = new JBList<>(getConfigurationsToAdd());
            list.setCellRenderer(new RunConfigurationListCellRenderer());
            new PopupChooserBuilder<>(list)
                    .setItemChosenCallback(() -> {
                        int[] selectedIndices = list.getSelectedIndices();
                        for (int index : selectedIndices) {
                            RunConfiguration selectedRunConfiguration = list.getModel().getElementAt(index);
                            if (selectedRunConfiguration != null) {
                                ((DefaultListModel<RunConfiguration>) configurations.getModel()).addElement(selectedRunConfiguration);
                            }

                            markConfigurationsChanged();
                        }
                    })
                    .createPopup()
                    .showUnderneathOf(button.getContextComponent());
        });
        myDecorator.setAddActionUpdater(e -> !getConfigurationsToAdd().isEmpty());

        startOneByOne.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                delayTime.setEnabled(startOneByOne.isSelected());
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel myDecoratorPanel = myDecorator.createPanel();
        panel.add(myDecoratorPanel, BorderLayout.CENTER);
        collectionsPanel.add(myDecoratorPanel);

        configurationsListChanged.setVisible(false);

        return myMainPanel;
    }

    private void markConfigurationsChanged() {
        // use hidden checkbox to fire the modified event;
        configurationsListChanged.setSelected(!configurationsListChanged.isSelected());
    }

    @Override
    protected void disposeEditor() {
    }

    private static class RunConfigurationListCellRenderer extends ListCellRendererWrapper<RunConfiguration> {
        @Override
        public void customize(JList list, RunConfiguration data, int index, boolean selected, boolean hasFocus) {
            if (data != null) {
                setIcon(data.getIcon());
                setText("Run '" + data.getName() + "'");
            }
        }
    }

    private java.util.List<RunConfiguration> getConfigurationsToAdd() {
        java.util.List<RunConfiguration> result = new ArrayList<>();
        if (this.configuration == null) {
            return result;
        }

        java.util.List<RunConfiguration> allConfigurations = RunManager.getInstance(project).getAllConfigurationsList();
        for (RunConfiguration configuration : allConfigurations) {
            if (this.configuration.equals(configuration)) {
                // skip current
                continue;
            }
            if (this.configuration.getRunConfigurations().contains(configuration)) {
                // skip already added
                continue;
            }
            if (configuration instanceof MultirunRunConfiguration) {
                // exclude configurations that may cause loopies
                if (RunConfigurationHelper.containsLoopies((MultirunRunConfiguration) configuration, this.configuration)) {
                    continue;
                }
            }
            result.add(configuration);
        }

        return result;
    }

}
