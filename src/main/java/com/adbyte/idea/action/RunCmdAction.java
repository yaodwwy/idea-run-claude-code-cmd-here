package com.adbyte.idea.action;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.executor.CmdExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class RunCmdAction extends AnAction {

    private static final Icon ICON = IconLoader.getIcon("/META-INF/pluginIcon.svg", RunCmdAction.class);
    private final CmdConfig config;

    public RunCmdAction(CmdConfig config) {
        super(Objects.requireNonNull(config, "CmdConfig cannot be null").getDisplayName(), null, ICON);
        this.config = config;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        String workingDirectory = getWorkingDirectory(file, project);
        CmdExecutor.execute(project, config, workingDirectory);
    }

    @Nullable
    private String getWorkingDirectory(VirtualFile file, Project project) {
        if (file == null) {
            return project != null ? project.getBasePath() : null;
        }

        if (file.isDirectory()) {
            return file.getPath();
        }

        VirtualFile parent = file.getParent();
        return parent != null ? parent.getPath() : project.getBasePath();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}