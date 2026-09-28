/*
 * Copyright 2026 Mini Conversation contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.bluetooth.communicator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, order-checking accumulator for the legacy Bluetooth fragment protocol. */
final class InboundMessageAssembler {
    enum Status {
        ACCEPTED,
        COMPLETE,
        REJECTED
    }

    static final class Result {
        final Status status;
        @Nullable final byte[] payload;

        private Result(Status status, @Nullable byte[] payload) {
            this.status = status;
            this.payload = payload;
        }

        static Result accepted() {
            return new Result(Status.ACCEPTED, null);
        }

        static Result complete(byte[] payload) {
            return new Result(Status.COMPLETE, payload);
        }

        static Result rejected() {
            return new Result(Status.REJECTED, null);
        }
    }

    private static final class Assembly {
        final ByteArrayOutputStream bytes;
        int fragmentCount;
        int lastSequence;
        long lastUpdatedAtMs;
        byte[] lastFragment;

        Assembly(int firstCapacity, long nowMs) {
            bytes = new ByteArrayOutputStream(firstCapacity);
            lastUpdatedAtMs = nowMs;
        }
    }

    private final int maxPayloadBytes;
    private final int maxFragments;
    private final int maxInFlight;
    private final long timeoutMs;
    private final LinkedHashMap<String, Assembly> assemblies = new LinkedHashMap<>();

    InboundMessageAssembler(int maxPayloadBytes, int maxFragments, int maxInFlight, long timeoutMs) {
        if (maxPayloadBytes < 1 || maxFragments < 1 || maxInFlight < 1 || timeoutMs < 1) {
            throw new IllegalArgumentException("All inbound limits must be positive");
        }
        this.maxPayloadBytes = maxPayloadBytes;
        this.maxFragments = maxFragments;
        this.maxInFlight = maxInFlight;
        this.timeoutMs = timeoutMs;
    }

    @NonNull
    synchronized Result accept(@NonNull String id, int sequence, boolean isFinal,
                               @NonNull byte[] fragment, long nowMs) {
        expire(nowMs);
        if (id.length() == 0 || sequence < 0 || fragment.length == 0
                || fragment.length > maxPayloadBytes) {
            assemblies.remove(id);
            return Result.rejected();
        }

        Assembly assembly = assemblies.get(id);
        if (assembly == null) {
            if (sequence != 0 || assemblies.size() >= maxInFlight) {
                return Result.rejected();
            }
            assembly = new Assembly(Math.min(fragment.length, maxPayloadBytes), nowMs);
            assemblies.put(id, assembly);
        } else if (sequence == assembly.lastSequence
                && Arrays.equals(fragment, assembly.lastFragment)) {
            assembly.lastUpdatedAtMs = nowMs;
            return Result.accepted();
        } else if (sequence != assembly.lastSequence + 1) {
            assemblies.remove(id);
            return Result.rejected();
        }

        if (assembly.fragmentCount + 1 > maxFragments
                || assembly.bytes.size() > maxPayloadBytes - fragment.length) {
            assemblies.remove(id);
            return Result.rejected();
        }

        assembly.bytes.write(fragment, 0, fragment.length);
        assembly.fragmentCount++;
        assembly.lastSequence = sequence;
        assembly.lastUpdatedAtMs = nowMs;
        assembly.lastFragment = Arrays.copyOf(fragment, fragment.length);
        if (!isFinal) {
            return Result.accepted();
        }

        assemblies.remove(id);
        return Result.complete(assembly.bytes.toByteArray());
    }

    synchronized void expire(long nowMs) {
        Iterator<Map.Entry<String, Assembly>> iterator = assemblies.entrySet().iterator();
        while (iterator.hasNext()) {
            Assembly assembly = iterator.next().getValue();
            if (nowMs - assembly.lastUpdatedAtMs >= timeoutMs) {
                iterator.remove();
            }
        }
    }

    synchronized int inFlightCount() {
        return assemblies.size();
    }

    synchronized void clear() {
        assemblies.clear();
    }
}
