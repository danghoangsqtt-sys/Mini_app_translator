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

package nie.translator.rtranslatordevedition.voice_translation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.util.ArrayList;
import java.util.List;
import nie.translator.rtranslatordevedition.GeneralActivity;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.api_management.ApiManagementActivity;
import nie.translator.rtranslatordevedition.diagnostics.AppDiagnostics;
import nie.translator.rtranslatordevedition.diagnostics.DiagnosticEvent;
import nie.translator.rtranslatordevedition.settings.SettingsActivity;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.tools.CustomServiceConnection;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.tools.ServiceConnectionHandle;
import nie.translator.rtranslatordevedition.tools.Tools;
import nie.translator.rtranslatordevedition.tools.gui.animations.CustomAnimator;
import nie.translator.rtranslatordevedition.tools.gui.peers.GuiPeer;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.PairingFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationService;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.main.ConversationMainFragment;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.ConversationBluetoothCommunicator;
import nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.BluetoothCapabilityEvaluator;
import com.bluetooth.communicator.BluetoothCommunicator;
import com.bluetooth.communicator.Peer;
import nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieFragment;
import nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieService;


public class VoiceTranslationActivity extends GeneralActivity {
    //flags
    public static final int NORMAL_START = 0;
    public static final int FIRST_START = 1;
    //costants
    public static final int PAIRING_FRAGMENT = 0;
    public static final int CONVERSATION_FRAGMENT = 1;
    public static final int WALKIE_TALKIE_FRAGMENT = 2;
    public static final int DEFAULT_FRAGMENT = PAIRING_FRAGMENT;
    public static final int NO_PERMISSIONS = -10;
    public static final int BLUETOOTH_UNAVAILABLE = -11;
    public static final int BLUETOOTH_DISCOVERY_UNSUPPORTED = -12;
    public static final int BLUETOOTH_LIBRARY_FAILURE = -13;
    public static final String PREF_FRAGMENT = "fragment";
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;
    private static final String BLUETOOTH_SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN";
    private static final String BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT";
    private static final String BLUETOOTH_ADVERTISE_PERMISSION = "android.permission.BLUETOOTH_ADVERTISE";
    //objects
    private Global global;
    private Fragment fragment;
    private CoordinatorLayout fragmentContainer;
    private int currentFragment = -1;
    private ArrayList<Callback> clientsCallbacks = new ArrayList<>();
    private ArrayList<CustomServiceConnection> conversationServiceConnections = new ArrayList<>();
    private ArrayList<CustomServiceConnection> walkieTalkieServiceConnections = new ArrayList<>();
    private ArrayList<ServiceConnectionHandle> pendingConversationConnections = new ArrayList<>();
    private ArrayList<ServiceConnectionHandle> pendingWalkieTalkieConnections = new ArrayList<>();
    private Handler mainHandler;  // handler that can be used to post to the main thread
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            handleBackNavigation();
        }
    };
    //variables
    private int connectionId = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        setContentView(R.layout.activity_main);
        global = (Global) getApplication();
        mainHandler = new Handler(Looper.getMainLooper());

        // Clean fragments (only if the app is recreated (When user disable permission))
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // Remove previous fragments (case of the app was restarted after changed permission on android 6 and higher)
        List<Fragment> fragmentList = fragmentManager.getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        fragmentContainer = findViewById(R.id.fragment_container);

        /*if (savedInstanceState != null) {
            //Restore the fragment's instance
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "myFragmentName");
        }*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        // when we return to the app's gui based on the service that was saved in the last closure we choose which fragment to start
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int savedFragment = sharedPreferences.getInt(PREF_FRAGMENT, DEFAULT_FRAGMENT);
        setFragment(resolveInitialFragment(savedFragment, hasActiveConversationSession()));
    }

    static int resolveInitialFragment(int savedFragment, boolean hasActiveConversationSession) {
        if (savedFragment == CONVERSATION_FRAGMENT && !hasActiveConversationSession) {
            return PAIRING_FRAGMENT;
        }
        if (savedFragment != PAIRING_FRAGMENT
                && savedFragment != CONVERSATION_FRAGMENT
                && savedFragment != WALKIE_TALKIE_FRAGMENT) {
            return PAIRING_FRAGMENT;
        }
        return savedFragment;
    }

    private boolean hasActiveConversationSession() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator != null && !communicator.getConnectedPeersList().isEmpty();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (item.getItemId() == R.id.apiManagement) {
            Intent intent = new Intent(this, ApiManagementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void setFragment(int fragmentName) {
        switch (fragmentName) {
            case PAIRING_FRAGMENT: {
                // possible stop of the Conversation and WalkieTalkie Service
                stopConversationService();
                stopWalkieTalkieService();
                // possible setting of the fragment
                if (getCurrentFragment() != PAIRING_FRAGMENT) {
                    PairingFragment paringFragment = new PairingFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    paringFragment.setArguments(bundle);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    transaction.replace(R.id.fragment_container, paringFragment);
                    transaction.commit();
                    currentFragment = PAIRING_FRAGMENT;
                    saveFragment();
                    //fragment=paringFragment;
                }
                break;
            }
            case CONVERSATION_FRAGMENT: {
                // possible setting of the fragment
                if (getCurrentFragment() != CONVERSATION_FRAGMENT) {
                    ConversationFragment conversationFragment = new ConversationFragment();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    conversationFragment.setArguments(bundle);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, conversationFragment);
                    transaction.commit();
                    currentFragment = CONVERSATION_FRAGMENT;
                    saveFragment();
                    //fragment= conversationFragment;
                }
                break;
            }
            case WALKIE_TALKIE_FRAGMENT: {
                // possible setting of the fragment
                if (getCurrentFragment() != WALKIE_TALKIE_FRAGMENT) {
                    WalkieTalkieFragment walkieTalkieFragment = new WalkieTalkieFragment();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("firstStart", true);
                    walkieTalkieFragment.setArguments(bundle);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.fragment_container, walkieTalkieFragment);
                    transaction.commit();
                    currentFragment = WALKIE_TALKIE_FRAGMENT;
                    saveFragment();
                    //fragment=walkieTalkieFragment;
                }
                break;
            }
        }
    }

    public void saveFragment() {
        persistFragmentPreference(this, getCurrentFragment());
    }

    public static void persistFragmentPreference(Context context, int fragmentName) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(PREF_FRAGMENT, fragmentName)
                .apply();
    }

    public int getCurrentFragment() {
        if (currentFragment != -1) {
            return currentFragment;
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment.getClass().equals(PairingFragment.class)) {
                    return PAIRING_FRAGMENT;
                }
                if (currentFragment.getClass().equals(ConversationFragment.class)) {
                    return CONVERSATION_FRAGMENT;
                }
                if (currentFragment.getClass().equals(WalkieTalkieFragment.class)) {
                    return WALKIE_TALKIE_FRAGMENT;
                }
            }
        }
        return -1;
    }

    public int startSearch() {
        if (!ensureNearbyPermissions()) {
            return NO_PERMISSIONS;
        }
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (communicator == null) {
            return BLUETOOTH_LIBRARY_FAILURE;
        }
        android.bluetooth.BluetoothAdapter bluetoothAdapter = communicator.getBluetoothAdapter();
        BluetoothCapabilityEvaluator.Capability preflight = BluetoothCapabilityEvaluator.evaluate(
                true,
                bluetoothAdapter != null,
                bluetoothAdapter != null && bluetoothAdapter.isEnabled(),
                BluetoothCapabilityEvaluator.SearchResult.NOT_ATTEMPTED);
        if (preflight != BluetoothCapabilityEvaluator.Capability.READY) {
            return toSearchStatus(preflight, BluetoothCommunicator.ERROR);
        }
        int searchResult = communicator.startSearch();
        BluetoothCapabilityEvaluator.Capability capability = BluetoothCapabilityEvaluator.evaluate(
                true,
                true,
                true,
                toEvaluatorSearchResult(searchResult));
        return toSearchStatus(capability, searchResult);
    }

    private BluetoothCapabilityEvaluator.SearchResult toEvaluatorSearchResult(int searchResult) {
        if (searchResult == BluetoothCommunicator.SUCCESS) {
            return BluetoothCapabilityEvaluator.SearchResult.SUCCESS;
        }
        if (searchResult == BluetoothCommunicator.ALREADY_STARTED) {
            return BluetoothCapabilityEvaluator.SearchResult.ALREADY_STARTED;
        }
        if (searchResult == BluetoothCommunicator.BLUETOOTH_LE_NOT_SUPPORTED) {
            return BluetoothCapabilityEvaluator.SearchResult.DISCOVERY_UNSUPPORTED;
        }
        return BluetoothCapabilityEvaluator.SearchResult.FAILURE;
    }

    private int toSearchStatus(BluetoothCapabilityEvaluator.Capability capability, int searchResult) {
        switch (capability) {
            case PERMISSION_MISSING:
                return NO_PERMISSIONS;
            case BLUETOOTH_UNAVAILABLE:
                return BLUETOOTH_UNAVAILABLE;
            case DISCOVERY_UNSUPPORTED:
                return BLUETOOTH_DISCOVERY_UNSUPPORTED;
            case LIBRARY_FAILURE:
                return BLUETOOTH_LIBRARY_FAILURE;
            case READY:
            default:
                return searchResult;
        }
    }

    public int stopSearch(boolean tryRestoreBluetoothStatus) {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator == null ? NO_PERMISSIONS : communicator.stopSearch(tryRestoreBluetoothStatus);
    }

    public boolean isSearching() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator != null && communicator.isSearching();
    }

    public void connect(Peer peer) {
        if (!ensureNearbyPermissions()) {
            return;
        }
        stopSearch(false);
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (communicator != null) {
            communicator.connect(peer);
        }
    }

    public void acceptConnection(Peer peer) {
        if (!ensureNearbyPermissions()) {
            return;
        }
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (communicator != null) {
            communicator.acceptConnection(peer);
        }
    }

    public void rejectConnection(Peer peer) {
        if (!ensureNearbyPermissions()) {
            return;
        }
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (communicator != null) {
            communicator.rejectConnection(peer);
        }
    }

    public ArrayList<GuiPeer> getConnectedPeersList() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator == null ? new ArrayList<GuiPeer>() : communicator.getConnectedPeersList();
    }

    public ArrayList<Peer> getConnectingPeersList() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator == null ? new ArrayList<Peer>() : communicator.getConnectingPeers();
    }

    @Nullable
    public android.bluetooth.BluetoothAdapter getBluetoothAdapter() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        return communicator == null ? null : communicator.getBluetoothAdapter();
    }

    public void disconnect(Peer peer) {
        if (!ensureNearbyPermissions()) {
            return;
        }
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (communicator != null) {
            communicator.disconnect(peer);
        }
    }

    public String[] getRequiredNearbyPermissions() {
        return getRequiredNearbyPermissionsForSdk(Build.VERSION.SDK_INT);
    }

    public static String[] getRequiredNearbyPermissionsForSdk(int sdkInt) {
        if (sdkInt >= Build.VERSION_CODES.S) {
            return new String[]{
                    BLUETOOTH_SCAN_PERMISSION,
                    BLUETOOTH_CONNECT_PERMISSION,
                    BLUETOOTH_ADVERTISE_PERMISSION,
            };
        }
        if (sdkInt >= Build.VERSION_CODES.Q) {
            return new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };
        }
        return new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };
    }

    private boolean hasNearbyPermissions() {
        return Tools.hasPermissions(this, getRequiredNearbyPermissions());
    }

    public void requestNearbyPermissions() {
        requestPermissions(getRequiredNearbyPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
    }

    private boolean ensureNearbyPermissions() {
        if (hasNearbyPermissions()) {
            return true;
        }
        requestNearbyPermissions();
        notifyMissingSearchPermission();
        return false;
    }

    @Nullable
    private ConversationBluetoothCommunicator initializeBluetoothCommunicator() {
        return hasNearbyPermissions() ? global.initializeBluetoothCommunicatorIfPermitted() : null;
    }



    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "myFragmentName", fragment);
    }*/

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        if (grantResults.length == 0 || !hasNearbyPermissions()) {
            notifyMissingSearchPermission();
            return;
        }
        if (initializeBluetoothCommunicator() == null) {
            notifyMissingSearchPermission();
            return;
        }
        attachCallbacksToBluetoothCommunicator();
        notifySearchPermissionGranted();
        //recreate();   // was called only if the grantResults were of length 0 or were neither PERMISSIONS_GRANTED nor PERMISSION_DENIED (I don't know what it is for anyway)
    }

    private void handleBackNavigation() {
        DialogInterface.OnClickListener confirmExitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitFromVoiceTranslation();
            }
        };

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (fragment instanceof ConversationFragment) {
                Fragment currentChildFragment = ((ConversationFragment) fragment).getCurrentFragment();
                if (currentChildFragment instanceof ConversationMainFragment) {
                    ConversationMainFragment conversationMainFragment = (ConversationMainFragment) currentChildFragment;
                    if (conversationMainFragment.isInputActive()) {
                        if (conversationMainFragment.isEditTextOpen()) {
                            conversationMainFragment.deleteEditText();
                        } else {
                            showConfirmExitDialog(confirmExitListener);
                        }
                    }
                } else {
                    showConfirmExitDialog(confirmExitListener);
                }
            } else if (fragment instanceof WalkieTalkieFragment) {
                WalkieTalkieFragment walkieTalkieFragment = (WalkieTalkieFragment) fragment;
                if (walkieTalkieFragment.isInputActive()) {
                    if (walkieTalkieFragment.isEditTextOpen()) {
                        walkieTalkieFragment.deleteEditText();
                    } else {
                        setFragment(DEFAULT_FRAGMENT);
                    }
                }
            } else {
                dispatchDefaultBackNavigation();
            }
        } else {
            dispatchDefaultBackNavigation();
        }
    }

    private void dispatchDefaultBackNavigation() {
        backPressedCallback.setEnabled(false);
        getOnBackPressedDispatcher().onBackPressed();
        backPressedCallback.setEnabled(true);
    }

    public void exitFromVoiceTranslation() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null && communicator.getConnectedPeersList().size() > 0) {
            communicator.disconnectFromAll();
        } else {
            setFragment(VoiceTranslationActivity.DEFAULT_FRAGMENT);
        }
    }


    // services management

    private void startConversationService(final Notification notification,
                                          final ServiceConnectionHandle handle,
                                          final Global.ResponseListener responseListener) {
        final Intent intent = new Intent(this, ConversationService.class);
        global.getLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                if (handle.isCancelled()) {
                    return;
                }
                int permissionError = getVoiceServicePermissionError(true);
                if (permissionError != 0) {
                    AppDiagnostics.recordEvent(DiagnosticEvent.Mode.CONVERSATION,
                            DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                            DiagnosticEvent.Operation.START_CONVERSATION,
                            DiagnosticEvent.ErrorCategory.PERMISSION);
                    responseListener.onFailure(new int[]{permissionError}, -1L);
                    return;
                }
                intent.putExtra("notification", notification);
                AppDiagnostics.recordEvent(DiagnosticEvent.Mode.CONVERSATION,
                        DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                        DiagnosticEvent.Operation.START_CONVERSATION,
                        DiagnosticEvent.ErrorCategory.NONE);
                try {
                    ContextCompat.startForegroundService(VoiceTranslationActivity.this, intent);
                    if (handle.markServiceStarted()) {
                        responseListener.onSuccess();
                    } else {
                        stopConversationServiceIfUnused();
                    }
                } catch (RuntimeException error) {
                    AppDiagnostics.recordFailure(DiagnosticEvent.Mode.CONVERSATION,
                            DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                            DiagnosticEvent.Operation.START_CONVERSATION, error);
                    if (!handle.isCancelled()) {
                        persistFragmentPreference(VoiceTranslationActivity.this, PAIRING_FRAGMENT);
                        responseListener.onFailure(new int[]{ErrorCodes.ERROR}, -1L);
                    }
                }
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                responseListener.onFailure(reasons, value);
            }
        });

    }

    private void startWalkieTalkieService(final Notification notification,
                                          final ServiceConnectionHandle handle,
                                          final Global.ResponseListener responseListener) {
        final Intent intent = new Intent(this, WalkieTalkieService.class);
        // initialization of the WalkieTalkieService
        global.getFirstLanguage(false, new Global.GetLocaleListener() {
            @Override
            public void onSuccess(CustomLocale result) {
                intent.putExtra("firstLanguage", result);
                global.getSecondLanguage(false, new Global.GetLocaleListener() {
                    @Override
                    public void onSuccess(CustomLocale result) {
                        if (handle.isCancelled()) {
                            return;
                        }
                        int permissionError = getVoiceServicePermissionError(false);
                        if (permissionError != 0) {
                            AppDiagnostics.recordEvent(DiagnosticEvent.Mode.WALKIE_TALKIE,
                                    DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                                    DiagnosticEvent.Operation.START_WALKIE_TALKIE,
                                    DiagnosticEvent.ErrorCategory.PERMISSION);
                            responseListener.onFailure(new int[]{permissionError}, -1L);
                            return;
                        }
                        intent.putExtra("secondLanguage", result);
                        intent.putExtra("notification", notification);
                        AppDiagnostics.recordEvent(DiagnosticEvent.Mode.WALKIE_TALKIE,
                                DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                                DiagnosticEvent.Operation.START_WALKIE_TALKIE,
                                DiagnosticEvent.ErrorCategory.NONE);
                        try {
                            ContextCompat.startForegroundService(VoiceTranslationActivity.this, intent);
                            if (handle.markServiceStarted()) {
                                responseListener.onSuccess();
                            } else {
                                stopWalkieTalkieServiceIfUnused();
                            }
                        } catch (RuntimeException error) {
                            AppDiagnostics.recordFailure(DiagnosticEvent.Mode.WALKIE_TALKIE,
                                    DiagnosticEvent.Stage.SERVICE_START_REQUESTED,
                                    DiagnosticEvent.Operation.START_WALKIE_TALKIE, error);
                            if (!handle.isCancelled()) {
                                responseListener.onFailure(new int[]{ErrorCodes.ERROR}, -1L);
                            }
                        }
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

    private int getVoiceServicePermissionError(boolean requiresBluetoothConnect) {
        boolean microphoneGranted = Tools.hasPermissions(this, Manifest.permission.RECORD_AUDIO);
        boolean bluetoothConnectGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || Tools.hasPermissions(this, BLUETOOTH_CONNECT_PERMISSION);
        return resolveVoiceServicePermissionError(
                microphoneGranted, requiresBluetoothConnect, bluetoothConnectGranted);
    }

    static int resolveVoiceServicePermissionError(boolean microphoneGranted,
                                                  boolean requiresBluetoothConnect,
                                                  boolean bluetoothConnectGranted) {
        if (!microphoneGranted) {
            return VoiceTranslationService.MISSING_MIC_PERMISSION;
        }
        if (requiresBluetoothConnect && !bluetoothConnectGranted) {
            return VoiceTranslationService.MISSING_NEARBY_PERMISSION;
        }
        return 0;
    }

    public synchronized ServiceConnectionHandle connectToConversationService(
            final VoiceTranslationService.VoiceTranslationServiceCallback callback,
            final ServiceCommunicatorListener responseListener) {
        final ServiceConnectionHandle handle = new ServiceConnectionHandle(
                callback, responseListener, new ServiceConnectionHandle.CancellationListener() {
            @Override
            public void onCancelled(ServiceConnectionHandle cancelled,
                                    CustomServiceConnection connection,
                                    boolean serviceStarted) {
                releaseConversationRequest(cancelled, connection,
                        serviceStarted && (connection == null || !connection.hasEverConnected()));
            }
        });
        pendingConversationConnections.add(handle);
        // possible start of ConversationService
        startConversationService(buildNotification(CONVERSATION_FRAGMENT), handle, new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                if (handle.isCancelled()) {
                    stopConversationServiceIfUnused();
                    return;
                }
                CustomServiceConnection conversationServiceConnection = new CustomServiceConnection(new ConversationService.ConversationServiceCommunicator(connectionId));
                connectionId++;
                if (!handle.attach(conversationServiceConnection)) {
                    releaseConversationRequest(handle, conversationServiceConnection, true);
                    return;
                }
                conversationServiceConnection.setTerminalListener(new CustomServiceConnection.TerminalListener() {
                    @Override public void onTerminal() {
                        releaseConversationRequest(handle, conversationServiceConnection,
                                !conversationServiceConnection.hasEverConnected());
                    }
                });
                boolean bound = false;
                synchronized (handle) {
                    try {
                        if (!handle.isCancelled()) {
                            bound = bindService(new Intent(VoiceTranslationActivity.this, ConversationService.class), conversationServiceConnection, BIND_ABOVE_CLIENT);
                            if (bound) {
                                bound = conversationServiceConnection.markRegistered();
                            }
                        }
                    } catch (RuntimeException error) {
                        AppDiagnostics.recordFailure(DiagnosticEvent.Mode.CONVERSATION,
                                DiagnosticEvent.Stage.IPC,
                                DiagnosticEvent.Operation.BIND_SERVICE, error);
                        // Report through the same recoverable service-connection path below.
                    }
                }
                if (bound) {
                    if (!activateConversationConnection(handle, conversationServiceConnection)) {
                        releaseConversationRequest(handle, conversationServiceConnection, true);
                    }
                } else {
                    conversationServiceConnection.reportBindFailure();
                }
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                failConversationRequest(handle, reasons, value);
            }
        });
        return handle;
    }

    public synchronized ServiceConnectionHandle connectToWalkieTalkieService(
            final VoiceTranslationService.VoiceTranslationServiceCallback callback,
            final ServiceCommunicatorListener responseListener) {
        final ServiceConnectionHandle handle = new ServiceConnectionHandle(
                callback, responseListener, new ServiceConnectionHandle.CancellationListener() {
            @Override
            public void onCancelled(ServiceConnectionHandle cancelled,
                                    CustomServiceConnection connection,
                                    boolean serviceStarted) {
                releaseWalkieTalkieRequest(cancelled, connection,
                        serviceStarted && (connection == null || !connection.hasEverConnected()));
            }
        });
        pendingWalkieTalkieConnections.add(handle);
        // possible start of WalkieTalkieService
        startWalkieTalkieService(buildNotification(WALKIE_TALKIE_FRAGMENT), handle, new Global.ResponseListener() {
            @Override
            public void onSuccess() {
                if (handle.isCancelled()) {
                    stopWalkieTalkieServiceIfUnused();
                    return;
                }
                CustomServiceConnection walkieTalkieServiceConnection = new CustomServiceConnection(new WalkieTalkieService.WalkieTalkieServiceCommunicator(connectionId));
                connectionId++;
                if (!handle.attach(walkieTalkieServiceConnection)) {
                    releaseWalkieTalkieRequest(handle, walkieTalkieServiceConnection, true);
                    return;
                }
                walkieTalkieServiceConnection.setTerminalListener(new CustomServiceConnection.TerminalListener() {
                    @Override public void onTerminal() {
                        releaseWalkieTalkieRequest(handle, walkieTalkieServiceConnection,
                                !walkieTalkieServiceConnection.hasEverConnected());
                    }
                });
                boolean bound = false;
                synchronized (handle) {
                    try {
                        if (!handle.isCancelled()) {
                            bound = bindService(new Intent(VoiceTranslationActivity.this, WalkieTalkieService.class), walkieTalkieServiceConnection, BIND_ABOVE_CLIENT);
                            if (bound) {
                                bound = walkieTalkieServiceConnection.markRegistered();
                            }
                        }
                    } catch (RuntimeException error) {
                        AppDiagnostics.recordFailure(DiagnosticEvent.Mode.WALKIE_TALKIE,
                                DiagnosticEvent.Stage.IPC,
                                DiagnosticEvent.Operation.BIND_SERVICE, error);
                        // Report through the same recoverable service-connection path below.
                    }
                }
                if (bound) {
                    if (!activateWalkieTalkieConnection(handle, walkieTalkieServiceConnection)) {
                        releaseWalkieTalkieRequest(handle, walkieTalkieServiceConnection, true);
                    }
                } else {
                    walkieTalkieServiceConnection.reportBindFailure();
                }
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                failWalkieTalkieRequest(handle, reasons, value);
            }
        });
        return handle;
    }

    private synchronized boolean activateConversationConnection(ServiceConnectionHandle handle,
                                                                  CustomServiceConnection connection) {
        synchronized (handle) {
            if (handle.isCancelled()) {
                return false;
            }
            pendingConversationConnections.remove(handle);
            conversationServiceConnections.add(connection);
            return true;
        }
    }

    private synchronized boolean activateWalkieTalkieConnection(ServiceConnectionHandle handle,
                                                                  CustomServiceConnection connection) {
        synchronized (handle) {
            if (handle.isCancelled()) {
                return false;
            }
            pendingWalkieTalkieConnections.remove(handle);
            walkieTalkieServiceConnections.add(connection);
            return true;
        }
    }

    private synchronized void failConversationRequest(ServiceConnectionHandle handle,
                                                       int[] reasons, long value) {
        pendingConversationConnections.remove(handle);
        boolean stopOrphan = handle.wasServiceStarted();
        handle.fail(reasons, value);
        if (stopOrphan) {
            stopConversationServiceIfUnused();
        }
    }

    private synchronized void failWalkieTalkieRequest(ServiceConnectionHandle handle,
                                                       int[] reasons, long value) {
        pendingWalkieTalkieConnections.remove(handle);
        boolean stopOrphan = handle.wasServiceStarted();
        handle.fail(reasons, value);
        if (stopOrphan) {
            stopWalkieTalkieServiceIfUnused();
        }
    }

    private synchronized void releaseConversationRequest(ServiceConnectionHandle handle,
                                                           CustomServiceConnection connection,
                                                           boolean stopOrphan) {
        pendingConversationConnections.remove(handle);
        if (connection != null) {
            releaseConnection(conversationServiceConnections, connection);
        }
        handle.markTerminal();
        if (stopOrphan) {
            stopConversationServiceIfUnused();
        }
    }

    private synchronized void releaseWalkieTalkieRequest(ServiceConnectionHandle handle,
                                                          CustomServiceConnection connection,
                                                          boolean stopOrphan) {
        pendingWalkieTalkieConnections.remove(handle);
        if (connection != null) {
            releaseConnection(walkieTalkieServiceConnections, connection);
        }
        handle.markTerminal();
        if (stopOrphan) {
            stopWalkieTalkieServiceIfUnused();
        }
    }

    private synchronized void releaseConnection(ArrayList<CustomServiceConnection> connections,
                                                CustomServiceConnection serviceConnection) {
        connections.remove(serviceConnection);
        serviceConnection.disconnect(new CustomServiceConnection.Unbinder() {
            @Override public void unbind(android.content.ServiceConnection connection) {
                unbindService(connection);
            }
        });
    }

    private synchronized void stopConversationServiceIfUnused() {
        if (conversationServiceConnections.isEmpty() && pendingConversationConnections.isEmpty()) {
            stopConversationService();
        }
    }

    private synchronized void stopWalkieTalkieServiceIfUnused() {
        if (walkieTalkieServiceConnections.isEmpty() && pendingWalkieTalkieConnections.isEmpty()) {
            stopWalkieTalkieService();
        }
    }

    public void stopConversationService() {
        stopService(new Intent(this, ConversationService.class));
    }

    public void stopWalkieTalkieService() {
        stopService(new Intent(this, WalkieTalkieService.class));
    }

    //notification
    private Notification buildNotification(int clickAction) {
        String channelID = "service_background_notification";
        String channelName = getResources().getString(R.string.notification_channel_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        // creation of the click on the notification
        Intent resultIntent = new Intent(this, VoiceTranslationActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // creation of the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        if (clickAction == CONVERSATION_FRAGMENT) {
            builder.setContentTitle("Conversation")
                    .setContentText("Conversation mode is running...")
                    .setContentIntent(resultPendingIntent)
                    .setSmallIcon(R.drawable.mic_icon)
                    .setOngoing(true)
                    .setChannelId(channelID)
                    .build();
        } else {
            builder.setContentTitle("WalkieTalkie")
                    .setContentText("WalkieTalkie mode is running...")
                    .setContentIntent(resultPendingIntent)
                    .setSmallIcon(R.drawable.mic_icon)
                    .setOngoing(true)
                    .setChannelId(channelID)
                    .build();
        }
        return builder.build();
    }


    public void addCallback(Callback callback) {
        boolean added = false;
        if (!clientsCallbacks.contains(callback)) {
            clientsCallbacks.add(callback);
            added = true;
        }
        ConversationBluetoothCommunicator communicator = initializeBluetoothCommunicator();
        if (added && communicator != null) {
            communicator.addCallback(callback);
        }
    }

    public void removeCallback(Callback callback) {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null) {
            communicator.removeCallback(callback);
        }
        clientsCallbacks.remove(callback);
    }

    private void attachCallbacksToBluetoothCommunicator() {
        ConversationBluetoothCommunicator communicator = global.getBluetoothCommunicator();
        if (communicator != null) {
            for (Callback callback : clientsCallbacks) {
                communicator.addCallback(callback);
            }
        }
    }

    private void notifyMissingSearchPermission() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onMissingSearchPermission();
                }
            }
        });
    }

    private void notifySearchPermissionGranted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < clientsCallbacks.size(); i++) {
                    clientsCallbacks.get(i).onSearchPermissionGranted();
                }
            }
        });
    }

    public CoordinatorLayout getFragmentContainer() {
        return fragmentContainer;
    }

    public static class Callback extends ConversationBluetoothCommunicator.Callback {
        public void onMissingSearchPermission() {
        }

        public void onSearchPermissionGranted() {
        }
    }
}
