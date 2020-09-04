package com.thief.idea;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainUi implements ToolWindowFactory, DumbAware {

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
    private Integer lineCount = Integer.parseInt(persistentState.getLineCount());

    /**
     * 读取行距设置
     **/
    private Integer lineSpace = Integer.parseInt(persistentState.getLineSpace());

    /**
     * 正文内容显示
     **/
    private JTextArea textArea;

    /**
     * 当前阅读页&跳页输入框
     **/
    private JTextField current;

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
    private String temp = "Stopping memory leak detection....";

    /**
     * 是否隐藏界面
     **/
    private boolean hide = false;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            JPanel panel = initPanel();
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(panel, "Thief-Book", false);
            toolWindow.getContentManager().addContent(content);

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
        panel.add(textArea, BorderLayout.CENTER);
        panel.add(initOperationPanel(), BorderLayout.EAST);
        return panel;
    }

    /**
     * 正文区域初始化
     **/
    private JTextArea initTextArea() {
        JTextArea textArea = new JTextArea();
        //初始化显示文字
        String welcome = "Memory leak detection has started....";
        textArea.setText(welcome);
        textArea.setOpaque(false);
        textArea.setTabSize(4);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(type, Font.PLAIN, Integer.parseInt(size)));
        textArea.setBorder(JBUI.Borders.empty(10, 30));
        return textArea;
    }

    /**
     * 初始化操作面板
     **/
    private JPanel initOperationPanel() {
        // 当前行
        current = initTextField();
        // 总行数
        total.setText("/" + (totalLine % lineCount == 0 ? totalLine / lineCount : totalLine / lineCount + 1));

        JPanel panelRight = new JPanel();
        panelRight.setBorder(JBUI.Borders.empty(0, 20));
        panelRight.setPreferredSize(new Dimension(280, 30));
        panelRight.add(current, BorderLayout.EAST);
        panelRight.add(total, BorderLayout.EAST);
        //加载按钮
        JButton fresh = initFreshButton();
        panelRight.add(fresh, BorderLayout.EAST);
        //上一页
        JButton up = initUpButton();
        panelRight.add(up, BorderLayout.EAST);
        //下一页
        JButton down = initDownButton();
        panelRight.add(down, BorderLayout.EAST);
        //老板键
        JButton boss = initBossButton(new JButton[]{fresh, up, down});
        panelRight.add(boss, BorderLayout.SOUTH);
        return panelRight;
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
                                currentPage = totalLine - 1;
                            }
                            countSeek();
                        }
                        textArea.setText(readBook());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        textArea.setText(e1.toString());
                    } catch (NumberFormatException e2) {
                        textArea.setText("请输入数字");
                    }

                }
            }
        });
        return current;
    }

    /**
     * 刷新按钮🔄
     **/
    private JButton initFreshButton() {
        JButton refresh = new JButton("\uD83D\uDD04");
        refresh.setPreferredSize(new Dimension(20, 20));
        refresh.setContentAreaFilled(false);
        refresh.setBorderPainted(false);
        refresh.addActionListener(e -> {
            try {
                persistentState = PersistentState.getInstanceForce();
                if (StringUtils.isEmpty(persistentState.getBookPathText()) || !bookFile.equals(persistentState.getBookPathText())) {
                    bookFile = persistentState.getBookPathText();
                    currentPage = 0;
                    seek = 0;
                    seekDictionary.clear();
                    if (StringUtils.isEmpty(bookFile)) {
                        totalLine = 0;
                        return;
                    }
                    totalLine = countLine();
                    countSeek();
                } else {
                    // 初始化当前行数
                    if (StringUtils.isNotEmpty(persistentState.getCurrentLine())) {
                        currentPage = Integer.parseInt(persistentState.getCurrentLine());
                    }
                    if (seekDictionary.size() <= 5 || totalLine == 0) {
                        totalLine = countLine();
                        countSeek();
                    }
                }
                type = persistentState.getFontType();
                size = persistentState.getFontSize();
                lineCount = Integer.parseInt(persistentState.getLineCount());
                lineSpace = Integer.parseInt(persistentState.getLineSpace());
                textArea.setText("已刷新");
                current.setText(" " + currentPage / lineCount);
                total.setText("/" + (totalLine % lineCount == 0 ? totalLine / lineCount : totalLine / lineCount + 1));
                textArea.setFont(new Font(type, Font.PLAIN, Integer.parseInt(size)));
            } catch (Exception newE) {
                newE.printStackTrace();
            }
        });
        return refresh;
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
            if (currentPage / lineCount > 1) {
                if (currentPage % lineCount == 0) {
                    currentPage = currentPage - lineCount * 2;
                } else {
                    while (currentPage % lineCount != 0) {
                        currentPage--;
                    }
                    currentPage -= lineCount;
                }
                try {
                    countSeek();
                    textArea.setText(readBook());
                    current.setText(" " + currentPage / lineCount);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        afterB.registerKeyboardAction(afterB.getActionListeners()[0],
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

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

            if (currentPage < totalLine) {
                try {
                    if (currentPage / lineCount <= 1) {
                        countSeek();
                    }
                    textArea.setText(readBook());
                    current.setText(" " + (currentPage % lineCount == 0 ? currentPage / lineCount : currentPage / lineCount + 1));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        });

        nextB.registerKeyboardAction(nextB.getActionListeners()[0],
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        return nextB;
    }

    /**
     * 隐藏按钮
     **/
    private JButton initBossButton(JButton[] buttons) {
        //老板键
        JButton bossB = new JButton(" ");
        bossB.setPreferredSize(new Dimension(5, 5));
        bossB.setContentAreaFilled(false);
        bossB.setBorderPainted(false);
        bossB.addActionListener(e -> {
            if (hide) {
                for (JButton b : buttons) {
                    b.setVisible(true);
                }
                current.setVisible(true);
                total.setVisible(true);
                textArea.setText(temp);
                hide = false;
            } else {
                for (JButton b : buttons) {
                    b.setVisible(false);
                }
                current.setVisible(false);
                total.setVisible(false);
                temp = textArea.getText();
                textArea.setText("Memory leak detection....");
                hide = true;
            }
        });
        bossB.registerKeyboardAction(bossB.getActionListeners()[0], KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        bossB.registerKeyboardAction(bossB.getActionListeners()[0], KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return bossB;
    }

    /**
     * 向下读取文件
     **/
    private String readBook() throws IOException {
        RandomAccessFile ra = null;
        StringBuilder str = new StringBuilder();
        StringBuilder nStr = new StringBuilder();
        try {
            ra = new RandomAccessFile(bookFile, "r");
            ra.seek(seek);
            for (int j = 0; j < lineSpace + 1; j++) {
                nStr.append("\n");
            }
            String temp;
            for (int i = 0; i < lineCount && (temp = ra.readLine()) != null; i++) {
                str.append(new String(temp.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)).append(nStr);
                currentPage++;
            }
            //实例化当前行数
            persistentState.setCurrentLine(String.valueOf(currentPage));
            seek = ra.getFilePointer();
            if (currentPage % cacheInterval == 0) {
                seekDictionary.put(currentPage, seek);
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
     **/
    private int countLine() throws IOException {
        try (RandomAccessFile ra = new RandomAccessFile(bookFile, "r")) {
            int i = 0;
            seekDictionary.put(0, ra.getFilePointer());
            while (ra.readLine() != null) {
                i++;
                if (i % cacheInterval == 0) {
                    seekDictionary.put(i, ra.getFilePointer());
                }
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
                ra = new RandomAccessFile(bookFile, "r");
                int line = 0;
                for (int i = 0; cacheInterval * i < currentPage; i++) {
                    line = cacheInterval * i;
                    ra.seek(seekDictionary.get(line));
                }
                while (ra.readLine() != null) {
                    line++;
                    if (line == currentPage) {
                        this.seek = ra.getFilePointer();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ra != null) {
                ra.close();
            }
        }
    }

}
