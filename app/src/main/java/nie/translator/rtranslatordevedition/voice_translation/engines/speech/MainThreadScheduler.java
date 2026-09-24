package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

/** Minimal main-thread seam for recognizer lifecycle work and its bounded watchdog. */
interface MainThreadScheduler {
    void execute(Runnable runnable);
    void executeAndWait(Runnable runnable);
    void schedule(Runnable runnable, long delayMillis);
    void cancel(Runnable runnable);
}
