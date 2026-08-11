package com.thief.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@State(
        name = "PersistentState",
        storages = {@Storage(
                value = "thief-book.xml"
        )}
)
public class PersistentState implements PersistentStateComponent<Element> {

    /**
     * 阅读区字体为"系统默认"时跟随 IDE 默认字体（UIUtil.getLabelFont）
     **/
    public static final String DEFAULT_FONT = "系统默认";

    private String bookPathText;

    private String showFlag;

    private String fontSize;

    private String fontType;

    private String before;

    private String next;

    private String currentLine;

    private String lineCount;

    private String lineSpace;

    private String bossKey;

    /**
     * 全部书本：路径 -> 各自阅读进度（行号），顺序即设置页列表顺序。
     * 支持选择多本书并在阅读界面切换，每本书独立保存进度。
     **/
    private LinkedHashMap<String, String> bookMap = new LinkedHashMap<>();

    public PersistentState() {
    }

    public static PersistentState getInstance() {
        return ApplicationManager.getApplication().getService(PersistentState.class);
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
        element.setAttribute("bossKey",this.getBossKey());
        for (Map.Entry<String, String> entry : bookMap.entrySet()) {
            Element book = new Element("book");
            book.setAttribute("path", entry.getKey());
            book.setAttribute("line", entry.getValue());
            element.addContent(book);
        }

        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        bookMap.clear();
        this.setBookPathText(state.getAttributeValue("bookPath"));
        this.setShowFlag(state.getAttributeValue("showFlag"));
        this.setFontSize(state.getAttributeValue("fontSize"));
        this.setBefore(state.getAttributeValue("before"));
        this.setNext(state.getAttributeValue("next"));
        this.setCurrentLine(state.getAttributeValue("currentLine"));
        this.setFontType(state.getAttributeValue("fontType"));
        this.setLineCount(state.getAttributeValue("lineCount"));
        this.setLineSpace(state.getAttributeValue("lineSpace"));
        this.setBossKey(state.getAttributeValue("bossKey"));
        for (Element book : state.getChildren("book")) {
            String path = book.getAttributeValue("path");
            if (path == null || path.isEmpty()) {
                continue;
            }
            bookMap.put(path, book.getAttributeValue("line"));
        }
        // 兼容旧版配置：只有 bookPath 属性、没有 book 子元素时，导入为第一本书
        String legacy = this.getBookPathText();
        if (!legacy.isEmpty() && !bookMap.containsKey(legacy)) {
            bookMap.put(legacy, this.currentLine);
        }

    }

    @Override
    public void noStateLoaded() {

    }

    public String getBookPathText() {
        return (bookPathText == null || bookPathText.isEmpty()) ? "" : this.bookPathText;
    }

    public void setBookPathText(String bookPathText) {
        this.bookPathText = bookPathText;
        if (bookPathText != null && !bookPathText.isEmpty() && !bookMap.containsKey(bookPathText)) {
            bookMap.put(bookPathText, this.currentLine != null ? this.currentLine : "0");
        }
    }

    /**
     * 全部书本路径（按添加顺序）
     **/
    public List<String> getBookPathList() {
        return new ArrayList<>(bookMap.keySet());
    }

    /**
     * 设置全部书本：保留已存在的进度，活动书被移除时自动切到列表第一本
     **/
    public void setBookPathList(List<String> paths) {
        LinkedHashMap<String, String> newMap = new LinkedHashMap<>();
        for (String path : paths) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            String progress = bookMap.get(path);
            newMap.put(path, progress != null ? progress : "0");
        }
        bookMap = newMap;
        if (!bookMap.containsKey(getBookPathText())) {
            String first = bookMap.isEmpty() ? "" : bookMap.keySet().iterator().next();
            setBookPathText(first);
        }
    }

    /**
     * 获取某本书的阅读进度（行号）；活动书回退到历史 currentLine，未读过的书返回 0
     **/
    public String getCurrentLineFor(String bookPath) {
        String line = bookMap.get(bookPath);
        if (line != null) {
            return line;
        }
        if (Objects.equals(bookPath, getBookPathText())) {
            return getCurrentLine();
        }
        return "0";
    }

    /**
     * 保存某本书的阅读进度（行号）；活动书同步更新历史 currentLine 属性
     **/
    public void setCurrentLineFor(String bookPath, String line) {
        if (bookPath == null || bookPath.isEmpty()) {
            return;
        }
        bookMap.put(bookPath, line);
        if (Objects.equals(bookPath, getBookPathText())) {
            this.currentLine = line;
        }
    }

    public String getShowFlag() {
        return (showFlag == null || showFlag.isEmpty()) ? "0" : this.showFlag;
    }

    public void setShowFlag(String showFlag) {
        this.showFlag = showFlag;
    }

    public String getBefore() {
        return (before == null || before.isEmpty()) ? "Ctrl+1" : this.before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getNext() {
        return (next == null || next.isEmpty()) ? "Ctrl+2" : this.next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getCurrentLine() {
        return (currentLine == null || currentLine.isEmpty()) ? "0" : this.currentLine;
    }

    public void setCurrentLine(String currentLine) {
        this.currentLine = currentLine;
    }

    public String getFontSize() {
        return (fontSize == null || fontSize.isEmpty()) ? "14" : this.fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontType() {
        return (fontType == null || fontType.isEmpty()) ? DEFAULT_FONT : this.fontType;
    }

    public void setFontType(String fontType) {
        this.fontType = fontType;
    }
    public String getLineCount() {
        return this.lineCount = (lineCount == null || lineCount.isEmpty()) ? "1" : lineCount;
    }
    public void setLineCount(String lineCount) {
        this.lineCount = lineCount;
    }

    public String getLineSpace() {
        return this.lineSpace=(lineSpace == null || lineSpace.isEmpty()) ? "0" : lineSpace;
    }

    public void setLineSpace(String lineSpace) {
        this.lineSpace = lineSpace;
    }

    public String getBossKey() {
        return (bossKey == null || bossKey.isEmpty()) ? "Ctrl+3" : this.bossKey;
    }

    public void setBossKey(String bossKey) {
        this.bossKey = bossKey;
    }
}