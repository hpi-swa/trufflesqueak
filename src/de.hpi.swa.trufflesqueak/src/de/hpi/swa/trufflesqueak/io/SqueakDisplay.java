/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT;
import static org.lwjgl.sdl.SDLEvents.SDL_PollEvent;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK;
import static org.lwjgl.sdl.SDLHints.SDL_HINT_RENDER_VSYNC;
import static org.lwjgl.sdl.SDLHints.SDL_SetHint;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ARGB8888;
import static org.lwjgl.sdl.SDLProperties.SDL_CreateProperties;
import static org.lwjgl.sdl.SDLProperties.SDL_DestroyProperties;
import static org.lwjgl.sdl.SDLProperties.SDL_SetBooleanProperty;
import static org.lwjgl.sdl.SDLProperties.SDL_SetNumberProperty;
import static org.lwjgl.sdl.SDLProperties.SDL_SetStringProperty;
import static org.lwjgl.sdl.SDLRect.SDL_GetRectUnionFloat;
import static org.lwjgl.sdl.SDLRender.SDL_CreateRenderer;
import static org.lwjgl.sdl.SDLRender.SDL_CreateSoftwareRenderer;
import static org.lwjgl.sdl.SDLRender.SDL_CreateTexture;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyRenderer;
import static org.lwjgl.sdl.SDLRender.SDL_DestroyTexture;
import static org.lwjgl.sdl.SDLRender.SDL_RenderClear;
import static org.lwjgl.sdl.SDLRender.SDL_RenderPresent;
import static org.lwjgl.sdl.SDLRender.SDL_RenderTexture;
import static org.lwjgl.sdl.SDLRender.SDL_TEXTUREACCESS_STREAMING;
import static org.lwjgl.sdl.SDLRender.SDL_UpdateTexture;
import static org.lwjgl.sdl.SDLStdinc.SDL_SetMemoryFunctions;
import static org.lwjgl.sdl.SDLVideo.SDL_CreateWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_CreateWindowWithProperties;
import static org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_SetSwapInterval;
import static org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale;
import static org.lwjgl.sdl.SDLVideo.SDL_GetWindowSurface;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_RESIZABLE_BOOLEAN;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_TITLE_STRING;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_X_NUMBER;
import static org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_CREATE_Y_NUMBER;
import static org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowMinimumSize;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle;
import static org.lwjgl.sdl.SDLVideo.SDL_ShowWindow;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOWPOS_CENTERED;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE;

import java.awt.*;
import java.awt.Taskbar.Feature;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;

import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_FRect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDL_Texture;
import org.lwjgl.system.MemoryUtil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.DRAG;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class SqueakDisplay {
    private static final String DEFAULT_WINDOW_TITLE = "TruffleSqueak";
    private static final long NULL = 0L;
    @CompilationFinal(dimensions = 1) private static final int[] CURSOR_COLORS = {0x00000000, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000};

    public final SqueakImageContext image;

    // public for the Java-based UI for TruffleSqueak.
    // public final Frame frame = new Frame(DEFAULT_WINDOW_TITLE);
    private long window = NULL;
    private SDL_Surface surface;
    private long renderer = NULL;
    private SDL_Texture texture;
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    private int width;
    private int height;
    private float scaleFactor;
    private boolean textureDirty = false;
    private NativeObject bitmap;
    private int bpp = 4; // TODO: for 32bit only!
    private SDL_FRect flipRect = SDL_FRect.create();
    private SDL_FRect renderRect = SDL_FRect.create();
    private SDL_Event event = SDL_Event.create();

    private final SqueakDisplayCanvas canvas = new SqueakDisplayCanvas();
    private final ArrayDeque<long[]> deferredEvents = new ArrayDeque<>();

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private Dimension rememberedWindowSize;
    private Point rememberedWindowLocation;
    private boolean deferUpdates;

    static {
        SDL_SetMemoryFunctions(
                        MemoryUtil::nmemAllocChecked,
                        MemoryUtil::nmemCallocChecked,
                        MemoryUtil::nmemReallocChecked,
                        MemoryUtil::nmemFree);
    }

    private SqueakDisplay(final SqueakImageContext image) {
// assert EventQueue.isDispatchThread();
        this.image = image;
// frame.add(canvas);
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);
// frame.setFocusTraversalKeysEnabled(false); // Ensure `Tab` key is captured.
// frame.setMinimumSize(new Dimension(200, 150));
// frame.setResizable(true);
//
// // Install event listeners
// canvas.addMouseListener(mouse);
// canvas.addMouseMotionListener(mouse);
// canvas.addMouseWheelListener(mouse);
// frame.addKeyListener(keyboard);
// installWindowAdapter();
// installDropTargetListener();

        if (!SDL_Init(SDL_INIT_VIDEO)) {
            throw new IllegalStateException("Unable to initialize SDL: " + SDL_GetError());
        }

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
// SDL.eventState(SDL.EventType.TEXTEDITING.getCValue(), SDL.ignore());
// SDL.eventState(SDL.EventType.FINGERDOWN.getCValue(), SDL.ignore());
// SDL.eventState(SDL.EventType.FINGERUP.getCValue(), SDL.ignore());
// SDL.eventState(SDL.EventType.FINGERMOTION.getCValue(), SDL.ignore());

        System.out.println("display created");

        // tryToSetTaskbarIcon();
    }

    private static void checkSdlError(boolean success) {
        if (!success) {
            throw new IllegalStateException("SDL error encountered: " + SDL_GetError());
        }
    }

    private static long checkSdlError(long resultPointer) {
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
        if (Taskbar.isTaskbarSupported()) {
            try {
                final Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(Toolkit.getDefaultToolkit().getImage(SqueakDisplay.class.getResource("/trufflesqueak-icon.png")));
                }
            } catch (Exception e) {
                LogUtils.IO.warning(e.toString());
            }
        }
    }

    public double getDisplayScale() {
        if (scaleFactor != NULL) {
            return scaleFactor;
        } else {
            return 1.0d;
        }
    }

    private static final class SqueakDisplayCanvas extends Component {
        @Serial private static final long serialVersionUID = 1L;
        private transient BufferedImage bufferedImage;

        @Override
        public boolean isOpaque() {
            return true;
        }

        /**
         * Override paint in case a repaint event is triggered (e.g. when window is moved to another
         * screen).
         */
        @Override
        public void paint(final Graphics g) {
            g.drawImage(bufferedImage, 0, 0, null);
        }

        /**
         * Paint directly onto graphics. Smalltalk manages repaints and thus, Swing's repaint
         * manager needs to be bypassed to avoid flickering (repaints otherwise have a slight
         * delay).
         */
        public void paintImmediately(final int left, final int top, final int right, final int bottom) {
            final Graphics g = getGraphics();
            if (g != null) {
                g.drawImage(bufferedImage, left, top, right, bottom, left, top, right, bottom, null);
                g.dispose();
            }
        }

        private void setSqueakDisplay(final PointersObject squeakDisplay) {
            final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
            final NativeObject bitmap = readNode.executeNative(squeakDisplay, FORM.BITS);
            if (!bitmap.isIntType()) {
                throw SqueakException.create("Display bitmap expected to be a words object");
            }
            final int width = readNode.executeInt(squeakDisplay, FORM.WIDTH);
            final int height = readNode.executeInt(squeakDisplay, FORM.HEIGHT);
            assert (long) squeakDisplay.instVarAt0Slow(FORM.DEPTH) == 32 : "Unsupported display depth";
            if (width > 0 && height > 0) {
                bufferedImage = MiscUtils.new32BitBufferedImage(bitmap.getIntStorage(), width, height, false);
            }
        }
    }

    @TruffleBoundary
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        assert left <= right && top <= bottom;
        paintImmediately(left, top, right, bottom);
    }

    private void paintImmediately(final int left, final int top, final int right, final int bottom) {
        copyPixels(left + top * width, right + bottom * width);
        recordDamage(left, top, right - left, bottom - top);
        textureDirty = true;
        render(true);
    }

    private void copyPixels(final int start, final int stop) {
        final int offset = start * bpp;
        assert offset >= 0;
        final int remainingSize = width * height * bpp - offset;
        if (remainingSize <= 0 || start >= stop) {
            LogUtils.IO.fine(() -> "remainingSize <= 0" + (remainingSize <= 0) + "start >= stop" + (start >= stop));
// return;
        }
        final int[] pixelInts = bitmap.getIntStorage();
        ByteBuffer pixels = MemoryUtil.memAlloc(pixelInts.length * Integer.BYTES);
        pixels.asIntBuffer().put(pixelInts);
        pixels.clear();
// surface.pixels(pixels);

        System.out.print(".");
        System.out.flush();

        SDL_UpdateTexture(texture, null, pixels, width * bpp);
    }

    private void recordDamage(final int x, final int y, final int w, final int h) {
        flipRect.x(x * 2.0f);
        flipRect.y(y * 2.0f);
        flipRect.w(Math.min(w + 1, width) * 2.0f);
        flipRect.h(Math.min(h + 1, height) * 2.0f);
// System.out.println("flipRect: " + flipRect.w() + " " + flipRect.h() + " " + flipRect.x() + " " +
// flipRect.y());
        SDL_GetRectUnionFloat(flipRect, renderRect, renderRect);
    }

    private void render(final boolean forced) {
// if (!forced && (deferUpdates || !textureDirty)) {
// return;
// }
        textureDirty = false;

        // Clear the renderer
        SDL_RenderClear(renderer);

        // Copy the texture to the rendering target (replaces SDL2's SDL_RenderCopy)
// System.out.println("RenderRect: " + renderRect.w() + " " + renderRect.h() + " " + renderRect.x()
// + " " + renderRect.y());
        SDL_RenderTexture(renderer, texture, null, null);

        // Present the updated renderer to the screen
        SDL_RenderPresent(renderer);

        System.out.print("x");
        System.out.flush();

// if (!SDL_RenderTexture(renderer, texture, renderRect, renderRect)) {
//// return;
// }
//
// SDL_RenderPresent(renderer);
// SDL_UpdateWindowSurface(window);
        resetDamage();
    }

    private void resetDamage() {
        renderRect.x(0);
        renderRect.y(0);
        renderRect.w(0);
        renderRect.h(0);
    }

    private void fullDamage() {
        renderRect.x(0);
        renderRect.y(0);
        renderRect.w(width);
        renderRect.h(height);
    }

    @TruffleBoundary
    public void close() {
        if (texture != null) {
            SDL_DestroyTexture(texture);
        }
        if (renderer != NULL) {
            SDL_DestroyRenderer(renderer);
        }
        if (window != NULL) {
            SDL_DestroyWindow(window);
        }
        SDL_Quit();
    }

    @TruffleBoundary
    public void resizeTo(final int width, final int height) {
    }

    public int getWindowWidth() {
        return width;
    }

    public int getWindowHeight() {
        return height;
    }

    @TruffleBoundary
    public void setFullscreen(final boolean enable) {
// EventQueue.invokeLater(() -> {
// if (enable) {
// rememberedWindowLocation = frame.getLocationOnScreen();
// rememberedWindowSize = frame.getSize();
// }
// frame.dispose();
// frame.setUndecorated(enable);
// if (enable) {
// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
// frame.setResizable(false);
// } else {
// frame.setExtendedState(Frame.NORMAL);
// canvas.setPreferredSize(rememberedWindowSize);
// frame.pack();
// frame.setResizable(true);
// }
// frame.pack();
// if (!enable) {
// if (rememberedWindowLocation != null) {
// frame.setLocation(rememberedWindowLocation);
// }
// rememberedWindowLocation = null;
// rememberedWindowSize = null;
// }
// frame.setVisible(true);
// });
    }

    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        bitmap = (NativeObject) sqDisplay.instVarAt0Slow(FORM.BITS);
        if (!bitmap.isIntType()) {
            throw SqueakException.create("Display bitmap expected to be a words object");
        }
        // canvas.setSqueakDisplay(sqDisplay);
        // Set or update frame title.
        final String imageFileName = new File(image.getImagePath()).getName();
        // Avoid name duplication in frame title.
        final String title;
        if (imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME)) {
            title = imageFileName;
        } else {
            title = imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
        }
        System.out.println("opening " + window);
        if (window == NULL) {
            width = (int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH);
            height = (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT);
            System.out.println("opening " + title);
            if (true) {
                init();
                fullDamage();
                return;
            }
            int props = SDL_CreateProperties();
            checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_X_NUMBER,
                            SDL_WINDOWPOS_CENTERED));
            checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_Y_NUMBER,
                            SDL_WINDOWPOS_CENTERED));
            checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER, width));
            checkSdlError(SDL_SetNumberProperty(props, SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER, height));
            checkSdlError(SDL_SetStringProperty(props, SDL_PROP_WINDOW_CREATE_TITLE_STRING, title));
            // checkSdlError(SDL_SetBooleanProperty(props, SDL_PROP_WINDOW_CREATE_OPENGL_BOOLEAN,
            // true));
            checkSdlError(SDL_SetBooleanProperty(props, SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN, true));
            checkSdlError(SDL_SetBooleanProperty(props, SDL_PROP_WINDOW_CREATE_RESIZABLE_BOOLEAN, true));

            window = checkSdlError(SDL_CreateWindowWithProperties(props));
            SDL_DestroyProperties(props);

            checkSdlError(SDL_SetWindowMinimumSize(window, width, height));

            // glContext = checkSdlError(SDL_GL_CreateContext(window));

// SDL_GL_MakeCurrent(window, glContext);

// Configuration.OPENGL_EXPLICIT_INIT.set(true);

// checkSdlError(SDL_GL_LoadLibrary((ByteBuffer) null));

// try (MemoryStack stack = stackPush()) {
// PointerBuffer windowPtr = stack.mallocPointer(1);
// PointerBuffer rendererPtr = stack.mallocPointer(1);
// checkSdlError(SDL_CreateWindowAndRenderer(title, width, height, 0, windowPtr, rendererPtr));
// window = windowPtr.get(0);
// renderer = rendererPtr.get(0);
// }

            SDL_GL_SetSwapInterval(0); // disable vsync

            checkSdlError(SDL_ShowWindow(window));
            checkSdlError(SDL_RaiseWindow(window));

            surface = SDL_GetWindowSurface(window);
            renderer = checkSdlError(SDL_CreateSoftwareRenderer(surface));
            texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING, width, height);
            System.out.println("texture created: " + texture);
        }
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

        // Create the hardware-accelerated renderer
        renderer = SDL_CreateRenderer(window, "metal");
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
    }

    @TruffleBoundary
    public boolean isVisible() {
        return window != NULL;
    }

    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] mask, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
// final Dimension bestCursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
// final Cursor cursor;
// if (bestCursorSize.width == 0 || bestCursorSize.height == 0) {
// cursor = Cursor.getDefaultCursor();
// } else {
// // TODO: Ensure the below works correctly for all cursor and maybe refactor senders.
// final int[] ints;
// if (mask != null) {
// ints = mergeCursorWithMask(cursorWords, mask);
// } else {
// ints = cursorWords;
// }
// final BufferedImage bufferedImage;
// if (depth == 32) {
// bufferedImage = MiscUtils.new32BitBufferedImage(cursorWords, width, height, true);
// } else {
// bufferedImage = new BufferedImage(bestCursorSize.width, bestCursorSize.height,
// BufferedImage.TYPE_INT_ARGB);
// for (int y = 0; y < height; y++) {
// final int word = ints[y];
// for (int x = 0; x < width; x++) {
// final int colorIndex = word >> (width - 1 - x) * 2 & 3;
// bufferedImage.setRGB(x, y, CURSOR_COLORS[colorIndex]);
// }
// }
// }
// // Ensure hotspot is within cursor bounds.
// final Point hotSpot = new Point(Math.min(Math.max(offsetX, 1), width - 1),
// Math.min(Math.max(offsetY, 1), height - 1));
// cursor = Toolkit.getDefaultToolkit().createCustomCursor(bufferedImage, hotSpot, "TruffleSqueak
// Cursor");
// }
// SDL_Cursor
// EventQueue.invokeLater(() -> frame.setCursor(cursor));
    }

    private static int[] mergeCursorWithMask(final int[] cursorWords, final int[] maskWords) {
        final int[] cursorMergedWords = new int[SqueakIOConstants.CURSOR_HEIGHT];
        for (int y = 0; y < SqueakIOConstants.CURSOR_HEIGHT; y++) {
            final int cursorWord = cursorWords[y];
            final int maskWord = maskWords[y];
            int bit = 0x80000000;
            int merged = 0;
            for (int x = 0; x < SqueakIOConstants.CURSOR_WIDTH; x++) {
                merged = merged | (maskWord & bit) >> x | (cursorWord & bit) >> x + 1;
                bit = bit >>> 1;
            }
            cursorMergedWords[y] = merged;
        }
        return cursorMergedWords;
    }

    public long[] getNextEvent() {
        while (SDL_PollEvent(event)) {
            final long time = getEventTime();
            final int eventType = event.type();
            System.out.println(event);
            if (event.type() == SDL_EVENT_QUIT) {
                close();
            }
        }
        return deferredEvents.pollFirst();
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        addEvent(eventType, value3, value4, value5, value6, 0L);
    }

    private void addDragEvent(final long type, final Point location) {
        addEvent(EVENT_TYPE.DRAG_DROP_FILES, type, (long) location.getX(), (long) location.getY(), buttons >> 3, image.dropPluginFileList.length);
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

    public int recordModifiers(final InputEvent e) {
        final int shiftValue = e.isShiftDown() ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = e.isControlDown() ? KEYBOARD.CTRL : 0;
        final int optValue = e.isAltGraphDown() ? KEYBOARD.ALT : 0;
        final int cmdValue = e.isAltDown() || e.isMetaDown() ? KEYBOARD.CMD : 0;
        final int modifiers = shiftValue + ctrlValue + optValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
        return modifiers;
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
        SDL_SetWindowTitle(window, title);
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public static String getClipboardData() {
        final Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (final UnsupportedFlavorException | IOException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
        return "";
    }

    @TruffleBoundary
    public static void setClipboardData(final String text) {
        final StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    @TruffleBoundary
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    private void installWindowAdapter() {
        assert EventQueue.isDispatchThread();
// frame.addWindowListener(new WindowAdapter() {
// @Override
// public void windowActivated(final WindowEvent e) {
// addWindowEvent(WINDOW.ACTIVATED);
// }
//
// @Override
// public void windowClosing(final WindowEvent e) {
// addWindowEvent(WINDOW.CLOSE);
// }
//
// @Override
// public void windowDeactivated(final WindowEvent e) {
// addWindowEvent(WINDOW.DEACTIVATED);
// }
//
// @Override
// public void windowIconified(final WindowEvent e) {
// addWindowEvent(WINDOW.ICONISE);
// }
//
// @Override
// public void windowStateChanged(final WindowEvent e) {
// addWindowEvent(WINDOW.METRIC_CHANGE);
// }
// });
    }

    @SuppressWarnings("unused")
    private void installDropTargetListener() {
        assert EventQueue.isDispatchThread();
        new DropTarget(canvas, new DropTargetAdapter() {
            @Override
            public void drop(final DropTargetDropEvent dtde) {
                final Transferable transferable = dtde.getTransferable();
                for (final DataFlavor flavor : transferable.getTransferDataFlavors()) {
                    if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        try {
                            @SuppressWarnings("unchecked")
                            final List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                            final String[] fileArray = new String[fileList.size()];
                            int i = 0;
                            for (final File file : fileList) {
                                fileArray[i++] = file.getCanonicalPath();
                            }
                            image.dropPluginFileList = fileArray;
                            addDragEvent(DRAG.DROP, dtde.getLocation());
                            dtde.getDropTargetContext().dropComplete(true);
                            return;
                        } catch (final IOException | UnsupportedFlavorException e) {
                            LogUtils.IO.warning(e.toString());
                        }
                    }
                }
                image.dropPluginFileList = ArrayUtils.EMPTY_STRINGS_ARRAY;
                addDragEvent(DRAG.DROP, dtde.getLocation());
                dtde.rejectDrop();
            }

            @Override
            public void dragOver(final DropTargetDragEvent dtde) {
                addDragEvent(DRAG.MOVE, dtde.getLocation());

            }

            @Override
            public void dragExit(final DropTargetEvent dte) {
                addDragEvent(DRAG.LEAVE, new Point(0, 0));

            }

            @Override
            public void dragEnter(final DropTargetDragEvent dtde) {
                addDragEvent(DRAG.ENTER, dtde.getLocation());

            }
        });
    }
}
