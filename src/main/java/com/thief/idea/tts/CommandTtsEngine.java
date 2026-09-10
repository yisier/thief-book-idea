package com.thief.idea.tts;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * 基于系统命令行的离线 TTS 引擎基类（macOS 的 say、Linux 的 espeak-ng）。
 * 文本经 stdin（UTF-8）传入，避免超长单行超出系统命令参数长度上限。
 * pause/resume 不支持（no-op），stop 通过销毁进程实现。
 **/
public abstract class CommandTtsEngine implements TtsEngine {

    protected volatile String voice = "";
    protected volatile double rate = 1.0;
    protected volatile boolean stopRequested = false;

    private volatile Process process;

    /**
     * 按当前语速/语音构造命令行（不含正文，正文走 stdin）
     **/
    protected abstract String[] buildCommand();

    @Override
    public void setVoice(@Nullable String voice) {
        this.voice = voice == null ? "" : voice;
    }

    @Override
    public void setRate(double rate) {
        this.rate = rate;
    }

    @Override
    public String[] getAvailableVoices() {
        return new String[0];
    }

    @Override
    public boolean speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        stopRequested = false;
        Process running = null;
        try {
            running = new ProcessBuilder(buildCommand()).redirectErrorStream(true).start();
            process = running;
            try (OutputStreamWriter writer = new OutputStreamWriter(running.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(text);
                writer.flush();
            }
            running.waitFor();
            return !stopRequested;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            process = null;
            if (running != null && running.isAlive()) {
                running.destroy();
            }
        }
    }

    @Override
    public void stop() {
        stopRequested = true;
        Process running = process;
        if (running != null) {
            running.destroy();
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    /**
     * 倍率换算为每分钟词数（正常约 175 词/分）
     **/
    protected int wordsPerMinute() {
        int wpm = (int) Math.round(175 * rate);
        return Math.max(80, Math.min(450, wpm));
    }
}
