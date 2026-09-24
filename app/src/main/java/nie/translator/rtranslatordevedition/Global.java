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

package nie.translator.rtranslatordevedition;

import android.app.Application;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import nie.translator.rtranslatordevedition.api_management.ConsumptionsDataManager;
import nie.translator.rtranslatordevedition.api_management.CredentialStore;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer.RecentPeersDataManager;
import nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice.Recorder;
import nie.translator.rtranslatordevedition.voice_translation.engines.translation.MlKitLanguageMapper;


public class Global extends Application {
    public static final List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final int TOKEN_FETCH_MARGIN = 60 * 1000; // one minute
    private ArrayList<CustomLocale> languages = new ArrayList<>();
    private CustomLocale language;
    private CustomLocale firstLanguage;
    private CustomLocale secondLanguage;
    private RecentPeersDataManager recentPeersDataManager;
    private BluetoothCommunicatorLifecycle<ConversationBluetoothCommunicator> bluetoothCommunicatorLifecycle;
    private MlKitLanguageMapper languageMapper;
    private String name = "";
    private String apiKeyFileName = "";
    private ConsumptionsDataManager databaseManager;
    private CredentialStore credentialStore;
    private int micSensitivity = -1;
    private int speechTimeout = -1;
    private int prevVoiceDuration = -1;
    private int amplitudeThreshold = Recorder.DEFAULT_AMPLITUDE_THRESHOLD;
    private Handler mainHandler;
    private Handler tokenRefreshHandler;
    private ApiTokenCoordinator apiTokenCoordinator;
    private ApiTokenCallbackDispatcher apiTokenCallbackDispatcher;
    private final Object apiTokenBoundaryLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        tokenRefreshHandler = new Handler(Looper.getMainLooper());
        apiTokenCallbackDispatcher = new ApiTokenCallbackDispatcher(
                new ApiTokenCallbackDispatcher.MainThreadExecutor() {
                    @Override
                    public void execute(Runnable runnable) {
                        mainHandler.post(runnable);
                    }
                });
        credentialStore = new CredentialStore(this);
        apiTokenCoordinator = new ApiTokenCoordinator(new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(command, "getAppToken").start();
            }
        }, new ApiTokenCoordinator.TokenFetcher() {
            @Override
            public AccessToken fetch() throws IOException {
                File legacyCredential = new File(getFilesDir(), getApiKeyFileName());
                try (InputStream stream = credentialStore.openCredential(legacyCredential)) {
                    return GoogleCredentials.fromStream(stream).createScoped(SCOPE)
                            .refreshAccessToken();
                }
            }
        }, new ApiTokenCoordinator.Scheduler() {
            @Override
            public void schedule(Runnable runnable, long delayMillis) {
                tokenRefreshHandler.postDelayed(runnable, delayMillis);
            }

            @Override
            public void cancel(Runnable runnable) {
                tokenRefreshHandler.removeCallbacks(runnable);
            }
        }, TOKEN_FETCH_MARGIN);
        recentPeersDataManager = new RecentPeersDataManager(this);
        bluetoothCommunicatorLifecycle = new BluetoothCommunicatorLifecycle<>(
                new BluetoothCommunicatorLifecycle.PermissionGate() {
                    @Override
                    public boolean isGranted() {
                        return hasBluetoothConnectPermission();
                    }
                },
                new BluetoothCommunicatorLifecycle.Factory<ConversationBluetoothCommunicator>() {
                    @Override
                    public ConversationBluetoothCommunicator create() {
                        return new ConversationBluetoothCommunicator(Global.this, getName(), BluetoothCommunicator.STRATEGY_P2P_WITH_RECONNECTION);
                    }
                },
                new BluetoothCommunicatorLifecycle.Destroyer<ConversationBluetoothCommunicator>() {
                    @Override
                    public void destroy(ConversationBluetoothCommunicator communicator, final Runnable onDestroyed) {
                        communicator.destroy(new BluetoothCommunicator.DestroyCallback() {
                            @Override
                            public void onDestroyed() {
                                onDestroyed.run();
                            }
                        });
                    }
                });
        languageMapper = new MlKitLanguageMapper();
        databaseManager = new ConsumptionsDataManager(this);
        getMicSensitivity();
    }


    @Nullable
    public ConversationBluetoothCommunicator getBluetoothCommunicator() {
        return bluetoothCommunicatorLifecycle.getIfPermitted();
    }

    @Nullable
    public ConversationBluetoothCommunicator initializeBluetoothCommunicatorIfPermitted() {
        return bluetoothCommunicatorLifecycle.initializeIfPermitted();
    }

    public void resetBluetoothCommunicator() {
        bluetoothCommunicatorLifecycle.resetIfPermitted();
    }

    private boolean hasBluetoothConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void getLanguages(final boolean recycleResult, final GetLocalesListListener responseListener) {
        if (recycleResult && languages.size() > 0) {
            responseListener.onSuccess(defensiveLanguageCopy(languages));
        } else {
            Global.this.languages = new ArrayList<>(languageMapper.getSupportedLocales(CustomLocale.getDefault()));
            responseListener.onSuccess(defensiveLanguageCopy(Global.this.languages));
        }
    }

    static ArrayList<CustomLocale> defensiveLanguageCopy(ArrayList<CustomLocale> source) {
        return new ArrayList<>(source);
    }

    public interface GetLocalesListListener {
        void onSuccess(ArrayList<CustomLocale> result);

        void onFailure(int[] reasons, long value);
    }

    public void getLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> languages) {
                CustomLocale predefinedLanguage = CustomLocale.getDefault();
                CustomLocale language = null;
                if (recycleResult && Global.this.language != null) {
                    language = Global.this.language;
                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                    String code = sharedPreferences.getString("language", predefinedLanguage.getCode());
                    if (code != null) {
                        language = CustomLocale.getInstance(code);
                    }
                }

                int index = CustomLocale.search(languages, language);
                if (index != -1) {
                    language = languages.get(index);
                } else {
                    int index2 = CustomLocale.search(languages, predefinedLanguage);
                    if (index2 != -1) {
                        language = predefinedLanguage;
                    } else {
                        language = new CustomLocale("en", "US");
                    }
                }

                Global.this.language = language;
                responseListener.onSuccess(language);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public void getFirstLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(final ArrayList<CustomLocale> languages) {
                getLanguage(true, new GetLocaleListener() {
                    @Override
                    public void onSuccess(CustomLocale predefinedLanguage) {
                        CustomLocale language = null;
                        if (recycleResult && Global.this.firstLanguage != null) {
                            language = Global.this.firstLanguage;
                        } else {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                            String code = sharedPreferences.getString("firstLanguage", predefinedLanguage.getCode());
                            if (code != null) {
                                language = CustomLocale.getInstance(code);
                            }
                        }

                        int index = CustomLocale.search(languages, language);
                        if (index != -1) {
                            language = languages.get(index);
                        } else {
                            int index2 = CustomLocale.search(languages, predefinedLanguage);
                            if (index2 != -1) {
                                language = predefinedLanguage;
                            } else {
                                language = new CustomLocale("en", "US");
                            }
                        }

                        Global.this.firstLanguage = language;
                        responseListener.onSuccess(language);
                    }

                    @Override
                    public void onFailure(int[] reasons, long value) {
                        responseListener.onFailure(reasons, value);
                    }
                });
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    public void getSecondLanguage(final boolean recycleResult, final GetLocaleListener responseListener) {
        getLanguages(true, new GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> languages) {
                CustomLocale predefinedLanguage = CustomLocale.getDefault();
                CustomLocale language = null;
                if (recycleResult && Global.this.secondLanguage != null) {
                    language = Global.this.secondLanguage;
                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Global.this);
                    String code = sharedPreferences.getString("secondLanguage", null);
                    if (code != null) {
                        language = CustomLocale.getInstance(code);
                    }
                }

                int index = CustomLocale.search(languages, language);
                if (index != -1) {
                    language = languages.get(index);
                } else {
                    language = new CustomLocale("en", "US");
                }

                Global.this.secondLanguage = language;
                responseListener.onSuccess(language);
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });
    }

    public interface GetLocaleListener {
        void onSuccess(CustomLocale result);

        void onFailure(int[] reasons, long value);
    }

    public void setLanguage(CustomLocale language) {
        this.language = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("language", language.getCode());
        editor.apply();
    }

    public void setFirstLanguage(CustomLocale language) {
        this.firstLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("firstLanguage", language.getCode());
        editor.apply();
    }

    public void setSecondLanguage(CustomLocale language) {
        this.secondLanguage = language;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("secondLanguage", language.getCode());
        editor.apply();
    }


    public int getAmplitudeThreshold() {
        return amplitudeThreshold;
    }


    public int getMicSensitivity() {
        if (micSensitivity == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            micSensitivity = sharedPreferences.getInt("micSensibility", 50);
            setAmplitudeThreshold(micSensitivity);
        }
        return micSensitivity;
    }

    public void setMicSensitivity(int value) {
        micSensitivity = value;
        setAmplitudeThreshold(micSensitivity);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("micSensibility", value);
        editor.apply();
    }

    public int getSpeechTimeout() {
        if (speechTimeout == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            speechTimeout = sharedPreferences.getInt("speechTimeout", Recorder.DEFAULT_SPEECH_TIMEOUT_MILLIS);
        }
        return speechTimeout;
    }

    public void setSpeechTimeout(int value) {
        speechTimeout = value;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("speechTimeout", value);
        editor.apply();
    }

    public int getPrevVoiceDuration() {
        if (prevVoiceDuration == -1) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            prevVoiceDuration = sharedPreferences.getInt("prevVoiceDuration", Recorder.DEFAULT_PREV_VOICE_DURATION);
        }
        return prevVoiceDuration;
    }

    public void setPrevVoiceDuration(int value) {
        prevVoiceDuration = value;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("prevVoiceDuration", value);
        editor.apply();
    }

    private void setAmplitudeThreshold(int micSensitivity) {
        float amplitudePercentage = 1f - (micSensitivity / 100f);
        if (amplitudePercentage < 0.5f) {
            amplitudeThreshold = Math.round(Recorder.MIN_AMPLITUDE_THRESHOLD + ((Recorder.DEFAULT_AMPLITUDE_THRESHOLD - Recorder.MIN_AMPLITUDE_THRESHOLD) * (amplitudePercentage * 2)));
        } else {
            amplitudeThreshold = Math.round(Recorder.DEFAULT_AMPLITUDE_THRESHOLD + ((Recorder.MAX_AMPLITUDE_THRESHOLD - Recorder.DEFAULT_AMPLITUDE_THRESHOLD) * ((amplitudePercentage - 0.5F) * 2)));
        }
    }

    public String getName() {
        if (name.length() == 0) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            name = sharedPreferences.getString("name", "user");
        }
        return name;
    }

    public void setName(String savedName) {
        name = savedName;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", savedName);
        editor.apply();
        ConversationBluetoothCommunicator communicator = getBluetoothCommunicator();
        if (communicator != null) {
            communicator.setName(savedName);  //si aggiorna il nome anche per il communicator
        }
    }

    public Peer getMyPeer() {
        return new Peer(null, getName(), false);
    }

    public abstract static class MyPeerListener {
        public abstract void onSuccess(Peer myPeer);

        public void onFailure(int[] reasons, long value) {
        }
    }

    public void getMyID(final MyIDListener responseListener) {
        responseListener.onSuccess(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    public abstract static class MyIDListener {
        public abstract void onSuccess(String id);

        public void onFailure(int[] reasons, long value) {
        }
    }

    public RecentPeersDataManager getRecentPeersDataManager() {
        return recentPeersDataManager;
    }

    public abstract static class ResponseListener {
        public void onSuccess() {

        }

        public void onFailure(int[] reasons, long value) {
        }
    }

    public void addUsage(float creditToSub) {
        // add consumption to the database
        databaseManager.addUsage(creditToSub);
    }

    public boolean isFirstStart() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("firstStart", true);
    }

    public void setFirstStart(boolean firstStart) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firstStart", firstStart);
        editor.apply();
    }

    public String getApiKeyFileName() {
        if (apiKeyFileName.length() == 0) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            apiKeyFileName = sharedPreferences.getString("apiKeyFileName", "");
        }
        return apiKeyFileName;
    }

    public void setApiKeyFileName(String apiKeyFileName) {
        this.apiKeyFileName = apiKeyFileName;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("apiKeyFileName", apiKeyFileName);
        editor.apply();
    }

    public CredentialStore getCredentialStore() {
        return credentialStore;
    }


    //api token

    public void resetApiToken() {
        synchronized (apiTokenBoundaryLock) {
            apiTokenCallbackDispatcher.reset(
                    new IOException("Credential changed before token callback delivery"));
            apiTokenCoordinator.reset();
        }
    }

    public void getApiToken(final boolean recycleResult, @Nullable final ApiTokenListener responseListener) {
        synchronized (apiTokenBoundaryLock) {
            if (responseListener == null) {
                apiTokenCoordinator.request(recycleResult, null);
                return;
            }

            final ApiTokenCallbackDispatcher.Registration registration =
                    apiTokenCallbackDispatcher.register(new ApiTokenCallbackDispatcher.Callback() {
                        @Override
                        public void onSuccess(AccessToken apiToken) {
                            responseListener.onSuccess(apiToken);
                        }

                        @Override
                        public void onFailure(IOException exception) {
                            Log.e("token", "Failed to obtain access token.", exception);
                            if (exception.getCause() instanceof UnknownHostException) {
                                responseListener.onFailure(new int[]{ErrorCodes.MISSED_CONNECTION}, -1);
                            } else if (getApiKeyFileName().length() == 0) {
                                responseListener.onFailure(new int[]{ErrorCodes.MISSING_API_KEY}, -1);
                            } else {
                                responseListener.onFailure(new int[]{ErrorCodes.WRONG_API_KEY}, -1);
                            }
                        }
                    });
            apiTokenCoordinator.request(recycleResult, new ApiTokenCoordinator.Listener() {
                @Override
                public void onSuccess(AccessToken apiToken) {
                    apiTokenCallbackDispatcher.postSuccess(registration, apiToken);
                }

                @Override
                public void onFailure(IOException exception) {
                    apiTokenCallbackDispatcher.postFailure(registration, exception);
                }
            });
        }
    }

    public interface ApiTokenListener {
        void onSuccess(AccessToken apiToken);

        void onFailure(int[] reasons, long value);
    }
}

