package com.thief.idea.tts;

import org.jetbrains.annotations.Nullable;

/**
 * 离线 TTS 引擎抽象。
 * 各平台实现：Windows 走 SAPI COM（JNA），macOS 走 say，Linux 走 espeak-ng。
 * 约定：speak() 在调用线程阻塞至朗读结束或被 stop() 打断，返回是否正常读完；
 * pause()/resume()/stop() 由其他线程调用，内部只置标志，实际 COM/进程操作在 speak 线程完成。
 **/
public interface TtsEngine {

    /**
     * 当前平台是否支持该引擎（如 Windows 是否装有语音）
     **/
    boolean isSupported();

    /**
     * 可用语音显示名列表，用于设置下拉；不支持时返回空数组
     **/
    String[] getAvailableVoices();

    /**
     * 选择语音（显示名），空串表示系统默认；引擎在下次 speak 时应用
     **/
    void setVoice(@Nullable String voice);

    /**
     * 设置语速倍率（0.5 ~ 3.0，1.0 为正常），引擎在下次 speak 时应用
     **/
    void setRate(double rate);

    /**
     * 阻塞朗读一段文本，读完返回 true；被 stop() 打断返回 false
     **/
    boolean speak(String text);

    /**
     * 打断当前朗读并清空后续
     **/
    void stop();

    /**
     * 暂停朗读
     **/
    void pause();

    /**
     * 从暂停处继续
     **/
    void resume();

    /**
     * 释放底层资源
     **/
    default void dispose() {
    }
}
