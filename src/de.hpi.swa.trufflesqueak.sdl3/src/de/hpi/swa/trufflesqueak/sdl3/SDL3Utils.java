/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetError;

import java.lang.foreign.MemorySegment;

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

    public static MemorySegment checkSdlError(final MemorySegment value) {
        if (value == null || value == MemorySegment.NULL) {
            throw new IllegalStateException(getSDLError());
        }
        return value;
    }

    /** Cannot use TruffleLogger on main thread. */
    public static void warning(final String message) {
        // Checkstyle: stop
        System.err.println("[SDL3 warning] " + message);
        // Checkstyle: start
    }
}
