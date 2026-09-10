package com.thief.idea.tts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TTS 引擎工厂与平台探测。
 **/
public final class TtsEngines {

    private TtsEngines() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /**
     * 创建当前平台的引擎。Windows SAPI 引擎须在朗读线程上创建（内部 CoInitialize）。
     **/
    public static TtsEngine create() {
        if (isWindows()) {
            return new WindowsSapiEngine();
        }
        if (isMac()) {
            return new MacTtsEngine();
        }
        return new LinuxTtsEngine();
    }

    /**
     * 当前平台是否可用离线 TTS
     **/
    public static boolean isSupported() {
        if (isWindows()) {
            return true;
        }
        if (isMac()) {
            return new MacTtsEngine().isSupported();
        }
        return new LinuxTtsEngine().isSupported();
    }

    /**
     * 按平台枚举可用语音，供设置下拉使用
     **/
    public static String[] listVoices() {
        if (isWindows()) {
            return listWindowsVoices();
        }
        if (isMac()) {
            return listMacVoices();
        }
        return new String[0];
    }

    /**
     * 在临时线程上枚举 Windows SAPI 语音（COM STA 要求），供设置下拉使用。
     **/
    public static String[] listWindowsVoices() {
        if (!isWindows()) {
            return new String[0];
        }
        final List<String> result = new ArrayList<>();
        Thread thread = new Thread(() -> {
            SapiVoice voice = null;
            try {
                com.sun.jna.platform.win32.Ole32.INSTANCE.CoInitializeEx(
                        null, com.sun.jna.platform.win32.Ole32.COINIT_APARTMENTTHREADED);
                voice = new SapiVoice();
                result.addAll(voice.listVoiceDescriptions());
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (voice != null) {
                    voice.releaseQuietly();
                }
                com.sun.jna.platform.win32.Ole32.INSTANCE.CoUninitialize();
            }
        }, "thief-book-tts-voices");
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.toArray(new String[0]);
    }

    /**
     * 枚举 macOS 语音：解析 `say -v ?` 输出，每行首列为语音名
     **/
    public static String[] listMacVoices() {
        List<String> result = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("/usr/bin/say", "-v", "?")
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    result.add(trimmed.split("\\s+")[0]);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toArray(new String[0]);
    }
}
