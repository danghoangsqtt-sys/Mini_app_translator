/*
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package nie.translator.rtranslatordevedition.voice_translation._conversation_mode.communication.recent_peer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Runs one task at a time while bounding retained pending work. A pending keyed
 * task may be replaced in-place, so the latest same-key state survives bursts
 * without changing its order relative to other keys.
 */
final class BoundedSerialExecutor {
    enum SubmitResult { ACCEPTED, COALESCED, REJECTED, CLOSED }

    private static final class Entry {
        final String key;
        Runnable task;

        Entry(String key, Runnable task) {
            this.key = key;
            this.task = task;
        }
    }

    private final Executor worker;
    private final int maxPending;
    private final ArrayDeque<Entry> queue = new ArrayDeque<>();
    private final Map<String, Entry> keyedEntries = new HashMap<>();
    private boolean drainScheduled;
    private boolean closed;

    BoundedSerialExecutor(Executor worker, int maxPending) {
        if (worker == null || maxPending < 1) {
            throw new IllegalArgumentException("worker and positive capacity are required");
        }
        this.worker = worker;
        this.maxPending = maxPending;
    }

    SubmitResult submit(String key, boolean coalesce, Runnable task) {
        boolean scheduleDrain = false;
        SubmitResult result;
        Entry added = null;
        synchronized (this) {
            if (closed) {
                return SubmitResult.CLOSED;
            }
            if (coalesce) {
                Entry existing = keyedEntries.get(key);
                if (existing != null) {
                    existing.task = task;
                    return SubmitResult.COALESCED;
                }
            }
            if (queue.size() >= maxPending) {
                return SubmitResult.REJECTED;
            }
            added = new Entry(key, task);
            queue.addLast(added);
            if (coalesce) {
                keyedEntries.put(key, added);
            }
            result = SubmitResult.ACCEPTED;
            if (!drainScheduled) {
                drainScheduled = true;
                scheduleDrain = true;
            }
        }
        if (scheduleDrain) {
            final Entry scheduledEntry = added;
            try {
                worker.execute(new Runnable() {
                    @Override public void run() { drain(); }
                });
            } catch (RuntimeException rejected) {
                synchronized (this) {
                    queue.remove(scheduledEntry);
                    if (keyedEntries.get(key) == scheduledEntry) {
                        keyedEntries.remove(key);
                    }
                    drainScheduled = false;
                }
                return SubmitResult.REJECTED;
            }
        }
        return result;
    }

    private void drain() {
        while (true) {
            Entry entry;
            synchronized (this) {
                if (closed) {
                    drainScheduled = false;
                    return;
                }
                entry = queue.pollFirst();
                if (entry == null) {
                    drainScheduled = false;
                    return;
                }
                if (keyedEntries.get(entry.key) == entry) {
                    keyedEntries.remove(entry.key);
                }
            }
            try {
                entry.task.run();
            } catch (RuntimeException ignored) {
                // Repository tasks contain/report their own failures. Keep the serial lane alive.
            }
        }
    }

    synchronized int pendingCount() {
        return queue.size();
    }

    int shutdownNow() {
        int dropped;
        synchronized (this) {
            if (closed) {
                return 0;
            }
            closed = true;
            dropped = queue.size();
            queue.clear();
            keyedEntries.clear();
        }
        if (worker instanceof ExecutorService) {
            ((ExecutorService) worker).shutdownNow();
        }
        return dropped;
    }
}
