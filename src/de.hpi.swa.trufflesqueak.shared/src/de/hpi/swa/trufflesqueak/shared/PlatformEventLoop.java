package de.hpi.swa.trufflesqueak.shared;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FIRST;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_LAST;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_EDITING;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_USER;
import static org.lwjgl.sdl.SDLEvents.SDL_GETEVENT;
import static org.lwjgl.sdl.SDLEvents.SDL_PeepEvents;
import static org.lwjgl.sdl.SDLEvents.SDL_PushEvent;
import static org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled;
import static org.lwjgl.sdl.SDLEvents.SDL_WaitEvent;
import static org.lwjgl.sdl.SDLEvents.SDL_WaitEventTimeout;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_RENDER_VSYNC;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_VIDEO_X11_NET_WM_PING;
import static org.lwjgl.sdl.SDLHints.SDL_SetHint;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_COPYRIGHT_STRING;
import static org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_CREATOR_STRING;
import static org.lwjgl.sdl.SDLInit.SDL_PROP_APP_METADATA_URL_STRING;
import static org.lwjgl.sdl.SDLInit.SDL_SetAppMetadata;
import static org.lwjgl.sdl.SDLInit.SDL_SetAppMetadataProperty;
import static org.lwjgl.sdl.SDLStdinc.SDL_SetMemoryFunctions;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.util.function.Consumer;

import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class PlatformEventLoop {
    // This sets the upper bound on physical screen redraws.
    private static final int EVENT_WAIT_TIMEOUT_MS = 5;

    private static final int EVENT_FETCH_BATCH_SIZE = 32;

    public static volatile Consumer<SDL_Event> osEventHandler = null;
    public static volatile Runnable renderFrameIfNeeded = null;
    public static volatile boolean isRunning = false;

    // Break the main thread's wait from any background thread
    public static void wakeUp() {
        if (!isRunning) {
            return;
        }
        try (MemoryStack stack = stackPush()) {
            final SDL_Event wakeupEvent = SDL_Event.calloc(stack);
            wakeupEvent.type(SDL_EVENT_USER);  // osEventHandler will ignore event
            SDL_PushEvent(wakeupEvent);
        }
    }

    public static void start() {
        isRunning = true;
    }

    public static void run() {
        SDL_SetMemoryFunctions(
                        MemoryUtil::nmemAllocChecked,
                        MemoryUtil::nmemCallocChecked,
                        MemoryUtil::nmemReallocChecked,
                        MemoryUtil::nmemFree);

        checkSdlError(SDL_SetAppMetadata("TruffleSqueak", SqueakLanguageConfig.VERSION, "de.hpi.swa.trufflesqueak"));
        checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_URL_STRING, SqueakLanguageConfig.WEBSITE));
        checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_CREATOR_STRING, "TruffleSqueak"));
        checkSdlError(SDL_SetAppMetadataProperty(SDL_PROP_APP_METADATA_COPYRIGHT_STRING, "License terms: " + SqueakLanguageConfig.WEBSITE));

        if (!SDL_Init(SDL_INIT_VIDEO)) {
            throw new IllegalStateException("Unable to initialize SDL: " + SDL_GetError());
        }

        // Enable VSync to accumulate damage and prevent tearing.
        checkSdlError(SDL_SetHint(SDL_HINT_RENDER_VSYNC, "1"));
        // Disable WM_PING, so the WM does not think it is hung.
        checkSdlError(SDL_SetHint(SDL_HINT_VIDEO_X11_NET_WM_PING, "0"));
        // Ctrl-Click on macOS is right click.
        checkSdlError(SDL_SetHint(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1"));

        // Disable unneeded events to avoid issues (e.g. double clicks).
        SDL_SetEventEnabled(SDL_EVENT_TEXT_EDITING, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_DOWN, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_UP, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_MOTION, false);

        try (MemoryStack stack = stackPush()) {

            // Allocate a contiguous buffer for fetching multiple events
            final SDL_Event.Buffer eventBuffer = SDL_Event.malloc(EVENT_FETCH_BATCH_SIZE, stack);

            // We use the first slot of the buffer for the blocking wait call
            final SDL_Event firstEvent = eventBuffer.get(0);

            // Initial drain before running
            while (!isRunning) {
                SDL_WaitEventTimeout(firstEvent, EVENT_WAIT_TIMEOUT_MS);
            }

            // The main event loop evaluation order: callbacks, OS events, render
            // Loop is triggered by an OS event or a wakeUp() ping (not by callbacks).
            while (isRunning) {
                // Sleep until an event (or wakeUp() ping) arrives.
                // All main thread callbacks are executed when the event or wake-up enqueues.
                if (SDL_WaitEvent(firstEvent)) {
                    int eventsRead;
                    // Now, process the first and all other waiting OS events
                    osEventHandler.accept(firstEvent);
                    while ((eventsRead = SDL_PeepEvents(eventBuffer, SDL_GETEVENT, SDL_EVENT_FIRST, SDL_EVENT_LAST)) > 0) {
                        for (int i = 0; i < eventsRead; i++) {
                            osEventHandler.accept(eventBuffer.get(i));
                        }
                    }
                    // All OS events processed; now render to the screen, if needed.
                    // If a render occurs, our thread will sleep until there is a vsync.
                    if (renderFrameIfNeeded != null) {
                        renderFrameIfNeeded.run();
                    }
                }
            }
        }
    }

    private static void checkSdlError(final boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }
}
