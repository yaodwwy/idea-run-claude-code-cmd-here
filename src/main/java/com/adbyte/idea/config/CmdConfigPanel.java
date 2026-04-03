package com.adbyte.idea.config;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CmdConfigPanel extends JPanel {

    private final JBTextField displayNameField = new JBTextField();
    private final JBTextField commandField = new JBTextField();
    private final JComboBox<String> shellPathBox;
    private final JButton browseButton = new JButton("...");
    private final JComboBox<ExecuteMode> executeModeBox = new JComboBox<>(ExecuteMode.values());

    private CmdConfig config;

    public CmdConfigPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // 设置文本框首选宽度
        Dimension textFieldSize = new Dimension(300, displayNameField.getPreferredSize().height);
        displayNameField.setPreferredSize(textFieldSize);
        commandField.setPreferredSize(textFieldSize);

        // 初始化 shell 下拉框（可编辑）
        Set<String> shellOptions = detectSystemShells();
        shellPathBox = new JComboBox<>(shellOptions.toArray(new String[0]));
        shellPathBox.setEditable(true);
        shellPathBox.setPreferredSize(new Dimension(350, shellPathBox.getPreferredSize().height));

        // 浏览按钮
        browseButton.addActionListener(e -> browseShellPath());

        // Shell路径面板（下拉框 + 浏览按钮）
        JPanel shellPanel = new JPanel(new BorderLayout(2, 0));
        shellPanel.add(shellPathBox, BorderLayout.CENTER);
        shellPanel.add(browseButton, BorderLayout.EAST);

        // Display Name
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Display Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(displayNameField, gbc);

        // Command
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Command:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(commandField, gbc);

        // Shell Path
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Shell Path:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(shellPanel, gbc);

        // Execute Mode
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Execute Mode:"), gbc);
        gbc.gridx = 1;
        add(executeModeBox, gbc);
    }

    /**
     * 检测系统中可用的 shell
     */
    private Set<String> detectSystemShells() {
        Set<String> shells = new LinkedHashSet<>();

        // 第一项：默认终端（空表示自动检测）
        shells.add("<Default Terminal>");

        if (SystemInfo.isWindows) {
            // CMD
            addShellIfExists(shells, System.getenv("COMSPEC"));

            // PowerShell
            addShellIfExists(shells, getPowerShellPath());

            // PowerShell Core (pwsh)
            addShellIfExists(shells, getPwshPath());

            // Git Bash
            addShellIfExists(shells, getGitBashPath());

            // WSL
            addShellIfExists(shells, getWslPath());

            // Cygwin
            addShellIfExists(shells, getCygwinBashPath());
        } else if (SystemInfo.isMac) {
            // macOS 默认 shell
            addShellIfExists(shells, "/bin/zsh");
            addShellIfExists(shells, "/bin/bash");
            addShellIfExists(shells, System.getenv("SHELL"));

            // iTerm2 / 其他终端
            addShellIfExists(shells, "/usr/local/bin/fish");
        } else {
            // Linux
            addShellIfExists(shells, "/bin/bash");
            addShellIfExists(shells, "/bin/zsh");
            addShellIfExists(shells, "/usr/bin/fish");
            addShellIfExists(shells, System.getenv("SHELL"));
        }

        return shells;
    }

    private void addShellIfExists(Set<String> shells, String path) {
        if (path != null && !path.isEmpty()) {
            File file = new File(path.split(" ")[0]); // 处理 "wsl bash" 这类情况
            if (file.exists() || path.startsWith("wsl")) {
                shells.add(path);
            }
        }
    }

    private String getPowerShellPath() {
        String windir = System.getenv("WINDIR");
        if (windir != null) {
            File ps = new File(windir, "System32\\WindowsPowerShell\\v1.0\\powershell.exe");
            if (ps.exists()) return ps.getAbsolutePath();
        }
        return null;
    }

    private String getPwshPath() {
        // PowerShell Core
        String[] paths = {
            "C:\\Program Files\\PowerShell\\7\\pwsh.exe",
            "C:\\Program Files\\PowerShell\\6\\pwsh.exe"
        };
        for (String path : paths) {
            if (new File(path).exists()) return path;
        }
        return null;
    }

    private String getGitBashPath() {
        // Git 安装路径
        String gitHome = System.getenv("GIT_HOME");
        if (gitHome != null) {
            File bash = new File(gitHome, "bin\\bash.exe");
            if (bash.exists()) return bash.getAbsolutePath();
        }

        // Program Files 中的 Git
        String[] programFiles = {
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            "C:\\Program Files",
            "C:\\Program Files (x86)"
        };
        for (String pf : programFiles) {
            if (pf != null) {
                File gitBash = new File(pf, "Git\\bin\\bash.exe");
                if (gitBash.exists()) return gitBash.getAbsolutePath();
                // Git for Windows 新版本路径
                gitBash = new File(pf, "Git\\usr\\bin\\bash.exe");
                if (gitBash.exists()) return gitBash.getAbsolutePath();
            }
        }
        return null;
    }

    private String getWslPath() {
        File wsl = new File("C:\\Windows\\System32\\wsl.exe");
        if (wsl.exists()) return "wsl";
        return null;
    }

    private String getCygwinBashPath() {
        String cygwinHome = System.getenv("CYGWIN_HOME");
        if (cygwinHome != null) {
            File bash = new File(cygwinHome, "bin\\bash.exe");
            if (bash.exists()) return bash.getAbsolutePath();
        }
        // 默认 Cygwin 安装路径
        String[] paths = {"C:\\cygwin64\\bin\\bash.exe", "C:\\cygwin\\bin\\bash.exe"};
        for (String path : paths) {
            if (new File(path).exists()) return path;
        }
        return null;
    }

    private void browseShellPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Shell Path");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Executable Files", "exe", "sh", "bat"));

        // 设置初始目录
        String currentPath = (String) shellPathBox.getSelectedItem();
        if (currentPath != null && !currentPath.isEmpty() && !currentPath.equals("<Default Terminal>")) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null) {
                chooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            shellPathBox.setSelectedItem(selected.getAbsolutePath());
        }
    }

    public void setConfig(CmdConfig config) {
        this.config = config;
        displayNameField.setText(config.getDisplayName());
        commandField.setText(config.getCommand());

        String shellPath = config.getShellPath();
        if (shellPath == null || shellPath.isEmpty()) {
            shellPathBox.setSelectedItem("<Default Terminal>");
        } else {
            // 检查是否在列表中，不在则直接设置文本
            boolean found = false;
            for (int i = 0; i < shellPathBox.getItemCount(); i++) {
                if (shellPathBox.getItemAt(i).equals(shellPath)) {
                    shellPathBox.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                shellPathBox.setSelectedItem(shellPath);
            }
        }

        executeModeBox.setSelectedItem(config.getExecuteMode());
    }

    public CmdConfig getConfig() {
        if (config == null) {
            config = new CmdConfig();
        }
        config.setDisplayName(displayNameField.getText());
        config.setCommand(commandField.getText());

        String shellPath = (String) shellPathBox.getSelectedItem();
        if (shellPath == null || shellPath.equals("<Default Terminal>")) {
            config.setShellPath(""); // 空表示默认终端
        } else {
            config.setShellPath(shellPath);
        }

        config.setExecuteMode((ExecuteMode) executeModeBox.getSelectedItem());
        return config;
    }

    public boolean isModified() {
        if (config == null) return true;

        String currentShellPath = (String) shellPathBox.getSelectedItem();
        String configShellPath = config.getShellPath();
        if (configShellPath == null) configShellPath = "";
        if ("<Default Terminal>".equals(currentShellPath)) currentShellPath = "";

        return !java.util.Objects.equals(displayNameField.getText(), config.getDisplayName())
            || !java.util.Objects.equals(commandField.getText(), config.getCommand())
            || !java.util.Objects.equals(currentShellPath, configShellPath)
            || !java.util.Objects.equals(executeModeBox.getSelectedItem(), config.getExecuteMode());
    }
}