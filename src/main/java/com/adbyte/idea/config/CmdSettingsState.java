package com.adbyte.idea.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "RunClaudeCodeCmdSettings",
    storages = @Storage("runClaudeCodeCmdSettings.xml")
)
public class CmdSettingsState implements PersistentStateComponent<CmdSettingsState> {

    private List<CmdConfig> commands = new ArrayList<>();

    public static CmdSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(CmdSettingsState.class);
    }

    public CmdSettingsState() {
        // Don't add defaults here - let getCommands() handle it
    }

    public List<CmdConfig> getCommands() {
        if (commands.isEmpty()) {
            CmdConfig defaultConfig = new CmdConfig("Run Claude Here", "claude --permission-mode bypassPermissions");
            defaultConfig.setShellPath(""); // 空表示自动检测
            defaultConfig.setExecuteMode(ExecuteMode.DIALOG);
            commands.add(defaultConfig);
        }
        return commands;
    }
    public void setCommands(List<CmdConfig> commands) { this.commands = commands; }

    @Nullable
    @Override
    public CmdSettingsState getState() { return this; }

    @Override
    public void loadState(@NotNull CmdSettingsState state) {
        this.commands = state.commands != null ? state.commands : new ArrayList<>();
    }
}
