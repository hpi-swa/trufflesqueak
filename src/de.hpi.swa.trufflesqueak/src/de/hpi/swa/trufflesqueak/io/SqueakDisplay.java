/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_BACKSPACE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_DELETE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_DOWN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_END;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_ESCAPE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_HOME;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_INSERT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_0;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_1;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_2;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_3;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_4;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_5;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_6;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_7;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_8;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_9;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_DIVIDE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_ENTER;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_MINUS;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_MULTIPLY;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_PERIOD;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_KP_PLUS;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_LALT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_LCTRL;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_LEFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_LGUI;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_LSHIFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_MODE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_PAGEDOWN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_PAGEUP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RALT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RCTRL;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RETURN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RGUI;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RIGHT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_RSHIFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_SPACE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_TAB;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDLK_UP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_BLENDMODE_NONE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_BUTTON_LEFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_BUTTON_MIDDLE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_BUTTON_RIGHT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_DROP_BEGIN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_DROP_COMPLETE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_DROP_FILE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_DROP_POSITION;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_KEY_DOWN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_KEY_UP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_MOUSE_BUTTON_UP;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_MOUSE_MOTION;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_MOUSE_WHEEL;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_QUIT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_RENDER_DEVICE_RESET;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_RENDER_TARGETS_RESET;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_TEXT_INPUT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_CLOSE_REQUESTED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_DISPLAY_CHANGED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_EXPOSED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_FOCUS_GAINED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_FOCUS_LOST;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_MINIMIZED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_MOUSE_LEAVE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_RESIZED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_LALT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_LCTRL;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_LGUI;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_LSHIFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_MODE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_RALT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_RCTRL;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_RGUI;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_KMOD_RSHIFT;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_PIXELFORMAT_ARGB8888;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_SCALEMODE_NEAREST;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_TEXTUREACCESS_STREAMING;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_WINDOW_HIDDEN;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_WINDOW_HIGH_PIXEL_DENSITY;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_WINDOW_RESIZABLE;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.checkSdlError;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.getSDLError;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.isNull;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.mainThreadCallback;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.runOnMainThread;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.runOnMainThreadAsync;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.warning;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.C_POINTER;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_CreateColorCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_CreateRenderer;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_CreateSurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_CreateTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_CreateWindow;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_DestroyCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_DestroyRenderer;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_DestroySurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_DestroyTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_DestroyWindow;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetClipboardText;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetDesktopDisplayMode;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetPrimaryDisplay;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetRendererName;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetWindowDisplayScale;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetWindowSize;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetWindowSizeInPixels;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_HasClipboardText;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_HideCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_IOFromMem;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_LoadPNG_IO;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_LockSurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_LockTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RaiseWindow;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderClear;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderPresent;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetClipboardText;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetTextureBlendMode;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetTextureScaleMode;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowFullscreen;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowIcon;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowSize;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowTitle;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_ShowCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_ShowWindow;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_StartTextInput;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_UnlockSurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_UnlockTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_UpdateTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_free;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.DRAG;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD_EVENT;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE_EVENT;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.WINDOW;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.trufflesqueak.sdl3.SDL3Utils;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_DisplayMode;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_DropEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_Event;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_KeyboardEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_MainThreadCallback;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_MouseButtonEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_MouseMotionEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_MouseWheelEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_Rect;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_Surface;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_TextInputEvent;
import de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_WindowEvent;
import de.hpi.swa.trufflesqueak.shared.PlatformEventLoop;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.OS;

public final class SqueakDisplay {
    private static final int METAL_LOCK_MIN_BYTES = 64 * 1024;

    public final SqueakImageContext image;

    private volatile MemorySegment window = MemorySegment.NULL;
    private MemorySegment cursor = MemorySegment.NULL;
    private MemorySegment renderer = MemorySegment.NULL;
    private MemorySegment texture = MemorySegment.NULL;

    private boolean isWindowHidden = false;

    // Squeak bitmap (physical pixels)
    private int width;
    private int height;
    private NativeObject bitmap;

    private final AtomicReference<CursorData> cursorData = new AtomicReference<>();

    // Swap ownership under the monitor; SDL only reads upload, and the image only writes staging.
    private PixelBuffer staging = new PixelBuffer();
    private PixelBuffer upload = new PixelBuffer();
    private RenderFrame pendingRepair;
    private final Arena uiArena = Arena.ofAuto();
    private final MemorySegment uploadRect = SDL_Rect.allocate(uiArena);
    private final MemorySegment windowWidthPointer = uiArena.allocate(ValueLayout.JAVA_INT);
    private final MemorySegment windowHeightPointer = uiArena.allocate(ValueLayout.JAVA_INT);
    private final MemorySegment texturePixelsPointer = uiArena.allocate(ValueLayout.ADDRESS);
    private final MemorySegment texturePitchPointer = uiArena.allocate(ValueLayout.JAVA_INT);
    private boolean preferTextureLocking;

    // Dirty 2D bounding box for coalescing damage
    private int dirtyLeft = Integer.MAX_VALUE;
    private int dirtyTop = Integer.MAX_VALUE;
    private int dirtyRight = Integer.MIN_VALUE;
    private int dirtyBottom = Integer.MIN_VALUE;

    private boolean deferUpdates;
    private boolean frameRequested = false;

    // UI thread tracking
    private int textureWidth = -1;
    private int textureHeight = -1;

    private volatile int logicalWindowWidth;
    private volatile int logicalWindowHeight;
    private volatile WindowMetrics windowMetrics = new WindowMetrics(0, 0, 0, 0, 1.0f);

    private final ConcurrentLinkedDeque<long[]> deferredEvents = new ConcurrentLinkedDeque<>();
    private long[] lastMouseMotion;

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private boolean isDragActive = false;

    private int currentEmulatedButton = 0;

    private double pendingScrollX = 0.0;
    private double pendingScrollY = 0.0;

    private volatile String clipboardText;
    private final List<String> dropFilesAccumulator = new ArrayList<>();
    private final int[] primaryDisplayDimensions = {0, 0};

    record CursorData(int[] cursorWords, int[] maskWords, int width, int height, int depth, int offsetX, int offsetY) {
    }

    private record RenderFrame(int width, int height, int left, int top, int right, int bottom) {
    }

    private record WindowMetrics(int width, int height, int pixelWidth, int pixelHeight, float displayScale) {
        private float scaleX() {
            return width > 0 && pixelWidth > 0 ? (float) pixelWidth / width : 1.0f;
        }

        private float scaleY() {
            return height > 0 && pixelHeight > 0 ? (float) pixelHeight / height : 1.0f;
        }

        private int logicalWidth(final int pixels) {
            return width > 0 && pixelWidth > 0 ? (int) Math.ceil((double) pixels * width / pixelWidth) : pixels;
        }

        private int logicalHeight(final int pixels) {
            return height > 0 && pixelHeight > 0 ? (int) Math.ceil((double) pixels * height / pixelHeight) : pixels;
        }
    }

    private static final class PixelBuffer implements AutoCloseable {
        private Arena arena;
        private MemorySegment pixels = MemorySegment.NULL;
        private long capacity;
        private int width;
        private int height;
        private int pitch;

        private boolean ensureSize(final int w, final int h) {
            final int newPitch = Math.multiplyExact(w, Integer.BYTES);
            final long bytes = (long) newPitch * h;
            boolean changed = width != w || height != h;
            if (arena == null || capacity < bytes) {
                close();
                arena = Arena.ofShared();
                pixels = arena.allocate(bytes, Integer.BYTES);
                capacity = bytes;
                changed = true;
            }
            width = w;
            height = h;
            pitch = newPitch;
            return changed;
        }

        private boolean matches(final int w, final int h) {
            return arena != null && width == w && height == h;
        }

        @Override
        public void close() {
            if (arena != null) {
                arena.close();
                arena = null;
            }
            pixels = MemorySegment.NULL;
            capacity = 0;
            width = 0;
            height = 0;
            pitch = 0;
        }
    }

    private final SDL_MainThreadCallback.Function closeTask = (_) -> {
        synchronized (this) {
            staging.close();
            upload.close();
            pendingRepair = null;
            frameRequested = false;
            resetDamage();
        }
        if (!isNull(texture)) {
            SDL_DestroyTexture(texture);
            texture = MemorySegment.NULL;
        }
        if (!isNull(renderer)) {
            SDL_DestroyRenderer(renderer);
            renderer = MemorySegment.NULL;
        }
        if (!isNull(cursor)) {
            SDL_DestroyCursor(cursor);
            cursor = MemorySegment.NULL;
        }
        if (!isNull(window)) {
            SDL_DestroyWindow(window);
            window = MemorySegment.NULL;
        }
    };

    private final MemorySegment setFullscreenTask = mainThreadCallback((isFullScreen) -> checkSdlError(SDL_SetWindowFullscreen(window, isFullScreen.address() == 1L)));

    private final SDL_MainThreadCallback.Function getClipboardTextTask = (_) -> {
        clipboardText = "";
        if (SDL_HasClipboardText()) {
            final MemorySegment textPtr = SDL_GetClipboardText();
            if (!isNull(textPtr)) {
                try {
                    clipboardText = textPtr.getString(0);
                } finally {
                    SDL_free(textPtr);
                }
                return;
            }
            warning("Failed to get clipboard data");
        }
    };

    private final SDL_MainThreadCallback.Function setClipboardTextTask = (text) -> {
        if (!SDL_SetClipboardText(text)) {
            warning("Failed to set clipboard text");
        }
    };

    private final SDL_MainThreadCallback.Function updateTitleTask = (title) -> checkSdlError(SDL_SetWindowTitle(window, title));

    private final MemorySegment setCursorTask = mainThreadCallback((_) -> {
        final CursorData data = cursorData.getAndSet(null);
        if (data == null) {
            return;
        }

        if (isBlankMaskedCursor(data)) {
            checkSdlError(SDL_HideCursor());
            return;
        }

        final int w = data.width;
        final int h = data.height;

        final MemorySegment surface = SDL_CreateSurface(w, h, SDL_PIXELFORMAT_ARGB8888);
        if (isNull(surface)) {
            return;
        }

        // Use SDL_LockSurface to ensure we have CPU access to the pixel buffer
        try {
            checkSdlError(SDL_LockSurface(surface));
            try {
                final MemorySegment pixels = SDL_Surface.pixels(surface);
                final int pitch = SDL_Surface.pitch(surface);
                final int[] sqPixels = data.cursorWords;
                final int[] sqMask = data.maskWords;

                if (data.depth == 32) {
                    /* Case 1: 32-bit ARGB (Direct Copy) */
                    final MemorySegment srcPixels = MemorySegment.ofArray(sqPixels);
                    for (int y = 0; y < h; y++) {
                        final long srcOffset = (long) y * w * Integer.BYTES;
                        final long dstOffset = (long) y * pitch;
                        MemorySegment.copy(srcPixels, srcOffset, pixels, dstOffset, (long) w * Integer.BYTES);
                    }
                } else if (sqMask != null && w == SqueakIOConstants.CURSOR_WIDTH && h == SqueakIOConstants.CURSOR_HEIGHT) {
                    /* Case 2: Legacy 16x16 Masked Cursor */
                    for (int y = 0; y < h; y++) {
                        final int cWord = sqPixels[y];
                        final int mWord = sqMask[y];
                        for (int x = 0; x < w; x++) {
                            final int bit = 0x80000000 >>> x;
                            final boolean c = (cWord & bit) != 0;
                            final boolean m = (mWord & bit) != 0;

                            int argb = 0; // Transparent (0,0)
                            if (m && c) {
                                argb = 0xFF000000;      // Black (1,1)
                            } else if (m) {
                                argb = 0xFFFFFFFF;      // White (1,0)
                            } else if (c) {
                                // True XOR/Invert is not supported by SDL3 ARGB cursors.
                                // Fallback to opaque Black so XOR crosshairs remain visible.
                                argb = 0xFF000000;      // Fallback Black (0,1)
                            }

                            pixels.set(ValueLayout.JAVA_INT, (long) y * pitch + (long) x * 4, argb);
                        }
                    }
                } else {
                    /* Case 3: Arbitrary Sized Monochrome (1-bit) */
                    // Squeak bit-padding: rows are padded to 32-bit boundaries
                    final int wordsPerRow = (w + 31) / 32;
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            final int wordIdx = y * wordsPerRow + (x / 32);
                            final int bitIdx = x % 32;
                            final boolean isSet = (sqPixels[wordIdx] & (0x80000000 >>> bitIdx)) != 0;

                            // Map 1 to Black, 0 to Transparent
                            pixels.set(ValueLayout.JAVA_INT, (long) y * pitch + (long) x * 4, isSet ? 0xFF000000 : 0x00000000);
                        }
                    }
                }
            } finally {
                SDL_UnlockSurface(surface);
            }

            final MemorySegment newCursor = checkSdlError(SDL_CreateColorCursor(surface, data.offsetX, data.offsetY));
            try {
                checkSdlError(SDL_SetCursor(newCursor));
            } catch (final RuntimeException | Error failure) {
                SDL_DestroyCursor(newCursor);
                throw failure;
            }
            if (!isNull(cursor)) {
                SDL_DestroyCursor(cursor);
            }
            cursor = newCursor;
            checkSdlError(SDL_ShowCursor());
        } finally {
            SDL_DestroySurface(surface);
        }
    });

    private final SDL_MainThreadCallback.Function getPrimaryDisplayDimensionsTask = (_) -> {
        final int displayId = SDL_GetPrimaryDisplay();
        if (displayId != 0) {
            final MemorySegment mode = SDL_GetDesktopDisplayMode(displayId);
            if (!isNull(mode)) {
                primaryDisplayDimensions[0] = SDL_DisplayMode.w(mode);
                primaryDisplayDimensions[1] = SDL_DisplayMode.h(mode);
            }
        }
    };

    private SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        // Initial window size is obtained from the image header.
        logicalWindowWidth = image.flags.getScreenWidth();
        logicalWindowHeight = image.flags.getScreenHeight();

        // If window size was not saved, use default as in OSVM.
        if (logicalWindowWidth == 0 || logicalWindowHeight == 0) {
            logicalWindowWidth = 640;
            logicalWindowHeight = 480;
        }
        PlatformEventLoop.start(this::processEvent, this::performRenderIfNeeded);
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        return new SqueakDisplay(image);
    }

    private static boolean isBlankMaskedCursor(final CursorData data) {
        return data.depth == 1 &&
                        data.maskWords != null &&
                        data.width == SqueakIOConstants.CURSOR_WIDTH &&
                        data.height == SqueakIOConstants.CURSOR_HEIGHT &&
                        areAllWordsZero(data.cursorWords, data.height) &&
                        areAllWordsZero(data.maskWords, data.height);
    }

    private static boolean areAllWordsZero(final int[] words, final int expectedLength) {
        if (words == null || words.length < expectedLength) {
            return false;
        }
        for (int i = 0; i < expectedLength; i++) {
            if (words[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public double getDisplayScale() {
        return windowMetrics.displayScale;
    }

    // Called by the main thread at the end of the event loop
    private void performRenderIfNeeded() {
        try {
            final RenderFrame frame = captureFrame();
            if (frame == null) {
                return;
            }

            // All SDL work uses a stable snapshot and runs outside the producer's monitor.
            if (textureWidth != frame.width || textureHeight != frame.height || isNull(texture)) {
                if (!isNull(texture)) {
                    SDL_DestroyTexture(texture);
                    texture = MemorySegment.NULL;
                }
                texture = checkSdlError(SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, frame.width, frame.height));
                textureWidth = frame.width;
                textureHeight = frame.height;
                // The display framebuffer is opaque; ignore its alpha byte.
                checkSdlError(SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_NONE));
                checkSdlError(SDL_SetTextureScaleMode(texture, SDL_SCALEMODE_NEAREST));
            }

            SDL_Rect.x(uploadRect, frame.left);
            SDL_Rect.y(uploadRect, frame.top);
            SDL_Rect.w(uploadRect, frame.right - frame.left);
            SDL_Rect.h(uploadRect, frame.bottom - frame.top);
            uploadTexture(frame);
            checkSdlError(SDL_RenderClear(renderer));
            checkSdlError(SDL_RenderTexture(renderer, texture, MemorySegment.NULL, MemorySegment.NULL));
            checkSdlError(SDL_RenderPresent(renderer));

            // Compare pixels directly; fractional scaling cannot be round-tripped through ceil.
            if (isWindowHidden && frame.width == getWindowWidth() && frame.height == getWindowHeight()) {
                checkSdlError(SDL_ShowWindow(window));
                isWindowHidden = false;
                checkSdlError(SDL_RaiseWindow(window));
            }
        } catch (final RuntimeException failure) {
            // Retain damage for the next update/expose without spinning on a failed renderer.
            if (!isNull(texture)) {
                SDL_DestroyTexture(texture);
                texture = MemorySegment.NULL;
            }
            fullDamage();
            warning("Rendering failed: " + failure);
        }
    }

    private void uploadTexture(final RenderFrame frame) {
        final long offset = (long) frame.top * upload.pitch + (long) frame.left * Integer.BYTES;
        final long rowBytes = (long) (frame.right - frame.left) * Integer.BYTES;
        final int rows = frame.bottom - frame.top;
        // Metal benefits from mapped uploads for larger updates; retain UpdateTexture elsewhere.
        if (preferTextureLocking && rowBytes * rows >= METAL_LOCK_MIN_BYTES && SDL_LockTexture(texture, uploadRect, texturePixelsPointer, texturePitchPointer)) {
            try {
                final int pitch = texturePitchPointer.get(ValueLayout.JAVA_INT, 0);
                if (pitch < rowBytes) {
                    throw new IllegalStateException("Locked texture pitch is shorter than a row");
                }
                // The borrowed SDL pixels remain valid until UnlockTexture, including row padding.
                final MemorySegment pixels = checkSdlError(texturePixelsPointer.get(C_POINTER, 0)).asSlice(0, (long) pitch * (rows - 1) + rowBytes);
                if (rowBytes == upload.pitch && rowBytes == pitch) {
                    MemorySegment.copy(upload.pixels, offset, pixels, 0, rowBytes * rows);
                } else {
                    for (int y = 0; y < rows; y++) {
                        MemorySegment.copy(upload.pixels, offset + (long) y * upload.pitch, pixels, (long) y * pitch, rowBytes);
                    }
                }
            } finally {
                SDL_UnlockTexture(texture);
            }
        } else {
            checkSdlError(SDL_UpdateTexture(texture, uploadRect, upload.pixels.asSlice(offset), upload.pitch));
        }
    }

    private synchronized RenderFrame captureFrame() {
        if (!frameRequested) {
            return null;
        }
        frameRequested = false;
        final PixelBuffer published = pendingRepair == null ? staging : upload;
        if (isNull(renderer) || !published.matches(width, height)) {
            return null;
        }
        int top = Math.max(0, dirtyTop);
        int bottom = Math.min(height, dirtyBottom);
        int left = Math.max(0, dirtyLeft);
        int right = Math.min(width, dirtyRight);
        if (textureWidth != width || textureHeight != height || isNull(texture)) {
            // Every pixel of a new texture must be initialized, including after device reset.
            left = 0;
            top = 0;
            right = width;
            bottom = height;
        } else if (right - left >= (long) width * 3 / 4) {
            left = 0;
            right = width;
        }
        if (top >= bottom || left >= right) {
            resetDamage();
            return null;
        }
        final RenderFrame frame = new RenderFrame(width, height, left, top, right, bottom);
        if (pendingRepair == null) {
            final PixelBuffer spare = upload;
            upload = staging;
            staging = spare;
            pendingRepair = frame;
        }
        resetDamage();
        return frame;
    }

    private static void copyPixels(final MemorySegment source, final MemorySegment destination, final int pitch, final int left, final int top, final int right, final int bottom) {
        long offset = (long) top * pitch + (long) left * Integer.BYTES;
        final long rowBytes = (long) (right - left) * Integer.BYTES;
        if (rowBytes == pitch) {
            MemorySegment.copy(source, offset, destination, offset, (long) (bottom - top) * pitch);
        } else {
            for (int y = top; y < bottom; y++) {
                MemorySegment.copy(source, offset, destination, offset, rowBytes);
                offset += pitch;
            }
        }
    }

    private void requestRender() {
        synchronized (this) {
            // Strictly synchronized bounds check
            if (dirtyTop >= dirtyBottom || dirtyLeft >= dirtyRight) {
                return;
            }

            if (frameRequested) {
                return;
            }

            // Claim the frame
            frameRequested = true;
        }

        wakeUpEventLoopToRenderFrame();
    }

    @TruffleBoundary
    private static void wakeUpEventLoopToRenderFrame() {
        PlatformEventLoop.wakeUp();
    }

    public void showDisplayBits(final PointersObject aForm, final int left, final int top, final int right, final int bottom) {
        if (!deferUpdates && aForm.isDisplay(image)) {
            updateDisplay(left, top, right, bottom);
        }
    }

    public void updateDisplay(final int left, final int top, final int right, final int bottom) {
        if (left <= right && top <= bottom) {
            ioShowDisplay(left, top, right, bottom);
        }
    }

    @TruffleBoundary
    private void ioShowDisplay(final int left, final int top, final int right, final int bottom) {
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

            if (bitmap == null) {
                return;
            }
            final int[] sqPixels = bitmap.getIntStorage();
            if (sqPixels.length < (long) currentWidth * currentHeight) {
                return;
            }

            // Wrap the Squeak pixel array into a Panama segment
            final MemorySegment srcSegment = MemorySegment.ofArray(sqPixels);

            final PixelBuffer published = pendingRepair == null ? staging : upload;
            final boolean layoutChanged = !published.matches(currentWidth, currentHeight);
            final boolean storageChanged = staging.ensureSize(currentWidth, currentHeight);
            if (layoutChanged) {
                // A changed layout needs a complete snapshot before any native upload.
                MemorySegment.copy(srcSegment, 0, staging.pixels, 0, (long) staging.pitch * currentHeight);
                pendingRepair = null;
                recordDamage(0, 0, currentWidth, currentHeight);
            } else {
                final int rowInts = safeRight - safeLeft;
                final boolean wholeRows = rowInts >= (long) currentWidth * 3 / 4;
                final int copyLeft = wholeRows ? 0 : safeLeft;
                final int copyRight = wholeRows ? currentWidth : safeRight;
                if (pendingRepair != null) {
                    final int repairLeft = storageChanged ? 0 : pendingRepair.left;
                    final int repairTop = storageChanged ? 0 : pendingRepair.top;
                    final int repairRight = storageChanged ? currentWidth : pendingRepair.right;
                    final int repairBottom = storageChanged ? currentHeight : pendingRepair.bottom;
                    // Restore the spare from published pixels, never from unreported bitmap data.
                    // A replacement that covers the missing region needs no repair copy at all.
                    if (copyLeft > repairLeft || safeTop > repairTop || copyRight < repairRight || safeBottom < repairBottom) {
                        copyPixels(upload.pixels, staging.pixels, staging.pitch, repairLeft, repairTop, repairRight, repairBottom);
                    }
                    pendingRepair = null;
                }
                copyPixels(srcSegment, staging.pixels, staging.pitch, copyLeft, safeTop, copyRight, safeBottom);
                recordDamage(safeLeft, safeTop, safeRight, safeBottom);
            }
        }

        requestRender();
    }

    private void recordDamage(final int left, final int top, final int right, final int bottom) {
        dirtyLeft = Math.clamp(left, 0, dirtyLeft);
        dirtyTop = Math.clamp(top, 0, dirtyTop);
        dirtyRight = Math.clamp(right, dirtyRight, width);
        dirtyBottom = Math.clamp(bottom, dirtyBottom, height);
    }

    private void resetDamage() {
        dirtyLeft = Integer.MAX_VALUE;
        dirtyTop = Integer.MAX_VALUE;
        dirtyRight = Integer.MIN_VALUE;
        dirtyBottom = Integer.MIN_VALUE;
    }

    private void fullDamage() {
        synchronized (this) {
            recordDamage(0, 0, width, height);
        }
    }

    private void clampDamageToBounds() {
        // Ensure damage is within the current bounds.
        dirtyLeft = Math.clamp(dirtyLeft, 0, width);
        dirtyTop = Math.clamp(dirtyTop, 0, height);
        dirtyRight = Math.clamp(dirtyRight, 0, width);
        dirtyBottom = Math.clamp(dirtyBottom, 0, height);
    }

    @TruffleBoundary
    public void close() {
        runOnMainThread(closeTask, MemorySegment.NULL);
    }

    public int getLogicalWindowWidth() {
        return logicalWindowWidth > 0 ? logicalWindowWidth : width;
    }

    public int getLogicalWindowHeight() {
        return logicalWindowHeight > 0 ? logicalWindowHeight : height;
    }

    public int getWindowWidth() {
        final WindowMetrics metrics = windowMetrics;
        return metrics.pixelWidth > 0 ? metrics.pixelWidth : getLogicalWindowWidth();
    }

    public int getWindowHeight() {
        final WindowMetrics metrics = windowMetrics;
        return metrics.pixelHeight > 0 ? metrics.pixelHeight : getLogicalWindowHeight();
    }

    private void updateWindowMetrics() {
        if (isNull(window)) {
            return;
        }
        checkSdlError(SDL_GetWindowSize(window, windowWidthPointer, windowHeightPointer));
        final int windowWidth = windowWidthPointer.get(ValueLayout.JAVA_INT, 0);
        final int windowHeight = windowHeightPointer.get(ValueLayout.JAVA_INT, 0);
        checkSdlError(SDL_GetWindowSizeInPixels(window, windowWidthPointer, windowHeightPointer));
        final int pixelWidth = windowWidthPointer.get(ValueLayout.JAVA_INT, 0);
        final int pixelHeight = windowHeightPointer.get(ValueLayout.JAVA_INT, 0);
        final float displayScale = checkSdlError(SDL_GetWindowDisplayScale(window));
        logicalWindowWidth = windowWidth;
        logicalWindowHeight = windowHeight;
        windowMetrics = new WindowMetrics(windowWidth, windowHeight, pixelWidth, pixelHeight, displayScale);
    }

    @TruffleBoundary
    public void setFullscreen(final boolean fullscreen) {
        if (isNull(window)) {
            return;
        }
        runOnMainThreadAsync(setFullscreenTask, MemorySegment.ofAddress(fullscreen ? 1L : 0L));
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
            if (bitmap != newBitmap) {
                // A same-sized replacement bitmap also needs a complete initial snapshot.
                staging.width = 0;
                staging.height = 0;
                pendingRepair = null;
            }
            bitmap = newBitmap;
            width = newWidth;
            height = newHeight;
            clampDamageToBounds();
        }

        // Wait for the main thread to finish SDL_Init
        PlatformEventLoop.waitUntilInitialized();

        if (isNull(window)) {
            final String imageFileName = new File(image.getImagePath()).getName();
            final String title = imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME) ? imageFileName
                            : imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
            /*
             * Keep the window hidden until a frame at its actual pixel size has been rendered.
             */
            runOnMainThread((_) -> {
                try {
                    // The window is initially hidden. It will be shown when rendered at full size.
                    isWindowHidden = true;
                    final long windowFlags = SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIDDEN | SDL_WINDOW_HIGH_PIXEL_DENSITY;

                    try (Arena windowTitleArena = Arena.ofConfined()) {
                        window = checkSdlError(SDL_CreateWindow(windowTitleArena.allocateFrom(title), logicalWindowWidth, logicalWindowHeight, windowFlags));
                    }

                    renderer = checkSdlError(SDL_CreateRenderer(window, MemorySegment.NULL));
                    preferTextureLocking = "metal".equals(checkSdlError(SDL_GetRendererName(renderer)).getString(0));

                    setWindowIcon(window);

                    updateWindowMetrics();

                    checkSdlError(SDL_StartTextInput(window));
                    fullDamage();
                    if (cursorData.get() != null) {
                        runOnMainThreadAsync(setCursorTask, MemorySegment.NULL);
                    }
                    if (OS.isMacOS()) {
                        MacMenuShortcutDisabler.disableSystemShortcuts();
                    }
                } catch (final RuntimeException | Error failure) {
                    closeTask.apply(MemorySegment.NULL);
                    throw failure;
                }
            }, MemorySegment.NULL);
        }
    }

    private static void setWindowIcon(final MemorySegment window) {
        try (InputStream is = SqueakDisplay.class.getResourceAsStream("/trufflesqueak-icon.png")) {
            if (is == null) {
                warning("The icon file /trufflesqueak-icon.png was not found.");
                return;
            }
            final byte[] imageBytes = is.readAllBytes();

            try (Arena arena = Arena.ofConfined()) {
                final MemorySegment imageBuffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, imageBytes);
                final MemorySegment ioStream = SDL_IOFromMem(imageBuffer, imageBytes.length);
                if (isNull(ioStream)) {
                    warning("SDL_IOFromMem failed: " + getSDLError());
                    return;
                }

                // Load the PNG directly from the IO stream
                final MemorySegment iconSurface = SDL_LoadPNG_IO(ioStream, true);

                if (!isNull(iconSurface)) {
                    if (!SDL_SetWindowIcon(window, iconSurface)) {
                        warning("SDL_SetWindowIcon failed: " + getSDLError());
                    }
                    SDL_DestroySurface(iconSurface);
                } else {
                    warning("SDL_LoadPNG_IO failed: " + getSDLError());
                }
            }
        } catch (final IOException e) {
            warning("Failed to load and set icon: " + e);
        }
    }

    @TruffleBoundary
    public void resizeTo(final int newWidth, final int newHeight) {
        synchronized (this) {
            width = newWidth;
            height = newHeight;
            clampDamageToBounds();
        }

        final WindowMetrics metrics = windowMetrics;
        final int targetLogicalWidth = metrics.logicalWidth(newWidth);
        final int targetLogicalHeight = metrics.logicalHeight(newHeight);

        logicalWindowWidth = targetLogicalWidth;
        logicalWindowHeight = targetLogicalHeight;

        // Save current logical window size in flags for later writing to disk image
        image.flags.setScreenSize(getLogicalWindowWidth(), getLogicalWindowHeight());

        if (!isNull(window)) {
            runOnMainThread((_) -> {
                checkSdlError(SDL_SetWindowSize(window, targetLogicalWidth, targetLogicalHeight));
                updateWindowMetrics();
            }, MemorySegment.NULL);
        }

        fullDamage();
    }

    @TruffleBoundary
    public boolean isVisible() {
        return !isNull(window);
    }

    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] maskWords, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
        assert depth == 1 || depth == 32 : "Bad cursor depth: " + depth;
        cursorData.set(new CursorData(cursorWords.clone(), maskWords == null ? null : maskWords.clone(), width, height, depth, offsetX, offsetY));
        if (isNull(window)) {
            return;
        }
        runOnMainThreadAsync(setCursorTask, MemorySegment.NULL);
    }

    public void processEvent(final MemorySegment event) {
        final int type = SDL_Event.type(event);
        final WindowMetrics metrics = windowMetrics;

        switch (type) {
            case SDL_EVENT_KEY_DOWN:
                processKeyDown(SDL_KeyboardEvent.key(event), SDL_KeyboardEvent.mod(event));
                break;
            case SDL_EVENT_KEY_UP:
                processKeyUp(SDL_KeyboardEvent.key(event), SDL_KeyboardEvent.mod(event));
                break;
            case SDL_EVENT_TEXT_INPUT:
                processTextInput(SDL_TextInputEvent.text(event));
                break;
            case SDL_EVENT_MOUSE_MOTION:
                processMouseMotion(event, metrics.scaleX(), metrics.scaleY());
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
                processMouseButtonDown(event, metrics.scaleX(), metrics.scaleY());
                break;
            case SDL_EVENT_MOUSE_BUTTON_UP:
                processMouseButtonUp(event, metrics.scaleX(), metrics.scaleY());
                break;
            case SDL_EVENT_MOUSE_WHEEL:
                processMouseWheel(event);
                break;
            case SDL_EVENT_DROP_BEGIN:
                isDragActive = true;
                dropFilesAccumulator.clear();
                addDragEvent(DRAG.ENTER, 0, 0);
                break;
            case SDL_EVENT_DROP_POSITION: {
                final int x = (int) (SDL_DropEvent.x(event) * metrics.scaleX());
                final int y = (int) (SDL_DropEvent.y(event) * metrics.scaleY());
                addDragEvent(DRAG.MOVE, x, y);
                break;
            }
            case SDL_EVENT_DROP_FILE: {
                // Read the C-string directly from the memory pointer
                final MemorySegment dataPtr = SDL_DropEvent.data(event);
                if (!isNull(dataPtr)) {
                    dropFilesAccumulator.add(dataPtr.getString(0));
                }
                break;
            }
            case SDL_EVENT_DROP_COMPLETE: {
                isDragActive = false;
                image.dropPluginFileList = dropFilesAccumulator.toArray(new String[0]);
                final int x = (int) (SDL_DropEvent.x(event) * metrics.scaleX());
                final int y = (int) (SDL_DropEvent.y(event) * metrics.scaleY());
                addDragEvent(DRAG.DROP, x, y);
                dropFilesAccumulator.clear();
                break;
            }
            case SDL_EVENT_WINDOW_MOUSE_LEAVE:
                if (isDragActive) {
                    addDragEvent(DRAG.LEAVE, 0, 0);
                    isDragActive = false;
                }
                break;
            case SDL_EVENT_QUIT, SDL_EVENT_WINDOW_CLOSE_REQUESTED:
                addWindowEvent(WINDOW.CLOSE);
                break;
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED, SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED:
                updateWindowMetrics();
                addWindowEvent(WINDOW.CHANGED_SCREEN);
                break;
            case SDL_EVENT_WINDOW_EXPOSED:
                addWindowEvent(WINDOW.PAINT);
                fullDamage();
                requestRender();
                break;
            case SDL_EVENT_WINDOW_MINIMIZED:
                addWindowEvent(WINDOW.ICONISE);
                break;
            case SDL_EVENT_WINDOW_FOCUS_GAINED:
                addWindowEvent(WINDOW.ACTIVATED);
                break;
            case SDL_EVENT_WINDOW_FOCUS_LOST:
                addWindowEvent(WINDOW.DEACTIVATED);
                break;
            case SDL_EVENT_WINDOW_RESIZED:
                logicalWindowWidth = SDL_WindowEvent.data1(event);
                logicalWindowHeight = SDL_WindowEvent.data2(event);
                updateWindowMetrics();
                image.flags.setScreenSize(getLogicalWindowWidth(), getLogicalWindowHeight());
                addWindowEvent(WINDOW.METRIC_CHANGE);
                fullDamage();
                requestRender();
                break;
            case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED:
                updateWindowMetrics();
                addWindowEvent(WINDOW.METRIC_CHANGE);
                fullDamage();
                requestRender();
                break;
            case SDL_EVENT_RENDER_TARGETS_RESET, SDL_EVENT_RENDER_DEVICE_RESET:
                if (!isNull(texture)) {
                    SDL_DestroyTexture(texture);
                    texture = MemorySegment.NULL;
                }
                fullDamage();
                requestRender();
                break;
        }
    }

    // --- Keyboard processing methods ---

    public void processKeyDown(final int sdlKeySym, final int sdlModifiers) {
        recordModifiers(sdlModifiers);

        if (isModifier(sdlKeySym)) {
            return;
        }

        final boolean isCommandOrCtrl = (buttons & (KEYBOARD.CTRL | KEYBOARD.CMD)) != 0;
        final int keyChar = toSqueakKey(sdlKeySym);

        /*
         * OSVM: Windows signals the interrupt; Linux enqueues the key; macOS does both.
         * See https://share.gemini.google/IHLpmtSinLV6
         * Doing both on macOS Cuis 7.3/7.5 can cause an interrupt of the interrupt notifier
         * on the running process; Squeak & Cuis 7.6 work as expected.
         */
        if (isCommandOrCtrl && keyChar == '.') {
            image.interrupt.setInterruptPending();
        }

        addKeyboardEvent(KEYBOARD_EVENT.DOWN, keyChar);

        if (isControlKey(sdlKeySym) || isCommandOrCtrl) {
            if (keyChar <= 65535) {
                addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyChar);
            }
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

    public void processTextInput(final MemorySegment textPtr) {
        if (isNull(textPtr)) {
            return;
        }
        final boolean isCommandOrCtrl = (buttons & (KEYBOARD.CTRL | KEYBOARD.CMD)) != 0;
        if (isCommandOrCtrl) {
            return;
        }
        try {
            final String text = textPtr.getString(0);
            text.codePoints().forEach(codePoint -> addKeyboardEvent(KEYBOARD_EVENT.CHAR, codePoint));
        } catch (final IllegalArgumentException | IndexOutOfBoundsException e) {
            SDL3Utils.warning("Error processing text input: " + e.getMessage());
        }
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, buttons >> 3, keyCharOrCode);
    }

    private static boolean isModifier(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLK_LSHIFT, SDLK_RSHIFT, SDLK_LCTRL, SDLK_RCTRL, SDLK_LALT, SDLK_RALT, SDLK_LGUI, SDLK_RGUI, SDLK_MODE -> true;
            default -> false;
        };
    }

    private static boolean isControlKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLK_BACKSPACE, SDLK_TAB, SDLK_RETURN, SDLK_KP_ENTER, SDLK_ESCAPE, SDLK_PAGEUP, SDLK_PAGEDOWN, SDLK_END, //
                SDLK_HOME, SDLK_LEFT, SDLK_UP, SDLK_RIGHT, SDLK_DOWN, SDLK_INSERT, SDLK_DELETE -> true;
            default -> false;
        };
    }

    private static int toSqueakKey(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLK_BACKSPACE -> 8;
            case SDLK_TAB -> 9;
            case SDLK_RETURN, SDLK_KP_ENTER -> 13;
            case SDLK_ESCAPE -> 27;
            case SDLK_SPACE -> 32;
            case SDLK_PAGEUP -> 11;
            case SDLK_PAGEDOWN -> 12;
            case SDLK_END -> 4;
            case SDLK_HOME -> 1;
            case SDLK_LEFT -> 28;
            case SDLK_UP -> 30;
            case SDLK_RIGHT -> 29;
            case SDLK_DOWN -> 31;
            case SDLK_INSERT -> 5;
            case SDLK_DELETE -> 127;
            case SDLK_KP_0 -> '0';
            case SDLK_KP_1 -> '1';
            case SDLK_KP_2 -> '2';
            case SDLK_KP_3 -> '3';
            case SDLK_KP_4 -> '4';
            case SDLK_KP_5 -> '5';
            case SDLK_KP_6 -> '6';
            case SDLK_KP_7 -> '7';
            case SDLK_KP_8 -> '8';
            case SDLK_KP_9 -> '9';
            case SDLK_KP_DIVIDE -> '/';
            case SDLK_KP_MULTIPLY -> '*';
            case SDLK_KP_MINUS -> '-';
            case SDLK_KP_PLUS -> '+';
            case SDLK_KP_PERIOD -> '.';
            default -> sdlKeySym;
        };
    }

// --- Mouse processing methods ---

    public void processMouseMotion(final MemorySegment event, final float scale) {
        processMouseMotion(event, scale, scale);
    }

    private void processMouseMotion(final MemorySegment event, final float scaleX, final float scaleY) {
        recordMouseEvent(MOUSE_EVENT.MOVE, SDL_MouseMotionEvent.x(event) * scaleX, SDL_MouseMotionEvent.y(event) * scaleY, 0);
    }

    public void processMouseButtonDown(final MemorySegment event, final float scale) {
        processMouseButtonDown(event, scale, scale);
    }

    private void processMouseButtonDown(final MemorySegment event, final float scaleX, final float scaleY) {
        recordMouseEvent(MOUSE_EVENT.DOWN, SDL_MouseButtonEvent.x(event) * scaleX, SDL_MouseButtonEvent.y(event) * scaleY, SDL_MouseButtonEvent.button(event));
    }

    public void processMouseButtonUp(final MemorySegment event, final float scale) {
        processMouseButtonUp(event, scale, scale);
    }

    private void processMouseButtonUp(final MemorySegment event, final float scaleX, final float scaleY) {
        recordMouseEvent(MOUSE_EVENT.UP, SDL_MouseButtonEvent.x(event) * scaleX, SDL_MouseButtonEvent.y(event) * scaleY, SDL_MouseButtonEvent.button(event));
    }

    public void processMouseWheel(final MemorySegment event) {
        // --- Accumulate raw fractional deltas ---
        final double currentDeltaX = SDL_MouseWheelEvent.x(event) * MOUSE.WHEEL_DELTA_FACTOR;
        final double currentDeltaY = SDL_MouseWheelEvent.y(event) * MOUSE.WHEEL_DELTA_FACTOR;

        pendingScrollX += currentDeltaX;
        pendingScrollY += currentDeltaY;

        // --- Extract integer portions ---
        final long finalScrollX = (long) pendingScrollX;
        final long finalScrollY = (long) pendingScrollY;

        if (finalScrollX != 0L || finalScrollY != 0L) {
            // Keep the fractional remainders
            pendingScrollX -= finalScrollX;
            pendingScrollY -= finalScrollY;

            addEvent(EVENT_TYPE.MOUSE_WHEEL, finalScrollX, finalScrollY, buttons & MOUSE.ALL, buttons >> 3);
        }
    }

    private void recordMouseEvent(final MOUSE_EVENT type, final float x, final float y, final int sdlButton) {
        final int currentButtons = buttons & MOUSE.ALL;

        // Resolve Emulated Button on DOWN
        if (type == MOUSE_EVENT.DOWN && sdlButton == SDL_BUTTON_LEFT) {
            if ((buttons & KEYBOARD.CMD) != 0) {
                currentEmulatedButton = MOUSE.BLUE;   // Cmd + Click = Right
            } else if ((buttons & KEYBOARD.ALT) != 0) {
                currentEmulatedButton = MOUSE.YELLOW; // Option/Alt + Click = Middle
            } else {
                currentEmulatedButton = MOUSE.RED;    // Normal Left Click
            }
        }

        final int eventButton = switch (sdlButton) {
            case SDL_BUTTON_LEFT -> currentEmulatedButton;
            case SDL_BUTTON_MIDDLE -> MOUSE.YELLOW;
            case SDL_BUTTON_RIGHT -> MOUSE.BLUE;
            default -> 0;
        };

        // Strip the emulated button trigger modifier
        final int modifiersForEvent = switch (currentEmulatedButton) {
            case MOUSE.BLUE -> (buttons & ~KEYBOARD.CMD) >> 3;
            case MOUSE.YELLOW -> (buttons & ~KEYBOARD.ALT) >> 3;
            default -> buttons >> 3;
        };

        final int newButtonState = switch (type) {
            case DOWN -> currentButtons | eventButton;
            case MOVE -> currentButtons;
            case UP -> currentButtons & ~eventButton;
        };

        // Clean up emulation state on UP
        if (type == MOUSE_EVENT.UP && sdlButton == SDL_BUTTON_LEFT) {
            currentEmulatedButton = 0;
        }

        buttons = newButtonState | (buttons & ~MOUSE.ALL);

        addEvent(EVENT_TYPE.MOUSE, (int) x, (int) y, buttons & MOUSE.ALL, modifiersForEvent, 0L, type == MOUSE_EVENT.MOVE);
    }

    // --- Event queue methods ---

    public void recordModifiers(final int sdlModifiers) {
        final int shiftValue = (sdlModifiers & (SDL_KMOD_LSHIFT | SDL_KMOD_RSHIFT)) != 0 ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = (sdlModifiers & (SDL_KMOD_LCTRL | SDL_KMOD_RCTRL)) != 0 ? KEYBOARD.CTRL : 0;

        final int optValue;
        final int cmdValue;

        if (OS.isMacOS()) {
            // macOS: Pure 1:1 physical key mapping
            optValue = (sdlModifiers & (SDL_KMOD_LALT | SDL_KMOD_RALT)) != 0 ? KEYBOARD.ALT : 0;
            cmdValue = (sdlModifiers & (SDL_KMOD_LGUI | SDL_KMOD_RGUI)) != 0 ? KEYBOARD.CMD : 0;
        } else {
            // Windows reports AltGr as RAlt; X11/Wayland use MODE.
            final boolean isAltGr = (sdlModifiers & SDL_KMOD_MODE) != 0 || OS.isWindows() && (sdlModifiers & SDL_KMOD_RALT) != 0;
            final int commandAltMask = isAltGr ? SDL_KMOD_LALT : (SDL_KMOD_LALT | SDL_KMOD_RALT);
            optValue = isAltGr ? KEYBOARD.ALT : 0;
            cmdValue = (sdlModifiers & (commandAltMask | SDL_KMOD_LGUI | SDL_KMOD_RGUI)) != 0 ? KEYBOARD.CMD : 0;
        }

        final int modifiers = shiftValue + ctrlValue + optValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
    }

    @TruffleBoundary
    public long[] getNextEvent() {
        return deferredEvents.pollFirst();
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        addEvent(eventType, value3, value4, value5, value6, 0L);
    }

    private void addDragEvent(final long type, final int x, final int y) {
        addEvent(EVENT_TYPE.DRAG_DROP_FILES, type, x, y, buttons >> 3, image.dropPluginFileList.length);
    }

    private void addWindowEvent(final long type) {
        addEvent(EVENT_TYPE.WINDOW, type, 0L, 0L, 0L);
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6, final long value7) {
        addEvent(eventType, value3, value4, value5, value6, value7, false);
    }

    private void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6, final long value7, final boolean mouseMotion) {
        if (mouseMotion && lastMouseMotion != null && lastMouseMotion[4] == value5 && lastMouseMotion[5] == value6) {
            // Only successive motion events may be replaced; keep press/release coordinates.
            deferredEvents.removeLastOccurrence(lastMouseMotion);
        }
        final long[] event = {eventType, getEventTime(), value3, value4, value5, value6, value7, HostWindowPlugin.DEFAULT_HOST_WINDOW_ID};
        deferredEvents.addLast(event);
        lastMouseMotion = mouseMotion ? event : null;
        if (image.options.signalInputSemaphore() && inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }

    private long getEventTime() {
        return System.currentTimeMillis() - image.startUpMillis;
    }

    public void setDeferUpdates(final boolean flag) {
        // When set to true, the image will use the primitive associated with showDisplayRect()
        // to force the screen update. This is used in old version of Cuis (~7.3).
        final boolean oldDeferUpdates = deferUpdates;
        deferUpdates = flag;

        // If we were deferring updates and are no longer, force the screen to redraw.
        if (oldDeferUpdates && !deferUpdates) {
            updateDisplay(0, 0, width, height);
        }
    }

    @TruffleBoundary
    public void setWindowTitle(final String title) {
        if (isNull(window)) {
            warning("Cannot set window title because window is not created yet");
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            runOnMainThread(updateTitleTask, arena.allocateFrom(title));
        }
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public String getClipboardData() {
        runOnMainThread(getClipboardTextTask, MemorySegment.NULL);
        return clipboardText;
    }

    @TruffleBoundary
    public void setClipboardData(final String text) {
        try (Arena arena = Arena.ofConfined()) {
            runOnMainThread(setClipboardTextTask, arena.allocateFrom(text));
        }
    }

    @TruffleBoundary
    public int[] getPrimaryDisplayDimensions() {
        runOnMainThread(getPrimaryDisplayDimensionsTask, MemorySegment.NULL);
        return primaryDisplayDimensions;
    }
}
