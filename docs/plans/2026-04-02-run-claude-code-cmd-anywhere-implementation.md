# Run Claude Code Cmd Any Where 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 创建一个 IntelliJ IDEA 插件，允许用户在任意目录右键运行预设的 CLI 命令（默认运行 Claude Code）。

**Architecture:** 采用标准 IntelliJ Platform 插件架构，使用 AnAction 实现右键菜单，Configurable 实现配置界面，PersistentStateComponent 实现配置持久化，ProcessBuilder 实现独立进程执行。

**Tech Stack:** Java 17, Gradle, IntelliJ Platform Plugin SDK, IntelliJ IDEA 2023.2+

---

## Task 1: 初始化项目结构和 Gradle 配置

**Files:**
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `settings.gradle`
- Create: `src/main/resources/META-INF/plugin.xml`
- Create: `src/main/java/com/adbyte/idea/.gitkeep`

**Step 1: 创建 Gradle 配置文件**

创建 `settings.gradle`:
```groovy
rootProject.name = 'run-claude-code-cmd-anywhere'
```

创建 `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2g
org.gradle.parallel=true
```

创建 `build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.jetbrains.intellij' version '1.16.0'
}

group 'com.adbyte'
version '1.0.0'

repositories {
    mavenCentral()
}

intellij {
    version = '2023.2'
    type = 'IC'
    plugins = []
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
```

**Step 2: 创建 plugin.xml**

创建 `src/main/resources/META-INF/plugin.xml`:
```xml
<idea-plugin>
    <id>run-claude-code-cmd-anywhere</id>
    <name>Run Claude Code Cmd Any Where</name>
    <vendor email="support@adbyte.com" url="https://adbyte.com">Adbyte</vendor>

    <description><![CDATA[
    <p>在任意目录快速运行 Claude Code CLI 命令，支持自定义命令预设与执行方式配置</p>
    <p>Quickly run Claude Code CLI commands in any directory, with customizable command presets and execution mode settings</p>
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <applicationConfigurable
            parentId="tools"
            instance="com.adbyte.idea.config.CmdSettingsConfigurable"
            id="runClaudeCodeCmdSettings"
            displayName="Run Claude Code Cmd Settings"/>
    </extensions>

    <actions>
        <group id="RunCmdHere.Menu" text="Run Claude Code" popup="true">
            <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
        </group>
    </actions>
</idea-plugin>
```

**Step 3: 创建 Java 包目录**

```bash
mkdir -p src/main/java/com/adbyte/idea/config
mkdir -p src/main/java/com/adbyte/idea/action
mkdir -p src/main/java/com/adbyte/idea/executor
mkdir -p src/main/resources/icons
```

**Step 4: 验证项目结构**

```bash
ls -la src/main/
```

Expected: 目录结构正确显示

**Step 5: Commit**

```bash
git add .
git commit -m "feat: 初始化项目结构和 Gradle 配置"
```

---

## Task 2: 实现配置数据类和持久化

**Files:**
- Create: `src/main/java/com/adbyte/idea/config/CmdConfig.java`
- Create: `src/main/java/com/adbyte/idea/config/ShellType.java`
- Create: `src/main/java/com/adbyte/idea/config/ExecuteMode.java`
- Create: `src/main/java/com/adbyte/idea/config/CmdSettingsState.java`

**Step 1: 创建 ShellType 枚举**

创建 `src/main/java/com/adbyte/idea/config/ShellType.java`:
```java
package com.adbyte.idea.config;

public enum ShellType {
    AUTO,
    BASH,
    CMD,
    POWERSHELL
}
```

**Step 2: 创建 ExecuteMode 枚举**

创建 `src/main/java/com/adbyte/idea/config/ExecuteMode.java`:
```java
package com.adbyte.idea.config;

public enum ExecuteMode {
    TERMINAL,
    DIALOG
}
```

**Step 3: 创建 CmdConfig 数据类**

创建 `src/main/java/com/adbyte/idea/config/CmdConfig.java`:
```java
package com.adbyte.idea.config;

public class CmdConfig {
    private String displayName;
    private String command;
    private ShellType shellType;
    private ExecuteMode executeMode;

    public CmdConfig() {
        this.shellType = ShellType.AUTO;
        this.executeMode = ExecuteMode.DIALOG;
    }

    public CmdConfig(String displayName, String command) {
        this.displayName = displayName;
        this.command = command;
        this.shellType = ShellType.AUTO;
        this.executeMode = ExecuteMode.DIALOG;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public ShellType getShellType() { return shellType; }
    public void setShellType(ShellType shellType) { this.shellType = shellType; }

    public ExecuteMode getExecuteMode() { return executeMode; }
    public void setExecuteMode(ExecuteMode executeMode) { this.executeMode = executeMode; }
}
```

**Step 4: 创建 CmdSettingsState 持久化类**

创建 `src/main/java/com/adbyte/idea/config/CmdSettingsState.java`:
```java
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
        // 默认配置
        CmdConfig defaultConfig = new CmdConfig("Run Claude Here", "claude --permission-mode bypassPermissions");
        defaultConfig.setShellType(ShellType.AUTO);
        defaultConfig.setExecuteMode(ExecuteMode.DIALOG);
        commands.add(defaultConfig);
    }

    public List<CmdConfig> getCommands() { return commands; }
    public void setCommands(List<CmdConfig> commands) { this.commands = commands; }

    @Nullable
    @Override
    public CmdSettingsState getState() { return this; }

    @Override
    public void loadState(@NotNull CmdSettingsState state) {
        this.commands = state.commands;
    }
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/adbyte/idea/config/
git commit -m "feat: 实现配置数据类和持久化组件"
```

---

## Task 3: 实现命令执行器

**Files:**
- Create: `src/main/java/com/adbyte/idea/executor/CmdExecutor.java`

**Step 1: 创建 CmdExecutor 类**

创建 `src/main/java/com/adbyte/idea/executor/CmdExecutor.java`:
```java
package com.adbyte.idea.executor;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.config.ExecuteMode;
import com.adbyte.idea.config.ShellType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import java.io.File;
import java.io.IOException;

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
        }
    }

    private static void executeInExternalTerminal(CmdConfig config, String workingDirectory) throws IOException {
        ProcessBuilder pb = buildProcessBuilder(config, workingDirectory);
        pb.directory(new File(workingDirectory));
        pb.start();
    }

    private static ProcessBuilder buildProcessBuilder(CmdConfig config, String workingDirectory) {
        ShellType shellType = config.getShellType();
        String command = config.getCommand();

        if (shellType == ShellType.AUTO) {
            shellType = SystemInfo.isWindows ? ShellType.CMD : ShellType.BASH;
        }

        if (SystemInfo.isWindows) {
            switch (shellType) {
                case CMD:
                    return new ProcessBuilder("cmd.exe", "/k",
                        "cd /d \"" + workingDirectory + "\" && " + command);
                case POWERSHELL:
                    return new ProcessBuilder("powershell.exe", "-NoExit", "-Command",
                        "cd \"" + workingDirectory + "\"; " + command);
                case BASH:
                    return new ProcessBuilder("bash", "-c",
                        "cd \"" + workingDirectory + "\"; " + command + "; exec bash");
                default:
                    return new ProcessBuilder("cmd.exe", "/k",
                        "cd /d \"" + workingDirectory + "\" && " + command);
            }
        } else {
            // macOS / Linux
            switch (shellType) {
                case CMD:
                    throw new IllegalArgumentException("CMD shell 仅支持 Windows 系统");
                case POWERSHELL:
                    return new ProcessBuilder("pwsh", "-NoExit", "-Command",
                        "cd \"" + workingDirectory + "\"; " + command);
                default:
                    if (SystemInfo.isMac) {
                        String script = "cd \"" + workingDirectory + "\"; " + command + "; exec bash";
                        return new ProcessBuilder("open", "-a", "Terminal.app", script);
                    } else {
                        return new ProcessBuilder("xterm", "-e",
                            "cd \"" + workingDirectory + "\"; " + command + "; exec bash");
                    }
            }
        }
    }

    private static void executeInTerminal(Project project, CmdConfig config, String workingDirectory) {
        // Terminal 模式需要 Terminal 插件 API，此处为简化实现
        // 实际实现需要依赖 terminal plugin
        showError("Terminal 模式需要安装 Terminal 插件支持");
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
```

**Step 2: Commit**

```bash
git add src/main/java/com/adbyte/idea/executor/
git commit -m "feat: 实现命令执行器"
```

---

## Task 4: 实现右键菜单 Action

**Files:**
- Create: `src/main/java/com/adbyte/idea/action/RunCmdAction.java`
- Create: `src/main/java/com/adbyte/idea/action/RunCmdActionGroup.java`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: 创建 RunCmdAction 类**

创建 `src/main/java/com/adbyte/idea/action/RunCmdAction.java`:
```java
package com.adbyte.idea.action;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.executor.CmdExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class RunCmdAction extends AnAction {

    private final CmdConfig config;

    public RunCmdAction(CmdConfig config) {
        super(config.getDisplayName());
        this.config = config;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        String workingDirectory = getWorkingDirectory(file, project);
        CmdExecutor.execute(project, config, workingDirectory);
    }

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
```

**Step 2: 创建 RunCmdActionGroup 类**

创建 `src/main/java/com/adbyte/idea/action/RunCmdActionGroup.java`:
```java
package com.adbyte.idea.action;

import com.adbyte.idea.config.CmdConfig;
import com.adbyte.idea.config.CmdSettingsState;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RunCmdActionGroup extends ActionGroup {

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<CmdConfig> commands = CmdSettingsState.getInstance().getCommands();
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
```

**Step 3: 更新 plugin.xml**

修改 `src/main/resources/META-INF/plugin.xml`，更新 actions 部分：
```xml
    <actions>
        <group id="RunCmdHere.Menu"
               text="Run Claude Code"
               popup="true"
               class="com.adbyte.idea.action.RunCmdActionGroup">
            <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
        </group>
    </actions>
```

同时添加 notification group 配置：
```xml
    <extensions defaultExtensionNs="com.intellij">
        <applicationConfigurable
            parentId="tools"
            instance="com.adbyte.idea.config.CmdSettingsConfigurable"
            id="runClaudeCodeCmdSettings"
            displayName="Run Claude Code Cmd Settings"/>

        <notificationGroup id="RunCmdHere.NotificationGroup" displayType="BALLOON"/>
    </extensions>
```

**Step 4: Commit**

```bash
git add src/main/java/com/adbyte/idea/action/
git add src/main/resources/META-INF/plugin.xml
git commit -m "feat: 实现右键菜单 Action"
```

---

## Task 5: 实现 Settings 配置界面

**Files:**
- Create: `src/main/java/com/adbyte/idea/config/CmdSettingsConfigurable.java`
- Create: `src/main/java/com/adbyte/idea/config/CmdConfigPanel.java`

**Step 1: 创建 CmdConfigPanel 类**

创建 `src/main/java/com/adbyte/idea/config/CmdConfigPanel.java`:
```java
package com.adbyte.idea.config;

import com.intellij.ui.components.JBTextField;
import com.intellij.ui.enumCombobox.EnumComboboxBox;

import javax.swing.*;
import java.awt.*;

public class CmdConfigPanel extends JPanel {

    private final JBTextField displayNameField = new JBTextField();
    private final JBTextField commandField = new JBTextField();
    private final EnumComboboxBox<ShellType> shellTypeBox = new EnumComboboxBox<>(ShellType.class);
    private final EnumComboboxBox<ExecuteMode> executeModeBox = new EnumComboboxBox<>(ExecuteMode.class);

    private CmdConfig config;

    public CmdConfigPanel() {
        setLayout(new GridLayout(4, 2, 5, 5));

        add(new JLabel("Display Name:"));
        add(displayNameField);

        add(new JLabel("Command:"));
        add(commandField);

        add(new JLabel("Shell Type:"));
        add(shellTypeBox);

        add(new JLabel("Execute Mode:"));
        add(executeModeBox);
    }

    public void setConfig(CmdConfig config) {
        this.config = config;
        displayNameField.setText(config.getDisplayName());
        commandField.setText(config.getCommand());
        shellTypeBox.setSelectedItem(config.getShellType());
        executeModeBox.setSelectedItem(config.getExecuteMode());
    }

    public CmdConfig getConfig() {
        if (config == null) {
            config = new CmdConfig();
        }
        config.setDisplayName(displayNameField.getText());
        config.setCommand(commandField.getText());
        config.setShellType((ShellType) shellTypeBox.getSelectedItem());
        config.setExecuteMode((ExecuteMode) executeModeBox.getSelectedItem());
        return config;
    }

    public boolean isModified() {
        if (config == null) return true;
        return !displayNameField.getText().equals(config.getDisplayName())
            || !commandField.getText().equals(config.getCommand())
            || shellTypeBox.getSelectedItem() != config.getShellType()
            || executeModeBox.getSelectedItem() != config.getExecuteMode();
    }
}
```

**Step 2: 创建 CmdSettingsConfigurable 类**

创建 `src/main/java/com/adbyte/idea/config/CmdSettingsConfigurable.java`:
```java
package com.adbyte.idea.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
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
        DefaultListModel<CmdConfig> listModel = new DefaultListModel<>();
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
        // 检查列表是否修改
        List<CmdConfig> current = CmdSettingsState.getInstance().getCommands();
        if (commands.size() != current.size()) return true;
        for (int i = 0; i < commands.size(); i++) {
            if (!commands.get(i).equals(current.get(i))) return true;
        }
        // 检查编辑面板是否修改
        return editPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        // 验证
        for (CmdConfig config : commands) {
            if (config.getDisplayName() == null || config.getDisplayName().isEmpty()) {
                throw new ConfigurationException("Display Name 不能为空");
            }
            if (config.getCommand() == null || config.getCommand().isEmpty()) {
                throw new ConfigurationException("Command 不能为空");
            }
        }

        // 保存当前编辑
        CmdConfig selected = commandsList.getSelectedValue();
        if (selected != null) {
            CmdConfig updated = editPanel.getConfig();
            selected.setDisplayName(updated.getDisplayName());
            selected.setCommand(updated.getCommand());
            selected.setShellType(updated.getShellType());
            selected.setExecuteMode(updated.getExecuteMode());
        }

        CmdSettingsState.getInstance().setCommands(new ArrayList<>(commands));
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/adbyte/idea/config/
git commit -m "feat: 实现 Settings 配置界面"
```

---

## Task 6: 构建和测试插件

**Step 1: 构建插件**

```bash
./gradlew buildPlugin
```

Expected: 构建成功，生成插件 zip 文件

**Step 2: 运行测试 IDE**

```bash
./gradlew runIde
```

Expected: 启动带有插件的测试 IDEA 实例

**Step 3: 手动测试功能**

1. 在测试 IDEA 中打开任意项目
2. 右键点击目录 → 查看 "Run Claude Code" 菜单
3. 点击 "Run Claude Here" → 验证外部终端启动
4. 打开 Settings → Tools → Run Claude Code Cmd Settings
5. 修改或添加命令配置 → 验证保存成功

**Step 4: Final Commit**

```bash
git add .
git commit -m "feat: 完成插件实现"
```

---

## 后续优化任务（可选）

### Task 7: 添加 Terminal 模式支持（可选）

需要依赖 `org.jetbrains.plugins.terminal` 插件，使用 `TerminalView` API。

### Task 8: 添加图标和国际化（可选）

创建图标资源文件，支持中英文语言切换。

---

计划完成并保存到 `docs/plans/2026-04-02-run-claude-code-cmd-anywhere-implementation.md`。

**两种执行方式：**

**1. Subagent-Driven（当前会话）** - 我为每个任务派发新的子代理，在任务间进行代码审查，快速迭代

**2. Parallel Session（独立会话）** - 在新会话中打开工作树，使用 executing-plans skill 批量执行并设置检查点

**你选择哪种方式？**