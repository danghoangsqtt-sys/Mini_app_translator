package nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie;

import android.content.Intent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import java.util.ArrayList;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.gui.messages.GuiMessage;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationService;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactory;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactoryRegistry;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineType;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine;
import nie.translator.rtranslatordevedition.voice_translation.engines.ondevice.OnDeviceEngineFactory;
import com.bluetooth.communicator.Message;
import com.bluetooth.communicator.Peer;

/** Default mode deliberately uses one recognizer and an explicit source direction. */
public class WalkieTalkieService extends VoiceTranslationService {
    public static final int CHANGE_FIRST_LANGUAGE = 22;
    public static final int CHANGE_SECOND_LANGUAGE = 23;
    public static final int GET_FIRST_LANGUAGE = 24;
    public static final int GET_SECOND_LANGUAGE = 25;
    public static final int CHANGE_SOURCE_DIRECTION = 26;
    public static final int GET_SOURCE_DIRECTION = 27;
    public static final int ON_FIRST_LANGUAGE = 22;
    public static final int ON_SECOND_LANGUAGE = 23;
    public static final int ON_SOURCE_DIRECTION = 24;
    private Global global;
    private CustomLocale firstLanguage;
    private CustomLocale secondLanguage;
    private boolean sourceIsFirst = true;
    private WalkieTalkieOnDeviceController controller;
    private final BindingAttemptTracker firstLanguageBindingAttempts = new BindingAttemptTracker();
    private final BindingAttemptTracker secondLanguageBindingAttempts = new BindingAttemptTracker();
    private final ServiceConnection firstLanguageConnection = new DormantConnection();
    private final ServiceConnection secondLanguageConnection = new DormantConnection();

    @Override public void onCreate() {
        super.onCreate();
        global = (Global) getApplication();
        sourceIsFirst = getSharedPreferences("walkie_direction", MODE_PRIVATE).getBoolean("sourceIsFirst", true);
        createController();
        clientHandler = new Handler(new Handler.Callback() {
            @Override public boolean handleMessage(android.os.Message message) {
                int command = message.getData().getInt("command", -1);
                if (!WalkieTalkieService.super.executeCommand(command, message.getData())) {
                    switch (command) {
                        case CHANGE_FIRST_LANGUAGE: applyLanguages((CustomLocale) message.getData().getSerializable("language"), secondLanguage); break;
                        case CHANGE_SECOND_LANGUAGE: applyLanguages(firstLanguage, (CustomLocale) message.getData().getSerializable("language")); break;
                        case CHANGE_SOURCE_DIRECTION:
                            boolean requestedDirection = message.getData().getBoolean("sourceIsFirst", true);
                            if (sourceIsFirst != requestedDirection) {
                                sourceIsFirst = requestedDirection;
                                getSharedPreferences("walkie_direction", MODE_PRIVATE).edit().putBoolean("sourceIsFirst", sourceIsFirst).apply();
                                controller.setSourceIsFirst(sourceIsFirst); finishOnDeviceTurn();
                            }
                            break;
                        case GET_FIRST_LANGUAGE: notifyLanguage(ON_FIRST_LANGUAGE, firstLanguage); break;
                        case GET_SECOND_LANGUAGE: notifyLanguage(ON_SECOND_LANGUAGE, secondLanguage); break;
                        case GET_SOURCE_DIRECTION: Bundle state = new Bundle(); state.putInt("callback", ON_SOURCE_DIRECTION); state.putBoolean("sourceIsFirst", sourceIsFirst); notifyToClient(state); break;
                        case RECEIVE_TEXT: String text = message.getData().getString("text"); if (text != null && !text.isEmpty()) { controller.translateTyped(text); } break;
                        default: break;
                    }
                }
                return true;
            }
        });
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(null, flags, startId);
        }
        applyLanguages((CustomLocale) intent.getSerializableExtra("firstLanguage"), (CustomLocale) intent.getSerializableExtra("secondLanguage"));
        return super.onStartCommand(intent, flags, startId);
    }

    private void createController() {
        EngineFactoryRegistry registry = new EngineFactoryRegistry();
        registry.register(new OnDeviceEngineFactory(this, getSpeechOutputEngine()));
        EngineFactory factory = registry.get(EngineType.ON_DEVICE);
        SpeechRecognitionEngine speech = factory.createSpeechRecognitionEngine();
        TextTranslationEngine translation = factory.createTextTranslationEngine();
        controller = new WalkieTalkieOnDeviceController(speech, translation, new EngineTurnCoordinator(speech, translation, null), new WalkieTalkieOnDeviceController.Listener() {
            @Override public void onTranslated(String text, CustomLocale language, boolean sourceFirst) {
                speak(text, language); GuiMessage message = new GuiMessage(new Message(WalkieTalkieService.this, text), sourceFirst, true); notifyMessage(message); addMessage(message);
            }
            @Override public void onError(nie.translator.rtranslatordevedition.voice_translation.engines.EngineError error,
                                          nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind kind) { notifyEngineError(error, kind); }
            @Override public void onTurnEnded() { finishOnDeviceTurn(); }
        });
    }
    private void applyLanguages(CustomLocale first, CustomLocale second) {
        if (sameLocale(firstLanguage, first) && sameLocale(secondLanguage, second)) { return; }
        firstLanguage = first; secondLanguage = second;
        if (controller != null) { controller.setLanguages(firstLanguage, secondLanguage); controller.setSourceIsFirst(sourceIsFirst); finishOnDeviceTurn(); }
    }
    private static boolean sameLocale(CustomLocale first, CustomLocale second) { return first == second || (first != null && first.equals(second)); }
    private void notifyLanguage(int callback, CustomLocale language) { Bundle data = new Bundle(); data.putInt("callback", callback); data.putSerializable("language", language); notifyToClient(data); }
    @Override protected void startOnDeviceTurn() { notifyVoiceStart(); controller.startTurn(); }
    @Override protected void cancelOnDeviceTurn() { if (controller != null) { controller.stopTurn(); } }
    @Override public void onDestroy() {
        if (controller != null) { controller.close(); controller = null; }
        stopFirstLanguageCommunication(); stopSecondLanguageCommunication();
        releaseBindings(firstLanguageBindingAttempts, firstLanguageConnection); releaseBindings(secondLanguageBindingAttempts, secondLanguageConnection);
        super.onDestroy();
    }

    // Retained dormant lifecycle seams keep legacy bindings isolated until Task 9.6 opt-in.
    private void bindFirstLanguageRecognizer(Intent intent) { if (firstLanguageBindingAttempts.recordBindAttempt()) { bindService(intent, firstLanguageConnection, Service.BIND_AUTO_CREATE); } }
    private void bindSecondLanguageRecognizer(Intent intent) { if (secondLanguageBindingAttempts.recordBindAttempt()) { bindService(intent, secondLanguageConnection, Service.BIND_AUTO_CREATE); } }
    private void stopFirstLanguageCommunication() { }
    private void stopSecondLanguageCommunication() { }
    private void releaseBindings(BindingAttemptTracker attempts, final ServiceConnection connection) {
        attempts.releaseAll(new BindingAttemptTracker.Unbinder() { @Override public void unbind() { unbindService(connection); } }, new BindingAttemptTracker.FailureListener() { @Override public void onFailure(RuntimeException error) { if (error != null) { return; } } });
    }
    private static final class DormantConnection implements ServiceConnection {
        @Override public void onServiceConnected(ComponentName name, android.os.IBinder service) { }
        @Override public void onServiceDisconnected(ComponentName name) { }
    }

    public static class WalkieTalkieServiceCommunicator extends VoiceTranslationServiceCommunicator {
        private final ArrayList<LanguageListener> firstListeners = new ArrayList<>();
        private final ArrayList<LanguageListener> secondListeners = new ArrayList<>();
        private final ArrayList<DirectionListener> directionListeners = new ArrayList<>();
        public WalkieTalkieServiceCommunicator(int id) {
            super(id); super.serviceHandler = new Handler(new Handler.Callback() {
                @Override public boolean handleMessage(android.os.Message msg) {
                    msg.getData().setClassLoader(Peer.class.getClassLoader()); Bundle data = msg.getData(); int callback = data.getInt("callback", -1);
                    if (!executeCallback(callback, data)) {
                        if (callback == ON_FIRST_LANGUAGE) { notifyLanguage(firstListeners, (CustomLocale) data.getSerializable("language")); }
                        else if (callback == ON_SECOND_LANGUAGE) { notifyLanguage(secondListeners, (CustomLocale) data.getSerializable("language")); }
                        else if (callback == ON_SOURCE_DIRECTION) { while (!directionListeners.isEmpty()) { directionListeners.remove(0).onDirection(data.getBoolean("sourceIsFirst")); } }
                    }
                    return true;
                }
            });
        }
        public void getFirstLanguage(LanguageListener listener) { firstListeners.add(listener); request(GET_FIRST_LANGUAGE); }
        public void getSecondLanguage(LanguageListener listener) { secondListeners.add(listener); request(GET_SECOND_LANGUAGE); }
        public void getSourceDirection(DirectionListener listener) { directionListeners.add(listener); request(GET_SOURCE_DIRECTION); }
        public void changeFirstLanguage(CustomLocale language) { sendLanguage(CHANGE_FIRST_LANGUAGE, language); }
        public void changeSecondLanguage(CustomLocale language) { sendLanguage(CHANGE_SECOND_LANGUAGE, language); }
        public void changeSourceDirection(boolean sourceIsFirst) { Bundle data = new Bundle(); data.putInt("command", CHANGE_SOURCE_DIRECTION); data.putBoolean("sourceIsFirst", sourceIsFirst); super.sendToService(data); }
        private void request(int command) { Bundle data = new Bundle(); data.putInt("command", command); super.sendToService(data); }
        private void sendLanguage(int command, CustomLocale language) { Bundle data = new Bundle(); data.putInt("command", command); data.putSerializable("language", language); super.sendToService(data); }
        private static void notifyLanguage(ArrayList<LanguageListener> listeners, CustomLocale language) { while (!listeners.isEmpty()) { listeners.remove(0).onLanguage(language); } }
    }
    public interface LanguageListener { void onLanguage(CustomLocale language); }
    public interface DirectionListener { void onDirection(boolean sourceIsFirst); }
}
