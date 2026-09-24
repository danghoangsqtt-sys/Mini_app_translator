package nie.translator.rtranslatordevedition.voice_translation.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import org.junit.Test;

public class EngineOperationTest {
    @Test public void completionWinsOnlyOnce() {
        EngineOperation operation = new EngineOperation(null);
        AtomicInteger callbacks = new AtomicInteger();
        assertTrue(operation.complete(new Runnable() { @Override public void run() { callbacks.incrementAndGet(); } }));
        assertFalse(operation.complete(new Runnable() { @Override public void run() { callbacks.incrementAndGet(); } }));
        assertEquals(1, callbacks.get());
    }

    @Test public void cancelledOperationRejectsLateCallbacks() {
        EngineOperation operation = new EngineOperation(null);
        AtomicInteger callbacks = new AtomicInteger();
        assertTrue(operation.cancel());
        assertFalse(operation.dispatch(new Runnable() { @Override public void run() { callbacks.incrementAndGet(); } }));
        assertFalse(operation.complete(new Runnable() { @Override public void run() { callbacks.incrementAndGet(); } }));
        assertEquals(0, callbacks.get());
    }

    @Test public void cancelActionRunsAtMostOnce() {
        AtomicInteger cancels = new AtomicInteger();
        EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() { @Override public void onCancel() { cancels.incrementAndGet(); } });
        assertTrue(operation.cancel());
        assertFalse(operation.cancel());
        assertEquals(1, cancels.get());
    }

    @Test public void cancelBlocksDispatchAdmittedAfterItsLinearizationPoint() throws Exception {
        final EngineOperation operation = new EngineOperation(null);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final AtomicBoolean dispatched = new AtomicBoolean(true);
        Thread canceller = new Thread(new Runnable() {
            @Override public void run() { operation.cancel(); cancelled.countDown(); }
        });
        Thread dispatcher = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    assertTrue(cancelled.await(2, TimeUnit.SECONDS));
                    dispatched.set(operation.dispatch(new Runnable() { @Override public void run() { } }));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        canceller.start();
        dispatcher.start();
        canceller.join();
        dispatcher.join();
        assertFalse(dispatched.get());
    }

    @Test public void completionAndCancelHaveOnlyOneWinnerUnderContention() throws Exception {
        final AtomicInteger callbacks = new AtomicInteger();
        final AtomicInteger cancels = new AtomicInteger();
        final EngineOperation operation = new EngineOperation(new EngineOperation.CancelAction() {
            @Override public void onCancel() { cancels.incrementAndGet(); }
        });
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        Thread completer = new Thread(new Runnable() {
            @Override public void run() {
                ready.countDown();
                await(start);
                operation.complete(new Runnable() { @Override public void run() { callbacks.incrementAndGet(); } });
            }
        });
        Thread canceller = new Thread(new Runnable() {
            @Override public void run() {
                ready.countDown();
                await(start);
                operation.cancel();
            }
        });
        completer.start();
        canceller.start();
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        completer.join();
        canceller.join();
        assertEquals(1, callbacks.get() + cancels.get());
    }

    @Test public void recognitionRequestAllowsEngineCaptureWithoutCallerSampleRate() {
        SpeechRecognitionEngine.RecognitionRequest request = new SpeechRecognitionEngine.RecognitionRequest(
                new CustomLocale("en", "US"), 0, SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE);
        assertEquals(0, request.getSampleRateHertz());
    }

    @Test(expected = IllegalArgumentException.class) public void recognitionRequestRequiresSampleRateForPcmStream() {
        new SpeechRecognitionEngine.RecognitionRequest(
                new CustomLocale("en", "US"), 0, SpeechRecognitionEngine.AudioInputMode.PCM_STREAM);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
