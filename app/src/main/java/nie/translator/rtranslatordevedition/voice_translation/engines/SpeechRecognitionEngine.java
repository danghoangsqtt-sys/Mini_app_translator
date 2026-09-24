package nie.translator.rtranslatordevedition.voice_translation.engines;

import java.util.List;
import java.util.Objects;
import nie.translator.rtranslatordevedition.tools.CustomLocale;

/** Runtime-neutral bounded speech recognition contract with explicit optional PCM input. */
public interface SpeechRecognitionEngine {
    enum AudioInputMode {
        ENGINE_CAPTURE,
        PCM_STREAM
    }

    final class RecognitionRequest {
        private final CustomLocale language;
        private final int sampleRateHertz;
        private final AudioInputMode audioInputMode;

        public RecognitionRequest(CustomLocale language, int sampleRateHertz, AudioInputMode audioInputMode) {
            this.language = Objects.requireNonNull(language, "language");
            this.audioInputMode = Objects.requireNonNull(audioInputMode, "audioInputMode");
            if (sampleRateHertz < 0 || (audioInputMode == AudioInputMode.PCM_STREAM && sampleRateHertz == 0)) {
                throw new IllegalArgumentException("PCM_STREAM requires a positive sampleRateHertz");
            }
            this.sampleRateHertz = sampleRateHertz;
        }

        public CustomLocale getLanguage() { return language; }
        /** Zero is valid only for ENGINE_CAPTURE, where the engine owns the audio source. */
        public int getSampleRateHertz() { return sampleRateHertz; }
        public AudioInputMode getAudioInputMode() { return audioInputMode; }
    }

    interface RecognitionCallback {
        void onResult(String text, CustomLocale language, float confidence, boolean isFinal);
        void onFailure(EngineError error);
    }

    EngineCapability getCapability();
    List<CustomLocale> getSupportedLanguages();
    EngineOperation start(RecognitionRequest request, RecognitionCallback callback);
    boolean submitAudio(EngineOperation operation, byte[] audio, int size);
    boolean finish(EngineOperation operation);
    void close();
}
