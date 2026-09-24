package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

/** Platform seam; construction methods are always invoked by the main-thread scheduler. */
interface SpeechRecognizerClientFactory {
    boolean isSystemRecognitionAvailable();
    boolean isOnDeviceRecognitionAvailable();
    SpeechRecognizerClient createOnDevice();
    SpeechRecognizerClient createSystem();
}
