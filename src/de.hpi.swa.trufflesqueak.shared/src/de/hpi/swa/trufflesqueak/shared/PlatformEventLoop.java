package de.hpi.swa.trufflesqueak.shared;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_EDITING;
import static org.lwjgl.sdl.SDLEvents.SDL_PollEvent;
import static org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled;
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
    private static final int EVENT_WAIT_TIMEOUT_MS = 5;
    public static volatile Consumer<SDL_Event> osEventHandler = null;
    public static volatile boolean isRunning = false;

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
            final SDL_Event event = SDL_Event.malloc(stack);

            while (!isRunning) {
                if (SDL_WaitEventTimeout(event, EVENT_WAIT_TIMEOUT_MS)) {
                    while (SDL_PollEvent(event)) {
                        // ignore all events
                    }
                }
            }
            while (isRunning) {
                if (SDL_WaitEventTimeout(event, EVENT_WAIT_TIMEOUT_MS)) {
                    do {
                        osEventHandler.accept(event);
                    } while (SDL_PollEvent(event));
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
