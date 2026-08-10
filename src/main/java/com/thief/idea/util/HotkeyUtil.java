package com.thief.idea.util;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * 热键文本与 KeyStroke 的互相转换。
 * 存储/显示格式："Ctrl+Shift+↓"，解析时兼容 "Alt + ←" 等带空格写法，非法文本返回 null。
 **/
public final class HotkeyUtil {

    /**
     * 是否正在设置页编辑热键：热键输入框获得焦点时置为 true，
     * 全局老板键监听器据此跳过，避免设置热键时误触发
     **/
    public static volatile boolean editingHotkey = false;

    private HotkeyUtil() {
    }

    /**
     * 把按键码与修饰键格式化为显示/存储文本，如 "Ctrl+Shift+↓"
     **/
    public static String format(int keyCode, int modifiersEx) {
        StringBuilder sb = new StringBuilder();
        if ((modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiersEx & InputEvent.META_DOWN_MASK) != 0) {
            sb.append("Meta+");
        }
        sb.append(keyText(keyCode));
        return sb.toString();
    }

    /**
     * 解析 "Ctrl+Shift+↓"（允许带空格，如 "Alt + ←"）为 KeyStroke，非法输入返回 null
     **/
    public static KeyStroke parse(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.trim().split("\\s*\\+\\s*");
        int modifiers = 0;
        int keyCode = 0;
        for (String part : parts) {
            String p = part.trim().toLowerCase(Locale.ROOT);
            switch (p) {
                case "ctrl":
                case "control":
                    modifiers |= InputEvent.CTRL_DOWN_MASK;
                    break;
                case "alt":
                case "option":
                    modifiers |= InputEvent.ALT_DOWN_MASK;
                    break;
                case "shift":
                    modifiers |= InputEvent.SHIFT_DOWN_MASK;
                    break;
                case "meta":
                case "cmd":
                case "win":
                    modifiers |= InputEvent.META_DOWN_MASK;
                    break;
                default:
                    keyCode = parseKey(p);
            }
        }
        if (keyCode == 0) {
            return null;
        }
        return KeyStroke.getKeyStroke(keyCode, modifiers);
    }

    private static int parseKey(String name) {
        if (name.isEmpty()) {
            return 0;
        }
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (Character.isLetterOrDigit(c)) {
                return KeyEvent.getExtendedKeyCodeForChar(c);
            }
        }
        switch (name) {
            case "↓":
            case "down":
                return KeyEvent.VK_DOWN;
            case "↑":
            case "up":
                return KeyEvent.VK_UP;
            case "←":
            case "left":
                return KeyEvent.VK_LEFT;
            case "→":
            case "right":
                return KeyEvent.VK_RIGHT;
            case "enter":
                return KeyEvent.VK_ENTER;
            case "tab":
                return KeyEvent.VK_TAB;
            case "space":
                return KeyEvent.VK_SPACE;
            case "backspace":
                return KeyEvent.VK_BACK_SPACE;
            case "delete":
            case "del":
                return KeyEvent.VK_DELETE;
            case "esc":
                return KeyEvent.VK_ESCAPE;
            case "home":
                return KeyEvent.VK_HOME;
            case "end":
                return KeyEvent.VK_END;
            case "pageup":
                return KeyEvent.VK_PAGE_UP;
            case "pagedown":
                return KeyEvent.VK_PAGE_DOWN;
            case "insert":
            case "ins":
                return KeyEvent.VK_INSERT;
            default:
                break;
        }
        // F1 ~ F24
        if (name.startsWith("f") && name.length() <= 3) {
            try {
                int n = Integer.parseInt(name.substring(1));
                if (n >= 1 && n <= 24) {
                    return KeyEvent.VK_F1 + n - 1;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static String keyText(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_DOWN:
                return "↓";
            case KeyEvent.VK_UP:
                return "↑";
            case KeyEvent.VK_LEFT:
                return "←";
            case KeyEvent.VK_RIGHT:
                return "→";
            case KeyEvent.VK_ENTER:
                return "Enter";
            case KeyEvent.VK_TAB:
                return "Tab";
            case KeyEvent.VK_SPACE:
                return "Space";
            case KeyEvent.VK_ESCAPE:
                return "Esc";
            case KeyEvent.VK_BACK_SPACE:
                return "Backspace";
            case KeyEvent.VK_DELETE:
                return "Delete";
            case KeyEvent.VK_HOME:
                return "Home";
            case KeyEvent.VK_END:
                return "End";
            case KeyEvent.VK_PAGE_UP:
                return "PageUp";
            case KeyEvent.VK_PAGE_DOWN:
                return "PageDown";
            case KeyEvent.VK_INSERT:
                return "Insert";
            default:
                return KeyEvent.getKeyText(keyCode);
        }
    }
}
