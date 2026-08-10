# AGENTS.md

IntelliJ IDEA 插件项目（thief-book-idea，IDE 内"摸鱼"小说阅读器）。Java 编写，面向 IntelliJ Platform。

## 构建方式（Gradle + IntelliJ Platform Gradle Plugin）
- 仓库使用 Gradle（`org.jetbrains.intellij` 插件）构建，不再使用 DevKit 模块方式。
- 构建入口：`./gradlew buildPlugin`，产物在 `build/distributions/thief-book-idea-<version>.zip`；单独打 jar 用 `./gradlew jar`。
- 本地起沙箱调试：`./gradlew runIde`（会自动下载 IntelliJ Platform 到 `~/.gradle` 缓存）。
- 修改后请运行 `./gradlew build` 验证编译与打包。

## 环境要求
- **JDK 17**：Gradle 守护进程使用 JDK 17（在 `gradle.properties` 的 `org.gradle.java.home` 中指定本机路径，路径不同请修改）。用 JDK 8/11 会因平台类字节码版本报错。
- Gradle Wrapper 版本 8.5（`gradle/wrapper/gradle-wrapper.properties`）。
- 构建用 IntelliJ Platform 版本在 `gradle.properties` 的 `intellijVersion`（默认 2023.3，社区版），最低兼容 IntelliJ Platform 2023.3（build 233）。
- **兼容版本由 `build.gradle` 的 `patchPluginXml` 强制指定**（`sinceBuild = '233.0'`、`untilBuild = ''`），只改 `plugin.xml` 的 `idea-version` 无效，需两处同步。注意 `gradle.properties` 顶部注释里的 `since-build="203.0"` 是过时残留，勿信。

## 源码布局（Gradle 标准目录）
- `src/main/java/` —— Java 源码根，包 `com.thief.idea`。
- `src/main/resources/` —— 资源根，含 `META-INF/plugin.xml` 与 `icons/`。

## 插件入口（真正的装配在 `src/main/resources/META-INF/plugin.xml`）
- `com.thief.idea.MainUi` —— `ToolWindowFactory`，"thief-book" 工具窗口（底部）。
- `com.thief.idea.Setting` —— `SearchableConfigurable`，`Settings → Other Settings → Thief-Book Config`。
- `com.thief.idea.PersistentState` —— `applicationService` + `PersistentStateComponent<Element>`，持久化到 `thief-book.xml`。`getInstance()` 通过 `ApplicationManager.getApplication().getService(...)` 获取。
- `com.thief.idea.ShowThiefBook` —— 注册在 `WindowMenu` 的 action，用于重新打开被关闭的工具窗口。

## GUI Designer（不要手改生成代码）
- `src/main/java/com/thief/idea/ui/SettingUi.java` 中的 `$$$setupUI$$$()` 方法和实例初始化块 `{}` 由 **IntelliJ GUI Designer** 依据同目录 `SettingUi.form` 生成，文件内明确标注 `DO NOT EDIT`。改 UI 必须用 IDEA 的 GUI Designer 编辑 `.form`，不要直接改生成代码。
- 第三方依赖 `org.apache.commons.lang.StringUtils`（commons-lang，非 lang3）与 `com.jgoodies.forms` 均随 IntelliJ Platform 提供（平台发行版自带的 jar），无需在 build.gradle 中额外添加依赖。

## 文件读取约束（MainUi.java）
- **编码自动检测**：`ensureCharset()`/`detectCharset()` 按"BOM → UTF-8 严格解码样本（尾部最多回退 4 字节重试）→ 回退 GB18030"检测编码并缓存于 `fileCharset` 字段；切书（refresh 的 `bookChanged` 分支）会重置缓存重新检测。GBK/ANSI/UTF-8（含 BOM，首行 `\uFEFF` 会被剥离）均支持；UTF-16 会报"暂不支持"。改动读取逻辑时不要绕过该检测，也不要写死 UTF-8。
- 翻页靠 `seekDictionary`（每 `cacheInterval=200` 行缓存一个文件指针）加速跳页；改动分页/跳页逻辑时注意维护该缓存。
- **读取走批量字节块**：`readLines()`/`appendLine()` 按 8KB 块读并切行（处理跨块残行、`\r\n`、末尾无换行行），`countLine()` 同样按字节块扫描换行符计数。新代码不要用 `RandomAccessFile.readLine()` 逐行读（慢且带 ISO-8859-1 往返）。

## 其他约定
- `TestUi.java` 的 `isApplicable()` 恒返回 `false`，是禁用/实验代码，不要当作活跃入口。
- 代码注释与 UI 文案为中文，新增内容请保持一致。
- 无测试、无 lint / typecheck / formatter 配置；提交信息简短、中英文混用，无强制规范。
