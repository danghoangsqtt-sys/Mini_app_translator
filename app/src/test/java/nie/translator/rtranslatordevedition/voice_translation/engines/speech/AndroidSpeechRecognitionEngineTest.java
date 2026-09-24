package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineError;
import nie.translator.rtranslatordevedition.voice_translation.engines.EngineOperation;
import nie.translator.rtranslatordevedition.voice_translation.engines.SpeechRecognitionEngine;
import org.junit.Test;

public class AndroidSpeechRecognitionEngineTest {
    @Test public void prefersOnDeviceAndForwardsOneBoundedRequest() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeFactory factory = new FakeFactory(true, true);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, factory);
        Result result = new Result();
        engine.start(request(), result);
        assertEquals(1, factory.onDeviceCreates);
        assertEquals(0, factory.systemCreates);
        assertTrue(factory.onDevice.started);
        factory.onDevice.finalResult("ok", .8f);
        assertEquals("ok", result.text);
        assertTrue(result.finalResult);
        assertEquals(1, factory.onDevice.destroyCalls);
        assertEquals(0, factory.onDevice.cancelCalls);
    }

    @Test public void constructionFailureFallsBackOnlyBeforeListening() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeFactory factory = new FakeFactory(true, true);
        factory.failOnDeviceCreate = true;
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, factory);
        engine.start(request(), new Result());
        assertEquals(1, factory.onDeviceCreates);
        assertEquals(1, factory.systemCreates);
        factory.system.error(1);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void noSystemOrOnDeviceServiceFailsRecoverably() {
        FakeFactory factory = new FakeFactory(false, false);
        Result result = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request(), result);
        assertEquals(EngineError.Category.MISSING_DEPENDENCY, result.error.getCategory());
    }

    @Test public void pcmStreamIsRejectedWithoutConstructingRecognizer() {
        FakeFactory factory = new FakeFactory(true, true);
        Result result = new Result();
        SpeechRecognitionEngine.RecognitionRequest pcm = new SpeechRecognitionEngine.RecognitionRequest(
                new CustomLocale("en", "US"), 16000, SpeechRecognitionEngine.AudioInputMode.PCM_STREAM);
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(pcm, result);
        assertEquals(EngineError.Category.UNSUPPORTED, result.error.getCategory());
        assertEquals(0, factory.systemCreates + factory.onDeviceCreates);
    }

    @Test public void finishStopsOnceAndWaitsForTerminalCallback() {
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory);
        Result result = new Result();
        EngineOperation operation = engine.start(request(), result);
        assertTrue(engine.finish(operation));
        assertTrue(engine.finish(operation));
        assertEquals(1, factory.system.stopCalls);
        assertEquals(0, result.calls);
        factory.system.finalResult("done", .4f);
        assertEquals(1, result.calls);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void cancelAndLateCallbackAreSuppressedWithExactOnceCleanup() {
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory);
        Result result = new Result();
        EngineOperation operation = engine.start(request(), result);
        assertTrue(operation.cancel());
        factory.system.finalResult("late", .1f);
        assertEquals(0, result.calls);
        assertEquals(1, factory.system.cancelCalls);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void timeoutCancelsAndDestroysRecognizer() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeFactory factory = new FakeFactory(true, false);
        Result result = new Result();
        new AndroidSpeechRecognitionEngine(scheduler, factory).start(request(), result);
        scheduler.runDelayed();
        assertEquals(EngineError.Category.INTERNAL_FAILURE, result.error.getCategory());
        assertEquals(1, factory.system.cancelCalls);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void closeAndOverlapAreLifecycleSafe() {
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory);
        engine.start(request(), new Result());
        Result overlap = new Result();
        engine.start(request(), overlap);
        assertEquals(EngineError.Category.BUSY, overlap.error.getCategory());
        engine.close();
        assertEquals(1, factory.system.cancelCalls);
        assertEquals(1, factory.system.destroyCalls);
        Result afterClose = new Result();
        engine.start(request(), afterClose);
        assertEquals(EngineError.Category.CLOSED, afterClose.error.getCategory());
    }

    @Test public void capabilityDisclosureCoversAllAvailabilityCombinations() {
        assertTrue(new AndroidSpeechRecognitionEngine(new FakeScheduler(), new FakeFactory(true, true))
                .getCapability().getDetail().contains("fallback may use network"));
        assertEquals("On-device speech recognition is available", new AndroidSpeechRecognitionEngine(
                new FakeScheduler(), new FakeFactory(false, true)).getCapability().getDetail());
        assertTrue(new AndroidSpeechRecognitionEngine(new FakeScheduler(), new FakeFactory(true, false))
                .getCapability().getDetail().contains("may use network"));
        assertEquals(nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState.UNSUPPORTED,
                new AndroidSpeechRecognitionEngine(new FakeScheduler(), new FakeFactory(false, false)).getCapability().getState());
    }

    @Test public void partialThenFinalForwardsLanguageAndDoesNotRestartSystemRecognizer() {
        FakeFactory factory = new FakeFactory(true, true);
        Result result = new Result();
        SpeechRecognitionEngine.RecognitionRequest request = request();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request, result);
        factory.onDevice.partialResult("part", .2f);
        factory.onDevice.finalResult("final", .7f);
        assertEquals(2, result.calls);
        assertEquals("final", result.text);
        assertTrue(result.finalResult);
        assertEquals(request, factory.onDevice.request);
        assertEquals(0, factory.systemCreates);
    }

    @Test public void securityExceptionFromStartIsPermissionAndStillCleansUp() {
        FakeFactory factory = new FakeFactory(true, false);
        factory.system.failStartWithSecurity = true;
        Result result = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request(), result);
        assertEquals(EngineError.Category.PERMISSION, result.error.getCategory());
        assertEquals(1, factory.system.cancelCalls);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void cleanupExceptionsDoNotEscapeOrSuppressTerminalCallback() {
        FakeFactory factory = new FakeFactory(true, false);
        factory.system.throwOnDestroy = true;
        Result result = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request(), result);
        factory.system.finalResult("done", .5f);
        assertEquals(1, result.calls);
        assertEquals("done", result.text);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void cancelExceptionStillDestroysExactlyOnceAndSuppressesLateCallbacks() {
        FakeFactory factory = new FakeFactory(true, false);
        factory.system.throwOnCancel = true;
        Result result = new Result();
        EngineOperation operation = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request(), result);
        assertTrue(operation.cancel());
        factory.system.finalResult("late", .1f);
        assertEquals(0, result.calls);
        assertEquals(1, factory.system.cancelCalls);
        assertEquals(1, factory.system.destroyCalls);
    }

    @Test public void startedOnDeviceErrorNeverCreatesOrRetriesSystemRecognizer() {
        FakeFactory factory = new FakeFactory(true, true);
        Result result = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory).start(request(), result);
        factory.onDevice.error(5);
        assertEquals(EngineError.Category.INTERNAL_FAILURE, result.error.getCategory());
        assertEquals(1, factory.onDeviceCreates);
        assertEquals(0, factory.systemCreates);
        assertEquals(1, factory.onDevice.destroyCalls);
    }

    @Test public void lateSessionCallbackCannotAffectReplacementSession() {
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory);
        Result first = new Result();
        engine.start(request(), first);
        SpeechRecognizerClient.Listener staleListener = factory.system.listener;
        factory.system.finalResult("first", .4f);
        Result second = new Result();
        engine.start(request(), second);
        staleListener.onFinalResult("late", .2f);
        assertEquals(0, second.calls);
        factory.system.finalResult("second", .8f);
        assertEquals("second", second.text);
        assertEquals(1, second.calls);
    }

    @Test public void cancelBeforeQueuedBeginNeverCreatesRecognizer() {
        QueuedScheduler scheduler = new QueuedScheduler();
        FakeFactory factory = new FakeFactory(true, false);
        EngineOperation operation = new AndroidSpeechRecognitionEngine(scheduler, factory).start(request(), new Result());
        assertEquals(0, factory.systemCreates);
        assertTrue(operation.cancel());
        scheduler.runAll();
        assertEquals(0, factory.systemCreates);
        assertFalse(factory.system.started);
    }

    @Test public void closeBeforeQueuedBeginNeverCreatesRecognizer() {
        QueuedScheduler scheduler = new QueuedScheduler();
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, factory);
        engine.start(request(), new Result());
        engine.close();
        scheduler.runAll();
        assertEquals(0, factory.systemCreates);
        assertFalse(factory.system.started);
    }

    @Test public void terminalCallbackRunsOutsideLockAndCanStartReplacementSession() {
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(new FakeScheduler(), factory);
        Result replacement = new Result();
        final EngineOperation[] replacementOperation = new EngineOperation[1];
        engine.start(request(), new SpeechRecognitionEngine.RecognitionCallback() {
            @Override public void onResult(String text, CustomLocale language, float confidence, boolean isFinal) {
                if (isFinal) { replacementOperation[0] = engine.start(request(), replacement); }
            }
            @Override public void onFailure(EngineError error) { }
        });
        factory.system.finalResult("done", .5f);
        assertTrue(replacementOperation[0].isActive());
        assertEquals(2, factory.systemCreates);
        factory.system.finalResult("replacement", .7f);
        assertEquals("replacement", replacement.text);
    }

    @Test public void watchdogIsRemovedForEveryTerminalPathAndCannotCallbackTwice() {
        assertWatchdogRemoved(new TerminalAction() {
            @Override public void end(AndroidSpeechRecognitionEngine engine, FakeFactory factory, EngineOperation operation) {
                factory.system.finalResult("done", .5f);
            }
        });
        assertWatchdogRemoved(new TerminalAction() {
            @Override public void end(AndroidSpeechRecognitionEngine engine, FakeFactory factory, EngineOperation operation) {
                factory.system.error(5);
            }
        });
        assertWatchdogRemoved(new TerminalAction() {
            @Override public void end(AndroidSpeechRecognitionEngine engine, FakeFactory factory, EngineOperation operation) {
                operation.cancel();
            }
        });
        assertWatchdogRemoved(new TerminalAction() {
            @Override public void end(AndroidSpeechRecognitionEngine engine, FakeFactory factory, EngineOperation operation) {
                engine.close();
            }
        });
    }

    @Test public void securityExceptionFromClientConstructionMapsToPermission() {
        FakeFactory onDeviceFactory = new FakeFactory(false, true);
        onDeviceFactory.failOnDeviceCreateWithSecurity = true;
        Result onDeviceResult = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), onDeviceFactory).start(request(), onDeviceResult);
        assertEquals(EngineError.Category.PERMISSION, onDeviceResult.error.getCategory());
        assertEquals(0, onDeviceFactory.systemCreates);

        FakeFactory systemFactory = new FakeFactory(true, false);
        systemFactory.failSystemCreateWithSecurity = true;
        Result systemResult = new Result();
        new AndroidSpeechRecognitionEngine(new FakeScheduler(), systemFactory).start(request(), systemResult);
        assertEquals(EngineError.Category.PERMISSION, systemResult.error.getCategory());
    }

    @Test public void rejectedCallbacksAreDispatchedByScheduler() {
        QueuedScheduler scheduler = new QueuedScheduler();
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, factory);
        engine.start(request(), new Result());
        Result busy = new Result();
        engine.start(request(), busy);
        SpeechRecognitionEngine.RecognitionRequest pcm = new SpeechRecognitionEngine.RecognitionRequest(
                new CustomLocale("en", "US"), 16000, SpeechRecognitionEngine.AudioInputMode.PCM_STREAM);
        Result pcmResult = new Result();
        engine.start(pcm, pcmResult);
        engine.close();
        Result closed = new Result();
        engine.start(request(), closed);
        assertEquals(0, busy.calls + pcmResult.calls + closed.calls);
        scheduler.runAll();
        assertEquals(EngineError.Category.BUSY, busy.error.getCategory());
        assertEquals(EngineError.Category.UNSUPPORTED, pcmResult.error.getCategory());
        assertEquals(EngineError.Category.CLOSED, closed.error.getCategory());
    }

    @Test public void capabilityProbeReturnsClosedWhenCloseWinsRace() throws Exception {
        BlockingCapabilityScheduler scheduler = new BlockingCapabilityScheduler();
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, new FakeFactory(true, true));
        final nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability[] result =
                new nie.translator.rtranslatordevedition.voice_translation.engines.EngineCapability[1];
        Thread probe = new Thread(new Runnable() {
            @Override public void run() { result[0] = engine.getCapability(); }
        });
        probe.start();
        assertTrue(scheduler.awaitProbe());
        engine.close();
        scheduler.releaseProbe();
        probe.join(2_000L);
        assertFalse(probe.isAlive());
        assertEquals(nie.translator.rtranslatordevedition.voice_translation.engines.CapabilityState.CLOSED,
                result[0].getState());
    }

    private static void assertWatchdogRemoved(TerminalAction action) {
        FakeScheduler scheduler = new FakeScheduler();
        FakeFactory factory = new FakeFactory(true, false);
        AndroidSpeechRecognitionEngine engine = new AndroidSpeechRecognitionEngine(scheduler, factory);
        Result result = new Result();
        EngineOperation operation = engine.start(request(), result);
        assertEquals(1, scheduler.delayedCount());
        action.end(engine, factory, operation);
        int callsAfterTerminal = result.calls;
        assertEquals(0, scheduler.delayedCount());
        scheduler.runDelayed();
        assertEquals(callsAfterTerminal, result.calls);
    }

    private interface TerminalAction {
        void end(AndroidSpeechRecognitionEngine engine, FakeFactory factory, EngineOperation operation);
    }

    private static SpeechRecognitionEngine.RecognitionRequest request() {
        return new SpeechRecognitionEngine.RecognitionRequest(new CustomLocale("en", "US"), 0,
                SpeechRecognitionEngine.AudioInputMode.ENGINE_CAPTURE);
    }

    private static final class Result implements SpeechRecognitionEngine.RecognitionCallback {
        private int calls;
        private String text;
        private boolean finalResult;
        private EngineError error;
        @Override public void onResult(String text, CustomLocale language, float confidence, boolean isFinal) {
            calls++; this.text = text; finalResult = isFinal;
        }
        @Override public void onFailure(EngineError error) { calls++; this.error = error; }
    }

    private static final class FakeScheduler implements MainThreadScheduler {
        private final List<Runnable> delayed = new ArrayList<>();
        @Override public void execute(Runnable runnable) { runnable.run(); }
        @Override public void executeAndWait(Runnable runnable) { runnable.run(); }
        @Override public void schedule(Runnable runnable, long delayMillis) { delayed.add(runnable); }
        @Override public void cancel(Runnable runnable) { delayed.remove(runnable); }
        private void runDelayed() { for (Runnable runnable : new ArrayList<>(delayed)) { runnable.run(); } }
        private int delayedCount() { return delayed.size(); }
    }

    private static final class QueuedScheduler implements MainThreadScheduler {
        private final List<Runnable> queued = new ArrayList<>();
        private final List<Runnable> delayed = new ArrayList<>();
        @Override public void execute(Runnable runnable) { queued.add(runnable); }
        @Override public void executeAndWait(Runnable runnable) { runnable.run(); }
        @Override public void schedule(Runnable runnable, long delayMillis) { delayed.add(runnable); }
        @Override public void cancel(Runnable runnable) { delayed.remove(runnable); }
        private void runAll() {
            while (!queued.isEmpty()) {
                Runnable runnable = queued.remove(0);
                runnable.run();
            }
        }
    }

    private static final class BlockingCapabilityScheduler implements MainThreadScheduler {
        private final CountDownLatch probeStarted = new CountDownLatch(1);
        private final CountDownLatch permitProbe = new CountDownLatch(1);
        @Override public void execute(Runnable runnable) { runnable.run(); }
        @Override public void executeAndWait(Runnable runnable) {
            probeStarted.countDown();
            try {
                if (!permitProbe.await(2L, TimeUnit.SECONDS)) { throw new IllegalStateException("test probe timed out"); }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("test probe interrupted", interrupted);
            }
            runnable.run();
        }
        @Override public void schedule(Runnable runnable, long delayMillis) { }
        @Override public void cancel(Runnable runnable) { }
        private boolean awaitProbe() throws InterruptedException { return probeStarted.await(2L, TimeUnit.SECONDS); }
        private void releaseProbe() { permitProbe.countDown(); }
    }

    private static final class FakeFactory implements SpeechRecognizerClientFactory {
        private final boolean systemAvailable;
        private final boolean onDeviceAvailable;
        private final FakeClient system = new FakeClient();
        private final FakeClient onDevice = new FakeClient();
        private boolean failOnDeviceCreate;
        private boolean failOnDeviceCreateWithSecurity;
        private boolean failSystemCreateWithSecurity;
        private int systemCreates;
        private int onDeviceCreates;
        private FakeFactory(boolean systemAvailable, boolean onDeviceAvailable) {
            this.systemAvailable = systemAvailable;
            this.onDeviceAvailable = onDeviceAvailable;
        }
        @Override public boolean isSystemRecognitionAvailable() { return systemAvailable; }
        @Override public boolean isOnDeviceRecognitionAvailable() { return onDeviceAvailable; }
        @Override public SpeechRecognizerClient createOnDevice() {
            onDeviceCreates++;
            if (failOnDeviceCreateWithSecurity) { throw new SecurityException(); }
            if (failOnDeviceCreate) { throw new UnsupportedOperationException(); }
            return onDevice;
        }
        @Override public SpeechRecognizerClient createSystem() {
            systemCreates++;
            if (failSystemCreateWithSecurity) { throw new SecurityException(); }
            return system;
        }
    }

    private static final class FakeClient implements SpeechRecognizerClient {
        private Listener listener;
        private boolean started;
        private int stopCalls;
        private int cancelCalls;
        private int destroyCalls;
        private SpeechRecognitionEngine.RecognitionRequest request;
        private boolean throwOnCancel;
        private boolean throwOnDestroy;
        private boolean failStartWithSecurity;
        @Override public void start(SpeechRecognitionEngine.RecognitionRequest request, Listener listener) {
            started = true; this.request = request; this.listener = listener;
            if (failStartWithSecurity) { throw new SecurityException(); }
        }
        @Override public void stop() { stopCalls++; }
        @Override public void cancel() { cancelCalls++; if (throwOnCancel) { throw new IllegalStateException(); } }
        @Override public void destroy() { destroyCalls++; if (throwOnDestroy) { throw new IllegalStateException(); } }
        private void partialResult(String text, float confidence) { listener.onPartialResult(text, confidence); }
        private void finalResult(String text, float confidence) { listener.onFinalResult(text, confidence); }
        private void error(int code) { listener.onError(code); }
    }
}
