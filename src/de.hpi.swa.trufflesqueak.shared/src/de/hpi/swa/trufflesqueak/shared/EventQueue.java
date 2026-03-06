package de.hpi.swa.trufflesqueak.shared;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_EDITING;
import static org.lwjgl.sdl.SDLEvents.SDL_PollEvent;
import static org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static org.lwjgl.sdl.SDLHints.SDL_SetHint;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLStdinc.SDL_SetMemoryFunctions;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryUtil;

public final class EventQueue extends ConcurrentLinkedQueue<Runnable> {
    public static final EventQueue INSTANCE = new EventQueue();

    public static final CountDownLatch start = new CountDownLatch(1);
    public static volatile Consumer<SDL_Event> osEventHandler = null;
    public static volatile boolean isRunning = true;
    public static volatile Runnable onClose = null;

    public static void run() {
        SDL_SetMemoryFunctions(
                        MemoryUtil::nmemAllocChecked,
                        MemoryUtil::nmemCallocChecked,
                        MemoryUtil::nmemReallocChecked,
                        MemoryUtil::nmemFree);

        if (!SDL_Init(SDL_INIT_VIDEO)) {
            throw new IllegalStateException("Unable to initialize SDL: " + SDL_GetError());
        }

        // Enable VSync to accumulate damage and prevent tearing.
        checkSdlError(SDL_SetHint(SDLHints.SDL_HINT_RENDER_VSYNC, "1"));

        // Disable WM_PING, so the WM does not think it is hung.
        checkSdlError(SDL_SetHint(SDLHints.SDL_HINT_VIDEO_X11_NET_WM_PING, "0"));
        // Ctrl-Click on macOS is right click.
        checkSdlError(SDL_SetHint(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1"));

        // Disable unneeded events to avoid issues (e.g. double clicks).
        SDL_SetEventEnabled(SDL_EVENT_TEXT_EDITING, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_DOWN, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_UP, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_MOTION, false);

        try {
            start.await();
            System.out.println("Waiting thread: Latch opened, proceeding!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final SDL_Event event = SDL_Event.create();

        while (isRunning) {
            Runnable r = EventQueue.INSTANCE.poll();
            while (r != null) {
                r.run();
                r = EventQueue.INSTANCE.poll();
            }

            while (SDL_PollEvent(event)) {
                // Route the event through the shared module to the VM
                osEventHandler.accept(event);
            }
        }

        onClose.run();
    }

    private static void checkSdlError(final boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }
}
