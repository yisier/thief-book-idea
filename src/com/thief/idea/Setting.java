package com.thief.idea;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.thief.idea.ui.SettingUi;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class Setting implements SearchableConfigurable {


    private SettingUi settingUi;

    private PersistentState persistentState = PersistentState.getInstance();


    @SuppressWarnings("FieldCanBeLocal")
    private final Project project;


    public Setting(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "thief.id";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "thief-book-config";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        if (settingUi == null) {
            settingUi = new SettingUi();
        }
        settingUi.innit(persistentState);

        return settingUi.mainPanel;

    }

    @Override
    public boolean isModified() {
        return !StringUtils.equals(persistentState.getBookPathText(), settingUi.bookPathText.getText())
                || !StringUtils.equals(persistentState.getFontSize(), settingUi.fontSize.getSelectedItem().toString())
                || !StringUtils.equals(persistentState.getBefore(), settingUi.before.getText())
                || !StringUtils.equals(persistentState.getNext(), settingUi.next.getText())
                || !StringUtils.equals(persistentState.getLineCount(), settingUi.lineCount.getSelectedItem().toString())
                || !StringUtils.equals(persistentState.getLineSpace(), settingUi.lineSpace.getSelectedItem().toString())
                || !StringUtils.equals(persistentState.getFontType(), settingUi.fontType.getSelectedItem().toString());

    }

    @Override
    public void apply() {
        persistentState.setBookPathText(settingUi.bookPathText.getText());
        persistentState.setFontSize(settingUi.fontSize.getSelectedItem().toString());
        persistentState.setBefore(settingUi.before.getText());
        persistentState.setNext(settingUi.next.getText());
        persistentState.setLineCount(settingUi.lineCount.getSelectedItem().toString());
        persistentState.setFontType(settingUi.fontType.getSelectedItem().toString());
        persistentState.setLineSpace(settingUi.lineSpace.getSelectedItem().toString());

    }

    @Override
    public void reset() {
//        settingUi.bookPathText.setText("");
//        settingUi.showFlag.setSelected(false);
//        settingUi.fontSize.setSelectedItem("5");
//        settingUi.before.setText("");
//        settingUi.next.setText("");
    }

    @Override
    public void disposeUIResources() {

    }
}