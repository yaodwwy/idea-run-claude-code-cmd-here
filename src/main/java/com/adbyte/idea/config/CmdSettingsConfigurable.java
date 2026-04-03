package com.adbyte.idea.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CmdSettingsConfigurable implements Configurable {

    private JPanel mainPanel;
    private JBList<CmdConfig> commandsList;
    private CmdConfigPanel editPanel;
    private List<CmdConfig> commands;
    private DefaultListModel<CmdConfig> listModel;

    @Override
    public @Nls String getDisplayName() {
        return "Run Claude Code Cmd Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        commands = new ArrayList<>(CmdSettingsState.getInstance().getCommands());

        mainPanel = new JPanel(new BorderLayout());

        // 命令列表
        listModel = new DefaultListModel<>();
        for (CmdConfig config : commands) {
            listModel.addElement(config);
        }
        commandsList = new JBList<>(listModel);
        commandsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CmdConfig) {
                    setText(((CmdConfig) value).getDisplayName());
                }
                return this;
            }
        });

        // 编辑面板
        editPanel = new CmdConfigPanel();
        commandsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                CmdConfig selected = commandsList.getSelectedValue();
                if (selected != null) {
                    editPanel.setConfig(selected);
                }
            }
        });

        // 工具栏装饰器
        JPanel listPanel = ToolbarDecorator.createDecorator(commandsList)
            .setAddAction(button -> {
                CmdConfig newConfig = new CmdConfig("New Command", "");
                listModel.addElement(newConfig);
                commandsList.setSelectedIndex(listModel.size() - 1);
                editPanel.setConfig(newConfig);
            })
            .setRemoveAction(button -> {
                int index = commandsList.getSelectedIndex();
                if (index >= 0) {
                    listModel.remove(index);
                    if (listModel.size() > 0) {
                        commandsList.setSelectedIndex(Math.min(index, listModel.size() - 1));
                    }
                }
            })
            .createPanel();

        mainPanel.add(listPanel, BorderLayout.WEST);
        mainPanel.add(editPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        // 保存当前编辑到列表
        saveCurrentEdit();

        // 检查列表是否修改
        List<CmdConfig> current = CmdSettingsState.getInstance().getCommands();
        if (commands.size() != current.size()) return true;
        for (int i = 0; i < commands.size(); i++) {
            if (!commands.get(i).equals(current.get(i))) return true;
        }
        return false;
    }

    private void saveCurrentEdit() {
        CmdConfig selected = commandsList.getSelectedValue();
        if (selected != null) {
            CmdConfig updated = editPanel.getConfig();
            selected.setDisplayName(updated.getDisplayName());
            selected.setCommand(updated.getCommand());
            selected.setShellPath(updated.getShellPath());
            selected.setExecuteMode(updated.getExecuteMode());
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        // 保存当前编辑
        saveCurrentEdit();

        // 从 listModel 同步到 commands 列表
        commands.clear();
        for (int i = 0; i < listModel.size(); i++) {
            commands.add(listModel.get(i));
        }

        // 验证
        for (CmdConfig config : commands) {
            if (config.getDisplayName() == null || config.getDisplayName().isEmpty()) {
                throw new ConfigurationException("Display Name 不能为空");
            }
            if (config.getCommand() == null || config.getCommand().isEmpty()) {
                throw new ConfigurationException("Command 不能为空");
            }
        }

        CmdSettingsState.getInstance().setCommands(new ArrayList<>(commands));
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public void reset() {
        commands = new ArrayList<>(CmdSettingsState.getInstance().getCommands());
        listModel.clear();
        for (CmdConfig config : commands) {
            listModel.addElement(config);
        }
        if (!commands.isEmpty()) {
            commandsList.setSelectedIndex(0);
        }
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        commandsList = null;
        editPanel = null;
        commands = null;
        listModel = null;
    }
}