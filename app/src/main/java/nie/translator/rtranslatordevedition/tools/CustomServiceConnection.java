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

package nie.translator.rtranslatordevedition.tools;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import java.util.ArrayList;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCallback;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicator;
import nie.translator.rtranslatordevedition.tools.services_communication.ServiceCommunicatorListener;

public class CustomServiceConnection implements ServiceConnection {
    public interface Unbinder {
        void unbind(ServiceConnection connection);
    }

    public interface TerminalListener {
        void onTerminal();
    }

    private ServiceCommunicator serviceCommunicator;
    private ArrayList<ServiceCallback> callbacksToAddOnBind = new ArrayList<>();
    private ArrayList<ServiceCommunicatorListener> callbacksToRespondOnBind = new ArrayList<>();
    private boolean registered;
    private boolean released;
    private boolean connected;
    private boolean terminalFailureReported;
    private TerminalListener terminalListener;

    public CustomServiceConnection(ServiceCommunicator serviceCommunicator){
        this.serviceCommunicator=serviceCommunicator;
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder iBinder) {
        if (released || connected) {
            return;
        }
        connected = true;
        serviceCommunicator.initializeCommunication(new Messenger(iBinder));
        for(int i = 0; i< callbacksToAddOnBind.size(); i++) {
            serviceCommunicator.addCallback(callbacksToAddOnBind.get(i));
        }
        for(int i = 0; i< callbacksToRespondOnBind.size(); i++) {
            ServiceCommunicatorListener responseListener1= callbacksToRespondOnBind.get(i);
            if(responseListener1!=null) {
                responseListener1.onServiceCommunicator(serviceCommunicator);
            }
        }
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        connected = false;
        disconnectCommunication();
    }

    @Override
    public void onBindingDied(ComponentName name) {
        reportTerminalFailure();
    }

    @Override
    public void onNullBinding(ComponentName name) {
        reportTerminalFailure();
    }

    public synchronized boolean markRegistered() {
        if (released || registered) {
            return false;
        }
        registered = true;
        return true;
    }

    public void reportBindFailure() {
        reportTerminalFailure();
    }

    public void disconnect(Unbinder unbinder) {
        boolean shouldUnbind;
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
            shouldUnbind = registered;
            registered = false;
            connected = false;
        }
        if (shouldUnbind) {
            try {
                unbinder.unbind(this);
            } catch (RuntimeException ignored) {
                // The framework may have already removed a dead registration.
            }
        }
        disconnectCommunication();
    }

    public synchronized boolean isRegistered() {
        return registered && !released;
    }

    public synchronized void setTerminalListener(TerminalListener listener) {
        terminalListener = listener;
    }

    private synchronized void reportTerminalFailure() {
        connected = false;
        disconnectCommunication();
        if (terminalFailureReported || released) {
            return;
        }
        terminalFailureReported = true;
        for (ServiceCommunicatorListener listener : callbacksToRespondOnBind) {
            if (listener != null) {
                listener.onFailure(new int[]{ErrorCodes.ERROR}, -1L);
            }
        }
        if (terminalListener != null) {
            terminalListener.onTerminal();
        }
    }

    private void disconnectCommunication(){
        for(int i=0;i<callbacksToAddOnBind.size();i++) {
            serviceCommunicator.removeCallback(callbacksToAddOnBind.get(i));
        }
        serviceCommunicator.stopCommunication();
    }

    public ServiceCommunicator getServiceCommunicator() {
        return serviceCommunicator;
    }

    public void addCallbacks(ServiceCallback serviceCallback, ServiceCommunicatorListener responseListener){
        callbacksToAddOnBind.add(serviceCallback);
        callbacksToRespondOnBind.add(responseListener);
    }
}
