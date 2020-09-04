package com.thief.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author <a href="mailto:yongpeng.zheng@vtradex.com">yongpeng.zheng</a>
 * @since 2020-09-04 15:57
 */
public class TestUi implements ToolWindowFactory{
    @Override
    public boolean isApplicable(@NotNull Project project) {
        return false;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

    }

}
