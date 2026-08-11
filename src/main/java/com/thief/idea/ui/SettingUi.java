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

public class SettingUi {

    /**
     * 字号默认值（与 PersistentState 的默认字号一致）
     **/
    private static final int DEFAULT_FONT_SIZE = 14;

    /**
     * 字体预览区示例文本
     **/
    private static final String FONT_PREVIEW_TEXT =
            "Font preview 字体预览\n天地玄黄，宇宙洪荒。日月盈昃，辰宿列张。0123456789";

    /**
     * 预览区固定高度：JTextComponent 的 minimum size 会随字号/文本暴涨，
     * 必须显式锁死 min/preferred/max，否则大字号会把下方控件挤出可视区域
     **/
    private static final int FONT_PREVIEW_HEIGHT = 72;


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
    public JButton button2;
    public JTextField bookPathText;
    public JEditorPane fontPreview;
    public JButton restoreDefaultButton;


    public SettingUi() {
        button2.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.showOpenDialog(mainPanel);
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                bookPathText.setText(file.getPath());
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
        // FontComboBox 每个下拉项都用自身字体渲染，选中后下方预览区实时按所选字体+字号展示；
        // 锁定三组尺寸，避免大字号把布局撑爆
        fontPreview.setText(FONT_PREVIEW_TEXT);
        fontPreview.setEditable(false);
        fontPreview.setBorder(BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border()), JBUI.Borders.empty(3)));
        Dimension previewSize = new Dimension(100, JBUIScale.scale(FONT_PREVIEW_HEIGHT));
        fontPreview.setPreferredSize(previewSize);
        fontPreview.setMinimumSize(previewSize);
        fontPreview.setMaximumSize(previewSize);

        // 字体/字号变化时刷新预览
        fontType.addItemListener(e -> updateFontPreview());
        fontSize.addItemListener(e -> updateFontPreview());

        // 恢复默认：字体跟随 IDE 默认字体，字号回到默认值
        restoreDefaultButton.addActionListener(e -> {
            fontType.setFontName(null);
            fontSize.setSelectedItem(String.valueOf(DEFAULT_FONT_SIZE));
            updateFontPreview();
        });

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
        bookPathText.setText(persistentState.getBookPathText());
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
     * 把字体下拉的当前值转成持久化格式：未选择（跟随 IDE 默认字体）记作"系统默认"，
     * 与老配置兼容，MainUi 无需改动
     **/
    public String getSelectedFontType() {
        String fontName = fontType.getFontName();
        return (fontName == null || fontName.isEmpty()) ? PersistentState.DEFAULT_FONT : fontName;
    }

    /**
     * 刷新预览区：用所选字体族 + 字号渲染示例文本；
     * 所选字体缺中文字形时用 UIUtil 的回退字体链兜底（否则中文显示成方块乱码）；
     * 未选字体时跟随 IDE 默认字体，与阅读区 MainUi.resolveFont() 的默认分支一致
     **/
    private void updateFontPreview() {
        String fontName = fontType.getFontName();
        int size = parseFontSize();
        if (fontName != null && !fontName.isEmpty()) {
            fontPreview.setFont(UIUtil.getFontWithFallback(new Font(fontName, Font.PLAIN, size)));
        } else {
            fontPreview.setFont(UIUtil.getFontWithFallback(UIUtil.getLabelFont()).deriveFont(Font.PLAIN, size));
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
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        CellConstraints cc = new CellConstraints();
        readerPanel = new JPanel();
        readerPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:max(60dlu;d):noGrow,left:4dlu:noGrow,fill:220px:noGrow,left:6dlu:noGrow,fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        readerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader"));
        mainPanel.add(readerPanel, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        chooseFileLabel = new JLabel();
        chooseFileLabel.setText("Select file:");
        readerPanel.add(chooseFileLabel, cc.xy(2, 2));
        bookPathText = new JTextField();
        readerPanel.add(bookPathText, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.DEFAULT));
        button2 = new JButton();
        button2.setText("...");
        readerPanel.add(button2, cc.xy(6, 2));
        label6 = new JLabel();
        label6.setText("Lines per page:");
        readerPanel.add(label6, cc.xy(2, 4));
        lineCount = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("1");
        defaultComboBoxModel3.addElement("2");
        defaultComboBoxModel3.addElement("3");
        lineCount.setModel(defaultComboBoxModel3);
        lineCount.setToolTipText("");
        readerPanel.add(lineCount, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        label7 = new JLabel();
        label7.setText("Line spacing:");
        readerPanel.add(label7, cc.xy(2, 6));
        lineSpace = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("0");
        defaultComboBoxModel4.addElement("1");
        defaultComboBoxModel4.addElement("2");
        lineSpace.setModel(defaultComboBoxModel4);
        lineSpace.setToolTipText("");
        readerPanel.add(lineSpace, cc.xy(4, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        fontsPanel = new JPanel();
        fontsPanel.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:max(60dlu;d):noGrow,left:4dlu:noGrow,fill:220px:noGrow,left:6dlu:noGrow,fill:max(d;4px):noGrow", "top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:6dlu:noGrow,center:d:noGrow,top:4dlu:noGrow"));
        fontsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Fonts"));
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
        fontPreview = new JEditorPane();
        fontPreview.setEditable(false);
        fontsPanel.add(fontPreview, cc.xyw(2, 6, 5, CellConstraints.FILL, CellConstraints.FILL));
        restoreDefaultButton = new JButton();
        restoreDefaultButton.setText("Restore default");
        fontsPanel.add(restoreDefaultButton, cc.xy(4, 8));
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
