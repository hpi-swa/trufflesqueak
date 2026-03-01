/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_FINGER_DOWN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_FINGER_MOTION;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_FINGER_UP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_FIRST;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_LAST;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_TEXT_EDITING;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_USER;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_GETEVENT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_HINT_MAC_BACKGROUND_APP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_HINT_RENDER_VSYNC;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_HINT_VIDEO_X11_NET_WM_PING;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_INIT_VIDEO;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.checkSdlError;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.warning;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_Init;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_PROP_APP_METADATA_IDENTIFIER_STRING;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_PROP_APP_METADATA_NAME_STRING;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_PROP_APP_METADATA_VERSION_STRING;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_PeepEvents;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_PushEvent;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_Quit;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetAppMetadataProperty;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetEventEnabled;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetHint;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_WaitEvent;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_Event;

public final class PlatformEventLoop {
    private static final int EVENT_FETCH_BATCH_SIZE = 32;
    private static final CountDownLatch startLatch = new CountDownLatch(1);
    private static volatile boolean isRunning = false;
    private static volatile boolean shutdownRequested = false;
    private static volatile Consumer<MemorySegment> eventHandler;
    private static volatile Runnable renderFrame;
    private static final MemorySegment wakeUpEvent;

    static {
        wakeUpEvent = SDL_Event.allocate(Arena.global());
        SDL_Event.type(wakeUpEvent, SDL_EVENT_USER);
    }

    public static void start(final Consumer<MemorySegment> theEventHandler, final Runnable theRenderFrame) {
        eventHandler = theEventHandler;
        renderFrame = theRenderFrame;
        startLatch.countDown();
    }

    public static void stop() {
        shutdownRequested = true;

        // Unblock the main thread if it is currently waiting for the display to initialize
        // (e.g., during an early fatal VM crash).
        startLatch.countDown();

        if (isRunning) {
            isRunning = false;
            pushWakeUpEvent();
        }
    }

    private static void pushWakeUpEvent() {
        if (!SDL_PushEvent(wakeUpEvent)) {
            warning("Failed to push wake up event");
        }
    }

    public static void wakeUp() {
        if (isRunning) {
            pushWakeUpEvent();
        }
    }

    public static void run() {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Exit immediately if a shutdown was requested while we were waiting on the latch.
        if (shutdownRequested) {
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            // Setup App Metadata using older properties (compatible with LWJGL's SDL3 binary)
            checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_NAME_STRING(), arena.allocateFrom("TruffleSqueak")));
            checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_VERSION_STRING(), arena.allocateFrom(SqueakLanguageConfig.VERSION)));
            checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_IDENTIFIER_STRING(), arena.allocateFrom("de.hpi.swa.trufflesqueak")));

            // Setup Hints
            checkSdlError(SDL_SetHint(arena.allocateFrom(SDL_HINT_RENDER_VSYNC), arena.allocateFrom("1")));
            checkSdlError(SDL_SetHint(arena.allocateFrom(SDL_HINT_VIDEO_X11_NET_WM_PING), arena.allocateFrom("0")));
            checkSdlError(SDL_SetHint(arena.allocateFrom(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK), arena.allocateFrom("1")));
            checkSdlError(SDL_SetHint(arena.allocateFrom(SDL_HINT_MAC_BACKGROUND_APP), arena.allocateFrom("0")));

            // Initialize SDL
            checkSdlError(SDL_Init(SDL_INIT_VIDEO));

            // Disable unneeded events
            SDL_SetEventEnabled(SDL_EVENT_TEXT_EDITING, false);
            SDL_SetEventEnabled(SDL_EVENT_FINGER_DOWN, false);
            SDL_SetEventEnabled(SDL_EVENT_FINGER_UP, false);
            SDL_SetEventEnabled(SDL_EVENT_FINGER_MOTION, false);

            /*
             * Mark the loop as running BEFORE checking the shutdown flag again. If stop() is called
             * from another thread at this exact nanosecond, this ordering ensures we don't
             * accidentally get trapped in the while-loop below.
             */
            isRunning = true;
            if (shutdownRequested) {
                isRunning = false;
            } else {
                wakeUp();
            }

            // Allocate a contiguous buffer for fetching multiple events
            final long eventSize = SDL_Event.layout().byteSize();
            final MemorySegment eventBuffer = SDL_Event.allocateArray(EVENT_FETCH_BATCH_SIZE, arena);
            final MemorySegment firstEvent = eventBuffer.asSlice(0, eventSize);

            while (isRunning) {
                // Sleep until an event (or wakeUp() ping) arrives
                if (SDL_WaitEvent(firstEvent)) {
                    eventHandler.accept(firstEvent);
                    // Peep additional events from the queue into our buffer
                    int eventsRead;
                    while ((eventsRead = SDL_PeepEvents(eventBuffer, EVENT_FETCH_BATCH_SIZE, SDL_GETEVENT, SDL_EVENT_FIRST, SDL_EVENT_LAST)) > 0) {
                        for (int i = 0; i < eventsRead; i++) {
                            // Slice the buffer to get a pointer to the i-th SDL_Event struct
                            final MemorySegment nextEvent = eventBuffer.asSlice(i * eventSize, eventSize);
                            eventHandler.accept(nextEvent);
                        }
                    }
                    renderFrame.run();
                }
            }

            // Event loop has been stopped -- tear down SDL.
            SDL_Quit();
        }
    }
}
