package com.thief.idea.tts;

import com.sun.jna.platform.win32.Ole32;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Windows 离线 TTS 引擎：JNA 调用 SAPI 的 SpVoice 自动化对象。
 * 必须在 TtsService 的朗读线程上构造与使用（COM STA）；pause/resume/stop 由其他线程只置标志。
 **/
public class WindowsSapiEngine implements TtsEngine {

    private final SapiVoice voice;

    private volatile String desiredVoice = "";
    private volatile double desiredRate = 1.0;

    private volatile boolean paused = false;
    private volatile boolean stopRequested = false;

    /**
     * 已实际应用到 SAPI 的配置，避免每次 speak 重复枚举语音
     **/
    private String appliedVoice = null;
    private double appliedRate = Double.NaN;

    public WindowsSapiEngine() {
        Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
        this.voice = new SapiVoice();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String[] getAvailableVoices() {
        return TtsEngines.listWindowsVoices();
    }

    @Override
    public void setVoice(@Nullable String voice) {
        this.desiredVoice = voice == null ? "" : voice;
    }

    @Override
    public void setRate(double rate) {
        this.desiredRate = rate;
    }

    private void applyPendingConfig() {
        if (!Objects.equals(appliedVoice, desiredVoice)) {
            if (voice.setVoiceByName(desiredVoice)) {
                appliedVoice = desiredVoice;
            }
        }
        if (appliedRate != desiredRate) {
            voice.applyRate(desiredRate);
            appliedRate = desiredRate;
        }
    }

    @Override
    public boolean speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        stopRequested = false;
        applyPendingConfig();
        voice.speakAsync(text);

        // 等待朗读真正开始（SAPI 异步启动有几十毫秒延迟），避免误判读完
        boolean started = false;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (stopRequested) {
                voice.purge();
                return false;
            }
            int state = voice.runningState();
            if (state == SapiVoice.RS_SPEAKING) {
                started = true;
                break;
            }
            // 极短文本可能在启动前就已读完
            if (state == SapiVoice.RS_DONE && System.currentTimeMillis() - start > 250) {
                return true;
            }
            sleep(30);
        }
        // 迟迟未进入朗读状态（如无音频设备），结束本次朗读避免死循环
        if (!started) {
            voice.purge();
            return false;
        }

        boolean sapiPaused = false;
        while (true) {
            if (stopRequested) {
                voice.purge();
                return false;
            }
            if (paused && !sapiPaused) {
                voice.pause();
                sapiPaused = true;
            } else if (!paused && sapiPaused) {
                voice.resume();
                sapiPaused = false;
            }
            // 语速实时生效：轮询期间发现变化就应用到正在朗读的语音
            if (appliedRate != desiredRate) {
                voice.applyRate(desiredRate);
                appliedRate = desiredRate;
            }
            voice.waitUntilDone(150);
            if (voice.runningState() == SapiVoice.RS_DONE) {
                return true;
            }
            sleep(10);
        }
    }

    @Override
    public void stop() {
        stopRequested = true;
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void dispose() {
        try {
            voice.releaseQuietly();
        } finally {
            Ole32.INSTANCE.CoUninitialize();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
