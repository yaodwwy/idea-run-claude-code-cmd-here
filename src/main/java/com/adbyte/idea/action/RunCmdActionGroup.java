package com.adbyte.idea.action;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.config.CmdSettingsState;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RunCmdActionGroup extends DefaultActionGroup {

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<CmdConfig> commands = CmdSettingsState.getInstance().getCommands();

        if (commands.isEmpty()) {
            return new AnAction[] { new AnAction("No commands configured") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    // Placeholder action - opens settings dialog
                }
            }};
        }

        List<AnAction> actions = new ArrayList<>();
        for (CmdConfig config : commands) {
            actions.add(new RunCmdAction(config));
        }

        return actions.toArray(new AnAction[0]);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}