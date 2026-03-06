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
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_RENDER_DEVICE_RESET;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_RENDER_TARGETS_RESET;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_DISPLAY_CHANGED;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_RESIZED;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_RENDER_VSYNC;
import static org.lwjgl.sdl.SDLHints.SDL_SetHint;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLKeyboard.SDL_StartTextInput;
import static org.lwjgl.sdl.SDLMouse.SDL_CreateCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_DestroyCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_SetCursor;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ARGB8888;
import static org.lwjgl.sdl.SDLRender.SDL_CreateTexture;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyRenderer;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyTexture;
import static org.lwjgl.sdl.SDLRender.SDL_LockTexture;
import static org.lwjgl.sdl.SDLRender.SDL_RenderClear;
import static org.lwjgl.sdl.SDLRender.SDL_RenderPresent;
import static org.lwjgl.sdl.SDLRender.SDL_RenderTexture;
import static org.lwjgl.sdl.SDLRender.SDL_SetTextureScaleMode;
import static org.lwjgl.sdl.SDLRender.SDL_TEXTUREACCESS_STREAMING;
import static org.lwjgl.sdl.SDLRender.SDL_UnlockTexture;
import static org.lwjgl.sdl.SDLRender.nSDL_CreateRenderer;
import static org.lwjgl.sdl.SDLSurface.SDL_SCALEMODE_NEAREST;
import static org.lwjgl.sdl.SDLVideo.SDL_CreateWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale;
import static org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowFullscreen;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_FRect;
import org.lwjgl.sdl.SDL_Point;
import org.lwjgl.sdl.SDL_Rect;
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
    private long renderer = NULL;
    private SDL_Texture texture;
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    // Squeak bitmap (physical pixels)
    private volatile int width;
    private volatile int height;
    private volatile NativeObject bitmap;

    // UI Thread Tracking (Texture & Logical Window)
    private int textureWidth = -1;
    private int textureHeight = -1;
    private int osWindowWidth;
    private int osWindowHeight;

    private final PointerBuffer pixels = BufferUtils.createPointerBuffer(1);
    final IntBuffer pitch = BufferUtils.createIntBuffer(1);

    private float scaleFactor;
    private static final int BPP = Integer.BYTES;

    // Dirty band tracking
    private int dirtyTop = Integer.MAX_VALUE;
    private int dirtyBottom = Integer.MIN_VALUE;
    private final AtomicBoolean frameRequested = new AtomicBoolean(false);

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

        // Register this display to receive events from the Launcher
        EventQueue.osEventHandler = this::processEvent;
        EventQueue.onClose = this::onClose;

        EventQueue.start.countDown();
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
        return new SqueakDisplay(image);
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
        paintImmediately(left, top, right, bottom);
    }

    private void paintImmediately(final int left, final int top, final int right, final int bottom) {
        if (deferUpdates) {
            return;
        }
        final int copyWidth = right - left;
        final int copyHeight = bottom - top;

        if (copyWidth <= 0 || copyHeight <= 0) {
            return;
        }

        recordDamage(left, top, copyWidth, copyHeight);
        render(false);
    }

    public void render(final boolean force) {
        // Capture and strictly clamp the dirty band
        final int safeTop = Math.max(0, dirtyTop);
        final int safeBottom = Math.min(height, dirtyBottom);

        if (!force && (deferUpdates || safeTop >= safeBottom)) {
            return;
        }

        // Try to queue a frame
        if (frameRequested.getAndSet(true)) {
            // A frame is already in flight.
            // IMPORTANT: DO NOT reset damage! Let the next frame pick up this missed update.
            return;
        }

        // We successfully claimed the frame. We can safely reset the damage trackers.
        resetDamage();

        // Queue the render task using our safely clamped bounds
        EventQueue.INSTANCE.add(() -> {
            if (renderer != NULL) {
                // LAZY TEXTURE SYNC: This absolutely guarantees the texture pitch matches Squeak's
                // bitmap
                if (textureWidth != width || textureHeight != height || texture == null) {
                    if (texture != null) {
                        SDL_DestroyTexture(texture);
                    }
                    textureWidth = width;
                    textureHeight = height;
                    texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, textureWidth, textureHeight);
                    SDL_SetTextureScaleMode(texture, SDL_SCALEMODE_NEAREST);
                }

                try (MemoryStack stack = stackPush()) {
                    final SDL_Rect lockRect = SDL_Rect.malloc(stack);
                    lockRect.set(0, safeTop, textureWidth, safeBottom - safeTop); // Use
                                                                                  // textureWidth!

                    if (SDL_LockTexture(texture, lockRect, pixels, pitch)) {
                        final long pixelBufferAddress = pixels.get(0);
                        final int currentPitch = pitch.get(0);
                        final int storageLength = bitmap.getIntStorage().length;

                        if (currentPitch == textureWidth * Integer.BYTES) {
                            final int srcOffsetInts = safeTop * textureWidth;
                            final int numIntsToCopy = (safeBottom - safeTop) * textureWidth;

                            final int maxSafeInts = Math.min(numIntsToCopy, storageLength - srcOffsetInts);
                            if (srcOffsetInts >= 0 && maxSafeInts > 0) {
                                MemoryUtil.memCopy(bitmap.getIntStorage(), pixelBufferAddress, srcOffsetInts, maxSafeInts);
                            }
                        } else {
                            final int dirtyH = safeBottom - safeTop;
                            for (int y = 0; y < dirtyH; y++) {
                                final int rowOffsetInts = (safeTop + y) * textureWidth;
                                if (rowOffsetInts >= 0 && rowOffsetInts + textureWidth <= storageLength) {
                                    MemoryUtil.memCopy(bitmap.getIntStorage(), pixelBufferAddress + (y * currentPitch), rowOffsetInts, textureWidth);
                                }
                            }
                        }
                        SDL_UnlockTexture(texture);
                    }
                }

                SDL_RenderClear(renderer);
                SDL_RenderTexture(renderer, texture, null, null);
                SDL_RenderPresent(renderer);
            }

            // Release the lock so the next frame can be scheduled
            frameRequested.set(false);
        });
    }

    private void recordDamage(final int x, final int y, final int w, final int h) {
        final int clippedTop = Math.max(0, y);
        final int clippedBottom = Math.min(height, y + h);

        dirtyTop = Math.min(dirtyTop, clippedTop);
        dirtyBottom = Math.max(dirtyBottom, clippedBottom);
    }

    private void resetDamage() {
        dirtyTop = Integer.MAX_VALUE;
        dirtyBottom = Integer.MIN_VALUE;
    }

    private void fullDamage() {
        dirtyTop = 0;
        dirtyBottom = height;
    }

    private static void debug(final String name, final SDL_FRect rect) {
        System.out.println(name + ": x=" + rect.x() + ", y=" + rect.y() + ", w=" + rect.w() + ", h=" + rect.h());
    }

    @TruffleBoundary
    public void close() {
        EventQueue.isRunning = false;
    }

    public void onClose() {
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
    }

    public int getWindowWidth() {
        return osWindowWidth > 0 ? (int) Math.ceil(osWindowWidth * getDisplayScale()) : width;
    }

    public int getWindowHeight() {
        return osWindowHeight > 0 ? (int) Math.ceil(osWindowHeight * getDisplayScale()) : height;
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

        // Squeak thread updates physical dimensions instantly
        width = (int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH);
        height = (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT);

        if (window == NULL) {
            // Initial boot
            osWindowWidth = (int) Math.ceil(width / getDisplayScale());
            osWindowHeight = (int) Math.ceil(height / getDisplayScale());
            EventQueue.INSTANCE.add(this::init);
        } else {
            // Prevent the shrink loop: only resize OS window if Squeak explicitly requested a
            // different logical size
            final int targetLogicalWidth = (int) Math.ceil(width / getDisplayScale());
            final int targetLogicalHeight = (int) Math.ceil(height / getDisplayScale());

            EventQueue.INSTANCE.add(() -> {
                if (targetLogicalWidth != osWindowWidth || targetLogicalHeight != osWindowHeight) {
                    osWindowWidth = targetLogicalWidth;
                    osWindowHeight = targetLogicalHeight;
                    SDL_SetWindowSize(window, osWindowWidth, osWindowHeight);
                }
            });
        }

        final String imageFileName = new File(image.getImagePath()).getName();
        final String title = imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME) ? imageFileName : imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
        setWindowTitle(title);
    }

    public void init() {
        checkSdlError(SDL_SetHint(SDL_HINT_RENDER_VSYNC, "1"));
        checkSdlError(SDL_SetHint(SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1"));

        window = SDL_CreateWindow(DEFAULT_WINDOW_TITLE, osWindowWidth, osWindowHeight, SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIGH_PIXEL_DENSITY);
        if (window == NULL) {
            throw new RuntimeException("Failed to create SDL window: " + SDL_GetError());
        }

        renderer = nSDL_CreateRenderer(window, NULL);
        if (renderer == NULL) {
            throw new RuntimeException("Failed to create SDL renderer: " + SDL_GetError());
        }

        checkSdlError(SDL_RaiseWindow(window));
        scaleFactor = SDL_GetWindowDisplayScale(window);
        SDL_StartTextInput(window);
        fullDamage();
    }

    @TruffleBoundary
    public void resizeTo(final int newWidth, final int newHeight) {
        // The Squeak interpreter is explicitly requesting a new physical size
        width = newWidth;
        height = newHeight;

        // Calculate the logical size needed for the OS window
        final int targetLogicalWidth = (int) Math.ceil(width / getDisplayScale());
        final int targetLogicalHeight = (int) Math.ceil(height / getDisplayScale());

        osWindowWidth = targetLogicalWidth;
        osWindowHeight = targetLogicalHeight;

        if (window != NULL) {
            EventQueue.INSTANCE.add(() -> {
                SDL_SetWindowSize(window, osWindowWidth, osWindowHeight);
            });
        }

        // Ensure the entire new area gets drawn on the next frame
        fullDamage();
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
                mouse.processMouseMotion(event.motion(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
                mouse.processMouseButtonDown(event.button(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_UP:
                mouse.processMouseButtonUp(event.button(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_WHEEL:
                mouse.processMouseWheel(event.wheel(), scaleFactor);
                break;
            case SDL_EVENT_QUIT, SDL_EVENT_WINDOW_CLOSE_REQUESTED:
                addWindowEvent(SqueakIOConstants.WINDOW.CLOSE);
                break;
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
                scaleFactor = SDL_GetWindowDisplayScale(window);
                addWindowEvent(SqueakIOConstants.WINDOW.CHANGED_SCREEN);
                break;
            case SDL_EVENT_WINDOW_RESIZED:
                final SDL_WindowEvent we = event.window();
                // SDL3 reports window sizes in logical coordinates
                osWindowWidth = we.data1();
                osWindowHeight = we.data2();
                addWindowEvent(SqueakIOConstants.WINDOW.METRIC_CHANGE);
                fullDamage();
                render(true);
                break;
            case SDL_EVENT_RENDER_TARGETS_RESET, SDL_EVENT_RENDER_DEVICE_RESET:
                fullDamage();
                render(true);
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
        // Coalesce mouse events if the button state has not changed.
        if (eventType == EVENT_TYPE.MOUSE) {
            final long[] lastEvent = deferredEvents.pollLast();
            if (lastEvent != null) {
                if (lastEvent[0] == EVENT_TYPE.MOUSE && lastEvent[4] == value5) {
                    // Throw away event if it is a mouse event with same button state.
                    // We will fall through and add a new one below.
                } else {
                    // Otherwise, add this event back and add a mouse event below.
                    deferredEvents.addLast(lastEvent);
                }
            }
        }
        deferredEvents.addLast(new long[]{eventType, getEventTime(), value3, value4, value5, value6, value7, HostWindowPlugin.DEFAULT_HOST_WINDOW_ID});
        if (image.options.signalInputSemaphore() && inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }

    private long getEventTime() {
        return System.currentTimeMillis() - image.startUpMillis;
    }

    public void setDeferUpdates(final boolean flag) {
        if (flag) {
            System.out.println("setDeferUpdates: " + flag);
        }
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
