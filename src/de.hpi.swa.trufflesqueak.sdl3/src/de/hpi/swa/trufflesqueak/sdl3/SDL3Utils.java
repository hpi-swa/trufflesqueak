/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetError;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RunOnMainThread;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicReference;

import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_MainThreadCallback;

public final class SDL3Utils {
    private SDL3Utils() {
    }

    public static String getSDLError() {
        return "SDL error encountered: " + SDL_GetError().getString(0);
    }

    public static void checkSdlError(final boolean success) {
        if (!success) {
            throw new IllegalStateException(getSDLError());
        }
    }

    public static float checkSdlError(final float value) {
        if (value == 0.0f) {
            throw new IllegalStateException(getSDLError());
        }
        return value;
    }

    public static MemorySegment checkSdlError(final MemorySegment segment) {
        if (segment == null || isNull(segment)) {
            throw new IllegalStateException(getSDLError());
        }
        return segment;
    }

    public static boolean isNull(final MemorySegment segment) {
        return segment.address() == 0;
    }

    /** Keep the returned stub alive until all asynchronous calls have completed. */
    public static MemorySegment mainThreadCallback(final SDL_MainThreadCallback.Function task) {
        return SDL_MainThreadCallback.allocate(data -> {
            try {
                task.apply(data);
            } catch (final Throwable failure) {
                // Exceptions must never escape a native upcall: that terminates the JVM.
                try {
                    warning("Main-thread callback failed: " + failure);
                } catch (final Throwable loggingFailure) {
                    // Even a failure while reporting the error must stay inside the upcall.
                }
            }
        }, Arena.ofAuto());
    }

    public static void runOnMainThread(final SDL_MainThreadCallback.Function task, final MemorySegment data) {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment callback = SDL_MainThreadCallback.allocate(argument -> {
                try {
                    task.apply(argument);
                } catch (final Throwable exception) {
                    failure.set(exception);
                }
            }, arena);
            checkSdlError(SDL_RunOnMainThread(callback, data, true));
        }
        final Throwable cause = failure.get();
        if (cause instanceof final RuntimeException exception) {
            throw exception;
        } else if (cause instanceof final Error error) {
            throw error;
        } else if (cause != null) {
            throw new IllegalStateException("Main-thread callback failed", cause);
        }
    }

    public static void runOnMainThreadAsync(final MemorySegment callback, final MemorySegment data) {
        checkSdlError(SDL_RunOnMainThread(callback, data, false));
    }

    /** Cannot use TruffleLogger on main thread. */
    public static void warning(final String message) {
        // Checkstyle: stop
        System.err.println("[SDL3 warning] " + message);
        // Checkstyle: start
    }
}
