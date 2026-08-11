package com.thief.idea;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.thief.idea.util.EpubUtil;
import com.thief.idea.util.HotkeyUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MainUi implements ToolWindowFactory, DumbAware {

    /**
     * 各项目工具窗口对应的 MainUi 实例，供全局老板键 action 查找并触发隐藏
     **/
    private static final Map<Project, MainUi> instances = new ConcurrentHashMap<>();

    /**
     * 根据项目获取 MainUi 实例，项目已销毁或窗口未创建时返回 null
     **/
    public static MainUi getInstance(Project project) {
        if (project == null) {
            return null;
        }
        MainUi mainUi = instances.get(project);
        if (mainUi != null && project.isDisposed()) {
            instances.remove(project);
            return null;
        }
        return mainUi;
    }

    private PersistentState persistentState = PersistentState.getInstance();

    /**
     * 缓存文件页数所对应的seek，避免搜索指针的时候每次从头读取文件
     **/
    private Map<Integer, Long> seekDictionary = new LinkedHashMap<>();

    /**
     * 缓存文件页数所对应seek的间隔
     * 该值越小，跳页时间越短，但对应的内存会增大
     **/
    private int cacheInterval = 200;

    /**
     * 读取文件路径
     **/
    private String bookFile = persistentState.getBookPathText();

    /**
     * 检测到的文件编码（UTF-8 或 GB18030），null 表示尚未检测
     **/
    private Charset fileCharset;

    /**
     * epub 书籍解包出的临时文本文件（UTF-8），非 epub 书籍恒为 null
     **/
    private File epubTextFile;

    /**
     * epub 源文件解包时的最后修改时间，源文件变化时自动重新解包
     **/
    private long epubLastModified;

    /**
     * 当前 epub 的目录（含正文行号），非 epub 书籍为 null
     **/
    private List<EpubUtil.TocEntry> epubToc;

    /**
     * epub 目录面板（左侧），仅在 epub 且有目录时显示
     **/
    private JPanel tocPanel;

    /**
     * 目录列表
     **/
    private JList<EpubUtil.TocEntry> tocList;

    /**
     * 读取字体设置
     **/
    private String type = persistentState.getFontType();

    /**
     * 读取字号设置
     **/
    private String size = persistentState.getFontSize();

    /**
     * 读取每页行数设置
     **/
    private Integer lineCount = parseIntSafe(persistentState.getLineCount(), 1);

    /**
     * 读取行距设置
     **/
    private Integer lineSpace = parseIntSafe(persistentState.getLineSpace(), 0);

    /**
     * 阅读区显示
     **/
    private JTextArea textArea;

    /**
     * 书本切换下拉框：设置页选择了多本书时显示，切换即换书并恢复该书进度
     **/
    private JComboBox<String> bookSelector;

    /**
     * 解析阅读区字体：未配置/选择"系统默认"/系统不存在的字体时，跟随 IDE 默认字体
     **/
    private Font resolveFont() {
        int s = parseIntSafe(size, 14);
        if (type != null && !type.isEmpty() && !PersistentState.DEFAULT_FONT.equals(type)) {
            for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if (type.equals(family)) {
                    return UIUtil.getFontWithFallback(new Font(type, Font.PLAIN, s));
                }
            }
        }
        return UIUtil.getFontWithFallback(UIUtil.getLabelFont()).deriveFont(Font.PLAIN, s);
    }

    /**
     * 当前阅读页&跳页输入框
     **/
    private JTextField current;

    /**
     * 上一页按钮
     **/
    private JButton upButton;

    /**
     * 下一页按钮
     **/
    private JButton downButton;

    /**
     * 上一页热键（设置页可修改）
     **/
    private KeyStroke prevKeyStroke;

    /**
     * 下一页热键（设置页可修改）
     **/
    private KeyStroke nextKeyStroke;

    /**
     * 老板键（设置页可修改），全局监听
     **/
    private KeyStroke bossKeyStroke;

    /**
     * 老板键按钮（5x5 隐形小按钮）
     **/
    private JButton bossButton;

    /**
     * 工具窗口引用，老板键隐藏时用于还原图标
     **/
    private ToolWindow toolWindow;

    /**
     * 工具窗口内容，老板键隐藏时用于修改 Tab 标题伪装
     **/
    private Content content;

    /**
     * 记录工具窗口原始图标，恢复老板键状态时还原
     **/
    private Icon originIcon;

    /**
     * 老板键伪装文案：模拟终端输出，避免被认出是小说阅读界面
     **/
    private static final String BOSS_FAKE_TEXT = "$ git status\n"
            + "On branch master\n"
            + "Your branch is up to date with 'origin/master'.\n"
            + "\n"
            + "nothing to commit, working tree clean\n"
            + "$ ";

    /**
     * 显示总页数
     **/
    private JLabel total = new JLabel();

    /**
     * 读取文件的指针
     **/
    private long seek = 0;

    /**
     * 当前文件总页数
     **/
    private int totalLine = 0;

    /**
     * 当前正在阅读页数
     **/
    private int currentPage = 0;

    /**
     * 缓存文字
     **/
    private String temp = "Thief-Book";

    /**
     * 是否隐藏界面
     **/
    private boolean hide = false;

    /**
     * 标记是否有后台 IO 任务正在进行，防止翻页连点导致状态错乱
     **/
    private final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * 刷新请求因 busy 被拦截时置位，当前 IO 任务完成后自动重试刷新，
     * 保证设置页点击 Apply 后新设置必然生效
     **/
    private final AtomicBoolean pendingRefresh = new AtomicBoolean(false);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            instances.put(project, this);
            // 老板键全局监听：任何窗口（包括设置页）获得焦点时都生效
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event.getID() != KeyEvent.KEY_PRESSED || HotkeyUtil.editingHotkey) {
                    return;
                }
                KeyEvent keyEvent = (KeyEvent) event;
                int mask = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
                        | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK;
                if (bossKeyStroke != null
                        && keyEvent.getKeyCode() == bossKeyStroke.getKeyCode()
                        && (keyEvent.getModifiersEx() & mask) == (bossKeyStroke.getModifiers() & mask)) {
                    toggleBoss();
                }
            }, AWTEvent.KEY_EVENT_MASK);
            JPanel panel = initPanel();
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(panel, "Thief-Book", false);
            toolWindow.getContentManager().addContent(content);
            this.toolWindow = toolWindow;
            this.content = content;
            // 打开工具窗口时自动加载书本并恢复上次阅读进度，无需手动点刷新
            refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化整体面板
     **/
    private JPanel initPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        textArea = initTextArea();
        panel.add(initBookBar(), BorderLayout.NORTH);
        panel.add(textArea, BorderLayout.CENTER);
        panel.add(initTocPanel(), BorderLayout.WEST);
        panel.add(initOperationPanel(), BorderLayout.EAST);
        return panel;
    }

    /**
     * 顶部书本切换栏：只有设置页配置了多本书时才可见
     **/
    private JPanel initBookBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bookSelector = initBookSelector();
        bar.add(bookSelector);
        return bar;
    }

    /**
     * 书本切换下拉框：显示文件名，悬停显示完整路径；
     * 切换后把该书设为活动书并刷新（进度按书独立保存，自动恢复）
     **/
    private JComboBox<String> initBookSelector() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setPreferredSize(new Dimension(240, 26));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String path = value.toString();
                    label.setText(new File(path).getName());
                    label.setToolTipText(path);
                }
                return label;
            }
        });
        combo.addActionListener(e -> {
            Object selected = combo.getSelectedItem();
            if (selected != null && !Objects.equals(selected, bookFile)) {
                persistentState.setBookPathText(selected.toString());
                refresh();
            }
        });
        return combo;
    }

    /**
     * 同步书本下拉框与设置页的书本列表，仅多本书时显示
     **/
    private void updateBookSelector() {
        if (bookSelector == null) {
            return;
        }
        String selected = bookFile;
        bookSelector.removeAllItems();
        for (String path : persistentState.getBookPathList()) {
            bookSelector.addItem(path);
        }
        bookSelector.setSelectedItem(selected);
        bookSelector.setVisible(persistentState.getBookPathList().size() > 1);
    }

    /**
     * 正文区域初始化
     **/
    private JTextArea initTextArea() {
        JTextArea textArea = new JTextArea();
        //初始化显示文字
        textArea.setText(temp);
        textArea.setOpaque(false);
        textArea.setTabSize(4);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(resolveFont());
        textArea.setBorder(JBUI.Borders.empty(10, 30));
        return textArea;
    }

    /**
     * 左侧目录面板：仅 epub 且有目录时可见，点击目录项跳转到对应章节
     **/
    private JPanel initTocPanel() {
        tocPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("目录");
        title.setBorder(JBUI.Borders.empty(4, 8));
        tocList = new JList<>();
        tocList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tocList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EpubUtil.TocEntry) {
                    EpubUtil.TocEntry entry = (EpubUtil.TocEntry) value;
                    label.setText("  ".repeat(entry.depth) + entry.title);
                    label.setToolTipText(entry.href);
                }
                return label;
            }
        });
        tocList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    jumpToToc(tocList.getSelectedIndex());
                }
            }
        });
        tocList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jumpToToc(tocList.getSelectedIndex());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tocList);
        tocPanel.add(title, BorderLayout.NORTH);
        tocPanel.add(scroll, BorderLayout.CENTER);
        tocPanel.setPreferredSize(new Dimension(220, 0));
        tocPanel.setVisible(false);
        return tocPanel;
    }

    /**
     * 按当前 epub 目录刷新左侧目录面板；非 epub 或无目录时隐藏
     **/
    private void updateTocPanel() {
        if (tocPanel == null || tocList == null) {
            return;
        }
        if (epubToc == null || epubToc.isEmpty()) {
            tocPanel.setVisible(false);
            return;
        }
        DefaultListModel<EpubUtil.TocEntry> model = new DefaultListModel<>();
        for (EpubUtil.TocEntry entry : epubToc) {
            model.addElement(entry);
        }
        tocList.setModel(model);
        tocPanel.setVisible(true);
    }

    /**
     * 点击目录项：跳转到对应章节（该章起始行作为本页第一行）
     **/
    private void jumpToToc(int index) {
        if (index < 0 || epubToc == null || index >= epubToc.size()) {
            return;
        }
        final int line = epubToc.get(index).line;
        runIoAsync(() -> {
            currentPage = line;
            countSeek();
            return readBook();
        }, content -> {
            textArea.setText(content);
            saveProgress();
            current.setText(" " + currentPage / lineCount);
            updatePageInfo();
            syncTocSelection();
        });
    }

    /**
     * 翻页/跳页/重载后，按当前页所在章节同步左侧目录高亮。
     * anchor 取当前页最后一行（currentPage - 1，clamp 到 0），保证落在当前显示页内，
     * 对最后一页不足 lineCount 行的情况同样正确。
     **/
    private void syncTocSelection() {
        if (tocList == null || epubToc == null || epubToc.isEmpty()) {
            return;
        }
        int anchor = Math.max(0, currentPage - 1);
        int best = -1;
        int bestLine = -1;
        for (int i = 0; i < epubToc.size(); i++) {
            int l = epubToc.get(i).line;
            if (l >= 0 && l <= anchor && l >= bestLine) {
                best = i;
                bestLine = l;
            }
        }
        if (best >= 0) {
            if (tocList.getSelectedIndex() != best) {
                tocList.setSelectedIndex(best);
            }
            tocList.ensureIndexIsVisible(best);
        }
    }

    /**
     * 初始化操作面板
     **/
    private JPanel initOperationPanel() {
        // 当前行
        current = initTextField();
        // 总行数
        total.setText("/" + totalPages());

        JPanel panelRight = new JPanel();
        panelRight.setBorder(JBUI.Borders.empty(0, 20));
        panelRight.setPreferredSize(new Dimension(280, 30));
        panelRight.add(current, BorderLayout.EAST);
        panelRight.add(total, BorderLayout.EAST);
        //上一页
        upButton = initUpButton();
        panelRight.add(upButton, BorderLayout.EAST);
        //下一页
        downButton = initDownButton();
        panelRight.add(downButton, BorderLayout.EAST);
        //老板键
        JButton boss = initBossButton();
        panelRight.add(boss, BorderLayout.SOUTH);
        updateHotkeys();
        return panelRight;
    }

    /**
     * 从设置读取热键并绑定：上一页/下一页仅工具窗口内生效，老板键全局生效。
     * refresh() 会重新调用本方法，使设置页修改的热键即时生效。
     **/
    private void updateHotkeys() {
        KeyStroke oldPrev = prevKeyStroke;
        KeyStroke oldNext = nextKeyStroke;
        prevKeyStroke = HotkeyUtil.parse(persistentState.getBefore());
        nextKeyStroke = HotkeyUtil.parse(persistentState.getNext());
        bossKeyStroke = HotkeyUtil.parse(persistentState.getBossKey());
        if (upButton == null || downButton == null) {
            return;
        }
        if (oldPrev != null) {
            upButton.unregisterKeyboardAction(oldPrev);
        }
        if (oldNext != null) {
            downButton.unregisterKeyboardAction(oldNext);
        }
        if (prevKeyStroke != null) {
            upButton.registerKeyboardAction(upButton.getActionListeners()[0], prevKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        if (nextKeyStroke != null) {
            downButton.registerKeyboardAction(downButton.getActionListeners()[0], nextKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
    }

    /**
     * 跳页输入框
     **/
    private JTextField initTextField() {
        JTextField current = new JTextField("current line:");
        current.setPreferredSize(new Dimension(50, 30));
        current.setOpaque(false);
        current.setBorder(JBUI.Borders.empty(0));
        current.setText(currentPage / lineCount + "");
        current.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //判断按下的键是否是回车键
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        String input = current.getText();
                        String inputCurrent = input.split("/")[0].trim();
                        int i = Integer.parseInt(inputCurrent);
                        if (i <= 1) {
                            seek = 0;
                            currentPage = 0;
                        } else {
                            currentPage = (i - 1) * lineCount;
                            if (currentPage > totalLine) {
                                currentPage = Math.max(0, totalLine - 1);
                            }
                        }
                        final boolean fromStart = currentPage == 0;
                        runIoAsync(() -> {
                            if (!fromStart) {
                                countSeek();
                            } else {
                                seek = 0;
                            }
                            return readBook();
                        }, content -> {
                            textArea.setText(content);
                            saveProgress();
                            current.setText(" " + currentPage / lineCount);
                            syncTocSelection();
                        });
                    } catch (NumberFormatException e2) {
                        textArea.setText("请输入数字");
                    }

                }
            }
        });
        return current;
    }

    /**
     * 重新读取设置并应用到阅读界面。
     * 打开窗口、切书与设置页 apply() 均调用本方法，设置修改后无需手动刷新即可生效。
     **/
    public void refresh() {
        if (textArea == null) {
            return;
        }
        try {
            persistentState = PersistentState.getInstance();
            String bookPath = persistentState.getBookPathText();
            boolean bookChanged = (bookPath == null || bookPath.isEmpty())
                    || !Objects.equals(bookFile, bookPath);
            if (bookChanged) {
                bookFile = bookPath;
                // 切书后从该书独立保存的进度恢复
                currentPage = parseIntSafe(persistentState.getCurrentLineFor(bookFile), 0);
                seek = 0;
                seekDictionary.clear();
                fileCharset = null;
                epubTextFile = null;
                epubLastModified = 0;
                epubToc = null;
            } else {
                // 初始化当前行数（按书独立保存）
                currentPage = parseIntSafe(persistentState.getCurrentLineFor(bookFile), currentPage);
            }
            type = persistentState.getFontType();
            size = persistentState.getFontSize();
            lineCount = parseIntSafe(persistentState.getLineCount(), lineCount);
            lineSpace = parseIntSafe(persistentState.getLineSpace(), lineSpace);
            updateHotkeys();
            updateBookSelector();

            if (bookFile == null || bookFile.isEmpty()) {
                totalLine = 0;
                textArea.setText(temp);
                updatePageInfo();
                updateTocPanel();
                textArea.setFont(resolveFont());
                return;
            }
            // 仅在切书或尚未统计过时才全量扫描行数，避免每次刷新都重扫大文件
            final boolean needCount = bookChanged || totalLine == 0;
            if (!runIoAsync(() -> {
                // 切书时先把 epub 解包成文本（首次解包可能耗时），失败直接抛给用户看
                resolveReadPath();
                if (needCount) {
                    totalLine = countLine();
                }
                // 重新定位到当前页起点并重读该页正文，保持阅读进度（currentPage）不变，
                // 仅首次加载（currentPage 为 0）时让 readBook 自然推进到第 1 页末尾，与手动翻页语义一致
                int pageStart = currentPage;
                if (pageStart > 0) {
                    currentPage = Math.max(0, currentPage - lineCount);
                }
                countSeek();
                String content = readBook();
                if (pageStart > 0) {
                    currentPage = pageStart;
                }
                return content;
            }, content -> {
                textArea.setText(content);
                updatePageInfo();
                updateTocPanel();
                syncTocSelection();
                textArea.setFont(resolveFont());
            })) {
                // 正在翻页/读取中：标记稍后自动重试，确保设置修改必然生效
                pendingRefresh.set(true);
            }
        } catch (Exception newE) {
            newE.printStackTrace();
        }
    }

    /**
     * 向上翻页按钮
     **/
    private JButton initUpButton() {
        JButton afterB = new JButton("prev");
        afterB.setPreferredSize(new Dimension(40, 20));
        afterB.setContentAreaFilled(false);
        afterB.setBorderPainted(false);
        afterB.addActionListener(e -> {
            if (currentPage > totalLine) {
                return;
            }
            if (currentPage / lineCount <= 1) {
                return;
            }
            if (currentPage % lineCount == 0) {
                currentPage = currentPage - lineCount * 2;
            } else {
                while (currentPage % lineCount != 0) {
                    currentPage--;
                }
                currentPage -= lineCount;
            }
            runIoAsync(() -> {
                countSeek();
                return readBook();
            }, content -> {
                textArea.setText(content);
                saveProgress();
                current.setText(" " + currentPage / lineCount);
                syncTocSelection();
            });
        });

        return afterB;
    }

    /**
     * 向下翻页按钮
     **/
    private JButton initDownButton() {
        JButton nextB = new JButton("next");
        nextB.setPreferredSize(new Dimension(40, 20));
        nextB.setContentAreaFilled(false);
        nextB.setBorderPainted(false);
        nextB.addActionListener(e -> {

            if (currentPage >= totalLine) {
                return;
            }
            runIoAsync(() -> {
                if (currentPage / lineCount <= 1) {
                    countSeek();
                }
                return readBook();
            }, content -> {
                textArea.setText(content);
                saveProgress();
                current.setText(" " + (currentPage % lineCount == 0 ? currentPage / lineCount : currentPage / lineCount + 1));
                syncTocSelection();
            });

        });

        return nextB;
    }

    /**
     * 老板键：隐藏/恢复阅读界面（正文与操作按钮）。
     * 隐藏时整个工具窗口伪装成"Terminal"（修改 Tab 标题、图标与正文），
     * 由老板键按钮与全局快捷键（设置页可配置）共同触发，
     * 任何窗口（包括设置页）获得焦点时都生效。
     **/
    public void toggleBoss() {
        JButton[] buttons = {upButton, downButton, bossButton};
        if (hide) {
            for (JButton b : buttons) {
                b.setVisible(true);
            }
            current.setVisible(true);
            total.setVisible(true);
            if (bookSelector != null) {
                bookSelector.setVisible(persistentState.getBookPathList().size() > 1);
            }
            if (tocPanel != null) {
                tocPanel.setVisible(epubToc != null && !epubToc.isEmpty());
            }
            textArea.setText(temp);
            textArea.setFont(resolveFont());
            if (content != null) {
                content.setDisplayName("Thief-Book");
            }
            if (toolWindow != null) {
                toolWindow.setIcon(originIcon);
            }
            hide = false;
        } else {
            for (JButton b : buttons) {
                b.setVisible(false);
            }
            current.setVisible(false);
            total.setVisible(false);
            if (bookSelector != null) {
                bookSelector.setVisible(false);
            }
            if (tocPanel != null) {
                tocPanel.setVisible(false);
            }
            temp = textArea.getText();
            textArea.setText(BOSS_FAKE_TEXT);
            textArea.setFont(UIUtil.getFontWithFallback(new Font(Font.MONOSPACED, Font.PLAIN, 12)));
            if (content != null) {
                content.setDisplayName("Terminal");
            }
            if (toolWindow != null) {
                if (originIcon == null) {
                    originIcon = toolWindow.getIcon();
                }
                toolWindow.setIcon(AllIcons.Debugger.Console);
            }
            hide = true;
        }
    }

    /**
     * 隐藏按钮
     **/
    private JButton initBossButton() {
        //老板键
        bossButton = new JButton(" ");
        bossButton.setPreferredSize(new Dimension(5, 5));
        bossButton.setContentAreaFilled(false);
        bossButton.setBorderPainted(false);
        bossButton.addActionListener(e -> toggleBoss());
        return bossButton;
    }

    /**
     * 向下读取文件
     **/
    private String readBook() throws IOException {
        RandomAccessFile ra = null;
        StringBuilder str = new StringBuilder();
        try {
            ensureCharset();
            ra = new RandomAccessFile(resolveReadPath(), "r");
            ra.seek(seek);
            StringBuilder nStr = new StringBuilder();
            for (int j = 0; j < lineSpace + 1; j++) {
                nStr.append("\n");
            }
            int got = readLines(ra, str, lineCount, nStr.toString(), fileCharset);
            currentPage += got;
            seek = ra.getFilePointer();
            if (currentPage % cacheInterval == 0) {
                seekDictionary.put(currentPage, seek);
            }
            // 去掉 UTF-8 BOM 字符
            if (str.length() > 0 && str.charAt(0) == '\uFEFF') {
                str.deleteCharAt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            if (ra != null) {
                ra.close();
            }
        }
        return str.toString();
    }

    /**
     * 读取文件总行数
     * 按字节块扫描换行符计数，避免逐行 readLine 的开销
     **/
    private int countLine() throws IOException {
        try (RandomAccessFile ra = new RandomAccessFile(resolveReadPath(), "r")) {
            int i = 0;
            seekDictionary.put(0, ra.getFilePointer());
            byte[] buf = new byte[8192];
            int n;
            boolean any = false;
            boolean lastByteNewline = false;
            while ((n = ra.read(buf)) != -1) {
                any = true;
                lastByteNewline = buf[n - 1] == '\n';
                long blockStart = ra.getFilePointer() - n;
                for (int j = 0; j < n; j++) {
                    if (buf[j] == '\n') {
                        i++;
                        if (i % cacheInterval == 0) {
                            seekDictionary.put(i, blockStart + j + 1);
                        }
                    }
                }
            }
            // 末行无换行结尾时补计一行，与 readLine 语义一致
            if (any && !lastByteNewline) {
                i++;
            }
            return i;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 找到当前指针应在位置
     **/
    private void countSeek() throws IOException {
        RandomAccessFile ra = null;

        try {
            if (seekDictionary.containsKey(currentPage)) {
                this.seek = seekDictionary.get(currentPage);
            } else {
                ra = new RandomAccessFile(resolveReadPath(), "r");
                int line = 0;
                for (int i = 0; cacheInterval * i < currentPage; i++) {
                    line = cacheInterval * i;
                    Long cached = seekDictionary.get(line);
                    if (cached != null) {
                        ra.seek(cached);
                    } else {
                        // 缓存缺失（如首次跳页尚未统计），回退从头读
                        ra.seek(0);
                        line = 0;
                        break;
                    }
                }
                StringBuilder dummy = new StringBuilder();
                readLines(ra, dummy, currentPage - line, "\n", null);
                this.seek = ra.getFilePointer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ra != null) {
                ra.close();
            }
        }
    }

    /**
     * 检测并缓存文件编码。
     * UTF-8（含 BOM）直接识别；非 UTF-8 的中文文本回退 GB18030（兼容 GBK/ANSI）。
     * 检测失败（如 UTF-16）时抛出异常，由调用方展示给用户。
     **/
    private void ensureCharset() throws IOException {
        if (fileCharset == null) {
            if (bookFile == null || bookFile.isEmpty()) {
                throw new IOException("未设置书本文件路径");
            }
            fileCharset = detectCharset(new File(resolveReadPath()));
        }
    }

    /**
     * 返回实际读取的文件路径：epub 先解包成 UTF-8 临时文本再读（源文件变化时自动重新解包），
     * 其余格式直接读原文件。解包失败（如损坏的 epub）时抛出异常，由调用方展示给用户。
     **/
    private String resolveReadPath() throws IOException {
        if (bookFile != null && !bookFile.isEmpty() && bookFile.toLowerCase().endsWith(".epub")) {
            File epub = new File(bookFile);
            long modified = epub.lastModified();
            if (epubTextFile == null || !epubTextFile.exists() || epubLastModified != modified) {
                // 一次性拿到正文临时文件与目录（含正文行号），后面翻页/跳页都直接读临时 txt
                EpubUtil.EpubBook pkg = EpubUtil.extract(epub);
                epubTextFile = EpubUtil.writeTempFile(pkg);
                epubToc = pkg.toc;
                epubLastModified = modified;
                // 换了临时文件，旧的指针缓存全部失效
                seekDictionary.clear();
            }
            return epubTextFile.getAbsolutePath();
        }
        // 非 epub：清掉 epub 专属目录，避免上次 epub 残留目录条目
        if (epubToc != null) {
            epubToc = null;
        }
        return bookFile;
    }

    private Charset detectCharset(File file) throws IOException {
        try (RandomAccessFile ra = new RandomAccessFile(file, "r")) {
            byte[] sample = new byte[4096];
            int total = 0;
            int n;
            while (total < sample.length && (n = ra.read(sample, total, sample.length - total)) != -1) {
                total += n;
            }
            if (total >= 3 && (sample[0] & 0xFF) == 0xEF && (sample[1] & 0xFF) == 0xBB && (sample[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
            if (total >= 2 && (sample[0] & 0xFF) == 0xFF && (sample[1] & 0xFF) == 0xFE) {
                throw new IOException("暂不支持 UTF-16 LE 编码的文本文件");
            }
            if (total >= 2 && (sample[0] & 0xFF) == 0xFE && (sample[1] & 0xFF) == 0xFF) {
                throw new IOException("暂不支持 UTF-16 BE 编码的文本文件");
            }
            // 从尾部最多回退 4 字节，避免多字节字符被截断导致误判
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            for (int cut = 0; cut < total && cut <= 4; cut++) {
                try {
                    decoder.reset().decode(ByteBuffer.wrap(sample, 0, total - cut));
                    return StandardCharsets.UTF_8;
                } catch (CharacterCodingException ignored) {
                }
            }
            return Charset.forName("GB18030");
        }
    }

    /**
     * 从当前文件指针批量读取最多 maxLines 行，按 sep 分隔追加到 out。
     * charset 为 null 时只统计行数不生成文本（供定位指针使用）。
     * 返回实际读取的行数。
     **/
    private int readLines(RandomAccessFile ra, StringBuilder out, int maxLines, String sep, Charset charset) throws IOException {
        byte[] buf = new byte[8192];
        byte[] tail = new byte[8192];
        int tailLen = 0;
        int count = 0;
        int n;
        // 最后一条被统计行的换行符之后的绝对字节位置（即下一条待读行的起点）
        long lineEnd = ra.getFilePointer();
        // 最后一条被统计的行是否无换行结尾（已读到文件尾），此时文件指针应保持在 EOF
        boolean lastLineNoNewline = false;
        while (count < maxLines && (n = ra.read(buf)) != -1) {
            long blockStart = ra.getFilePointer() - n;
            int start = 0;
            for (int j = 0; j < n && count < maxLines; j++) {
                if (buf[j] == '\n') {
                    appendLine(out, buf, start, j, tail, tailLen, sep, charset);
                    count++;
                    start = j + 1;
                    tailLen = 0;
                    lineEnd = blockStart + j + 1;
                    lastLineNoNewline = false;
                }
            }
            if (count < maxLines && start < n) {
                int need = tailLen + (n - start);
                if (need > tail.length) {
                    tail = Arrays.copyOf(tail, Math.max(need, tail.length * 2));
                }
                System.arraycopy(buf, start, tail, tailLen, n - start);
                tailLen = need;
            }
        }
        if (count < maxLines && tailLen > 0) {
            appendLine(out, buf, 0, 0, tail, tailLen, sep, charset);
            count++;
            lastLineNoNewline = true;
        }
        // 批量块读取会把指针推进到最后一个块的末尾，这里回退到最后一条被统计行的结尾，
        // 保证 seek 始终落在"下一行起点"，翻页/跳页不会跳过行或从某行中间开始读
        if (count > 0 && !lastLineNoNewline) {
            ra.seek(lineEnd);
        }
        return count;
    }

    private void appendLine(StringBuilder out, byte[] buf, int bufStart, int bufEnd, byte[] tail, int tailLen, String sep, Charset charset) {
        int rawLen = tailLen + (bufEnd - bufStart);
        byte[] lineBytes = new byte[rawLen];
        if (tailLen > 0) {
            System.arraycopy(tail, 0, lineBytes, 0, tailLen);
        }
        if (bufEnd > bufStart) {
            System.arraycopy(buf, bufStart, lineBytes, tailLen, bufEnd - bufStart);
        }
        if (rawLen > 0 && lineBytes[rawLen - 1] == '\r') {
            lineBytes = Arrays.copyOf(lineBytes, rawLen - 1);
        }
        if (charset != null) {
            out.append(new String(lineBytes, charset)).append(sep);
        }
    }

    /**
     * 把 IO 任务放到后台线程池执行，完成后回到 EDT 更新界面。
     * busy 标志保证同一时刻只有一个翻页/读取任务在跑，避免连点导致的状态错乱。
     * 返回是否成功启动；busy 中调用会返回 false，由调用方决定是否稍后重试。
     **/
    private boolean runIoAsync(IoSupplier ioSupplier, Consumer<String> onEdt) {

        if (!busy.compareAndSet(false, true)) {
            return false;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String content;
            try {
                content = ioSupplier.get();
            } catch (Exception e) {
                e.printStackTrace();
                final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                ApplicationManager.getApplication().invokeLater(() -> {
                    textArea.setText(msg);
                    busy.set(false);
                    retryPendingRefresh();
                });
                return;
            }
            final String result = content;
            ApplicationManager.getApplication().invokeLater(() -> {
                onEdt.accept(result);
                busy.set(false);
                retryPendingRefresh();
            });
        });
        return true;
    }

    /**
     * 若存在被 busy 拦截的刷新请求，在 EDT 上重新执行一次 refresh()
     **/
    private void retryPendingRefresh() {
        if (pendingRefresh.compareAndSet(true, false)) {
            refresh();
        }
    }

    @FunctionalInterface
    private interface IoSupplier {
        String get() throws IOException;
    }

    /**
     * 在 EDT 上保存当前阅读进度（按书独立保存）
     **/
    private void saveProgress() {
        persistentState.setCurrentLineFor(bookFile, String.valueOf(currentPage));
    }

    private void updatePageInfo() {
        current.setText(" " + currentPage / lineCount);
        total.setText("/" + totalPages());
    }

    private int totalPages() {
        return totalLine % lineCount == 0 ? totalLine / lineCount : totalLine / lineCount + 1;
    }

    private static int parseIntSafe(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

}
