/*
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package nie.translator.rtranslatordevedition.tools;

import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCallback;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;

/** Owns one service connection request from initiation through cancellation. */
public final class ServiceConnectionHandle {
    public interface CancellationListener {
        void onCancelled(ServiceConnectionHandle handle,
                         CustomServiceConnection connection,
                         boolean serviceStarted);
    }

    private ServiceCallback serviceCallback;
    private ServiceCommunicatorListener responseListener;
    private CancellationListener cancellationListener;
    private CustomServiceConnection connection;
    private boolean serviceStarted;
    private boolean cancelled;
    private boolean terminal;

    public ServiceConnectionHandle(ServiceCallback serviceCallback,
                                   ServiceCommunicatorListener responseListener,
                                   CancellationListener cancellationListener) {
        this.serviceCallback = serviceCallback;
        this.responseListener = responseListener;
        this.cancellationListener = cancellationListener;
    }

    /** Records that the foreground-service launch returned without throwing. */
    public synchronized boolean markServiceStarted() {
        serviceStarted = true;
        return !cancelled && !terminal;
    }

    /** Transfers UI callbacks to the real framework connection exactly once. */
    public synchronized boolean attach(CustomServiceConnection value) {
        if (cancelled || terminal || connection != null) {
            return false;
        }
        connection = value;
        value.addCallbacks(serviceCallback, responseListener);
        serviceCallback = null;
        responseListener = null;
        return true;
    }

    /** Delivers a failure only while the initiating owner is still active. */
    public void fail(int[] reasons, long value) {
        ServiceCommunicatorListener listener;
        synchronized (this) {
            if (cancelled || terminal) {
                clearReferences();
                return;
            }
            terminal = true;
            listener = responseListener;
            clearReferences();
        }
        if (listener != null) {
            listener.onFailure(reasons, value);
        }
    }

    public void cancel() {
        CancellationListener listener;
        CustomServiceConnection attached;
        boolean started;
        synchronized (this) {
            if (cancelled || terminal) {
                return;
            }
            cancelled = true;
            listener = cancellationListener;
            attached = connection;
            started = serviceStarted;
            clearReferences();
        }
        if (listener != null) {
            listener.onCancelled(this, attached, started);
        }
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public synchronized boolean wasServiceStarted() {
        return serviceStarted;
    }

    /** Clears ownership after bind failure/death or explicit Activity release. */
    public synchronized void markTerminal() {
        terminal = true;
        clearReferences();
    }

    private void clearReferences() {
        serviceCallback = null;
        responseListener = null;
        cancellationListener = null;
        connection = null;
    }
}
