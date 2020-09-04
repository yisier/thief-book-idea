package com.thief.idea;

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

    private String fontSize;

    private String fontType;

    private String before;

    private String next;

    private String currentLine;

    private String lineCount;

    private String lineSpace;

//    private String bossKey;



    public PersistentState() {
    }

    public static PersistentState getInstance() {
        if (persistentState == null) {
            persistentState = ServiceManager.getService(PersistentState.class);
        }
        return persistentState;
    }

    public static PersistentState getInstanceForce() {
        return ServiceManager.getService(PersistentState.class);
    }


    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("PersistentState");
        element.setAttribute("bookPath", this.getBookPathText());
        element.setAttribute("showFlag", this.getShowFlag());
        element.setAttribute("fontSize", this.getFontSize());
        element.setAttribute("before", this.getBefore());
        element.setAttribute("next", this.getNext());
        element.setAttribute("currentLine", this.getCurrentLine());
        element.setAttribute("fontType", this.getFontType());
        element.setAttribute("lineCount",this.getLineCount());
        element.setAttribute("lineSpace",this.getLineSpace());
//        element.setAttribute("bossKey",this.getBossKey());

        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        this.setBookPathText(state.getAttributeValue("bookPath"));
        this.setShowFlag(state.getAttributeValue("showFlag"));
        this.setFontSize(state.getAttributeValue("fontSize"));
        this.setBefore(state.getAttributeValue("before"));
        this.setNext(state.getAttributeValue("next"));
        this.setCurrentLine(state.getAttributeValue("currentLine"));
        this.setFontType(state.getAttributeValue("fontType"));
        this.setLineCount(state.getAttributeValue("lineCount"));
        this.setLineSpace(state.getAttributeValue("lineSpace"));
//        this.setLineSpace(state.getAttributeValue("bossKey"));

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

    public String getFontSize() {
        return StringUtils.isEmpty(fontSize) ? "14" : this.fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontType() {
        return StringUtils.isEmpty(fontType) ? "Microsoft JhengHei" : this.fontType;
    }

    public void setFontType(String fontType) {
        this.fontType = fontType;
    }
    public String getLineCount() {
        return this.lineCount =StringUtils.isEmpty(lineCount) ? "1" : lineCount;
    }
    public void setLineCount(String lineCount) {
        this.lineCount = lineCount;
    }

    public String getLineSpace() {
        return this.lineSpace=StringUtils.isEmpty(lineSpace) ? "0" : lineSpace;
    }

    public void setLineSpace(String lineSpace) {
        this.lineSpace = lineSpace;
    }

//    public String getBossKey() {
//        return StringUtils.isEmpty(bossKey) ? "Ctrl + Shift + ↓" : this.bossKey;
//    }
//
//    public void setBossKey(String bossKey) {
//        this.bossKey = bossKey;
//    }
}