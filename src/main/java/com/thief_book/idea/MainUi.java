package com.thief_book.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MainUi implements ToolWindowFactory, ActionListener, KeyListener {

    private PersistentState persistentState = PersistentState.getInstance();

    private JPanel panel;

    // 文件路径
    private String bookFile = persistentState.getBookPathText();

    //自动翻页秒数
    int autoNextSecond = Integer.parseInt(persistentState.getAutoNextSecond());

    JTextArea textArea;

    JTextField current;
    JLabel total = new JLabel();

    long seek = 0;

    //设置Timer定时器，并启动
    Timer timer = new Timer(autoNextSecond * 1000, this);

    //总行数
    int totalLine = 0;

    //当前行
    int currentLine = 0;

    // 开启自动翻页
    boolean off = false;

    public static String welcome = "Memory leak detection has started....";

    private String s = "Auto";
    private String st = "Stop";

    ImageIcon nextIcon = new ImageIcon(toByteArray(this.getClass().getResourceAsStream("/icons/mainUI/right-circle.png")));
    ImageIcon backIcon = new ImageIcon(toByteArray(this.getClass().getResourceAsStream("/icons/mainUI/left-circle.png")));
    ImageIcon playIcon = new ImageIcon(toByteArray(this.getClass().getResourceAsStream("/icons/mainUI/play-circle.png")));
    ImageIcon stopIcon = new ImageIcon(toByteArray(this.getClass().getResourceAsStream("/icons/mainUI/timeout.png")));
    ImageIcon searchIcon = new ImageIcon(toByteArray(this.getClass().getResourceAsStream("/icons/mainUI/search.png")));

    JButton action = new JButton(playIcon);

    public MainUi() throws IOException {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            // 初始化当前行数
            if (StringUtils.isNotEmpty(persistentState.getCurrentLine())) {
                currentLine = Integer.parseInt(persistentState.getCurrentLine());
            }

            // 查询当前行数seek值
            this.countSeek();

            //查询总行数
            this.countLine();

            // 初始化页面
            panel = initPanel(panel);

            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(panel, "Control", false);
            toolWindow.getContentManager().addContent(content);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化页面
     * @param panel
     */
    private JPanel initPanel(JPanel panel) {
        panel = new JPanel();

        textArea = new JTextArea();
        textArea.append("");
        textArea.setOpaque(false);
        textArea.setRows(1);
        textArea.setColumns(130);
        textArea.setTabSize(14);
        textArea.setEditable(false);
        textArea.setLineWrap(true);        //激活自动换行功能
        textArea.setWrapStyleWord(true);    // 激活断行不断字功能

        panel.add(textArea, BorderLayout.WEST);

        JPanel panelRight = new JPanel();
        // 当前行
        current = new JTextField("current line:");
        current.setText(currentLine + "");
        current.addKeyListener(this);      //键盘事件

        // 总行数
        total.setText("/" + totalLine);



        // 开始结束按钮
        action.addActionListener(e -> {
            if (off) {
//                action.setText(s);
                action.setIcon(playIcon);
                timer.stop();
                off = false;
            } else {        // start
//                action.setText(st);
                action.setIcon(stopIcon);
                timer.start();
                off = true;
            }
        });

        //上一页
        JButton afterB = new JButton(backIcon);
        afterB.addActionListener(e -> {
            if (currentLine > 1) {
                currentLine = currentLine - 2;
                try {
                    countSeek();
                    textArea.setText(readBook());
                    current.setText(" " + currentLine);
                    total.setText("/" + totalLine);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        //下一页
        JButton nextB = new JButton(nextIcon);
        nextB.addActionListener(e -> {
            if (currentLine == totalLine) {
                //什么都不做
            } else {
                try {
                    if (currentLine <= 1) {
                        countSeek();
                    }
                    textArea.setText(readBook());
                    current.setText(" " + currentLine);
                    total.setText("/" + totalLine);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        action.registerKeyboardAction(action.getActionListeners()[0],
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.ALT_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        afterB.registerKeyboardAction(afterB.getActionListeners()[0],
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        nextB.registerKeyboardAction(nextB.getActionListeners()[0],
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.ALT_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 设置大小
        int size = 22;
        action.setPreferredSize(new Dimension(size, size));
        afterB.setPreferredSize(new Dimension(size, size));
        nextB.setPreferredSize(new Dimension(size, size));

        // 按钮加入panel
        panelRight.add(current, BorderLayout.NORTH);
        panelRight.add(total, BorderLayout.NORTH);
        panelRight.add(action, BorderLayout.EAST);
        panelRight.add(afterB, BorderLayout.EAST);
        panelRight.add(nextB, BorderLayout.EAST);

        panel.add(panelRight, BorderLayout.EAST);

        // 界面加载完先设置标语
        textArea.setText(welcome);

        if ("1".equals(persistentState.getShowFlag())) {
            afterB.hide();
            nextB.hide();
        }


        return panel;
    }


    /**
     * 读取文件
     * @return
     * @throws IOException
     */
    public String readBook() throws IOException {
        RandomAccessFile ra = null;
        String str = "";
        try {
            ra = new BufferedRandomAccessFile(bookFile, "r");
            ra.seek(seek);

            str = new String(ra.readLine().getBytes("ISO-8859-1"), "gbk");
            currentLine++;
            //实例化当前行数
            persistentState.setCurrentLine(String.valueOf(currentLine));
            seek = ra.getFilePointer();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ra.close();
        }
        return str;
    }

    //定时事件
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            if (currentLine == totalLine) {
                action.setText(s);
                timer.stop();
            } else {
                textArea.setText(readBook());
                current.setText(" " + currentLine);
                total.setText("/" + totalLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 查询总行数
     * @throws IOException
     */
    public void countLine() throws IOException {
        RandomAccessFile ra = null;
        try {
            ra = new BufferedRandomAccessFile(bookFile, "r");
            while (ra.readLine() != null) {
                totalLine++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ra.close();
        }
    }


    @Override
    public void keyTyped(KeyEvent keyEvent) { }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        //行数跳转
        if (keyEvent.getSource() == current) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) { //判断按下的键是否是回车键
                try {
                    String input = current.getText();
                    String inputCurrent = input.split("/")[0].trim();

                    int i = Integer.parseInt(inputCurrent);
                    if (i <= 1) {
                        seek = 0;
                        currentLine = 0;
                    } else {
                        currentLine = i - 1;
                        countSeek();
                    }
                    textArea.setText(readBook());
                } catch (IOException e) {
                    e.printStackTrace();
                    textArea.setText(e.toString());
                }

            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) { }


    public void countSeek() throws IOException {

        RandomAccessFile ra = null;

        int line = 0;
        try {
            ra = new BufferedRandomAccessFile(bookFile, "r");

            while (ra.readLine() != null) {
                line++;
                if (line == currentLine) {
                    this.seek = ra.getFilePointer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ra.close();
        }
    }

    /**
     * inputstream 转 byte[]
     * @param input
     * @return
     * @throws IOException
     */
    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
