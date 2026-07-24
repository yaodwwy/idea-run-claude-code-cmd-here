package com.hmydk.aigit;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 首次安装时显示一次欢迎通知，引导用户前往设置页配置。
 *
 * <p>仅首次安装触发（用 {@link PropertiesComponent} 布尔标记），不查询插件版本，
 * 从而避免使用 IntelliJ 内部 API（PluginManagerCore）。</p>
 */
public class WelcomeNotification implements ProjectActivity {

    private static final String NOTIFICATION_GROUP_ID = "AI Git Commit Notifications";
    private static final String PLUGIN_NAME = "Run Claude Code Cmd";
    private static final String WELCOME_TITLE = "Welcome to AI Commit Message!";
    private static final String WELCOME_CONTENT = "Thank you for installing AI Commit Message. " +
            "To get started, please configure the plugin in the settings.";
    private static final String WELCOMED_FLAG = "com.hmydk.aigit.welcomed";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        PropertiesComponent props = PropertiesComponent.getInstance();
        if (!props.getBoolean(WELCOMED_FLAG, false)) {
            showWelcomeNotification(project);
            props.setValue(WELCOMED_FLAG, true);
        }
        return Unit.INSTANCE;
    }

    private void showWelcomeNotification(@NotNull Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(WELCOME_TITLE, WELCOME_CONTENT, NotificationType.INFORMATION)
                .setIcon(null) // 可按需设置自定义图标
                .addAction(new ConfigureAction())
                .notify(project);
    }

    /**
     * 通知中的"配置"操作：点击后打开设置对话框并定位到本插件配置页。
     */
    private static class ConfigureAction extends com.intellij.openapi.actionSystem.AnAction {
        ConfigureAction() {
            super("Configure");
        }

        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), PLUGIN_NAME);
        }
    }
}
