package com.thief_book.idea;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;

import java.io.IOException;
import java.io.RandomAccessFile;

public class test extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here

        if (e.getProject() != null) {
            // 将项目对象，ToolWindow的id传入，获取控件对象
            ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow("thief-book");
            if (toolWindow != null) {
                // 无论当前状态为关闭/打开，进行强制打开ToolWindow
                toolWindow.show(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }


    }
}