package com.thief.idea.ui;

import com.intellij.ui.FontComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.thief.idea.PersistentState;
import com.thief.idea.util.HotkeyUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class SettingUi {

    /**
     * 字号默认值（与 PersistentState 的默认字号一致）
     **/
    private static final int DEFAULT_FONT_SIZE = 14;

    /**
     * 每页行数 / 行间距默认值（与 PersistentState.getLineCount/getLineSpace 的兜底值一致）
     **/
    private static final String DEFAULT_LINE_COUNT = "1";
    private static final String DEFAULT_LINE_SPACE = "0";

    /**
     * 字体预览区示例文本行池：按"每页行数"取前 N 行，不足时循环取
     **/
    private static final String[] FONT_PREVIEW_LINES = {
            "Font preview 字体预览 0123456789",
            "天地玄黄，宇宙洪荒。日月盈昃，辰宿列张。",
            "寒来暑往，秋收冬藏。闰余成岁，律吕调阳。",
            "云腾致雨，露结为霜。金生丽水，玉出昆冈。",
    };

    /**
     * 预览区高度上限：JTextComponent 的 minimum size 会随字号/行数暴涨，
     * 必须显式锁死 min/preferred/max，否则大字号或多行会把下方控件挤出可视区域
     **/
    private static final int FONT_PREVIEW_MAX_HEIGHT = 240;

    /**
     * 书本列表可视行数及对应的固定高度（配合 readerPanel 第 2 行的 fill:d 行高）
     **/
    private static final int BOOK_LIST_ROWS = 5;
    private static final int BOOK_LIST_HEIGHT = 90;


    public JPanel mainPanel;
    public JPanel readerPanel;
    public JPanel fontsPanel;
    public JPanel hotkeysPanel;
    public JLabel chooseFileLabel;
    public JLabel Label3;
    public JComboBox fontSize;
    public FontComboBox fontType;
    public JLabel fontSizeLabel;
    public JLabel label6;
    public JComboBox lineCount;
    public JLabel label7;
    public JComboBox lineSpace;
    public JLabel label4;
    public JTextField before;
    public JLabel label5;
    public JTextField next;
    public JTextField bossKey;
    public JButton removeBookButton;
    public JButton addBookButton;
    public JList bookList = new JList();
    public JScrollPane bookScrollPane;
    public JEditorPane fontPreview;
    public JButton restoreDefaultButton;

    /**
     * 书本列表数据模型（工作副本，Apply 时写入 PersistentState）
     **/
    private final DefaultListModel<String> bookListModel = new DefaultListModel<>();


    public SettingUi() {
        // 书本列表：单选/多选均可，列表显示文件名，完整路径见提示。
        // bookList 不在 .form 中声明：GUI Designer 的 <scrollpane/> 约束会被 instrumentCode
        // 织入成 readerPanel.add(bookList, cc(1,1))，把列表塞到面板左上角并撑宽第一列。
        // 这里由代码显式装进 bookScrollPane，form 只负责摆放滚动区。
        bookScrollPane.setViewportView(bookList);
        bookList.setModel(bookListModel);
        bookList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        bookList.setVisibleRowCount(BOOK_LIST_ROWS);

        // 与 fontPreview 同理：列表为空时 JList 报出的首选高度会把整行压扁，
        // 这里显式锁死滚动区高度，保证空列表也留出固定的可视行数
        Dimension bookListSize = new Dimension(100, JBUIScale.scale(BOOK_LIST_HEIGHT));
        bookScrollPane.setPreferredSize(bookListSize);
        bookScrollPane.setMinimumSize(bookListSize);

        // 添加书本：文件选择器支持多选，追加到列表并去重
        addBookButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.showOpenDialog(mainPanel);
            File[] files = fileChooser.getSelectedFiles();
            if (files != null) {
                for (File file : files) {
                    String path = file.getPath();
                    if (!bookListModel.contains(path)) {
                        bookListModel.addElement(path);
                    }
                }
            }
        });

        // 移除选中的书本
        removeBookButton.addActionListener(e -> {
            for (Object selected : bookList.getSelectedValuesList()) {
                bookListModel.removeElement(selected);
            }
        });

        // 字号下拉：11~24
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        for (int i = 11; i < 25; i++) {
            defaultComboBoxModel1.addElement(i + "");
        }
        fontSize.setModel(defaultComboBoxModel1);
        fontSize.setToolTipText("");

        // 字体预览区（仿 TranslationPlugin 的字体设置页）：
        // FontComboBox 每个下拉项都用自身字体渲染，选中后下方预览区实时按
        // 所选字体 + 字号 + 每页行数 + 行间距展示；尺寸由 updateFontPreview 统一锁定
        fontPreview.setEditable(false);
        fontPreview.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border()), JBUI.Borders.empty(3)));

        // 字体/字号变化时刷新预览
        fontType.addItemListener(e -> updateFontPreview());
        fontSize.addItemListener(e -> updateFontPreview());

        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        for (int i = 1; i < 11; i++) {
            defaultComboBoxModel3.addElement(i + "");
        }
        lineCount.setModel(defaultComboBoxModel3);
        lineCount.setToolTipText("");

        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        for (int i = 0; i < 3; i++) {
            defaultComboBoxModel4.addElement(i + "");
        }
        lineSpace.setModel(defaultComboBoxModel4);
        lineSpace.setToolTipText("");

        // 每页行数 / 行间距变化时同样刷新预览（监听要在 setModel 之后注册，
        // 避免装填下拉项的过程中触发无谓的重绘）
        lineCount.addItemListener(e -> updateFontPreview());
        lineSpace.addItemListener(e -> updateFontPreview());

        // 恢复默认：字体跟随 IDE 默认字体，字号 / 每页行数 / 行间距回到默认值
        restoreDefaultButton.addActionListener(e -> {
            fontType.setFontName(null);
            fontSize.setSelectedItem(String.valueOf(DEFAULT_FONT_SIZE));
            lineCount.setSelectedItem(DEFAULT_LINE_COUNT);
            lineSpace.setSelectedItem(DEFAULT_LINE_SPACE);
            updateFontPreview();
        });

        // 热键输入框：直接按下组合键即可录入，退格键清空
        installHotkeyCapture(before);
        installHotkeyCapture(next);
        installHotkeyCapture(bossKey);
    }

    /**
     * 热键输入框按键捕获：
     * 按下非修饰键时记录组合键并显示，退格键清空；
     * 捕获期间置位 HotkeyUtil.editingHotkey，避免误触发全局老板键
     **/
    private void installHotkeyCapture(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                // 单独按下修饰键或 Esc 时忽略
                if (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL
                        || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META
                        || keyCode == KeyEvent.VK_WINDOWS || keyCode == KeyEvent.VK_ESCAPE) {
                    return;
                }
                e.consume();
                if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    field.setText("");
                    return;
                }
                field.setText(HotkeyUtil.format(keyCode, e.getModifiersEx()));
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                HotkeyUtil.editingHotkey = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                HotkeyUtil.editingHotkey = false;
            }
        });
    }

    public void innit(PersistentState persistentState) {
        if (fontSize.getSelectedItem() == null) {
            fontSize.setSelectedItem(DEFAULT_FONT_SIZE);
        }
        bookListModel.clear();
        for (String path : persistentState.getBookPathList()) {
            bookListModel.addElement(path);
        }
        fontSize.setSelectedItem(persistentState.getFontSize());
        // 老配置里的"系统默认"等价于 FontComboBox 的未选择（null），其余直接按字体族名回填
        String fontTypeValue = persistentState.getFontType();
        fontType.setFontName(PersistentState.DEFAULT_FONT.equals(fontTypeValue) ? null : fontTypeValue);
        before.setText(persistentState.getBefore());
        next.setText(persistentState.getNext());
        lineCount.setSelectedItem(persistentState.getLineCount());
        lineSpace.setSelectedItem(persistentState.getLineSpace());
        bossKey.setText(persistentState.getBossKey());
        updateFontPreview();
    }

    /**
     * 把书本下拉的当前值转成持久化格式：未选择（跟随 IDE 默认字体）记作"系统默认"，
     * 与老配置兼容，MainUi 无需改动
     **/
    public String getSelectedFontType() {
        String fontName = fontType.getFontName();
        return (fontName == null || fontName.isEmpty()) ? PersistentState.DEFAULT_FONT : fontName;
    }

    /**
     * 当前书本列表（按添加顺序）
     **/
    public List<String> getBookList() {
        return Collections.list(bookListModel.elements());
    }

    /**
     * 刷新预览区：用所选字体族 + 字号渲染示例文本，并按"每页行数""行间距"还原阅读区的排版；
     * 所选字体缺中文字形时用 UIUtil 的回退字体链兜底（否则中文显示成方块乱码）；
     * 未选字体时跟随 IDE 默认字体，与阅读区 MainUi.resolveFont() 的默认分支一致
     **/
    private void updateFontPreview() {
        String fontName = fontType.getFontName();
        int size = parseFontSize();
        Font font;
        if (fontName != null && !fontName.isEmpty()) {
            font = UIUtil.getFontWithFallback(new Font(fontName, Font.PLAIN, size));
        } else {
            font = UIUtil.getFontWithFallback(UIUtil.getLabelFont()).deriveFont(Font.PLAIN, size);
        }
        fontPreview.setFont(font);
        fontPreview.setText(buildPreviewText());

        // 预览高度随行数/字号变化，但要锁死上限，否则会把下方控件挤出可视区域
        int visualLines = countPreviewLines();
        int lineHeight = fontPreview.getFontMetrics(font).getHeight();
        int height = Math.min(
                visualLines * lineHeight + JBUIScale.scale(12),
                JBUIScale.scale(FONT_PREVIEW_MAX_HEIGHT));
        Dimension previewSize = new Dimension(100, height);
        fontPreview.setPreferredSize(previewSize);
        fontPreview.setMinimumSize(previewSize);
        fontPreview.setMaximumSize(previewSize);
        fontPreview.revalidate();
        fontPreview.repaint();
    }

    /**
     * 按当前"每页行数""行间距"拼出示例文本：
     * 与 MainUi.readBook() 一致——每页取 lineCount 行，行间插入 lineSpace 个空行
     **/
    private String buildPreviewText() {
        int lines = parseComboInt(lineCount, 1);
        int space = parseComboInt(lineSpace, 0);
        StringBuilder separator = new StringBuilder("\n");
        for (int i = 0; i < space; i++) {
            separator.append("\n");
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            if (i > 0) {
                text.append(separator);
            }
            text.append(FONT_PREVIEW_LINES[i % FONT_PREVIEW_LINES.length]);
        }
        return text.toString();
    }

    /**
     * 预览文本占用的视觉行数（正文行 + 行间空行）
     **/
    private int countPreviewLines() {
        int lines = parseComboInt(lineCount, 1);
        int space = parseComboInt(lineSpace, 0);
        return lines + Math.max(0, lines - 1) * space;
    }

    private int parseComboInt(JComboBox comboBox, int fallback) {
        Object selected = comboBox.getSelectedItem();
        if (selected == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(selected.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseFontSize() {
        Object selected = fontSize.getSelectedItem();
        if (selected == null) {
            return DEFAULT_FONT_SIZE;
        }
        try {
            return Integer.parseInt(selected.toString());
        } catch (NumberFormatException e) {
            return DEFAULT_FONT_SIZE;
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void createUIComponents() {
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        CellConstraints cc = new CellConstraints();
        readerPanel = new JPanel();
        readerPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:max(60dlu;d):noGrow,left:4dlu:noGrow,fill:220px:noGrow,left:6dlu:noGrow,fill:max(d;4px):noGrow", "top:4dlu:noGrow,fill:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        readerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader"));
        mainPanel.add(readerPanel, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        chooseFileLabel = new JLabel();
        chooseFileLabel.setText("Books:");
        readerPanel.add(chooseFileLabel, cc.xy(2, 2, CellConstraints.LEFT, CellConstraints.TOP));
        bookScrollPane = new JScrollPane();
        readerPanel.add(bookScrollPane, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.FILL));
        removeBookButton = new JButton();
        removeBookButton.setText("Remove");
        readerPanel.add(removeBookButton, cc.xy(6, 2, CellConstraints.DEFAULT, CellConstraints.TOP));
        addBookButton = new JButton();
        addBookButton.setText("Add...");
        readerPanel.add(addBookButton, cc.xy(4, 4));
        fontsPanel = new JPanel();
        fontsPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:max(60dlu;d):noGrow,left:4dlu:noGrow,fill:220px:noGrow,left:6dlu:noGrow,fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        fontsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Style"));
        mainPanel.add(fontsPanel, cc.xy(1, 4, CellConstraints.FILL, CellConstraints.FILL));
        Label3 = new JLabel();
        Label3.setText("Font:");
        fontsPanel.add(Label3, cc.xy(2, 2));
        fontType = new FontComboBox();
        fontsPanel.add(fontType, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.DEFAULT));
        fontSizeLabel = new JLabel();
        fontSizeLabel.setText("Font size:");
        fontsPanel.add(fontSizeLabel, cc.xy(2, 4));
        fontSize = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("8");
        defaultComboBoxModel1.addElement("9");
        defaultComboBoxModel1.addElement("10");
        defaultComboBoxModel1.addElement("11");
        defaultComboBoxModel1.addElement("12");
        defaultComboBoxModel1.addElement("13");
        defaultComboBoxModel1.addElement("14");
        defaultComboBoxModel1.addElement("15");
        defaultComboBoxModel1.addElement("16");
        defaultComboBoxModel1.addElement("17");
        defaultComboBoxModel1.addElement("18");
        defaultComboBoxModel1.addElement("19");
        defaultComboBoxModel1.addElement("20");
        defaultComboBoxModel1.addElement("21");
        defaultComboBoxModel1.addElement("22");
        defaultComboBoxModel1.addElement("23");
        defaultComboBoxModel1.addElement("24");
        fontSize.setModel(defaultComboBoxModel1);
        fontSize.setToolTipText("");
        fontsPanel.add(fontSize, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        label6 = new JLabel();
        label6.setText("Lines per page:");
        fontsPanel.add(label6, cc.xy(2, 6));
        lineCount = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("1");
        defaultComboBoxModel3.addElement("2");
        defaultComboBoxModel3.addElement("3");
        lineCount.setModel(defaultComboBoxModel3);
        lineCount.setToolTipText("");
        fontsPanel.add(lineCount, cc.xy(4, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        label7 = new JLabel();
        label7.setText("Line spacing:");
        fontsPanel.add(label7, cc.xy(2, 8));
        lineSpace = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("0");
        defaultComboBoxModel4.addElement("1");
        defaultComboBoxModel4.addElement("2");
        lineSpace.setModel(defaultComboBoxModel4);
        lineSpace.setToolTipText("");
        fontsPanel.add(lineSpace, cc.xy(4, 8, CellConstraints.FILL, CellConstraints.DEFAULT));
        fontPreview = new JEditorPane();
        fontPreview.setEditable(false);
        fontsPanel.add(fontPreview, cc.xyw(2, 10, 5, CellConstraints.FILL, CellConstraints.FILL));
        restoreDefaultButton = new JButton();
        restoreDefaultButton.setText("Restore default");
        fontsPanel.add(restoreDefaultButton, cc.xy(4, 12));
        hotkeysPanel = new JPanel();
        hotkeysPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:max(60dlu;d):noGrow,left:4dlu:noGrow,fill:220px:noGrow,left:6dlu:noGrow,fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        hotkeysPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Hotkeys"));
        mainPanel.add(hotkeysPanel, cc.xy(1, 6, CellConstraints.FILL, CellConstraints.FILL));
        label4 = new JLabel();
        label4.setText("Previous page key:");
        hotkeysPanel.add(label4, cc.xy(2, 2));
        before = new JTextField();
        before.setToolTipText("Click, then press a key combination. Backspace to clear.");
        hotkeysPanel.add(before, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.DEFAULT));
        label5 = new JLabel();
        label5.setText("Next page key:");
        hotkeysPanel.add(label5, cc.xy(2, 4));
        next = new JTextField();
        next.setToolTipText("Click, then press a key combination. Backspace to clear.");
        hotkeysPanel.add(next, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label8 = new JLabel();
        label8.setText("Boss key:");
        hotkeysPanel.add(label8, cc.xy(2, 6));
        bossKey = new JTextField();
        bossKey.setToolTipText("Click, then press a key combination. Backspace to clear.");
        hotkeysPanel.add(bossKey, cc.xy(4, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label9 = new JLabel();
        label9.setText("<html>Tip: Click a key field, then press a key combination. Press Backspace to clear. Click Refresh in the tool window to apply.</html>");
        hotkeysPanel.add(label9, cc.xyw(2, 8, 5));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
