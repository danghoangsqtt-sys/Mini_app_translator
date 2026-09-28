package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Test;

public class BoundedSerialExecutorTest {
    @Test public void latestSameKeyReplacesPendingWorkInPlace() {
        ManualExecutor worker = new ManualExecutor();
        BoundedSerialExecutor executor = new BoundedSerialExecutor(worker, 2);
        List<Integer> values = new ArrayList<>();

        assertEquals(BoundedSerialExecutor.SubmitResult.ACCEPTED,
                executor.submit("peer", true, () -> values.add(1)));
        assertEquals(BoundedSerialExecutor.SubmitResult.COALESCED,
                executor.submit("peer", true, () -> values.add(2)));
        assertEquals(1, executor.pendingCount());
        assertEquals(1, worker.size());

        worker.runAll();
        assertEquals(java.util.Collections.singletonList(2), values);
    }

    @Test public void distinctOverflowRejectsButExistingKeyStillCoalesces() {
        ManualExecutor worker = new ManualExecutor();
        BoundedSerialExecutor executor = new BoundedSerialExecutor(worker, 2);

        assertEquals(BoundedSerialExecutor.SubmitResult.ACCEPTED,
                executor.submit("a", true, () -> { }));
        assertEquals(BoundedSerialExecutor.SubmitResult.ACCEPTED,
                executor.submit("b", true, () -> { }));
        assertEquals(BoundedSerialExecutor.SubmitResult.REJECTED,
                executor.submit("c", true, () -> { }));
        assertEquals(BoundedSerialExecutor.SubmitResult.COALESCED,
                executor.submit("a", true, () -> { }));
        assertEquals(2, executor.pendingCount());
    }

    @Test public void taskFailureDoesNotKillFollowingWork() {
        ManualExecutor worker = new ManualExecutor();
        BoundedSerialExecutor executor = new BoundedSerialExecutor(worker, 2);
        final int[] completed = { 0 };
        executor.submit("bad", false, () -> { throw new IllegalStateException("boom"); });
        executor.submit("good", false, () -> completed[0]++);

        worker.runAll();
        assertEquals(1, completed[0]);
    }

    @Test public void shutdownDropsPendingAndRejectsNewWork() {
        ManualExecutor worker = new ManualExecutor();
        BoundedSerialExecutor executor = new BoundedSerialExecutor(worker, 2);
        executor.submit("a", true, () -> { });
        executor.submit("b", true, () -> { });

        assertEquals(2, executor.shutdownNow());
        assertEquals(0, executor.pendingCount());
        assertEquals(BoundedSerialExecutor.SubmitResult.CLOSED,
                executor.submit("c", true, () -> { }));
        assertEquals(0, executor.shutdownNow());
    }

    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable command) { tasks.add(command); }
        int size() { return tasks.size(); }
        void runAll() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
    }
}
