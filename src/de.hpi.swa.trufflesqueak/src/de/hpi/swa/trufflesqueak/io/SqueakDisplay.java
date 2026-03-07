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

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
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
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD_EVENT;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE_EVENT;
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

    private long window = NULL;
    private long cursor = NULL;
    private long renderer = NULL;
    private SDL_Texture texture;

    // Squeak bitmap (physical pixels)
    private int width;
    private int height;
    private NativeObject bitmap;

    // Async Staging Buffer (Decouples Squeak thread from GPU thread)
    private int[] stagingPixels = new int[0];

    // Dirty 2D bounding box for coalescing damage
    private int dirtyLeft = Integer.MAX_VALUE;
    private int dirtyTop = Integer.MAX_VALUE;
    private int dirtyRight = Integer.MIN_VALUE;
    private int dirtyBottom = Integer.MIN_VALUE;

    private boolean frameRequested = false;

    // UI Thread Tracking (Texture & Logical Window)
    private int textureWidth = -1;
    private int textureHeight = -1;
    private int osWindowWidth;
    private int osWindowHeight;

    private final PointerBuffer pixels = BufferUtils.createPointerBuffer(1);
    final IntBuffer pitch = BufferUtils.createIntBuffer(1);

    private float scaleFactor;

    // Pre-allocated render task to eliminate per-frame GC allocations
    private final Runnable renderTask = this::performRender;

    private final ConcurrentLinkedDeque<long[]> deferredEvents = new ConcurrentLinkedDeque<>();

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private boolean deferUpdates;

    private SqueakDisplay(final SqueakImageContext image) {
        this.image = image;

        EventQueue.osEventHandler = this::processEvent;
        EventQueue.onClose = this::onClose;

        EventQueue.start.countDown();
    }

    private static void checkSdlError(final boolean success) {
        if (!success) {
            throw SqueakException.create("SDL error encountered: " + SDL_GetError());
        }
    }

    private static float checkSdlError(final float value) {
        if (value == 0.0f) {
            throw SqueakException.create("SDL error encountered: " + SDL_GetError());
        }
        return value;
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        return new SqueakDisplay(image);
    }

    private void ensureStagingPixels(final int w, final int h) {
        final int requiredLength = w * h;
        if (stagingPixels.length != requiredLength) {
            stagingPixels = new int[requiredLength];
        }
    }

    public double getDisplayScale() {
        if (scaleFactor != NULL) {
            return scaleFactor;
        } else {
            return 1.0d;
        }
    }

    public void render(final boolean force) {
        synchronized (this) {
            // Strictly synchronized bounds check
            if (!force && (deferUpdates || dirtyTop >= dirtyBottom || dirtyLeft >= dirtyRight)) {
                return;
            }

            if (frameRequested) {
                return; // A frame is already queued
            }

            // Claim the frame
            frameRequested = true;
        }

        // Add to the queue safely outside the lock to keep the critical section tiny
        EventQueue.INSTANCE.add(renderTask);
    }

    private void performRender() {
        final int safeTop, safeBottom;
        final int sqWidth, sqHeight;

        // Atomically capture the vertical bounds and dimensions.
        synchronized (this) {
            sqWidth = width;
            sqHeight = height;

            safeTop = Math.max(0, dirtyTop);
            safeBottom = Math.min(sqHeight, dirtyBottom);
            resetDamage();
            frameRequested = false;
        }

        // Prepare SDL Texture
        if (renderer != NULL && safeTop < safeBottom) {
            if (textureWidth != sqWidth || textureHeight != sqHeight || texture == null) {
                if (texture != null) {
                    SDL_DestroyTexture(texture);
                }
                textureWidth = sqWidth;
                textureHeight = sqHeight;
                texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, textureWidth, textureHeight);
                if (texture == null) {
                    throw SqueakException.create("Failed to create texture");
                }
                checkSdlError(SDL_SetTextureScaleMode(texture, SDL_SCALEMODE_NEAREST));
            }

            try (MemoryStack stack = stackPush()) {
                final SDL_Rect lockRect = SDL_Rect.malloc(stack);

                // Lock the dirty rows
                lockRect.set(0, safeTop, sqWidth, safeBottom - safeTop);

                if (SDL_LockTexture(texture, lockRect, pixels, pitch)) {
                    final long pixelBufferAddress = pixels.get(0);
                    final int currentPitchBytes = pitch.get(0);

                    final IntBuffer intBuf = MemoryUtil.memByteBuffer(pixelBufferAddress, (safeBottom - safeTop) * currentPitchBytes).asIntBuffer();

                    if (bitmap.getIntLength() >= sqWidth * sqHeight) {

                        // Check if the GPU driver perfectly packed the rows (no padding)
                        if (currentPitchBytes == sqWidth * Integer.BYTES) {

                            // Single, uninterrupted native memory blast
                            final int startOffset = safeTop * sqWidth;
                            final int totalInts = (safeBottom - safeTop) * sqWidth;
                            intBuf.put(bitmap.getIntStorage(), startOffset, totalInts);

                        } else {
                            // FALLBACK: The GPU padded the rows. We must copy row-by-row.
                            final int currentPitchInts = currentPitchBytes / Integer.BYTES;
                            for (int y = safeTop; y < safeBottom; y++) {
                                intBuf.position((y - safeTop) * currentPitchInts);
                                intBuf.put(bitmap.getIntStorage(), y * sqWidth, sqWidth);
                            }
                        }
                    }

                    SDL_UnlockTexture(texture);
                }
            }

            checkSdlError(SDL_RenderClear(renderer));
            checkSdlError(SDL_RenderTexture(renderer, texture, null, null));
            checkSdlError(SDL_RenderPresent(renderer));
        }
    }

    @TruffleBoundary
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        assert left <= right && top <= bottom;

        if (deferUpdates) {
            return;
        }

        synchronized (this) {
            final int currentWidth = width;
            final int currentHeight = height;

            final int safeLeft = Math.max(0, left);
            final int safeTop = Math.max(0, top);
            final int safeRight = Math.min(currentWidth, right);
            final int safeBottom = Math.min(currentHeight, bottom);

            if (safeLeft >= safeRight || safeTop >= safeBottom) {
                return;
            }

            // ensureStagingPixels(currentWidth, currentHeight);
            final int[] sqPixels = bitmap.getIntStorage();

            if (sqPixels.length >= currentWidth * currentHeight) {
                final int startOffset = safeTop * currentWidth;
                final int totalLengthToCopy = (safeBottom - safeTop) * currentWidth;
                // System.arraycopy(sqPixels, startOffset, stagingPixels, startOffset,
                // totalLengthToCopy);
            }

            recordDamage(safeLeft, safeTop, safeRight, safeBottom);
        }

        render(false);
    }

    private void recordDamage(final int left, final int top, final int right, final int bottom) {
        dirtyLeft = Math.min(dirtyLeft, Math.max(0, left));
        dirtyTop = Math.min(dirtyTop, Math.max(0, top));
        dirtyRight = Math.max(dirtyRight, Math.min(width, right));
        dirtyBottom = Math.max(dirtyBottom, Math.min(height, bottom));
    }

    private void resetDamage() {
        dirtyLeft = Integer.MAX_VALUE;
        dirtyTop = Integer.MAX_VALUE;
        dirtyRight = Integer.MIN_VALUE;
        dirtyBottom = Integer.MIN_VALUE;
    }

    private void fullDamage() {
        synchronized (this) { // Explicit block instead of method modifier
            recordDamage(0, 0, width, height);
        }
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
            checkSdlError(SDL_SetWindowFullscreen(window, fullscreen));
        });
    }

    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        final NativeObject newBitmap = (NativeObject) sqDisplay.instVarAt0Slow(FORM.BITS);
        if (!newBitmap.isIntType()) {
            throw SqueakException.create("Display bitmap expected to be a words object");
        }

        final int newWidth = (int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH);
        final int newHeight = (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT);

        // Safely update the dimensions and bitmap reference together
        synchronized (this) {
            bitmap = newBitmap;
            width = newWidth;
            height = newHeight;
        }

        if (window == NULL) {
            osWindowWidth = (int) Math.ceil(width / getDisplayScale());
            osWindowHeight = (int) Math.ceil(height / getDisplayScale());
            EventQueue.INSTANCE.add(this::init);
        } else {
            final int targetLogicalWidth = (int) Math.ceil(width / getDisplayScale());
            final int targetLogicalHeight = (int) Math.ceil(height / getDisplayScale());

            EventQueue.INSTANCE.add(() -> {
                if (targetLogicalWidth != osWindowWidth || targetLogicalHeight != osWindowHeight) {
                    osWindowWidth = targetLogicalWidth;
                    osWindowHeight = targetLogicalHeight;
                    checkSdlError(SDL_SetWindowSize(window, osWindowWidth, osWindowHeight));
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
            throw SqueakException.create("Failed to create SDL window: " + SDL_GetError());
        }

        renderer = nSDL_CreateRenderer(window, NULL);
        if (renderer == NULL) {
            throw SqueakException.create("Failed to create SDL renderer: " + SDL_GetError());
        }

        checkSdlError(SDL_RaiseWindow(window));
        scaleFactor = checkSdlError(SDL_GetWindowDisplayScale(window));
        checkSdlError(SDL_StartTextInput(window));
        fullDamage();
    }

    @TruffleBoundary
    public void resizeTo(final int newWidth, final int newHeight) {
        synchronized (this) {
            width = newWidth;
            height = newHeight;
        }

        final int targetLogicalWidth = (int) Math.ceil(newWidth / getDisplayScale());
        final int targetLogicalHeight = (int) Math.ceil(newHeight / getDisplayScale());

        osWindowWidth = targetLogicalWidth;
        osWindowHeight = targetLogicalHeight;

        if (window != NULL) {
            EventQueue.INSTANCE.add(() -> {
                checkSdlError(SDL_SetWindowSize(window, osWindowWidth, osWindowHeight));
            });
        }

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
            if (cursor == NULL) {
                throw SqueakException.create("Failed to create SDL cursor: " + SDL_GetError());
            }
        }
        checkSdlError(SDL_SetCursor(cursor));
    }

    private static void copyIntoBuffer(final int[] words, final ByteBuffer buffer) {
        for (int word : words) {
            buffer.put((byte) (word >> 24));
            buffer.put((byte) (word >> 16));
        }
        buffer.clear();
    }

    public void processEvent(final SDL_Event event) {
        switch (event.type()) {
            case SDL_EVENT_KEY_DOWN:
                processKeyDown(event.key().key(), event.key().mod());
                break;
            case SDL_EVENT_KEY_UP:
                processKeyUp(event.key().key(), event.key().mod());
                break;
            case SDL_EVENT_TEXT_INPUT:
                processTextInput(event.text().textString());
                break;
            case SDL_EVENT_MOUSE_MOTION:
                processMouseMotion(event.motion(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
                processMouseButtonDown(event.button(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_UP:
                processMouseButtonUp(event.button(), scaleFactor);
                break;
            case SDL_EVENT_MOUSE_WHEEL:
                processMouseWheel(event.wheel(), scaleFactor);
                break;
            case SDL_EVENT_QUIT, SDL_EVENT_WINDOW_CLOSE_REQUESTED:
                addWindowEvent(SqueakIOConstants.WINDOW.CLOSE);
                break;
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
                scaleFactor = checkSdlError(SDL_GetWindowDisplayScale(window));
                addWindowEvent(SqueakIOConstants.WINDOW.CHANGED_SCREEN);
                break;
            case SDL_EVENT_WINDOW_RESIZED:
                final SDL_WindowEvent we = event.window();
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

    // --- Keyboard processing methods ---

    public void processKeyDown(final int sdlKeySym, final int sdlModifiers) {
        recordModifiers(sdlModifiers);

        if (isModifier(sdlKeySym)) {
            return;
        }

        final int keyChar = toSqueakKey(sdlKeySym);

        addKeyboardEvent(KEYBOARD_EVENT.DOWN, keyChar);

        final boolean isShortcut = (sdlModifiers & (SDLKeycode.SDL_KMOD_LCTRL | SDLKeycode.SDL_KMOD_RCTRL |
                        SDLKeycode.SDL_KMOD_LGUI | SDLKeycode.SDL_KMOD_RGUI |
                        SDLKeycode.SDL_KMOD_LALT | SDLKeycode.SDL_KMOD_RALT)) != 0;

        if (isControlKey(sdlKeySym) || isShortcut) {
            if (keyChar <= 65535) {
                addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyChar);
            }
        }

        if (isShortcut && keyChar == '.') {
            image.interrupt.setInterruptPending();
        }
    }

    public void processKeyUp(final int sdlKeySym, final int sdlModifiers) {
        recordModifiers(sdlModifiers);

        if (isModifier(sdlKeySym)) {
            return;
        }

        final int keyChar = toSqueakKey(sdlKeySym);
        addKeyboardEvent(KEYBOARD_EVENT.UP, keyChar);
    }

    public void processTextInput(final String text) {
        final int currentModifiers = buttons >> 3;
        final boolean isShortcut = (currentModifiers & (KEYBOARD.CTRL | KEYBOARD.CMD | KEYBOARD.ALT)) != 0;

        if (isShortcut || text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            addKeyboardEvent(KEYBOARD_EVENT.CHAR, text.charAt(i));
        }
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, buttons >> 3, keyCharOrCode);
    }

    private boolean isModifier(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_LSHIFT, SDLKeycode.SDLK_RSHIFT, SDLKeycode.SDLK_LCTRL, SDLKeycode.SDLK_RCTRL, SDLKeycode.SDLK_LALT, SDLKeycode.SDLK_RALT, SDLKeycode.SDLK_LGUI, SDLKeycode.SDLK_RGUI -> true;
            default -> false;
        };
    }

    private boolean isControlKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_BACKSPACE, SDLKeycode.SDLK_TAB, SDLKeycode.SDLK_RETURN, SDLKeycode.SDLK_KP_ENTER, SDLKeycode.SDLK_ESCAPE, SDLKeycode.SDLK_PAGEUP, SDLKeycode.SDLK_PAGEDOWN, SDLKeycode.SDLK_END, //
                SDLKeycode.SDLK_HOME, SDLKeycode.SDLK_LEFT, SDLKeycode.SDLK_UP, SDLKeycode.SDLK_RIGHT, SDLKeycode.SDLK_DOWN, SDLKeycode.SDLK_INSERT, SDLKeycode.SDLK_DELETE -> true;
            default -> false;
        };
    }

    private static int toSqueakKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLKeycode.SDLK_BACKSPACE -> 8;
            case SDLKeycode.SDLK_TAB -> 9;
            case SDLKeycode.SDLK_RETURN, SDLKeycode.SDLK_KP_ENTER -> 13;
            case SDLKeycode.SDLK_ESCAPE -> 27;
            case SDLKeycode.SDLK_SPACE -> 32;
            case SDLKeycode.SDLK_PAGEUP -> 11;
            case SDLKeycode.SDLK_PAGEDOWN -> 12;
            case SDLKeycode.SDLK_END -> 4;
            case SDLKeycode.SDLK_HOME -> 1;
            case SDLKeycode.SDLK_LEFT -> 28;
            case SDLKeycode.SDLK_UP -> 30;
            case SDLKeycode.SDLK_RIGHT -> 29;
            case SDLKeycode.SDLK_DOWN -> 31;
            case SDLKeycode.SDLK_INSERT -> 5;
            case SDLKeycode.SDLK_DELETE -> 127;
            case SDLKeycode.SDLK_KP_0 -> '0';
            case SDLKeycode.SDLK_KP_1 -> '1';
            case SDLKeycode.SDLK_KP_2 -> '2';
            case SDLKeycode.SDLK_KP_3 -> '3';
            case SDLKeycode.SDLK_KP_4 -> '4';
            case SDLKeycode.SDLK_KP_5 -> '5';
            case SDLKeycode.SDLK_KP_6 -> '6';
            case SDLKeycode.SDLK_KP_7 -> '7';
            case SDLKeycode.SDLK_KP_8 -> '8';
            case SDLKeycode.SDLK_KP_9 -> '9';
            case SDLKeycode.SDLK_KP_DIVIDE -> '/';
            case SDLKeycode.SDLK_KP_MULTIPLY -> '*';
            case SDLKeycode.SDLK_KP_MINUS -> '-';
            case SDLKeycode.SDLK_KP_PLUS -> '+';
            case SDLKeycode.SDLK_KP_PERIOD -> '.';
            default -> sdlKeySym;
        };
    }

    // --- Mouse processing methods ---

    public void processMouseMotion(final SDL_MouseMotionEvent event, final float scaleFactor) {
        recordMouseEvent(MOUSE_EVENT.MOVE, event.x() * scaleFactor, event.y() * scaleFactor, 0);
    }

    public void processMouseButtonDown(final SDL_MouseButtonEvent event, final float scaleFactor) {
        recordMouseEvent(MOUSE_EVENT.DOWN, event.x() * scaleFactor, event.y() * scaleFactor, event.button());
    }

    public void processMouseButtonUp(final SDL_MouseButtonEvent event, final float scaleFactor) {
        recordMouseEvent(MOUSE_EVENT.UP, event.x() * scaleFactor, event.y() * scaleFactor, event.button());
    }

    public void processMouseWheel(final SDL_MouseWheelEvent event, final float scaleFactor) {
        addEvent(EVENT_TYPE.MOUSE_WHEEL, 0L, (long) (event.y() * scaleFactor * MOUSE.WHEEL_DELTA_FACTOR), buttons >> 3, 0L);
    }

    private void recordMouseEvent(final MOUSE_EVENT type, final float x, final float y, final int sdlButton) {
        final int currentButtons = buttons & MOUSE.ALL;

        final int newButtonState = switch (type) {
            case DOWN -> currentButtons | mapButton(sdlButton);
            case MOVE -> currentButtons;
            case UP -> currentButtons & ~mapButton(sdlButton);
        };

        buttons = newButtonState | (buttons & ~MOUSE.ALL);

        addEvent(EVENT_TYPE.MOUSE, (int) x, (int) y, buttons & MOUSE.ALL, buttons >> 3);
    }

    private static int mapButton(final int sdlButton) {
        return switch (sdlButton) {
            case SDLMouse.SDL_BUTTON_LEFT -> MOUSE.RED;
            case SDLMouse.SDL_BUTTON_MIDDLE -> MOUSE.YELLOW;
            case SDLMouse.SDL_BUTTON_RIGHT -> MOUSE.BLUE;
            default -> 0;
        };
    }

    // --- Event queue methods ---

    public void recordModifiers(final int sdlModifiers) {
        final int shiftValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LSHIFT | SDLKeycode.SDL_KMOD_RSHIFT)) != 0 ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LCTRL | SDLKeycode.SDL_KMOD_RCTRL)) != 0 ? KEYBOARD.CTRL : 0;
        final int optValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LALT | SDLKeycode.SDL_KMOD_RALT)) != 0 ? KEYBOARD.ALT : 0;
        final int cmdValue = (sdlModifiers & (SDLKeycode.SDL_KMOD_LGUI | SDLKeycode.SDL_KMOD_RGUI)) != 0 ? KEYBOARD.CMD : 0;

        final int modifiers = shiftValue + ctrlValue + optValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
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
        if (eventType == EVENT_TYPE.MOUSE) {
            final long[] lastEvent = deferredEvents.pollLast();
            if (lastEvent != null) {
                if (lastEvent[0] == EVENT_TYPE.MOUSE && lastEvent[4] == value5) {
                    // Throw away event if it is a mouse event with same button state.
                } else {
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
            checkSdlError(SDL_SetWindowTitle(window, title));
        });
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public static String getClipboardData() {
        if (SDL_HasClipboardText()) {
            final String text = SDL_GetClipboardText();
            if (text != null) {
                return text;
            }
            LogUtils.IO.warning("Failed to get clipboard data");
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
