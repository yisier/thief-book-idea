package com.thief.idea.tts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * macOS 离线 TTS：/usr/bin/say
 **/
public class MacTtsEngine extends CommandTtsEngine {

    @Override
    public boolean isSupported() {
        return new File("/usr/bin/say").canExecute();
    }

    @Override
    protected String[] buildCommand() {
        List<String> command = new ArrayList<>();
        command.add("/usr/bin/say");
        if (!voice.isEmpty()) {
            command.add("-v");
            command.add(voice);
        }
        command.add("-r");
        command.add(String.valueOf(wordsPerMinute()));
        return command.toArray(new String[0]);
    }
}
