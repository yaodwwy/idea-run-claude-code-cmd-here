package com.adbyte.idea.executor;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.config.ExecuteMode;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdExecutor {

    public static void execute(Project project, CmdConfig config, String workingDirectory) {
        if (workingDirectory == null || !new File(workingDirectory).exists()) {
            showError("工作目录不存在: " + workingDirectory);
            return;
        }

        try {
            if (config.getExecuteMode() == ExecuteMode.DIALOG) {
                executeInExternalTerminal(config, workingDirectory);
            } else {
                executeInTerminal(project, config, workingDirectory);
            }
        } catch (Exception e) {
            showError("命令执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeInExternalTerminal(CmdConfig config, String workingDirectory) throws IOException {
        String shellPath = config.getShellPath();
        String command = config.getCommand();

        // 如果 shellPath 为空，自动检测系统 shell
        if (shellPath == null || shellPath.isEmpty()) {
            shellPath = getDefaultShell();
        }

        if (SystemInfo.isWindows) {
            executeWindowsTerminal(shellPath, command, workingDirectory);
        } else if (SystemInfo.isMac) {
            executeMacTerminal(shellPath, command, workingDirectory);
        } else {
            executeLinuxTerminal(shellPath, command, workingDirectory);
        }
    }

    private static void executeWindowsTerminal(String shellPath, String command, String workingDirectory) throws IOException {
        File workDir = new File(workingDirectory);

        // PowerShell
        if (shellPath.contains("powershell") || shellPath.endsWith("pwsh.exe")) {
            List<String> cmdList = new ArrayList<>();
            cmdList.add(shellPath);
            cmdList.add("-NoExit");
            cmdList.add("-Command");
            cmdList.add("Set-Location -Path '" + workingDirectory + "'; " + command);
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.directory(workDir);
            pb.start();
        }
        // WSL
        else if (shellPath.equals("wsl") || shellPath.startsWith("wsl")) {
            List<String> cmdList = new ArrayList<>();
            cmdList.add("cmd.exe");
            cmdList.add("/c");
            cmdList.add("start");
            cmdList.add("wsl");
            cmdList.add("--cd");
            cmdList.add(workingDirectory);
            cmdList.add("-e");
            cmdList.add("bash");
            cmdList.add("-c");
            cmdList.add(command + "; exec bash");
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.directory(workDir);
            pb.start();
        }
        // Git Bash
        else if (shellPath.contains("bash.exe")) {
            List<String> cmdList = new ArrayList<>();
            cmdList.add("cmd.exe");
            cmdList.add("/c");
            cmdList.add("start");
            cmdList.add("\"Git Bash\"");
            cmdList.add(shellPath);
            cmdList.add("--cd=" + workingDirectory);
            cmdList.add("-c");
            cmdList.add(command + "; exec bash");
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.directory(workDir);
            pb.start();
        }
        // CMD (默认)
        else {
            // 使用 cmd /c start 打开新的 CMD 窗口
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "start",
                "cmd.exe", "/k",
                "cd /d \"" + workingDirectory + "\" && " + command
            );
            pb.directory(workDir);
            pb.start();
        }
    }

    private static void executeMacTerminal(String shellPath, String command, String workingDirectory) throws IOException {
        File workDir = new File(workingDirectory);
        String escapedWorkingDir = workingDirectory.replace("'", "'\"'\"'");
        String escapedCommand = command.replace("'", "'\"'\"'");

        // 使用 AppleScript 打开 Terminal.app
        String appleScript = String.format(
            "tell application \"Terminal\" to do script \"cd '%s' && %s\"",
            escapedWorkingDir, escapedCommand
        );
        ProcessBuilder pb = new ProcessBuilder("osascript", "-e", appleScript);
        pb.directory(workDir);
        pb.start();
    }

    private static void executeLinuxTerminal(String shellPath, String command, String workingDirectory) throws IOException {
        File workDir = new File(workingDirectory);

        // 尝试使用 gnome-terminal
        ProcessBuilder pb = new ProcessBuilder(
            "gnome-terminal", "--working-directory=" + workingDirectory,
            "-e", shellPath + " -c \"" + command + "; exec bash\""
        );
        pb.directory(workDir);
        try {
            pb.start();
        } catch (IOException e) {
            // 如果 gnome-terminal 不存在，尝试 xterm
            pb = new ProcessBuilder(
                "xterm", "-e",
                "cd \"" + workingDirectory + "\"; " + command + "; exec bash"
            );
            pb.directory(workDir);
            pb.start();
        }
    }

    private static String getDefaultShell() {
        if (SystemInfo.isWindows) {
            String comspec = System.getenv("COMSPEC");
            return comspec != null ? comspec : "cmd.exe";
        } else {
            String shell = System.getenv("SHELL");
            return shell != null ? shell : "/bin/bash";
        }
    }

    private static void executeInTerminal(Project project, CmdConfig config, String workingDirectory) {
        if (project == null) {
            showError("无法获取项目上下文");
            return;
        }

        try {
            TerminalView terminalView = TerminalView.getInstance(project);
            String command = config.getCommand();

            // 创建新的终端标签页并执行命令
            ShellTerminalWidget widget = terminalView.createLocalShellWidget(workingDirectory, "Run Command", true);
            widget.executeCommand(command);
        } catch (Exception e) {
            showError("无法打开终端: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showError(String message) {
        Notification notification = new Notification(
            "RunCmdHere.NotificationGroup",
            "Run Claude Code Cmd Error",
            message,
            NotificationType.ERROR
        );
        Notifications.Bus.notify(notification);
    }
}