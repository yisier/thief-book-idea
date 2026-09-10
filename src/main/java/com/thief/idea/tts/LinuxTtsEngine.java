package com.thief.idea.tts;

import java.util.ArrayList;
import java.util.List;

/**
 * Linux 离线 TTS：优先 espeak-ng，其次 espeak。
 **/
public class LinuxTtsEngine extends CommandTtsEngine {

    private final String executable;

    public LinuxTtsEngine() {
        this.executable = commandExists("espeak-ng") ? "espeak-ng"
                : (commandExists("espeak") ? "espeak" : "espeak-ng");
    }

    @Override
    public boolean isSupported() {
        return commandExists("espeak-ng") || commandExists("espeak");
    }

    @Override
    protected String[] buildCommand() {
        List<String> command = new ArrayList<>();
        command.add(executable);
        // 未指定语音时默认中文（cmn）
        command.add("-v");
        command.add(voice.isEmpty() ? "cmn" : voice);
        command.add("-s");
        command.add(String.valueOf(wordsPerMinute()));
        return command.toArray(new String[0]);
    }

    private static boolean commandExists(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (new java.io.File(dir, name).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
