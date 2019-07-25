package com.thief_book.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
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
import java.io.IOException;
import java.io.RandomAccessFile;

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

    //
    long seek = 0;

    //设置Timer定时器，并启动
    Timer timer = new Timer(autoNextSecond * 1000, this);

    //总行数
    int totalLine = 0;

    //当前行
    int currentLine = 0;

    // 开启自动翻页
    boolean off = false;

    private String welcome = "Memory leak detection has started....";

    private String s = "自动翻页";
    private String st = "暂停";
    JButton action = new JButton(s);

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

            panel.add(textArea);

            JPanel panelRight = new JPanel();
            // 当前行
            current = new JTextField("current line:");
            current.setText(currentLine + "");
            current.addKeyListener(this);      //键盘事件


            // 总行数
            total.setText("/" + totalLine);

            panelRight.add(current, BorderLayout.NORTH);
            panelRight.add(total, BorderLayout.NORTH);

            // 开始结束按钮
            action.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // stop
                    if (off) {
                        action.setText(s);
                        timer.stop();
                        off = false;
                    } else {        // start
                        action.setText(st);
                        timer.start();
                        off = true;
                    }

                }
            });
            panelRight.add(action, BorderLayout.EAST);

            //上一页
            JButton afterB = new JButton("上页");
            afterB.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
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

                    // 页数小于等于1，什么都不做

                }
            });

            panelRight.add(afterB, BorderLayout.EAST);

            //下一页
            JButton nextB = new JButton("下页");

            nextB.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

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

                }
            });

            action.registerKeyboardAction(action.getActionListeners()[0], KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.ALT_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
            afterB.registerKeyboardAction(afterB.getActionListeners()[0], KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
            nextB.registerKeyboardAction(nextB.getActionListeners()[0], KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.ALT_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

            panelRight.add(nextB, BorderLayout.EAST);


            panel.add(panelRight, BorderLayout.EAST);

            // 界面加载完先设置标语
            textArea.setText(welcome);

            if ("1".equals(persistentState.getShowFlag())) {
//                action.hide();
                afterB.hide();
                nextB.hide();
            }

            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(panel, "Control", false);
            toolWindow.getContentManager().addContent(content);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public String readBook() throws IOException {
        RandomAccessFile ra = null;
        String str = "";
        try {
            ra = new RandomAccessFile(bookFile, "r");
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


    // 查询总行数
    public void countLine() throws IOException {
        RandomAccessFile ra = null;

        try {
            ra = new RandomAccessFile(bookFile, "r");

            String str = "";
            while ((str = ra.readLine()) != null) {
                totalLine++;
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ra.close();
        }
    }


    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {


        //行数跳转
        if (keyEvent.getSource() == current) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) { //判断按下的键是否是回车键
                try {
                    String input = current.getText();
                    String inputCurrent = input.split("/")[0].trim();
                    System.out.println(inputCurrent);

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
    public void keyReleased(KeyEvent keyEvent) {

    }


    public void countSeek() throws IOException {

        RandomAccessFile ra = null;

        int line = 0;
        try {
            ra = new RandomAccessFile(bookFile, "r");

            String str = "";
            while ((str = ra.readLine()) != null) {
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

}
