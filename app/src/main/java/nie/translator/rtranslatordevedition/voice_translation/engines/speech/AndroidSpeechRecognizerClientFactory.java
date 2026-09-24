package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import android.content.Context;
import android.os.Build;
import android.speech.SpeechRecognizer;
import androidx.annotation.RequiresApi;

/** Uses only application context and isolates API 31 symbols behind a real SDK guard. */
final class AndroidSpeechRecognizerClientFactory implements SpeechRecognizerClientFactory {
    private final Context applicationContext;

    AndroidSpeechRecognizerClientFactory(Context context) {
        applicationContext = context.getApplicationContext();
    }

    @Override public boolean isSystemRecognitionAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(applicationContext);
    }

    @Override public boolean isOnDeviceRecognitionAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Api31.isOnDeviceAvailable(applicationContext);
    }

    @Override public SpeechRecognizerClient createOnDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw new UnsupportedOperationException("On-device recognition requires API 31");
        }
        return new AndroidSpeechRecognizerClient(Api31.createOnDevice(applicationContext));
    }

    @Override public SpeechRecognizerClient createSystem() {
        return new AndroidSpeechRecognizerClient(SpeechRecognizer.createSpeechRecognizer(applicationContext));
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Api31 {
        private static boolean isOnDeviceAvailable(Context context) {
            return SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
        }

        private static SpeechRecognizer createOnDevice(Context context) {
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        }
    }
}
