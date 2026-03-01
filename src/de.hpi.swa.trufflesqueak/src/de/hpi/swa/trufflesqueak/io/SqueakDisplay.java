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
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_EXPOSED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_FOCUS_GAINED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_FOCUS_LOST;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_MINIMIZED;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_EVENT_WINDOW_MOUSE_LEAVE;
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
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_WINDOW_HIGH_PIXEL_DENSITY;
import static de.hpi.swa.trufflesqueak.sdl3.SDL3Constants.SDL_WINDOW_RESIZABLE;
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
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetError;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetPrimaryDisplay;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_GetWindowDisplayScale;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_HasClipboardText;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_IOFromMem;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_LoadPNG_IO;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_LockSurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RaiseWindow;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderClear;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderPresent;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RenderTexture;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_RunOnMainThread;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetClipboardText;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetCursor;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetTextureScaleMode;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowFullscreen;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowIcon;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowSize;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_SetWindowTitle;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_StartTextInput;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_UnlockSurface;
import static de.hpi.swa.trufflesqueak.sdl3.bindings.SDL_h.SDL_UpdateTexture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

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
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.OS;

public final class SqueakDisplay {
    public final SqueakImageContext image;

    private MemorySegment window = MemorySegment.NULL;
    private MemorySegment cursor = MemorySegment.NULL;
    private MemorySegment renderer = MemorySegment.NULL;
    private MemorySegment texture = MemorySegment.NULL;

    // Squeak bitmap (physical pixels)
    private int width;
    private int height;
    private NativeObject bitmap;

    private CursorData cursorData;

    // Async Staging Buffer (Managed by Project Panama)
    private MemorySegment stagingBuffer = MemorySegment.NULL;
    private Arena stagingArena = null;
    private int stagingCapacity = 0;
    private int stagingPitchBytes = 0;

    // Dirty 2D bounding box for coalescing damage
    private int dirtyLeft = Integer.MAX_VALUE;
    private int dirtyTop = Integer.MAX_VALUE;
    private int dirtyRight = Integer.MIN_VALUE;
    private int dirtyBottom = Integer.MIN_VALUE;

    private boolean deferUpdates;
    private boolean frameRequested = false;

    // UI Thread Tracking (Texture & Logical Window)
    private int textureWidth = -1;
    private int textureHeight = -1;
    private volatile int osWindowWidth;
    private volatile int osWindowHeight;

    private float scaleFactor;

    private final ConcurrentLinkedDeque<long[]> deferredEvents = new ConcurrentLinkedDeque<>();

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private boolean isDragActive = false;

    private int currentEmulatedButton = 0;

    private double pendingScrollX = 0.0;
    private double pendingScrollY = 0.0;

    private final List<String> dropFilesAccumulator = new ArrayList<>();
    private final int[] primaryDisplayDimensions = {0, 0};

    record CursorData(int[] cursorWords, int[] maskWords, int width, int height, int depth, int offsetX, int offsetY) {
    }

    private String title = "TruffleSqueak";

    private final MemorySegment getPrimaryDisplayDimensionsTask = SDL_MainThreadCallback.allocate((_) -> {
        final int displayId = SDL_GetPrimaryDisplay();
        if (displayId != 0) {
            final MemorySegment mode = SDL_GetDesktopDisplayMode(displayId);
            if (mode != MemorySegment.NULL) {
                primaryDisplayDimensions[0] = SDL_DisplayMode.w(mode);
                primaryDisplayDimensions[1] = SDL_DisplayMode.h(mode);
            }
        }
    }, Arena.global());

    private final MemorySegment setFullscreenTask = SDL_MainThreadCallback.allocate((userdata) -> checkSdlError(SDL_SetWindowFullscreen(window, userdata.address() == 1L)), Arena.global());

    private final MemorySegment resizeTask = SDL_MainThreadCallback.allocate((_ /* userdata */) -> checkSdlError(SDL_SetWindowSize(window, osWindowWidth, osWindowHeight)), Arena.global());

    private final MemorySegment updateTitleTask = SDL_MainThreadCallback.allocate((_ /* userdata */) -> {
        try (Arena arena = Arena.ofConfined()) {
            checkSdlError(SDL_SetWindowTitle(window, arena.allocateFrom(title)));
        }
    }, Arena.global());

    private final MemorySegment setCursorTask = SDL_MainThreadCallback.allocate((_ /* userdata */) -> {
        if (cursorData == null) {
            return;
        }
        if (cursor != MemorySegment.NULL) {
            SDL_DestroyCursor(cursor);
            cursor = MemorySegment.NULL;
        }

        final int w = cursorData.width;
        final int h = cursorData.height;

        final MemorySegment surface = SDL_CreateSurface(w, h, SDL_PIXELFORMAT_ARGB8888);
        if (surface == MemorySegment.NULL) {
            return;
        }

        // Use SDL_LockSurface to ensure we have CPU access to the pixel buffer
        if (SDL_LockSurface(surface)) {
            try {
                final MemorySegment pixels = SDL_Surface.pixels(surface);
                final int pitch = SDL_Surface.pitch(surface);
                final int[] sqPixels = cursorData.cursorWords;
                final int[] sqMask = cursorData.maskWords;

                if (cursorData.depth == 32) {
                    /* Case 1: 32-bit ARGB (Direct Copy) */
                    for (int y = 0; y < h; y++) {
                        final long srcOffset = (long) y * w * Integer.BYTES;
                        final long dstOffset = (long) y * pitch;
                        MemorySegment.copy(MemorySegment.ofArray(sqPixels), srcOffset, pixels, dstOffset, (long) w * Integer.BYTES);
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
                                argb = 0x00FFFFFF;      // Invert (0,1)
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
        }

        cursor = SDL_CreateColorCursor(surface, cursorData.offsetX, cursorData.offsetY);
        SDL_DestroySurface(surface);

        if (cursor != MemorySegment.NULL) {
            checkSdlError(SDL_SetCursor(cursor));
        }
        cursorData = null;
    }, Arena.global());

    private SqueakDisplay(final SqueakImageContext image) {
        this.image = image;

        PlatformEventLoop.osEventHandler = this::processEvent;
        PlatformEventLoop.renderFrameIfNeeded = this::performRenderIfNeeded;

        PlatformEventLoop.start();
    }

    private static void checkSdlError(final boolean success) {
        if (!success) {
            throw SqueakException.create("SDL error encountered: " + SDL_GetError().getString(0));
        }
    }

    private static float checkSdlError(final float value) {
        if (value == 0.0f) {
            throw SqueakException.create("SDL error encountered: " + SDL_GetError().getString(0));
        }
        return value;
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        return new SqueakDisplay(image);
    }

    private void ensureStagingPixels(final int w, final int h) {
        final int requiredBytes = w * h * Integer.BYTES;
        if (stagingArena == null || stagingCapacity < requiredBytes) {
            // DO NOT close the old arena. The GC will clean it up automatically.
            stagingArena = Arena.ofAuto();
            stagingBuffer = stagingArena.allocate(requiredBytes);
            stagingCapacity = requiredBytes;
        }
        stagingPitchBytes = w * Integer.BYTES;
    }

    public double getDisplayScale() {
        if (scaleFactor != 0.0f) {
            return scaleFactor;
        } else {
            return 1.0d;
        }
    }

    // Called by the Main Thread at the end of the event loop
    private void performRenderIfNeeded() {
        if (renderer == MemorySegment.NULL) {
            return;
        }

        // Lock the instance to safely read bounds and upload the texture
        synchronized (this) {
            if (!frameRequested) {
                return;
            }

            final int safeTop = Math.max(0, dirtyTop);
            final int safeBottom = Math.min(height, dirtyBottom);
            int safeLeft = Math.max(0, dirtyLeft);
            int safeRight = Math.min(width, dirtyRight);

            resetDamage();
            frameRequested = false;

            if (safeTop >= safeBottom || safeLeft >= safeRight) {
                return;
            }

            // GPU Threshold Blast
            if ((safeRight - safeLeft) >= (width * 3) / 4) {
                safeLeft = 0;
                safeRight = width;
            }

            // Prepare SDL Texture
            if (textureWidth != width || textureHeight != height || texture == MemorySegment.NULL) {
                if (texture != MemorySegment.NULL) {
                    SDL_DestroyTexture(texture);
                }
                textureWidth = width;
                textureHeight = height;
                texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, textureWidth, textureHeight);
                if (texture == null || texture == MemorySegment.NULL) {
                    throw SqueakException.create("Failed to create texture");
                }
                checkSdlError(SDL_SetTextureScaleMode(texture, SDL_SCALEMODE_NEAREST));
            }

            // Upload the texture
            try (Arena arena = Arena.ofConfined()) {
                final MemorySegment dirtyRect = SDL_Rect.allocate(arena);
                SDL_Rect.x(dirtyRect, safeLeft);
                SDL_Rect.y(dirtyRect, safeTop);
                SDL_Rect.w(dirtyRect, safeRight - safeLeft);
                SDL_Rect.h(dirtyRect, safeBottom - safeTop);

                final long pixelStartOffsetBytes = ((long) safeTop * stagingPitchBytes) + ((long) safeLeft * Integer.BYTES);
                final MemorySegment pixelPointer = stagingBuffer.asSlice(pixelStartOffsetBytes);

                if (!SDL_UpdateTexture(texture, dirtyRect, pixelPointer, stagingPitchBytes)) {
                    return; // FIXME: invalid pixels
                }
            }
        }

        checkSdlError(SDL_RenderClear(renderer));
        checkSdlError(SDL_RenderTexture(renderer, texture, MemorySegment.NULL, MemorySegment.NULL));
        checkSdlError(SDL_RenderPresent(renderer));
    }

    public void render(final boolean force) {
        synchronized (this) {
            // Strictly synchronized bounds check
            if (!force && (deferUpdates || dirtyTop >= dirtyBottom || dirtyLeft >= dirtyRight)) {
                return;
            }

            if (frameRequested) {
                return;
            }

            // Claim the frame
            frameRequested = true;
        }

        // Wake the event loop to render the frame
        PlatformEventLoop.wakeUp();
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

            // TODO: on SVM, we should be able to use PinnedObject to get a fixed pointer into the
            // bitmap, which makes the staging pixel redundant.
            // SS: I think this could result in visual tearing since Squeak would be able to change
            // the pixels during the time interval before the pixels are pushed to the screen.
            ensureStagingPixels(currentWidth, currentHeight);
            final int[] sqPixels = bitmap.getIntStorage();

            if (sqPixels.length >= currentWidth * currentHeight) {
                final int rowInts = safeRight - safeLeft;

                // Wrap the Squeak pixel array into a Panama segment
                final MemorySegment srcSegment = MemorySegment.ofArray(sqPixels);

                // Fast Path: one single transfer if more than 75% screen width
                if (rowInts >= (currentWidth * 3) / 4) {
                    final long startOffsetBytes = (long) safeTop * currentWidth * Integer.BYTES;
                    final long totalBytes = (long) (safeBottom - safeTop) * currentWidth * Integer.BYTES;
                    MemorySegment.copy(srcSegment, startOffsetBytes, stagingBuffer, startOffsetBytes, totalBytes);
                } else {
                    // Row-by-Row Path
                    long srcOffsetBytes = ((long) safeTop * currentWidth + safeLeft) * Integer.BYTES;
                    long dstOffsetBytes = ((long) safeTop * stagingPitchBytes) + ((long) safeLeft * Integer.BYTES);
                    final long rowBytes = (long) rowInts * Integer.BYTES;

                    for (int y = safeTop; y < safeBottom; y++) {
                        MemorySegment.copy(srcSegment, srcOffsetBytes, stagingBuffer, dstOffsetBytes, rowBytes);
                        srcOffsetBytes += (long) currentWidth * Integer.BYTES;
                        dstOffsetBytes += stagingPitchBytes;
                    }
                }
            }

            recordDamage(safeLeft, safeTop, safeRight, safeBottom);
        }

        render(false);
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
        synchronized (this) { // Explicit block instead of method modifier
            recordDamage(0, 0, width, height);
        }
    }

    @TruffleBoundary
    public void close() {
        final MemorySegment closeTask = SDL_MainThreadCallback.allocate((_ /* userdata */) -> {

            // Just drop the reference so the GC can safely free the native memory
            stagingArena = null;

            if (texture != MemorySegment.NULL) {
                SDL_DestroyTexture(texture);
                texture = MemorySegment.NULL;
            }
            if (renderer != MemorySegment.NULL) {
                SDL_DestroyRenderer(renderer);
                renderer = MemorySegment.NULL;
            }
            if (cursor != MemorySegment.NULL) {
                SDL_DestroyCursor(cursor);
                cursor = MemorySegment.NULL;
            }
            if (window != MemorySegment.NULL) {
                SDL_DestroyWindow(window);
                window = MemorySegment.NULL;
            }

            // Terminate the event loop.
            PlatformEventLoop.stop();
        }, Arena.global());

        SDL_RunOnMainThread(closeTask, MemorySegment.NULL, true);
    }

    public int getLogicalWindowWidth() {
        return osWindowWidth > 0 ? osWindowWidth : width;
    }

    public int getLogicalWindowHeight() {
        return osWindowHeight > 0 ? osWindowHeight : height;
    }

    public int getWindowWidth() {
        return osWindowWidth > 0 ? (int) Math.ceil(osWindowWidth * getDisplayScale()) : width;
    }

    public int getWindowHeight() {
        return osWindowHeight > 0 ? (int) Math.ceil(osWindowHeight * getDisplayScale()) : height;
    }

    @TruffleBoundary
    public void setFullscreen(final boolean fullscreen) {
        if (window == MemorySegment.NULL) {
            return;
        }
        // Use MemorySegment.ofAddress to pass the flag as a raw pointer value
        SDL_RunOnMainThread(setFullscreenTask, MemorySegment.ofAddress(fullscreen ? 1L : 0L), false);
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

        if (window == MemorySegment.NULL) {
            // When Smalltalk opens the Display the first time, we do not yet know the scaleFactor.
            // We assume that Smalltalk uses the values stored in the image header together with
            // the current scaleFactor (1.0 initially) to create the initial Display. Therefore,
            // we can request an initial window with logical pixel dimensions equal to the bitmap
            // dimensions. After we create the window, we will know the scaleFactor and Smalltalk
            // can use that scaleFactor to resize Display.
            final MemorySegment openTask = SDL_MainThreadCallback.allocate((_ /* userdata */) -> {
                long windowFlags = SDL_WINDOW_RESIZABLE;
                if (image.flags.upscaleDisplayIfHighDPI()) {
                    windowFlags |= SDL_WINDOW_HIGH_PIXEL_DENSITY;
                }

                try (Arena arena = Arena.ofConfined()) {
                    window = SDL_CreateWindow(arena.allocateFrom(title), width, height, windowFlags);
                    if (window == MemorySegment.NULL) {
                        throw SqueakException.create("Failed to create SDL window: " + SDL_GetError().getString(0));
                    }
                }

                renderer = SDL_CreateRenderer(window, MemorySegment.NULL);
                if (renderer == MemorySegment.NULL) {
                    throw SqueakException.create("Failed to create SDL renderer: " + SDL_GetError().getString(0));
                }

                setWindowIcon(window);
                checkSdlError(SDL_RaiseWindow(window));

                // Query the actual display scale factor now that the window exists
                scaleFactor = checkSdlError(SDL_GetWindowDisplayScale(window));

                // Store the logical dimensions exactly as Smalltalk requested them
                osWindowWidth = width;
                osWindowHeight = height;

                checkSdlError(SDL_StartTextInput(window));
                fullDamage();
                if (cursorData != null) {
                    SDL_RunOnMainThread(setCursorTask, MemorySegment.NULL, false);
                }
            }, Arena.global());

            SDL_RunOnMainThread(openTask, MemorySegment.NULL, true);
        } else {
            // On subsequent calls to open() (via DisplayScreen>>beDisplay), we assume that
            // Smalltalk knows the scaleFactor and the dimensions are in physical pixels.
            // Therefore, we request the window to resize to the logical pixel dimensions.
            final int targetLogicalWidth = (int) Math.ceil(width / getDisplayScale());
            final int targetLogicalHeight = (int) Math.ceil(height / getDisplayScale());
            if (targetLogicalWidth != osWindowWidth || targetLogicalHeight != osWindowHeight) {
                osWindowWidth = targetLogicalWidth;
                osWindowHeight = targetLogicalHeight;
                SDL_RunOnMainThread(resizeTask, MemorySegment.NULL, true);
            }
        }

        // Save current logical window size in flags for later writing to disk image
        image.flags.setScreenSize(getLogicalWindowWidth(), getLogicalWindowHeight());

        final String imageFileName = new File(image.getImagePath()).getName();
        setWindowTitle(imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME) ? imageFileName : imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME);
    }

    private static void setWindowIcon(final MemorySegment window) {
        try (InputStream is = SqueakDisplay.class.getResourceAsStream("/trufflesqueak-icon.png")) {
            if (is == null) {
                LogUtils.IO.warning("The icon file /trufflesqueak-icon.png was not found.");
                return;
            }
            final byte[] imageBytes = is.readAllBytes();

            try (Arena arena = Arena.ofConfined()) {
                final MemorySegment imageBuffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, imageBytes);
                final MemorySegment ioStream = SDL_IOFromMem(imageBuffer, imageBytes.length);
                if (ioStream == MemorySegment.NULL) {
                    LogUtils.IO.warning("Failed to create SDL ioStream: " + SDL_GetError().getString(0));
                    return;
                }

                // Load the PNG directly from the IO stream
                final MemorySegment iconSurface = SDL_LoadPNG_IO(ioStream, true);

                if (iconSurface != MemorySegment.NULL) {
                    SDL_SetWindowIcon(window, iconSurface);
                    SDL_DestroySurface(iconSurface);
                } else {
                    LogUtils.IO.warning("Failed to create SDL icon surface: " + SDL_GetError().getString(0));
                }
            }
        } catch (final IOException e) {
            LogUtils.IO.warning("Failed to load and set icon: " + e);
        }
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

        // Save current logical window size in flags for later writing to disk image
        image.flags.setScreenSize(getLogicalWindowWidth(), getLogicalWindowHeight());

        if (window != MemorySegment.NULL) {
            SDL_RunOnMainThread(resizeTask, MemorySegment.NULL, true);
        }

        fullDamage();
    }

    @TruffleBoundary
    public boolean isVisible() {
        return window != MemorySegment.NULL;
    }

    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] maskWords, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
        assert depth == 1 || depth == 32 : "Bad cursor depth: " + depth;
        cursorData = new CursorData(cursorWords, maskWords, width, height, depth, offsetX, offsetY);
        if (window == MemorySegment.NULL) {
            return;
        }
        SDL_RunOnMainThread(setCursorTask, MemorySegment.NULL, false);
    }

    public void processEvent(final MemorySegment event) {
        final int type = SDL_Event.type(event);

        switch (type) {
            case SDL_EVENT_KEY_DOWN:
                processKeyDown(SDL_KeyboardEvent.key(event), SDL_KeyboardEvent.mod(event));
                break;
            case SDL_EVENT_KEY_UP:
                processKeyUp(SDL_KeyboardEvent.key(event), SDL_KeyboardEvent.mod(event));
                break;
            case SDL_EVENT_TEXT_INPUT:
                final MemorySegment textPtr = SDL_TextInputEvent.text(event);
                if (textPtr != MemorySegment.NULL) {
                    processTextInput(textPtr.getString(0));
                }
                break;
            case SDL_EVENT_MOUSE_MOTION:
                processMouseMotion(event, scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
                processMouseButtonDown(event, scaleFactor);
                break;
            case SDL_EVENT_MOUSE_BUTTON_UP:
                processMouseButtonUp(event, scaleFactor);
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
                final int x = (int) (SDL_DropEvent.x(event) * scaleFactor);
                final int y = (int) (SDL_DropEvent.y(event) * scaleFactor);
                addDragEvent(DRAG.MOVE, x, y);
                break;
            }
            case SDL_EVENT_DROP_FILE: {
                // Read the C-string directly from the memory pointer
                final MemorySegment dataPtr = SDL_DropEvent.data(event);
                if (dataPtr != MemorySegment.NULL) {
                    dropFilesAccumulator.add(dataPtr.getString(0));
                }
                break;
            }
            case SDL_EVENT_DROP_COMPLETE: {
                isDragActive = false;
                image.dropPluginFileList = dropFilesAccumulator.toArray(new String[0]);
                final int x = (int) (SDL_DropEvent.x(event) * scaleFactor);
                final int y = (int) (SDL_DropEvent.y(event) * scaleFactor);
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
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
                scaleFactor = checkSdlError(SDL_GetWindowDisplayScale(window));
                addWindowEvent(WINDOW.CHANGED_SCREEN);
                break;
            case SDL_EVENT_WINDOW_EXPOSED:
                addWindowEvent(WINDOW.PAINT);
                fullDamage();
                render(true);
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
                osWindowWidth = SDL_WindowEvent.data1(event);
                osWindowHeight = SDL_WindowEvent.data2(event);
                addWindowEvent(WINDOW.METRIC_CHANGE);
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

        final boolean isCommandOrCtrl = (sdlModifiers & (SDL_KMOD_LCTRL | SDL_KMOD_RCTRL |
                        SDL_KMOD_LGUI | SDL_KMOD_RGUI)) != 0;

        if (isControlKey(sdlKeySym) || isCommandOrCtrl) {
            if (keyChar <= 65535) {
                addKeyboardEvent(KEYBOARD_EVENT.CHAR, keyChar);
            }
        }

        if (isCommandOrCtrl && keyChar == '.') {
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

        final boolean isCommandOrCtrl = (currentModifiers & (KEYBOARD.CTRL | KEYBOARD.CMD)) != 0;

        if (isCommandOrCtrl || text == null || text.isEmpty()) {
            return;
        }

        text.codePoints().forEach(codePoint -> addKeyboardEvent(KEYBOARD_EVENT.CHAR, codePoint));
    }

    private void addKeyboardEvent(final long eventType, final int keyCharOrCode) {
        addEvent(EVENT_TYPE.KEYBOARD, keyCharOrCode, eventType, buttons >> 3, keyCharOrCode);
    }

    private static boolean isModifier(final int sdlKeySym) {
        return switch (sdlKeySym) {
            case SDLK_LSHIFT, SDLK_RSHIFT, SDLK_LCTRL, SDLK_RCTRL, SDLK_LALT, SDLK_RALT, SDLK_LGUI, SDLK_RGUI -> true;
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
        recordMouseEvent(MOUSE_EVENT.MOVE, SDL_MouseMotionEvent.x(event) * scale, SDL_MouseMotionEvent.y(event) * scale, 0);
    }

    public void processMouseButtonDown(final MemorySegment event, final float scale) {
        recordMouseEvent(MOUSE_EVENT.DOWN, SDL_MouseButtonEvent.x(event) * scale, SDL_MouseButtonEvent.y(event) * scale, SDL_MouseButtonEvent.button(event));
    }

    public void processMouseButtonUp(final MemorySegment event, final float scale) {
        recordMouseEvent(MOUSE_EVENT.UP, SDL_MouseButtonEvent.x(event) * scale, SDL_MouseButtonEvent.y(event) * scale, SDL_MouseButtonEvent.button(event));
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

        addEvent(EVENT_TYPE.MOUSE, (int) x, (int) y, buttons & MOUSE.ALL, modifiersForEvent);
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
            // Windows/Linux: Alt hijacked for Cmd, AltGr (MODE) isolated for Opt
            optValue = (sdlModifiers & SDL_KMOD_MODE) != 0 ? KEYBOARD.ALT : 0;
            cmdValue = (sdlModifiers & (SDL_KMOD_LALT | SDL_KMOD_RALT | SDL_KMOD_LGUI | SDL_KMOD_RGUI)) != 0 ? KEYBOARD.CMD : 0;
        }

        final int modifiers = shiftValue + ctrlValue + optValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
    }

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
        if (eventType == EVENT_TYPE.MOUSE) {
            final long[] lastEvent = deferredEvents.pollLast();
            if (lastEvent != null) {
                // Restore the last event if it is not a mouse event with the same button state.
                // If the image's UI thread is keeping up and the queue is empty, no coalescing
                // occurs; however, if mouse events are generated more quickly than the image
                // can consume them (e.g., high-polling mice), this coalescing prevents the image
                // from working through a backlog of stale coordinates and unnecessarily updating
                // the GUI with old mouse locations.
                if (!(lastEvent[0] == EVENT_TYPE.MOUSE && lastEvent[4] == value5)) {
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
        // ToDo: It appears that deferUpdates is never set to true.
        deferUpdates = flag;
    }

    public boolean getDeferUpdates() {
        return deferUpdates;
    }

    @TruffleBoundary
    public void setWindowTitle(final String title) {
        if (window == MemorySegment.NULL) {
            return;
        }
        this.title = title;
        SDL_RunOnMainThread(updateTitleTask, MemorySegment.NULL, false);
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public static String getClipboardData() {
        if (SDL_HasClipboardText()) {
            final MemorySegment textPtr = SDL_GetClipboardText();
            if (textPtr != MemorySegment.NULL) {
                return textPtr.getString(0);
            }
            LogUtils.IO.warning("Failed to get clipboard data");
        }
        return "";
    }

    @TruffleBoundary
    public static void setClipboardData(final String text) {
        try (Arena arena = Arena.ofConfined()) {
            if (!SDL_SetClipboardText(arena.allocateFrom(text))) {
                LogUtils.IO.warning("Failed to set clipboard text");
            }
        }
    }

    public int[] getPrimaryDisplayDimensions() {
        SDL_RunOnMainThread(getPrimaryDisplayDimensionsTask, MemorySegment.NULL, true);
        return primaryDisplayDimensions;
    }
}
