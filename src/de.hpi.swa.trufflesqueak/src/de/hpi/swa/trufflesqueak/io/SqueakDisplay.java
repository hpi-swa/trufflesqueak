/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import static org.lwjgl.sdl.SDLClipboard.SDL_GetClipboardText;
import static org.lwjgl.sdl.SDLClipboard.SDL_HasClipboardText;
import static org.lwjgl.sdl.SDLClipboard.SDL_SetClipboardText;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_EDITING;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_DISPLAY_CHANGED;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_RESIZED;
import static org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_RENDER_VSYNC;
import static org.lwjgl.sdl.SDLHints.SDL_SetHint;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLKeyboard.SDL_StartTextInput;
import static org.lwjgl.sdl.SDLMouse.SDL_CreateCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_DestroyCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_SetCursor;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ARGB8888;
import static org.lwjgl.sdl.SDLRect.SDL_GetRectUnion;
import static org.lwjgl.sdl.SDLRect.SDL_GetRectUnionFloat;
import static org.lwjgl.sdl.SDLRender.SDL_CreateTexture;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyRenderer;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyTexture;
import static org.lwjgl.sdl.SDLRender.SDL_RenderPresent;
import static org.lwjgl.sdl.SDLRender.SDL_RenderTexture;
import static org.lwjgl.sdl.SDLRender.SDL_TEXTUREACCESS_STREAMING;
import static org.lwjgl.sdl.SDLRender.SDL_UpdateTexture;
import static org.lwjgl.sdl.SDLRender.nSDL_CreateRenderer;
import static org.lwjgl.sdl.SDLVideo.SDL_CreateWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale;
import static org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowFullscreen;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

import org.lwjgl.BufferUtils;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_FRect;
import org.lwjgl.sdl.SDL_Point;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDL_Texture;
import org.lwjgl.sdl.SDL_WindowEvent;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.trufflesqueak.shared.EventQueue;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class SqueakDisplay {
    private static final String DEFAULT_WINDOW_TITLE = "TruffleSqueak";
    @CompilationFinal(dimensions = 1) private static final int[] CURSOR_COLORS = {0x00000000, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000};

    public final SqueakImageContext image;

    // public for the Java-based UI for TruffleSqueak.
    // public final Frame frame = new Frame(DEFAULT_WINDOW_TITLE);
    private long window = NULL;
    private long cursor = NULL;
    private SDL_Surface surface;
    private long renderer = NULL;
    private SDL_Texture texture;
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    private int width;
    private int height;
    private ByteBuffer pixelBuffer;
    private float scaleFactor;
    private NativeObject bitmap;
    private static final int BPP = Integer.BYTES;
    private SDL_Rect updateRect = SDL_Rect.create();
    private SDL_FRect flipRect = SDL_FRect.create();
    private SDL_Rect flipRect2 = SDL_Rect.create();
    private SDL_FRect sourceRect = SDL_FRect.create();
    private SDL_FRect renderRect = SDL_FRect.create();

    private final ConcurrentLinkedDeque<long[]> deferredEvents = new ConcurrentLinkedDeque<>();

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private boolean deferUpdates;

    private SqueakDisplay(final SqueakImageContext image) {
// assert EventQueue.isDispatchThread();
        this.image = image;
// frame.add(canvas);
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);

        // Do not wait for vsync.
        checkSdlError(SDL_SetHint(SDLHints.SDL_HINT_RENDER_VSYNC, "0"));
        // Nearest pixel sampling.
        // checkSdlError(SDLHints.SDL_SetHint(SDLHints.HINT_RENDER_SCALE_QUALITY, "0"));
        // SDL.setHint(SDL.HINT_RENDER_SCALE_QUALITY, "0");
        // Disable WM_PING, so the WM does not think it is hung.
        checkSdlError(SDL_SetHint(SDLHints.SDL_HINT_VIDEO_X11_NET_WM_PING, "0"));
        // Ctrl-Click on macOS is right click.
        checkSdlError(SDL_SetHint(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1"));

        // Disable unneeded events to avoid issues (e.g. double clicks).
        SDL_SetEventEnabled(SDL_EVENT_TEXT_EDITING, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_DOWN, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_UP, false);
        SDL_SetEventEnabled(SDL_EVENT_FINGER_MOTION, false);

        // Register this display to receive events from the Launcher
        de.hpi.swa.trufflesqueak.shared.EventQueue.osEventHandler = (obj) -> {
            if (obj instanceof SDL_Event sdlEvent) {
                this.processEvent(sdlEvent);
            }
        };

        tryToSetTaskbarIcon();
    }

    private static void checkSdlError(final boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }

    private static long checkSdlError(final long resultPointer) {
        if (resultPointer == 0) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
        return resultPointer;
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        final SqueakDisplay[] display = new SqueakDisplay[1];
        EventQueue.INSTANCE.add(() -> display[0] = new SqueakDisplay(image));
        while (display[0] == null) {
            LockSupport.parkNanos(1_000_000L);
        }
        return display[0];
    }

    private static void tryToSetTaskbarIcon() {
        // TODO
    }

    public double getDisplayScale() {
        if (scaleFactor != NULL) {
            return scaleFactor;
        } else {
            return 1.0d;
        }
    }

    @TruffleBoundary
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        assert left <= right && top <= bottom;
        de.hpi.swa.trufflesqueak.shared.EventQueue.INSTANCE.add(() -> paintImmediately(left, top, right, bottom));
    }

    private void paintImmediately(final int left, final int top, final int right, final int bottom) {
        if (deferUpdates) {
            return;
        }
        final int copyWidth = right - left;
        final int copyHeight = bottom - top;
        // System.out.println("updateXXXX: x=" + left + " y=" + top + " w=" + copyWidth + " h=" +
        // copyHeight);
        if (copyWidth <= 0 || copyHeight <= 0) {
            System.out.println("skipping frame");
            return;
        }

        recordDamage(left, top, copyWidth, copyHeight);
        copyPixels(left, top, right, bottom, copyWidth, copyHeight);
        render();
    }

    private void copyPixels(final int left, final int top, final int right, final int bottom, final int copyWidth, final int copyHeight) {
        final int pitch = width * BPP;
        final int byteOffset = top * pitch + left * BPP;

        final int start = updateRect.y() * width + updateRect.x();
        final int end = (updateRect.y() + updateRect.h()) * width + (updateRect.x() + updateRect.w());
// final int end= updateRect.y() * width + updateRect.x();

        // final int byteOffsetEnd = bottom * pitch + right * bpp;

        pixelBuffer.clear();
        MemoryUtil.memCopy(bitmap.getIntStorage(), pixelBuffer);

// pixelBuffer.position(start * bpp);
// SDL_UpdateTexture(texture, updateRect, pixelBuffer, pitch);

        pixelBuffer.clear();
        SDL_UpdateTexture(texture, null, pixelBuffer, pitch);
    }

    private void recordDamage(final int x, final int y, final int w, final int h) {
        updateRect.x(x);
        updateRect.y(y);
        updateRect.w(w);
        updateRect.h(h);

        sourceRect.x(x);
        sourceRect.y(y);
        sourceRect.w(w);
        sourceRect.h(h);

        renderRect.x(x * scaleFactor);
        renderRect.y(y * scaleFactor);
        renderRect.w(w * scaleFactor);
        renderRect.h(h * scaleFactor);
    }

    private void recordDamage2(final int x, final int y, final int w, final int h) {
        System.out.println("            x=" + x + " y=" + y + " w=" + w + " h=" + h);
        flipRect2.x(x);
        flipRect2.y(y);
        flipRect2.w(w);
        flipRect2.h(h);
        System.out.println("flipRect2:  x=" + flipRect2.x() + " y=" + flipRect2.y() + " w=" + flipRect2.w() + " h=" + flipRect2.h());
        System.out.println("updateRect: x=" + updateRect.x() + " y=" + updateRect.y() + " w=" + updateRect.w() + " h=" + updateRect.h());
        SDL_GetRectUnion(flipRect2, updateRect, updateRect);
        System.out.println("updateRect: x=" + updateRect.x() + " y=" + updateRect.y() + " w=" + updateRect.w() + " h=" + updateRect.h());

        flipRect.x(x);
        flipRect.y(y);
        flipRect.w(w);
        flipRect.h(h);
        System.out.println("flipRect:   x=" + flipRect.x() + " y=" + flipRect.y() + " w=" + flipRect.w() + " h=" + flipRect.h());
        System.out.println("sourceRect: x=" + sourceRect.x() + " y=" + sourceRect.y() + " w=" + sourceRect.w() + " h=" + sourceRect.h());
        SDL_GetRectUnionFloat(flipRect, sourceRect, sourceRect);
        System.out.println("sourceRect: x=" + sourceRect.x() + " y=" + sourceRect.y() + " w=" + sourceRect.w() + " h=" + sourceRect.h());

        flipRect.x(x * scaleFactor);
        flipRect.y(y * scaleFactor);
        flipRect.w(w * scaleFactor);
        flipRect.h(h * scaleFactor);
// System.out.println("flipRect: " + flipRect.w() + " " + flipRect.h() + " " + flipRect.x() + " " +
// flipRect.y());
        SDL_GetRectUnionFloat(flipRect, renderRect, renderRect);
    }

    private void render() {

        // Clear the renderer
        // SDL_RenderClear(renderer);

        // Copy the texture to the rendering target (replaces SDL2's SDL_RenderCopy)
// System.out.println("updateRect: x=" + updateRect.x() + " y=" + updateRect.y() + " w=" +
// updateRect.w() + " h=" + updateRect.h());
// System.out.println("sourceRect: x=" + sourceRect.x() + " y=" + sourceRect.y() + " w=" +
// sourceRect.w() + " h=" + sourceRect.h());
// System.out.println("renderRect: x=" + renderRect.x() + " y=" + renderRect.y() + " w=" +
// renderRect.w() + " h=" + renderRect.h());
        checkSdlError(SDL_RenderTexture(renderer, texture, null, null));

        // Present the updated renderer to the screen
        SDL_RenderPresent(renderer);

        // System.out.print("x");
        // System.out.flush();

// if (!SDL_RenderTexture(renderer, texture, renderRect, renderRect)) {
//// return;
// }
//
// SDL_RenderPresent(renderer);
// SDL_UpdateWindowSurface(window);
        resetDamage();
    }

    private void resetDamage() {
        updateRect.x(0);
        updateRect.y(0);
        updateRect.w(0);
        updateRect.h(0);

        sourceRect.x(0);
        sourceRect.y(0);
        sourceRect.w(0);
        sourceRect.h(0);

        renderRect.x(0);
        renderRect.y(0);
        renderRect.w(0);
        renderRect.h(0);
    }

    private void fullDamage() {
        updateRect.x(0);
        updateRect.y(0);
        updateRect.w(width);
        updateRect.h(height);

        sourceRect.x(0);
        sourceRect.y(0);
        sourceRect.w(width);
        sourceRect.h(height);

        renderRect.x(0);
        renderRect.y(0);
        renderRect.w(width * scaleFactor);
        renderRect.h(height * scaleFactor);
    }

    @TruffleBoundary
    public void close() {
        de.hpi.swa.trufflesqueak.shared.EventQueue.INSTANCE.add(() -> {
            if (texture != null) {
                SDL_DestroyTexture(texture);
            }
            if (renderer != NULL) {
                SDL_DestroyRenderer(renderer);
            }
            if (window != NULL) {
                SDL_DestroyWindow(window);
            }
            System.out.println("Quitting SqueakVM");
            SDL_Quit();
        });
    }

    @TruffleBoundary
    public void resizeTo(final int newWidth, final int newHeight) {
        width = newWidth;
        height = newHeight;

        if (texture != null) {
            SDL_DestroyTexture(texture);
        }

        texture = SDL_CreateTexture(
                        renderer,
                        SDL_PIXELFORMAT_ARGB8888,
                        SDL_TEXTUREACCESS_STREAMING,
                        width,
                        height);
        if (texture == null) {
            throw new RuntimeException("Failed to create SDL texture: " + SDL_GetError());
        }
    }

    public int getWindowWidth() {
        return width;
    }

    public int getWindowHeight() {
        return height;
    }

    @TruffleBoundary
    public void setFullscreen(final boolean fullscreen) {
        if (window == NULL) {
            return;
        }
        EventQueue.INSTANCE.add(() -> {
            SDL_SetWindowFullscreen(window, fullscreen);
        });
    }

    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        bitmap = (NativeObject) sqDisplay.instVarAt0Slow(FORM.BITS);
        if (!bitmap.isIntType()) {
            throw SqueakException.create("Display bitmap expected to be a words object");
        }
        width = (int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH);
        height = (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT);
        if (pixelBuffer != null) {
            MemoryUtil.memFree(pixelBuffer);
        }
        pixelBuffer = BufferUtils.createByteBuffer(width * height * BPP);
        if (window == NULL) {
            EventQueue.INSTANCE.add(this::init);
        } else {
            EventQueue.INSTANCE.add(() -> resizeTo(width, height));
        }

        // Set or update frame title.
        final String imageFileName = new File(image.getImagePath()).getName();
        // Avoid name duplication in frame title.
        final String title;
        if (imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME)) {
            title = imageFileName;
        } else {
            title = imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
        }
        setWindowTitle(title);
    }

    public void init() {
        // Initialize the SDL Video subsystem
        if (!SDL_Init(SDL_INIT_VIDEO)) {
            throw new RuntimeException("Failed to initialize SDL3: " + SDL_GetError());
        }

        checkSdlError(SDL_SetHint(SDL_HINT_RENDER_VSYNC, "0"));
        checkSdlError(SDL_SetHint(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1"));

        // Create the native window
        window = SDL_CreateWindow("TruffleSqueak - SDL3 via LWJGL", width, height, SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIGH_PIXEL_DENSITY);
        if (window == NULL) {
            throw new RuntimeException("Failed to create SDL window: " + SDL_GetError());
        }
        checkSdlError(SDL_RaiseWindow(window));
        scaleFactor = SDL_GetWindowDisplayScale(window);

        // Tell the OS we want translated text input events
        SDL_StartTextInput(window);

        // Create the hardware-accelerated renderer
// surface = SDL_GetWindowSurface(window);
// renderer = SDL_CreateSoftwareRenderer(surface);
        renderer = nSDL_CreateRenderer(window, NULL);
        if (renderer == NULL) {
            throw new RuntimeException("Failed to create SDL renderer: " + SDL_GetError());
        }

        // Create a texture configured for frequent updates (streaming).
        // Note: Adjust the PixelFormat if TruffleSqueak is yielding a different byte order (e.g.,
        // BGRA).
        texture = SDL_CreateTexture(
                        renderer,
                        SDL_PIXELFORMAT_ARGB8888,
                        SDL_TEXTUREACCESS_STREAMING,
                        width,
                        height);

        if (texture == null) {
            throw new RuntimeException("Failed to create SDL texture: " + SDL_GetError());
        }

        // checkSdlError(SDL_GL_SetSwapInterval(0)); // disable vsync

    }

    @TruffleBoundary
    public boolean isVisible() {
        return window != NULL;
    }

    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] maskWords, final int width, final int height, final int offsetX, final int offsetY) {
        if (window == NULL) {
            return;
        }
        EventQueue.INSTANCE.add(() -> {
            setSDLCursor(cursorWords, maskWords, width, height, offsetX, offsetY);
        });
    }

    private void setSDLCursor(final int[] cursorWords, final int[] maskWords, final int width, final int height, final int offsetX, final int offsetY) {
        if (cursor != NULL) {
            SDL_DestroyCursor(cursor);
        }
        try (MemoryStack stack = stackPush()) {
            final int numBytes = cursorWords.length * 2;
            final ByteBuffer data = stack.calloc(numBytes);
            final ByteBuffer mask = stack.calloc(numBytes);
            copyIntoBuffer(cursorWords, data);
            if (maskWords != null) {
                copyIntoBuffer(maskWords, mask);
            }
            cursor = SDL_CreateCursor(data, mask, width, height, offsetX, offsetY);
        }
        checkSdlError(SDL_SetCursor(cursor));
    }

    private static void copyIntoBuffer(final int[] words, final ByteBuffer buffer) {
        /*
         * In Squeak, only the upper 16bits of the cursor form seem to count (I'm guessing because
         * the code was ported over from 16-bit machines), so this ignores the lower 16-bits of each
         * word.
         */
        for (int word : words) {
            buffer.put((byte) (word >> 24));
            buffer.put((byte) (word >> 16));
        }
        buffer.clear();
    }

    public void processEvent(final SDL_Event event) {
        switch (event.type()) {
            case SDL_EVENT_KEY_DOWN:
                // Note the removal of keysym() here!
                keyboard.processKeyDown(event.key().key(), event.key().mod());
                break;
            case SDL_EVENT_KEY_UP:
                keyboard.processKeyUp(event.key().key(), event.key().mod());
                break;
            case SDL_EVENT_TEXT_INPUT:
                keyboard.processTextInput(event.text().textString());
                break;
            case SDL_EVENT_MOUSE_MOTION:
                mouse.processMouseMotion(event.motion());
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
                mouse.processMouseButtonDown(event.button());
                break;
            case SDL_EVENT_MOUSE_BUTTON_UP:
                mouse.processMouseButtonUp(event.button());
                break;
            case SDL_EVENT_MOUSE_WHEEL:
                mouse.processMouseWheel(event.wheel());
                break;
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
                addWindowEvent(SqueakIOConstants.WINDOW.CHANGED_SCREEN);
                break;
            case SDL_EVENT_WINDOW_RESIZED:
                final SDL_WindowEvent we = event.window();
                resizeTo(we.data1(), we.data2());
                addWindowEvent(SqueakIOConstants.WINDOW.METRIC_CHANGE);
                break;
        }
    }

    public int recordModifiers(final int sdlModifiers) {
        // Changed SDLKeymod to SDLKeycode
        final int shiftValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LSHIFT | SDLKeycode.SDL_KMOD_RSHIFT)) != 0 ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LCTRL | SDLKeycode.SDL_KMOD_RCTRL)) != 0 ? KEYBOARD.CTRL : 0;
        final int optValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LALT | SDLKeycode.SDL_KMOD_RALT)) != 0 ? KEYBOARD.ALT : 0;
        final int cmdValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LGUI | SDLKeycode.SDL_KMOD_RGUI)) != 0 ? KEYBOARD.CMD : 0;

        final int modifiers = shiftValue + ctrlValue + optValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
        return modifiers;
    }

    public long[] getNextEvent() {
        return deferredEvents.pollFirst();
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        addEvent(eventType, value3, value4, value5, value6, 0L);
    }

    private void addDragEvent(final long type, final SDL_Point location) {
        addEvent(EVENT_TYPE.DRAG_DROP_FILES, type, (long) location.x(), (long) location.y(), buttons >> 3, image.dropPluginFileList.length);
    }

    private void addWindowEvent(final long type) {
        addEvent(EVENT_TYPE.WINDOW, type, 0L, 0L, 0L);
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6, final long value7) {
        deferredEvents.add(new long[]{eventType, getEventTime(), value3, value4, value5, value6, value7, HostWindowPlugin.DEFAULT_HOST_WINDOW_ID});
        if (image.options.signalInputSemaphore() && inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }

    private long getEventTime() {
        return System.currentTimeMillis() - image.startUpMillis;
    }

    public void setDeferUpdates(final boolean flag) {
        deferUpdates = flag;
    }

    public boolean getDeferUpdates() {
        return deferUpdates;
    }

    @TruffleBoundary
    public void setWindowTitle(final String title) {
        if (window == NULL) {
            return;
        }
        EventQueue.INSTANCE.add(() -> {
            SDL_SetWindowTitle(window, title);
        });
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public static String getClipboardData() {
        if (SDL_HasClipboardText()) {
            return SDL_GetClipboardText();
        }
        return "";
    }

    @TruffleBoundary
    public static void setClipboardData(final String text) {
        if (!SDL_SetClipboardText(text)) {
            LogUtils.IO.warning("Failed to set clipboard text");
        }
    }

    @TruffleBoundary
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    private void installWindowAdapter() {
        // TODO
    }

    private void installDropTargetListener() {
        // TODO
    }
}
