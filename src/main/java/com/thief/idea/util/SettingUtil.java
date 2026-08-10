package com.thief.idea.util;


import com.thief.idea.PersistentState;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class SettingUtil {


    public static PersistentState getPersistentState(){
        return PersistentState.getInstance();
    }

    /**
     * 系统字体列表变更极少，缓存一次后复用，避免每次打开设置页都重新枚举全部字体
     **/
    private static List<String> cachedFontTypes;

    public static List<String> getAllFontType() {
        if (cachedFontTypes == null) {
            Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            java.util.Set<String> dedup = new LinkedHashSet<>();
            for (Font font : allFonts) {
                dedup.add(font.getFamily());
            }
            cachedFontTypes = new ArrayList<>(dedup);
        }
        return Collections.unmodifiableList(cachedFontTypes);
    }
}
