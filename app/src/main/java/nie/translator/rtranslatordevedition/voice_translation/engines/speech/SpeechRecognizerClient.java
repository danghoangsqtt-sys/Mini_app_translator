package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;

/** One owned platform recognizer with no state shared across engine generations. */
interface SpeechRecognizerClient {
    interface Listener {
        void onPartialResult(String text, float confidence);
        void onFinalResult(String text, float confidence);
        void onError(int errorCode);
    }

    void start(SpeechRecognitionEngine.RecognitionRequest request, Listener listener);
    void stop();
    void cancel();
    void destroy();
}
