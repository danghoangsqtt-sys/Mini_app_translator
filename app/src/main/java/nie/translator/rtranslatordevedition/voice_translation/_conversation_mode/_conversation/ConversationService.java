/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.tools.BluetoothHeadsetUtils;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.gui.messages.GuiMessage;
import nie.translator.rtranslatordevedition.tools.gui.peers.GuiPeer;
import nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationService;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;

import com.bluetooth.communicator.tools.Timer;
import com.bluetooth.communicator.Message;
import com.bluetooth.communicator.Peer;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.CloudApiText;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.Translator;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.Recognizer;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.Recorder;


public class ConversationService extends VoiceTranslationService {
    //commands
    public static final int CHANGE_LANGUAGE = 15;

    private static final long WAKELOCK_TIMEOUT = 20 * 1000L;  // 10 minutes, so if the service stopped without calling onDestroyed the wakeLock would still be released within 10 minutes
    private Timer wakeLockTimer;  // to reactivate the timer every 10 minutes, so as long as the service is active the wakelock will never expire
    private PowerManager.WakeLock screenWakeLock;
    private String myPeerName;
    private ConversationOnDeviceController controller;
    private BluetoothHelper mBluetoothHelper;
    private Global global;
    private ConversationBluetoothCommunicator.Callback communicationCallback;
    private Handler mainHandler;
    private ScoReconnectCoordinator scoReconnectCoordinator;
    private boolean closed;


    @Override
    public void onCreate() {
        super.onCreate();
        global = (Global) getApplication();
        createController();
        mainHandler = new Handler(Looper.getMainLooper());
        scoReconnectCoordinator = new ScoReconnectCoordinator(new ScoReconnectCoordinator.Dispatcher() {
            @Override
            public void postDelayed(Runnable runnable, long delayMillis) {
                mainHandler.postDelayed(runnable, delayMillis);
            }

            @Override
            public void removeCallbacks(Runnable runnable) {
                mainHandler.removeCallbacks(runnable);
            }
        }, new ScoReconnectCoordinator.Reconnector() {
            @Override
            public void reconnect() {
                if (mBluetoothHelper != null) {
                    mBluetoothHelper.stop();
                    mBluetoothHelper.start();
                }
            }
        });
        // wake lock initialization (to keep the process active when the phone is on standby)
        acquireWakeLock();
        //startBluetoothSco
        mBluetoothHelper = new BluetoothHelper(this);
        clientHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(final android.os.Message message) {
                int command = message.getData().getInt("command", -1);
                if (command != -1) {
                    if (!ConversationService.super.executeCommand(command, message.getData())) {
                        switch (command) {
                            case RECEIVE_TEXT:
                                global.getLanguage(false,new Global.GetLocaleListener() {
                                    @Override
                                    public void onSuccess(CustomLocale language) {
                                        String text = message.getData().getString("text");
                                        if (text != null) {
                                            GuiMessage guiMessage = new GuiMessage(new Message(global, text), true, true);
                                            // send the message
                                            sendMessage(new ConversationMessage(new CloudApiText(text, language)));

                                            notifyMessage(guiMessage);
                                            // we save every new message in the exchanged messages so that the fragment can restore them
                                            addMessage(guiMessage);
                                        }
                                    }

                                    @Override
                                    public void onFailure(int[] reasons, long value) {
                                        ConversationService.super.notifyError(reasons, value);
                                    }
                                });
                                break;
                        }
                    }
                }
                return false;
            }
        });
        communicationCallback = new ConversationBluetoothCommunicator.Callback() {
            @Override
            public void onMessageReceived(final Message message) {
                super.onMessageReceived(message);
                global.getLanguage(false,new Global.GetLocaleListener() {
                    @Override
                    public void onSuccess(CustomLocale result) {
                        ConversationOnDeviceController current = controller;
                        if (!closed && current != null) { current.onIncoming(message.getText(), result); }
                    }

                    @Override
                    public void onFailure(int[] reasons, long value) {
                        ConversationService.super.notifyError(reasons, value);
                    }
                });
            }

            @Override
            public void onDisconnected(GuiPeer peer, int peersLeft) {
                super.onDisconnected(peer, peersLeft);
                if (peersLeft == 0) {
                    stopSelf();
                }
            }
        };
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null) {
            communicator.addCallback(communicationCallback);
        }

        mBluetoothHelper.start();
    }

    private void sendMessage(ConversationMessage conversationMessage) {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null) {
            communicator.sendMessage(new Message(global, ConversationPayloadCodec.encode(conversationMessage.getPayload().getText(), conversationMessage.getPayload().getLanguage())));
        }
    }

    private void createController() {
        nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactoryRegistry registry = new nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactoryRegistry();
        registry.register(new nie.translator.rtranslatordevedition.voice_translation.engines.ondevice.OnDeviceEngineFactory(this, getSpeechOutputEngine()));
        nie.translator.rtranslatordevedition.voice_translation.engines.EngineFactory factory = registry.get(nie.translator.rtranslatordevedition.voice_translation.engines.EngineType.ON_DEVICE);
        final nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine speech = factory.createSpeechRecognitionEngine();
        final nie.translator.rtranslatordevedition.voice_translation.engines.TextTranslationEngine translation = factory.createTextTranslationEngine();
        final nie.translator.rtranslatordevedition.voice_translation.engines.SpeechOutputEngine output = factory.createSpeechOutputEngine();
        controller = new ConversationOnDeviceController(speech, translation,
                new nie.translator.rtranslatordevedition.voice_translation.engines.EngineTurnCoordinator(speech, translation, null),
                new ConversationOnDeviceController.Listener() {
                    @Override public void onPartial(String text) { notifyMessage(new GuiMessage(new Message(global, text), true, false)); }
                    @Override public void onOutboundFinal(String text, CustomLocale language) {
                        ConversationService.this.sendMessage(new ConversationMessage(new CloudApiText(text, language)));
                        GuiMessage message = new GuiMessage(new Message(global, text), true, true); notifyMessage(message); addMessage(message);
                    }
                    @Override public void onIncomingText(String text, CustomLocale language) {
                        speak(text, language); GuiMessage message = new GuiMessage(new Message(global, text), false, true); notifyMessage(message); addMessage(message);
                    }
                    @Override public void onError(nie.translator.rtranslatordevedition.voice_translation.engines.EngineError error,
                                                  nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind kind) {
                        if (kind == nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind.TRANSLATION
                                || kind == nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.OperationKind.LANGUAGE_DETECTION) {
                            notifyRecoverableEngineError(nie.translator.rtranslatordevedition.voice_translation.engines.EngineServiceErrorMapper.map(error, kind));
                        } else { notifyEngineError(error, kind); }
                    }
                    @Override public void onTurnEnded() { finishOnDeviceTurn(); }
                });
    }

    @Override protected void startOnDeviceTurn() {
        global.getLanguage(true, new Global.GetLocaleListener() {
            @Override public void onSuccess(CustomLocale language) { if (!closed && controller != null) { notifyVoiceStart(); controller.startTurn(language); } }
            @Override public void onFailure(int[] reasons, long value) { notifyError(reasons, value); }
        });
    }

    @Override protected void cancelOnDeviceTurn() { if (controller != null) { controller.stopTurn(); } }

    public String getMyPeerName() {
        return myPeerName;
    }

    public void setMyPeerName(String myPeerName) {
        this.myPeerName = myPeerName;
    }

    @Override
    protected boolean shouldStopMicDuringTTS() {
        return !mBluetoothHelper.isOnHeadsetSco();
    }

    @Override
    protected boolean isBluetoothHeadsetConnected() {
        return mBluetoothHelper.isOnHeadsetSco();
    }

    private void startWakeLockReactivationTimer(final Timer.Callback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                wakeLockTimer = new Timer(WAKELOCK_TIMEOUT - 10000);  //si riattiva 10 secondi prima cosi da essere sicuri che non ci siano istanti in cui il wakelock è rilasciato
                wakeLockTimer.setCallback(callback);
                wakeLockTimer.start();
            }
        });
    }

    private void resetWakeLockReactivationTimer() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (wakeLockTimer != null) {
                    wakeLockTimer.cancel();
                    wakeLockTimer = null;
                }
            }
        });
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        screenWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "speechGoogleUserEdition:screenWakeLock");
        screenWakeLock.acquire(WAKELOCK_TIMEOUT);
        startWakeLockReactivationTimer(new Timer.Callback() {
            @Override
            public void onFinished() {
                acquireWakeLock();
            }
        });
    }

    @Override
    public void onDestroy() {
        // Mark lifecycle ended before Bluetooth teardown can synchronously emit SCO callbacks.
        closed = true;
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null && communicationCallback != null) { communicator.removeCallback(communicationCallback); }
        scoReconnectCoordinator.destroy();
        if (controller != null) { controller.close(); controller = null; }
        //stop Bluetooth helper
        mBluetoothHelper.stop();
        super.onDestroy();
        //release wake lock
        resetWakeLockReactivationTimer();
        if (screenWakeLock != null) {
            while (screenWakeLock.isHeld()) {
                screenWakeLock.release();
            }
            screenWakeLock = null;
        }
    }

    private class BluetoothHelper extends BluetoothHeadsetUtils {
        private BluetoothHelper(Context context) {
            super(context);
        }

        @Override
        public void onHeadsetConnected() {
        }

        @Override
        public void onScoAudioConnected() {
            Bundle bundle = new Bundle();
            bundle.putInt("callback", ON_CONNECTED_BLUETOOTH_HEADSET);
            notifyToClient(bundle);
        }

        @Override
        public void onScoAudioDisconnected() {
            Bundle bundle = new Bundle();
            bundle.putInt("callback", ON_DISCONNECTED_BLUETOOTH_HEADSET);
            notifyToClient(bundle);
            scoReconnectCoordinator.scheduleReconnect(1000);
        }

        @Override
        public void onHeadsetDisconnected() {
        }
    }

    public static class ConversationServiceCommunicator extends VoiceTranslationServiceCommunicator {
        public ConversationServiceCommunicator(int id) {
            super(id);
            super.serviceHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(android.os.Message msg) {
                    msg.getData().setClassLoader(Peer.class.getClassLoader());
                    int callbackMessage = msg.getData().getInt("callback", -1);
                    Bundle data = msg.getData();
                    executeCallback(callbackMessage, data);
                    return true;
                }
            });
        }
    }
}
