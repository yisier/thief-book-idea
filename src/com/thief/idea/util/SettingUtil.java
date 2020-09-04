package com.thief.idea.util;


import com.thief.idea.PersistentState;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SettingUtil {


    public static PersistentState getPersistentState(){
        return PersistentState.getInstance();
    }

    public static List<String> getAllFontType() {
        Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        List<String> fontList = new LinkedList<>();
        for (Font font : allFonts) {
            fontList.add(font.getFamily());
        }
        fontList = removeDuplicate(fontList);
        return fontList;
    }

    private static List<String> removeDuplicate(List<String> fontList) {
        HashSet<String> temp = new LinkedHashSet<>(fontList);
        return new LinkedList<>(temp);
    }
}
