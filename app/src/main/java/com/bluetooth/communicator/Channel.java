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

package com.bluetooth.communicator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bluetooth.communicator.tools.CustomCountDownTimer;
import com.bluetooth.communicator.tools.Timer;

import java.util.ArrayDeque;

abstract class Channel {
    static final int MAX_TEXT_BYTES = 64 * 1024;
    static final int MAX_DATA_BYTES = 256 * 1024;
    static final int MAX_FRAGMENTS = 1024;
    static final int MAX_IN_FLIGHT_PER_STREAM = 4;
    static final long INBOUND_TIMEOUT_MS = 10_000L;
    private static final int MAX_RECENT_IDS = 32;
    //timeouts of timers
    private final int RECONNECTION_TIMEOUT = 30000;
    private final int CONNECTION_COMPLETE_TIMEOUT = 10000;
    protected final int MESSAGE_TIMEOUT = 1000;
    private final int NOTIFY_DISCONNECTION_TIMEOUT = 5000;
    private final int DISCONNECTION_TIMEOUT = 4000;
    //variables and objects
    protected Context context;
    @NonNull
    private Peer peer;
    private BluetoothMessage.SequenceNumber messageID;
    private BluetoothMessage.SequenceNumber dataID;
    @Nullable
    protected ArrayDeque<BluetoothMessage> pendingMessage;
    @Nullable
    protected ArrayDeque<BluetoothMessage> pendingData;
    private final InboundMessageAssembler receivingMessages = new InboundMessageAssembler(
            MAX_TEXT_BYTES, MAX_FRAGMENTS, MAX_IN_FLIGHT_PER_STREAM, INBOUND_TIMEOUT_MS);
    private final InboundMessageAssembler receivingData = new InboundMessageAssembler(
            MAX_DATA_BYTES, MAX_FRAGMENTS, MAX_IN_FLIGHT_PER_STREAM, INBOUND_TIMEOUT_MS);
    private final ArrayDeque<String> receivedMessageIds = new ArrayDeque<>();
    private final ArrayDeque<String> receivedDataIds = new ArrayDeque<>();
    private Timer connectionCompleteTimer;
    private Timer reconnectionTimer;
    private Timer messageTimer;
    private Timer dataTimer;
    private Timer notifyDisconnectionTimer;
    private Timer disconnectionTimer;
    protected Handler mainHandler;
    private Handler messageHandler;
    private Handler dataHandler;
    private MessageCallback messageCallback;
    private MessageCallback dataCallback;
    @Nullable
    protected DisconnectionNotificationCallback disconnectionNotificationCallback;
    private boolean notifyingDisconnection = false;
    private boolean disconnecting = false;
    protected boolean messagesPaused = false;
    protected boolean dataPaused = false;
    protected final Object lock = new Object();
    private boolean inboundCleanupScheduled;
    private final Runnable inboundCleanup = new Runnable() {
        @Override
        public void run() {
            synchronized (lock) {
                inboundCleanupScheduled = false;
                long now = SystemClock.elapsedRealtime();
                receivingMessages.expire(now);
                receivingData.expire(now);
                scheduleInboundCleanupIfNeeded();
            }
        }
    };

    protected Channel(Context context, @NonNull Peer peer) {
        this.context = context;
        this.messageID= new BluetoothMessage.SequenceNumber(context,BluetoothMessage.ID_LENGTH);
        this.dataID= new BluetoothMessage.SequenceNumber(context,BluetoothMessage.ID_LENGTH);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.messageHandler = new Handler(Looper.getMainLooper());
        this.dataHandler = new Handler(Looper.getMainLooper());
        this.peer = peer;
    }


    public void writeMessage(Message message, MessageCallback callback) {
        synchronized (lock) {
            if (pendingMessage == null) {       // if it is true then we are not writing any messages
                // division from the message and sending of the various parts
                pendingMessage = message.splitInBluetoothMessages(messageID);
                messageID.increment();
                messageCallback = callback;
                writeSubMessage();
            }
        }
    }

    public void writeData(Message data, MessageCallback callback) {
        synchronized (lock) {
            if (pendingData == null) {       // if it is true then we are not writing any messages
                // division from the message and sending of the various parts
                pendingData = data.splitInBluetoothMessages(dataID);
                dataID.increment();
                dataCallback = callback;
                writeSubData();
            }
        }
    }

    public void onSubMessageWriteSuccess() {
        synchronized (lock) {
            BluetoothMessage subMessageSent = null;
            if (pendingMessage != null) {
                subMessageSent = pendingMessage.pollFirst();  // remove the newly sent subMessage
            }
            resetMessageTimer();
            if (subMessageSent != null && subMessageSent.getType() == BluetoothMessage.FINAL) {
                pendingMessage = null;   // remove the newly sent ConversationMessage
                notifyMessageSent();
            } else {
                writeSubMessage();
            }
        }
    }

    public void onSubMessageWriteFailed() {
        writeSubMessage();
    }

    public void onSubDataWriteSuccess() {
        synchronized (lock) {
            BluetoothMessage subDataSent = null;
            if (pendingData != null) {
                subDataSent = pendingData.pollFirst();  // remove the newly sent subMessage
            }
            resetDataTimer();
            if (subDataSent != null && subDataSent.getType() == BluetoothMessage.FINAL) {
                pendingData = null;   // remove the newly sent ConversationMessage
                notifyDataSent();
            } else {
                writeSubData();
            }
        }
    }

    public void onSubDataWriteFailed() {
        writeSubData();
    }

    public void pausePendingMessage() {
        synchronized (lock) {
            if (!messagesPaused) {
                // delete all runnables in messageHandler to prevent the pause from being activated again
                messageHandler.removeCallbacksAndMessages(null);
                messagesPaused = true;
            }
        }
    }

    public void resumePendingMessage() {
        synchronized (lock) {
            if (messagesPaused) {
                // delete all runnables in messageHandler to ensure that it can not be run multiple times
                messageHandler.removeCallbacksAndMessages(null);
                messageHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            messagesPaused = false;
                        }
                    }
                }, 1500);
            }
        }
    }

    public void pausePendingData() {
        synchronized (lock) {
            if (!dataPaused) {
                // delete all runnables in dataHandler to prevent the pause from being activated again
                dataHandler.removeCallbacksAndMessages(null);
                dataPaused = true;
            }
        }
    }

    public void resumePendingData() {
        synchronized (lock) {
            if (dataPaused) {
                // delete all runnables in dataHandler to ensure that it can not be run multiple times
                dataHandler.removeCallbacksAndMessages(null);
                dataHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            dataPaused = false;
                        }
                    }
                }, 1500);
            }
        }
    }

    public void setPeer(@NonNull Peer peer) {
        this.peer = peer;
    }

    protected abstract void writeSubMessage();

    protected abstract void writeSubData();

    public abstract void readPhy();

    public abstract boolean notifyNameUpdated(String name);

    public Timer getReconnectionTimer() {
        return reconnectionTimer;
    }

    @NonNull
    public Peer getPeer() {
        return peer;
    }

    @Nullable
    public BluetoothMessage getPendingSubMessage() {
        synchronized (lock) {
            if (pendingMessage != null) {
                return pendingMessage.peekFirst();
            } else {
                return null;
            }
        }
    }

    @Nullable
    public BluetoothMessage getPendingSubData() {
        synchronized (lock) {
            if (pendingData != null) {
                return pendingData.peekFirst();
            } else {
                return null;
            }
        }
    }

    @Nullable
    public Message acceptIncomingFragment(@NonNull BluetoothMessage fragment, boolean dataStream) {
        synchronized (lock) {
            String id = fragment.getId().getValue();
            ArrayDeque<String> completedIds = dataStream ? receivedDataIds : receivedMessageIds;
            if (completedIds.contains(id)) {
                return null;
            }
            InboundMessageAssembler assembler = dataStream ? receivingData : receivingMessages;
            InboundMessageAssembler.Result result = assembler.accept(
                    id,
                    fragment.getSequenceNumber().toOrdinal(),
                    fragment.getType() == BluetoothMessage.FINAL,
                    fragment.getData(),
                    SystemClock.elapsedRealtime());
            scheduleInboundCleanupIfNeeded();
            if (result.status != InboundMessageAssembler.Status.COMPLETE || result.payload == null) {
                return null;
            }
            completedIds.addLast(id);
            while (completedIds.size() > MAX_RECENT_IDS) {
                completedIds.removeFirst();
            }
            return fragment.convertPayloadInMessage(result.payload);
        }
    }

    private void scheduleInboundCleanupIfNeeded() {
        if (!inboundCleanupScheduled
                && (receivingMessages.inFlightCount() > 0 || receivingData.inFlightCount() > 0)) {
            inboundCleanupScheduled = true;
            mainHandler.postDelayed(inboundCleanup, INBOUND_TIMEOUT_MS);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Channel) {
            return peer.equals(((Channel) obj).getPeer());
        }
        if (obj instanceof Peer) {
            return peer.equals(obj);
        }
        return false;
    }

    protected boolean notifyDisconnection(final DisconnectionNotificationCallback disconnectionNotificationCallback) {
        synchronized (lock) {
            if (!notifyingDisconnection) {
                notifyingDisconnection = true;
                getPeer().setDisconnecting(true);
                this.disconnectionNotificationCallback = disconnectionNotificationCallback;
                // start of notify disconnection timer
                startNotifyDisconnectionTimer(new Timer.Callback() {
                    @Override
                    public void onFinished() {
                        disconnect(new DisconnectionCallback() {
                            @Override
                            public void onDisconnectionFailed() {
                                disconnectionNotificationCallback.onDisconnectionFailed();
                            }
                        });
                    }
                });
                // stop sending messages
                notifyMessageSent();
                notifyDataSent();
                return true;
            }
            return false;
        }
    }

    public boolean disconnect(final DisconnectionCallback callback) {
        synchronized (lock) {
            if (!disconnecting) {
                disconnecting = true;
                getPeer().setDisconnecting(true);
                // stop of notify disconnection timer
                resetNotifyDisconnectionTimer();

                if (disconnectionNotificationCallback != null) {
                    disconnectionNotificationCallback.onDisconnectionNotificationSent();
                    disconnectionNotificationCallback = null;
                }
                if (getPeer().isHardwareConnected()) {
                    // start disconnection timer
                    startDisconnectionTimer(new Timer.Callback() {
                        @Override
                        public void onFinished() {
                            callback.onDisconnectionFailed();
                        }
                    });
                } else {
                    callback.onAlreadyDisconnected(getPeer());
                }
                // stop sending messages
                notifyMessageSent();
                notifyDataSent();
                return true;
            }
            return false;
        }
    }

    public void onDisconnected() {
        resetDisconnectionTimer();
    }

    public void destroy() {
        synchronized (lock) {
            resetMessageTimer();
            resetDataTimer();
            resetReconnectionTimer();
            resetConnectionCompleteTimer();
            resetDisconnectionTimer();
            resetNotifyDisconnectionTimer();
            messageHandler.removeCallbacksAndMessages(null);
            dataHandler.removeCallbacksAndMessages(null);
            mainHandler.removeCallbacks(inboundCleanup);
            inboundCleanupScheduled = false;
            receivingMessages.clear();
            receivingData.clear();
            receivedMessageIds.clear();
            receivedDataIds.clear();
            pendingMessage = null;
            pendingData = null;
            disconnectionNotificationCallback = null;
            messageCallback = null;
            dataCallback = null;
        }
    }


    private void notifyMessageSent() {
        if (messageCallback != null) {
            // this is done because onMessageSent must be the last operation performed by this method since the latter sends subsequent messages
            MessageCallback oldCallback = messageCallback;
            messageCallback = null;
            oldCallback.onMessageSent();
        }
    }

    private void notifyDataSent() {
        if (dataCallback != null) {
            // this is done because onMessageSent must be the last operation performed by this method since the latter sends subsequent messages
            MessageCallback oldCallback = dataCallback;
            dataTimer = null;
            oldCallback.onMessageSent();
        }
    }


    public void startConnectionCompleteTimer(final Timer.Callback callback) {
        synchronized (lock) {
            connectionCompleteTimer = new Timer(CONNECTION_COMPLETE_TIMEOUT);
            connectionCompleteTimer.setCallback(callback);
            connectionCompleteTimer.start();
        }
    }

    public void resetConnectionCompleteTimer() {
        synchronized (lock) {
            if (connectionCompleteTimer != null) {
                connectionCompleteTimer.cancel();
                connectionCompleteTimer = null;
            }
        }
    }

    public void startReconnectionTimer(final Timer.Callback callback) {
        synchronized (lock) {
            reconnectionTimer = new Timer(RECONNECTION_TIMEOUT);
            reconnectionTimer.setCallback(callback);
            reconnectionTimer.start();
        }
    }

    public void resetReconnectionTimer() {
        synchronized (lock) {
            if (reconnectionTimer != null) {
                reconnectionTimer.cancel();
                reconnectionTimer = null;
            }
        }
    }

    protected void startMessageTimer(final Timer.Callback callback) {
        synchronized (lock) {
            messageTimer = new Timer(MESSAGE_TIMEOUT);
            messageTimer.setCallback(callback);
            messageTimer.start();
        }
    }

    private void resetMessageTimer() {
        synchronized (lock) {
            if (messageTimer != null) {
                messageTimer.cancel();
                messageTimer = null;
            }
        }
    }

    protected void startDataTimer(final Timer.Callback callback) {
        synchronized (lock) {
            dataTimer = new Timer(MESSAGE_TIMEOUT);
            dataTimer.setCallback(callback);
            dataTimer.start();
        }
    }

    private void resetDataTimer() {
        synchronized (lock) {
            if (dataTimer != null) {
                dataTimer.cancel();
                dataTimer = null;
            }
        }
    }

    private void startNotifyDisconnectionTimer(final Timer.Callback callback) {
        synchronized (lock) {
            notifyDisconnectionTimer = new Timer(NOTIFY_DISCONNECTION_TIMEOUT);
            notifyDisconnectionTimer.setCallback(callback);
            notifyDisconnectionTimer.start();
        }
    }

    private void resetNotifyDisconnectionTimer() {
        synchronized (lock) {
            if (notifyDisconnectionTimer != null) {
                notifyDisconnectionTimer.cancel();
                notifyDisconnectionTimer = null;
            }
        }
    }

    private void startDisconnectionTimer(final Timer.Callback callback) {
        synchronized (lock) {
            disconnectionTimer = new Timer(DISCONNECTION_TIMEOUT);
            disconnectionTimer.setCallback(callback);
            disconnectionTimer.start();
        }
    }

    private void resetDisconnectionTimer() {
        synchronized (lock) {
            if (disconnectionTimer != null) {
                disconnectionTimer.cancel();
                disconnectionTimer = null;
            }
        }
    }

    public static abstract class MessageCallback {
        public abstract void onMessageSent();
    }

    public static abstract class DisconnectionNotificationCallback {
        public abstract void onDisconnectionNotificationSent();

        public void onDisconnectionFailed() {
        }
    }

    public static abstract class DisconnectionCallback {
        public void onAlreadyDisconnected(Peer peer) {
        }

        public abstract void onDisconnectionFailed();
    }
}
