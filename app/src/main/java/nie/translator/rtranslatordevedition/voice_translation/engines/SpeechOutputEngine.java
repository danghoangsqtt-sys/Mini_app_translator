package nie.translator.rtranslatordevedition.voice_translation.engines;

import nie.translator.rtranslatordevedition.tools.CustomLocale;

/** Runtime-neutral speech output contract. Successful speak callbacks mean the request was accepted into a queue, not that an utterance finished. */
public interface SpeechOutputEngine {
    enum QueueMode {
        FLUSH,
        ADD
    }

    /** A success confirms request acceptance only; it is not an utterance-completion signal. */
    interface ResultCallback {
        void onSuccess();
        void onFailure(EngineError error);
    }

    EngineCapability getCapability();
    EngineOperation setLanguage(CustomLocale language, ResultCallback callback);
    EngineOperation speak(CharSequence text, QueueMode queueMode, ResultCallback callback);
    void stop();
    void close();
}
