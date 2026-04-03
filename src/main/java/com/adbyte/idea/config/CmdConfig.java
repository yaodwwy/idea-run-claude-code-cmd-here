package com.adbyte.idea.config;

public class CmdConfig {
    private String displayName;
    private String command;
    private String shellPath; // Shell路径，如 cmd.exe, powershell.exe, bash 等
    private ExecuteMode executeMode;

    public CmdConfig() {
        this.shellPath = ""; // 空表示自动检测
        this.executeMode = ExecuteMode.DIALOG;
    }

    public CmdConfig(String displayName, String command) {
        this.displayName = displayName;
        this.command = command;
        this.shellPath = ""; // 空表示自动检测
        this.executeMode = ExecuteMode.DIALOG;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getShellPath() { return shellPath; }
    public void setShellPath(String shellPath) { this.shellPath = shellPath; }

    public ExecuteMode getExecuteMode() { return executeMode; }
    public void setExecuteMode(ExecuteMode executeMode) { this.executeMode = executeMode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CmdConfig that = (CmdConfig) o;
        return java.util.Objects.equals(displayName, that.displayName) &&
               java.util.Objects.equals(command, that.command) &&
               java.util.Objects.equals(shellPath, that.shellPath) &&
               executeMode == that.executeMode;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(displayName, command, shellPath, executeMode);
    }

    @Override
    public String toString() {
        return "CmdConfig{displayName='" + displayName + "', command='" + command + "'}";
    }
}
