![b](https://s2.ax1x.com/2019/12/25/lFCd41.jpg "b")

# thief-book-idea

一款 IntelliJ IDEA 插件，让你在 IDE 里悄悄看小说——"摸鱼神器" Thief-Book 的 IDEA 版。灵感来自 [Thief-Book](https://github.com/cteams/Thief-Book)（PC 端 / VS Code 版）。

## 功能特性

- **IDE 内阅读**：底部工具窗口展示小说内容，无需切出 IDE
- **自定义阅读体验**：字体、字号、每页行数、行间距均可配置
- **热键翻页**：默认 `Ctrl+1` 上一页 / `Ctrl+2` 下一页，可在设置中修改
- **老板键**：默认 `Ctrl+3`，一键隐藏阅读窗口，随时摸鱼
- **进度记忆**：阅读进度实时保存，下次打开自动续读
- **跳页**：在进度栏输入行号回车即可跳转
- **精简模式**：隐藏翻页按钮，只留阅读区
- **编码自动识别**：支持 UTF-8（含 BOM）/ GBK / ANSI，切书自动重新检测
- **后台读取**：翻页、跳页、刷新均不阻塞 IDE

## 安装

### 方式一：JetBrains 插件市场（推荐）

`File | Settings | Plugins | Marketplace`，搜索 **thief-book** 安装，重启 IDE。

市场地址：[thief-book-idea](https://plugins.jetbrains.com/plugin/15442-thief-book-idea)

### 方式二：本地安装

1. 从 [GitHub Releases](https://github.com/yisier/thief-book-idea/releases) 下载 `thief-book-idea-<version>.zip`
2. `File | Settings | Plugins`，点击齿轮按钮 → `Install Plugin from Disk...`，选择下载的 zip
3. 重启 IDE

## 使用方式

1. 打开设置：`File | Settings | Other Settings | Thief-Book Config`
2. 在设置页选择一本 **txt** 小说文件，按需调整字体、字号、每页行数、行间距与热键
3. 点击 `OK` 保存设置
4. 点击 IDE 底部工具窗口 `thief-book` 标签打开阅读窗口
5. 点击窗口左上角的 **刷新按钮** 加载书本，开始阅读

> 切换书本或修改设置后，只需再点一次刷新按钮即可生效，**无需重启 IDE**。

### 阅读窗口操作

| 操作 | 方式 |
| --- | --- |
| 上一页 / 下一页 | 点击 `PREV` / `NEXT` 按钮，或按 `Ctrl+1` / `Ctrl+2`（可自定义） |
| 跳页 | 在进度栏输入行号后回车 |
| 老板键 | 按 `Ctrl+3`（可自定义）隐藏窗口，再按一次恢复 |
| 精简模式 | 在设置页勾选，隐藏上下翻页按钮 |
| 恢复窗口 | 误关窗口后，`Window` 菜单 → `Show Thief` |

### 设置项说明

| 设置项 | 说明 | 默认值 |
| --- | --- | --- |
| 书本路径 | 选择要阅读的 txt 文件 | 无 |
| 字体 | 阅读区字体，可选"系统默认"跟随 IDE | 系统默认 |
| 字号 | 阅读区字体大小 | 14 |
| 每页行数 | 每页显示的行数 | 1 |
| 行间距 | 行与行之间的间隔 | 0 |
| 上一页热键 | 上一页快捷键 | Ctrl+1 |
| 下一页热键 | 下一页快捷键 | Ctrl+2 |
| 老板键 | 隐藏/显示阅读窗口 | Ctrl+3 |
| 精简模式 | 勾选后隐藏翻页按钮 | 关 |

## 常见问题

**1. 小说出现乱码？**
插件会自动识别 UTF-8（含 BOM）/ GBK / ANSI 编码；若仍乱码，可新建一个 UTF-8 编码的空白 txt 文件，把书本内容复制进去再重新选择。

**2. 字体显示异常？**
阅读区字体请选择系统中真实存在的字体，Windows 推荐微软雅黑。

**3. 热键可以自定义吗？**
可以。进入 `Thief-Book Config` 设置页，点击热键输入框后直接按下想要的组合键即可（按 Backspace 清除）。

## 从源码构建

要求 JDK 17（路径见 `gradle.properties`）：

```
./gradlew buildPlugin   # 打包插件，产物在 build/distributions/thief-book-idea-<version>.zip
./gradlew runIde        # 启动本地沙箱 IDEA 调试插件
```

最低兼容 IntelliJ Platform 2023.3（build 233）。

## 更新记录

### V0.1.2（2026-08-10）

- 翻页/跳页/刷新的文件读取改为后台线程执行，不再阻塞 UI
- 替换已废弃 API（ServiceManager、ContentFactory.SERVICE）
- 缓存系统字体列表，避免每次打开设置页重复枚举
- 刷新时仅在切书或未统计时全量扫描行数，避免大文件重复扫描
- 编码自动检测：支持 BOM / UTF-8 / GBK / ANSI，切书自动重新检测
- 改用 Gradle 构建（org.jetbrains.intellij），最低兼容 IntelliJ Platform 2023.3

## License

[MIT](LICENSE)
