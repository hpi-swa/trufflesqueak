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
import java.util.ArrayDeque;
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
import io.github.humbleui.jwm.Layer;
import io.github.humbleui.jwm.MouseCursor;
import io.github.humbleui.jwm.Platform;
import io.github.humbleui.jwm.Window;
import io.github.humbleui.jwm.skija.EventFrameSkija;
import io.github.humbleui.jwm.skija.LayerD3D12Skija;
import io.github.humbleui.jwm.skija.LayerGLSkija;
import io.github.humbleui.jwm.skija.LayerMetalSkija;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.IRect;

public final class SqueakDisplay implements Consumer<Event> {
    private static final String DEFAULT_WINDOW_TITLE = "TruffleSqueak";

    public final SqueakImageContext image;
    public Window window;
    public Layer layer;

    // Input handlers
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    // Graphics
    private Bitmap squeakBitmap;
    private int[] squeakBitmapPixels;
    private IntBuffer pixelIntBuffer; // Direct buffer mapped to Skija's native memory
    private boolean deferUpdates;
    private boolean frameRequested = false;

    // Event Queue
    private final ArrayDeque<long[]> deferredEvents = new ArrayDeque<>();
    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;

    // Window state tracking
    private int rememberedWindowWidth;
    private int rememberedWindowHeight;
    private int rememberedWindowX;
    private int rememberedWindowY;

    private SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        this.mouse = new SqueakMouse(this);
        this.keyboard = new SqueakKeyboard(this);

        // JWM must run on UI thread
        App.runOnUIThread(() -> {
            window = App.makeWindow();
            window.setEventListener(this);
            window.setTitle(DEFAULT_WINDOW_TITLE);

            // Select appropriate layer based on OS
            if (Platform.CURRENT == Platform.MACOS) {
                layer = new LayerMetalSkija();
            } else if (Platform.CURRENT == Platform.WINDOWS) {
                layer = new LayerD3D12Skija();
            } else {
                layer = new LayerGLSkija();
            }

            window.setLayer(layer);
            window.setVisible(true);
            window.bringToFront();
            tryToSetTaskbarIcon();
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
            final String iconExt;
            if (Platform.CURRENT == Platform.MACOS) {
                iconExt = ".icns";
            } else if (Platform.CURRENT == Platform.WINDOWS) {
                iconExt = ".ico";
            } else {
                // X11 and Wayland (Linux) support standard PNGs
                iconExt = ".png";
            }

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

    @Override
    public void accept(final Event e) {
        if (e instanceof EventFrameSkija) {
            paint(((EventFrameSkija) e).getSurface());
        } else if (e instanceof EventWindowClose || e instanceof EventWindowCloseRequest) {
            // Don't terminate app here, let Squeak decide or use explicit close
            addWindowEvent(WINDOW.CLOSE);
        } else if (e instanceof EventWindowResize) {
            addWindowEvent(WINDOW.METRIC_CHANGE);
        } else if (e instanceof EventWindowFocusIn) {
            addWindowEvent(WINDOW.ACTIVATED);
        } else if (e instanceof EventWindowFocusOut) {
            addWindowEvent(WINDOW.DEACTIVATED);
        } else if (e instanceof EventMouseMove) {
            mouse.onMove((EventMouseMove) e);
        } else if (e instanceof EventMouseButton) {
            mouse.onButton((EventMouseButton) e);
        } else if (e instanceof EventMouseScroll) {
            mouse.onScroll((EventMouseScroll) e);
        } else if (e instanceof EventKey) {
            keyboard.onKey((EventKey) e);
        } else if (e instanceof EventTextInput) {
            keyboard.onTextInput((EventTextInput) e);
        }
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        if (window != null) {
            boolean shouldRequestFrame = false;

            synchronized (this) {
                // Zero-allocation, Zero-Java-copy snapshot directly into Skija's native memory!
                if (pixelIntBuffer != null && squeakBitmapPixels != null && pixelIntBuffer.capacity() == squeakBitmapPixels.length) {
                    pixelIntBuffer.clear(); // Reset the buffer position to 0
                    pixelIntBuffer.put(squeakBitmapPixels); // Fast block-transfer directly to C++
                }

                // Check and set the lock atomically within the pixel-copy boundary
                if (!frameRequested) {
                    frameRequested = true;
                    shouldRequestFrame = true;
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

    private void paint(final Surface surface) {
        if (window == null || surface == null || squeakBitmap == null) {
            return;
        }

        synchronized (this) {
            // Open the gate for the NEXT frame exactly as we consume the CURRENT frame
            frameRequested = false;

            final Canvas canvas = surface.getCanvas();
            canvas.clear(0xFF000000);

            // makeRasterFromBitmap safely handles the native GPU handoff
            try (Image skiaImage = Image.makeRasterFromBitmap(squeakBitmap)) {
                canvas.drawImage(skiaImage, 0, 0);
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

                // Clean up the old bitmap if we are resizing to prevent native memory leaks
                if (squeakBitmap != null) {
                    squeakBitmap.close();
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
            }

            // Force a repaint now that the buffers are ready
            showDisplayRect(0, 0, width - 1, height - 1);
        }
    }

    @TruffleBoundary
    public void close() {
        App.runOnUIThread(() -> {
            if (window != null) {
                window.close();
                window = null;
            }
        });
    }

    @TruffleBoundary
    public void resizeTo(final int width, final int height) {
        App.runOnUIThread(() -> {
            if (window != null) {
                window.setContentSize(width, height);
            }
        });
    }

    public int getWindowWidth() {
        return window != null ? window.getContentRect().getWidth() : 0;
    }

    public int getWindowHeight() {
        return window != null ? window.getContentRect().getHeight() : 0;
    }

    @TruffleBoundary
    public void setFullscreen(final boolean enable) {
        App.runOnUIThread(() -> {
            if (window == null) {
                return;
            }
            // JWM doesn't have a direct "setFullscreen" convenience yet that matches AWT exactly,
            // but we can maximize or use screen bounds.
            // For now, let's assuming maximizing is close enough or use explicit bounds.
            if (enable) {
                final IRect rect = window.getWindowRect();
                rememberedWindowX = rect.getLeft();
                rememberedWindowY = rect.getTop();
                rememberedWindowWidth = rect.getWidth();
                rememberedWindowHeight = rect.getHeight();

                // This is a simplification; true fullscreen often requires Screen API
                // window.maximize() is available in newer JWM versions?
                // Using setContentSize to screen size is a common workaround if API missing.
                // Assuming maximize is what we want:
                // window.maximize();
            } else {
                window.setWindowPosition(rememberedWindowX, rememberedWindowY);
                window.setContentSize(rememberedWindowWidth, rememberedWindowHeight);
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
        return window != null; // Simplified
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public void setCursor(final int[] cursorWords, final int[] mask, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
        // ToDo: Do this right!
        if (window == null) {
            return;
        }

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
        final ClipboardEntry entry = Clipboard.get(ClipboardFormat.TEXT);
        return entry == null ? "" : entry.getString();
    }

    @TruffleBoundary
    public static void setClipboardData(final String text) {
        Clipboard.set(ClipboardEntry.makeString(ClipboardFormat.TEXT, text));
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
