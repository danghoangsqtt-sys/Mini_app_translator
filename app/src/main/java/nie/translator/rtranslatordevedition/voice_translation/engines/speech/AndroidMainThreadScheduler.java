package nie.translator.rtranslatordevedition.voice_translation.engines.speech;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Uses the application main looper and never retains a UI lifecycle object. */
final class AndroidMainThreadScheduler implements MainThreadScheduler {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void execute(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    @Override public void executeAndWait(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        final CountDownLatch complete = new CountDownLatch(1);
        final AtomicReference<RuntimeException> failure = new AtomicReference<>();
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    runnable.run();
                } catch (RuntimeException error) {
                    failure.set(error);
                } finally {
                    complete.countDown();
                }
            }
        });
        try {
            complete.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the main thread", interrupted);
        }
        if (failure.get() != null) { throw failure.get(); }
    }

    @Override public void schedule(Runnable runnable, long delayMillis) {
        handler.postDelayed(runnable, delayMillis);
    }

    @Override public void cancel(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}
