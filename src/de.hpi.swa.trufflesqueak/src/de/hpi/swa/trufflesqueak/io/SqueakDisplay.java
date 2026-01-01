/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.DRAG;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.WINDOW;
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
    @CompilationFinal(dimensions = 1) private static final int[] CURSOR_COLORS = {0x00000000, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000};

    public final SqueakImageContext image;

    // public for the Java-based UI for TruffleSqueak.
    public final Frame frame = new Frame(DEFAULT_WINDOW_TITLE);
    public final SqueakMouse mouse;
    public final SqueakKeyboard keyboard;

    private final SqueakDisplayCanvas canvas = new SqueakDisplayCanvas();
    private final ArrayDeque<long[]> deferredEvents = new ArrayDeque<>();

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons;
    private Dimension rememberedWindowSize;
    private Point rememberedWindowLocation;
    private boolean deferUpdates;

    private SqueakDisplay(final SqueakImageContext image) {
        assert EventQueue.isDispatchThread();
        this.image = image;
        frame.add(canvas);
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);
        frame.setFocusTraversalKeysEnabled(false); // Ensure `Tab` key is captured.
        frame.setMinimumSize(new Dimension(200, 150));
        frame.setResizable(true);

        // Install event listeners
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);
        frame.addKeyListener(keyboard);
        installWindowAdapter();
        installDropTargetListener();

        tryToSetTaskbarIcon();
    }

    public static SqueakDisplay create(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        final SqueakDisplay[] display = new SqueakDisplay[1];
        try {
            EventQueue.invokeAndWait(() -> display[0] = new SqueakDisplay(image));
        } catch (InvocationTargetException | InterruptedException e) {
            LogUtils.IO.warning(e.toString());
        }
        return Objects.requireNonNull(display[0]);
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
            final NativeObject bitmap = readNode.executeNative(null, squeakDisplay, FORM.BITS);
            if (!bitmap.isIntType()) {
                throw SqueakException.create("Display bitmap expected to be a words object");
            }
            final int width = readNode.executeInt(null, squeakDisplay, FORM.WIDTH);
            final int height = readNode.executeInt(null, squeakDisplay, FORM.HEIGHT);
            assert (long) squeakDisplay.instVarAt0Slow(FORM.DEPTH) == 32 : "Unsupported display depth";
            if (width > 0 && height > 0) {
                bufferedImage = MiscUtils.new32BitBufferedImage(bitmap.getIntStorage(), width, height, false);
            }
        }
    }

    @TruffleBoundary
    public void showDisplayRect(final int left, final int top, final int right, final int bottom) {
        assert left <= right && top <= bottom;
        canvas.paintImmediately(left, top, right, bottom);
    }

    @TruffleBoundary
    public void close() {
        EventQueue.invokeLater(() -> {
            frame.setVisible(false);
            frame.dispose();
        });
    }

    @TruffleBoundary
    public void resizeTo(final int width, final int height) {
        EventQueue.invokeLater(() -> {
            canvas.setPreferredSize(new Dimension(width, height));
            frame.pack();
        });
    }

    public int getWindowWidth() {
        return canvas.getWidth();
    }

    public int getWindowHeight() {
        return canvas.getHeight();
    }

    @TruffleBoundary
    public void setFullscreen(final boolean enable) {
        EventQueue.invokeLater(() -> {
            if (enable) {
                rememberedWindowLocation = frame.getLocationOnScreen();
                rememberedWindowSize = frame.getSize();
            }
            frame.dispose();
            frame.setUndecorated(enable);
            if (enable) {
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                frame.setResizable(false);
            } else {
                frame.setExtendedState(Frame.NORMAL);
                canvas.setPreferredSize(rememberedWindowSize);
                frame.pack();
                frame.setResizable(true);
            }
            frame.pack();
            if (!enable) {
                if (rememberedWindowLocation != null) {
                    frame.setLocation(rememberedWindowLocation);
                }
                rememberedWindowLocation = null;
                rememberedWindowSize = null;
            }
            frame.setVisible(true);
        });
    }

    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        canvas.setSqueakDisplay(sqDisplay);
        // Set or update frame title.
        final String imageFileName = new File(image.getImagePath()).getName();
        // Avoid name duplication in frame title.
        final String title;
        if (imageFileName.contains(SqueakLanguageConfig.IMPLEMENTATION_NAME)) {
            title = imageFileName;
        } else {
            title = imageFileName + " running on " + SqueakLanguageConfig.IMPLEMENTATION_NAME;
        }
        EventQueue.invokeLater(() -> {
            frame.setTitle(title);
            if (!frame.isVisible()) {
                canvas.setPreferredSize(new Dimension(image.flags.getSnapshotScreenWidth(), image.flags.getSnapshotScreenHeight()));
                frame.pack();
                frame.setVisible(true);
                frame.requestFocus();
            }
        });
    }

    @TruffleBoundary
    public boolean isVisible() {
        return frame.isVisible();
    }

    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] mask, final int width, final int height, final int depth, final int offsetX, final int offsetY) {
        final Dimension bestCursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
        final Cursor cursor;
        if (bestCursorSize.width == 0 || bestCursorSize.height == 0) {
            cursor = Cursor.getDefaultCursor();
        } else {
            // TODO: Ensure the below works correctly for all cursor and maybe refactor senders.
            final int[] ints;
            if (mask != null) {
                ints = mergeCursorWithMask(cursorWords, mask);
            } else {
                ints = cursorWords;
            }
            final BufferedImage bufferedImage;
            if (depth == 32) {
                bufferedImage = MiscUtils.new32BitBufferedImage(cursorWords, width, height, true);
            } else {
                bufferedImage = new BufferedImage(bestCursorSize.width, bestCursorSize.height, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < height; y++) {
                    final int word = ints[y];
                    for (int x = 0; x < width; x++) {
                        final int colorIndex = word >> (width - 1 - x) * 2 & 3;
                        bufferedImage.setRGB(x, y, CURSOR_COLORS[colorIndex]);
                    }
                }
            }
            // Ensure hotspot is within cursor bounds.
            final Point hotSpot = new Point(Math.min(Math.max(offsetX, 1), width - 1), Math.min(Math.max(offsetY, 1), height - 1));
            cursor = Toolkit.getDefaultToolkit().createCustomCursor(bufferedImage, hotSpot, "TruffleSqueak Cursor");
        }
        EventQueue.invokeLater(() -> frame.setCursor(cursor));
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
        EventQueue.invokeLater(() -> frame.setTitle(title));
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
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(final WindowEvent e) {
                addWindowEvent(WINDOW.ACTIVATED);
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                addWindowEvent(WINDOW.CLOSE);
            }

            @Override
            public void windowDeactivated(final WindowEvent e) {
                addWindowEvent(WINDOW.DEACTIVATED);
            }

            @Override
            public void windowIconified(final WindowEvent e) {
                addWindowEvent(WINDOW.ICONISE);
            }

            @Override
            public void windowStateChanged(final WindowEvent e) {
                addWindowEvent(WINDOW.METRIC_CHANGE);
            }
        });
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
