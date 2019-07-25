package com.thief_book.idea;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@State(
        name = "PersistentState",
        storages = {@Storage(
                value = "thief-book.xml"
        )}
)
public class PersistentState implements PersistentStateComponent<Element> {

    private static PersistentState persistentState;

    private String bookPathText;

    private String showFlag;

    private String autoNextSecond;

    private String before;

    private String next;

    private String currentLine;

    private String autoKeymap;

    public PersistentState() {
    }

    public static PersistentState getInstance() {
        if (persistentState == null) {
            persistentState = ServiceManager.getService(PersistentState.class);
        }

        return persistentState;
    }


    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("PersistentState");
        element.setAttribute("bookPath", this.getBookPathText());
        element.setAttribute("showFlag", this.getShowFlag());
        element.setAttribute("autoNextSecond", this.getAutoNextSecond());
        element.setAttribute("before", this.getBefore());
        element.setAttribute("next", this.getNext());
        element.setAttribute("currentLine", this.getCurrentLine());
        element.setAttribute("autoKeymap", this.getAutoKeymap());
        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        this.setBookPathText(state.getAttributeValue("bookPath"));
        this.setShowFlag(state.getAttributeValue("showFlag"));
        this.setAutoNextSecond(state.getAttributeValue("autoNextSecond"));
        this.setBefore(state.getAttributeValue("before"));
        this.setNext(state.getAttributeValue("next"));
        this.setCurrentLine(state.getAttributeValue("currentLine"));
        this.setAutoKeymap(state.getAttributeValue("autoKeymap"));

    }

    @Override
    public void noStateLoaded() {

    }

    public String getBookPathText() {
        return bookPathText;
    }

    public void setBookPathText(String bookPathText) {
        this.bookPathText = bookPathText;
    }

    public String getShowFlag() {
        return StringUtils.isEmpty(showFlag) ? "0" : this.showFlag;
    }

    public void setShowFlag(String showFlag) {
        this.showFlag = showFlag;
    }

    public String getAutoNextSecond() {
        return StringUtils.isEmpty(autoNextSecond) ? "5" : this.autoNextSecond;
    }

    public void setAutoNextSecond(String autoNextSecond) {
        this.autoNextSecond = autoNextSecond;
    }

    public String getBefore() {
        return StringUtils.isEmpty(before) ? "Alt + ←" : this.before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getNext() {
        return StringUtils.isEmpty(next) ? "Alt + →" : this.next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getCurrentLine() {
        return StringUtils.isEmpty(currentLine) ? "0" : this.currentLine;
    }

    public void setCurrentLine(String currentLine) {
        this.currentLine = currentLine;
    }

    public String getAutoKeymap() {
        return StringUtils.isEmpty(autoKeymap) ? "Alt + ↑" : this.autoKeymap;
    }

    public void setAutoKeymap(String autoKeymap) {
        this.autoKeymap = autoKeymap;
    }
}
