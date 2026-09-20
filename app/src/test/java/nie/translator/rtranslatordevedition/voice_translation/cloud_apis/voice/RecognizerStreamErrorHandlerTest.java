/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.voice;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Status;
import nie.translator.rtranslatordevedition.tools.ErrorCodes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecognizerStreamErrorHandlerTest {
    @Test
    public void onError_mapsStatusAndNotifiesOncePerStream() {
        final List<Integer> reasons = new ArrayList<>();
        RecognizerStreamErrorHandler handler = new RecognizerStreamErrorHandler(
                new RecognizerStreamErrorHandler.Listener() {
                    @Override
                    public void onStreamError(long streamId, int reason) {
                        reasons.add(reason);
                    }
                });

        long networkStream = handler.onStreamStarted();
        assertTrue(handler.onError(networkStream, Status.UNAVAILABLE.asRuntimeException()));
        assertFalse(handler.onError(networkStream, Status.INTERNAL.asRuntimeException()));

        long authenticationStream = handler.onStreamStarted();
        assertTrue(handler.onError(authenticationStream,
                Status.UNAUTHENTICATED.asRuntimeException()));

        long internalStream = handler.onStreamStarted();
        assertTrue(handler.onError(internalStream, Status.INTERNAL.asRuntimeException()));

        assertEquals(3, reasons.size());
        assertEquals(ErrorCodes.MISSED_CONNECTION, reasons.get(0).intValue());
        assertEquals(ErrorCodes.WRONG_API_KEY, reasons.get(1).intValue());
        assertEquals(ErrorCodes.ERROR, reasons.get(2).intValue());
    }

    @Test
    public void completedOrDestroyedStream_ignoresLateError() {
        final List<Integer> reasons = new ArrayList<>();
        RecognizerStreamErrorHandler handler = new RecognizerStreamErrorHandler(
                new RecognizerStreamErrorHandler.Listener() {
                    @Override
                    public void onStreamError(long streamId, int reason) {
                        reasons.add(reason);
                    }
                });

        long completedStream = handler.onStreamStarted();
        assertTrue(handler.onStreamFinished(completedStream));
        assertFalse(handler.onStreamFinished(completedStream));
        assertFalse(handler.onError(completedStream, Status.CANCELLED.asRuntimeException()));
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void staleStreamError_doesNotTerminateReplacementStream() {
        final List<Integer> reasons = new ArrayList<>();
        RecognizerStreamErrorHandler handler = new RecognizerStreamErrorHandler(
                new RecognizerStreamErrorHandler.Listener() {
                    @Override
                    public void onStreamError(long streamId, int reason) {
                        reasons.add(reason);
                    }
                });

        long firstStream = handler.onStreamStarted();
        long replacementStream = handler.onStreamStarted();

        assertFalse(handler.onError(firstStream, Status.CANCELLED.asRuntimeException()));
        assertTrue(handler.onError(replacementStream, Status.UNAVAILABLE.asRuntimeException()));
        assertEquals(1, reasons.size());
        assertEquals(ErrorCodes.MISSED_CONNECTION, reasons.get(0).intValue());
    }
}
