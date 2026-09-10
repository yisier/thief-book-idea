package com.thief.idea.tts;

/**
 * 朗读调度：后台线程逐页取文本交给引擎朗读，读完自动取下一页，直到结尾或被停止。
 * 页文本由调用方（MainUi）通过 PageSource 提供，取下一页时同时负责界面翻页。
 **/
public class TtsService {

    /**
     * 逐页提供待朗读文本；返回 null 表示已到书末，朗读结束
     **/
    public interface PageSource {
        String nextPage();
    }

    private final PageSource source;
    private final Runnable onStateChanged;

    private volatile TtsEngine engine;
    private volatile String desiredVoice = "";
    private volatile double desiredRate = 1.0;
    private volatile boolean stopRequested = false;
    private volatile boolean paused = false;
    private volatile boolean running = false;

    private Thread worker;

    public TtsService(PageSource source, Runnable onStateChanged) {
        this.source = source;
        this.onStateChanged = onStateChanged;
    }

    /**
     * 当前平台是否支持离线朗读
     **/
    public boolean isSupported() {
        return TtsEngines.isSupported();
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        stopRequested = false;
        paused = false;
        running = true;
        worker = new Thread(this::runLoop, "thief-book-tts");
        worker.setDaemon(true);
        worker.start();
        fireStateChanged();
    }

    public void stop() {
        stopRequested = true;
        TtsEngine current = engine;
        if (current != null) {
            current.stop();
        }
    }

    public void pause() {
        paused = true;
        TtsEngine current = engine;
        if (current != null) {
            current.pause();
        }
    }

    public void resume() {
        paused = false;
        TtsEngine current = engine;
        if (current != null) {
            current.resume();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setVoice(String voice) {
        this.desiredVoice = voice == null ? "" : voice;
        TtsEngine current = engine;
        if (current != null) {
            current.setVoice(desiredVoice);
        }
    }

    public void setRate(double rate) {
        this.desiredRate = rate;
        TtsEngine current = engine;
        if (current != null) {
            current.setRate(rate);
        }
    }

    private void runLoop() {
        try {
            engine = TtsEngines.create();
            engine.setVoice(desiredVoice);
            engine.setRate(desiredRate);
            if (!engine.isSupported()) {
                return;
            }
            while (!stopRequested) {
                String page = source.nextPage();
                if (page == null) {
                    break;
                }
                if (page.trim().isEmpty()) {
                    continue;
                }
                if (!engine.speak(page)) {
                    break;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            TtsEngine current = engine;
            if (current != null) {
                current.dispose();
            }
            engine = null;
            running = false;
            paused = false;
            stopRequested = false;
            fireStateChanged();
        }
    }

    private void fireStateChanged() {
        if (onStateChanged != null) {
            onStateChanged.run();
        }
    }
}
