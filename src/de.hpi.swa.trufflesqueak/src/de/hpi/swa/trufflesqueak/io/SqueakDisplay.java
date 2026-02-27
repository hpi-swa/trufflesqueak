/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.WINDOW;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import io.github.humbleui.jwm.App;
import io.github.humbleui.jwm.Clipboard;
import io.github.humbleui.jwm.ClipboardEntry;
import io.github.humbleui.jwm.ClipboardFormat;
import io.github.humbleui.jwm.Event;
import io.github.humbleui.jwm.EventKey;
import io.github.humbleui.jwm.EventMouseButton;
import io.github.humbleui.jwm.EventMouseMove;
import io.github.humbleui.jwm.EventMouseScroll;
import io.github.humbleui.jwm.EventTextInput;
import io.github.humbleui.jwm.EventWindowClose;
import io.github.humbleui.jwm.EventWindowCloseRequest;
import io.github.humbleui.jwm.EventWindowFocusIn;
import io.github.humbleui.jwm.EventWindowFocusOut;
import io.github.humbleui.jwm.EventWindowResize;
import io.github.humbleui.jwm.EventWindowScreenChange;
import io.github.humbleui.jwm.Layer;
import io.github.humbleui.jwm.MouseCursor;
import io.github.humbleui.jwm.Platform;
import io.github.humbleui.jwm.Window;
import io.github.humbleui.jwm.skija.EventFrameSkija;
import io.github.humbleui.jwm.skija.LayerD3D12Skija;
import io.github.humbleui.jwm.skija.LayerGLSkija;
import io.github.humbleui.jwm.skija.LayerMetalSkija;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

public final class SqueakDisplay implements Consumer<Event> {
    private static final String DEFAULT_WINDOW_TITLE = "TruffleSqueak";

    public final SqueakImageContext image;
    private Window window;
    private volatile boolean hasWindow = false;
    private Layer layer;

    // Input handlers
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    // Graphics
    private Bitmap squeakBitmap;
    private int[] squeakBitmapPixels;
    private IntBuffer pixelIntBuffer; // Direct buffer mapped to Skija's native memory
    private Surface gpuSurface;       // Permanent Offscreen VRAM texture
    private int dirtyTop = Integer.MAX_VALUE;
    private int dirtyBottom = -1;
    private boolean deferUpdates;
    private boolean frameRequested = false;

    // Cached window information (avoids calls on UI thread)
    private int windowWidth = 0;
    private int windowHeight = 0;
    private double windowScaleFactor = 1.0d;

    // Event Queue
    private final java.util.concurrent.ConcurrentLinkedDeque<long[]> deferredEvents = new java.util.concurrent.ConcurrentLinkedDeque<>();
    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;

    private SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        this.mouse = new SqueakMouse(this);
        this.keyboard = new SqueakKeyboard(this);

        // JWM must run on UI thread
        App.runOnUIThread(() -> {
            window = App.makeWindow();
            hasWindow = true;
            window.setEventListener(this);
            window.setTitle(DEFAULT_WINDOW_TITLE);
            window.setWindowSize(image.flags.getSnapshotScreenWidth(), image.flags.getSnapshotScreenHeight());
            layer = switch (Platform.CURRENT) {
                case MACOS -> new LayerMetalSkija();
                case WINDOWS -> new LayerD3D12Skija();
                case X11 -> new LayerGLSkija();
            };
            window.setLayer(layer);
            window.setVisible(true);
            window.bringToFront();
            tryToSetTaskbarIcon();
            cacheWindowInfo();
        });
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        return new SqueakDisplay(image);
    }

    private void tryToSetTaskbarIcon() {
        if (window == null) {
            return;
        }

        try {
            final String iconExt = switch (Platform.CURRENT) {
                case MACOS -> ".icns";
                case WINDOWS -> ".ico";
                case X11 -> ".png";
            };

            final String resourcePath = "/trufflesqueak-icon" + iconExt;
            final java.net.URL resource = SqueakDisplay.class.getResource(resourcePath);

            if (resource != null) {
                final File tempIcon = File.createTempFile("trufflesqueak-icon", iconExt);
                tempIcon.deleteOnExit(); // Clean up when Squeak closes

                try (InputStream is = resource.openStream()) {
                    Files.copy(is, tempIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                window.setIcon(tempIcon);
            }
        } catch (Exception e) {
            LogUtils.IO.warning(e.toString());
        }
    }

    private void cacheWindowInfo() {
        // Called from the UI thread
        if (window != null) {
            windowWidth = window.getWindowRect().getWidth();
            windowHeight = window.getWindowRect().getHeight();
            windowScaleFactor = window.getScreen().getScale();
        } else {
            windowWidth = 0;
            windowHeight = 0;
            windowScaleFactor = 1.0d;
        }
    }

    @Override
    public void accept(final Event e) {
        switch (e) {
            case EventFrameSkija f -> paint(f.getSurface());

            case EventWindowClose c -> addWindowEvent(WINDOW.CLOSE);
            case EventWindowCloseRequest cr -> addWindowEvent(WINDOW.CLOSE);

            case EventWindowResize r -> {
                cacheWindowInfo();
                addWindowEvent(WINDOW.METRIC_CHANGE);
            }
            case EventWindowScreenChange sc -> {
                // Reconfigure the GPU swap chain for the new monitor
                if (layer != null) {
                    layer.reconfigure();
                }
                cacheWindowInfo();
                addWindowEvent(WINDOW.METRIC_CHANGE);
            }

            case EventWindowFocusIn fi -> addWindowEvent(WINDOW.ACTIVATED);
            case EventWindowFocusOut fo -> addWindowEvent(WINDOW.DEACTIVATED);

            case EventMouseMove m -> mouse.onMove(m);
            case EventMouseButton b -> mouse.onButton(b);
            case EventMouseScroll s -> mouse.onScroll(s);

            case EventKey k -> keyboard.onKey(k);
            case EventTextInput ti -> keyboard.onTextInput(ti);

            default -> {
                // Ignore any other JWM events we don't care about
            }
        }
    }

    private void paint(final Surface swapchainSurface) {
        if (window == null || swapchainSurface == null || squeakBitmap == null) {
            return;
        }

        synchronized (this) {
            frameRequested = false;

            final int width = squeakBitmap.getWidth();
            final int height = squeakBitmap.getHeight();

            // Lazily allocate the GPU surface
            if (gpuSurface == null) {
                final ImageInfo info = new ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL);

                // Grab the context directly from the active swapchain surface
                if (swapchainSurface._context != null) {
                    // Create a permanent hardware-accelerated VRAM texture
                    gpuSurface = Surface.makeRenderTarget(swapchainSurface._context, false, info);
                }

                if (gpuSurface == null) {
                    // Fallback to CPU raster if hardware rendering is unavailable
                    gpuSurface = Surface.makeRaster(info);
                }
            }

            // Upload ONLY the dirty band to the GPU
            if (dirtyTop <= dirtyBottom) {
                try (Image skiaImage = Image.makeRasterFromBitmap(squeakBitmap)) {
                    final Rect dirtyRect = Rect.makeLTRB(0, dirtyTop, width, dirtyBottom + 1);
                    gpuSurface.getCanvas().drawImageRect(skiaImage, dirtyRect, dirtyRect, null);
                }

                // Reset the dirty tracker for the next frame
                dirtyTop = Integer.MAX_VALUE;
                dirtyBottom = -1;
            }

            // Stamp the complete, tear-free GPU texture onto the rotating JWM swapchain
            try (Image gpuSnapshot = gpuSurface.makeImageSnapshot()) {
                swapchainSurface.getCanvas().drawImage(gpuSnapshot, 0, 0);
            }
        }
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        if (squeakBitmap != null) {
            boolean shouldRequestFrame = false;

            // Clip vertical bounds to prevent array out-of-bounds exceptions
            final int width = squeakBitmap.getWidth();
            final int height = squeakBitmap.getHeight();
            final int clippedTop = Math.max(0, top);
            final int clippedBottom = Math.min(height - 1, bottom);

            if (clippedTop <= clippedBottom) {
                synchronized (this) {
                    if (pixelIntBuffer != null && squeakBitmapPixels != null) {
                        // Fast block-transfer exactly one slice of memory
                        final int offset = clippedTop * width;
                        final int length = (clippedBottom - clippedTop + 1) * width;

                        pixelIntBuffer.position(offset);
                        pixelIntBuffer.put(squeakBitmapPixels, offset, length);

                        // Accumulate the dirty band for the GPU thread
                        dirtyTop = Math.min(dirtyTop, clippedTop);
                        dirtyBottom = Math.max(dirtyBottom, clippedBottom);
                    }

                    // Check and set the lock atomically
                    if (!frameRequested) {
                        frameRequested = true;
                        shouldRequestFrame = true;
                    }
                }
            }

            // Queue the frame outside the sync block to avoid stalling the Squeak thread
            if (shouldRequestFrame) {
                App.runOnUIThread(() -> {
                    if (window != null) {
                        window.requestFrame();
                    }
                });
            }
        }
    }

    /**
     * Called by Squeak when the display bitmap changes (e.g. resize or startup).
     */
    public void setSqueakDisplay(final PointersObject squeakDisplay) {
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final NativeObject bitmap = readNode.executeNative(squeakDisplay, FORM.BITS);
        if (!bitmap.isIntType()) {
            throw SqueakException.create("Display bitmap expected to be a words object");
        }
        final int width = readNode.executeInt(squeakDisplay, FORM.WIDTH);
        final int height = readNode.executeInt(squeakDisplay, FORM.HEIGHT);
        assert (long) squeakDisplay.instVarAt0Slow(FORM.DEPTH) == 32 : "Unsupported display depth";

        if (width > 0 && height > 0) {
            synchronized (this) {
                squeakBitmapPixels = bitmap.getIntStorage();

                // Clean up the old bitmap and GPU surface if resizing
                if (squeakBitmap != null) {
                    squeakBitmap.close();
                }
                if (gpuSurface != null) {
                    gpuSurface.close();
                    gpuSurface = null; // Will be lazily recreated on the UI thread
                }

                // Initialize the new Skija Bitmap
                squeakBitmap = new Bitmap();
                final ImageInfo info = new ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL);
                squeakBitmap.allocPixels(info);

                // Extract the Pixmap view, then grab its direct native memory buffer!
                try (Pixmap pixmap = squeakBitmap.peekPixels()) {
                    if (pixmap != null) {
                        final ByteBuffer byteBuffer = pixmap.getBuffer();
                        if (byteBuffer != null) {
                            pixelIntBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
                        }
                    }
                }

                // Mark the entire screen as dirty for the initial frame
                dirtyTop = 0;
                dirtyBottom = height - 1;

                // Tell the OS we are ready to draw the very first frame
                if (!frameRequested) {
                    frameRequested = true;
                    App.runOnUIThread(() -> {
                        if (window != null) {
                            window.requestFrame();
                        }
                    });
                }
            }
        }
    }

    @TruffleBoundary
    public void close() {
        hasWindow = false;
        App.runOnUIThread(() -> {
            if (window != null) {
                window.close();
                window = null;
                cacheWindowInfo();
            }
        });
    }

    @TruffleBoundary
    public void resizeTo(final int width, final int height) {
        windowWidth = width;
        windowHeight = height;
        App.runOnUIThread(() -> {
            if (window != null) {
                window.setWindowSize(windowWidth, windowHeight);
            }
        });
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public double getWindowScaleFactor() {
        return windowScaleFactor;
    }

    @TruffleBoundary
    public void setFullscreen(final boolean enable) {
        App.runOnUIThread(() -> {
            if (window == null) {
                return;
            }

            if (enable) {
                // Tells the native OS to maximize the window
                window.maximize();
            } else {
                // Tells the native OS to snap the window back to its previous dimensions and
                // position
                window.restore();
            }
        });
    }

    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        setSqueakDisplay(sqDisplay);
        final String imageFileName = new File(image.getImagePath()).getName();
        final String title;
        if (imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME)) {
            title = imageFileName;
        } else {
            title = imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
        }
        setWindowTitle(title);
    }

    @TruffleBoundary
    public boolean isVisible() {
        return hasWindow;
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public void setCursor(final int[] cursorWords, final int[] mask, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
        // ToDo: Do this right!
        MouseCursor jwmCursor = MouseCursor.ARROW;

        if (cursorWords != null && cursorWords.length > 0) {
            // Generate a unique footprint for this specific Smalltalk cursor
            final int hash = java.util.Arrays.hashCode(cursorWords);

            // Uncomment this line temporarily to discover the hashes of Squeak cursors
            // Systemx.out.println("Cursor Hash: " + hash);

            switch (hash) {
                // TODO: Replace these hashes with the actual hashes printed to console
                case 1447681537:
                    jwmCursor = MouseCursor.IBEAM;
                    break;
                case -594223615:
                    jwmCursor = MouseCursor.POINTING_HAND;
                    break;

                case 111111111:
                    jwmCursor = MouseCursor.CROSSHAIR;
                    break;
                case 222222222:
                    jwmCursor = MouseCursor.WAIT; // The Hourglass/Spinner
                    break;

                case 246013441:
                    jwmCursor = MouseCursor.RESIZE_NS;
                    break;
                case 576642561:
                    jwmCursor = MouseCursor.RESIZE_WE;
                    break;
                case -1628447231:
                    jwmCursor = MouseCursor.RESIZE_NESW;
                    break;
                case 149937665:
                    jwmCursor = MouseCursor.RESIZE_NWSE;
                    break;
                default:
                    // If it's a completely custom Smalltalk cursor (like a paintbrush),
                    // we gracefully degrade back to the standard OS arrow.
                    jwmCursor = MouseCursor.ARROW;
                    break;
            }
        }

        final MouseCursor finalCursor = jwmCursor;
        App.runOnUIThread(() -> {
            if (window != null) {
                window.setMouseCursor(finalCursor);
            }
        });
    }

    public long[] getNextEvent() {
        return deferredEvents.pollFirst();
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        addEvent(eventType, value3, value4, value5, value6, 0L);
    }

    public void addDragEvent(final long type, final int x, final int y) {
        // ToDo: Need to add drag & drop somehow -- JWM doesn't supply one
        addEvent(EVENT_TYPE.DRAG_DROP_FILES, type, x, y, buttons >> 3, image.dropPluginFileList.length);
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
        App.runOnUIThread(() -> {
            if (window != null) {
                window.setTitle(title);
            }
        });
    }

    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @TruffleBoundary
    public static String getClipboardData() {
        final java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();

        App.runOnUIThread(() -> {
            try {
                final ClipboardEntry entry = Clipboard.get(ClipboardFormat.TEXT);
                future.complete(entry == null ? "" : entry.getString());
            } catch (Exception e) {
                // Clipboard might be locked by another OS process
                future.complete("");
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            return "";
        }
    }

    @TruffleBoundary
    public static void setClipboardData(final String text) {
        App.runOnUIThread(() -> {
            try {
                Clipboard.set(ClipboardEntry.makeString(ClipboardFormat.TEXT, text));
            } catch (Exception e) {
                LogUtils.IO.warning("JWM failed to set OS clipboard: " + e.getMessage());
            }
        });
    }

    @TruffleBoundary
    public static void beep() {
        // JWM doesn't have beep. Java Toolkit might still work for simple beep if available,
        // otherwise ignore or implement platform specific.
        // ToDo: is there anything to be done with this?
        // java.awt.Toolkit.getDefaultToolkit().beep(); // Fallback if java.desktop is present?
        // If java.desktop is GONE, this will fail. We should probably do nothing.
    }
}
