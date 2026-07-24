# Run Claude Code Cmd Any Where - 设计文档

## 概述

**插件名称：** Run Claude Code Cmd Any Where
**插件 ID：** `run-claude-code-cmd-anywhere`
**中文简介：** 在任意目录快速运行 Claude Code CLI 命令，支持自定义命令预设与执行方式配置
**英文简介：** Quickly run Claude Code CLI commands in any directory, with customizable command presets and execution mode settings

## 功能目标

用户安装插件后，可以在目录、编辑器、项目根节点上右键触发预设命令执行，实现在任意目录快速运行 Claude Code 或其他自定义 CLI 命令。

## 整体架构

```
idea-run-cmd-here/
├── src/main/
│   ├── java/com/adbyte/idea/
│   │   ├── RunCmdHerePlugin.java        # 插件入口
│   │   ├── action/
│   │   │   └── RunCmdAction.java        # 右键菜单动作
│   │   ├── config/
│   │   │   ├── CmdConfig.java           # 命令配置数据类
│   │   │   ├── CmdSettingsConfigurable.java  # Settings 配置界面
│   │   │   └── CmdSettingsState.java    # Settings 状态持久化
│   │   └── executor/
│   │   │   └── CmdExecutor.java         # 命令执行器
│   └── resources/
│   │   ├── META-INF/plugin.xml          # 插件配置文件
│   │   └── icons/                       # 图标资源
├── build.gradle                         # Gradle 构建配置
└── gradle.properties                    # Gradle 属性配置
```

**核心流程：**
1. 用户在目录/编辑器/项目根节点右键 → 显示预设命令列表
2. 用户点击某个命令 → 执行器根据配置运行命令
3. 结果显示在配置指定的位置（内置终端或独立进程窗口）

## 配置项结构

每个预设命令的配置项（单一配置项模式）：

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| displayName | String | 右键菜单显示的名称 | "Run Claude Here" |
| command | String | 要执行的命令行 | `claude --permission-mode bypassPermissions` |
| shellType | Enum | Shell 类型 | bash/cmd/powershell/auto |
| executeMode | Enum | 执行方式 | terminal/dialog |

**默认配置命令：**
- displayName: "Run Claude Here"
- command: `claude --permission-mode bypassPermissions`
- shellType: auto
- executeMode: dialog（独立 cmd 进程窗口）

**Settings 配置界面设计：**
- 使用 IntelliJ 的 `Configurable` API 创建标准设置面板
- 配置列表：显示所有已配置的命令
- 添加/编辑/删除按钮：管理命令配置
- 每个配置项的编辑表单包含上述字段

**配置持久化：**
- 使用 `PersistentStateComponent` 将配置保存到 IDEA 的 XML 配置文件
- 配置文件位置：`~/.IntelliJIDEA/config/options/runCmdHere.xml`

## 右键菜单触发机制

**触发位置与 Action 注册：**

| 触发位置 | Action Group | 注册方式 |
|---------|-------------|---------|
| 目录右键菜单 | `ProjectViewPopupMenu` | 通过 plugin.xml 的 `<group>` 注册 |
| 编辑器右键菜单 | `EditorPopupMenu` | 同上 |
| 项目根节点右键 | `ProjectViewPopupMenu` | 与目录右键共用，自动判断是否为根节点 |

**菜单显示逻辑：**
- 动态生成子菜单：根据 Settings 中配置的命令数量动态创建菜单项
- 每个配置项对应一个菜单项，显示 `displayName`

**plugin.xml 配置示例：**
```xml
<actions>
  <group id="RunCmdHere.Menu" text="Run Command Here" popup="true">
    <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>
  </group>
</actions>
```

## 命令执行机制

**执行流程：**
1. 用户点击菜单项 → 获取对应的 `CmdConfig` 配置
2. 根据触发位置确定工作目录：
   - 目录右键：选中的目录路径
   - 编辑器右键：当前打开文件所在目录
   - 项目根节点：项目根目录
3. 根据 `shellType` 选择执行环境
4. 根据 `executeMode` 选择输出方式

**Shell 类型处理：**

| shellType | Windows | macOS/Linux |
|-----------|---------|-------------|
| bash | Git Bash 或 WSL | /bin/bash |
| cmd | cmd.exe | 不支持（提示警告） |
| powershell | powershell.exe | pwsh（如已安装） |
| auto | cmd.exe | /bin/bash |

**执行模式：**

| executeMode | 实现 |
|-------------|------|
| terminal | 使用 IDEA 的 `TerminalView` API，在工作目录打开新终端并执行命令 |
| dialog | 使用 `ProcessBuilder` 启动独立系统终端窗口，执行命令后窗口保持打开，由用户手动关闭 |

**独立终端窗口启动方式：**
- Windows: `cmd.exe /k "cd /d <工作目录> && <命令>"` 或 `powershell.exe -NoExit -Command "cd <工作目录>; <命令>"`
- macOS: `open -a Terminal.app` 执行脚本
- Linux: `xterm -e "cd <工作目录>; <命令>; exec bash"` 保持窗口

## 错误处理与边界情况

**需要处理的错误场景：**

| 场景 | 处理方式 |
|------|---------|
| 无配置命令 | 使用默认命令：`claude --permission-mode bypassPermissions` |
| Shell 类型不兼容 | 显示错误提示对话框，告知当前系统不支持该 Shell |
| 工作目录不存在 | 显示错误提示对话框，告知目录路径无效 |
| 命令执行失败 | terminal 模式在终端显示错误；dialog 模式在独立窗口显示错误 |

**配置验证：**
- Settings 中保存配置时验证必填字段（displayName、command）
- 验证 shellType 和 executeMode 为有效枚举值

## 技术栈与依赖

**技术栈：**

| 项目 | 选择 |
|------|------|
| 语言 | Java 17 |
| 构建工具 | Gradle + IntelliJ Platform Gradle Plugin |
| 目标 IDE | IntelliJ IDEA 2023.2+ |
| 兼容性 | 支持 Community 和 Ultimate 版本 |

**主要依赖（build.gradle）：**
```groovy
intellij {
    version = '2023.2'
    type = 'IC'  // IntelliJ IDEA Community
    plugins = []
}
```

**核心 API 使用：**
- `AnAction` / `AnActionGroup` - 右键菜单注册
- `Configurable` - Settings 配置界面
- `PersistentStateComponent` - 配置持久化
- `TerminalView`（可选） - 内置终端集成
- `ProcessBuilder` - 独立进程启动
- `VirtualFile` / `Project` - 文件和项目路径获取

**测试策略：**
- 使用 IntelliJ Platform Test Framework 进行单元测试
- 测试重点：配置持久化、路径解析、命令执行逻辑

## 成功标准

1. 用户可以在目录、编辑器、项目根节点右键看到预设命令菜单
2. 用户可以在 Settings 中添加、编辑、删除命令配置
3. 命令可以在指定的工作目录正确执行
4. 支持两种执行模式：内置终端和独立进程窗口
5. 配置可以持久化保存，重启 IDEA 后仍然有效